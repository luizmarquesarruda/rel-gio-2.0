package com.luizmarquesarruda.relogio20

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import com.luizmarquesarruda.relogio20.notifications.NotificationCenterActivity
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var content: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var logView: TextView
    private lateinit var clockView: TextView
    private lateinit var stepsView: TextView
    private lateinit var healthStatusView: TextView
    private var healthConnectClient: HealthConnectClient? = null
    private val healthScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stepReadPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(stepReadPermissions)) {
            appendLog("Permissão READ_STEPS concedida no Health Connect.")
            readTodaySteps()
        } else {
            healthStatusView.text = "Permissão de passos não concedida."
            appendLog("Acesso aos passos negado pelo usuário.")
        }
    }
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var isGattDisconnecting = false
    private var currentScanCallback: ScanCallback? = null
    private var scanStopTask: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val clockTicker = object : Runnable {
        override fun run() {
            updatePhoneClock()
            handler.postDelayed(this, 1_000L)
        }
    }
    private val stepsTicker = object : Runnable {
        override fun run() {
            if (healthConnectClient != null) readTodaySteps()
            handler.postDelayed(this, 60_000L)
        }
    }
    private val scanResults = linkedMapOf<String, ScanResult>()
    private val logLines = mutableListOf<String>()
    private val activeNotifications = mutableSetOf<String>()
    private val pendingNotifications = mutableSetOf<String>()
    private val gattOperationQueue = SerialGattOperationQueue()

    private val cccdUuid: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 2401
        private const val SCAN_DURATION_MS = 15_000L
        private const val MAX_LOG_LINES = 250
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter
        buildScreen()
        appendLog("Aplicativo iniciado. Diagnóstico BLE local.")
        appendLog("Nenhum comando será enviado às características do relógio.")
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(0xFFF7F8FA.toInt())
        }

        val title = TextView(this).apply {
            text = "Relógio 2.0"
            textSize = 26f
            setTextColor(0xFF17212B.toInt())
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Diagnóstico Bluetooth LE • fase 1"
            textSize = 14f
            setTextColor(0xFF526170.toInt())
        }
        root.addView(subtitle)

        clockView = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFF17212B.toInt())
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(clockView)
        updatePhoneClock()

        stepsView = TextView(this).apply {
            text = "Passos hoje: aguardando conexão"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(stepsView)

        healthStatusView = TextView(this).apply {
            text = "Fonte: Conexão Saúde (Health Connect)"
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(healthStatusView)

        val healthActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        healthActions.addView(makeButton("Conectar Health Connect") {
            connectHealthConnect()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        healthActions.addView(makeButton("Atualizar passos") {
            readTodaySteps()
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        root.addView(healthActions)

        root.addView(makeButton("WhatsApp e notificações Android") {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))

        addText(root, "Horário do relógio: sincronização ainda não disponível. " +
            "O protocolo de ajuste do GT3 MAX não foi identificado.", 12f)

        statusView = TextView(this).apply {
            text = "Pronto para procurar dispositivos."
            textSize = 14f
            setTextColor(0xFF263746.toInt())
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(statusView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actions.addView(makeButton("Procurar relógio") { startBleScan() },
            LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(makeButton("Desconectar") { disconnectGatt() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart = dp(8)
            })
        root.addView(actions)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val logTitle = TextView(this).apply {
            text = "Registro da sessão (permanece apenas nesta tela)"
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(logTitle)

        val logScroll = ScrollView(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        logView = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFF263746.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        logScroll.addView(logView)
        root.addView(logScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
        ))

        setContentView(root)
    }

    private fun updatePhoneClock() {
        clockView.text = "Hora do celular: " +
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun connectHealthConnect() {
        val availability = HealthConnectClient.getSdkStatus(this)
        if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
            healthStatusView.text = "Conexão Saúde indisponível. Verifique se está instalada/ativada."
            appendLog("Health Connect indisponível neste aparelho.")
            return
        }
        if (availability == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            healthStatusView.text = "Atualize a Conexão Saúde na Play Store."
            appendLog("Health Connect precisa de atualização.")
            return
        }

        val client = HealthConnectClient.getOrCreate(this)
        healthConnectClient = client
        healthScope.launch {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(stepReadPermissions)) {
                    readTodaySteps()
                } else {
                    healthPermissionLauncher.launch(stepReadPermissions)
                }
            } catch (error: Exception) {
                healthStatusView.text = "Não foi possível consultar as permissões."
                appendLog("Erro Health Connect: " + (error.message ?: "desconhecido"))
            }
        }
    }

    private fun readTodaySteps() {
        val client = healthConnectClient
        if (client == null) {
            connectHealthConnect()
            return
        }
        healthScope.launch {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                if (!granted.containsAll(stepReadPermissions)) {
                    stepsView.text = "Passos hoje: sem acesso"
                    healthStatusView.text = "Permissão READ_STEPS ausente. Use Conectar Health Connect para autorizar."
                    return@launch
                }
                val zone = ZoneId.systemDefault()
                val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
                val end = java.time.Instant.now()
                val response = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                val count = response[StepsRecord.COUNT_TOTAL]
                stepsView.text = StepsCountFormatter.label(count)
                if (count == null) {
                    healthStatusView.text = "Health Connect não retornou registros de passos para hoje."
                    appendLog("Health Connect sem registros de passos hoje; valor mantido como indisponível, não como zero.")
                } else {
                    healthStatusView.text =
                        "Dados agregados do Health Connect • atualizado " +
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    appendLog("Total agregado de passos de hoje no Health Connect: " + count)
                }
            } catch (error: SecurityException) {
                healthStatusView.text = "Acesso aos passos revogado ou não autorizado."
                appendLog("Permissão READ_STEPS ausente: " + (error.message ?: "sem detalhes"))
            } catch (error: Exception) {
                healthStatusView.text = "Falha ao ler passos do Health Connect."
                appendLog("Erro ao ler passos: " + (error.message ?: "desconhecido"))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockTicker)
        handler.post(clockTicker)
        handler.removeCallbacks(stepsTicker)
        if (healthConnectClient != null) readTodaySteps()
        handler.postDelayed(stepsTicker, 60_000L)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTicker)
        handler.removeCallbacks(stepsTicker)
        super.onPause()
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float = 14f,
        bold: Boolean = false
    ) {
        val view = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(0xFF263746.toInt())
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(4))
        }
        parent.addView(view)
    }

    private fun addDivider(parent: LinearLayout) {
        val divider = View(this).apply {
            setBackgroundColor(0xFFDCE2E8.toInt())
        }
        parent.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        ).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun requiredBlePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasBlePermissions(): Boolean {
        return requiredBlePermissions().all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun canReadDeviceIdentity(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        if (!canReadDeviceIdentity()) return null
        return runCatching { device.name }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceAddress(device: BluetoothDevice): String? {
        if (!canReadDeviceIdentity()) return null
        return runCatching { device.address }.getOrNull()
    }

    private fun requestBlePermissions() {
        requestPermissions(requiredBlePermissions(), REQUEST_BLE_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBleScan()
            } else {
                updateStatus("Permissão Bluetooth não concedida.")
                appendLog("Busca cancelada: permissão não concedida.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!hasBlePermissions()) {
            requestBlePermissions()
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            updateStatus("Este telefone não disponibilizou um adaptador Bluetooth.")
            appendLog("Bluetooth indisponível neste aparelho.")
            return
        }
        if (!adapter.isEnabled) {
            updateStatus("Ative o Bluetooth do telefone e tente novamente.")
            appendLog("Busca não iniciada: Bluetooth desligado.")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            updateStatus("Scanner BLE indisponível.")
            appendLog("Scanner BLE não foi disponibilizado pelo Android.")
            return
        }

        stopBleScan()
        scanResults.clear()
        content.removeAllViews()
        addText(content, "Dispositivos encontrados", 18f, true)
        updateStatus("Procurando dispositivos BLE por 15 segundos...")
        appendLog("Início da busca BLE por 15 segundos.")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                runOnUiThread {
                    if (currentScanCallback !== this) return@runOnUiThread
                    val address = safeDeviceAddress(result.device)
                    if (address == null) {
                        appendLog("Resultado BLE ignorado: permissão para identificar dispositivo indisponível.")
                        return@runOnUiThread
                    }
                    val name = result.scanRecord?.deviceName
                        ?: safeDeviceName(result.device)
                        ?: "Dispositivo sem nome"
                    scanResults[address] = result
                    renderScanResults()
                    updateStatus("Busca em andamento. Dispositivos: " + scanResults.size)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                runOnUiThread {
                    if (currentScanCallback !== this) return@runOnUiThread
                    scanStopTask?.let(handler::removeCallbacks)
                    scanStopTask = null
                    updateStatus("Falha na busca BLE (código " + errorCode + ").")
                    appendLog("Falha no scan BLE: " + errorCode)
                    currentScanCallback = null
                }
            }
        }

        currentScanCallback = callback
        try {
            scanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                callback
            )
        } catch (error: SecurityException) {
            currentScanCallback = null
            updateStatus("Permissão Bluetooth foi revogada. Autorize novamente.")
            appendLog("Scan BLE cancelado por SecurityException: " + (error.message ?: "sem detalhes"))
            return
        } catch (error: IllegalStateException) {
            currentScanCallback = null
            updateStatus("Não foi possível iniciar a busca BLE. Verifique o Bluetooth.")
            appendLog("Scan BLE não iniciado: " + (error.message ?: "estado inválido"))
            return
        }
        val timeout = Runnable {
            if (currentScanCallback === callback) {
                stopBleScan()
                updateStatus("Busca concluída. Dispositivos encontrados: " + scanResults.size)
                appendLog("Busca BLE concluída. Total: " + scanResults.size)
                renderScanResults()
            }
        }
        scanStopTask = timeout
        handler.postDelayed(timeout, SCAN_DURATION_MS)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        scanStopTask?.let(handler::removeCallbacks)
        scanStopTask = null
        val callback = currentScanCallback ?: return
        currentScanCallback = null
        runCatching {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
        }
    }

    private fun renderScanResults() {
        content.removeAllViews()
        addText(content, "Toque no GT3 MAX para conectar", 18f, true)

        if (scanResults.isEmpty()) {
            addText(content, "Nenhum dispositivo encontrado até agora.")
            return
        }

        scanResults.values
            .sortedByDescending { it.rssi }
            .forEach { result ->
                val address = safeDeviceAddress(result.device) ?: return@forEach
                val name = result.scanRecord?.deviceName
                    ?: safeDeviceName(result.device)
                    ?: "Dispositivo sem nome"
                val button = makeButton(
                    name + "  •  " + result.rssi + " dBm  •  " +
                        maskAddress(address)
                ) {
                    stopBleScan()
                    connectToDevice(result.device)
                }
                content.addView(button, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
                ))
            }
    }

    private fun maskAddress(address: String): String {
        val parts = address.split(":")
        return if (parts.size == 6) {
            "XX:XX:XX:" + parts[3] + ":" + parts[4] + ":" + parts[5]
        } else {
            "endereço oculto"
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasBlePermissions()) {
            requestBlePermissions()
            return
        }
        isGattDisconnecting = false
        gattOperationQueue.reset()
        pendingNotifications.clear()
        activeNotifications.clear()
        runCatching { bluetoothGatt?.close() }
        bluetoothGatt = null
        updateStatus("Conectando ao dispositivo BLE...")
        appendLog("Solicitada conexão BLE com " +
            (safeDeviceName(device) ?: "dispositivo sem nome") +
            " (" + (safeDeviceAddress(device)?.let(::maskAddress) ?: "endereço oculto") + ").")
        bluetoothGatt = device.connectGatt(
            this,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE,
            BluetoothDevice.PHY_LE_1M_MASK,
            handler
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (bluetoothGatt !== gatt) {
                runCatching { gatt.close() }
                return
            }
            appendLog("Estado GATT: status=" + status + ", estado=" + newState)
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED) {
                isGattDisconnecting = false
                updateStatus("Conectado. Descobrindo serviços GATT...")
                val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
                appendLog("Descoberta de serviços solicitada: " + started)
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                isGattDisconnecting = false
                gattOperationQueue.reset()
                pendingNotifications.clear()
                activeNotifications.clear()
                if (bluetoothGatt === gatt) bluetoothGatt = null
                updateStatus("Falha na conexão GATT. Status: " + status)
                appendLog("Conexão GATT falhou; estado e fila foram limpos.")
                runCatching { gatt.close() }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isGattDisconnecting = false
                updateStatus("Relógio desconectado.")
                appendLog("Conexão GATT encerrada.")
                if (bluetoothGatt === gatt) {
                    gattOperationQueue.reset()
                    pendingNotifications.clear()
                    activeNotifications.clear()
                    bluetoothGatt = null
                }
                runCatching { gatt.close() }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (bluetoothGatt !== gatt || isGattDisconnecting) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("Serviços descobertos: " + gatt.services.size)
                renderGattServices(gatt)
                updateStatus("Conectado. Serviços encontrados: " + gatt.services.size)
            } else {
                updateStatus("Falha ao descobrir serviços GATT.")
                appendLog("Falha na descoberta GATT. Status: " + status)
            }
        }

        @Deprecated("Deprecated by Android API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (bluetoothGatt !== gatt) return
            val value = characteristic.value ?: byteArrayOf()
            handleRead(characteristic.uuid.toString(), status, value)
            completeGattOperation(gatt)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (bluetoothGatt !== gatt) return
            handleRead(characteristic.uuid.toString(), status, value)
            completeGattOperation(gatt)
        }

        @Deprecated("Deprecated by Android API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (bluetoothGatt !== gatt) return
            handleNotification(characteristic.uuid.toString(), characteristic.value ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (bluetoothGatt !== gatt) return
            handleNotification(characteristic.uuid.toString(), value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (bluetoothGatt !== gatt) return
            val characteristic = descriptor.characteristic
            val key = characteristic.service.uuid.toString() + "/" +
                characteristic.uuid.toString()
            val wasPending = pendingNotifications.remove(key)
            if (wasPending && status == BluetoothGatt.GATT_SUCCESS) {
                activeNotifications.add(key)
                appendLog("Notificação confirmada pelo callback GATT para " + characteristic.uuid)
            } else if (wasPending) {
                activeNotifications.remove(key)
                gatt.setCharacteristicNotification(characteristic, false)
                appendLog("Falha na confirmação do CCCD para " + characteristic.uuid + ". Status: " + status)
            } else {
                appendLog("Callback de descritor sem inscrição pendente: " + descriptor.uuid)
            }
            completeGattOperation(gatt)
        }
    }

    private fun renderGattServices(gatt: BluetoothGatt) {
        runOnUiThread {
            content.removeAllViews()
            addText(content, "Inventário GATT", 19f, true)
            addText(content, "O app não oferece botões de escrita nas características. " +
                "A escuta usa somente o descritor padrão CCCD (0x2902).", 12f)

            val nusService = gatt.getService(WearfitProtocolProfile.SERVICE_UUID)
            val txCharacteristic = nusService?.getCharacteristic(
                WearfitProtocolProfile.TX_CHARACTERISTIC_UUID
            )
            if (txCharacteristic != null) {
                addText(
                    content,
                    "Perfil BLE candidato: NUS + quadros AB (compatibilidade observada em referência DT78). " +
                        "Identidade/chipset do GT3 Maxx não confirmados; comandos proprietários permanecem bloqueados.",
                    12f
                )
                appendLog("Fingerprint BLE candidato detectado: " + WearfitProtocolProfile.PROFILE_ID)
            } else {
                addText(content, "Perfil NUS/AB candidato não detectado nesta conexão.", 12f)
            }

            gatt.services.forEach { service ->
                addDivider(content)
                addText(content, "Serviço", 15f, true)
                addText(content, service.uuid.toString(), 12f)

                service.characteristics.forEach { characteristic ->
                    val properties = characteristic.properties
                    val labels = mutableListOf<String>()
                    if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                        labels.add("READ")
                    }
                    if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                        labels.add("NOTIFY")
                    }
                    if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                        labels.add("INDICATE")
                    }
                    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                        labels.add("WRITE (bloqueado)")
                    }
                    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                        labels.add("WRITE_NO_RESPONSE (bloqueado)")
                    }

                    addText(content, "Característica: " + characteristic.uuid, 13f, true)
                    addText(content, "Propriedades: " +
                        if (labels.isEmpty()) "não informadas" else labels.joinToString(" • "),
                        11f)

                    characteristic.descriptors.forEach { descriptor ->
                        addText(content, "Descritor: " + descriptor.uuid, 10f)
                    }

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                        row.addView(makeButton("Ler") {
                            readCharacteristic(characteristic)
                        }, LinearLayout.LayoutParams(0, dp(44), 1f))
                    }

                    if (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                            BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                        row.addView(makeButton("Ouvir") {
                            enableNotifications(gatt, characteristic)
                        }, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                            marginStart = dp(6)
                        })
                    }

                    if (row.childCount > 0) {
                        content.addView(row)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val gatt = bluetoothGatt
        if (gatt == null || isGattDisconnecting) {
            appendLog("Leitura cancelada: sem conexão GATT ativa ou desconexão em andamento.")
            return
        }
        gattOperationQueue.enqueue {
            if (bluetoothGatt !== gatt) {
                appendLog("Leitura descartada: conexão GATT mudou antes da execução.")
                false
            } else {
                val started = if (Build.VERSION.SDK_INT >= 33) {
                    gatt.readCharacteristic(characteristic)
                } else {
                    @Suppress("DEPRECATION")
                    gatt.readCharacteristic(characteristic)
                }
                appendLog("Leitura solicitada para " + characteristic.uuid +
                    ". Iniciada: " + started)
                if (!started) appendLog("O Android não iniciou a leitura.")
                started
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val key = characteristic.service.uuid.toString() + "/" +
            characteristic.uuid.toString()
        if (activeNotifications.contains(key) || pendingNotifications.contains(key)) {
            appendLog("Notificação já ativa ou pendente para " + characteristic.uuid)
            return
        }
        if (bluetoothGatt !== gatt || isGattDisconnecting) {
            appendLog("Inscrição cancelada: conexão GATT não está ativa para novas operações.")
            return
        }

        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor == null) {
            appendLog("Sem CCCD (0x2902): não foi possível ativar " +
                characteristic.uuid)
            return
        }

        val descriptorValue =
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }

        pendingNotifications.add(key)
        gattOperationQueue.enqueue {
            if (bluetoothGatt !== gatt) {
                pendingNotifications.remove(key)
                appendLog("Inscrição descartada: conexão GATT mudou antes da execução.")
                false
            } else {
                val localEnabled = gatt.setCharacteristicNotification(characteristic, true)
                if (!localEnabled) {
                    pendingNotifications.remove(key)
                    appendLog("Android recusou a inscrição local em " + characteristic.uuid)
                    false
                } else {
                    val writeStatus = if (Build.VERSION.SDK_INT >= 33) {
                        gatt.writeDescriptor(descriptor, descriptorValue)
                    } else {
                        @Suppress("DEPRECATION")
                        run {
                            descriptor.value = descriptorValue
                            if (gatt.writeDescriptor(descriptor)) {
                                BluetoothStatusCodes.SUCCESS
                            } else {
                                BluetoothStatusCodes.ERROR_UNKNOWN
                            }
                        }
                    }
                    if (writeStatus == BluetoothStatusCodes.SUCCESS) {
                        appendLog("CCCD solicitado para " + characteristic.uuid +
                            "; aguardando confirmação do callback GATT.")
                        true
                    } else {
                        pendingNotifications.remove(key)
                        gatt.setCharacteristicNotification(characteristic, false)
                        appendLog("Falha ao solicitar CCCD. Código: " + writeStatus)
                        false
                    }
                }
            }
        }
    }

    private fun completeGattOperation(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt) return
        gattOperationQueue.onOperationCompleted()
    }

    private fun handleRead(uuid: String, status: Int, value: ByteArray) {
        val description = if (value.isEmpty()) "0 bytes" else
            value.size.toString() + " bytes: " + BleValueFormatter.toHex(value)
        appendLog("Leitura " + uuid + " | status=" + status + " | " + description)
    }

    private fun handleNotification(uuid: String, value: ByteArray) {
        val description = if (value.isEmpty()) "0 bytes" else
            value.size.toString() + " bytes: " + BleValueFormatter.toHex(value)
        appendLog("NOTIFICAÇÃO " + uuid + " | " + description)

        if (uuid.equals(WearfitProtocolProfile.TX_CHARACTERISTIC_UUID.toString(), ignoreCase = true)) {
            val frame = WearfitAbFrameParser.parse(value)
            val battery = frame?.batteryPercent
            if (battery != null) {
                appendLog(
                    "Interpretação candidata do quadro AB/0x91: bateria reportada pelo relógio = " +
                        battery + "%. Não validada por comparação repetida com Wearfit Pro."
                )
            } else if (frame != null) {
                appendLog(
                    "Quadro AB reconhecido; comando=0x" +
                        frame.command.toString(16).uppercase(Locale.US) +
                        ", resposta=0x" + frame.response.toString(16).uppercase(Locale.US) +
                        ". Sem interpretação configurada para este comando."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectGatt() {
        stopBleScan()
        val gatt = bluetoothGatt
        if (gatt == null) {
            updateStatus("Nenhuma conexão ativa.")
            return
        }
        appendLog("Desconexão solicitada pelo usuário.")
        isGattDisconnecting = true
        gattOperationQueue.reset()
        pendingNotifications.clear()
        activeNotifications.clear()
        gatt.disconnect()
        updateStatus("Desconectando...")
    }

    private fun updateStatus(value: String) {
        runOnUiThread { statusView.text = value }
    }

    private fun appendLog(value: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        runOnUiThread {
            logLines.add(time + "  " + value)
            while (logLines.size > MAX_LOG_LINES) logLines.removeAt(0)
            logView.text = logLines.joinToString("\n")
        }
    }

    override fun onDestroy() {
        stopBleScan()
        gattOperationQueue.reset()
        pendingNotifications.clear()
        healthScope.cancel()
        isGattDisconnecting = true
        runCatching { bluetoothGatt?.disconnect() }
        runCatching { bluetoothGatt?.close() }
        bluetoothGatt = null
        super.onDestroy()
    }
}

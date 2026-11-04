package com.luizmarquesarruda.relogio20

import android.Manifest
import android.app.AlertDialog
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import com.luizmarquesarruda.relogio20.notifications.NotificationCenterActivity
import com.luizmarquesarruda.relogio20.device.ble.gatt.SerialGattOperationQueue
import com.luizmarquesarruda.relogio20.device.ble.core.BleReconnectPolicy
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.WearfitAbFrameParser
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.WearfitProtocolProfile
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.Gt3MaxCallDeviceProfile
import com.luizmarquesarruda.relogio20.HealthPrivacyActivity
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectAvailability
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsRepository
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsResult
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
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
import android.widget.EditText
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
    private lateinit var statusDetailView: TextView
    private lateinit var statusIndicatorView: TextView
    private lateinit var deviceProfileView: TextView
    private lateinit var deviceBatteryView: TextView
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var logView: TextView
    private lateinit var clockView: TextView
    private lateinit var stepsView: TextView
    private lateinit var healthStatusView: TextView
    private val healthConnectRepository by lazy(LazyThreadSafetyMode.NONE) {
        HealthConnectStepsRepository(this)
    }
    private var healthConnectInitialized = false
    private val healthScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthConnectRepository.readPermissions)) {
            healthConnectInitialized = true
            appendLog("Permissão READ_STEPS concedida no Health Connect.")
            readTodaySteps()
        } else {
            healthConnectInitialized = false
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
            if (healthConnectInitialized) readTodaySteps()
            handler.postDelayed(this, 60_000L)
        }
    }
    private val scanResults = linkedMapOf<String, ScanResult>()
    private val logLines = mutableListOf<String>()
    private var pendingDiagnosticReport: String? = null
    private var lastConnectedDeviceName: String? = null
    private var lastConnectedDeviceAddress: String? = null
    private val identityPrefs by lazy { getSharedPreferences("device_identity", MODE_PRIVATE) }
    private val expectedWatchAddress: String? get() = BleDeviceIdentity.normalize(identityPrefs.getString("gt3_max_address", null))
    private var autoConnectAttempted = false
    private var weakSignalWarningLogged = false
    private var isAutoReconnectScan = false
    private var isActivityResumed = false
    private var isGattConnected = false
    private var reconnectRequestedForResume = false
    private var reconnectRunnable: Runnable? = null
    private var connectionTimeoutTask: Runnable? = null
    private val connectionPrefs by lazy { getSharedPreferences("device_connection", MODE_PRIVATE) }
    private val reconnectPolicy by lazy {
        BleReconnectPolicy(connectionPrefs.getBoolean("auto_reconnect_enabled", false))
    }
    private val diagnosticExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val report = pendingDiagnosticReport
        pendingDiagnosticReport = null
        if (uri == null || report == null) {
            appendLog("Exportação do diagnóstico cancelada pelo usuário.")
        } else {
            runCatching {
                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(report.toByteArray(Charsets.UTF_8))
                } ?: error("Não foi possível abrir o destino escolhido.")
            }.onSuccess {
                appendLog("Relatório TXT exportado localmente com sucesso.")
            }.onFailure { error ->
                appendLog("Falha ao exportar relatório TXT: " + (error.message ?: "erro desconhecido"))
            }
        }
    }
    private val activeNotifications = mutableSetOf<String>()
    private val pendingNotifications = mutableSetOf<String>()
    private val gattOperationQueue = SerialGattOperationQueue()

    private val cccdUuid: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 2401
        private const val SCAN_DURATION_MS = 15_000L
        private const val AUTO_RECONNECT_SCAN_DURATION_MS = 8_000L
        private const val GATT_CONNECT_TIMEOUT_MS = 15_000L
        private const val AUTO_CONNECT_MIN_RSSI = -90
        private const val MAX_LOG_LINES = 250
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter
        buildScreen()
        appendLog("Aplicativo iniciado. Diagnóstico BLE local.")
        appendLog("Nenhum comando será enviado às características do relógio.")
        if (reconnectPolicy.isEnabled) {
            appendLog("Reconexão automática restaurada para a identidade GT3 MAX configurada localmente.")
        }
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
            text = "SAÚDE & FITNESS • smartwatch + Conexão Saúde"
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

        addText(root, "Conexão do relógio", 18f, true)

        val connectionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedSurface(0xFFFFFFFF.toInt(), 0xFFE1E7EF.toInt())
            elevation = dp(2).toFloat()
        }
        val connectionHeading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusIndicatorView = TextView(this).apply {
            text = "●"
            textSize = 18f
            setTextColor(0xFF8A94A6.toInt())
            setPadding(0, 0, dp(10), 0)
            contentDescription = "Indicador do estado Bluetooth"
        }
        connectionHeading.addView(statusIndicatorView)
        statusView = TextView(this).apply {
            text = "Relógio não conectado"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
        }
        connectionHeading.addView(statusView)
        connectionCard.addView(connectionHeading)

        statusDetailView = TextView(this).apply {
            text = "Ative o Bluetooth e toque em Buscar relógio para localizar o GT3 MAX."
            textSize = 13f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, dp(14))
        }
        connectionCard.addView(statusDetailView)

        deviceProfileView = TextView(this).apply {
            text = "Perfil informado: GT3 MAX Call • firmware V1.07 • build SH0050-1_1.1.3_1.23_1.18_1.1"
            textSize = 11f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(2), 0, dp(6))
        }
        connectionCard.addView(deviceProfileView)

        deviceBatteryView = TextView(this).apply {
            text = "Bateria do relógio: aguardando notificação BLE"
            textSize = 13f
            setTextColor(0xFF263746.toInt())
            setPadding(0, 0, 0, dp(12))
        }
        connectionCard.addView(deviceBatteryView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        scanButton = makeButton("Buscar e conectar automaticamente") { beginAutomaticConnection() }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF176B5B.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        disconnectButton = makeButton("Desconectar") { disconnectGatt() }.apply {
            isEnabled = false
        }
        actions.addView(scanButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(disconnectButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        connectionCard.addView(actions)
        root.addView(connectionCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(6)
            bottomMargin = dp(8)
        })

        root.addView(makeButton("Configurar identificação do GT3 MAX") { configureExpectedWatchAddress() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { bottomMargin = dp(6) })

        root.addView(makeButton("Exportar diagnóstico TXT") {
            exportDiagnosticReport()
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ).apply { bottomMargin = dp(6) })

        stepsView = TextView(this).apply {
            text = "Passos hoje: aguardando dados da Conexão Saúde"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(stepsView)

        healthStatusView = TextView(this).apply {
            text = "Fonte: Conexão Saúde • total agregado de fontes autorizadas"
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(healthStatusView)

        val healthActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        healthActions.addView(makeButton("Conectar Conexão Saúde") {
            connectHealthConnect()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        healthActions.addView(makeButton("Atualizar atividade") {
            readTodaySteps()
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        root.addView(healthActions)

        root.addView(makeButton("Notificações no relógio • leitura") {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))

        root.addView(makeButton("Explorar funções do relógio") {
            startActivity(Intent(this, FeatureHubActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))

        addText(root, "Fluxo de saúde: sensores do relógio → aplicativo → Conexão Saúde. " +
            "A gravação de amostras do GT3 Maxx será ativada somente após validar os dados e sua origem.", 12f)

        addText(root, "Frequência cardíaca, sono e SpO₂: aguardando integração e validação dos sensores. " +
            "Não exibir estimativas como medições confirmadas.", 12f)

        root.addView(makeButton("Dados de saúde e privacidade") {
            startActivity(Intent(this, HealthPrivacyActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))

        addText(root, "Horário do relógio: sincronização ainda não disponível. " +
            "O protocolo de ajuste do GT3 MAX não foi identificado.", 12f)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(content, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val logTitle = TextView(this).apply {
            text = "Registro da sessão (últimos 250 eventos; exportação manual disponível)"
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, dp(4))
        }
        root.addView(logTitle)

        logView = TextView(this).apply {
            textSize = 11f
            maxLines = 8
            setTextColor(0xFF263746.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedSurface(0xFFFFFFFF.toInt(), 0xFFE1E7EF.toInt())
        }
        root.addView(logView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val pageScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(pageScroll)
    }

    private fun updatePhoneClock() {
        clockView.text = "Hora do celular: " +
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun connectHealthConnect() {
        when (healthConnectRepository.availability()) {
            HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                healthConnectInitialized = false
                healthStatusView.text = "Conexão Saúde indisponível. Verifique se está instalada/ativada."
                appendLog("Health Connect indisponível neste aparelho.")
                return
            }
            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                healthConnectInitialized = false
                healthStatusView.text = "Atualize a Conexão Saúde na Play Store."
                appendLog("Health Connect precisa de atualização.")
                return
            }
            HealthConnectAvailability.AVAILABLE -> Unit
        }

        healthScope.launch {
            try {
                if (healthConnectRepository.hasReadPermission()) {
                    healthConnectInitialized = true
                    readTodaySteps()
                } else {
                    healthConnectInitialized = false
                    healthPermissionLauncher.launch(healthConnectRepository.readPermissions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                healthConnectInitialized = false
                healthStatusView.text = "Não foi possível consultar as permissões."
                appendLog("Erro Health Connect: " + (error.message ?: "desconhecido"))
            }
        }
    }

    private fun readTodaySteps() {
        if (!healthConnectInitialized) {
            connectHealthConnect()
            return
        }
        healthScope.launch {
            when (val result = healthConnectRepository.readTodaySteps()) {
                is HealthConnectStepsResult.Success -> {
                    val count = result.snapshot.totalSteps
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
                }
                HealthConnectStepsResult.PermissionRequired -> {
                    healthConnectInitialized = false
                    stepsView.text = "Passos hoje: sem acesso"
                    healthStatusView.text = "Permissão READ_STEPS ausente ou revogada."
                    appendLog("Permissão READ_STEPS ausente ou revogada durante a leitura.")
                }
                HealthConnectStepsResult.ProviderUnavailable -> {
                    healthConnectInitialized = false
                    healthStatusView.text = "Conexão Saúde indisponível. Verifique se está instalada/ativada."
                    appendLog("Health Connect ficou indisponível durante a leitura.")
                }
                HealthConnectStepsResult.ProviderUpdateRequired -> {
                    healthConnectInitialized = false
                    healthStatusView.text = "Atualize a Conexão Saúde na Play Store."
                    appendLog("Health Connect precisa de atualização.")
                }
                is HealthConnectStepsResult.Failure -> {
                    healthStatusView.text = "Falha ao ler passos do Health Connect."
                    appendLog("Erro ao ler passos: " + result.reason)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        handler.removeCallbacks(clockTicker)
        handler.post(clockTicker)
        handler.removeCallbacks(stepsTicker)
        if (healthConnectInitialized) readTodaySteps()
        handler.postDelayed(stepsTicker, 60_000L)
        if (reconnectPolicy.isEnabled && bluetoothGatt == null && currentScanCallback == null) {
            scheduleAutoReconnect("Aplicativo retomado", immediate = true)
        }
    }

    override fun onPause() {
        isActivityResumed = false
        if (reconnectRunnable != null) {
            handler.removeCallbacks(reconnectRunnable!!)
            reconnectRunnable = null
            reconnectRequestedForResume = reconnectPolicy.isEnabled
        }
        if (isAutoReconnectScan) stopBleScan()
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

    private fun roundedSurface(fillColor: Int, strokeColor: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(fillColor)
            setStroke(dp(1), strokeColor)
            cornerRadius = dp(18).toFloat()
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

    private fun beginAutomaticConnection() {
        cancelReconnectTask()
        if (expectedWatchAddress == null) configureExpectedWatchAddress { startBleScan() }
        else startBleScan()
    }

    private fun configureExpectedWatchAddress(afterSave: (() -> Unit)? = null) {
        val input = EditText(this).apply {
            hint = "AA:BB:CC:DD:EE:FF"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine(true)
            setText(expectedWatchAddress.orEmpty())
        }
        AlertDialog.Builder(this)
            .setTitle("Identificar meu GT3 MAX")
            .setMessage("Informe o endereço Bluetooth exibido no relógio. Será salvo somente neste aparelho, sem inclusão no código público ou no relatório TXT.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Remover") { _, _ -> identityPrefs.edit().remove("gt3_max_address").apply() }
            .setPositiveButton("Salvar") { _, _ ->
                val address = BleDeviceIdentity.normalize(input.text.toString())
                if (address == null) updateStatus("Endereço inválido. Use AA:BB:CC:DD:EE:FF.")
                else {
                    identityPrefs.edit().putString("gt3_max_address", address).apply()
                    appendLog("Identidade GT3 MAX salva localmente: " + maskAddress(address))
                    afterSave?.invoke()
                }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan(automatic: Boolean = false) {
        if (automatic && !isActivityResumed) {
            reconnectRequestedForResume = true
            appendLog("Reconexão automática adiada: aplicativo fora do primeiro plano.")
            return
        }
        if (bluetoothGatt != null) {
            val currentState = if (isGattDisconnecting) {
                "Desconectando..."
            } else {
                "Conectado. A busca foi bloqueada para preservar a sessão BLE ativa."
            }
            updateStatus(currentState)
            appendLog("Nova busca bloqueada: já existe uma sessão GATT ativa ou em encerramento.")
            return
        }
        if (!hasBlePermissions()) {
            if (automatic) {
                reconnectRequestedForResume = true
                updateStatus("Permissão Bluetooth necessária para reconectar.")
                appendLog("Reconexão pausada: permissão Bluetooth ausente; nenhuma solicitação automática foi exibida.")
            } else {
                requestBlePermissions()
            }
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
            if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Bluetooth do telefone desligado")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            updateStatus("Scanner BLE indisponível.")
            appendLog("Scanner BLE não foi disponibilizado pelo Android.")
            if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Scanner BLE indisponível")
            return
        }

        stopBleScan()
        isAutoReconnectScan = automatic
        autoConnectAttempted = false
        weakSignalWarningLogged = false
        scanResults.clear()
        content.removeAllViews()
        addText(content, "Dispositivos encontrados", 18f, true)
        updateStatus(if (automatic) "Procurando GT3 MAX para reconexão automática..." else "Procurando dispositivos BLE por 15 segundos...")
        appendLog(if (automatic) "Início da busca automática pelo endereço GT3 MAX salvo localmente." else "Início da busca BLE por 15 segundos.")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                runOnUiThread {
                    if (currentScanCallback !== this) return@runOnUiThread
                    val address = safeDeviceAddress(result.device)
                    if (address == null) {
                        appendLog("Resultado BLE ignorado: permissão para identificar dispositivo indisponível.")
                        return@runOnUiThread
                    }
                    if (automatic && !BleDeviceIdentity.matches(expectedWatchAddress, address)) return@runOnUiThread
                    val name = result.scanRecord?.deviceName
                        ?: safeDeviceName(result.device)
                        ?: "Dispositivo sem nome"
                    scanResults[address] = result
                    appendLog("Dispositivo BLE observado: " + name + " | RSSI=" + result.rssi + " dBm | endereço=" + maskAddress(address))
                    if (!autoConnectAttempted && BleDeviceIdentity.matches(expectedWatchAddress, address)) {
                        if (result.rssi <= AUTO_CONNECT_MIN_RSSI) {
                            if (!weakSignalWarningLogged) {
                                weakSignalWarningLogged = true
                                appendLog("GT3 MAX identificado, mas RSSI fraco: " + result.rssi + " dBm. Conexão automática suspensa.")
                                updateStatus("GT3 MAX identificado, mas sinal muito fraco.")
                            }
                        } else {
                            autoConnectAttempted = true
                            appendLog("Correspondência exata do GT3 MAX confirmada; iniciando conexão automática.")
                            stopBleScan()
                            connectToDevice(result.device, automaticAttempt = automatic)
                            return@runOnUiThread
                        }
                    }
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
                    isAutoReconnectScan = false
                    if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Falha no scan BLE: " + errorCode)
                }
            }
        }

        currentScanCallback = callback
        try {
            scanner.startScan(
                null,
                ScanSettings.Builder()
                    .setScanMode(if (automatic) ScanSettings.SCAN_MODE_LOW_POWER else ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build(),
                callback
            )
        } catch (error: SecurityException) {
            currentScanCallback = null
            isAutoReconnectScan = false
            updateStatus("Permissão Bluetooth foi revogada. Autorize novamente.")
            appendLog("Scan BLE cancelado por SecurityException: " + (error.message ?: "sem detalhes"))
            if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Permissão Bluetooth revogada")
            return
        } catch (error: IllegalStateException) {
            currentScanCallback = null
            isAutoReconnectScan = false
            updateStatus("Não foi possível iniciar a busca BLE. Verifique o Bluetooth.")
            appendLog("Scan BLE não iniciado: " + (error.message ?: "estado inválido"))
            if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Falha ao iniciar o scan BLE")
            return
        }
        val timeout = Runnable {
            if (currentScanCallback === callback) {
                stopBleScan()
                val foundExpected = expectedWatchAddress?.let { expected ->
                    scanResults.keys.any { BleDeviceIdentity.matches(expected, it) }
                } ?: false
                updateStatus(if (foundExpected) "GT3 MAX encontrado, mas sinal insuficiente para conexão automática."
                    else "Busca concluída. GT3 MAX não identificado.")
                appendLog("Busca BLE concluída. Total=" + scanResults.size + "; identidade esperada encontrada=" + foundExpected)
                renderScanResults()
                if ((automatic || reconnectPolicy.isEnabled) && !foundExpected) {
                    scheduleAutoReconnect("GT3 MAX não localizado nesta tentativa")
                } else if (automatic && foundExpected) {
                    scheduleAutoReconnect("Sinal do GT3 MAX insuficiente nesta tentativa")
                }
            }
        }
        scanStopTask = timeout
        handler.postDelayed(timeout, if (automatic) AUTO_RECONNECT_SCAN_DURATION_MS else SCAN_DURATION_MS)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        scanStopTask?.let(handler::removeCallbacks)
        scanStopTask = null
        val callback = currentScanCallback
        currentScanCallback = null
        isAutoReconnectScan = false
        if (callback == null) return
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
                val deviceCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = roundedSurface(0xFFFFFFFF.toInt(), 0xFFE1E7EF.toInt())
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Conectar a $name. Sinal ${result.rssi} dBm."
                    setOnClickListener {
                        stopBleScan()
                        connectToDevice(result.device)
                    }
                }
                val deviceName = TextView(this).apply {
                    text = name
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF17212B.toInt())
                }
                val deviceMeta = TextView(this).apply {
                    text = "Sinal ${result.rssi} dBm  •  identificação protegida: " + maskAddress(address)
                    textSize = 12f
                    setTextColor(0xFF526170.toInt())
                    setPadding(0, dp(4), 0, 0)
                }
                deviceCard.addView(deviceName)
                deviceCard.addView(deviceMeta)
                content.addView(deviceCard, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                })
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
    private fun connectToDevice(device: BluetoothDevice, automaticAttempt: Boolean = false) {
        val observedAddress = safeDeviceAddress(device)
        if (!BleDeviceIdentity.matches(expectedWatchAddress, observedAddress)) {
            updateStatus("Dispositivo não identificado. Conexão bloqueada.")
            appendLog("Conexão bloqueada: endereço observado não corresponde à identidade local configurada.")
            return
        }
        if (!hasBlePermissions()) {
            requestBlePermissions()
            return
        }
        isGattDisconnecting = false
        isGattConnected = false
        cancelConnectionTimeout()
        lastConnectedDeviceName = safeDeviceName(device) ?: "Dispositivo sem nome"
        lastConnectedDeviceAddress = safeDeviceAddress(device)
        gattOperationQueue.reset()
        pendingNotifications.clear()
        activeNotifications.clear()
        runCatching { bluetoothGatt?.close() }
        bluetoothGatt = null
        updateStatus("Conectando ao dispositivo BLE...")
        appendLog("Solicitada conexão BLE com " +
            (safeDeviceName(device) ?: "dispositivo sem nome") +
            " (" + (safeDeviceAddress(device)?.let(::maskAddress) ?: "endereço oculto") + ").")
        val newGatt = try {
            device.connectGatt(
                this,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK,
                handler
            )
        } catch (error: SecurityException) {
            updateStatus("Permissão Bluetooth foi revogada. Autorize novamente.")
            appendLog("Conexão GATT bloqueada por permissão: " + (error.message ?: "sem detalhes"))
            return
        } catch (error: IllegalArgumentException) {
            updateStatus("Dispositivo BLE inválido ou indisponível.")
            appendLog("Não foi possível iniciar GATT: " + (error.message ?: "argumento inválido"))
            return
        }
        if (newGatt == null) {
            updateStatus("O Android não iniciou a conexão GATT.")
            appendLog("connectGatt retornou null.")
            return
        }
        bluetoothGatt = newGatt
        val timeout = Runnable {
            if (bluetoothGatt === newGatt && !isGattConnected) {
                bluetoothGatt = null
                isGattDisconnecting = false
                gattOperationQueue.reset()
                pendingNotifications.clear()
                activeNotifications.clear()
                runCatching { newGatt.disconnect() }
                runCatching { newGatt.close() }
                updateStatus("Falha na conexão GATT: tempo limite aguardando conexão.")
                appendLog("Tempo limite de conexão GATT após ${GATT_CONNECT_TIMEOUT_MS} ms.")
                if (automaticAttempt || reconnectPolicy.isEnabled) {
                    scheduleAutoReconnect("Tempo limite de conexão GATT")
                }
            }
        }
        connectionTimeoutTask = timeout
        handler.postDelayed(timeout, GATT_CONNECT_TIMEOUT_MS)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
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
                newState == BluetoothProfile.STATE_CONNECTING) {
                updateStatus("Conectando ao dispositivo BLE...")
            } else if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED) {
                isGattDisconnecting = false
                isGattConnected = true
                cancelConnectionTimeout()
                reconnectPolicy.markConnected()
                connectionPrefs.edit().putBoolean("auto_reconnect_enabled", true).apply()
                reconnectRequestedForResume = false
                appendLog("Reconexão automática habilitada após conexão GATT confirmada.")
                updateStatus("Conectado. Descobrindo serviços GATT...")
                val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
                appendLog("Descoberta de serviços solicitada: " + started)
                if (!started) {
                    updateStatus("Falha ao iniciar descoberta dos serviços GATT.")
                    appendLog("O Android recusou o início da descoberta GATT.")
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                isGattDisconnecting = false
                isGattConnected = false
                cancelConnectionTimeout()
                gattOperationQueue.reset()
                pendingNotifications.clear()
                activeNotifications.clear()
                if (bluetoothGatt === gatt) bluetoothGatt = null
                updateStatus("Falha na conexão GATT. Status: " + status)
                appendLog("Conexão GATT falhou; estado e fila foram limpos.")
                runCatching { gatt.close() }
                if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Falha na conexão GATT: " + status)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isGattDisconnecting = false
                isGattConnected = false
                cancelConnectionTimeout()
                deviceBatteryView.text = "Bateria do relógio: sem sessão BLE ativa"
                updateStatus("Relógio desconectado.")
                appendLog("Conexão GATT encerrada.")
                if (bluetoothGatt === gatt) {
                    gattOperationQueue.reset()
                    pendingNotifications.clear()
                    activeNotifications.clear()
                    bluetoothGatt = null
                }
                runCatching { gatt.close() }
                if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Conexão GATT perdida")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (bluetoothGatt !== gatt || isGattDisconnecting) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("Serviços descobertos: " + gatt.services.size)
                renderGattServices(gatt)
                inspectGt3MaxProfileAndSubscribe(gatt)
                updateStatus("Conectado. Serviços encontrados: " + gatt.services.size)
            } else {
                updateStatus("Falha ao descobrir serviços GATT.")
                appendLog("Falha na descoberta GATT. Status: " + status)
            }
        }

        @Deprecated("Deprecated by Android API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (bluetoothGatt !== gatt) return
            handleNotification(characteristic.service.uuid.toString(), characteristic.uuid.toString(), characteristic.value ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (bluetoothGatt !== gatt) return
            handleNotification(characteristic.service.uuid.toString(), characteristic.uuid.toString(), value)
        }

        @SuppressLint("MissingPermission")
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
                if (hasBlePermissions()) {
                    runCatching { gatt.setCharacteristicNotification(characteristic, false) }
                } else {
                    appendLog("Permissão Bluetooth revogada; não foi possível desativar a inscrição local.")
                }
                appendLog("Falha na confirmação do CCCD para " + characteristic.uuid + ". Status: " + status)
            } else {
                appendLog("Callback de descritor sem inscrição pendente: " + descriptor.uuid)
            }
            completeGattOperation(gatt)
        }
    }

    @SuppressLint("MissingPermission")
    private fun inspectGt3MaxProfileAndSubscribe(gatt: BluetoothGatt) {
        val observed = gatt.services.associate { service ->
            service.uuid to service.characteristics.map { it.uuid }.toSet()
        }
        val match = Gt3MaxCallDeviceProfile.evaluate(lastConnectedDeviceName, observed)
        val matchLabel = when (match) {
            Gt3MaxCallDeviceProfile.Match.CAPTURED_GATT_FINGERPRINT ->
                "assinatura GATT igual à captura de referência"
            Gt3MaxCallDeviceProfile.Match.DEVICE_NAME_AND_NUS ->
                "nome e transporte NUS reconhecidos; assinatura parcial"
            Gt3MaxCallDeviceProfile.Match.DEVICE_NAME_ONLY ->
                "nome reconhecido; transporte NUS ausente"
            Gt3MaxCallDeviceProfile.Match.NUS_ONLY ->
                "NUS presente; nome não confirmado"
            Gt3MaxCallDeviceProfile.Match.OTHER ->
                "perfil específico não identificado"
        }
        deviceProfileView.text =
            "Perfil informado: GT3 MAX Call • firmware V1.07 • build SH0050-1_1.1.3_1.23_1.18_1.1\n" +
                "Identificação GATT: " + matchLabel +
                ". Firmware/build são metadados informados, não lidos diretamente do GATT."
        appendLog("Identificação do perfil GT3 MAX Call: " + match.name)

        if (!Gt3MaxCallDeviceProfile.shouldAutoSubscribeBattery(lastConnectedDeviceName, observed)) {
            appendLog("Inscrição automática NUS TX não iniciada: nome e serviço/characterísticas exigidos não coincidiram.")
            return
        }

        val tx = gatt.getService(Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID)
            ?.getCharacteristic(Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID)
        if (tx == null ||
            tx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0 ||
            tx.getDescriptor(cccdUuid) == null
        ) {
            appendLog("NUS TX detectado sem Notify/CCCD compatível; inscrição automática não realizada.")
            return
        }

        appendLog("GT3 MAX Call reconhecido como candidato. Ativando somente NUS TX Notify via CCCD padrão; NUS RX permanece sem escrita.")
        enableNotifications(gatt, tx)
    }

    @SuppressLint("MissingPermission")
    private fun renderGattServices(gatt: BluetoothGatt) {
        runOnUiThread {
            content.removeAllViews()
            addText(content, "Inventário GATT", 19f, true)
            addText(content, "Inventário passivo: características desconhecidas não são lidas nem inscritas. " +
                "Somente NUS TX pode receber inscrição automática via CCCD padrão, após o fingerprint completo do GT3 MAX Call.", 12f)

            val nusService = gatt.getService(WearfitProtocolProfile.SERVICE_UUID)
            val rxCharacteristic = nusService?.getCharacteristic(
                WearfitProtocolProfile.RX_CHARACTERISTIC_UUID
            )
            val txCharacteristic = nusService?.getCharacteristic(
                WearfitProtocolProfile.TX_CHARACTERISTIC_UUID
            )
            if (rxCharacteristic != null && txCharacteristic != null) {
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
                appendLog("GATT serviço observado: " + service.uuid)
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

                    appendLog("GATT característica observada: serviço=" + service.uuid + " | característica=" + characteristic.uuid + " | propriedades=" + if (labels.isEmpty()) "não informadas" else labels.joinToString(","))
                    characteristic.descriptors.forEach { descriptor ->
                        appendLog("GATT descritor observado: " + descriptor.uuid + " | característica=" + characteristic.uuid)
                        addText(content, "Descritor: " + descriptor.uuid, 10f)
                    }

                }
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
                    val writeStarted = if (Build.VERSION.SDK_INT >= 33) {
                        writeDescriptorApi33(gatt, descriptor, descriptorValue)
                    } else {
                        @Suppress("DEPRECATION")
                        run {
                            descriptor.value = descriptorValue
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                    if (writeStarted) {
                        appendLog("CCCD solicitado para " + characteristic.uuid +
                            "; aguardando confirmação do callback GATT.")
                        true
                    } else {
                        pendingNotifications.remove(key)
                        gatt.setCharacteristicNotification(characteristic, false)
                        appendLog("O Android recusou iniciar a inscrição CCCD para " + characteristic.uuid)
                        false
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun writeDescriptorApi33(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ): Boolean =
        gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
    private fun completeGattOperation(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt) return
        gattOperationQueue.onOperationCompleted()
    }

    private fun handleNotification(serviceUuid: String, uuid: String, value: ByteArray) {
        val description = if (value.isEmpty()) "0 bytes" else
            value.size.toString() + " bytes: " + BleValueFormatter.toHex(value)
        appendLog("NOTIFICAÇÃO " + uuid + " | " + description)

        if (serviceUuid.equals(WearfitProtocolProfile.SERVICE_UUID.toString(), ignoreCase = true) &&
            uuid.equals(WearfitProtocolProfile.TX_CHARACTERISTIC_UUID.toString(), ignoreCase = true)) {
            val frame = WearfitAbFrameParser.parse(value)
            val battery = frame?.batteryPercent
            if (battery != null) {
                runOnUiThread {
                    val receivedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    deviceBatteryView.text =
                        "Bateria BLE candidata: " + battery + "% • recebido " + receivedAt +
                            " • aguardando confirmação com Wearfit Pro"
                }
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
        cancelReconnectTask()
        reconnectPolicy.markManuallyDisconnected()
        connectionPrefs.edit().putBoolean("auto_reconnect_enabled", false).apply()
        reconnectRequestedForResume = false
        appendLog("Reconexão automática desativada por solicitação do usuário.")
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
        runOnUiThread {
            val headline: String
            val detail: String
            val indicatorColor: Int
            when {
                value.startsWith("Reconexão automática aguardando") -> {
                    headline = "Reconexão automática"
                    detail = value + ". O aplicativo tentará localizar somente o GT3 MAX salvo neste aparelho."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Reconectando") -> {
                    headline = "Reconectando GT3 MAX"
                    detail = "Tentativa automática em andamento; nenhum comando de escrita será enviado ao relógio."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Procurando") -> {
                    headline = "Procurando relógio"
                    detail = "Busca BLE ativa por até 15 segundos. Mantenha o GT3 MAX próximo ao celular."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Busca em andamento") -> {
                    headline = "Busca em andamento"
                    detail = value + ". Toque no dispositivo encontrado para iniciar a conexão."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Busca concluída") -> {
                    headline = "Busca finalizada"
                    detail = value + ". Se o relógio não apareceu, aproxime-o e tente novamente."
                    indicatorColor = 0xFF8A94A6.toInt()
                }
                value.startsWith("Conectando") -> {
                    headline = "Conectando ao relógio"
                    detail = "O Android está abrindo a conexão Bluetooth LE. Aguarde a resposta do dispositivo."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Conectado.") && value.contains("Descobrindo") -> {
                    headline = "Bluetooth conectado"
                    detail = "Conexão estabelecida. Identificando os serviços GATT disponíveis."
                    indicatorColor = 0xFF16845B.toInt()
                }
                value.startsWith("Conectado.") -> {
                    headline = "Bluetooth conectado"
                    detail = value + ". Isso confirma o canal GATT, não a leitura validada dos sensores."
                    indicatorColor = 0xFF16845B.toInt()
                }
                value.startsWith("Desconectando") -> {
                    headline = "Encerrando conexão"
                    detail = "A conexão com o relógio está sendo encerrada."
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Relógio desconectado") || value.startsWith("Nenhuma conexão") -> {
                    headline = "Relógio desconectado"
                    detail = "O relógio não está conectado. Você pode iniciar uma nova busca."
                    indicatorColor = 0xFF8A94A6.toInt()
                }
                value.startsWith("Falha na conexão GATT. Status: 147") -> {
                    headline = "Tempo limite na conexão BLE"
                    detail = "A conexão GATT expirou (status 147). Aproxime o relógio, mantenha-o ativo e tente novamente. A causa ainda não foi determinada."
                    indicatorColor = 0xFFCA4B4B.toInt()
                }
                value.startsWith("Falha") || value.startsWith("Não foi possível") ||
                    value.startsWith("O Android não iniciou") -> {
                    headline = "Não foi possível conectar"
                    detail = value + " Confira a proximidade do relógio e tente novamente."
                    indicatorColor = 0xFFCA4B4B.toInt()
                }
                value.startsWith("Ative o Bluetooth") -> {
                    headline = "Bluetooth desligado"
                    detail = value
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                value.startsWith("Permissão") -> {
                    headline = "Permissão necessária"
                    detail = value
                    indicatorColor = 0xFFE0A12B.toInt()
                }
                else -> {
                    headline = "Estado da conexão"
                    detail = value
                    indicatorColor = 0xFF8A94A6.toInt()
                }
            }
            statusView.text = headline
            statusDetailView.text = detail
            statusIndicatorView.setTextColor(indicatorColor)
            val gattSessionExists = bluetoothGatt != null
            val scanInProgress = value.startsWith("Procurando") ||
                value.startsWith("Busca em andamento")
            scanButton.isEnabled = !gattSessionExists && !scanInProgress &&
                !value.startsWith("Conectando") && !value.startsWith("Desconectando") &&
                !value.startsWith("Reconectando")
            disconnectButton.isEnabled = gattSessionExists
        }
    }

    private fun exportDiagnosticReport() {
        val now = Date()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(now)
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "desconhecida"
        }.getOrDefault("desconhecida")
        val report = BleDiagnosticReport.build(
            appVersion = version,
            androidVersion = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
            connectionState = statusView.text?.toString() ?: "indisponível",
            deviceName = lastConnectedDeviceName,
            deviceAddress = lastConnectedDeviceAddress,
            generatedAt = generatedAt,
            logLines = logLines.toList()
        )
        pendingDiagnosticReport = report
        diagnosticExportLauncher.launch("relogio2_diagnostico_GT3MAX_" + timestamp + ".txt")
        appendLog("Solicitada exportação TXT. O Android abrirá o seletor para escolher onde salvar.")
    }

    private fun appendLog(value: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        runOnUiThread {
            logLines.add(time + "  " + value)
            while (logLines.size > MAX_LOG_LINES) logLines.removeAt(0)
            logView.text = logLines.takeLast(8).joinToString("\n")
        }
    }

    private fun cancelReconnectTask() {
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
    }

    private fun cancelConnectionTimeout() {
        connectionTimeoutTask?.let(handler::removeCallbacks)
        connectionTimeoutTask = null
    }

    private fun scheduleAutoReconnect(reason: String, immediate: Boolean = false) {
        if (!reconnectPolicy.isEnabled) return
        if (expectedWatchAddress == null) {
            appendLog("Reconexão automática bloqueada: identidade GT3 MAX não está configurada.")
            return
        }
        if (!isActivityResumed) {
            reconnectRequestedForResume = true
            appendLog("Reconexão automática aguardará o aplicativo voltar ao primeiro plano.")
            return
        }
        if (bluetoothGatt != null || currentScanCallback != null) return

        cancelReconnectTask()
        val delayMillis = if (immediate) 0L else reconnectPolicy.nextRetryDelayMillis() ?: return
        appendLog(reason + ". Próxima tentativa de reconexão em " + delayMillis + " ms.")
        updateStatus(if (delayMillis == 0L) "Reconectando GT3 MAX..." else "Reconexão automática aguardando " + delayMillis + " ms")

        val task = Runnable {
            reconnectRunnable = null
            if (!isActivityResumed) {
                reconnectRequestedForResume = true
                return@Runnable
            }
            if (!reconnectPolicy.isEnabled || bluetoothGatt != null || currentScanCallback != null) return@Runnable
            if (!hasBlePermissions()) {
                reconnectRequestedForResume = true
                updateStatus("Permissão Bluetooth necessária para reconectar.")
                appendLog("Reconexão pausada: restaure a permissão Bluetooth e retorne ao aplicativo.")
                return@Runnable
            }
            startBleScan(automatic = true)
        }
        reconnectRunnable = task
        if (delayMillis == 0L) handler.post(task) else handler.postDelayed(task, delayMillis)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        isActivityResumed = false
        cancelReconnectTask()
        cancelConnectionTimeout()
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

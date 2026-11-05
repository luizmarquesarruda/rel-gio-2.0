package com.luizmarquesarruda.relogio20.ui.screens

import android.Manifest
import android.app.AlertDialog
import android.annotation.SuppressLint
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.provider.Settings
import com.luizmarquesarruda.relogio20.notifications.NotificationCenterActivity
import com.luizmarquesarruda.relogio20.device.ble.core.BleDeviceIdentity
import com.luizmarquesarruda.relogio20.device.ble.core.BleScanCandidatePolicy
import com.luizmarquesarruda.relogio20.device.ble.core.BleValueFormatter
import com.luizmarquesarruda.relogio20.domain.diagnostics.BleDiagnosticReport
import com.luizmarquesarruda.relogio20.ui.formatters.StepsCountFormatter
import com.luizmarquesarruda.relogio20.device.ble.gatt.SerialGattOperationQueue
import com.luizmarquesarruda.relogio20.device.ble.gatt.BluetoothCurrentTimePayload
import com.luizmarquesarruda.relogio20.device.ble.gatt.BluetoothCurrentTimeCapabilityPolicy
import com.luizmarquesarruda.relogio20.device.ble.core.BleReconnectPolicy
import com.luizmarquesarruda.relogio20.device.ble.core.BleReconnectCoordinator
import com.luizmarquesarruda.relogio20.device.ble.core.GattCallbackSafetyPolicy
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.WearfitAbFrameParser
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.WearfitProtocolProfile
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.Gt3MaxCallDeviceProfile
import com.luizmarquesarruda.relogio20.data.discovery.FeatureDiscoveryStore
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryKind
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryStore
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryReadResult
import com.luizmarquesarruda.relogio20.device.discovery.GattCharacteristicRecord
import com.luizmarquesarruda.relogio20.device.discovery.GattServiceRecord
import com.luizmarquesarruda.relogio20.device.discovery.SafeGattExplorationPlan
import com.luizmarquesarruda.relogio20.device.discovery.AutomaticGattDiscoveryPolicy
import com.luizmarquesarruda.relogio20.device.discovery.AutomaticCaptureSessionPolicy
import com.luizmarquesarruda.relogio20.device.discovery.GattValueDecoder
import com.luizmarquesarruda.relogio20.device.discovery.GattExplorationProgressTracker
import com.luizmarquesarruda.relogio20.device.discovery.GattProgressOperation
import com.luizmarquesarruda.relogio20.device.discovery.BlePacketFingerprintAnalyzer
import com.luizmarquesarruda.relogio20.device.discovery.PassiveBleObservationTracker
import com.luizmarquesarruda.relogio20.device.discovery.GattDiscoveryRecoveryPolicy
import com.luizmarquesarruda.relogio20.device.gt3max.protocol.WearfitAbStreamDecoder
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectAvailability
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsRepository
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsResult
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectPermissionState
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectPermissionStatePolicy
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
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
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    private lateinit var content: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var statusDetailView: TextView
    private lateinit var statusIndicatorView: TextView
    private lateinit var deviceProfileView: TextView
    private lateinit var deviceBatteryView: TextView
    private lateinit var deviceTimeSyncView: TextView
    private lateinit var clockSyncButton: Button
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var deepScanButton: Button
    private lateinit var automaticGattDiscoveryButton: Button
    private lateinit var captureSessionView: TextView
    private lateinit var gattProgressView: TextView
    private val gattExplorationProgress = GattExplorationProgressTracker()
    private lateinit var logView: TextView
    private lateinit var clockView: TextView
    private lateinit var stepsView: TextView
    private lateinit var healthStatusView: TextView
    private lateinit var healthSettingsButton: Button
    private val healthConnectRepository by lazy(LazyThreadSafetyMode.NONE) {
        HealthConnectStepsRepository(this)
    }
    private val featureDiscoveryStore by lazy(LazyThreadSafetyMode.NONE) {
        FeatureDiscoveryStore(this)
    }
    private val discoveryLibraryStore by lazy(LazyThreadSafetyMode.NONE) {
        DiscoveryLibraryStore(this)
    }
    private var healthConnectInitialized = false
    private val healthPermissionPrefs by lazy {
        getSharedPreferences("health_connect_permission_state", MODE_PRIVATE)
    }
    private var healthPermissionState: HealthConnectPermissionState
        get() = runCatching {
            HealthConnectPermissionState.valueOf(
                healthPermissionPrefs.getString("read_steps_state", HealthConnectPermissionState.NOT_REQUESTED.name)
                    ?: HealthConnectPermissionState.NOT_REQUESTED.name
            )
        }.getOrDefault(HealthConnectPermissionState.NOT_REQUESTED)
        set(value) {
            healthPermissionPrefs.edit { putString("read_steps_state", value.name) }
        }
    private var healthPermissionRequestPending = false
    private var lastLoggedStepsTotal: Long? = null
    private var missingStepsWasLogged = false
    private val healthScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthPermissionRequestPending = false
        val permissionGranted = granted.containsAll(healthConnectRepository.readPermissions)
        healthPermissionState = HealthConnectPermissionStatePolicy.afterPermissionResult(permissionGranted)
        healthConnectInitialized = permissionGranted
        updateHealthConnectPermissionUi()
        if (permissionGranted) {
            healthStatusView.text = "Acesso aos passos autorizado no Health Connect."
            appendLog("Permissão READ_STEPS concedida no Health Connect.")
            readTodaySteps()
        } else {
            stepsView.text = "Passos hoje: sem acesso"
            healthStatusView.text = "Acesso aos passos não concedido. A atualização não repetirá o pedido; use as configurações de acesso."
            appendLog("Acesso aos passos negado; estado persistido para recuperação pelas configurações.")
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
    private val automaticGattDiscoveryPrefs by lazy {
        getSharedPreferences("automatic_gatt_discovery", MODE_PRIVATE)
    }
    private val automaticGattDiscoveryEnabled: Boolean
        get() = automaticGattDiscoveryPrefs.getBoolean("enabled", false)
    private var automaticGattProbeStartedForConnection = false
    private val expectedWatchAddress: String? get() = BleDeviceIdentity.normalize(identityPrefs.getString("gt3_max_address", null))
    private var autoConnectAttempted = false
    private var initialDiscoveryPending = true
    private var connectionFromDiscoveryCandidate = false
    private var weakSignalWarningLogged = false
    private var isAutoReconnectScan = false
    private var isActivityResumed = false
    private var isGattConnected = false
    private var reconnectRunnable: Runnable? = null
    private var connectionTimeoutTask: Runnable? = null
    private var serviceDiscoveryTimeoutTask: Runnable? = null
    private val connectionPrefs by lazy { getSharedPreferences("device_connection", MODE_PRIVATE) }
    private val reconnectPolicy by lazy {
        BleReconnectPolicy(connectionPrefs.getBoolean("auto_reconnect_enabled", false))
    }
    private val reconnectCoordinator by lazy { BleReconnectCoordinator(reconnectPolicy) }
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
    private val pendingAutomaticNotifications = mutableSetOf<String>()
    private val automaticNotificationKeys = mutableSetOf<String>()
    private val pendingAutomaticUnsubscribeKeys = mutableSetOf<String>()
    private val queuedAutomaticUnsubscribeKeys = mutableSetOf<String>()
    private val gattOperationQueue = SerialGattOperationQueue()
    private var activeGattOperationKey: String? = null
    private var gattOperationTimeoutTask: Runnable? = null
    private val deepExplorationAttempted = mutableSetOf<String>()
    private val invalidatedGattOperationKeys = mutableSetOf<String>()
    private val invalidatedCccdCharacteristicKeys = mutableSetOf<String>()
    private val nusFrameDecoder = WearfitAbStreamDecoder()
    private val passiveBleObservationTracker = PassiveBleObservationTracker()
    private var clockSyncAttemptedForSession = false
    private var pendingCurrentTimeWriteKey: String? = null
    private var pendingDeepExplorationRequest = false

    private val cccdUuid: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 2401
        private const val SCAN_DURATION_MS = 15_000L
        private const val AUTO_RECONNECT_SCAN_DURATION_MS = 8_000L
        private const val GATT_CONNECT_TIMEOUT_MS = 15_000L
        private const val GATT_SERVICE_DISCOVERY_TIMEOUT_MS = 12_000L
        private const val GATT_OPERATION_TIMEOUT_MS = 8_000L
        private const val AUTO_CONNECT_MIN_RSSI = -90
        private const val MAX_LOG_LINES = 250
        private const val AUTOMATIC_GATT_CAPTURE_SECONDS = 600
        private const val KEY_AUTOMATIC_SESSION_ID = "automatic_capture_session_id"
        private const val KEY_AUTOMATIC_SESSION_OWNED = "automatic_capture_session_owned"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter
        // Migration: remove the legacy Wearfit handoff lock; this app now owns BLE sessions.
        connectionPrefs.edit { remove("wearfit_handoff_active") }
        buildScreen()
        appendLog("Aplicativo iniciado. Diagnóstico BLE local.")
        appendLog(
            "Payloads proprietários permanecem bloqueados. A sincronização de hora só pode ser iniciada manualmente " +
                "pelo CTS padrão 0x1805/0x2A2B quando a característica anunciar WRITE."
        )
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

        deviceTimeSyncView = TextView(this).apply {
            text = "Hora do relógio: aguardando conexão BLE"
            textSize = 13f
            setTextColor(0xFF526170.toInt())
            setPadding(0, 0, 0, dp(12))
        }
        connectionCard.addView(deviceTimeSyncView)
        clockSyncButton = makeButton("Sincronizar hora do relógio") {
            val activeGatt = bluetoothGatt
            if (activeGatt == null || !isGattConnected || isGattDisconnecting) {
                deviceTimeSyncView.text = "Sincronização indisponível: conecte o relógio primeiro."
            } else {
                syncClockUsingCurrentTimeService(activeGatt)
            }
        }.apply { isEnabled = false }
        connectionCard.addView(clockSyncButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { bottomMargin = dp(10) })
        addText(connectionCard, "A hora só é enviada após toque manual e somente pelo serviço Bluetooth SIG CTS, se anunciado pelo relógio.", 12f, false)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        scanButton = makeButton("Buscar e conectar automaticamente") { beginAutomaticConnection() }.apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF176B5B.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            isEnabled = true
        }
        disconnectButton = makeButton("Desconectar") { disconnectGatt() }.apply {
            isEnabled = false
        }
        actions.addView(scanButton, LinearLayout.LayoutParams(0, dp(56), 1f))
        actions.addView(disconnectButton, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
            marginStart = dp(8)
        })
        connectionCard.addView(actions)
        root.addView(connectionCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(6)
            bottomMargin = dp(8)
        })

        root.addView(makeButton("Configurar endereço manualmente (opcional)") { configureExpectedWatchAddress() }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { bottomMargin = dp(6) })

        addText(root, "Laboratório e registros", 17f, true)
        root.addView(makeButton("Testes e descobertas BLE") {
            startActivity(Intent(this, FeatureDiscoveryActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { bottomMargin = dp(5) })

        root.addView(makeButton("Biblioteca interna de descobertas") {
            startActivity(Intent(this, DiscoveryLibraryActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { bottomMargin = dp(5) })

        deepScanButton = makeButton("Explorar todos os canais compatíveis") {
            requestDeepGattExploration()
        }.apply {
            isEnabled = false
        }
        root.addView(deepScanButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { bottomMargin = dp(5) })

        automaticGattDiscoveryButton = makeButton(
            if (automaticGattDiscoveryEnabled) {
                "Pausar coleta automática"
            } else {
                "Ativar testes automáticos ao conectar"
            }
        ) {
            toggleAutomaticGattDiscovery()
        }
        root.addView(automaticGattDiscoveryButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ).apply { bottomMargin = dp(4) })
        addText(
            root,
            "Após a ativação, o app testa sozinho o GT3 MAX identificado, lê canais READ e recebe NOTIFY/INDICATE compatíveis. Valores e HEX recebidos ficam no histórico local enquanto a captura estiver ativa.",
            11f
        )

        captureSessionView = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(0xFF174B3B.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedSurface(0xFFE5F2EC.toInt(), 0xFFB7D6C7.toInt())
            contentDescription = "Contador regressivo da captura BLE em andamento"
        }
        root.addView(captureSessionView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })

        gattProgressView = TextView(this).apply {
            text = "Exploração GATT: aguardando início."
            textSize = 12f
            setTextColor(0xFF34495E.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundedSurface(0xFFFFFFFF.toInt(), 0xFFE1E7EF.toInt())
        }
        root.addView(gattProgressView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(5) })
        root.addView(makeButton("Registrar ação realizada no relógio") {
            registerWearableActionMarker()
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { bottomMargin = dp(8) })

        addText(root, "Saúde e integração", 17f, true)
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
        }, LinearLayout.LayoutParams(0, dp(56), 1f))
        healthActions.addView(makeButton("Atualizar atividade") {
            readTodaySteps()
        }, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
            marginStart = dp(8)
        })
        root.addView(healthActions)

        healthSettingsButton = makeButton("Abrir configurações de acesso") {
            openHealthConnectSettings()
        }.apply {
            visibility = View.GONE
        }
        root.addView(healthSettingsButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { topMargin = dp(4) })

        addText(root, "Mais recursos", 17f, true)
        root.addView(makeButton("Notificações do telefone") {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ))
        root.addView(makeButton("Catálogo de funções do relógio") {
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

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val inventoryPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(6), 0, dp(6))
        }
        inventoryPanel.addView(content, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(makeButton("Inventário GATT • mostrar/ocultar") {
            inventoryPanel.visibility = if (inventoryPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))
        root.addView(inventoryPanel)

        val diagnosticPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(6), 0, dp(6))
        }
        diagnosticPanel.addView(makeButton("Exportar diagnóstico TXT") {
            exportDiagnosticReport()
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))
        diagnosticPanel.addView(TextView(this).apply {
            text = "Registro técnico • últimos 250 eventos"
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, dp(4))
        })
        logView = TextView(this).apply {
            textSize = 11f
            maxLines = 8
            setTextColor(0xFF263746.toInt())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedSurface(0xFFFFFFFF.toInt(), 0xFFE1E7EF.toInt())
        }
        diagnosticPanel.addView(logView)
        root.addView(makeButton("Diagnóstico avançado • mostrar/ocultar") {
            diagnosticPanel.visibility = if (diagnosticPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))
        root.addView(diagnosticPanel)

        val pageScroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
        setContentView(pageScroll)
        updateHealthConnectPermissionUi()
        updateGattProgressUi()
    }

    private fun updatePhoneClock() {
        clockView.text = "Hora do celular: " +
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        if (::captureSessionView.isInitialized) {
            val nowMillis = System.currentTimeMillis()
            val session = discoveryLibraryStore.activeSession(nowMillis)
            if (session == null) {
                captureSessionView.visibility = View.GONE
                val automaticSessionId = automaticGattDiscoveryPrefs.getString(KEY_AUTOMATIC_SESSION_ID, null)
                if (automaticGattDiscoveryEnabled &&
                    isGattConnected &&
                    automaticGattProbeStartedForConnection &&
                    automaticSessionId != null
                ) {
                    automaticGattDiscoveryPrefs.edit {
                        remove(KEY_AUTOMATIC_SESSION_ID)
                        remove(KEY_AUTOMATIC_SESSION_OWNED)
                    }
                    if (ensureAutomaticCaptureSession()) {
                        appendLog("Janela automática de captura renovada; coleta continua até pausar ou desconectar.")
                    }
                }
            } else {
                val remaining = session.remainingSeconds(nowMillis)
                val time = String.format(Locale.ROOT, "%02d:%02d", remaining / 60, remaining % 60)
                val captured = discoveryLibraryStore.recordsForSession(session.id)
                captureSessionView.text =
                    "CAPTURA BLE ATIVA • " + time + " restantes • " + captured + " registros"
                captureSessionView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateGattProgressUi() {
        if (!::gattProgressView.isInitialized) return
        val snapshot = gattExplorationProgress.snapshot()
        val details = snapshot.channelSummary()
        gattProgressView.text = snapshot.summary() +
            if (details.isBlank()) "" else "\n" + details
    }

    private fun registerWearableActionMarker() {
        val actions = arrayOf(
            "Abri a tela de frequência cardíaca",
            "Iniciei medição de SpO₂",
            "Iniciei medição de pressão",
            "Iniciei medição de temperatura",
            "Iniciei um treino",
            "Abri a tela de sono/histórico",
            "Outra ação"
        )
        AlertDialog.Builder(this)
            .setTitle("Marcar ação no relógio")
            .setMessage("Registre o momento da ação feita fisicamente. O horário será usado para comparar com os eventos BLE recebidos.")
            .setItems(actions) { _, which ->
                if (which == actions.lastIndex) {
                    val input = EditText(this).apply {
                        hint = "Descreva a ação"
                        setSingleLine(true)
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Outra ação do relógio")
                        .setView(input)
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Registrar") { _, _ ->
                            val value = input.text.toString().trim().take(120)
                            if (value.isNotBlank()) persistWearableActionMarker(value)
                        }
                        .show()
                } else {
                    persistWearableActionMarker(actions[which])
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun persistWearableActionMarker(action: String) {
        val now = System.currentTimeMillis()
        if (!discoveryLibraryStore.flushPendingWrites()) {
            appendLog("Marcador temporal não registrado: a fila de gravação BLE não foi concluída.")
            return
        }
        val saved = discoveryLibraryStore.recordDiscovery(
            title = "Ação manual no relógio • " + action,
            detail = "Marcador temporal informado pelo usuário para correlação com leituras e notificações BLE. Não representa uma medição confirmada.",
            nowMillis = now,
            source = "USER_WEARABLE_ACTION",
            confidence = "MARCADOR_TEMPORAL_MANUAL"
        )
        if (saved) {
            appendLog("Marcador temporal registrado: " + action + " • " +
                SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now)))
        } else {
            appendLog("Marcador temporal não foi salvo: biblioteca indisponível, corrompida ou com schema incompatível.")
        }
    }

    private fun connectHealthConnect() {
        if (healthPermissionRequestPending) return

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
                val permissionGranted = healthConnectRepository.hasReadPermission()
                healthPermissionState = HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                    permissionGranted,
                    healthPermissionState
                )
                if (permissionGranted) {
                    healthConnectInitialized = true
                    updateHealthConnectPermissionUi()
                    readTodaySteps()
                } else {
                    healthConnectInitialized = false
                    stepsView.text = "Passos hoje: sem acesso"
                    updateHealthConnectPermissionUi()
                    if (HealthConnectPermissionStatePolicy.shouldRequestPermission(healthPermissionState)) {
                        launchHealthConnectPermissionRequest()
                    } else {
                        showHealthConnectPermissionRecoveryDialog()
                    }
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

    private fun launchHealthConnectPermissionRequest() {
        if (healthPermissionRequestPending) return
        if (healthConnectRepository.availability() != HealthConnectAvailability.AVAILABLE) {
            healthStatusView.text = "Conexão Saúde indisponível ou precisa de atualização."
            appendLog("Pedido de permissão não iniciado: provider indisponível.")
            return
        }
        healthPermissionRequestPending = true
        try {
            healthPermissionLauncher.launch(healthConnectRepository.readPermissions)
        } catch (error: Exception) {
            healthPermissionRequestPending = false
            healthStatusView.text = "Não foi possível abrir as permissões da Conexão Saúde."
            appendLog("Falha ao abrir pedido READ_STEPS: " + (error.message ?: "desconhecido"))
        }
    }

    private fun showHealthConnectPermissionRecoveryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Acesso aos passos não concedido")
            .setMessage(
                "A permissão READ_STEPS foi recusada ou removida anteriormente. " +
                    "Para evitar pedidos repetidos, o botão Atualizar atividade não solicitará acesso. " +
                    "Abra as configurações da Conexão Saúde para alterar a autorização."
            )
            .setPositiveButton("Abrir configurações") { _, _ -> openHealthConnectSettings() }
            .setNeutralButton("Tentar novamente") { _, _ -> launchHealthConnectPermissionRequest() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun openHealthConnectSettings() {
        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        } else {
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        }

        try {
            startActivity(settingsIntent)
        } catch (error: ActivityNotFoundException) {
            val providerSettingsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:com.google.android.apps.healthdata")
            )
            try {
                startActivity(providerSettingsIntent)
            } catch (fallbackError: ActivityNotFoundException) {
                healthStatusView.text = "Não foi possível abrir as configurações da Conexão Saúde neste aparelho."
                appendLog(
                    "Falha ao abrir configurações do Health Connect: " +
                        (fallbackError.message ?: error.message ?: "atividade indisponível")
                )
            }
        }
    }

    private fun updateHealthConnectPermissionUi() {
        val state = healthPermissionState
        healthSettingsButton.visibility = if (
            HealthConnectPermissionStatePolicy.shouldOfferSettings(state)
        ) View.VISIBLE else View.GONE

        when (state) {
            HealthConnectPermissionState.DENIED ->
                healthStatusView.text = "Acesso aos passos não concedido. Abra as configurações para autorizar."
            HealthConnectPermissionState.REVOKED ->
                healthStatusView.text = "Acesso aos passos removido. Abra as configurações para restaurar."
            HealthConnectPermissionState.NOT_REQUESTED,
            HealthConnectPermissionState.GRANTED -> Unit
        }
    }

    private fun refreshHealthConnectPermissionState() {
        if (healthPermissionRequestPending) return

        when (healthConnectRepository.availability()) {
            HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                healthConnectInitialized = false
                healthStatusView.text = "Conexão Saúde indisponível. Verifique se está instalada/ativada."
                return
            }
            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                healthConnectInitialized = false
                healthStatusView.text = "Atualize a Conexão Saúde na Play Store."
                return
            }
            HealthConnectAvailability.AVAILABLE -> Unit
        }

        healthScope.launch {
            try {
                val permissionGranted = healthConnectRepository.hasReadPermission()
                healthPermissionState = HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                    permissionGranted,
                    healthPermissionState
                )
                healthConnectInitialized = permissionGranted
                updateHealthConnectPermissionUi()
                if (permissionGranted) {
                    readTodaySteps()
                } else {
                    stepsView.text = "Passos hoje: sem acesso"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                healthConnectInitialized = false
                healthStatusView.text = "Não foi possível verificar o acesso à Conexão Saúde."
                appendLog("Falha ao atualizar estado READ_STEPS: " + (error.message ?: "desconhecido"))
            }
        }
    }

    private fun readTodaySteps() {
        if (!healthConnectInitialized) {
            stepsView.text = if (
                HealthConnectPermissionStatePolicy.shouldOfferSettings(healthPermissionState)
            ) "Passos hoje: sem acesso" else "Passos hoje: aguardando conexão"
            updateHealthConnectPermissionUi()
            return
        }
        healthScope.launch {
            when (val result = healthConnectRepository.readTodaySteps()) {
                is HealthConnectStepsResult.Success -> {
                    val count = result.snapshot.totalSteps
                    stepsView.text = StepsCountFormatter.label(count)
                    if (count == null) {
                        healthStatusView.text = "Health Connect não retornou registros de passos para hoje."
                        if (!missingStepsWasLogged) {
                            appendLog("Health Connect sem registros de passos hoje; valor mantido como indisponível, não como zero.")
                            missingStepsWasLogged = true
                        }
                        lastLoggedStepsTotal = null
                    } else {
                        healthStatusView.text =
                            "Dados agregados do Health Connect • atualizado " +
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        if (lastLoggedStepsTotal != count) {
                            appendLog("Total agregado de passos de hoje no Health Connect: " + count)
                            lastLoggedStepsTotal = count
                        }
                        missingStepsWasLogged = false
                    }
                }
                HealthConnectStepsResult.PermissionRequired -> {
                    healthConnectInitialized = false
                    healthPermissionState = HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                        false,
                        healthPermissionState
                    )
                    stepsView.text = "Passos hoje: sem acesso"
                    updateHealthConnectPermissionUi()
                    appendLog("Permissão READ_STEPS ausente ou revogada durante a leitura; recuperação manual disponível.")
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
        updateAutomaticGattDiscoveryButton()
        val explorationRequests = getSharedPreferences("ble_exploration_requests", MODE_PRIVATE)
        pendingDeepExplorationRequest = explorationRequests.getBoolean("pending_deep_exploration", false)
        if (pendingDeepExplorationRequest) {
            explorationRequests.edit { remove("pending_deep_exploration") }
        }
        handler.removeCallbacks(clockTicker)
        handler.post(clockTicker)
        handler.removeCallbacks(stepsTicker)
        if (healthConnectInitialized) readTodaySteps() else refreshHealthConnectPermissionState()
        handler.postDelayed(stepsTicker, 60_000L)
        if (pendingDeepExplorationRequest && isGattConnected) {
            pendingDeepExplorationRequest = false
            appendLog("Solicitação de exploração recebida; abrindo confirmação de segurança.")
            handler.post { requestDeepGattExploration() }
        }
        if (reconnectPolicy.isEnabled && bluetoothGatt == null && currentScanCallback == null) {
            initialDiscoveryPending = false
            scheduleAutoReconnect("Aplicativo retomado", immediate = true)
        } else if (initialDiscoveryPending && bluetoothGatt == null && currentScanCallback == null) {
            initialDiscoveryPending = false
            reconnectPolicy.markConnectionRequested()
            connectionPrefs.edit { putBoolean("auto_reconnect_enabled", true) }
            appendLog("Busca BLE geral iniciada automaticamente ao abrir o aplicativo; o Relógio 2.0 mantém o controle da conexão.")
            startBleScan()
        }
    }

    override fun onPause() {
        isActivityResumed = false
        reconnectRunnable?.let(handler::removeCallbacks)
        reconnectRunnable = null
        if (currentScanCallback != null) stopBleScan()
        handler.removeCallbacks(clockTicker)
        handler.removeCallbacks(stepsTicker)
        super.onPause()
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dp(56)
            maxLines = 2
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(5), dp(8), dp(5))
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
        reconnectPolicy.markConnectionRequested()
        connectionPrefs.edit { putBoolean("auto_reconnect_enabled", true) }
        appendLog("Controle BLE assumido pelo Relógio 2.0; reconexão automática habilitada após solicitação do usuário.")
        cancelReconnectTask()
        startBleScan()
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
            .setMessage("Opcional. O aplicativo procura dispositivos BLE automaticamente e valida o perfil GATT do GT3 MAX. Informe um endereço somente se quiser fixar manualmente a identidade; ele fica salvo neste aparelho.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Remover") { _, _ ->
                disconnectGatt()
                identityPrefs.edit { remove("gt3_max_address") }
                appendLog("Identidade GT3 MAX removida; reconexão automática desativada.")
            }
            .setPositiveButton("Salvar") { _, _ ->
                val address = BleDeviceIdentity.normalize(input.text.toString())
                if (address == null) updateStatus("Endereço inválido. Use AA:BB:CC:DD:EE:FF.")
                else {
                    if (expectedWatchAddress != null && expectedWatchAddress != address) {
                        disconnectGatt()
                        appendLog("Identidade do relógio alterada; uma nova conexão manual será necessária para habilitar a reconexão automática.")
                    }
                    identityPrefs.edit { putString("gt3_max_address", address) }
                    appendLog("Identidade GT3 MAX salva localmente: " + maskAddress(address))
                    afterSave?.invoke()
                }
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan(automatic: Boolean = false) {
        if (automatic && !isActivityResumed) {
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
            if (automatic || reconnectPolicy.isEnabled) scheduleAutoReconnect("Adaptador Bluetooth indisponível")
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
        updateStatus(if (automatic) "Procurando GT3 MAX e candidatos BLE próximos..." else "Procurando dispositivos BLE próximos por 15 segundos...")
        appendLog(if (automatic) "Início da busca BLE geral para localizar o GT3 MAX; endereço salvo será usado como pista, não como único filtro." else "Início da busca BLE geral por 15 segundos; dispositivos sem nome também serão listados.")

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
                    val previous = scanResults[address]
                    scanResults[address] = result
                    val candidateMatch = scanCandidateMatch(result, address, name)
                    if (previous == null) {
                        appendLog("Sinal BLE encontrado: " + name + " | RSSI=" + result.rssi +
                            " dBm | endereço=" + maskAddress(address) + " | classificação=" + candidateMatch.name)
                    } else if (kotlin.math.abs(previous.rssi - result.rssi) >= 8) {
                        appendLog("RSSI atualizado: " + name + " | " + result.rssi + " dBm")
                    }
                    if (!autoConnectAttempted && candidateMatch != BleScanCandidatePolicy.Match.OTHER) {
                        if (result.rssi <= AUTO_CONNECT_MIN_RSSI) {
                            if (!weakSignalWarningLogged) {
                                weakSignalWarningLogged = true
                                appendLog("Candidato GT3 MAX encontrado, mas RSSI fraco: " + result.rssi + " dBm. A conexão automática aguardará sinal melhor.")
                                updateStatus("GT3 MAX candidato encontrado, mas sinal muito fraco.")
                            }
                        } else {
                            autoConnectAttempted = true
                            appendLog("Candidato BLE compatível (" + candidateMatch.name + "); iniciando conexão para validar fingerprint GATT antes de explorar serviços.")
                            stopBleScan()
                            connectToDevice(
                                result.device,
                                automaticAttempt = automatic,
                                discoveryCandidate = candidateMatch != BleScanCandidatePolicy.Match.SAVED_ADDRESS
                            )
                            return@runOnUiThread
                        }
                    }
                    renderScanResults()
                    updateStatus("Busca em andamento. Dispositivos BLE encontrados: " + scanResults.size)
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
                val foundCandidate = scanResults.values.any { result ->
                    val address = safeDeviceAddress(result.device)
                    address != null && scanCandidateMatch(
                        result, address, result.scanRecord?.deviceName ?: safeDeviceName(result.device)
                    ) != BleScanCandidatePolicy.Match.OTHER
                }
                updateStatus(if (foundCandidate) "Candidato GT3 MAX encontrado, aguardando sinal adequado."
                    else "Busca concluída: " + scanResults.size + " dispositivos BLE observados; GT3 MAX ainda não identificado.")
                appendLog("Busca BLE concluída. Total observado=" + scanResults.size +
                    "; candidato GT3 MAX encontrado=" + foundCandidate)
                renderScanResults()
                if ((automatic || reconnectPolicy.isEnabled) && !foundCandidate) {
                    scheduleAutoReconnect("GT3 MAX não localizado na busca geral")
                } else if (automatic && foundCandidate) {
                    scheduleAutoReconnect("Sinal do candidato GT3 MAX insuficiente nesta tentativa")
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

    private fun scanCandidateMatch(
        result: ScanResult,
        address: String,
        observedName: String?
    ): BleScanCandidatePolicy.Match {
        val advertisedServices = result.scanRecord?.serviceUuids
            ?.map { it.uuid.toString() }
            ?.toSet()
            .orEmpty()
        return BleScanCandidatePolicy.evaluate(
            savedAddress = expectedWatchAddress,
            observedAddress = address,
            advertisedName = observedName,
            advertisedServiceUuids = advertisedServices
        )
    }

    private fun renderScanResults() {
        content.removeAllViews()
        addText(content, "Dispositivos BLE encontrados • toque no GT3 MAX ou em um dispositivo para validar o perfil GATT", 18f, true)

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
                        val savedIdentity = BleDeviceIdentity.matches(expectedWatchAddress, address)
                        connectToDevice(result.device, discoveryCandidate = !savedIdentity)
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
    private fun connectToDevice(
        device: BluetoothDevice,
        automaticAttempt: Boolean = false,
        discoveryCandidate: Boolean = false
    ) {
        val observedAddress = safeDeviceAddress(device)
        val savedIdentityMatches = BleDeviceIdentity.matches(expectedWatchAddress, observedAddress)
        if (!savedIdentityMatches && !discoveryCandidate) {
            updateStatus("Dispositivo não identificado. Conexão bloqueada.")
            appendLog("Conexão bloqueada: dispositivo não corresponde à identidade salva nem foi selecionado para validação GATT.")
            return
        }
        connectionFromDiscoveryCandidate = discoveryCandidate && !savedIdentityMatches
        automaticGattProbeStartedForConnection = false
        gattExplorationProgress.begin(emptyList())
        updateGattProgressUi()
        if (!hasBlePermissions()) {
            if (automaticAttempt) {
                updateStatus("Permissão Bluetooth necessária para reconectar.")
                appendLog("Tentativa automática interrompida: permissão Bluetooth foi revogada; nenhuma caixa de permissão foi aberta.")
            } else {
                requestBlePermissions()
            }
            return
        }
        isGattDisconnecting = false
        isGattConnected = false
        invalidatedGattOperationKeys.clear()
        invalidatedCccdCharacteristicKeys.clear()
        clockSyncAttemptedForSession = false
        pendingCurrentTimeWriteKey = null
        if (::deviceTimeSyncView.isInitialized) {
            deviceTimeSyncView.text = "Hora do relógio: aguardando descoberta dos serviços GATT"
        }
        cancelConnectionTimeout()
        cancelGattServiceDiscoveryTimeout()
        lastConnectedDeviceName = safeDeviceName(device) ?: "Dispositivo sem nome"
        lastConnectedDeviceAddress = safeDeviceAddress(device)
        // A new BluetoothGatt instance is a new CTS attempt scope, even if the old
        // connection ended without delivering its final disconnect callback.
        clockSyncAttemptedForSession = false
        pendingCurrentTimeWriteKey = null
        if (::clockSyncButton.isInitialized) clockSyncButton.isEnabled = false
        resetGattOperationQueue()
        pendingNotifications.clear()
        activeNotifications.clear()
        pendingAutomaticNotifications.clear()
        automaticNotificationKeys.clear()
        pendingAutomaticUnsubscribeKeys.clear()
        queuedAutomaticUnsubscribeKeys.clear()
        deepExplorationAttempted.clear()
        nusFrameDecoder.reset()
        passiveBleObservationTracker.resetSession()
        BlePacketFingerprintAnalyzer.resetSession()
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
            if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Permissão Bluetooth revogada durante conexão")
            return
        } catch (error: IllegalArgumentException) {
            updateStatus("Dispositivo BLE inválido ou indisponível.")
            appendLog("Não foi possível iniciar GATT: " + (error.message ?: "argumento inválido"))
            if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Dispositivo BLE indisponível")
            return
        }
        if (newGatt == null) {
            updateStatus("O Android não iniciou a conexão GATT.")
            appendLog("connectGatt retornou null.")
            if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Android não iniciou a conexão GATT")
            return
        }
        bluetoothGatt = newGatt
        val timeout = Runnable {
            if (bluetoothGatt === newGatt && !isGattConnected) {
                val connectionWasCancelled = isGattDisconnecting
                bluetoothGatt = null
                isGattDisconnecting = false
                gattExplorationProgress.markRemainingSkipped()
                updateGattProgressUi()
                resetGattOperationQueue()
                pendingNotifications.clear()
                activeNotifications.clear()
                pendingAutomaticNotifications.clear()
                automaticNotificationKeys.clear()
                pendingAutomaticUnsubscribeKeys.clear()
                queuedAutomaticUnsubscribeKeys.clear()
                runCatching { newGatt.disconnect() }
                runCatching { newGatt.close() }
                updateStatus(
                    if (connectionWasCancelled) {
                        "Nenhuma conexão ativa."
                    } else {
                        "Falha na conexão GATT: tempo limite aguardando conexão."
                    }
                )
                appendLog("Tempo limite de conexão GATT após ${GATT_CONNECT_TIMEOUT_MS} ms.")
                if (!connectionWasCancelled &&
                    (automaticAttempt || reconnectPolicy.isEnabled)
                ) scheduleAutoReconnect("Tempo limite de conexão GATT")
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
            if (isGattDisconnecting && newState == BluetoothProfile.STATE_CONNECTED) {
                appendLog("Conexão GATT concluída após cancelamento manual; encerrando sem reativar a reconexão automática.")
                runCatching { gatt.disconnect() }
                return
            }
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTING) {
                updateStatus("Conectando ao dispositivo BLE...")
            } else if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED) {
                isGattDisconnecting = false
                isGattConnected = true
                cancelConnectionTimeout()
                cancelReconnectTask()
                reconnectPolicy.markConnected()
                connectionPrefs.edit { putBoolean("auto_reconnect_enabled", true) }
                appendLog("Reconexão automática habilitada após conexão GATT confirmada.")
                updateStatus("Conectado. Descobrindo serviços GATT...")
                val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
                appendLog("Descoberta de serviços solicitada: " + started)
                if (started) {
                    discoveryLibraryStore.recordDiscoveryAsync(
                        title = "Descoberta GATT solicitada",
                        detail = "A API Android aceitou discoverServices(); aguardando callback onServicesDiscovered. Inventário anterior não representa esta conexão.",
                        nowMillis = System.currentTimeMillis(),
                        source = "GATT_LIFECYCLE",
                        confidence = "AGUARDANDO_CALLBACK"
                    )
                    scheduleGattServiceDiscoveryTimeout(gatt)
                } else {
                    failGattServiceDiscovery(gatt, "Android recusou iniciar discoverServices().")
                }
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                isGattDisconnecting = false
                isGattConnected = false
                cancelGattServiceDiscoveryTimeout()
                gattExplorationProgress.markRemainingSkipped()
                updateGattProgressUi()
                cancelConnectionTimeout()
                resetGattOperationQueue()
                pendingNotifications.clear()
                activeNotifications.clear()
                pendingAutomaticNotifications.clear()
                automaticNotificationKeys.clear()
                pendingAutomaticUnsubscribeKeys.clear()
                queuedAutomaticUnsubscribeKeys.clear()
                pendingCurrentTimeWriteKey = null
                clockSyncAttemptedForSession = false
                deviceTimeSyncView.text = "Hora do relógio: sessão GATT encerrada antes da confirmação."
                if (bluetoothGatt === gatt) bluetoothGatt = null
                updateStatus("Falha na conexão GATT. Status: " + status)
                appendLog("Conexão GATT falhou; estado e fila foram limpos.")
                finishAutomaticCaptureSession("Conexão GATT falhou")
                automaticGattProbeStartedForConnection = false
                runCatching { gatt.close() }
                if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Falha na conexão GATT: " + status)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isGattDisconnecting = false
                isGattConnected = false
                cancelGattServiceDiscoveryTimeout()
                gattExplorationProgress.markRemainingSkipped()
                updateGattProgressUi()
                cancelConnectionTimeout()
                deviceBatteryView.text = "Bateria do relógio: sem sessão BLE ativa"
                pendingCurrentTimeWriteKey = null
                clockSyncAttemptedForSession = false
                deviceTimeSyncView.text = "Hora do relógio: sem sessão BLE ativa"
                if (::clockSyncButton.isInitialized) clockSyncButton.isEnabled = false
                updateStatus("Relógio desconectado.")
                appendLog("Conexão GATT encerrada.")
                finishAutomaticCaptureSession("Conexão BLE encerrada")
                automaticGattProbeStartedForConnection = false
                if (bluetoothGatt === gatt) {
                    resetGattOperationQueue()
                    pendingNotifications.clear()
                    activeNotifications.clear()
                    pendingAutomaticNotifications.clear()
                    automaticNotificationKeys.clear()
                    pendingAutomaticUnsubscribeKeys.clear()
                    queuedAutomaticUnsubscribeKeys.clear()
                    bluetoothGatt = null
                }
                runCatching { gatt.close() }
                if (reconnectPolicy.isEnabled) {
                    scheduleAutoReconnect("Conexão GATT perdida")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServiceChanged(gatt: BluetoothGatt) {
            if (!GattCallbackSafetyPolicy.shouldRediscoverServices(
                    connectionIsCurrent = bluetoothGatt === gatt,
                    connected = isGattConnected,
                    disconnecting = isGattDisconnecting,
                )
            ) {
                return
            }
            appendLog("Android informou Service Changed: tabela GATT desatualizada. Limpando operações e redescobrindo serviços.")
            activeGattOperationKey?.let { activeKey ->
                invalidatedGattOperationKeys.add(activeKey)
                val parts = activeKey.split("/", limit = 3)
                if (parts.size == 3 && parts[2].startsWith("CCCD_")) {
                    invalidatedCccdCharacteristicKeys.add(parts[0] + "/" + parts[1])
                }
            }
            cancelGattServiceDiscoveryTimeout()
            resetGattOperationQueue()
            gattExplorationProgress.markRemainingSkipped()
            updateGattProgressUi()
            pendingNotifications.clear()
            activeNotifications.clear()
            pendingAutomaticNotifications.clear()
            automaticNotificationKeys.clear()
            pendingAutomaticUnsubscribeKeys.clear()
            queuedAutomaticUnsubscribeKeys.clear()
            pendingCurrentTimeWriteKey = null
            clockSyncAttemptedForSession = false
            automaticGattProbeStartedForConnection = false
            nusFrameDecoder.reset()
            passiveBleObservationTracker.resetSession()
            gattExplorationProgress.begin(emptyList())
            updateGattProgressUi()

            val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
            if (started) {
                discoveryLibraryStore.recordDiscoveryAsync(
                    title = "Tabela de serviços GATT alterada",
                    detail = "onServiceChanged recebido. O Android aceitou uma nova descoberta; a tabela anterior não será reutilizada.",
                    nowMillis = System.currentTimeMillis(),
                    source = "GATT_LIFECYCLE",
                    confidence = "REDESCOBERTA_SOLICITADA"
                )
                updateStatus("Tabela GATT alterada. Atualizando serviços do relógio...")
                scheduleGattServiceDiscoveryTimeout(gatt)
            } else {
                failGattServiceDiscovery(gatt, "Android recusou redescobrir serviços após Service Changed.")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (bluetoothGatt !== gatt || isGattDisconnecting) return
            cancelGattServiceDiscoveryTimeout()
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendLog("Serviços descobertos: " + gatt.services.size)
                discoveryLibraryStore.recordDiscoveryAsync(
                    title = "Callback de descoberta GATT recebido",
                    detail = "onServicesDiscovered concluído com sucesso • " + gatt.services.size + " serviços nesta conexão.",
                    nowMillis = System.currentTimeMillis(),
                    source = "GATT_LIFECYCLE",
                    confidence = "CALLBACK_OK"
                )
                if (!verifyDiscoveryCandidateBeforeExploration(gatt)) return
                captureFeatureDiscovery(gatt)
                renderGattServices(gatt)
                runSafeGattExploration(gatt)
                inspectGt3MaxProfileAndSubscribe(gatt)
                maybeStartAutomaticGattProbe(gatt)
                updateClockSyncAvailability(gatt)
                updateStatus("Conectado. Serviços encontrados: " + gatt.services.size)
                if (pendingDeepExplorationRequest) {
                    pendingDeepExplorationRequest = false
                    appendLog("Solicitação de exploração recebida pela tela Testes e descobertas.")
                    runOnUiThread { requestDeepGattExploration() }
                }
            } else {
                failGattServiceDiscovery(gatt, "Callback onServicesDiscovered retornou status " + status + ".")
            }
        }

        @Deprecated("Deprecated by Android API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val key = gattOperationKey(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), "READ"
            )
            if (!acceptsGattOperationCallback(gatt, key)) return
            val value = characteristic.value ?: byteArrayOf()
            logSafeGattRead(characteristic, value, status)
            completeGattOperation(gatt, key)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val key = gattOperationKey(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), "READ"
            )
            if (!acceptsGattOperationCallback(gatt, key)) return
            logSafeGattRead(characteristic, value, status)
            completeGattOperation(gatt, key)
        }

        @Deprecated("Deprecated by Android API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (!GattCallbackSafetyPolicy.acceptsNotificationCallback(
                    connectionIsCurrent = bluetoothGatt === gatt,
                    connected = isGattConnected,
                    disconnecting = isGattDisconnecting
                )
            ) return
            handleNotification(characteristic.service.uuid.toString(), characteristic.uuid.toString(), characteristic.value ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (!GattCallbackSafetyPolicy.acceptsNotificationCallback(
                    connectionIsCurrent = bluetoothGatt === gatt,
                    connected = isGattConnected,
                    disconnecting = isGattDisconnecting
                )
            ) return
            handleNotification(characteristic.service.uuid.toString(), characteristic.uuid.toString(), value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val key = characteristic.service.uuid.toString() + "/" + characteristic.uuid.toString()
            val operationKey = gattOperationKey(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), "WRITE"
            )
            if (!acceptsGattOperationCallback(gatt, operationKey) || pendingCurrentTimeWriteKey != key) return

            pendingCurrentTimeWriteKey = null
            if (status == BluetoothGatt.GATT_SUCCESS) {
                deviceTimeSyncView.text =
                    "Hora enviada pelo CTS; o GATT confirmou a escrita. O visor/RTC do relógio ainda não foi verificado."
                appendLog(
                    "Bluetooth CTS 0x2A2B: callback GATT confirmou a escrita da hora local do Android. " +
                        "A aceitação GATT não comprova que o firmware atualizou o RTC ou o mostrador."
                )
            } else {
                deviceTimeSyncView.text =
                    "Falha ao gravar a hora pelo CTS (status " + status + ")."
                appendLog("Bluetooth CTS 0x2A2B: escrita GATT falhou. Status=" + status)
            }
            completeGattOperation(gatt, operationKey)
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val characteristic = descriptor.characteristic
            val key = characteristic.service.uuid.toString() + "/" +
                characteristic.uuid.toString()
            val enableOperationKey = gattOperationKey(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), "CCCD_ENABLE"
            )
            val disableOperationKey = gattOperationKey(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), "CCCD_DISABLE"
            )
            val operationKey = GattCallbackSafetyPolicy.descriptorWriteOperationKey(
                activeOperationKey = activeGattOperationKey,
                enableOperationKey = enableOperationKey,
                disableOperationKey = disableOperationKey
            ) ?: run {
                appendLog("Callback CCCD sem operação de habilitar/desabilitar ativa: " + key)
                return
            }
            if (!acceptsGattOperationCallback(gatt, operationKey)) return
            val wasDisablingAutomatic =
                operationKey == disableOperationKey && pendingAutomaticUnsubscribeKeys.remove(key)
            if (wasDisablingAutomatic) {
                pendingAutomaticNotifications.remove(key)
                automaticNotificationKeys.remove(key)
                activeNotifications.remove(key)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    appendLog("Inscrição automática desativada via CCCD para " + characteristic.uuid)
                } else {
                    appendLog(
                        "O relógio não confirmou o cancelamento do CCCD de " +
                            characteristic.uuid + " | status=" + status +
                            ". A inscrição local já foi desativada."
                    )
                }
            } else {
                val wasPending = pendingNotifications.remove(key)
                if (wasPending) {
                    gattExplorationProgress.markSubscriptionResult(
                        characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                        status == BluetoothGatt.GATT_SUCCESS
                    )
                    updateGattProgressUi()
                }
                if (wasPending && status == BluetoothGatt.GATT_SUCCESS) {
                    activeNotifications.add(key)
                    if (pendingAutomaticNotifications.remove(key)) {
                        automaticNotificationKeys.add(key)
                    }
                    appendLog("Notificação/indicação confirmada pelo callback GATT para " + characteristic.uuid)
                    discoveryLibraryStore.recordDiscoveryAsync(
                        title = "Inscrição GATT confirmada • " + characteristic.uuid,
                        detail = "Callback Android confirmou o descritor CCCD 0x2902. O app passa a aguardar dados emitidos pelo relógio.",
                        nowMillis = System.currentTimeMillis(),
                        source = "GATT_CCCD",
                        confidence = "SUBSCRICAO_CONFIRMADA"
                    )
                } else if (wasPending) {
                    pendingAutomaticNotifications.remove(key)
                    activeNotifications.remove(key)
                    discoveryLibraryStore.recordDiscoveryAsync(
                        title = "Inscrição GATT falhou • " + characteristic.uuid,
                        detail = "O callback do descritor CCCD retornou status " + status + ".",
                        nowMillis = System.currentTimeMillis(),
                        source = "GATT_CCCD",
                        confidence = "SUBSCRICAO_FALHOU"
                    )
                    if (hasBlePermissions()) {
                        runCatching { gatt.setCharacteristicNotification(characteristic, false) }
                    } else {
                        appendLog("Permissão Bluetooth revogada; não foi possível desativar a inscrição local.")
                    }
                    appendLog("Falha na confirmação do CCCD para " + characteristic.uuid + ". Status: " + status)
                } else {
                    appendLog("Callback de descritor sem inscrição pendente: " + descriptor.uuid)
                }
            }
            completeGattOperation(gatt, operationKey)
        }
    }

    @SuppressLint("MissingPermission")
    private fun verifyDiscoveryCandidateBeforeExploration(gatt: BluetoothGatt): Boolean {
        if (!connectionFromDiscoveryCandidate) return true
        val observed = gatt.services.associate { service ->
            service.uuid to service.characteristics.map { it.uuid }.toSet()
        }
        val match = Gt3MaxCallDeviceProfile.evaluate(lastConnectedDeviceName, observed)
        if (match != Gt3MaxCallDeviceProfile.Match.CAPTURED_GATT_FINGERPRINT) {
            appendLog("Dispositivo candidato rejeitado antes da exploração: fingerprint GATT não corresponde ao GT3 MAX Call (" +
                match.name + "). Nenhuma leitura de sensor ou inscrição foi iniciada.")
            connectionFromDiscoveryCandidate = false
            isGattDisconnecting = true
            isGattConnected = false
            updateStatus("Dispositivo BLE não corresponde ao GT3 MAX. Conexão encerrada.")
            runCatching { gatt.disconnect() }
            return false
        }
        val address = safeDeviceAddress(gatt.device)
        if (address != null) {
            identityPrefs.edit { putString("gt3_max_address", address) }
            appendLog("Identidade GT3 MAX salva automaticamente após fingerprint GATT completo: " + maskAddress(address))
        } else {
            appendLog("Fingerprint GATT completo observado, mas o endereço não pôde ser salvo por falta de permissão.")
        }
        connectionFromDiscoveryCandidate = false
        appendLog("GT3 MAX Call validado por compatibilidade do fingerprint GATT; iniciando inventário e exploração permitida.")
        return true
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
    private fun captureFeatureDiscovery(gatt: BluetoothGatt) {
        val observedServices = gatt.services.map { service ->
            GattServiceRecord(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    val properties = linkedSetOf<String>()
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) properties.add("WRITE_NO_RESPONSE")
                    GattCharacteristicRecord(
                        uuid = characteristic.uuid.toString(),
                        properties = properties,
                        descriptorUuids = characteristic.descriptors.map { it.uuid.toString() }.toSet()
                    )
                }
            )
        }
        val saved = featureDiscoveryStore.recordInventory(
            services = observedServices,
            nowMillis = System.currentTimeMillis()
        )
        if (saved) {
            val characteristicCount = observedServices.sumOf { it.characteristics.size }
            val inventoryDetail = observedServices.joinToString("\n") { service ->
                service.uuid + "\n" + service.characteristics.joinToString("\n") { characteristic ->
                    characteristic.uuid + " • " + characteristic.properties.joinToString(",")
                }
            }
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Inventário GATT • " + observedServices.size + " serviços",
                detail = characteristicCount.toString() + " características catalogadas\n" + inventoryDetail,
                nowMillis = System.currentTimeMillis()
            )
            appendLog(
                "Autodescoberta BLE atualizada: " + observedServices.size +
                    " serviços e " + characteristicCount + " características catalogados; " +
                    "nenhuma leitura adicional ou inscrição nova foi iniciada."
            )
        } else {
            appendLog(
                "Autodescoberta BLE não sobrescreveu o catálogo local: conteúdo inválido " +
                    "ou schema mais novo detectado."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun runSafeGattExploration(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt || isGattDisconnecting) return
        val inventory = gatt.services.map { service ->
            GattServiceRecord(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    val properties = linkedSetOf<String>()
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) properties.add("WRITE_NO_RESPONSE")
                    GattCharacteristicRecord(
                        uuid = characteristic.uuid.toString(),
                        properties = properties,
                        descriptorUuids = characteristic.descriptors.map { it.uuid.toString() }.toSet()
                    )
                }
            )
        }
        val plan = SafeGattExplorationPlan.build(inventory)
        appendLog("Exploração GATT segura: " + plan.size + " operações padronizadas elegíveis; canais proprietários, NUS RX e DFU excluídos.")
        plan.forEach { operation ->
            val service = gatt.getService(runCatching { UUID.fromString(operation.serviceUuid) }.getOrNull())
            val characteristic = service?.getCharacteristic(runCatching { UUID.fromString(operation.characteristicUuid) }.getOrNull())
            if (characteristic == null) {
                appendLog("Operação GATT ignorada: característica não está mais disponível: " + operation.characteristicUuid)
                return@forEach
            }
            when (operation.kind) {
                SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE -> {
                    enqueueGattOperation(
                        gattOperationKey(service.uuid.toString(), characteristic.uuid.toString(), "READ"),
                        onTimeout = {
                            appendLog("Leitura padronizada expirou; a fila continuará sem presumir que houve resposta.")
                        }
                    ) {
                        if (bluetoothGatt !== gatt || isGattDisconnecting) false
                        else {
                            val started = gatt.readCharacteristic(characteristic)
                            if (started) appendLog("Leitura GATT padronizada solicitada: " + characteristic.uuid)
                            else appendLog("Android recusou leitura GATT padronizada: " + characteristic.uuid)
                            started
                        }
                    }
                }
                SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT -> {
                    enableNotifications(gatt, characteristic)
                }
                SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE -> {
                    appendLog("Canal proprietário ignorado no fluxo automático; exige exploração manual.")
                }
                SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL -> {
                    appendLog("Inscrição proprietária ignorada no fluxo automático; exige exploração manual.")
                }
            }
        }
    }

    private fun updateAutomaticGattDiscoveryButton() {
        if (!::automaticGattDiscoveryButton.isInitialized) return
        automaticGattDiscoveryButton.text = if (automaticGattDiscoveryEnabled) {
            "Pausar coleta automática"
        } else {
            "Ativar testes automáticos ao conectar"
        }
    }

    private fun toggleAutomaticGattDiscovery() {
        if (automaticGattDiscoveryEnabled) {
            automaticGattDiscoveryPrefs.edit { putBoolean("enabled", false) }
            finishAutomaticCaptureSession("Coleta automática pausada pelo usuário")
            stopAutomaticGattNotifications()
            automaticGattProbeStartedForConnection = false
            appendLog(
                "Coleta automática pausada. Novas leituras/inscrições param; canais ativados pelo teste automático serão desinscritos via CCCD padrão."
            )
            updateAutomaticGattDiscoveryButton()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Ativar testes automáticos do GT3 MAX")
            .setMessage(
                "Depois de ativar, o aplicativo vai iniciar automaticamente quando reconhecer o fingerprint GATT completo do GT3 MAX Call. " +
                    "Ele percorrerá os canais compatíveis que anunciam READ e ativará NOTIFY/INDICATE com CCCD padrão. " +
                    "Leituras e notificações recebidas serão salvas localmente, incluindo HEX que pode conter dados de saúde, em sessões de até 10 minutos. " +
                    "A autorização fica salva neste aparelho até você pausar. A captura usa blocos locais de até 10 minutos, renovados enquanto o app permanece em primeiro plano e conectado. Ao sair da tela, a renovação fica suspensa e retoma ao voltar. Nenhum payload de escrita, comando proprietário, NUS RX, DFU, OTA ou reset será enviado. " +
                    "Ativar uma notificação não força o relógio a iniciar uma medição: o app só recebe dados que o firmware disponibilizar."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Ativar automaticamente") { _, _ ->
                automaticGattDiscoveryPrefs.edit { putBoolean("enabled", true) }
                updateAutomaticGattDiscoveryButton()
                appendLog("Consentimento local para testes GATT automáticos ativado.")
                bluetoothGatt?.let(::maybeStartAutomaticGattProbe)
            }
            .show()
    }

    private fun ensureAutomaticCaptureSession(): Boolean {
        val now = System.currentTimeMillis()
        val active = discoveryLibraryStore.activeSession(now)
        if (active != null) {
            automaticGattDiscoveryPrefs.edit {
                putString(KEY_AUTOMATIC_SESSION_ID, active.id)
                putBoolean(KEY_AUTOMATIC_SESSION_OWNED, false)
            }
            appendLog(
                "Sessão de captura manual existente será reutilizada sem assumir sua propriedade. " +
                    "Ao pausar a coleta automática, o teste manual não será encerrado."
            )
            return true
        }

        automaticGattDiscoveryPrefs.edit { remove(KEY_AUTOMATIC_SESSION_ID) }
        if (!discoveryLibraryStore.startTestSession(AUTOMATIC_GATT_CAPTURE_SECONDS, now)) {
            appendLog(
                "Testes automáticos bloqueados: a biblioteca local não aceitou iniciar a sessão. " +
                    "Conteúdo corrompido ou schema futuro permanece preservado."
            )
            return false
        }

        val created = discoveryLibraryStore.activeSession(now)
        if (created == null) {
            appendLog("Testes automáticos bloqueados: a sessão não foi confirmada no armazenamento local.")
            return false
        }

        automaticGattDiscoveryPrefs.edit {
            putString(KEY_AUTOMATIC_SESSION_ID, created.id)
            putBoolean(KEY_AUTOMATIC_SESSION_OWNED, true)
        }
        discoveryLibraryStore.recordDiscoveryAsync(
            title = "Captura automática GATT iniciada",
            detail = "Fingerprint completo GT3 MAX Call. Sessão local de até 10 minutos; somente READ e CCCD padrão.",
            nowMillis = now,
            source = "AUTO_GATT_PROBE",
            confidence = "CONSENTIMENTO_LOCAL"
        )
        appendLog("Sessão automática de captura iniciada por até 10 minutos.")
        updatePhoneClock()
        return true
    }

    private fun finishAutomaticCaptureSession(reason: String) {
        val ownedSessionId = automaticGattDiscoveryPrefs.getString(KEY_AUTOMATIC_SESSION_ID, null)
            ?: return
        val active = discoveryLibraryStore.activeSession(System.currentTimeMillis())
        val sessionWasCreatedAutomatically = automaticGattDiscoveryPrefs.getBoolean(
            KEY_AUTOMATIC_SESSION_OWNED,
            false
        )
        if (AutomaticCaptureSessionPolicy.shouldFinish(
                sessionMatches = active?.id == ownedSessionId,
                createdByAutomaticMode = sessionWasCreatedAutomatically
            )
        ) {
            discoveryLibraryStore.finishTestSession(System.currentTimeMillis(), reason)
            appendLog("Sessão automática de captura finalizada: " + reason)
        } else if (active?.id == ownedSessionId) {
            appendLog("Sessão manual compartilhada preservada; somente a coleta automática foi desativada.")
        }
        automaticGattDiscoveryPrefs.edit {
            remove(KEY_AUTOMATIC_SESSION_ID)
            remove(KEY_AUTOMATIC_SESSION_OWNED)
        }
        if (::captureSessionView.isInitialized) updatePhoneClock()
    }

    @SuppressLint("MissingPermission")
    private fun maybeStartAutomaticGattProbe(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt || !isGattConnected || isGattDisconnecting) return
        if (!automaticGattDiscoveryEnabled || automaticGattProbeStartedForConnection) return
        if (pendingAutomaticUnsubscribeKeys.isNotEmpty() || queuedAutomaticUnsubscribeKeys.isNotEmpty()) {
            handler.postDelayed({
                if (bluetoothGatt === gatt) maybeStartAutomaticGattProbe(gatt)
            }, 300L)
            return
        }

        val observed = gatt.services.associate { service ->
            service.uuid to service.characteristics.map { it.uuid }.toSet()
        }
        val fingerprintMatches =
            Gt3MaxCallDeviceProfile.evaluate(lastConnectedDeviceName, observed) ==
                Gt3MaxCallDeviceProfile.Match.CAPTURED_GATT_FINGERPRINT
        val shouldStart = AutomaticGattDiscoveryPolicy.shouldStart(
            consentEnabled = automaticGattDiscoveryEnabled,
            fullDeviceFingerprintMatched = fingerprintMatches,
            alreadyStartedForConnection = automaticGattProbeStartedForConnection
        )
        if (!fingerprintMatches) {
            appendLog(
                "Testes automáticos aguardam o fingerprint GATT completo do GT3 MAX Call. " +
                    "Nenhum canal proprietário foi lido/assinado nesta conexão."
            )
            return
        }
        if (!shouldStart || !ensureAutomaticCaptureSession()) return

        automaticGattProbeStartedForConnection = true
        deepExplorationAttempted.clear()
        val inventory = gatt.services.map { service ->
            GattServiceRecord(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    val properties = linkedSetOf<String>()
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) properties.add("WRITE_NO_RESPONSE")
                    GattCharacteristicRecord(
                        uuid = characteristic.uuid.toString(),
                        properties = properties,
                        descriptorUuids = characteristic.descriptors.map { it.uuid.toString() }.toSet()
                    )
                }
            )
        }
        val operationCount = SafeGattExplorationPlan.buildDeep(inventory).size
        discoveryLibraryStore.recordDiscoveryAsync(
            title = "Varredura automática de sensores iniciada",
            detail = operationCount.toString() + " operações GATT elegíveis. Escritas de características, NUS RX e DFU/OTA permanecem bloqueados.",
            nowMillis = System.currentTimeMillis(),
            source = "AUTO_GATT_PROBE",
            confidence = "FINGERPRINT_COMPLETO"
        )
        appendLog(
            "Fingerprint completo confirmado. Iniciando exploração automática de " +
                operationCount + " operações READ/CCCD compatíveis."
        )
        runDeepGattExploration(gatt, automatic = true)
    }

    @SuppressLint("MissingPermission")
    private fun stopAutomaticGattNotifications() {
        val gatt = bluetoothGatt
        if (gatt == null || !isGattConnected || isGattDisconnecting) {
            pendingAutomaticNotifications.clear()
            pendingAutomaticUnsubscribeKeys.clear()
            queuedAutomaticUnsubscribeKeys.clear()
            automaticNotificationKeys.clear()
            return
        }

        // Include pending operations: the queue will serialize a disable after any in-flight CCCD.
        val keys = (automaticNotificationKeys + pendingAutomaticNotifications).toList()
        if (keys.isEmpty()) {
            appendLog("Nenhuma inscrição automática de sensores estava ativa.")
            return
        }

        keys.forEach { key ->
            val parts = key.split("/")
            if (parts.size != 2) return@forEach
            val serviceUuid = runCatching { UUID.fromString(parts[0]) }.getOrNull()
            val characteristicUuid = runCatching { UUID.fromString(parts[1]) }.getOrNull()
            val characteristic = if (serviceUuid != null && characteristicUuid != null) {
                gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            } else {
                null
            }
            if (characteristic == null) {
                pendingAutomaticNotifications.remove(key)
                automaticNotificationKeys.remove(key)
                activeNotifications.remove(key)
                return@forEach
            }
            val descriptor = characteristic.getDescriptor(cccdUuid)
            if (descriptor == null) {
                pendingAutomaticNotifications.remove(key)
                automaticNotificationKeys.remove(key)
                activeNotifications.remove(key)
                runCatching { gatt.setCharacteristicNotification(characteristic, false) }
                return@forEach
            }

            queuedAutomaticUnsubscribeKeys.add(key)
            enqueueGattOperation(
                gattOperationKey(parts[0], parts[1], "CCCD_DISABLE"),
                onTimeout = {
                    pendingAutomaticUnsubscribeKeys.remove(key)
                    queuedAutomaticUnsubscribeKeys.remove(key)
                    pendingAutomaticNotifications.remove(key)
                    automaticNotificationKeys.remove(key)
                    activeNotifications.remove(key)
                    runCatching { gatt.setCharacteristicNotification(characteristic, false) }
                    appendLog(
                        "Timeout ao cancelar CCCD automático de " + characteristic.uuid +
                            "; inscrição local desativada."
                    )
                }
            ) {
                if (bluetoothGatt !== gatt || isGattDisconnecting) {
                    queuedAutomaticUnsubscribeKeys.remove(key)
                    false
                } else {
                    gatt.setCharacteristicNotification(characteristic, false)
                    queuedAutomaticUnsubscribeKeys.remove(key)
                    pendingAutomaticUnsubscribeKeys.add(key)
                    val started = if (Build.VERSION.SDK_INT >= 33) {
                        writeDescriptorApi33(
                            gatt,
                            descriptor,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        run {
                            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                    if (!started) {
                        pendingAutomaticUnsubscribeKeys.remove(key)
                        queuedAutomaticUnsubscribeKeys.remove(key)
                        pendingAutomaticNotifications.remove(key)
                        automaticNotificationKeys.remove(key)
                        activeNotifications.remove(key)
                        appendLog("Android recusou iniciar cancelamento CCCD de " + characteristic.uuid)
                    } else {
                        appendLog("Cancelamento CCCD solicitado para " + characteristic.uuid)
                    }
                    started
                }
            }
        }
    }

    private fun requestDeepGattExploration() {
        val gatt = bluetoothGatt
        if (gatt == null || !isGattConnected || isGattDisconnecting) {
            appendLog("Exploração aprofundada indisponível: conecte primeiro o GT3 MAX.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Exploração manual dos canais GT3 MAX")
            .setMessage(
                "O aplicativo vai ler somente características que anunciam READ e ativar " +
                    "notificações/indicações que anunciam NOTIFY/INDICATE com CCCD padrão. " +
                    "Isso pode revelar dados privados de saúde. Nenhum comando de característica, " +
                    "NUS RX, DFU, OTA ou reset será enviado. A varredura pode levar vários minutos. " +
                    "O HEX só será salvo durante teste cronometrado ativo. A exploração pode ser encerrada " +
                    "desconectando o relógio. Continuar?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Iniciar exploração") { _, _ -> runDeepGattExploration(gatt) }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun runDeepGattExploration(gatt: BluetoothGatt, automatic: Boolean = false) {
        if (bluetoothGatt !== gatt || !isGattConnected || isGattDisconnecting) return
        val inventory = gatt.services.map { service ->
            GattServiceRecord(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    val properties = linkedSetOf<String>()
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) properties.add("READ")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) properties.add("NOTIFY")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) properties.add("INDICATE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) properties.add("WRITE")
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) properties.add("WRITE_NO_RESPONSE")
                    GattCharacteristicRecord(
                        uuid = characteristic.uuid.toString(),
                        properties = properties,
                        descriptorUuids = characteristic.descriptors.map { it.uuid.toString() }.toSet()
                    )
                }
            )
        }
        val plan = SafeGattExplorationPlan.buildDeep(inventory)
        val operationsToRun = plan.filter { operation ->
            val attemptKey = operation.serviceUuid.lowercase() + "/" +
                operation.characteristicUuid.lowercase() + "/" + operation.kind.name
            attemptKey !in deepExplorationAttempted
        }
        gattExplorationProgress.begin(operationsToRun)
        updateGattProgressUi()
        if (plan.isEmpty()) {
            appendLog("Exploração concluída: nenhum canal adicional elegível no inventário atual.")
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Exploração GATT sem canais elegíveis",
                detail = "O inventário não contém características adicionais compatíveis com READ ou NOTIFY/INDICATE via CCCD.",
                nowMillis = System.currentTimeMillis(),
                source = if (automatic) "AUTO_GATT_PROBE" else "MANUAL_GATT_PROBE",
                confidence = "INVENTARIO_OBSERVADO"
            )
            return
        }
        if (operationsToRun.isEmpty()) {
            appendLog("Exploração GATT: todos os canais elegíveis desta conexão já foram testados.")
            updateGattProgressUi()
            return
        }
        val probeSource = if (automatic) "AUTO_GATT_PROBE" else "MANUAL_GATT_PROBE"
        appendLog(
            (if (automatic) "Exploração GATT automática iniciada: " else "Exploração GATT manual iniciada: ") +
                operationsToRun.size + " operações elegíveis. Somente READ e CCCD padrão; escritas de payload continuam bloqueadas."
        )
        discoveryLibraryStore.recordDiscoveryAsync(
            title = if (automatic) "Exploração automática iniciada" else "Exploração manual iniciada",
            detail = operationsToRun.size.toString() + " operações elegíveis no inventário GATT.",
            nowMillis = System.currentTimeMillis(),
            source = probeSource,
            confidence = "OPERACOES_PLANEJADAS"
        )
        operationsToRun.forEach { operation ->
            val attemptKey = operation.serviceUuid.lowercase() + "/" +
                operation.characteristicUuid.lowercase() + "/" + operation.kind.name
            if (!deepExplorationAttempted.add(attemptKey)) return@forEach
            val serviceUuid = runCatching { UUID.fromString(operation.serviceUuid) }.getOrNull()
            val characteristicUuid = runCatching { UUID.fromString(operation.characteristicUuid) }.getOrNull()
            val characteristic = if (serviceUuid != null && characteristicUuid != null) {
                gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
            } else {
                null
            }
            if (characteristic == null) {
                gattExplorationProgress.markSkipped(
                    operation.serviceUuid, operation.characteristicUuid,
                    when (operation.kind) {
                        SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE,
                        SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE -> GattProgressOperation.READ
                        else -> GattProgressOperation.SUBSCRIBE
                    }
                )
                updateGattProgressUi()
                appendLog("Canal ignorado: característica não está mais disponível: " + operation.characteristicUuid)
                return@forEach
            }
            when (operation.kind) {
                SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE,
                SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE -> {
                    enqueueGattOperation(
                        gattOperationKey(operation.serviceUuid, operation.characteristicUuid, "READ"),
                        onTimeout = {
                            gattExplorationProgress.markTimeout(
                                operation.serviceUuid, operation.characteristicUuid,
                                GattProgressOperation.READ
                            )
                            updateGattProgressUi()
                            appendLog("Leitura GATT somente READ expirou: " + operation.characteristicUuid)
                            discoveryLibraryStore.recordDiscoveryAsync(
                                title = "Leitura GATT expirou • " + operation.characteristicUuid,
                                detail = "Nenhum callback de leitura foi recebido no prazo da fila GATT.",
                                nowMillis = System.currentTimeMillis(),
                                source = probeSource,
                                confidence = "READ_TIMEOUT"
                            )
                        }
                    ) {
                        if (bluetoothGatt !== gatt || isGattDisconnecting) {
                            gattExplorationProgress.markSkipped(
                                operation.serviceUuid, operation.characteristicUuid,
                                GattProgressOperation.READ
                            )
                            updateGattProgressUi()
                            false
                        } else if (automatic && !automaticGattDiscoveryEnabled) {
                            gattExplorationProgress.markSkipped(
                                operation.serviceUuid, operation.characteristicUuid,
                                GattProgressOperation.READ
                            )
                            updateGattProgressUi()
                            appendLog("Leitura automática cancelada: testes automáticos foram pausados.")
                            false
                        } else {
                            gattExplorationProgress.markStarted(
                                operation.serviceUuid, operation.characteristicUuid,
                                GattProgressOperation.READ
                            )
                            val started = gatt.readCharacteristic(characteristic)
                            if (!started) {
                                gattExplorationProgress.markRejected(
                                    operation.serviceUuid, operation.characteristicUuid,
                                    GattProgressOperation.READ
                                )
                            }
                            updateGattProgressUi()
                            appendLog(if (started) {
                                (if (automatic) "Leitura automática somente READ solicitada: " else "Leitura manual somente READ solicitada: ") +
                                    operation.serviceUuid + "/" + operation.characteristicUuid
                            } else {
                                "Android recusou leitura GATT: " + operation.characteristicUuid
                            })
                            discoveryLibraryStore.recordDiscoveryAsync(
                                title = "Leitura GATT solicitada • " + operation.characteristicUuid,
                                detail = if (started) "Android aceitou iniciar a leitura assíncrona." else "Android recusou iniciar a leitura.",
                                nowMillis = System.currentTimeMillis(),
                                source = probeSource,
                                confidence = if (started) "SOLICITADA" else "API_RECUSOU"
                            )
                            started
                        }
                    }
                }
                SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
                SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL -> {
                    enableNotifications(gatt, characteristic, automaticDiscovery = automatic)
                }
            }
        }
    }

    private fun logSafeGattRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        val nowMillis = System.currentTimeMillis()
        val serviceUuid = characteristic.service.uuid.toString()
        val characteristicUuid = characteristic.uuid.toString()
        if (status != BluetoothGatt.GATT_SUCCESS) {
            gattExplorationProgress.markReadResult(serviceUuid, characteristicUuid, false, 0, 0)
            updateGattProgressUi()
            appendLog("Leitura GATT falhou: " + characteristicUuid + " | status=" + status)
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Leitura GATT falhou • " + characteristicUuid,
                detail = "Callback Android retornou status " + status + ".",
                nowMillis = nowMillis,
                source = "GATT_READ",
                confidence = "READ_FALHOU"
            )
            return
        }

        val payloadHex = BleValueFormatter.toHex(value)
        val activeSession = discoveryLibraryStore.activeSession(nowMillis) != null
        appendLog(
            "Leitura GATT concluída: " + characteristicUuid + " | " + value.size + " bytes" +
                if (activeSession) " | HEX=" + payloadHex else ""
        )
        val payloadQueued = discoveryLibraryStore.recordPayloadAsync(
            kind = DiscoveryLibraryKind.GATT_READ,
            title = "Leitura GATT • " + characteristicUuid,
            detail = "Leitura concluída • " + value.size + " bytes",
            source = "GATT_READ",
            confidence = "OBSERVADO",
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            payloadHex = payloadHex,
            nowMillis = nowMillis
        )
        if (activeSession && !payloadQueued) {
            appendLog("Leitura GATT recebida, mas HEX não entrou no histórico: fila de gravação cheia.")
        }

        if (value.isEmpty()) {
            gattExplorationProgress.markReadResult(serviceUuid, characteristicUuid, true, 0, 0)
            updateGattProgressUi()
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Leitura concluída sem bytes • " + characteristicUuid,
                detail = "O relógio respondeu ao READ com comprimento zero. Isso não prova ausência de função; o canal pode depender de notificações ou de uma sequência ainda não identificada.",
                nowMillis = nowMillis,
                source = "GATT_READ",
                confidence = "READ_OK_SEM_PAYLOAD"
            )
            return
        }

        val decoded = GattValueDecoder.decode(serviceUuid, characteristicUuid, value)
        gattExplorationProgress.markReadResult(
            serviceUuid, characteristicUuid, true, value.size, decoded.size
        )
        updateGattProgressUi()
        if (decoded.isEmpty()) {
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Valor GATT recebido • " + characteristicUuid,
                detail = value.size.toString() +
                    " bytes recebidos; formato não reconhecido por decoder Bluetooth SIG. HEX só é persistido durante captura ativa.",
                nowMillis = nowMillis,
                source = "GATT_READ",
                confidence = "BYTES_OBSERVADOS_SEM_DECODER"
            )
        } else {
            decoded.forEach { item ->
                appendLog("Valor GATT decodificado: " + item.metric + " = " + item.value)
                if (activeSession) {
                    val resultQueued = discoveryLibraryStore.recordSessionDiscoveryAsync(
                        title = "Dado GATT observado • " + item.metric,
                        detail = item.value + " • origem=" + item.source +
                            " • formato Bluetooth SIG decodificado; precisão do sensor não validada.",
                        nowMillis = nowMillis,
                        source = "GATT_SIG_DECODER",
                        confidence = "FORMATO_PADRONIZADO_OBSERVADO"
                    )
                    if (!resultQueued) {
                        appendLog("Valor decodificado recebido, mas não enfileirado no histórico: fila de gravação cheia.")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateClockSyncAvailability(gatt: BluetoothGatt) {
        if (!::clockSyncButton.isInitialized || !::deviceTimeSyncView.isInitialized) return
        if (bluetoothGatt !== gatt || !isGattConnected || isGattDisconnecting) {
            clockSyncButton.isEnabled = false
            deviceTimeSyncView.text = "Hora do relógio: aguardando uma sessão BLE ativa."
            return
        }

        val service = gatt.getService(UUID.fromString(BluetoothCurrentTimePayload.SERVICE_UUID))
        val characteristic = service?.getCharacteristic(
            UUID.fromString(BluetoothCurrentTimePayload.CURRENT_TIME_UUID)
        )
        val capability = BluetoothCurrentTimeCapabilityPolicy.evaluate(
            connected = true,
            servicePresent = service != null,
            characteristicPresent = characteristic != null,
            supportsWrite = characteristic?.let {
                it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            } == true
        )
        when (capability) {
            BluetoothCurrentTimeCapabilityPolicy.Capability.DISCONNECTED -> {
                clockSyncButton.isEnabled = false
                deviceTimeSyncView.text = "Hora do relógio: aguardando uma sessão BLE ativa."
            }
            BluetoothCurrentTimeCapabilityPolicy.Capability.SERVICE_MISSING -> {
                clockSyncButton.isEnabled = false
                deviceTimeSyncView.text =
                    "Hora não sincronizada: CTS padrão (0x1805) ausente. Investigação passiva mantida; nenhum comando alternativo será enviado."
                appendLog("CTS 0x1805 ausente nesta sessão. Sincronização manual indisponível; nenhum protocolo proprietário foi tentado.")
            }
            BluetoothCurrentTimeCapabilityPolicy.Capability.CHARACTERISTIC_MISSING -> {
                clockSyncButton.isEnabled = false
                deviceTimeSyncView.text =
                    "CTS 0x1805 anunciado, mas Current Time (0x2A2B) ausente."
                appendLog("CTS anunciado sem 0x2A2B; sincronização manual indisponível.")
            }
            BluetoothCurrentTimeCapabilityPolicy.Capability.WRITE_UNSUPPORTED -> {
                clockSyncButton.isEnabled = false
                deviceTimeSyncView.text =
                    "CTS encontrado, mas 0x2A2B não anuncia WRITE. Sincronização manual indisponível."
                appendLog("CTS 0x1805/0x2A2B identificado sem WRITE. Nenhuma escrita será tentada.")
            }
            BluetoothCurrentTimeCapabilityPolicy.Capability.READY -> {
                clockSyncButton.isEnabled = !clockSyncAttemptedForSession
                deviceTimeSyncView.text = if (clockSyncAttemptedForSession) {
                    "CTS disponível; já houve uma tentativa nesta conexão. Reconecte para tentar novamente."
                } else {
                    "CTS disponível (0x1805/0x2A2B). Toque em “Sincronizar hora do relógio” para enviar a hora local do celular."
                }
                appendLog("CTS 0x1805/0x2A2B identificado com WRITE. Sincronização habilitada somente por ação manual.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun syncClockUsingCurrentTimeService(gatt: BluetoothGatt) {
        if (bluetoothGatt !== gatt || !isGattConnected || isGattDisconnecting) {
            if (::clockSyncButton.isInitialized) clockSyncButton.isEnabled = false
            deviceTimeSyncView.text = "Sincronização cancelada: a sessão BLE não está mais ativa."
            appendLog("Escrita CTS não iniciada: a sessão GATT não é mais a conexão ativa.")
            return
        }
        if (clockSyncAttemptedForSession) {
            deviceTimeSyncView.text = "Sincronização CTS já tentada nesta conexão. Reconecte para uma nova tentativa."
            return
        }

        val serviceUuid = UUID.fromString(BluetoothCurrentTimePayload.SERVICE_UUID)
        val characteristicUuid = UUID.fromString(BluetoothCurrentTimePayload.CURRENT_TIME_UUID)
        val service = gatt.getService(serviceUuid)
        if (service == null) {
            deviceTimeSyncView.text =
                "Hora não sincronizada: o relógio não anunciou o serviço CTS padrão (0x1805)."
            appendLog(
                "Sincronização manual de hora indisponível: CTS 0x1805 ausente. " +
                    "Nenhum comando proprietário foi enviado."
            )
            return
        }

        val characteristic = service.getCharacteristic(characteristicUuid)
        if (characteristic == null) {
            deviceTimeSyncView.text =
                "CTS encontrado, mas a característica Current Time (0x2A2B) não foi anunciada."
            appendLog("CTS 0x1805 sem a característica obrigatória Current Time 0x2A2B.")
            return
        }
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
            deviceTimeSyncView.text =
                "CTS encontrado, mas 0x2A2B não anuncia WRITE. Sincronização não iniciada."
            appendLog(
                "CTS 0x1805/0x2A2B sem propriedade GATT WRITE. " +
                    "A sincronização manual foi bloqueada sem tentar outro canal."
            )
            return
        }

        clockSyncAttemptedForSession = true
        if (::clockSyncButton.isInitialized) clockSyncButton.isEnabled = false
        val localDateTime = LocalDateTime.now()
        val payload = BluetoothCurrentTimePayload.encode(localDateTime)
        val key = service.uuid.toString() + "/" + characteristic.uuid.toString()
        pendingCurrentTimeWriteKey = key
        deviceTimeSyncView.text =
            "Sincronizando com a hora local do celular: " + localDateTime.toString()
        appendLog(
            "CTS compatível encontrado. Solicitada sincronização com a hora local do Android: " +
                localDateTime.toString() + ". A operação usa somente o serviço padrão 0x1805/0x2A2B."
        )

        enqueueGattOperation(
            gattOperationKey(service.uuid.toString(), characteristic.uuid.toString(), "WRITE"),
            onTimeout = {
                pendingCurrentTimeWriteKey = null
                deviceTimeSyncView.text = "Sincronização CTS expirou; o relógio não foi confirmado."
            }
        ) {
            if (bluetoothGatt !== gatt || !isGattConnected || isGattDisconnecting) {
                pendingCurrentTimeWriteKey = null
                deviceTimeSyncView.text = "Sincronização cancelada: sessão GATT não está ativa."
                appendLog("Escrita CTS cancelada antes do início: sessão GATT mudou ou foi encerrada.")
                false
            } else {
                val started = if (Build.VERSION.SDK_INT >= 33) {
                    writeCurrentTimeApi33(gatt, characteristic, payload)
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        characteristic.value = payload
                        gatt.writeCharacteristic(characteristic)
                    }
                }
                if (started) {
                    appendLog("Escrita CTS iniciada; aguardando callback onCharacteristicWrite.")
                    true
                } else {
                    pendingCurrentTimeWriteKey = null
                    deviceTimeSyncView.text =
                        "O Android recusou iniciar a escrita de hora pelo CTS."
                    appendLog("Android recusou iniciar a escrita GATT de Current Time (0x2A2B).")
                    false
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun writeCurrentTimeApi33(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean =
        gatt.writeCharacteristic(
            characteristic,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ) == BluetoothStatusCodes.SUCCESS

    @SuppressLint("MissingPermission")
    private fun renderGattServices(gatt: BluetoothGatt) {
        runOnUiThread {
            content.removeAllViews()
            addText(content, "Inventário GATT", 19f, true)
            addText(content, "Inventário passivo. O fluxo automático usa somente perfis Bluetooth SIG permitidos; " +
                "a exploração manual, mediante confirmação, pode ler canais customizados somente com READ " +
                "e ativar NOTIFY/INDICATE via CCCD. Nenhum payload de escrita é enviado.", 12f)

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
        characteristic: BluetoothGattCharacteristic,
        automaticDiscovery: Boolean = false
    ) {
        val key = characteristic.service.uuid.toString() + "/" +
            characteristic.uuid.toString()
        if (activeNotifications.contains(key)) {
            gattExplorationProgress.markSubscriptionResult(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(), true
            )
            updateGattProgressUi()
            appendLog("Notificação já ativa para " + characteristic.uuid)
            return
        }
        if (pendingNotifications.contains(key)) {
            gattExplorationProgress.markStarted(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                GattProgressOperation.SUBSCRIBE
            )
            updateGattProgressUi()
            appendLog("Notificação já pendente para " + characteristic.uuid)
            return
        }
        if (bluetoothGatt !== gatt || isGattDisconnecting) {
            appendLog("Inscrição cancelada: conexão GATT não está ativa para novas operações.")
            return
        }

        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor == null) {
            gattExplorationProgress.markRejected(
                characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                GattProgressOperation.SUBSCRIBE
            )
            updateGattProgressUi()
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
        if (automaticDiscovery) pendingAutomaticNotifications.add(key)
        enqueueGattOperation(
            gattOperationKey(characteristic.service.uuid.toString(), characteristic.uuid.toString(), "CCCD_ENABLE"),
            onTimeout = {
                gattExplorationProgress.markTimeout(
                    characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                    GattProgressOperation.SUBSCRIBE
                )
                updateGattProgressUi()
                pendingNotifications.remove(key)
                pendingAutomaticNotifications.remove(key)
                automaticNotificationKeys.remove(key)
                activeNotifications.remove(key)
                runCatching { gatt.setCharacteristicNotification(characteristic, false) }
                appendLog("Inscrição CCCD expirou para " + characteristic.uuid + "; inscrição local desativada.")
            }
        ) {
            if (bluetoothGatt !== gatt || isGattDisconnecting) {
                pendingNotifications.remove(key)
                pendingAutomaticNotifications.remove(key)
                gattExplorationProgress.markSkipped(
                    characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                    GattProgressOperation.SUBSCRIBE
                )
                updateGattProgressUi()
                appendLog("Inscrição descartada: conexão GATT mudou antes da execução.")
                false
            } else if (automaticDiscovery && !automaticGattDiscoveryEnabled) {
                pendingNotifications.remove(key)
                pendingAutomaticNotifications.remove(key)
                gattExplorationProgress.markSkipped(
                    characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                    GattProgressOperation.SUBSCRIBE
                )
                updateGattProgressUi()
                appendLog("Inscrição automática cancelada: coleta automática foi pausada.")
                false
            } else {
                gattExplorationProgress.markStarted(
                    characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                    GattProgressOperation.SUBSCRIBE
                )
                val localEnabled = gatt.setCharacteristicNotification(characteristic, true)
                if (!localEnabled) {
                    gattExplorationProgress.markRejected(
                        characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                        GattProgressOperation.SUBSCRIBE
                    )
                    updateGattProgressUi()
                    pendingNotifications.remove(key)
                    pendingAutomaticNotifications.remove(key)
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
                        pendingAutomaticNotifications.remove(key)
                        gattExplorationProgress.markRejected(
                            characteristic.service.uuid.toString(), characteristic.uuid.toString(),
                            GattProgressOperation.SUBSCRIBE
                        )
                        updateGattProgressUi()
                        gatt.setCharacteristicNotification(characteristic, false)
                        appendLog("O Android recusou iniciar a inscrição CCCD para " + characteristic.uuid)
                        false
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun writeDescriptorApi33(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ): Boolean =
        gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
    private fun gattOperationKey(serviceUuid: String, characteristicUuid: String, kind: String): String =
        serviceUuid.lowercase() + "/" + characteristicUuid.lowercase() + "/" + kind

    private fun enqueueGattOperation(
        key: String,
        onTimeout: () -> Unit = {},
        operation: () -> Boolean
    ) {
        gattOperationQueue.enqueue {
            val keyParts = key.split("/", limit = 3)
            val isCccdOperation = keyParts.size == 3 && keyParts[2].startsWith("CCCD_")
            val characteristicKey = if (keyParts.size == 3) keyParts[0] + "/" + keyParts[1] else ""
            if (key in invalidatedGattOperationKeys ||
                (isCccdOperation && characteristicKey in invalidatedCccdCharacteristicKeys)
            ) {
                if (keyParts.size == 3) {
                    when {
                        keyParts[2] == "READ" -> gattExplorationProgress.markSkipped(
                            keyParts[0], keyParts[1], GattProgressOperation.READ
                        )
                        isCccdOperation -> gattExplorationProgress.markSkipped(
                            keyParts[0], keyParts[1], GattProgressOperation.SUBSCRIBE
                        )
                    }
                    updateGattProgressUi()
                }
                appendLog("Operação GATT não repetida nesta conexão após timeout/Service Changed: " + key)
                false
            } else if (bluetoothGatt == null || isGattDisconnecting) {
                false
            } else {
                activeGattOperationKey = key
                val started = runCatching(operation).getOrDefault(false)
                if (started) {
                    val timeout = Runnable {
                        if (activeGattOperationKey == key) {
                            invalidatedGattOperationKeys.add(key)
                            val timedOutParts = key.split("/", limit = 3)
                            if (timedOutParts.size == 3 && timedOutParts[2].startsWith("CCCD_")) {
                                invalidatedCccdCharacteristicKeys.add(
                                    timedOutParts[0] + "/" + timedOutParts[1]
                                )
                            }
                            activeGattOperationKey = null
                            gattOperationTimeoutTask = null
                            appendLog("Timeout GATT após " + GATT_OPERATION_TIMEOUT_MS + " ms: " + key)
                            runCatching(onTimeout)
                            gattOperationQueue.onOperationCompleted()
                        }
                    }
                    gattOperationTimeoutTask = timeout
                    handler.postDelayed(timeout, GATT_OPERATION_TIMEOUT_MS)
                } else {
                    activeGattOperationKey = null
                }
                started
            }
        }
    }

    private fun acceptsGattOperationCallback(gatt: BluetoothGatt, key: String): Boolean {
        val accepted = GattCallbackSafetyPolicy.acceptsOperationCallback(
            connectionIsCurrent = bluetoothGatt === gatt,
            activeOperationKey = activeGattOperationKey,
            callbackOperationKey = key,
            invalidatedOperationKeys = invalidatedGattOperationKeys
        )
        if (!accepted) {
            appendLog("Callback GATT tardio ou não correspondente descartado antes de registrar dados: " + key)
        }
        return accepted
    }

    private fun completeGattOperation(gatt: BluetoothGatt, key: String) {
        if (bluetoothGatt !== gatt) return
        if (activeGattOperationKey != key) {
            appendLog("Callback GATT tardio ou não correspondente ignorado: " + key)
            return
        }
        gattOperationTimeoutTask?.let(handler::removeCallbacks)
        gattOperationTimeoutTask = null
        activeGattOperationKey = null
        gattOperationQueue.onOperationCompleted()
    }

    private fun resetGattOperationQueue() {
        gattOperationTimeoutTask?.let(handler::removeCallbacks)
        gattOperationTimeoutTask = null
        activeGattOperationKey = null
        gattOperationQueue.reset()
    }

    private fun handleNotification(serviceUuid: String, uuid: String, value: ByteArray) {
        val observedAtMillis = System.currentTimeMillis()
        val captureActive = discoveryLibraryStore.activeSession(observedAtMillis) != null
        val description = if (value.isEmpty()) "0 bytes" else {
            value.size.toString() + " bytes" +
                if (captureActive) ": " + BleValueFormatter.toHex(value) else ""
        }
        appendLog("NOTIFICAÇÃO " + uuid + " | " + description)
        val fingerprint = BlePacketFingerprintAnalyzer.observe(serviceUuid, uuid, value)
        val observation = passiveBleObservationTracker.observe(
            serviceUuid = serviceUuid,
            characteristicUuid = uuid,
            payloadBytes = value.size,
            fingerprint = fingerprint.sha256Prefix,
            observedAtMillis = observedAtMillis
        )
        appendLog(
            "Observação passiva #" + observation.sequence +
                " | canal=" + observation.channelNotificationCount +
                " | bytes acumulados=" + observation.channelByteCount +
                " | intervalo=" + (observation.intervalSincePreviousMillis?.toString() ?: "primeira") + " ms" +
                " | fingerprints distintos=" + observation.distinctFingerprints
        )
        appendLog(
            "Fingerprint BLE: tamanho=" + fingerprint.length +
                (if (captureActive) " | prefixo HEX=" + fingerprint.prefixHex else " | prefixo HEX=oculto fora da captura") +
                " | SHA-256/48=" + fingerprint.sha256Prefix +
                " | repetição=" + fingerprint.repeatedCount
        )

        val isNusTx =
            serviceUuid.equals(WearfitProtocolProfile.SERVICE_UUID.toString(), ignoreCase = true) &&
                uuid.equals(WearfitProtocolProfile.TX_CHARACTERISTIC_UUID.toString(), ignoreCase = true)
        val frames = if (isNusTx) nusFrameDecoder.append(value) else emptyList()
        if (isNusTx && frames.isEmpty()) {
            appendLog("NUS TX: nenhum quadro AB completo nesta notificação; aguardando fragmento ou formato ainda desconhecido.")
        }

        val lastFrame = frames.lastOrNull()
        val batteryFrame = frames.lastOrNull { it.batteryPercent != null }
        val battery = batteryFrame?.batteryPercent
        val observedFrame = batteryFrame ?: lastFrame
        val frameSummary = frames.joinToString("; ") { frame ->
            "AB cmd=0x" + frame.command.toString(16).uppercase(Locale.US) +
                "/resp=0x" + frame.response.toString(16).uppercase(Locale.US)
        }
        val payloadQueued = discoveryLibraryStore.recordPayloadAsync(
            kind = DiscoveryLibraryKind.NOTIFICATION,
            title = "Notificação BLE • " + uuid,
            detail = value.size.toString() + " bytes recebidos" +
                if (battery != null) " • bateria candidata " + battery + "%" else
                    if (frameSummary.isNotBlank()) " • " + frameSummary else " • sem interpretação",
            source = "BLE_NOTIFY",
            confidence = if (battery != null) "CANDIDATO" else "OBSERVADO",
            serviceUuid = serviceUuid,
            characteristicUuid = uuid,
            payloadHex = BleValueFormatter.toHex(value),
            nowMillis = observedAtMillis
        )
        if (!payloadQueued) appendLog("Fila de gravação BLE cheia; captura HEX desta notificação não foi enfileirada.")
        featureDiscoveryStore.recordNotification(
            serviceUuid = serviceUuid,
            characteristicUuid = uuid,
            payloadBytes = value.size,
            nowMillis = observedAtMillis,
            frameCommand = observedFrame?.command,
            frameResponse = observedFrame?.response,
            batteryCandidatePercent = battery
        )

        val decodedNotificationValues = GattValueDecoder.decode(serviceUuid, uuid, value)
        gattExplorationProgress.observeNotification(
            serviceUuid, uuid, value.size, decodedNotificationValues.size, observedAtMillis
        )
        updateGattProgressUi()
        if (discoveryLibraryStore.activeSession(observedAtMillis) != null) {
            decodedNotificationValues.forEach { item ->
                discoveryLibraryStore.recordSessionDiscoveryAsync(
                    title = "Dado BLE observado • " + item.metric,
                    detail = item.value + " • origem=" + item.source +
                        " • formato Bluetooth SIG decodificado; precisão do sensor não validada.",
                    nowMillis = observedAtMillis,
                    source = "BLE_SIG_DECODER",
                    confidence = "FORMATO_PADRONIZADO_OBSERVADO"
                )
                appendLog("Notificação decodificada: " + item.metric + " = " + item.value)
            }
        }

        frames.forEach { frame ->
            val candidateBattery = frame.batteryPercent
            val commandHex = frame.command.toString(16).uppercase(Locale.US).padStart(2, '0')
            val responseHex = frame.response.toString(16).uppercase(Locale.US).padStart(2, '0')
            discoveryLibraryStore.recordDiscoveryAsync(
                title = "Quadro AB " + commandHex + "/" + responseHex,
                detail = if (candidateBattery != null) {
                    "Interpretação candidata de bateria: " + candidateBattery +
                        "%. Comparação independente do valor ainda necessária."
                } else {
                    "Quadro de estrutura AB observado; sem semântica validada para este comando."
                },
                nowMillis = observedAtMillis,
                source = "PARSER_AB",
                confidence = if (candidateBattery != null) "CANDIDATO" else "FORMATO_OBSERVADO"
            )
            if (candidateBattery != null) {
                runOnUiThread {
                    val receivedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    deviceBatteryView.text =
                        "Bateria BLE candidata: " + candidateBattery + "% • recebido " + receivedAt +
                            " • aguardando confirmação independente"
                }
                appendLog(
                    "Interpretação candidata do quadro AB/0x91: bateria reportada pelo relógio = " +
                        candidateBattery + "%. Não validada por comparação independente repetida."
                )
            } else {
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
        connectionPrefs.edit { putBoolean("auto_reconnect_enabled", false) }
        appendLog("Reconexão automática desativada por solicitação do usuário.")
        val gatt = bluetoothGatt
        if (gatt == null) {
            updateStatus("Nenhuma conexão ativa.")
            return
        }
        appendLog("Desconexão solicitada pelo usuário.")
        recordPendingGattDiscoveryInterruption("Desconexão solicitada pelo usuário.")
        isGattDisconnecting = true
        resetGattOperationQueue()
        pendingNotifications.clear()
        activeNotifications.clear()
        pendingAutomaticNotifications.clear()
        automaticNotificationKeys.clear()
        pendingAutomaticUnsubscribeKeys.clear()
        queuedAutomaticUnsubscribeKeys.clear()
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
                    detail = if (value.contains("reconexão automática")) {
                        "Busca automática de baixa potência pelo GT3 MAX salvo localmente."
                    } else {
                        "Busca BLE ativa por até 15 segundos. Mantenha o GT3 MAX próximo ao celular."
                    }
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
            disconnectButton.text = if (gattSessionExists) "Desconectar" else "Parar reconexão"
            disconnectButton.isEnabled = gattSessionExists || reconnectPolicy.isEnabled || reconnectRunnable != null || isAutoReconnectScan
            if (::deepScanButton.isInitialized) {
                deepScanButton.isEnabled = isGattConnected && !isGattDisconnecting
            }
        }
    }

    private fun exportDiagnosticReport() {
        val historyFlushed = discoveryLibraryStore.flushPendingWrites()
        if (!historyFlushed) {
            appendLog("A exportação seguirá com o último snapshot confirmado; há gravações BLE pendentes.")
        }
        val now = Date()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now)
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(now)
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "desconhecida"
        }.getOrDefault("desconhecida")
        val discoveryEntries = when (val stored = discoveryLibraryStore.read()) {
            DiscoveryLibraryReadResult.Empty -> listOf("Nenhuma descoberta persistida até esta exportação.")
            is DiscoveryLibraryReadResult.Available -> stored.snapshot.records.takeLast(1_000).map { record ->
                val occurredAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(Date(record.occurredAtMillis))
                buildString {
                    append(occurredAt).append(" | ").append(record.kind).append(" | ")
                    append(record.title).append(" | ").append(record.detail)
                    append(" | origem=").append(record.source).append(" | confiança=").append(record.confidence)
                    record.serviceUuid?.let { append(" | serviço=").append(it) }
                    record.characteristicUuid?.let { append(" | característica=").append(it) }
                    record.payloadHex?.let { append(" | HEX=").append(it) }
                }
            }
            DiscoveryLibraryReadResult.Corrupt -> listOf("Biblioteca local corrompida; conteúdo preservado e não sobrescrito.")
            is DiscoveryLibraryReadResult.UnsupportedSchema -> listOf(
                "Biblioteca local usa schema futuro (" + stored.version + "); conteúdo preservado e não sobrescrito."
            )
        }
        val report = BleDiagnosticReport.build(
            appVersion = version,
            androidVersion = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
            connectionState = statusView.text?.toString() ?: "indisponível",
            deviceName = lastConnectedDeviceName,
            deviceAddress = lastConnectedDeviceAddress,
            generatedAt = generatedAt,
            logLines = logLines.toList(),
            discoveryEntries = discoveryEntries
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

    private fun recordPendingGattDiscoveryInterruption(reason: String) {
        if (serviceDiscoveryTimeoutTask == null || bluetoothGatt == null) return
        val historyFlushed = discoveryLibraryStore.flushPendingWrites()
        discoveryLibraryStore.recordDiscovery(
            title = "Descoberta GATT interrompida antes do callback",
            detail = reason + " O inventário de uma conexão anterior não representa esta tentativa.",
            nowMillis = System.currentTimeMillis(),
            source = "GATT_LIFECYCLE",
            confidence = "INTERRUPTIDA"
        )
        appendLog("Descoberta GATT interrompida: " + reason +
            if (historyFlushed) "" else " • gravações BLE anteriores ainda pendentes")
    }

    private fun scheduleGattServiceDiscoveryTimeout(gatt: BluetoothGatt) {
        cancelGattServiceDiscoveryTimeout()
        val timeout = Runnable {
            val shouldRecover = GattDiscoveryRecoveryPolicy.shouldRecover(
                connectionIsCurrent = bluetoothGatt === gatt,
                connected = isGattConnected,
                disconnecting = isGattDisconnecting,
                callbackReceived = false
            )
            if (shouldRecover) {
                failGattServiceDiscovery(
                    gatt,
                    "Tempo limite de ${GATT_SERVICE_DISCOVERY_TIMEOUT_MS} ms aguardando onServicesDiscovered()."
                )
            }
        }
        serviceDiscoveryTimeoutTask = timeout
        handler.postDelayed(timeout, GATT_SERVICE_DISCOVERY_TIMEOUT_MS)
    }

    private fun cancelGattServiceDiscoveryTimeout() {
        serviceDiscoveryTimeoutTask?.let(handler::removeCallbacks)
        serviceDiscoveryTimeoutTask = null
    }

    @SuppressLint("MissingPermission")
    private fun failGattServiceDiscovery(gatt: BluetoothGatt, reason: String) {
        if (bluetoothGatt !== gatt) return
        cancelGattServiceDiscoveryTimeout()
        isGattConnected = false
        gattExplorationProgress.markRemainingSkipped()
        updateGattProgressUi()
        resetGattOperationQueue()
        pendingNotifications.clear()
        activeNotifications.clear()
        pendingAutomaticNotifications.clear()
        automaticNotificationKeys.clear()
        pendingAutomaticUnsubscribeKeys.clear()
        queuedAutomaticUnsubscribeKeys.clear()
        automaticGattProbeStartedForConnection = false
        finishAutomaticCaptureSession("Descoberta GATT não concluída")
        discoveryLibraryStore.recordDiscoveryAsync(
            title = "Descoberta GATT não concluída",
            detail = reason + " A sessão anterior não será apresentada como inventário desta conexão; reconexão será tentada conforme a política local.",
            nowMillis = System.currentTimeMillis(),
            source = "GATT_LIFECYCLE",
            confidence = if (reason.contains("Tempo limite")) "TIMEOUT" else "FALHA"
        )
        appendLog("Descoberta GATT não concluída: " + reason)
        bluetoothGatt = null
        isGattDisconnecting = true
        updateStatus("Falha ao descobrir serviços GATT. " + reason)
        runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
        isGattDisconnecting = false
        if (reconnectPolicy.isEnabled) scheduleAutoReconnect("Falha na descoberta dos serviços GATT")
    }

    private fun scheduleAutoReconnect(reason: String, immediate: Boolean = false) {
        if (expectedWatchAddress == null && reconnectPolicy.isEnabled) {
            appendLog("Reconexão automática fará busca BLE geral; a identidade será aprendida após validação do fingerprint GATT.")
        }

        when (val decision = reconnectCoordinator.planSchedule(
            activityResumed = isActivityResumed,
            gattSessionExists = bluetoothGatt != null,
            scanInProgress = currentScanCallback != null,
            immediate = immediate
        )) {
            BleReconnectCoordinator.ScheduleDecision.Disabled -> return
            BleReconnectCoordinator.ScheduleDecision.DeferredToForeground -> {
                appendLog("Reconexão automática aguardará o aplicativo voltar ao primeiro plano.")
                return
            }
            BleReconnectCoordinator.ScheduleDecision.AlreadyActive -> return
            is BleReconnectCoordinator.ScheduleDecision.Scheduled -> {
                cancelReconnectTask()
                val delayMillis = decision.delayMillis
                appendLog(reason + ". Próxima tentativa de reconexão em " + delayMillis + " ms.")
                updateStatus(if (delayMillis == 0L) "Reconectando GT3 MAX..." else "Reconexão automática aguardando " + delayMillis + " ms")

                val task = Runnable {
                    reconnectRunnable = null
                    when (reconnectCoordinator.evaluateAttempt(
                        activityResumed = isActivityResumed,
                        gattSessionExists = bluetoothGatt != null,
                        scanInProgress = currentScanCallback != null,
                        hasBlePermissions = { hasBlePermissions() }
                    )) {
                        BleReconnectCoordinator.AttemptDecision.Skip -> return@Runnable
                        BleReconnectCoordinator.AttemptDecision.PermissionRequired -> {
                            updateStatus("Permissão Bluetooth necessária para reconectar.")
                            appendLog("Reconexão pausada: restaure a permissão Bluetooth e retorne ao aplicativo.")
                            return@Runnable
                        }
                        BleReconnectCoordinator.AttemptDecision.StartScan ->
                            startBleScan(automatic = true)
                    }
                }
                reconnectRunnable = task
                if (delayMillis == 0L) handler.post(task) else handler.postDelayed(task, delayMillis)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        if (!discoveryLibraryStore.flushPendingWrites()) {
            appendLog("Encerramento com gravações BLE ainda pendentes; o último lote pode não entrar no histórico.")
        }
        isActivityResumed = false
        cancelReconnectTask()
        cancelConnectionTimeout()
        recordPendingGattDiscoveryInterruption("Aplicativo encerrado antes do callback onServicesDiscovered.")
        cancelGattServiceDiscoveryTimeout()
        stopBleScan()
        finishAutomaticCaptureSession("Aplicativo encerrado")
        resetGattOperationQueue()
        pendingNotifications.clear()
        activeNotifications.clear()
        pendingAutomaticNotifications.clear()
        automaticNotificationKeys.clear()
        pendingAutomaticUnsubscribeKeys.clear()
        queuedAutomaticUnsubscribeKeys.clear()
        healthScope.cancel()
        isGattDisconnecting = true
        runCatching { bluetoothGatt?.disconnect() }
        runCatching { bluetoothGatt?.close() }
        bluetoothGatt = null
        super.onDestroy()
    }
}

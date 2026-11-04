package com.luizmarquesarruda.relogio20

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryKind
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryReadResult
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryRecord
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local-only library of GATT discoveries and readings captured during timed tests. */
class DiscoveryLibraryActivity : Activity() {
    private val store by lazy { DiscoveryLibraryStore(this) }
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private lateinit var search: EditText
    private var selectedFilter = FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) render()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(0xFFF4F6F8.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(Button(this).apply {
            text = "‹"
            textSize = 24f
            isAllCaps = false
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(48)))
        header.addView(TextView(this).apply {
            text = "Biblioteca interna"
            textSize = 21f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "Descobertas e leituras salvas somente neste aparelho. HEX é armazenado apenas durante uma sessão de teste iniciada por você."
            textSize = 12f
            setTextColor(0xFF526574.toInt())
            setPadding(0, dp(8), 0, dp(8))
        })

        summary = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(summary)

        search = EditText(this).apply {
            hint = "Pesquisar UUID, leitura ou descoberta"
            setSingleLine(true)
            textSize = 14f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedSurface()
        }
        root.addView(search, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ).apply { bottomMargin = dp(8) })
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderRecords()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            FILTER_ALL to "Tudo",
            FILTER_DISCOVERY to "Descobertas",
            FILTER_READINGS to "Leituras",
            FILTER_SESSIONS to "Testes"
        ).forEach { (filter, label) ->
            filters.addView(Button(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                setOnClickListener {
                    selectedFilter = filter
                    renderRecords()
                }
            }, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                marginEnd = dp(4)
            })
        }
        root.addView(filters)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(20))
        }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun render() {
        when (val result = store.read()) {
            DiscoveryLibraryReadResult.Empty -> {
                summary.text = "0 registros salvos"
                list.removeAllViews()
                addMessage("A biblioteca está vazia. Faça uma descoberta GATT ou inicie um teste cronometrado para guardar leituras.")
            }
            is DiscoveryLibraryReadResult.Available -> {
                val session = result.snapshot.activeSession
                summary.text = result.snapshot.records.size.toString() + " registros • " +
                    if (session != null) "teste em andamento" else "nenhum teste ativo"
                renderRecords()
            }
            DiscoveryLibraryReadResult.Corrupt -> {
                summary.text = "Biblioteca local inválida"
                list.removeAllViews()
                addMessage("Os dados existentes foram preservados. O aplicativo não sobrescreverá automaticamente este arquivo.")
            }
            is DiscoveryLibraryReadResult.UnsupportedSchema -> {
                summary.text = "Biblioteca de versão mais nova"
                list.removeAllViews()
                addMessage("Schema " + result.version + " não é compatível com esta versão. Os registros foram preservados.")
            }
        }
    }

    private fun renderRecords() {
        if (!::list.isInitialized) return
        val snapshot = (store.read() as? DiscoveryLibraryReadResult.Available)?.snapshot
        list.removeAllViews()
        if (snapshot == null) return
        val query = search.text.toString().trim().lowercase(Locale.ROOT)
        val records = snapshot.records.asReversed().filter { record ->
            matchesFilter(record) &&
                (query.isEmpty() || listOf(
                    record.title, record.detail, record.source, record.confidence,
                    record.serviceUuid.orEmpty(), record.characteristicUuid.orEmpty(),
                    record.payloadHex.orEmpty()
                ).any { it.lowercase(Locale.ROOT).contains(query) })
        }
        if (records.isEmpty()) {
            addMessage("Nenhum registro corresponde a este filtro.")
            return
        }
        records.forEach { addRecordCard(it) }
    }

    private fun matchesFilter(record: DiscoveryLibraryRecord): Boolean = when (selectedFilter) {
        FILTER_DISCOVERY -> record.kind == DiscoveryLibraryKind.DISCOVERY
        FILTER_READINGS -> record.kind == DiscoveryLibraryKind.NOTIFICATION ||
            record.kind == DiscoveryLibraryKind.GATT_READ
        FILTER_SESSIONS -> record.kind == DiscoveryLibraryKind.SESSION_STARTED ||
            record.kind == DiscoveryLibraryKind.SESSION_FINISHED
        else -> true
    }

    private fun addRecordCard(record: DiscoveryLibraryRecord) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = roundedSurface()
        }
        val type = when (record.kind) {
            DiscoveryLibraryKind.DISCOVERY -> "DESCOBERTA"
            DiscoveryLibraryKind.NOTIFICATION -> "NOTIFICAÇÃO BLE"
            DiscoveryLibraryKind.GATT_READ -> "LEITURA GATT"
            DiscoveryLibraryKind.SESSION_STARTED -> "TESTE INICIADO"
            DiscoveryLibraryKind.SESSION_FINISHED -> "TESTE FINALIZADO"
            else -> record.kind
        }
        addText(card, type + " • " + formatDate(record.occurredAtMillis), 10f, 0xFF687786.toInt(), true)
        addText(card, record.title, 15f, 0xFF14212B.toInt(), true)
        addText(card, record.detail, 12f, 0xFF526574.toInt())
        addText(card, "Origem: " + record.source + " • Estado: " + record.confidence, 10f, 0xFF687786.toInt())
        if (record.characteristicUuid != null) {
            addText(card, record.characteristicUuid, 10f, 0xFF687786.toInt())
        }
        if (record.payloadHex != null) {
            addText(card, "HEX: " + record.payloadHex, 10f, 0xFF34495E.toInt())
        }
        list.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })
    }

    private fun addMessage(value: String) {
        addText(list, value, 13f, 0xFF526574.toInt())
    }

    private fun addText(parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(4))
        })
    }

    private fun roundedSurface(): GradientDrawable = GradientDrawable().apply {
        setColor(0xFFFFFFFF.toInt())
        setStroke(dp(1), 0xFFE0E6EB.toInt())
        cornerRadius = dp(12).toFloat()
    }

    private fun formatDate(timestamp: Long): String =
        if (timestamp <= 0L) "Hora indisponível"
        else SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val FILTER_ALL = "ALL"
        private const val FILTER_DISCOVERY = "DISCOVERY"
        private const val FILTER_READINGS = "READINGS"
        private const val FILTER_SESSIONS = "SESSIONS"
    }
}

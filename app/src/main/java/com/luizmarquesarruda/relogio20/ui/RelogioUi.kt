package com.luizmarquesarruda.relogio20.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Shared visual language for the Relógio 2.0 primary navigation.
 * Kept independent from BLE, health data and protocol code.
 */
object RelogioUi {
    val screen = 0xFF07111F.toInt()
    val surface = 0xFF0E1B2D.toInt()
    val surfaceRaised = 0xFF13243A.toInt()
    val surfaceInset = 0xFF091626.toInt()
    val border = 0xFF28415F.toInt()
    val text = 0xFFF8FAFC.toInt()
    val textMuted = 0xFF9FB4CF.toInt()
    val textSubtle = 0xFF7185A1.toInt()
    val accent = 0xFF7DB3FF.toInt()
    val success = 0xFF4ADE80.toInt()
    val warning = 0xFFFBBF24.toInt()
    val danger = 0xFFFB7185.toInt()

    fun install(
        activity: Activity,
        title: String,
        subtitle: String,
        showBack: Boolean = true
    ): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(screen)
            setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 10))
        }
        applyEdgeToEdge(
            activity = activity,
            root = root,
            paddingLeftDp = 16,
            paddingTopDp = 12,
            paddingRightDp = 16,
            paddingBottomDp = 10
        )

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        if (showBack) {
            val back = TextView(activity).apply {
                text = "‹"
                textSize = 34f
                gravity = Gravity.CENTER
                setTextColor(accent)
                isClickable = true
                isFocusable = true
                contentDescription = "Voltar"
                setOnClickListener { activity.finish() }
            }
            header.addView(back, LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 48)))
        }
        val titleColumn = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(if (showBack) dp(activity, 8) else 0, 0, 0, 0)
        }
        titleColumn.addView(textView(activity, title, 21f, text, true))
        titleColumn.addView(textView(activity, subtitle, 9f, accent, true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(activity, 3), 0, 0)
        })
        header.addView(titleColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header)

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(activity, 14), 0, dp(activity, 24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        activity.setContentView(root)
        return body
    }


    /**
     * Uses edge-to-edge consistently and applies system-bar/cutout insets to the root.
     * Base padding remains in dp; inset values are added at runtime for each device.
     */
    fun applyEdgeToEdge(
        activity: Activity,
        root: View,
        paddingLeftDp: Int,
        paddingTopDp: Int,
        paddingRightDp: Int,
        paddingBottomDp: Int,
        lightSystemBarIcons: Boolean = false
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        if (Build.VERSION.SDK_INT < 35) {
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.navigationBarColor = Color.TRANSPARENT
        }
        WindowInsetsControllerCompat(activity.window, root).apply {
            isAppearanceLightStatusBars = lightSystemBarIcons
            isAppearanceLightNavigationBars = lightSystemBarIcons
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                dp(activity, paddingLeftDp) + systemInsets.left,
                dp(activity, paddingTopDp) + systemInsets.top,
                dp(activity, paddingRightDp) + systemInsets.right,
                dp(activity, paddingBottomDp) + systemInsets.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    fun section(parent: LinearLayout, title: String, hint: String? = null) {
        val heading = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 2), dp(context, 11), dp(context, 2), dp(context, 7))
        }
        heading.addView(textView(parent.context, title, 11f, accent, true).apply {
            letterSpacing = 0.09f
        })
        if (!hint.isNullOrBlank()) {
            heading.addView(textView(parent.context, hint, 10f, textSubtle, false).apply {
                setPadding(0, dp(context, 3), 0, 0)
            })
        }
        parent.addView(heading)
    }

    fun message(parent: LinearLayout, value: String) {
        card(parent, value, null, null, null, null)
    }

    fun metric(
        parent: LinearLayout,
        label: String,
        value: String,
        detail: String? = null
    ) {
        card(parent, label, detail, value, null, null)
    }

    fun card(
        parent: LinearLayout,
        title: String,
        description: String?,
        status: String? = null,
        actionLabel: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        val context = parent.context
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
            background = rounded(context, surface, border)
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(textView(context, title, 14f, text, true), LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))
        if (!status.isNullOrBlank()) {
            top.addView(textView(context, status, 10f, accent, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4))
                background = rounded(context, surfaceInset, border)
            })
        }
        card.addView(top)
        if (!description.isNullOrBlank()) {
            card.addView(textView(context, description, 11f, textMuted, false).apply {
                setPadding(0, dp(context, 6), 0, 0)
                setLineSpacing(dp(context, 2).toFloat(), 1f)
            })
        }
        if (!actionLabel.isNullOrBlank()) {
            card.addView(textView(context, actionLabel, 10f, accent, true).apply {
                setPadding(0, dp(context, 9), 0, 0)
                letterSpacing = 0.05f
            })
        }
        parent.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(context, 8)
        })
    }

    fun button(parent: LinearLayout, label: String, onClick: () -> Unit) {
        val context = parent.context
        val button = Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            maxLines = 2
            setTextColor(RelogioUi.text)
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6))
            background = rounded(context, surfaceRaised, accent)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        parent.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, 50)
        ).apply { bottomMargin = dp(context, 8) })
    }

    fun textView(
        context: Context,
        value: String,
        size: Float = 13f,
        color: Int = textMuted,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(context, 1).toFloat(), 1f)
    }

    fun rounded(context: Context, fill: Int = surface, stroke: Int = border): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            setStroke(dp(context, 1), stroke)
            cornerRadius = dp(context, 16).toFloat()
        }

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}

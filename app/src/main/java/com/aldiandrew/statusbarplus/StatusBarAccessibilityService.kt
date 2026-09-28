package com.aldiandrew.statusbarplus

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusBarAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var textView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            updateOverlay()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        synchronized(lock) { instance = this }
        createOverlay()
        handler.post(tick)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        updateOverlay()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        synchronized(lock) { instance = null }
        super.onDestroy()
    }

    private fun createOverlay() {
        if (textView != null) return

        val tv = TextView(this).apply {
            setSingleLine(true)
            includeFontPadding = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER_VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        textView = tv

        val params = WindowManager.LayoutParams(
            dp(180f),
            statusBarHeight(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager.addView(tv, params)
        } catch (_: Exception) {
            textView = null
        }
        updateOverlay()
    }

    private fun updateOverlay() {
        val tv = textView ?: return
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val short = prefs.getBoolean("short_day", false)
        val offset = prefs.getFloat("offset_dp", 58f)
        val size = prefs.getFloat("text_size", 13f)
        val dark = prefs.getBoolean("dark_text", false)

        val pattern = if (short) "EEE" else "EEEE"
        val day = SimpleDateFormat(pattern, Locale("id", "ID")).format(Date())

        tv.text = day
        tv.textSize = size
        tv.setTextColor(if (dark) Color.BLACK else Color.WHITE)
        tv.setShadowLayer(
            dp(1f).toFloat(),
            0f,
            dp(0.7f).toFloat(),
            if (dark) Color.WHITE else Color.BLACK
        )

        val params = tv.layoutParams as? WindowManager.LayoutParams ?: return
        params.x = dp(offset)
        params.height = statusBarHeight()
        try {
            windowManager.updateViewLayout(tv, params)
        } catch (_: Exception) {
        }
    }

    private fun removeOverlay() {
        textView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        textView = null
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else dp(24f)
    }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private val lock = Any()
        private var instance: StatusBarAccessibilityService? = null

        fun refresh() {
            synchronized(lock) {
                instance?.updateOverlay()
            }
        }
    }
}

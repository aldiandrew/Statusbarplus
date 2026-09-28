package com.aldiandrew.statusbarplus

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
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
    private var screenOn = true

    private val tick = object : Runnable {
        override fun run() {
            updateOverlay()
            handler.postDelayed(this, 1000L)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenOn = intent?.action != Intent.ACTION_SCREEN_OFF
            updateOverlay()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        synchronized(lock) { instance = this }

        screenOn = true
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            RECEIVER_NOT_EXPORTED
        )

        createOverlay()
        handler.post(tick)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        updateOverlay()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
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
            dp(240f),
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
        if (!screenOn || !isStatusBarLikelyVisible()) {
            tv.visibility = View.GONE
            return
        }

        tv.visibility = View.VISIBLE

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val short = prefs.getBoolean("short_day", false)
        val size = prefs.getFloat("text_size", 13f)
        val dark = prefs.getBoolean("dark_text", false)

        val locale = Locale.getDefault()
        val pattern = if (short) "EEE" else "EEEE"
        val day = SimpleDateFormat(pattern, locale).format(Date())

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
        params.x = calculateClockEndOffset(locale)
        params.height = statusBarHeight()
        try {
            windowManager.updateViewLayout(tv, params)
        } catch (_: Exception) {
        }
    }

    private fun calculateClockEndOffset(locale: Locale): Int {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val manual = prefs.getBoolean("manual_position", false)
        if (manual) return dp(prefs.getFloat("offset_dp", 58f))

        val is24 = DateFormat.is24HourFormat(this)
        val pattern = if (is24) "HH:mm" else "h:mm a"
        val clock = SimpleDateFormat(pattern, locale).format(Date())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = dp(14f).toFloat()
        }
        val measuredClockWidth = paint.measureText(clock)
        val leftInset = getStatusBarLeftInset()
        val gap = dp(7f)

        // Motorola's stock clock is placed at the start of the status bar.
        // Anchor the day after the measured clock width instead of using a
        // fixed x-position, so the first activation does not overlap the clock.
        return (leftInset + measuredClockWidth + gap).toInt()
    }

    private fun getStatusBarLeftInset(): Int {
        return try {
            windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
                .left
        } catch (_: Exception) {
            0
        }
    }

    private fun isStatusBarLikelyVisible(): Boolean {
        // Prefer the actual SystemUI accessibility window when available.
        // This avoids treating Android 15/16 edge-to-edge app windows as
        // fullscreen merely because their content occupies the whole display.
        val bar = statusBarHeight()
        val systemWindowVisible = windows.any { window ->
            if (window.type != android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM) {
                false
            } else {
                val bounds = android.graphics.Rect()
                window.getBoundsInScreen(bounds)
                bounds.top <= 1 && bounds.height() <= bar * 2
            }
        }
        if (systemWindowVisible) return true

        // If SystemUI does not expose its status-bar window to accessibility,
        // keep the overlay visible rather than risking a permanent false hide.
        return windows.isEmpty()
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

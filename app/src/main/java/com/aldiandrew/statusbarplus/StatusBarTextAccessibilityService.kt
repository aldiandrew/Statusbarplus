package com.aldiandrew.statusbarplus

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusBarTextAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_REFRESH = "com.aldiandrew.statusbarplus.ACCESSIBILITY_REFRESH"
        const val ACTION_HIDE = "com.aldiandrew.statusbarplus.ACCESSIBILITY_HIDE"

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val component = ComponentName(context, StatusBarTextAccessibilityService::class.java)
                .flattenToString()
            return enabled.split(':').any { it.equals(component, ignoreCase = true) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: TextView? = null
    private var windowManager: WindowManager? = null
    private var receiverRegistered = false

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                ACTION_REFRESH -> scheduleRefresh()
                ACTION_HIDE -> removeOverlay()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 250
            packageNames = arrayOf("com.android.systemui")
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_REFRESH)
            addAction(ACTION_HIDE)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(refreshReceiver, filter)
        }
        receiverRegistered = true
        scheduleRefresh()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.android.systemui") return
        scheduleRefresh()
    }

    override fun onInterrupt() {
        removeOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(refreshReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun scheduleRefresh() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ updateOverlay() }, 180)
    }

    private fun updateOverlay() {
        if (!DayNotificationManager.isEnabled(this)) {
            removeOverlay()
            return
        }

        val clock = findSystemUiClock() ?: run {
            removeOverlay()
            handler.postDelayed({ updateOverlay() }, 700)
            return
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val locale = Locale.getDefault()
        val shortDay = prefs.getBoolean("short_day", true)
        val mode = prefs.getString("display_mode", "day") ?: "day"

        val day = SimpleDateFormat(if (shortDay) "EEE" else "EEEE", locale).format(Date())
        val date = SimpleDateFormat("d", locale).format(Date())
        val month = SimpleDateFormat("MMM", locale).format(Date())

        val text = when (mode) {
            "day_date" -> "$day\n$date"
            "day_date_month" -> "$day\n$date $month"
            "date_month" -> "$date\n$month"
            else -> day
        }

        val density = resources.displayMetrics.density
        val requestedSp = prefs.getFloat("text_size", 20f).coerceIn(12f, 22f)
        val multiLine = text.contains('\n')
        val clockHeightDp = clock.height().coerceAtLeast((16f * density).toInt()) / density

        // Keep the selected size where possible, but never let the glyphs exceed
        // the status-bar text band. Multi-line modes use a conservative cap.
        val maxSp = if (multiLine) {
            (clockHeightDp * 0.42f).coerceIn(8f, 13f)
        } else {
            (clockHeightDp * 0.92f).coerceIn(11f, 22f)
        }
        val actualSp = requestedSp.coerceAtMost(maxSp)

        val tv = overlay ?: TextView(this).also {
            it.includeFontPadding = false
            it.gravity = Gravity.CENTER
            it.textAlignment = View.TEXT_ALIGNMENT_CENTER
            it.setPadding(0, 0, 0, 0)
            it.isClickable = false
            it.isFocusable = false
            overlay = it
        }

        tv.text = text
        tv.typeface = FontManager.getTypeface(this)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, actualSp)
        tv.setTextColor(statusBarTextColor())
        tv.maxLines = if (multiLine) 2 else 1
        tv.setLineSpacing(0f, 0.86f)

        tv.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val textWidth = tv.measuredWidth.coerceAtLeast((8f * density).toInt())
        val textHeight = tv.measuredHeight.coerceAtLeast(clock.height())
        val displayWidth = resources.displayMetrics.widthPixels
        val gap = (2f * density).toInt()

        // Follow the actual SystemUI clock instead of guessing a fixed position.
        // If the clock is already near the right edge, place the text before it.
        val afterX = clock.right + gap
        val beforeX = clock.left - gap - textWidth
        val x = if (afterX + textWidth <= displayWidth) afterX else beforeX.coerceAtLeast(0)

        val y = clock.centerY() - textHeight / 2

        val params = WindowManager.LayoutParams(
            textWidth,
            textHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y.coerceAtLeast(0)
        }

        try {
            if (tv.parent == null) {
                windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager?.addView(tv, params)
            } else {
                windowManager?.updateViewLayout(tv, params)
            }
        } catch (_: Exception) {
            removeOverlay()
        }
    }

    private fun statusBarTextColor(): Int {
        val night = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (night) Color.WHITE else Color.BLACK
    }

    private fun findSystemUiClock(): Rect? {
        val allWindows = windows
        for (window in allWindows) {
            val root = window.root ?: continue
            val packageName = root.packageName?.toString() ?: continue
            if (packageName != "com.android.systemui") continue

            val node = findClockNode(root) ?: continue
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            node.recycle()
            root.recycle()
            if (!bounds.isEmpty && bounds.height() > 0) return bounds
        }
        return null
    }

    private fun findClockNode(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
        val id = node.viewIdResourceName.orEmpty()
        val className = node.className?.toString().orEmpty()
        val text = node.text?.toString().orEmpty()

        val idLooksLikeClock =
            id.endsWith(":id/clock") ||
                id.endsWith("/clock") ||
                id.contains("clock_left") ||
                id.contains("clock_right") ||
                id.contains("clock_center")

        val textLooksLikeTime =
            text.matches(Regex("""\d{1,2}:\d{2}([\s:]*[AaPp][Mm])?"""))

        if ((idLooksLikeClock || textLooksLikeTime) &&
            className.contains("TextView") &&
            !TextUtils.isEmpty(text)
        ) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val found = findClockNode(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun removeOverlay() {
        overlay?.let { view ->
            runCatching {
                if (view.parent != null) {
                    (windowManager ?: getSystemService(WINDOW_SERVICE) as WindowManager)
                        .removeView(view)
                }
            }
        }
        overlay = null
    }

    private fun Rect.centerY(): Int = top + height() / 2
}

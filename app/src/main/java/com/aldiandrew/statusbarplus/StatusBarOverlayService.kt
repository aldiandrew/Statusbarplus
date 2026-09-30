package com.aldiandrew.statusbarplus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatusBarOverlayService : Service() {
    companion object {
        private const val CHANNEL_ID = "statusbar_overlay_service"
        private const val NOTIFICATION_ID = 1604

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, StatusBarOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StatusBarOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var textView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForegroundCompat()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!DayNotificationManager.isEnabled(this) || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        textView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        textView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        val view = textView ?: createTextView().also { textView = it }
        updateText(view)
        if (view.parent == null) {
            try {
                windowManager.addView(view, layoutParams())
            } catch (_: Exception) {
                stopSelf()
            }
        } else {
            try { windowManager.updateViewLayout(view, layoutParams()) } catch (_: Exception) {}
        }
    }

    private fun createTextView() = TextView(this).apply {
        setTextColor(Color.WHITE)
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        gravity = Gravity.CENTER
        includeFontPadding = true
        isClickable = false
        isFocusable = false
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun updateText(view: TextView) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val locale = Locale.getDefault()
        val now = Date()
        val day = SimpleDateFormat("EEE", locale).format(now)
        val date = SimpleDateFormat("d", locale).format(now)
        val month = SimpleDateFormat("MMM", locale).format(now)
        val mode = prefs.getString("display_mode", "day") ?: "day"
        view.text = when (mode) {
            "day_date" -> "$day\n$date"
            "date_month" -> "$date\n$month"
            else -> day
        }
        view.textSize = prefs.getFloat("text_size", 20f).coerceIn(12f, 22f)
        view.typeface = Typeface.create(FontManager.getTypeface(this), Typeface.BOLD)

        val density = resources.displayMetrics.density
        val padding = prefs.getFloat("layout_padding", 4f).coerceIn(0f, 18f)
        val horizontal = prefs.getFloat("horizontal_offset", 0f).coerceIn(-60f, 60f)
        val vertical = prefs.getFloat("vertical_offset", 0f).coerceIn(-20f, 20f)
        val spacing = prefs.getFloat("line_spacing", 0f).coerceIn(-6f, 12f)

        view.setPadding(
            (padding * density).toInt(),
            (padding * density).toInt(),
            (padding * density).toInt(),
            (padding * density).toInt()
        )
        view.translationX = horizontal * density
        view.translationY = vertical * density
        view.setLineSpacing(spacing * resources.displayMetrics.scaledDensity, 1f)
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        android.graphics.PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(58f)
        y = 0
    }

    private fun startForegroundCompat() {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_statusbarplus)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_service_notification))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.overlay_service_channel),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.overlay_service_channel_description)
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    private fun dp(value: Float) =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}

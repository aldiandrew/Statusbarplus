package com.aldiandrew.statusbarplus

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

object DayNotificationManager {
    private const val CHANNEL_ID = "day_status_bar"
    private const val NOTIFICATION_ID = 1601
    private const val ACTION_DAY_CHANGED = "com.aldiandrew.statusbarplus.DAY_CHANGED"

    fun show(context: Context) {
        if (!isEnabled(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val locale = Locale.getDefault()
        val shortDay = prefs.getBoolean("short_day", true)
        val mode = prefs.getString("display_mode", "day") ?: "day"
        val day = SimpleDateFormat(if (shortDay) "EEE" else "EEEE", locale).format(Date())
        val date = SimpleDateFormat("d", locale).format(Date())
        val month = SimpleDateFormat("MMM", locale).format(Date())
        val sizeSp = prefs.getFloat("text_size", 18f).coerceIn(12f, 22f)

        val icon = Icon.createWithBitmap(
            createTextIcon(context, day, date, month, mode, sizeSp)
        )

        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (mode) {
            "day_date" -> "$day $date"
            "day_date_month" -> "$day $date $month"
            "date_month" -> "$date $month"
            else -> day
        }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_description))
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        scheduleNextDay(context)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
        cancelAlarm(context)
    }

    fun isEnabled(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Hari di status bar",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Menampilkan hari sebagai ikon teks di status bar."
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    private fun createTextIcon(
        context: Context,
        day: String,
        date: String,
        month: String,
        mode: String,
        sizeSp: Float
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        // Keep every display mode on a single line. SystemUI gives notification
        // icons a fixed slot, so the complete text is fitted as one unit.
        val canvasSize = (48f * density).toInt().coerceAtLeast(144)

        val statusBarId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarId != 0) {
            context.resources.getDimensionPixelSize(statusBarId).toFloat()
        } else {
            24f * density
        }

        // Base the requested size on the device's own status-bar clock area.
        val systemClockSp = ((statusBarHeight / scaledDensity) * 0.68f).coerceIn(16f, 24f)
        val requested = sizeSp.coerceIn(12f, 22f)
        val requestedPx = (systemClockSp * (requested / 22f)) * scaledDensity

        val text = when (mode) {
            "day_date" -> "$day $date"
            "day_date_month" -> "$day $date $month"
            "date_month" -> "$date $month"
            else -> day
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = FontManager.getTypeface(context)
            textAlign = Paint.Align.CENTER
            textSize = requestedPx.coerceAtLeast(1f)
        }

        // Fit the complete line once instead of shrinking individual parts.
        // This prevents one part from being unnecessarily reduced because a
        // previous line was long.
        val maxWidth = canvasSize * 0.96f
        val measured = paint.measureText(text)
        if (measured > maxWidth && measured > 0f) {
            paint.textSize *= maxWidth / measured
        }

        val metrics = paint.fontMetrics
        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = canvasSize / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, canvasSize / 2f, baseline, paint)
        return bitmap
    }

    private fun scheduleNextDay(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        val next = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 2)
            set(Calendar.MILLISECOND, 0)
        }
        alarm.cancel(pending)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }

    private fun cancelAlarm(context: Context) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 1602,
        Intent(context, DayNotificationReceiver::class.java).setAction(ACTION_DAY_CHANGED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

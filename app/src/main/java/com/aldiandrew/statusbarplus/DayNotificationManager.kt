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
        val icon = Icon.createWithBitmap(
            createTextIcon(context, day, date, month, mode, prefs.getFloat("text_size", 20f))
        )
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(when (mode) {
                "day_date" -> "$day $date"
                "day_date_month" -> "$day $date $month"
                "date_month" -> "$date $month"
                else -> day
            })
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
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("enabled", false)

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Hari di status bar", NotificationManager.IMPORTANCE_LOW).apply {
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
        val requestedSp = sizeSp.coerceIn(12f, 22f)
        val multiLine = mode != "day"

        // SystemUI renders notification small icons in a fixed status-bar slot.
        // A square 24dp bitmap prevents the single-line glyph from being shifted
        // upward by a tall/non-square drawable.
        val iconSize = (32f * density).toInt().coerceAtLeast(96)
        val lines = when (mode) {
            "day_date" -> listOf(day, date)
            "day_date_month" -> listOf(day, "$date $month")
            "date_month" -> listOf(date, month)
            else -> listOf(day)
        }

        val desiredTextSize = requestedSp * scaledDensity
        val maxTextSize = if (multiLine) {
            (iconSize * 0.30f).coerceAtMost(10f * scaledDensity)
        } else {
            (iconSize * 0.56f).coerceAtMost(20f * scaledDensity)
        }
        var textSize = desiredTextSize.coerceAtMost(maxTextSize).coerceAtLeast(1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = FontManager.getTypeface(context)
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
        }

        if (!multiLine) {
            // Fit long weekday names without shrinking the entire drawable.
            val maxWidth = iconSize * 0.96f
            val measured = paint.measureText(day)
            if (measured > maxWidth && measured > 0f) {
                textSize *= maxWidth / measured
                paint.textSize = textSize
            }
        }

        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val metrics = paint.fontMetrics
        val lineHeight = metrics.descent - metrics.ascent

        if (!multiLine) {
            // Center the font's actual metrics, so the glyph sits on the same
            // vertical center line as the Android status-bar clock.
            val baseline = iconSize / 2f - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(day, iconSize / 2f, baseline, paint)
        } else {
            val gap = density
            val total = lineHeight * lines.size + gap * (lines.size - 1)
            var baseline = (iconSize - total) / 2f - metrics.ascent
            for (line in lines) {
                canvas.drawText(line, iconSize / 2f, baseline, paint)
                baseline += lineHeight + gap
            }
        }
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

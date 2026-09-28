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
import android.graphics.Typeface
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
        val mode = prefs.getString("display_mode", "day") ?: "day"
        val now = Date()
        val day = SimpleDateFormat("EEE", locale).format(now)
        val date = SimpleDateFormat("d", locale).format(now)
        val month = SimpleDateFormat("MMM", locale).format(now)
        val sizeSp = prefs.getFloat("text_size", 18f).coerceIn(12f, 22f)

        val icon = Icon.createWithBitmap(
            createTextIcon(context, day, date, month, mode, sizeSp, prefs)
        )

        val title = when (mode) {
            "day_date" -> "$day $date"
            "date_month" -> "$date $month"
            else -> day
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            1603,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentIntent(contentIntent)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_description))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        scheduleNextDay(context)
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
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
                    description = "Menampilkan informasi kalender sebagai ikon teks di status bar."
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
        sizeSp: Float,
        prefs: android.content.SharedPreferences
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        val canvasSize = (48f * density).toInt().coerceAtLeast(144)
        val requestedSp = sizeSp.coerceIn(12f, 22f)
        val padding = prefs.getFloat("layout_padding", 4f).coerceIn(0f, 18f)
        val horizontalOffset = prefs.getFloat("horizontal_offset", 0f).coerceIn(-12f, 12f) * density
        val verticalOffset = prefs.getFloat("vertical_offset", 0f).coerceIn(-12f, 12f) * density
        val lineSpacing = prefs.getFloat("line_spacing", 0f).coerceIn(-6f, 12f) * scaledDensity

        val lines = when (mode) {
            "day_date" -> listOf(day, date)
            "date_month" -> listOf(date, month)
            else -> listOf(day)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(FontManager.getTypeface(context), Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = requestedSp * scaledDensity
        }

        val maxWidth = canvasSize * (1f - (padding / 48f)).coerceIn(0.55f, 0.96f)
        val widestLine = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        if (widestLine > maxWidth && widestLine > 0f) {
            paint.textScaleX = (maxWidth / widestLine).coerceAtLeast(0.55f)
        }

        val fittedWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        if (fittedWidth > maxWidth && fittedWidth > 0f) {
            paint.textSize *= maxWidth / fittedWidth
            paint.textScaleX = 1f
        }

        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val metrics = paint.fontMetrics
        val x = canvasSize / 2f + horizontalOffset

        if (lines.size == 1) {
            val baseline = canvasSize / 2f - (metrics.ascent + metrics.descent) / 2f + verticalOffset
            canvas.drawText(lines[0], x, baseline, paint)
        } else {
            val lineHeight = (metrics.descent - metrics.ascent) + lineSpacing
            val totalHeight = lineHeight * 2f
            val firstBaseline = canvasSize / 2f - totalHeight / 2f - metrics.ascent + verticalOffset
            canvas.drawText(lines[0], x, firstBaseline, paint)
            canvas.drawText(lines[1], x, firstBaseline + lineHeight, paint)
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
        context,
        1602,
        Intent(context, DayNotificationReceiver::class.java).setAction(ACTION_DAY_CHANGED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

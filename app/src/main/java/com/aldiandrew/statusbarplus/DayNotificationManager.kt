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
        val shortDay = prefs.getBoolean("short_day", true)
        val mode = prefs.getString("display_mode", "day") ?: "day"
        val dayPattern = if (shortDay) "EEE" else "EEEE"
        val day = SimpleDateFormat(dayPattern, locale).format(Date())
        val date = SimpleDateFormat("d", locale).format(Date())
        val month = SimpleDateFormat("MMM", locale).format(Date())
        val icon = Icon.createWithBitmap(
            createTextIcon(
                context,
                day = day,
                date = date,
                month = month,
                mode = mode,
                sizeSp = prefs.getFloat("text_size", 20f)
            )
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

        // Small notification icons are normalized by SystemUI into a fixed slot.
        // Use a compact canvas and size the glyph against the device status-bar
        // height so the result tracks the visual scale of the system clock.
        val statusBarHeightPx = context.resources.run {
            val id = getIdentifier("status_bar_height", "dimen", "android")
            if (id != 0) getDimensionPixelSize(id) else (24f * density).toInt()
        }
        val statusBarDp = statusBarHeightPx / density
        val canvasHeightDp = if (multiLine) 48f else statusBarDp.coerceIn(24f, 36f)
        val canvasHeight = (canvasHeightDp * density).toInt()

        val lines = when (mode) {
            "day_date" -> listOf(day, date)
            "day_date_month" -> listOf(day, "$date $month")
            "date_month" -> listOf(date, month)
            else -> listOf(day)
        }

        val modeScale = when (mode) {
            "day" -> 1.45f
            "day_date_month", "date_month" -> 1.12f
            else -> 1f
        }
        val desiredTextSize = requestedSp * scaledDensity * modeScale
        val maxTextSize = if (multiLine) {
            // Keep two-line layouts inside the same status-bar slot. The selected
            // value remains the user's preference, but is automatically fitted
            // instead of being clipped.
            ((canvasHeightPx(canvasHeight, lines.size, density) / lines.size)
                .coerceAtMost(20f * scaledDensity))
        } else {
            canvasHeight * 0.84f
        }
        val textSize = desiredTextSize.coerceAtMost(maxTextSize).coerceAtLeast(1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
        }

        val maxLinePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
        }

        val metrics = paint.fontMetrics
        val lineHeight = (metrics.descent - metrics.ascent).coerceAtLeast(1f)
        val lineGap = 0f
        val horizontalPadding = 2f * density
        val width = (lines.maxOf { maxLinePaint.measureText(it) } + horizontalPadding * 2)
            .coerceAtLeast(20f * density).toInt()

        val bitmap = Bitmap.createBitmap(width, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val totalTextHeight = lineHeight * lines.size
        val firstBaseline = (canvasHeight - totalTextHeight) / 2f - metrics.ascent
        var baseline = firstBaseline
        for (line in lines) {
            canvas.drawText(line, width / 2f, baseline, paint)
            baseline += lineHeight + lineGap
        }
        return bitmap
    }

    private fun canvasHeightPx(heightPx: Int, lineCount: Int, density: Float): Float {
        val availablePerLine = heightPx / lineCount.toFloat()
        return (availablePerLine - 2f * density).coerceAtLeast(1f)
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
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        1602,
        Intent(context, DayNotificationReceiver::class.java).setAction(ACTION_DAY_CHANGED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
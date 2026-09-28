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

        /*
         * The reference app uses the notification small-icon slot as a tiny
         * text canvas.  Do the same here: render text directly into the icon
         * instead of using a normal notification glyph.
         *
         * IMPORTANT:
         * - "day" stays on one line.
         * - every other mode is deliberately two lines.
         * - the requested text size is NOT reduced based on the number of
         *   characters.  This keeps the glyph visually consistent with the
         *   device clock as far as Android's fixed notification slot allows.
         */
        val canvasSize = (48f * density).toInt().coerceAtLeast(144)
        val requestedSp = sizeSp.coerceIn(12f, 22f)
        val textSizePx = requestedSp * scaledDensity

        val lines = when (mode) {
            "day_date" -> listOf(day, date)
            "day_date_month" -> listOf(day, "$date $month")
            "date_month" -> listOf(date, month)
            else -> listOf(day)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = FontManager.getTypeface(context)
            textAlign = Paint.Align.CENTER
            textSize = textSizePx.coerceAtLeast(1f)
        }

        val horizontalPadding = canvasSize * 0.05f
        val maxWidth = canvasSize - horizontalPadding * 2f

        /*
         * Keep the same base font size for both lines. Only reduce it when a
         * particular line physically cannot fit inside the icon canvas.
         * This is different from the old implementation which derived a much
         * smaller size from status-bar height.
         */
        val widestLine = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        if (widestLine > maxWidth && widestLine > 0f) {
            paint.textSize *= maxWidth / widestLine
        }

        val bitmap = Bitmap.createBitmap(
            canvasSize,
            canvasSize,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val metrics = paint.fontMetrics

        if (lines.size == 1) {
            val baseline = canvasSize / 2f - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(lines[0], canvasSize / 2f, baseline, paint)
        } else {
            /*
             * Two lines use the full icon height.  The baseline positions are
             * calculated from real font metrics so the two lines stay visually
             * centered rather than being pushed toward the top/bottom.
             */
            val lineHeight = metrics.descent - metrics.ascent
            val totalHeight = lineHeight * 2f
            val firstBaseline = canvasSize / 2f - totalHeight / 2f - metrics.ascent
            val secondBaseline = firstBaseline + lineHeight
            canvas.drawText(lines[0], canvasSize / 2f, firstBaseline, paint)
            canvas.drawText(lines[1], canvasSize / 2f, secondBaseline, paint)
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

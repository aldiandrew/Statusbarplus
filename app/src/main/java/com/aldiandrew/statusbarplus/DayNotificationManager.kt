package com.aldiandrew.statusbarplus

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        if (StatusBarTextAccessibilityService.isEnabled(context)) {
            context.sendBroadcast(
                Intent(StatusBarTextAccessibilityService.ACTION_REFRESH)
                    .setPackage(context.packageName)
            )
            cancelNotificationOnly(context)
            scheduleNextDay(context)
            return
        }

        // Fallback mode: Android requires a small notification icon for a
        // notification status-bar entry. The date text itself is shown in
        // the notification content; no bitmap text rendering is used.
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val locale = Locale.getDefault()
        val shortDay = prefs.getBoolean("short_day", true)
        val mode = prefs.getString("display_mode", "day") ?: "day"
        val day = SimpleDateFormat(if (shortDay) "EEE" else "EEEE", locale).format(Date())
        val date = SimpleDateFormat("d", locale).format(Date())
        val month = SimpleDateFormat("MMM", locale).format(Date())

        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_statusbarplus)
            .setContentTitle(
                when (mode) {
                    "day_date" -> "$day $date"
                    "day_date_month" -> "$day $date $month"
                    "date_month" -> "$date $month"
                    else -> day
                }
            )
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
        cancelNotificationOnly(context)
        context.sendBroadcast(
            Intent(StatusBarTextAccessibilityService.ACTION_HIDE)
                .setPackage(context.packageName)
        )
        cancelAlarm(context)
    }

    private fun cancelNotificationOnly(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }

    fun isEnabled(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Statusbarplus",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Fallback notifikasi untuk Statusbarplus."
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
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
        context, 1602,
        Intent(context, DayNotificationReceiver::class.java).setAction(ACTION_DAY_CHANGED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

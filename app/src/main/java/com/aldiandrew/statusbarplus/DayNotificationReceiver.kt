package com.aldiandrew.statusbarplus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DayNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (DayNotificationManager.isEnabled(context)) DayNotificationManager.show(context)
    }
}
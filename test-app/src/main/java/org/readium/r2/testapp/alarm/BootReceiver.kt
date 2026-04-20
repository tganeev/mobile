package org.readium.r2.testapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as Application
                app.alarmPreferencesDataStore.alarmPreferencesFlow.collect { prefs ->
                    AlarmScheduler.rescheduleAllAlarms(context, prefs)
                }
            }
        }
    }
}
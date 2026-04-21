package org.readium.r2.testapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import org.readium.r2.testapp.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("AlarmReceiver", "=== ALARM TRIGGERED ===")
        android.util.Log.d("AlarmReceiver", "Time: ${System.currentTimeMillis()}")

        // Получаем Wakelock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmReceiver::wakelock"
        )
        wakeLock.acquire(30_000)

        try {
            val alarmType = intent.getStringExtra("alarm_type") ?: "morning"
            android.util.Log.d("AlarmReceiver", "Alarm type: $alarmType")

            // Запускаем Activity с максимальными флагами
            val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_type", alarmType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

            context.startActivity(alertIntent)
            android.util.Log.d("AlarmReceiver", "Activity started")

        } catch (e: Exception) {
            android.util.Log.e("AlarmReceiver", "Failed to start activity", e)
        } finally {
            wakeLock.release()
        }
    }
}
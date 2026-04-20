package org.readium.r2.testapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Получаем Wakelock, чтобы телефон не уснул во время обработки
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmReceiver::wakelock"
        )
        wakeLock.acquire(10_000) // 10 секунд максимум

        try {
            val alarmType = intent.getStringExtra("alarm_type") ?: return

            // Запускаем Activity с диалогом
            val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_type", alarmType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Для Android 8+ нужно использовать новые флаги
                alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(alertIntent)

        } finally {
            wakeLock.release()
        }
    }
}
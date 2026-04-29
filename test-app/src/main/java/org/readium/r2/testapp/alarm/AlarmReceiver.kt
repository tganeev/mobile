package org.readium.r2.testapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import org.readium.r2.testapp.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "=== ALARM TRIGGERED ===")
        createNotificationChannel(context)

        // Удерживаем CPU активным, пока будильник обрабатывается
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmReceiver::WakeLock"
        )
        wakeLock.acquire(30_000)

        try {
            val alarmType = intent.getStringExtra("alarm_type") ?: "morning"

            // Интент для полноэкранного запуска Activity
            val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_type", alarmType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (alarmType == "morning") "🌅 Доброе утро!" else "🌙 Спокойной ночи!"
            val message = if (alarmType == "morning") "Время просыпаться" else "Время ложиться спать"

            // Уведомление, которое откроется поверх блокировки
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false) // Запрещаем свайп, чтобы пользователь не потерял будильник
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true) // 🔑 Ключевой параметр
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            // Фоллбэк для Android < 10
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {
                Log.d("AlarmReceiver", "Direct start blocked, relying on full-screen intent")
            }

        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to handle alarm", e)
        } finally {
            wakeLock.release()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Будильник",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления для будильника"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
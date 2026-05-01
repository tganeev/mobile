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
import androidx.core.app.NotificationManagerCompat
import org.readium.r2.testapp.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "alarm_channel_important"
        const val NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "=== ALARM TRIGGERED ===")

        // 1. Создаем/обновляем канал уведомлений с MAX приоритетом
        createNotificationChannel(context)

        // 2. Удерживаем CPU активным, пока обрабатываем будильник
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmReceiver::WakeLock"
        )
        wakeLock.acquire(30_000) // 30 секунд

        try {
            val alarmType = intent.getStringExtra("alarm_type") ?: "morning"

            // 3. Интент для полноэкранной активности
            val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_type", alarmType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

            // PendingIntent должен быть IMMUTABLE для безопасности
            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                0,
                fullScreenIntent,
                pendingIntentFlags
            )

            val title = if (alarmType == "morning") "🌅 Доброе утро!" else "🌙 Спокойной ночи!"
            val message = if (alarmType == "morning") "Время просыпаться" else "Время ложиться спать"

            // 4. Создаем уведомление
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false) // Запрещаем свайп, пока пользователь не нажмет кнопку
                .setOngoing(true)
                // 🔑 Ключевой параметр: система сама запустит активность поверх блокировки
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build()

            // 5. Публикуем уведомление
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

            Log.d("AlarmReceiver", "Full-screen notification posted")

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
                "Будильник PKMS",
                NotificationManager.IMPORTANCE_HIGH // 🔥 Высокая важность обязательна
            ).apply {
                description = "Уведомления будильника с полноэкранным запуском"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true) // Обход режима "Не беспокоить"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
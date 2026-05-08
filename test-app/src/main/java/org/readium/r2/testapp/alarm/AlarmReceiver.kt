package org.readium.r2.testapp.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.AlarmType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1002

        @JvmStatic
        var mediaPlayer: MediaPlayer? = null
            private set

        private var vibrator: Vibrator? = null

        @JvmStatic
        fun stopAlarmSound() {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
                mediaPlayer = null

                vibrator?.cancel()
                vibrator = null
                Log.d("AlarmReceiver", "Alarm sound stopped")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to stop sound", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "=== ALARM TRIGGERED ===")

        // Логируем получение WakeLock
        AlarmLogger.logWakeLockAcquired()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmReceiver::WakeLock"
        )
        wakeLock.acquire(30_000)

        try {
            val alarmTypeString = intent.getStringExtra("alarm_type") ?: "morning"
            val alarmType = if (alarmTypeString == "morning") AlarmType.MORNING else AlarmType.EVENING
            val triggerTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            // Логируем срабатывание будильника
            AlarmLogger.logTriggered(
                alarmType = alarmType,
                triggerTime = triggerTime
            )

            // Проигрываем звук будильника
            playAlarmSound(context)

            // Вибрируем
            vibrateDevice(context)

            // Создаем канал уведомлений
            createNotificationChannel(context)

            // Создаем Intent для запуска Activity
            val activityIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_type", alarmTypeString)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                alarmTypeString.hashCode(),
                activityIntent,
                pendingIntentFlags
            )

            val title = if (alarmTypeString == "morning") "🌅 Доброе утро!" else "🌙 Спокойной ночи!"
            val message = if (alarmTypeString == "morning") "Нажмите, чтобы отметить подъём" else "Нажмите, чтобы отметить отбой"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(contentPendingIntent, true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d("AlarmReceiver", "Notification posted")
            AlarmLogger.logActivityShown(alarmType)

        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to handle alarm", e)
            AlarmLogger.logError(
                alarmType = null,
                message = "Ошибка обработки будильника: ${e.message}",
                exception = e
            )
        } finally {
            wakeLock.release()
            AlarmLogger.logWakeLockReleased()
        }
    }

    private fun playAlarmSound(context: Context) {
        try {
            stopAlarmSound()

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val uri = if (alarmSound != null) alarmSound else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d("AlarmReceiver", "Alarm sound playing")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to play sound", e)
            AlarmLogger.logError(
                alarmType = null,
                message = "Не удалось воспроизвести звук: ${e.message}",
                exception = e
            )
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500, 1000, 500, 1000),
                        intArrayOf(0, 255, 0, 255, 0, 255),
                        0
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), 0)
                }
            }
            Log.d("AlarmReceiver", "Vibration started")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Vibration failed", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Будильник PKMS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Будильник с отметкой времени"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
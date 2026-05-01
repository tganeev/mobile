package org.readium.r2.testapp.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import org.readium.r2.testapp.R

class AlarmSoundService : Service() {

    companion object {
        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val EXTRA_ALARM_TYPE = "alarm_type"

        private const val CHANNEL_ID = "alarm_sound_channel"
        private const val NOTIFICATION_ID = 1003

        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null
        private var wakeLock: PowerManager.WakeLock? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Получаем wakelock для удержания CPU во время воспроизведения
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmSoundService::WakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmType = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: "morning"
                // Приобретаем wakelock
                wakeLock?.acquire(60_000) // На 60 секунд
                startAlarm(alarmType)
                startForeground(NOTIFICATION_ID, createNotification(alarmType))
                Log.d("AlarmSoundService", "Alarm started with wake lock")
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                wakeLock?.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAlarm(alarmType: String) {
        try {
            stopAlarm()

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val uri = alarmSound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(this@AlarmSoundService, uri)
                isLooping = true
                prepare()
                start()
            }

            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
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

            Log.d("AlarmSoundService", "Alarm started successfully")
        } catch (e: Exception) {
            Log.e("AlarmSoundService", "Failed to start alarm", e)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null

            vibrator?.cancel()
            vibrator = null

            Log.d("AlarmSoundService", "Alarm stopped")
        } catch (e: Exception) {
            Log.e("AlarmSoundService", "Failed to stop alarm", e)
        }
    }

    private fun createNotification(alarmType: String): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Сервис будильника",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Воспроизведение звука будильника"
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (alarmType == "morning") "🌅 Утренний будильник" else "🌙 Вечерний будильник"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Будильник активен")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopAlarm()
        wakeLock?.release()
        super.onDestroy()
    }
}
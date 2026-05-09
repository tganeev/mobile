package org.readium.r2.testapp.yoga

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.readium.r2.testapp.MainActivity
import org.readium.r2.testapp.R

class YogaTimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "yoga_timer_channel"
        private const val NOTIFICATION_ID = 2001
        private val activeTimers = mutableMapOf<String, Job>()
        private val timerCallbacks = mutableMapOf<String, (Long) -> Unit>()
        private val completionCallbacks = mutableMapOf<String, () -> Unit>()

        fun startTimer(
            context: Context,
            timerId: String,
            durationSeconds: Long,
            onTick: (Long) -> Unit,
            onComplete: () -> Unit
        ) {
            timerCallbacks[timerId] = onTick
            completionCallbacks[timerId] = onComplete

            val intent = Intent(context, YogaTimerService::class.java).apply {
                putExtra("timer_id", timerId)
                putExtra("duration_seconds", durationSeconds)
                putExtra("action", "START")
            }
            context.startService(intent)
        }

        fun pauseTimer(context: Context, timerId: String) {
            val intent = Intent(context, YogaTimerService::class.java).apply {
                putExtra("timer_id", timerId)
                putExtra("action", "PAUSE")
            }
            context.startService(intent)
        }

        fun resumeTimer(context: Context, timerId: String) {
            val intent = Intent(context, YogaTimerService::class.java).apply {
                putExtra("timer_id", timerId)
                putExtra("action", "RESUME")
            }
            context.startService(intent)
        }

        fun stopTimer(context: Context, timerId: String) {
            val intent = Intent(context, YogaTimerService::class.java).apply {
                putExtra("timer_id", timerId)
                putExtra("action", "STOP")
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        val timerId = intent?.getStringExtra("timer_id") ?: return START_STICKY

        when (action) {
            "START" -> {
                val durationSeconds = intent.getLongExtra("duration_seconds", 0)
                startTimerInternal(timerId, durationSeconds)
            }
            "PAUSE" -> pauseTimerInternal(timerId)
            "RESUME" -> resumeTimerInternal(timerId)
            "STOP" -> stopTimerInternal(timerId)
        }

        return START_STICKY
    }

    private fun startTimerInternal(timerId: String, durationSeconds: Long) {
        stopTimerInternal(timerId)

        var remainingSeconds = durationSeconds

        val job = serviceScope.launch {
            while (remainingSeconds > 0 && isActive) {
                withContext(Dispatchers.Main) {
                    timerCallbacks[timerId]?.invoke(remainingSeconds)
                }
                delay(1000)
                remainingSeconds--
            }

            if (remainingSeconds == 0L) {
                withContext(Dispatchers.Main) {
                    completionCallbacks[timerId]?.invoke()
                }
                stopTimerInternal(timerId)
            }
        }

        activeTimers[timerId] = job
    }

    private fun pauseTimerInternal(timerId: String) {
        activeTimers[timerId]?.cancel()
        activeTimers.remove(timerId)
    }

    private fun resumeTimerInternal(timerId: String) {
        // Need to store remaining time separately for resume
        // Simplified: restart with stored remaining time
    }

    private fun stopTimerInternal(timerId: String) {
        activeTimers[timerId]?.cancel()
        activeTimers.remove(timerId)
        timerCallbacks.remove(timerId)
        completionCallbacks.remove(timerId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yoga Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления активных таймеров йоги"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yoga Timer")
            .setContentText("Активных таймеров: ${activeTimers.size}")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)  // Используем системную иконку
            .setContentIntent(pendingIntent)
            .setOngoing(activeTimers.isNotEmpty())
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
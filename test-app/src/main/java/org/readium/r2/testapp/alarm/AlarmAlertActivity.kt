package org.readium.r2.testapp.alarm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.R
import java.time.LocalDate
import java.time.LocalTime

class AlarmAlertActivity : Activity() {

    private lateinit var alarmType: String
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private var isActive = false
        private var alarmMediaPlayer: MediaPlayer? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isActive) {
            finish()
            return
        }
        isActive = true

        // Показываем поверх блокировки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmAlertActivity::wakelock"
        )
        wakeLock.acquire(30_000)

        setContentView(R.layout.activity_alarm_alert)

        alarmType = intent.getStringExtra("alarm_type") ?: "morning"

        setupUI()
        playAlarmSound()
        vibrate()

        wakeLock.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        stopAlarmSound()
    }

    private fun setupUI() {
        val titleText = findViewById<TextView>(R.id.alarmTitle)
        val messageText = findViewById<TextView>(R.id.alarmMessage)
        val primaryButton = findViewById<Button>(R.id.primaryButton)
        val secondaryButton = findViewById<Button>(R.id.secondaryButton)

        if (alarmType == "morning") {
            titleText.text = "🌅 Доброе утро!"
            messageText.text = "Отметить подъём?"
            primaryButton.text = "✅ Встаю"
            secondaryButton.text = "⏰ Остаюсь лежать"
        } else {
            titleText.text = "🌙 Спокойной ночи!"
            messageText.text = "Отметить отбой?"
            primaryButton.text = "✅ Ложусь"
            secondaryButton.text = "❌ Не ложусь"
        }

        primaryButton.setOnClickListener { onPrimaryAction() }
        secondaryButton.setOnClickListener { onSecondaryAction() }
    }

    private fun cancelNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(AlarmReceiver.NOTIFICATION_ID)
        } catch (e: Exception) { }
    }

    private fun onPrimaryAction() {
        stopAlarmSound()
        cancelNotification()

        scope.launch {
            val app = application as Application
            val now = LocalTime.now()
            val today = LocalDate.now()

            if (alarmType == "morning") {
                app.sleepRepository.saveWakeTime(today, now, isManual = false)
                app.alarmPreferencesDataStore.resetSnoozeCount()
            } else {
                app.sleepRepository.saveBedTime(today, now, isManual = false)
            }
        }

        finishAndRemoveTask()
    }

    private fun onSecondaryAction() {
        stopAlarmSound()
        cancelNotification()

        val snoozeMinutes = if (alarmType == "morning") 5 else 15

        scope.launch {
            val app = application as Application
            val today = LocalDate.now()

            if (alarmType == "evening") {
                app.sleepRepository.markBedTimeAsMissing(today)
            }

            var snoozeCount = 0
            app.alarmPreferencesDataStore.alarmPreferencesFlow.collect { prefs ->
                snoozeCount = prefs.snoozeCount
                return@collect
            }

            if (snoozeCount >= 2) {
                runOnUiThread {
                    AlertDialog.Builder(this@AlarmAlertActivity)
                        .setTitle("Напоминание")
                        .setMessage(if (alarmType == "morning")
                            "Вы несколько раз отложили будильник. Укажите фактическое время подъёма в статистике."
                        else
                            "Вы несколько раз отложили будильник. Укажите фактическое время отбоя в статистике.")
                        .setPositiveButton("OK") { _, _ -> finishAndRemoveTask() }
                        .setCancelable(false)
                        .show()
                }
            } else {
                AlarmScheduler.snoozeAlarm(this@AlarmAlertActivity, alarmType, snoozeMinutes)
                app.alarmPreferencesDataStore.incrementSnoozeCount()
                finishAndRemoveTask()
            }
        }

        finishAndRemoveTask()
    }

    private fun playAlarmSound() {
        try {
            stopAlarmSound()

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val uri = alarmSound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            alarmMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(this@AlarmAlertActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        try {
            alarmMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            alarmMediaPlayer = null
        } catch (e: Exception) { }
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(5000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(5000)
            }
        } catch (e: Exception) { }
    }
}
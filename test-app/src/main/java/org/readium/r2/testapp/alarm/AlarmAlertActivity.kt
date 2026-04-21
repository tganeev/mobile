package org.readium.r2.testapp.alarm

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.withContext

class AlarmAlertActivity : Activity() {

    private lateinit var alarmType: String
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Максимальные флаги для пробуждения экрана
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Пробуждаем устройство
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmAlertActivity::wakelock"
        )
        wakeLock.acquire(10_000)

        setContentView(R.layout.activity_alarm_alert)

        alarmType = intent.getStringExtra("alarm_type") ?: "morning"

        setupUI()
        playAlarmSound()
        vibrate()

        // Освобождаем wakelock через 10 секунд
        wakeLock.release()
    }

    private fun setupUI() {
        val titleText = findViewById<TextView>(R.id.alarmTitle)
        val messageText = findViewById<TextView>(R.id.alarmMessage)
        val primaryButton = findViewById<Button>(R.id.primaryButton)
        val secondaryButton = findViewById<Button>(R.id.secondaryButton)

        if (alarmType == "morning") {
            titleText.text = "🌅 Доброе утро!"
            messageText.text = "Время просыпаться"
            primaryButton.text = "Встаю"
            secondaryButton.text = "Остаюсь лежать"
        } else {
            titleText.text = "🌙 Спокойной ночи!"
            messageText.text = "Время ложиться спать"
            primaryButton.text = "Ложусь"
            secondaryButton.text = "Не ложусь"
        }

        primaryButton.setOnClickListener {
            onPrimaryAction()
        }

        secondaryButton.setOnClickListener {
            onSecondaryAction()
        }
    }

    private fun onPrimaryAction() {
        stopAlarm()

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

        finish()
    }

    private fun onSecondaryAction() {
        stopAlarm()

        val snoozeMinutes = if (alarmType == "morning") 5 else 15

        scope.launch {
            val app = application as Application
            val today = LocalDate.now()

            // Для вечернего будильника при выборе "Не ложусь" — помечаем как пропущенный
            if (alarmType == "evening") {
                // Сохраняем специальное значение, означающее "пропущено"
                app.sleepRepository.markBedTimeAsMissing(today)
            }

            // Получаем текущий счётчик откладываний
            var snoozeCount = 0
            app.alarmPreferencesDataStore.alarmPreferencesFlow.collect { prefs ->
                snoozeCount = prefs.snoozeCount
                return@collect
            }

            if (snoozeCount >= 2) {
                withContext(Dispatchers.Main) {
                    showReminderDialog()
                }
            } else {
                AlarmScheduler.snoozeAlarm(
                    this@AlarmAlertActivity,
                    alarmType,
                    snoozeMinutes
                )
                app.alarmPreferencesDataStore.incrementSnoozeCount()
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }

        finish()
    }

    private fun showReminderDialog() {
        runOnUiThread {
            val message = if (alarmType == "morning") {
                "Вы несколько раз отложили будильник. Пожалуйста, укажите фактическое время подъёма."
            } else {
                "Вы несколько раз отложили будильник. Пожалуйста, укажите фактическое время отбоя."
            }

            AlertDialog.Builder(this)
                .setTitle("Напоминание")
                .setMessage(message)
                .setPositiveButton("Указать время") { _, _ ->
                    finish()
                }
                .setNegativeButton("Позже") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun playAlarmSound() {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val uri = alarmSound ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
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

    private fun vibrate() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(3000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(3000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
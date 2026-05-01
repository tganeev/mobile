package org.readium.r2.testapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.readium.r2.testapp.data.AlarmPreferencesDataStore
import java.time.LocalTime
import java.util.Calendar

object AlarmScheduler {

    private const val REQUEST_CODE_MORNING = 1001
    private const val REQUEST_CODE_EVENING = 1002

    fun scheduleMorningAlarm(context: Context, time: LocalTime, enabled: Boolean) {
        if (enabled) {
            scheduleAlarm(context, time, REQUEST_CODE_MORNING, "morning")
        } else {
            cancelAlarm(context, REQUEST_CODE_MORNING)
            // Также останавливаем сервис если он запущен
            stopAlarmSoundService(context)
        }
    }

    fun scheduleEveningAlarm(context: Context, time: LocalTime, enabled: Boolean) {
        if (enabled) {
            scheduleAlarm(context, time, REQUEST_CODE_EVENING, "evening")
        } else {
            cancelAlarm(context, REQUEST_CODE_EVENING)
            stopAlarmSoundService(context)
        }
    }

    private fun scheduleAlarm(context: Context, time: LocalTime, requestCode: Int, type: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = calculateTriggerTime(time)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_type", type)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        try {
            // Используем setAlarmClock для максимальной надежности
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)
                android.util.Log.d("AlarmScheduler", "Alarm set with setAlarmClock for $type at ${java.util.Date(triggerTime)}")
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule alarm", e)
        }
    }

    private fun calculateTriggerTime(targetTime: LocalTime): Long {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val targetHour = targetTime.hour
        val targetMinute = targetTime.minute

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetHour < currentHour || (targetHour == currentHour && targetMinute <= currentMinute)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun stopAlarmSoundService(context: Context) {
        try {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                action = AlarmSoundService.ACTION_STOP_ALARM
            }
            context.stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Failed to stop sound service", e)
        }
    }

    fun snoozeAlarm(context: Context, type: String, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = if (type == "morning") REQUEST_CODE_MORNING else REQUEST_CODE_EVENING

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_type", type)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        }
        android.util.Log.d("AlarmScheduler", "Snooze set for ${minutes} minutes")
    }

    fun rescheduleAllAlarms(context: Context, prefs: AlarmPreferencesDataStore.AlarmPreferences) {
        scheduleMorningAlarm(context, prefs.morningTime, prefs.isMorningEnabled)
        scheduleEveningAlarm(context, prefs.eveningTime, prefs.isEveningEnabled)
    }
}
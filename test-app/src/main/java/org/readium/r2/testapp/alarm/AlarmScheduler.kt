package org.readium.r2.testapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.readium.r2.testapp.data.model.AlarmType
import java.time.LocalTime
import java.util.Calendar
import org.readium.r2.testapp.data.model.AlarmLogType

object AlarmScheduler {

    private const val REQUEST_CODE_MORNING = 1001
    private const val REQUEST_CODE_EVENING = 1002

    fun scheduleMorningAlarm(context: Context, time: LocalTime, enabled: Boolean) {
        val timeStr = String.format("%02d:%02d", time.hour, time.minute)

        AlarmLogger.log(
            type = if (enabled) AlarmLogType.SCHEDULED else AlarmLogType.CANCELLED,
            alarmType = AlarmType.MORNING,
            scheduledTime = timeStr,
            message = if (enabled) "Планирование утреннего будильника на $timeStr" else "Отмена утреннего будильника"
        )

        if (enabled) {
            scheduleAlarm(context, time, REQUEST_CODE_MORNING, "morning")
        } else {
            cancelAlarm(context, REQUEST_CODE_MORNING)
            stopAlarmSoundService(context)
        }
    }

    fun scheduleEveningAlarm(context: Context, time: LocalTime, enabled: Boolean) {
        val timeStr = String.format("%02d:%02d", time.hour, time.minute)

        AlarmLogger.log(
            type = if (enabled) AlarmLogType.SCHEDULED else AlarmLogType.CANCELLED,
            alarmType = AlarmType.EVENING,
            scheduledTime = timeStr,
            message = if (enabled) "Планирование вечернего будильника на $timeStr" else "Отмена вечернего будильника"
        )

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
        val alarmTypeEnum = if (type == "morning") AlarmType.MORNING else AlarmType.EVENING

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
            // Проверка наличия разрешения на точные будильники (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    AlarmLogger.logPermissionMissing("SCHEDULE_EXACT_ALARM")
                    return
                }
            }

            // Используем setAlarmClock для максимальной надежности
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmInfo, pendingIntent)

                AlarmLogger.log(
                    type = AlarmLogType.SCHEDULED,
                    alarmType = alarmTypeEnum,
                    scheduledTime = String.format("%02d:%02d", time.hour, time.minute),
                    triggerTime = java.util.Date(triggerTime).toString(),
                    message = "AlarmClock установлен на ${java.util.Date(triggerTime)}"
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                AlarmLogger.log(
                    type = AlarmLogType.SCHEDULED,
                    alarmType = alarmTypeEnum,
                    scheduledTime = String.format("%02d:%02d", time.hour, time.minute),
                    message = "set() установлен на ${java.util.Date(triggerTime)}"
                )
            }
        } catch (e: SecurityException) {
            AlarmLogger.logError(
                alarmType = alarmTypeEnum,
                message = "SecurityException при планировании: ${e.message}",
                exception = e
            )
        } catch (e: Exception) {
            AlarmLogger.logError(
                alarmType = alarmTypeEnum,
                message = "Ошибка планирования будильника: ${e.message}",
                exception = e
            )
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

        // Если время уже прошло сегодня, планируем на завтра
        if (targetHour < currentHour || (targetHour == currentHour && targetMinute <= currentMinute)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            AlarmLogger.log(
                type = AlarmLogType.SCHEDULED,
                alarmType = null,
                message = "Время уже прошло сегодня, планируем на завтра"
            )
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

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            AlarmLogger.log(
                type = AlarmLogType.CANCELLED,
                alarmType = if (requestCode == REQUEST_CODE_MORNING) AlarmType.MORNING else AlarmType.EVENING,
                message = "Будильник отменён через AlarmManager.cancel()"
            )
        } catch (e: Exception) {
            AlarmLogger.logError(
                alarmType = if (requestCode == REQUEST_CODE_MORNING) AlarmType.MORNING else AlarmType.EVENING,
                message = "Ошибка при отмене будильника: ${e.message}",
                exception = e
            )
        }
    }

    private fun stopAlarmSoundService(context: Context) {
        try {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                action = AlarmSoundService.ACTION_STOP_ALARM
            }
            context.stopService(intent)
            AlarmLogger.log(
                type = AlarmLogType.CANCELLED,
                alarmType = null,
                message = "AlarmSoundService остановлен"
            )
        } catch (e: Exception) {
            AlarmLogger.logError(
                alarmType = null,
                message = "Ошибка остановки звукового сервиса: ${e.message}",
                exception = e
            )
        }
    }

    fun snoozeAlarm(context: Context, type: String, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = if (type == "morning") REQUEST_CODE_MORNING else REQUEST_CODE_EVENING
        val alarmTypeEnum = if (type == "morning") AlarmType.MORNING else AlarmType.EVENING

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

        try {
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

            AlarmLogger.logSnoozed(alarmTypeEnum, minutes)
            android.util.Log.d("AlarmScheduler", "Snooze set for ${minutes} minutes")
        } catch (e: Exception) {
            AlarmLogger.logError(
                alarmType = alarmTypeEnum,
                message = "Ошибка при откладывании будильника: ${e.message}",
                exception = e
            )
        }
    }

    fun rescheduleAllAlarms(context: Context, prefs: org.readium.r2.testapp.data.AlarmPreferencesDataStore.AlarmPreferences) {
        AlarmLogger.log(
            type = AlarmLogType.SCHEDULED,
            alarmType = null,
            message = "Перепланирование всех будильников после перезагрузки"
        )
        scheduleMorningAlarm(context, prefs.morningTime, prefs.isMorningEnabled)
        scheduleEveningAlarm(context, prefs.eveningTime, prefs.isEveningEnabled)
    }
}
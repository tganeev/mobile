package org.readium.r2.testapp.alarm

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.data.model.AlarmLog
import org.readium.r2.testapp.data.model.AlarmLogType
import org.readium.r2.testapp.data.model.AlarmType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlarmLogger {

    private var app: Application? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun init(context: Context) {
        app = context.applicationContext as Application
    }

    fun log(
        type: AlarmLogType,
        alarmType: AlarmType? = null,
        scheduledTime: String? = null,
        triggerTime: String? = null,
        message: String,
        exception: Throwable? = null
    ) {
        scope.launch {
            try {
                val log = AlarmLog(
                    type = type,
                    alarmType = alarmType,
                    scheduledTime = scheduledTime,
                    triggerTime = triggerTime,
                    message = message,
                    exception = exception?.stackTraceToString()
                )
                app?.bookRepository?.insertAlarmLog(log)

                // Также пишем в лог Android для быстрой отладки
                android.util.Log.d("AlarmLogger", "[${type.name}] $message")
            } catch (e: Exception) {
                android.util.Log.e("AlarmLogger", "Failed to save log", e)
            }
        }
    }

    fun logScheduled(alarmType: AlarmType, scheduledTime: String) {
        log(
            type = AlarmLogType.SCHEDULED,
            alarmType = alarmType,
            scheduledTime = scheduledTime,
            message = "Будильник запланирован на $scheduledTime"
        )
    }

    fun logTriggered(alarmType: AlarmType, triggerTime: String) {
        log(
            type = AlarmLogType.TRIGGERED,
            alarmType = alarmType,
            triggerTime = triggerTime,
            message = "Будильник сработал в $triggerTime"
        )
    }

    fun logSnoozed(alarmType: AlarmType, snoozeMinutes: Int) {
        log(
            type = AlarmLogType.SNOOZED,
            alarmType = alarmType,
            message = "Будильник отложен на $snoozeMinutes минут"
        )
    }

    fun logCancelled(alarmType: AlarmType) {
        log(
            type = AlarmLogType.CANCELLED,
            alarmType = alarmType,
            message = "Будильник отменён пользователем"
        )
    }

    fun logError(alarmType: AlarmType?, message: String, exception: Throwable? = null) {
        log(
            type = AlarmLogType.ERROR,
            alarmType = alarmType,
            message = message,
            exception = exception
        )
    }

    fun logPermissionMissing(permission: String) {
        log(
            type = AlarmLogType.PERMISSION_MISSING,
            message = "Отсутствует разрешение: $permission"
        )
    }

    fun logWakeLockAcquired() {
        log(
            type = AlarmLogType.WAKE_LOCK_ACQUIRED,
            message = "WakeLock получен"
        )
    }

    fun logWakeLockReleased() {
        log(
            type = AlarmLogType.WAKE_LOCK_RELEASED,
            message = "WakeLock освобождён"
        )
    }

    fun logActivityShown(alarmType: AlarmType) {
        log(
            type = AlarmLogType.ALARM_ACTIVITY_SHOWN,
            alarmType = alarmType,
            message = "Диалог будильника показан"
        )
    }
}
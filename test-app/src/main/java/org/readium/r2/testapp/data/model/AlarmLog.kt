package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_logs")
data class AlarmLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "type")
    val type: AlarmLogType,  // SCHEDULED, TRIGGERED, SNOOZED, CANCELLED, ERROR, PERMISSION_MISSING

    @ColumnInfo(name = "alarm_type")
    val alarmType: AlarmType?,  // MORNING, EVENING

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: String?,  // Формат "HH:MM"

    @ColumnInfo(name = "trigger_time")
    val triggerTime: String?,  // Формат "HH:MM:ss"

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "exception")
    val exception: String? = null
)

enum class AlarmLogType {
    SCHEDULED,      // Будильник запланирован
    TRIGGERED,      // Будильник сработал
    SNOOZED,        // Отложен
    CANCELLED,      // Отменён
    ERROR,          // Ошибка при планировании/срабатывании
    PERMISSION_MISSING, // Отсутствует разрешение
    WAKE_LOCK_ACQUIRED, // WakeLock получен
    WAKE_LOCK_RELEASED, // WakeLock освобождён
    ALARM_ACTIVITY_SHOWN, // Диалог показан
    ALARM_ACTIVITY_DISMISSED // Диалог закрыт
}

enum class AlarmType {
    MORNING, EVENING
}
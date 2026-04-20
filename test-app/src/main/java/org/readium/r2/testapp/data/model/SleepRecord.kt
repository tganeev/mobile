package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "wake_time")
    val wakeTime: LocalTime? = null,

    @ColumnInfo(name = "bed_time")
    val bedTime: LocalTime? = null,

    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Вспомогательные методы для обновления (возвращают новую копию)
    fun withWakeTime(wakeTime: LocalTime?, isManual: Boolean): SleepRecord {
        return this.copy(
            wakeTime = wakeTime,
            isManual = isManual || this.isManual,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
    }

    fun withBedTime(bedTime: LocalTime?, isManual: Boolean): SleepRecord {
        return this.copy(
            bedTime = bedTime,
            isManual = isManual || this.isManual,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
    }

    fun markSynced(): SleepRecord {
        return this.copy(synced = true)
    }
}
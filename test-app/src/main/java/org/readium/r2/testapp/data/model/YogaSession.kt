package org.readium.r2.testapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "yoga_sessions")
data class YogaSession(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "practice_name")
    val practiceName: String,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "completed")
    val completed: Boolean = true
)
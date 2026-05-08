package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.AlarmLog

@Dao
interface AlarmLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AlarmLog): Long

    @Query("SELECT * FROM alarm_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AlarmLog>>

    @Query("SELECT * FROM alarm_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<AlarmLog>>

    @Query("DELETE FROM alarm_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteLogsOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM alarm_logs")
    suspend fun deleteAllLogs()
}
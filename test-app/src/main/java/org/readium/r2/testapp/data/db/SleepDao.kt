package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.SleepRecord
import java.time.LocalDate

@Dao
interface SleepDao {

    @Query("SELECT * FROM sleep_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records WHERE date = :date")
    suspend fun getRecordByDate(date: LocalDate): SleepRecord?

    @Query("SELECT * FROM sleep_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SleepRecord): Long

    @Update
    suspend fun updateRecord(record: SleepRecord)

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("SELECT * FROM sleep_records WHERE synced = 0")
    suspend fun getUnsyncedRecords(): List<SleepRecord>

    @Query("UPDATE sleep_records SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT * FROM sleep_records WHERE id = :id")
    suspend fun getRecordById(id: Long): SleepRecord?
}
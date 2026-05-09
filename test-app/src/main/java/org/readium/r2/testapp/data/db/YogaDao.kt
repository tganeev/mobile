package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.YogaSession
import java.time.LocalDate

@Dao
interface YogaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: YogaSession): Long

    @Query("SELECT * FROM yoga_sessions ORDER BY date DESC, start_time DESC")
    fun getAllSessions(): Flow<List<YogaSession>>

    @Query("SELECT * FROM yoga_sessions WHERE date = :date ORDER BY start_time DESC")
    fun getSessionsByDate(date: LocalDate): Flow<List<YogaSession>>

    @Query("SELECT SUM(duration_seconds) FROM yoga_sessions WHERE practice_name = :practiceName")
    suspend fun getTotalDurationForPractice(practiceName: String): Long?

    @Query("""
        SELECT practice_name, SUM(duration_seconds) as total_seconds, COUNT(*) as session_count 
        FROM yoga_sessions 
        GROUP BY practice_name
    """)
    fun getPracticeStats(): Flow<List<YogaPracticeStat>>

    @Query("DELETE FROM yoga_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}

data class YogaPracticeStat(
    val practice_name: String,
    val total_seconds: Long,
    val session_count: Int
)
package org.readium.r2.testapp.data

import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.db.SleepDao
import org.readium.r2.testapp.data.model.SleepRecord
import java.time.LocalDate
import java.time.LocalTime

class SleepRepository(
    private val sleepDao: SleepDao
) {

    fun getAllRecords(): Flow<List<SleepRecord>> = sleepDao.getAllRecords()

    fun getRecordsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepRecord>> =
        sleepDao.getRecordsInRange(startDate, endDate)

    suspend fun getRecordByDate(date: LocalDate): SleepRecord? =
        sleepDao.getRecordByDate(date)

    suspend fun saveWakeTime(date: LocalDate, wakeTime: LocalTime, isManual: Boolean = false) {
        val existing = sleepDao.getRecordByDate(date)
        if (existing != null) {
            sleepDao.updateRecord(existing.withWakeTime(wakeTime, isManual))
        } else {
            val record = SleepRecord(
                date = date,
                wakeTime = wakeTime,
                isManual = isManual
            )
            sleepDao.insertRecord(record)
        }
    }

    suspend fun saveBedTime(date: LocalDate, bedTime: LocalTime, isManual: Boolean = false) {
        val existing = sleepDao.getRecordByDate(date)
        if (existing != null) {
            sleepDao.updateRecord(existing.withBedTime(bedTime, isManual))
        } else {
            val record = SleepRecord(
                date = date,
                bedTime = bedTime,
                isManual = isManual
            )
            sleepDao.insertRecord(record)
        }
    }

    suspend fun deleteRecord(id: Long) = sleepDao.deleteRecord(id)

    suspend fun getUnsyncedRecords(): List<SleepRecord> = sleepDao.getUnsyncedRecords()

    suspend fun markAsSynced(ids: List<Long>) {
        val records = ids.mapNotNull { id ->
            sleepDao.getRecordById(id)?.markSynced()
        }
        records.forEach { sleepDao.updateRecord(it) }
    }

    suspend fun calculateSleepDuration(date: LocalDate): Long? {
        val record = sleepDao.getRecordByDate(date) ?: return null
        val bedTime = record.bedTime ?: return null

        val nextDayRecord = sleepDao.getRecordByDate(date.plusDays(1))
        val wakeTime = nextDayRecord?.wakeTime ?: return null

        val bedMinutes = bedTime.hour * 60 + bedTime.minute
        val wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

        val durationMinutes = if (wakeMinutes >= bedMinutes) {
            wakeMinutes - bedMinutes
        } else {
            (24 * 60 - bedMinutes) + wakeMinutes
        }

        return durationMinutes.toLong()
    }
}
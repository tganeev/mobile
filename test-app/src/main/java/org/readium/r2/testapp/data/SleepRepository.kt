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
            val updated = existing.copy(
                wakeTime = wakeTime,
                isManual = isManual || existing.isManual,
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            sleepDao.updateRecord(updated)
        } else {
            val record = SleepRecord(
                date = date,
                wakeTime = wakeTime,
                isManual = isManual,
                synced = false
            )
            sleepDao.insertRecord(record)
        }
    }

    suspend fun saveBedTime(date: LocalDate, bedTime: LocalTime, isManual: Boolean = false) {
        val existing = sleepDao.getRecordByDate(date)
        if (existing != null) {
            val updated = existing.copy(
                bedTime = bedTime,
                isManual = isManual || existing.isManual,
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            sleepDao.updateRecord(updated)
        } else {
            val record = SleepRecord(
                date = date,
                bedTime = bedTime,
                isManual = isManual,
                synced = false
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

    /**
     * Отмечает, что пользователь пропустил фиксацию времени отбоя
     * (выбрал "Не ложусь" и больше не указал время)
     */
    suspend fun markBedTimeAsMissing(date: LocalDate) {
        val existing = sleepDao.getRecordByDate(date)
        if (existing != null) {
            // Если запись уже существует, просто помечаем, что время не указано
            // (оставляем bedTime = null, но можно добавить флаг isMissing)
            val updated = existing.copy(
                bedTime = null,
                isManual = false,
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            sleepDao.updateRecord(updated)
        } else {
            // Создаём запись с null для bedTime (означает "пропущено")
            val record = SleepRecord(
                date = date,
                bedTime = null,
                isManual = false,
                synced = false
            )
            sleepDao.insertRecord(record)
        }
    }

    /**
     * Отмечает, что пользователь пропустил фиксацию времени подъёма
     * (выбрал "Остаюсь лежать" и больше не указал время)
     */
    suspend fun markWakeTimeAsMissing(date: LocalDate) {
        val existing = sleepDao.getRecordByDate(date)
        if (existing != null) {
            val updated = existing.copy(
                wakeTime = null,
                isManual = false,
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            sleepDao.updateRecord(updated)
        } else {
            val record = SleepRecord(
                date = date,
                wakeTime = null,
                isManual = false,
                synced = false
            )
            sleepDao.insertRecord(record)
        }
    }

    suspend fun updateSleepRecord(id: Long, date: LocalDate, wakeTime: LocalTime?, bedTime: LocalTime?) {
        val existing = sleepDao.getRecordById(id)
        if (existing != null) {
            val updated = existing.copy(
                date = date,
                wakeTime = wakeTime,
                bedTime = bedTime,
                isManual = true,  // Отмечаем как ручное редактирование
                updatedAt = System.currentTimeMillis(),
                synced = false
            )
            sleepDao.updateRecord(updated)
        }
    }
}
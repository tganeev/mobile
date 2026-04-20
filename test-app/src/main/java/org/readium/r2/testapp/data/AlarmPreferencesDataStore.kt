package org.readium.r2.testapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alarm_preferences")

class AlarmPreferencesDataStore(private val context: Context) {

    companion object {
        private val MORNING_ENABLED = booleanPreferencesKey("morning_enabled")
        private val MORNING_HOUR = longPreferencesKey("morning_hour")
        private val MORNING_MINUTE = longPreferencesKey("morning_minute")
        private val EVENING_ENABLED = booleanPreferencesKey("evening_enabled")
        private val EVENING_HOUR = longPreferencesKey("evening_hour")
        private val EVENING_MINUTE = longPreferencesKey("evening_minute")
        private val SNOOZE_COUNT = longPreferencesKey("snooze_count")
        private val LAST_SNOOZE_DATE = longPreferencesKey("last_snooze_date")
    }

    data class AlarmPreferences(
        val isMorningEnabled: Boolean = true,
        val morningTime: LocalTime = LocalTime.of(7, 0),
        val isEveningEnabled: Boolean = true,
        val eveningTime: LocalTime = LocalTime.of(23, 0),
        val snoozeCount: Int = 0,
        val lastSnoozeDate: Long = 0
    )

    val alarmPreferencesFlow: Flow<AlarmPreferences> = context.dataStore.data.map { prefs ->
        AlarmPreferences(
            isMorningEnabled = prefs[MORNING_ENABLED] ?: true,
            morningTime = LocalTime.of(
                (prefs[MORNING_HOUR] ?: 7).toInt(),
                (prefs[MORNING_MINUTE] ?: 0).toInt()
            ),
            isEveningEnabled = prefs[EVENING_ENABLED] ?: true,
            eveningTime = LocalTime.of(
                (prefs[EVENING_HOUR] ?: 23).toInt(),
                (prefs[EVENING_MINUTE] ?: 0).toInt()
            ),
            snoozeCount = (prefs[SNOOZE_COUNT] ?: 0).toInt(),
            lastSnoozeDate = prefs[LAST_SNOOZE_DATE] ?: 0
        )
    }

    suspend fun updateMorningAlarm(enabled: Boolean, time: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[MORNING_ENABLED] = enabled
            prefs[MORNING_HOUR] = time.hour.toLong()
            prefs[MORNING_MINUTE] = time.minute.toLong()
        }
    }

    suspend fun updateEveningAlarm(enabled: Boolean, time: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[EVENING_ENABLED] = enabled
            prefs[EVENING_HOUR] = time.hour.toLong()
            prefs[EVENING_MINUTE] = time.minute.toLong()
        }
    }

    suspend fun incrementSnoozeCount() {
        context.dataStore.edit { prefs ->
            val current = (prefs[SNOOZE_COUNT] ?: 0).toInt()
            prefs[SNOOZE_COUNT] = (current + 1).toLong()
            prefs[LAST_SNOOZE_DATE] = System.currentTimeMillis()
        }
    }

    suspend fun resetSnoozeCount() {
        context.dataStore.edit { prefs ->
            prefs[SNOOZE_COUNT] = 0
            prefs[LAST_SNOOZE_DATE] = 0
        }
    }
}
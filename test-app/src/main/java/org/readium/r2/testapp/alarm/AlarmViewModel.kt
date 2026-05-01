package org.readium.r2.testapp.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.AlarmPreferencesDataStore
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.SleepRecord

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val preferences = app.alarmPreferencesDataStore

    private val _toastMessage = MutableStateFlow("")
    val toastMessage = _toastMessage.asStateFlow()
    val allSleepRecords: Flow<List<SleepRecord>> = app.sleepRepository.getAllRecords()

    val alarmPreferences = preferences.alarmPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmPreferencesDataStore.AlarmPreferences()
        )

    fun updateMorningAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val current = alarmPreferences.value
                preferences.updateMorningAlarm(enabled, current.morningTime)

                // Планируем или отменяем будильник
                AlarmScheduler.scheduleMorningAlarm(
                    getApplication(),
                    current.morningTime,
                    enabled  // enabled уже Boolean, все правильно
                )

                _toastMessage.value = if (enabled) "Утренний будильник включён" else "Утренний будильник выключен"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
                android.util.Log.e("AlarmViewModel", "Failed to update morning alarm", e)
            }
        }
    }

    fun updateEveningAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val current = alarmPreferences.value
                preferences.updateEveningAlarm(enabled, current.eveningTime)

                AlarmScheduler.scheduleEveningAlarm(
                    getApplication(),
                    current.eveningTime,
                    enabled  // enabled уже Boolean, все правильно
                )

                _toastMessage.value = if (enabled) "Вечерний будильник включён" else "Вечерний будильник выключен"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
                android.util.Log.e("AlarmViewModel", "Failed to update evening alarm", e)
            }
        }
    }

    fun updateMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val current = alarmPreferences.value
                val newTime = LocalTime.of(hour, minute)
                preferences.updateMorningAlarm(current.isMorningEnabled, newTime)

                AlarmScheduler.scheduleMorningAlarm(
                    getApplication(),
                    newTime,
                    current.isMorningEnabled  // это Boolean, все правильно
                )

                _toastMessage.value = "Утренний будильник установлен на ${String.format("%02d:%02d", hour, minute)}"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
                android.util.Log.e("AlarmViewModel", "Failed to update morning time", e)
            }
        }
    }

    fun updateEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val current = alarmPreferences.value
                val newTime = LocalTime.of(hour, minute)
                preferences.updateEveningAlarm(current.isEveningEnabled, newTime)

                AlarmScheduler.scheduleEveningAlarm(
                    getApplication(),
                    newTime,
                    current.isEveningEnabled  // это Boolean, все правильно
                )

                _toastMessage.value = "Вечерний будильник установлен на ${String.format("%02d:%02d", hour, minute)}"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
                android.util.Log.e("AlarmViewModel", "Failed to update evening time", e)
            }
        }
    }

    fun toastCleared() {
        _toastMessage.value = ""
    }

    fun saveWakeTimeManual(date: LocalDate, time: LocalTime) {
        viewModelScope.launch {
            try {
                app.sleepRepository.saveWakeTime(date, time, isManual = true)
                _toastMessage.value = "Время подъёма сохранено"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun saveBedTimeManual(date: LocalDate, time: LocalTime) {
        viewModelScope.launch {
            try {
                app.sleepRepository.saveBedTime(date, time, isManual = true)
                _toastMessage.value = "Время отбоя сохранено"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun deleteSleepRecord(id: Long) {
        viewModelScope.launch {
            app.sleepRepository.deleteRecord(id)
            _toastMessage.value = "Запись удалена"
        }
    }

    fun updateSleepRecord(id: Long, date: LocalDate, wakeTime: LocalTime?, bedTime: LocalTime?) {
        viewModelScope.launch {
            try {
                app.sleepRepository.updateSleepRecord(id, date, wakeTime, bedTime)
                _toastMessage.value = "Запись обновлена"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка: ${e.message}"
            }
        }
    }
}
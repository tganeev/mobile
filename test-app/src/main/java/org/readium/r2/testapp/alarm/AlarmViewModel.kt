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
import org.readium.r2.testapp.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.SleepRecord

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    private val preferences = app.alarmPreferencesDataStore

    private val _toastMessage = MutableStateFlow("")
    val toastMessage = _toastMessage.asStateFlow()
    val allSleepRecords: Flow<List<SleepRecord>> = app.sleepRepository.getAllRecords()

    // Исправлено: полный путь к AlarmPreferences
    val alarmPreferences = preferences.alarmPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmPreferencesDataStore.AlarmPreferences()  // <-- ИСПРАВЛЕНО
        )

    fun updateMorningAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = alarmPreferences.value
            preferences.updateMorningAlarm(enabled, current.morningTime)

            // Планируем или отменяем будильник
            AlarmScheduler.scheduleMorningAlarm(
                getApplication(),
                current.morningTime,
                enabled
            )

            _toastMessage.value = if (enabled) "Утренний будильник включён" else "Утренний будильник выключен"
        }
    }

    fun updateEveningAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = alarmPreferences.value
            preferences.updateEveningAlarm(enabled, current.eveningTime)
            _toastMessage.value = if (enabled) "Вечерний будильник включён" else "Вечерний будильник выключен"
        }
    }

    fun updateMorningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = alarmPreferences.value
            val newTime = LocalTime.of(hour, minute)
            preferences.updateMorningAlarm(current.isMorningEnabled, newTime)

            // Перепланируем будильник
            AlarmScheduler.scheduleMorningAlarm(
                getApplication(),
                newTime,
                current.isMorningEnabled
            )

            _toastMessage.value = "Утренний будильник установлен на ${String.format("%02d:%02d", hour, minute)}"
        }
    }

    fun updateEveningTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = alarmPreferences.value
            val newTime = LocalTime.of(hour, minute)
            preferences.updateEveningAlarm(current.isEveningEnabled, newTime)
            _toastMessage.value = "Вечерний будильник установлен на ${String.format("%02d:%02d", hour, minute)}"
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
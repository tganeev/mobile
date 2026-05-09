package org.readium.r2.testapp.yoga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.YogaPractice
import org.readium.r2.testapp.data.model.YogaPractices

data class YogaUiState(
    val activeTimers: List<ActiveTimer> = emptyList(),
    val selectedPractice: YogaPractice? = YogaPractices.practices.firstOrNull(),
    val selectedDurationMinutes: Int = 20,
    val isAnyTimerRunning: Boolean = false
)

class YogaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _uiState = MutableStateFlow(YogaUiState())
    val uiState: StateFlow<YogaUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun selectPractice(practice: YogaPractice) {
        _uiState.value = _uiState.value.copy(
            selectedPractice = practice,
            selectedDurationMinutes = practice.defaultDurationMinutes
        )
    }

    fun updateDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(selectedDurationMinutes = minutes)
    }

    fun startTimer() {
        val practice = _uiState.value.selectedPractice ?: return
        val durationSeconds = _uiState.value.selectedDurationMinutes * 60L

        val newTimer = ActiveTimer(
            practice = practice,
            remainingSeconds = durationSeconds,
            isRunning = true
        )

        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers + newTimer,
            isAnyTimerRunning = true
        )

        // Сохраняем сессию при старте
        saveSessionStart(practice.name, durationSeconds)
    }

    fun pauseTimer(timerId: String) {
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.map { timer ->
                if (timer.id == timerId) timer.copy(isRunning = false)
                else timer
            }
        )
    }

    fun resumeTimer(timerId: String) {
        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.map { timer ->
                if (timer.id == timerId) timer.copy(isRunning = true)
                else timer
            }
        )
    }

    fun stopTimer(timerId: String) {
        val timer = _uiState.value.activeTimers.find { it.id == timerId }

        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers.filter { it.id != timerId },
            isAnyTimerRunning = _uiState.value.activeTimers.size > 1
        )

        // Сохраняем сессию при остановке
        timer?.let {
            saveSessionComplete(it.practice.name, it.elapsedSeconds)
        }
    }

    private fun saveSessionStart(practiceName: String, durationSeconds: Long) {
        viewModelScope.launch {
            // Можно сохранять начало сессии, но для простоты сохраняем при завершении
        }
    }

    fun startTimer(practice: YogaPractice, remainingSeconds: Long) {
        val newTimer = ActiveTimer(
            practice = practice,
            remainingSeconds = remainingSeconds,
            isRunning = true
        )

        _uiState.value = _uiState.value.copy(
            activeTimers = _uiState.value.activeTimers + newTimer,
            isAnyTimerRunning = true
        )
    }

    private fun saveSessionComplete(practiceName: String, elapsedSeconds: Long) {
        viewModelScope.launch {
            val session = org.readium.r2.testapp.data.model.YogaSession(
                practiceName = practiceName,
                durationSeconds = elapsedSeconds,
                startTime = System.currentTimeMillis() - (elapsedSeconds * 1000),
                endTime = System.currentTimeMillis(),
                date = java.time.LocalDate.now(),
                completed = true
            )
            app.bookRepository.insertYogaSession(session)
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            app.bookRepository.getAllYogaSessions().collect { sessions ->
                // Можно обновить статистику в UI
            }
        }
    }
}
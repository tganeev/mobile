package org.readium.r2.testapp.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App
import org.readium.r2.testapp.data.model.AlarmLog

class AlarmLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _logs = MutableStateFlow<List<AlarmLog>>(emptyList())
    val logs: StateFlow<List<AlarmLog>> = _logs.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            app.bookRepository.getAllAlarmLogs().collect { logs ->
                _logs.value = logs
            }
        }
    }

    suspend fun clearAllLogs() {
        app.bookRepository.deleteAllAlarmLogs()
        loadLogs()
    }
}
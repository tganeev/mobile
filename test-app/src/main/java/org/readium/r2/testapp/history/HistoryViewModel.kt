package org.readium.r2.testapp.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application as App

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App

    private val _historyRecords = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val historyRecords: StateFlow<List<HistoryRecord>> = _historyRecords

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    suspend fun restoreFromServer() {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val result = app.historySyncManager.restoreAllData()
            if (result.isSuccess) {
                val records = result.getOrNull() ?: emptyList()
                _historyRecords.value = records
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Неизвестная ошибка"
        } finally {
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
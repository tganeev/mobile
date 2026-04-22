package org.readium.r2.testapp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MenuVisibilityViewModel : ViewModel() {
    private val _isMenuVisible = MutableStateFlow(true)
    val isMenuVisible: StateFlow<Boolean> = _isMenuVisible

    fun setMenuVisible(visible: Boolean) {
        _isMenuVisible.value = visible
    }
}
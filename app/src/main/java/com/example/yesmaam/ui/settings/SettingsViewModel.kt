package com.example.yesmaam.ui.settings

import androidx.lifecycle.ViewModel
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val store = container.settings
    private val _ui = MutableStateFlow(
        store.settings.value.let { Pair(it.teacherName, it.institution) }
    )
    val ui: StateFlow<Pair<String, String>> = _ui

    fun onName(v: String) = _ui.update { it.copy(first = v) }
    fun onInstitution(v: String) = _ui.update { it.copy(second = v) }
    fun save(onDone: () -> Unit) { store.update(_ui.value.first.trim(), _ui.value.second.trim()); onDone() }
}

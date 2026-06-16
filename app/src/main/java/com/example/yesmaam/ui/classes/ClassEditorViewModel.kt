package com.example.yesmaam.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassEditorUi(
    val name: String = "",
    val note: String = "",
    val emoji: String = "📕",
    val colorKey: String = "sage",
    val loaded: Boolean = false,
)

class ClassEditorViewModel(private val container: AppContainer, private val classId: Long?) : ViewModel() {
    private val repo = container.repository
    private val _ui = MutableStateFlow(ClassEditorUi())
    val ui: StateFlow<ClassEditorUi> = _ui

    init {
        if (classId != null) viewModelScope.launch {
            repo.getClass(classId)?.let { c ->
                _ui.value = ClassEditorUi(c.name, c.note ?: "", c.emoji, c.colorKey, true)
            }
        } else _ui.update { it.copy(loaded = true) }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v) }
    fun onNote(v: String) = _ui.update { it.copy(note = v) }
    fun onEmoji(v: String) = _ui.update { it.copy(emoji = v) }
    fun onColor(v: String) = _ui.update { it.copy(colorKey = v) }

    val canSave get() = _ui.value.name.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _ui.value
        if (classId == null) {
            repo.createClass(ClassEntity(
                name = s.name.trim(), note = s.note.ifBlank { null }, emoji = s.emoji,
                colorKey = s.colorKey, createdAt = System.currentTimeMillis(),
            ))
        } else {
            repo.getClass(classId)?.let {
                repo.updateClass(it.copy(name = s.name.trim(), note = s.note.ifBlank { null }, emoji = s.emoji, colorKey = s.colorKey))
            }
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (classId != null) repo.getClass(classId)?.let { repo.deleteClass(it) }
        onDone()
    }
}

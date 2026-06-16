package com.example.yesmaam.ui.classroom.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentEditorUi(val name: String = "", val roll: String = "", val phone: String = "")

class StudentEditorViewModel(
    private val container: AppContainer,
    private val classId: Long,
    private val studentId: Long?,
) : ViewModel() {
    private val repo = container.repository
    private val _ui = MutableStateFlow(StudentEditorUi())
    val ui: StateFlow<StudentEditorUi> = _ui

    init {
        if (studentId != null) viewModelScope.launch {
            repo.getStudent(studentId)?.let { _ui.value = StudentEditorUi(it.name, it.rollNumber, it.guardianPhone ?: "") }
        }
    }

    fun onName(v: String) = _ui.update { it.copy(name = v) }
    fun onRoll(v: String) = _ui.update { it.copy(roll = v) }
    fun onPhone(v: String) = _ui.update { it.copy(phone = v) }
    val canSave get() = _ui.value.name.isNotBlank() && _ui.value.roll.isNotBlank()

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = _ui.value
        if (studentId == null) {
            repo.addStudent(StudentEntity(
                classId = classId, name = s.name.trim(), rollNumber = s.roll.trim(),
                guardianPhone = s.phone.ifBlank { null }, createdAt = System.currentTimeMillis(),
            ))
        } else {
            repo.getStudent(studentId)?.let {
                repo.updateStudent(it.copy(name = s.name.trim(), rollNumber = s.roll.trim(), guardianPhone = s.phone.ifBlank { null }))
            }
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (studentId != null) repo.getStudent(studentId)?.let { repo.deleteStudent(it) }
        onDone()
    }
}

package com.example.yesmaam.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class TeacherSettings(val teacherName: String = "", val institution: String = "")

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("yesmaam_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        TeacherSettings(
            teacherName = prefs.getString(KEY_NAME, "") ?: "",
            institution = prefs.getString(KEY_INSTITUTION, "") ?: "",
        )
    )
    val settings: StateFlow<TeacherSettings> = _settings

    fun update(name: String, institution: String) {
        prefs.edit().putString(KEY_NAME, name).putString(KEY_INSTITUTION, institution).apply()
        _settings.update { it.copy(teacherName = name, institution = institution) }
    }

    companion object {
        private const val KEY_NAME = "teacher_name"
        private const val KEY_INSTITUTION = "institution"
    }
}

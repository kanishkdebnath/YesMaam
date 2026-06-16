package com.example.yesmaam.ui.classroom.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TodayViewModel(private val container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    val date: LocalDate = LocalDate.now()

    data class Row(val student: StudentEntity, val status: AttendanceStatus)
    data class Ui(val isHoliday: Boolean = false, val rows: List<Row> = emptyList(),
                  val present: Int = 0, val absent: Int = 0, val late: Int = 0)

    val ui: StateFlow<Ui> = combine(
        repo.observeStudents(classId),
        repo.observeDay(classId, date),
        repo.observeIsHoliday(classId, date),
    ) { students, records, holidayCount ->
        val byId = records.associate { it.studentId to it.status }
        val rows = students.map { Row(it, byId[it.id] ?: AttendanceStatus.PRESENT) }
        Ui(
            isHoliday = holidayCount > 0,
            rows = rows,
            present = rows.count { it.status == AttendanceStatus.PRESENT },
            absent = rows.count { it.status == AttendanceStatus.ABSENT },
            late = rows.count { it.status == AttendanceStatus.LATE },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Ui())

    fun toggle(studentId: Long, status: AttendanceStatus) = viewModelScope.launch {
        val current = ui.value.rows.associate { it.student.id to it.status }.toMutableMap()
        current[studentId] = status
        repo.saveDay(date, current) // persists the whole roster -> creates/updates the session
    }

    fun markHoliday() = viewModelScope.launch { repo.markHoliday(classId, date) }
    fun removeHoliday() = viewModelScope.launch { repo.removeHoliday(classId, date) }
}

package com.example.yesmaam.ui.classroom.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.domain.report.toDate
import com.example.yesmaam.domain.report.toRecord
import com.example.yesmaam.domain.report.toRef
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

class StudentsViewModel(container: AppContainer, classId: Long) : ViewModel() {
    private val repo = container.repository
    private val month = YearMonth.now()

    data class RowUi(val student: StudentEntity, val percentage: Int)

    val rows: StateFlow<List<RowUi>> = combine(
        repo.observeStudents(classId),
        repo.observeMonth(classId, month),
        repo.observeHolidays(classId, month),
    ) { students, attendance, holidays ->
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(classId, "", null, "", "sage"),
                students = students.map { it.toRef() },
                attendance = attendance.map { it.toRecord() },
                holidays = holidays.map { it.toDate() },
                month = month,
            )
        )
        val pct = report.rows.associate { it.student.id to it.percentage }
        students.map { RowUi(it, pct[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

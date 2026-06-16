package com.example.yesmaam.ui.classroom.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.domain.report.toDate
import com.example.yesmaam.domain.report.toInfo
import com.example.yesmaam.domain.report.toRecord
import com.example.yesmaam.domain.report.toRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    private val settings = container.settings
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    val report: StateFlow<MonthlyReport?> = _month.flatMapLatest { m ->
        combine(
            repo.observeClass(classId),
            repo.observeStudents(classId),
            repo.observeMonth(classId, m),
            repo.observeHolidays(classId, m),
            settings.settings,
        ) { cls, students, attendance, holidays, sett ->
            if (cls == null) null else buildMonthlyReport(
                ReportInputs(
                    classInfo = cls.toInfo(),
                    students = students.map { it.toRef() },
                    attendance = attendance.map { it.toRecord() },
                    holidays = holidays.map { it.toDate() },
                    month = m,
                    teacherName = sett.teacherName.ifBlank { null },
                    institution = sett.institution.ifBlank { null },
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
}

package com.example.yesmaam.ui.classroom.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.ui.components.DayMark
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val container: AppContainer, private val classId: Long) : ViewModel() {
    private val repo = container.repository
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month
    val today: LocalDate = LocalDate.now()

    data class Selected(val date: LocalDate, val isHoliday: Boolean, val hasSession: Boolean)
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)

    val marks: StateFlow<Map<LocalDate, DayMark>> = _month.flatMapLatest { m ->
        combine(repo.observeMonth(classId, m), repo.observeHolidays(classId, m)) { attendance, holidays ->
            val holidaySet = holidays.map { LocalDate.ofEpochDay(it.date) }.toSet()
            val sessionSet = attendance.map { LocalDate.ofEpochDay(it.date) }.toSet() - holidaySet
            buildMap {
                sessionSet.forEach { put(it, DayMark.TAKEN) }
                holidaySet.forEach { put(it, DayMark.HOLIDAY) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Derived from marks so the action panel self-corrects when the DB write re-emits
    // (e.g. it flips to "Holiday" the instant marking lands, without a re-tap).
    val selected: StateFlow<Selected?> = combine(_selectedDate, marks) { date, ms ->
        date?.let { d ->
            val m = ms[d]
            Selected(d, m == DayMark.HOLIDAY, m == DayMark.TAKEN)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }

    fun selectDay(date: LocalDate) { _selectedDate.value = date }

    fun markHoliday(date: LocalDate) = viewModelScope.launch { repo.markHoliday(classId, date) }
    fun removeHoliday(date: LocalDate) = viewModelScope.launch { repo.removeHoliday(classId, date) }
}

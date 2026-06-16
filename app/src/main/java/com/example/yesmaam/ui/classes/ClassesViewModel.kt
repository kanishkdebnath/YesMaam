package com.example.yesmaam.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.di.AppContainer
import com.example.yesmaam.ui.components.ClassDayState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ClassRowUi(val entity: ClassEntity, val studentCount: Int, val state: ClassDayState)

class ClassesViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val today = LocalDate.now()

    val state: StateFlow<List<ClassRowUi>> = combine(
        repo.observeClasses(),
        repo.observeStudentCounts(),
        repo.observeClassIdsWithAttendance(today),
        repo.observeClassIdsOnHoliday(today),
    ) { classes, counts, takenIds, holidayIds ->
        val countMap = counts.associate { it.classId to it.n }
        classes.map { c ->
            val st = when {
                c.id in holidayIds -> ClassDayState.HOLIDAY
                c.id in takenIds -> ClassDayState.DONE
                else -> ClassDayState.PENDING
            }
            ClassRowUi(c, countMap[c.id] ?: 0, st)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

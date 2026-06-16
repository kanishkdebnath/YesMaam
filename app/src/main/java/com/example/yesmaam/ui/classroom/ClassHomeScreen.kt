package com.example.yesmaam.ui.classroom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yesmaam.di.appContainer

private enum class Tab(val label: String) { TODAY("Today"), CALENDAR("Calendar"), STUDENTS("Students"), REPORTS("Reports") }

@Composable
fun ClassHomeScreen(classId: Long, onEditClass: () -> Unit, onEditStudent: (Long?) -> Unit) {
    val container = LocalContext.current.appContainer
    val classEntity by container.repository.observeClass(classId)
        .collectAsStateWithLifecycle(initialValue = null)
    val colorKey = classEntity?.colorKey ?: "sage"
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.TODAY -> Icons.Filled.CheckCircle
                                    Tab.CALENDAR -> Icons.Filled.DateRange
                                    Tab.STUDENTS -> Icons.Filled.Person
                                    Tab.REPORTS -> Icons.AutoMirrored.Filled.List
                                }, contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (Tab.entries[tab]) {
                Tab.TODAY -> com.example.yesmaam.ui.classroom.today.TodayScreen(classId, colorKey)
                Tab.CALENDAR -> com.example.yesmaam.ui.classroom.calendar.CalendarScreen(classId)
                Tab.STUDENTS -> com.example.yesmaam.ui.classroom.students.StudentsScreen(classId, colorKey, onEditStudent)
                Tab.REPORTS -> com.example.yesmaam.ui.classroom.reports.ReportsScreen(classId)
            }
        }
    }
}

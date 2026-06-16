package com.example.yesmaam.ui.classroom.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.CalendarGrid
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.SectionCard
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(classId: Long) {
    val container = LocalContext.current.appContainer
    val vm: CalendarViewModel = viewModel(factory = viewModelFactory { initializer { CalendarViewModel(container, classId) } })
    val month by vm.month.collectAsStateWithLifecycle()
    val marks by vm.marks.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::prevMonth) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        }
        CalendarGrid(month = month, today = vm.today, marks = marks, onDayClick = vm::selectDay)

        selected?.let { sel ->
            SectionCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column {
                    Text(sel.date.format(DateTimeFormatter.ofPattern("EEEE · d MMMM")) +
                        if (sel.isHoliday) " — Holiday" else "", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sel.isHoliday) {
                            PrimaryButton("Remove holiday", onClick = { vm.removeHoliday(sel.date) }, modifier = Modifier.weight(1f))
                        } else {
                            GhostButton("Mark holiday", onClick = { vm.markHoliday(sel.date) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

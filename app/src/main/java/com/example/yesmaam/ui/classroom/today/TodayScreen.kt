package com.example.yesmaam.ui.classroom.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.StudentRow
import com.example.yesmaam.ui.components.StatusToggle
import com.example.yesmaam.ui.components.paletteFor
import com.example.yesmaam.ui.theme.LocalYesMaamColors
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(classId: Long, colorKey: String) {
    val container = LocalContext.current.appContainer
    val vm: TodayViewModel = viewModel(factory = viewModelFactory { initializer { TodayViewModel(container, classId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = LocalYesMaamColors.current
    val avatar = paletteFor(colorKey).color

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(vm.date.format(DateTimeFormatter.ofPattern("EEEE · d MMM yyyy")),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp))
        Text("Today's Register", style = MaterialTheme.typography.headlineMedium)

        if (ui.isHoliday) {
            Surface(
                color = c.holidayTint, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { vm.removeHoliday() },
            ) { Text("☂ Marked as holiday — tap to remove", color = c.onHoliday,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }
        } else {
            Surface(
                color = c.holidayTint, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { vm.markHoliday() },
            ) { Text("☂ No class today? Mark holiday", color = c.onHoliday,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }

            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("${ui.present} present", c.presentTint, c.onPresent)
                Chip("${ui.absent} absent", c.absentTint, c.onAbsent)
                Chip("${ui.late} late", c.lateTint, c.onLate)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ui.rows, key = { it.student.id }) { row ->
                    StudentRow(
                        name = row.student.name,
                        subtitle = "Roll ${row.student.rollNumber}",
                        avatarColor = avatar,
                    ) {
                        StatusToggle(selected = row.status, onSelect = { vm.toggle(row.student.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = CircleShape) {
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp))
    }
}

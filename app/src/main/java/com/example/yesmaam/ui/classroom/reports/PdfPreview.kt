package com.example.yesmaam.ui.classroom.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.ui.theme.LocalYesMaamColors

@Composable
fun ReportPreview(report: MonthlyReport) {
    val c = LocalYesMaamColors.current
    val holidaySet = report.holidayDates.toSet()
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            Text("Name", Modifier.width(96.dp), style = MaterialTheme.typography.labelMedium)
            report.columnDates.forEach {
                Text(it.dayOfMonth.toString(), Modifier.width(20.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
            Text("%", Modifier.width(34.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
        }
        report.rows.forEach { row ->
            Row(Modifier.padding(top = 4.dp)) {
                Text(row.student.name.take(12), Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                report.columnDates.forEach { date ->
                    val (letter, color) = when {
                        date in holidaySet -> "H" to c.onHoliday
                        row.statusByDate[date] == AttendanceStatus.PRESENT -> "P" to c.onPresent
                        row.statusByDate[date] == AttendanceStatus.LATE -> "L" to c.onLate
                        else -> "A" to c.onAbsent
                    }
                    Text(letter, Modifier.width(20.dp), textAlign = TextAlign.Center, color = color, style = MaterialTheme.typography.labelSmall)
                }
                Text("${row.percentage}", Modifier.width(34.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

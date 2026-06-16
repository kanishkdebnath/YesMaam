package com.example.yesmaam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yesmaam.ui.theme.LocalYesMaamColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class DayMark { NONE, TAKEN, HOLIDAY }

@Composable
fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    marks: Map<LocalDate, DayMark>,
    onDayClick: (LocalDate) -> Unit,
) {
    val c = LocalYesMaamColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val cells = buildList<LocalDate?> {
            repeat(lead) { add(null) }
            (1..month.lengthOfMonth()).forEach { add(month.atDay(it)) }
            while (size % 7 != 0) add(null)
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val mark = marks[date] ?: DayMark.NONE
                            val bg = when (mark) {
                                DayMark.TAKEN -> c.presentTint; DayMark.HOLIDAY -> c.holidayTint
                                DayMark.NONE -> MaterialTheme.colorScheme.surface
                            }
                            val border = if (date == today) BorderStroke(2.dp, c.present)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f)
                                    .background(bg, MaterialTheme.shapes.small)
                                    .border(border, MaterialTheme.shapes.small)
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (mark == DayMark.HOLIDAY) "☂" else date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mark == DayMark.HOLIDAY) c.onHoliday else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

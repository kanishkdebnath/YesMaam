package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceStatus
import java.time.LocalDate
import kotlin.math.roundToInt

fun buildMonthlyReport(inputs: ReportInputs): MonthlyReport {
    val month = inputs.month
    fun inMonth(date: LocalDate) = yearMonthMatches(date, month)

    val holidaySet = inputs.holidays.filter { inMonth(it) }.toSet()

    val recordedDates = inputs.attendance.map { it.date }.filter { inMonth(it) }.toSet()
    val sessionDates = (recordedDates - holidaySet).sorted()
    val holidayDates = holidaySet.sorted()
    val columnDates = (sessionDates + holidayDates).distinct().sorted()

    val byStudent = inputs.attendance.groupBy { it.studentId }

    val rows = inputs.students.map { student ->
        val statusByDate = byStudent[student.id].orEmpty()
            .filter { it.date in sessionDates }
            .associate { it.date to it.status }
        var present = 0; var late = 0; var absent = 0
        for (date in sessionDates) {
            when (statusByDate[date]) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.ABSENT, null -> absent++
            }
        }
        val attended = present + late
        val pct = if (sessionDates.isEmpty()) 0 else (attended * 100.0 / sessionDates.size).roundToInt()
        StudentReportRow(student, statusByDate, present, late, absent, attended, pct)
    }

    val avg = if (rows.isEmpty()) 0 else (rows.sumOf { it.percentage }.toDouble() / rows.size).roundToInt()

    return MonthlyReport(
        classInfo = inputs.classInfo,
        month = month,
        teacherName = inputs.teacherName,
        institution = inputs.institution,
        sessionDates = sessionDates,
        holidayDates = holidayDates,
        columnDates = columnDates,
        rows = rows,
        summary = ReportSummary(
            studentCount = inputs.students.size,
            sessionCount = sessionDates.size,
            holidayCount = holidayDates.size,
            averagePercentage = avg,
        ),
    )
}

private fun yearMonthMatches(date: LocalDate, month: java.time.YearMonth): Boolean =
    date.year == month.year && date.monthValue == month.monthValue

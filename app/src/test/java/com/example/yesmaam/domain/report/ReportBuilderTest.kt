package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.ABSENT
import com.example.yesmaam.domain.model.AttendanceStatus.LATE
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportBuilderTest {
    private val cls = ClassInfo(1, "VI-A", null, "📕", "sage")
    private val ann = StudentRef(1, "Ann", "01", null)
    private val bob = StudentRef(2, "Bob", "02", null)
    private fun d(day: Int) = LocalDate.of(2026, 6, day)

    private fun inputs(
        students: List<StudentRef>,
        attendance: List<AttendanceRecord>,
        holidays: List<LocalDate> = emptyList(),
    ) = ReportInputs(cls, students, attendance, holidays, YearMonth.of(2026, 6))

    @Test fun `sessions are dates with records excluding holidays`() {
        val att = listOf(
            AttendanceRecord(1, d(3), PRESENT),
            AttendanceRecord(1, d(4), PRESENT),
            AttendanceRecord(1, d(5), PRESENT), // 5th is a holiday -> not a session
        )
        val report = buildMonthlyReport(inputs(listOf(ann), att, holidays = listOf(d(5))))
        assertEquals(listOf(d(3), d(4)), report.sessionDates)
        assertEquals(listOf(d(5)), report.holidayDates)
        assertEquals(listOf(d(3), d(4), d(5)), report.columnDates)
        assertEquals(2, report.summary.sessionCount)
        assertEquals(1, report.summary.holidayCount)
    }

    @Test fun `percentage counts present and late over sessions`() {
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT),
            AttendanceRecord(1, d(2), LATE),
            AttendanceRecord(1, d(3), ABSENT),
            AttendanceRecord(1, d(4), PRESENT),
        )
        val report = buildMonthlyReport(inputs(listOf(ann), att))
        val row = report.rows.single()
        assertEquals(2, row.presentCount)
        assertEquals(1, row.lateCount)
        assertEquals(1, row.absentCount)
        assertEquals(3, row.attendedCount)
        assertEquals(75, row.percentage) // 3 of 4
    }

    @Test fun `missing record on a session date counts as absent`() {
        // Bob has no record on the 1st, which is a session because Ann was marked.
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT),
            AttendanceRecord(2, d(2), PRESENT),
        )
        val report = buildMonthlyReport(inputs(listOf(ann, bob), att))
        assertEquals(listOf(d(1), d(2)), report.sessionDates)
        val bobRow = report.rows.single { it.student.id == 2L }
        assertEquals(1, bobRow.presentCount)
        assertEquals(1, bobRow.absentCount) // missing on the 1st
        assertEquals(50, bobRow.percentage)
    }

    @Test fun `holiday overrides a stray attendance record on that day`() {
        // A record exists on the 5th, but the 5th is a holiday: it must not count as a
        // session, must not appear in statusByDate, and must not inflate any tally.
        val att = listOf(
            AttendanceRecord(1, d(3), PRESENT),
            AttendanceRecord(1, d(4), PRESENT),
            AttendanceRecord(1, d(5), PRESENT), // stray record on a holiday
        )
        val report = buildMonthlyReport(inputs(listOf(ann), att, holidays = listOf(d(5))))
        val row = report.rows.single()
        assertEquals(listOf(d(3), d(4)), report.sessionDates)
        assertEquals(setOf(d(3), d(4)), row.statusByDate.keys) // holiday day excluded
        assertEquals(2, row.presentCount)
        assertEquals(0, row.absentCount)
        assertEquals(100, row.percentage) // present on both sessions; holiday ignored
    }

    @Test fun `empty roster yields zero average`() {
        val report = buildMonthlyReport(inputs(emptyList(), emptyList()))
        assertEquals(0, report.summary.studentCount)
        assertEquals(0, report.summary.averagePercentage)
    }

    @Test fun `empty month yields zero percentages`() {
        val report = buildMonthlyReport(inputs(listOf(ann), emptyList()))
        assertEquals(0, report.summary.sessionCount)
        assertEquals(0, report.rows.single().percentage)
        assertEquals(0, report.summary.averagePercentage)
    }

    @Test fun `average percentage is the mean of student percentages`() {
        val att = listOf(
            AttendanceRecord(1, d(1), PRESENT), AttendanceRecord(1, d(2), PRESENT), // Ann 100
            AttendanceRecord(2, d(1), PRESENT), AttendanceRecord(2, d(2), ABSENT),  // Bob 50
        )
        val report = buildMonthlyReport(inputs(listOf(ann, bob), att))
        assertEquals(75, report.summary.averagePercentage)
    }
}

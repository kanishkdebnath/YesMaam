package com.example.yesmaam.export

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import com.example.yesmaam.export.ooxml.XlsxCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class XlsxReportExporterTest {
    @Test fun `sheet has header and a holiday column shows H`() {
        val ann = StudentRef(1, "Ann", "01", null)
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(1, "VI-A", null, "📕", "sage"),
                students = listOf(ann),
                attendance = listOf(
                    AttendanceRecord(1, LocalDate.of(2026, 6, 3), PRESENT),
                    AttendanceRecord(1, LocalDate.of(2026, 6, 4), PRESENT),
                ),
                holidays = listOf(LocalDate.of(2026, 6, 5)),
                month = YearMonth.of(2026, 6),
            )
        )
        val sheet = XlsxReportExporter().buildSheet(report)
        val header = sheet.rows.first().filterIsInstance<XlsxCell.Text>().map { it.value }
        assertTrue("Name" in header)
        assertTrue("%" in header)
        // body row: holiday column (5) renders "H"
        val body = sheet.rows[1].filterIsInstance<XlsxCell.Text>().map { it.value }
        assertTrue("H" in body)
        assertTrue("P" in body)
        assertEquals("Ann", body.first())
    }
}

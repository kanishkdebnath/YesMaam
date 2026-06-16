package com.example.yesmaam.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus.PRESENT
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import com.example.yesmaam.domain.report.ReportInputs
import com.example.yesmaam.domain.report.buildMonthlyReport
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class PdfReportExporterTest {
    @Test fun producesNonEmptyPdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val report = buildMonthlyReport(
            ReportInputs(
                classInfo = ClassInfo(1, "VI-A", null, "📕", "sage"),
                students = listOf(StudentRef(1, "Ann", "01", null)),
                attendance = listOf(AttendanceRecord(1, LocalDate.of(2026, 6, 3), PRESENT)),
                holidays = emptyList(),
                month = YearMonth.of(2026, 6),
            )
        )
        val bos = ByteArrayOutputStream()
        PdfReportExporter(ctx).write(report, bos)
        val bytes = bos.toByteArray()
        assertTrue(bytes.size > 100)
        assertTrue(String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-")
    }
}

package com.example.yesmaam.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.yesmaam.R
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import java.io.OutputStream
import java.time.format.DateTimeFormatter

class PdfReportExporter(private val context: Context) : ReportExporter {
    override val format = ExportFormat.PDF

    private val pageW = 842; private val pageH = 595
    private val margin = 36f
    private val rowH = 20f
    private val nameW = 150f; private val rollW = 40f; private val dayW = 22f; private val totW = 34f

    private val fraunces = ResourcesCompat.getFont(context, R.font.fraunces_semibold)
    private val lora = ResourcesCompat.getFont(context, R.font.lora_regular)

    override fun write(report: MonthlyReport, out: OutputStream) {
        val doc = PdfDocument()
        val title = Paint().apply { typeface = fraunces; textSize = 16f; color = 0xFF3E3A35.toInt() }
        val meta = Paint().apply { typeface = lora; textSize = 9f; color = 0xFF9A9086.toInt() }
        val head = Paint().apply { typeface = fraunces; textSize = 8f; color = 0xFF3E3A35.toInt(); textAlign = Paint.Align.CENTER }
        val cell = Paint().apply { typeface = lora; textSize = 8f; color = 0xFF3E3A35.toInt(); textAlign = Paint.Align.CENTER }
        val left = Paint().apply { typeface = lora; textSize = 8f; color = 0xFF3E3A35.toInt() }
        val line = Paint().apply { color = 0xFFECE3D6.toInt(); strokeWidth = 0.5f }

        val rowsPerPage = ((pageH - margin * 2 - 60) / rowH).toInt()
        val pages = report.rows.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }
        val holidaySet = report.holidayDates.toSet()
        val monthLabel = report.month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        pages.forEachIndexed { pageIndex, pageRows ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create())
            val c = page.canvas
            var y = margin + 14f
            c.drawText("Attendance Register", margin, y, title)
            y += 14f
            val sub = buildString {
                append(report.classInfo.name); append(" · ").append(monthLabel)
                report.teacherName?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            }
            c.drawText(sub, margin, y, meta)
            y += 16f

            // header row
            var x = margin
            c.drawText("Name", x + 2f, y, left.alignLeftHead())
            x += nameW; c.drawText("Roll", x + rollW / 2, y, head)
            x += rollW
            for (date in report.columnDates) { c.drawText(date.dayOfMonth.toString(), x + dayW / 2, y, head); x += dayW }
            c.drawText("P", x + totW / 2, y, head); x += totW
            c.drawText("%", x + totW / 2, y, head)
            y += 6f
            c.drawLine(margin, y, x + totW, y, line)
            y += 12f

            // body
            for (row in pageRows) {
                x = margin
                c.drawText(row.student.name.take(24), x + 2f, y, left)
                x += nameW; c.drawText(row.student.rollNumber, x + rollW / 2, y, cell)
                x += rollW
                for (date in report.columnDates) {
                    val (letter, color) = when {
                        date in holidaySet -> "H" to 0xFFC68E4F.toInt()
                        row.statusByDate[date] == AttendanceStatus.PRESENT -> "P" to 0xFF6E8C66.toInt()
                        row.statusByDate[date] == AttendanceStatus.LATE -> "L" to 0xFFC68E4F.toInt()
                        else -> "A" to 0xFFB9777B.toInt()
                    }
                    cell.color = color; c.drawText(letter, x + dayW / 2, y, cell); cell.color = 0xFF3E3A35.toInt()
                    x += dayW
                }
                c.drawText(row.attendedCount.toString(), x + totW / 2, y, cell); x += totW
                c.drawText("${row.percentage}", x + totW / 2, y, cell)
                y += rowH
            }
            doc.finishPage(page)
        }
        doc.writeTo(out)
        doc.close()
    }

    private fun Paint.alignLeftHead(): Paint =
        Paint(this).apply { typeface = fraunces; textSize = 8f; textAlign = Paint.Align.LEFT; color = 0xFF3E3A35.toInt() }
}

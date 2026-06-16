package com.example.yesmaam.export

import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.report.MonthlyReport
import com.example.yesmaam.export.ooxml.XlsxCell
import com.example.yesmaam.export.ooxml.XlsxSheet
import com.example.yesmaam.export.ooxml.XlsxWriter
import java.io.OutputStream

class XlsxReportExporter : ReportExporter {
    override val format = ExportFormat.EXCEL

    // Style ids from XlsxWriter: 1 header, 2 present, 3 absent, 4 late/holiday, 5 bold totals.
    fun buildSheet(report: MonthlyReport): XlsxSheet {
        val holidaySet = report.holidayDates.toSet()
        val header = buildList {
            add(XlsxCell.Text("Name", 1)); add(XlsxCell.Text("Roll", 1))
            report.columnDates.forEach { add(XlsxCell.Text(it.dayOfMonth.toString(), 1)) }
            add(XlsxCell.Text("P", 1)); add(XlsxCell.Text("A", 1))
            add(XlsxCell.Text("L", 1)); add(XlsxCell.Text("%", 1))
        }
        val body = report.rows.map { row ->
            buildList<XlsxCell> {
                add(XlsxCell.Text(row.student.name)); add(XlsxCell.Text(row.student.rollNumber))
                for (date in report.columnDates) {
                    if (date in holidaySet) { add(XlsxCell.Text("H", 4)); continue }
                    when (row.statusByDate[date]) {
                        AttendanceStatus.PRESENT -> add(XlsxCell.Text("P", 2))
                        AttendanceStatus.LATE -> add(XlsxCell.Text("L", 4))
                        AttendanceStatus.ABSENT, null -> add(XlsxCell.Text("A", 3))
                    }
                }
                add(XlsxCell.Number(row.presentCount.toDouble(), 5))
                add(XlsxCell.Number(row.absentCount.toDouble(), 5))
                add(XlsxCell.Number(row.lateCount.toDouble(), 5))
                add(XlsxCell.Number(row.percentage.toDouble(), 5))
            }
        }
        return XlsxSheet(report.classInfo.name.take(31), listOf(header) + body)
    }

    override fun write(report: MonthlyReport, out: OutputStream) =
        XlsxWriter.write(buildSheet(report), out)
}

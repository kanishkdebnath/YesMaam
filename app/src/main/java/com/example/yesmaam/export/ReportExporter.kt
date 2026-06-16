package com.example.yesmaam.export

import com.example.yesmaam.domain.report.MonthlyReport
import java.io.OutputStream

interface ReportExporter {
    val format: ExportFormat
    fun write(report: MonthlyReport, out: OutputStream)
}

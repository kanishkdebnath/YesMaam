package com.example.yesmaam.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.yesmaam.domain.report.MonthlyReport
import java.io.File
import java.io.FileOutputStream

object ExportDelivery {
    fun shareReport(context: Context, exporter: ReportExporter, report: MonthlyReport) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = "${report.classInfo.name}-${report.month}"
            .replace(Regex("[^A-Za-z0-9\\-_]"), "_")
        val file = File(dir, "$safeName.${exporter.format.ext}")
        FileOutputStream(file).use { exporter.write(report, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = exporter.format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share ${exporter.format.label} report")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

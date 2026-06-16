package com.example.yesmaam.export

enum class ExportFormat(val label: String, val mime: String, val ext: String) {
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PDF("PDF", "application/pdf", "pdf"),
}

package com.example.yesmaam.domain.report

import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import java.time.LocalDate
import java.time.YearMonth

data class ReportInputs(
    val classInfo: ClassInfo,
    val students: List<StudentRef>,
    val attendance: List<AttendanceRecord>,
    val holidays: List<LocalDate>,
    val month: YearMonth,
    val teacherName: String? = null,
    val institution: String? = null,
)

data class StudentReportRow(
    val student: StudentRef,
    val statusByDate: Map<LocalDate, AttendanceStatus>,
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val attendedCount: Int,
    val percentage: Int,
)

data class ReportSummary(
    val studentCount: Int,
    val sessionCount: Int,
    val holidayCount: Int,
    val averagePercentage: Int,
)

data class MonthlyReport(
    val classInfo: ClassInfo,
    val month: YearMonth,
    val teacherName: String?,
    val institution: String?,
    val sessionDates: List<LocalDate>,
    val holidayDates: List<LocalDate>,
    val columnDates: List<LocalDate>,
    val rows: List<StudentReportRow>,
    val summary: ReportSummary,
)

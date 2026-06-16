package com.example.yesmaam.domain.model

import java.time.LocalDate

enum class AttendanceStatus { PRESENT, ABSENT, LATE }

data class ClassInfo(
    val id: Long,
    val name: String,
    val note: String?,
    val emoji: String,
    val colorKey: String,
)

data class StudentRef(
    val id: Long,
    val name: String,
    val rollNumber: String,
    val guardianPhone: String?,
)

data class AttendanceRecord(
    val studentId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
)

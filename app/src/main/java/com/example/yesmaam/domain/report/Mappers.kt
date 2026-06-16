package com.example.yesmaam.domain.report

import com.example.yesmaam.data.db.AttendanceEntity
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.data.db.HolidayEntity
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.domain.model.AttendanceRecord
import com.example.yesmaam.domain.model.ClassInfo
import com.example.yesmaam.domain.model.StudentRef
import java.time.LocalDate

fun ClassEntity.toInfo() = ClassInfo(id, name, note, emoji, colorKey)
fun StudentEntity.toRef() = StudentRef(id, name, rollNumber, guardianPhone)
fun AttendanceEntity.toRecord() = AttendanceRecord(studentId, LocalDate.ofEpochDay(date), status)
fun HolidayEntity.toDate(): LocalDate = LocalDate.ofEpochDay(date)

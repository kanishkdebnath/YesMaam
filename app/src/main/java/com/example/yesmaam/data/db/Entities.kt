package com.example.yesmaam.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.yesmaam.domain.model.AttendanceStatus

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String? = null,
    val emoji: String,
    val colorKey: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "students",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class, parentColumns = ["id"], childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("classId")],
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val name: String,
    val rollNumber: String,
    val guardianPhone: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "attendance",
    foreignKeys = [ForeignKey(
        entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["studentId", "date"], unique = true)],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: Long, // epoch day
    val status: AttendanceStatus,
)

@Entity(
    tableName = "holidays",
    foreignKeys = [ForeignKey(
        entity = ClassEntity::class, parentColumns = ["id"], childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["classId", "date"], unique = true)],
)
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val date: Long, // epoch day
    val note: String? = null,
)

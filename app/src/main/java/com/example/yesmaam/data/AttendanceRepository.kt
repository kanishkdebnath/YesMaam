package com.example.yesmaam.data

import com.example.yesmaam.data.db.AppDatabase
import com.example.yesmaam.data.db.AttendanceEntity
import com.example.yesmaam.data.db.ClassEntity
import com.example.yesmaam.data.db.HolidayEntity
import com.example.yesmaam.data.db.StudentEntity
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class AttendanceRepository(private val db: AppDatabase) {

    // ----- classes -----
    fun observeClasses(): Flow<List<ClassEntity>> = db.classDao().observeAll()
    fun observeClass(id: Long): Flow<ClassEntity?> = db.classDao().observe(id)
    suspend fun getClass(id: Long): ClassEntity? = db.classDao().get(id)
    suspend fun createClass(c: ClassEntity): Long = db.classDao().insert(c)
    suspend fun updateClass(c: ClassEntity) = db.classDao().update(c)
    suspend fun deleteClass(c: ClassEntity) = db.classDao().delete(c)

    fun observeStudentCounts() = db.studentDao().observeCounts()
    fun observeClassIdsWithAttendance(date: java.time.LocalDate) =
        db.attendanceDao().observeClassIdsWithAttendance(date.toEpochDay())
    fun observeClassIdsOnHoliday(date: java.time.LocalDate) =
        db.holidayDao().observeClassIdsOnHoliday(date.toEpochDay())

    // ----- students -----
    fun observeStudents(classId: Long): Flow<List<StudentEntity>> =
        db.studentDao().observeByClass(classId)
    suspend fun getStudents(classId: Long): List<StudentEntity> =
        db.studentDao().getByClass(classId)
    suspend fun getStudent(id: Long): StudentEntity? = db.studentDao().get(id)
    suspend fun addStudent(s: StudentEntity): Long = db.studentDao().insert(s)
    suspend fun updateStudent(s: StudentEntity) = db.studentDao().update(s)
    suspend fun deleteStudent(s: StudentEntity) = db.studentDao().delete(s)

    // ----- attendance -----
    fun observeDay(classId: Long, date: LocalDate): Flow<List<AttendanceEntity>> =
        db.attendanceDao().observeForClassDay(classId, date.toEpochDay())

    fun observeMonth(classId: Long, month: YearMonth): Flow<List<AttendanceEntity>> {
        val (start, end) = month.range()
        return db.attendanceDao().observeForClassRange(classId, start, end)
    }

    suspend fun getMonthAttendance(classId: Long, month: YearMonth): List<AttendanceEntity> {
        val (start, end) = month.range()
        return db.attendanceDao().getForClassRange(classId, start, end)
    }

    /** Persist the whole day's statuses for a class (creates/updates the session). */
    suspend fun saveDay(date: LocalDate, statuses: Map<Long, AttendanceStatus>) {
        val day = date.toEpochDay()
        db.attendanceDao().upsertAll(
            statuses.map { (studentId, status) -> AttendanceEntity(studentId = studentId, date = day, status = status) }
        )
    }

    // ----- holidays -----
    fun observeHolidays(classId: Long, month: YearMonth): Flow<List<HolidayEntity>> {
        val (start, end) = month.range()
        return db.holidayDao().observeForClassRange(classId, start, end)
    }

    suspend fun getHolidays(classId: Long, month: YearMonth): List<HolidayEntity> {
        val (start, end) = month.range()
        return db.holidayDao().getForClassRange(classId, start, end)
    }

    fun observeIsHoliday(classId: Long, date: LocalDate): Flow<Int> =
        db.holidayDao().observeIsHoliday(classId, date.toEpochDay())

    suspend fun markHoliday(classId: Long, date: LocalDate, note: String? = null) =
        db.holidayDao().insert(HolidayEntity(classId = classId, date = date.toEpochDay(), note = note))

    suspend fun removeHoliday(classId: Long, date: LocalDate) =
        db.holidayDao().remove(classId, date.toEpochDay())

    private fun YearMonth.range(): Pair<Long, Long> =
        atDay(1).toEpochDay() to atEndOfMonth().toEpochDay()
}

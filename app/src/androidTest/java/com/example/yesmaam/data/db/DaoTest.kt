package com.example.yesmaam.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.yesmaam.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun teardown() = db.close()

    @Test fun deletingClassCascadesStudentsAndAttendance() = runBlocking {
        val classId = db.classDao().insert(ClassEntity(name = "VI-A", emoji = "📕", colorKey = "sage", createdAt = 0))
        val sId = db.studentDao().insert(StudentEntity(classId = classId, name = "Ann", rollNumber = "01", createdAt = 0))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.PRESENT)))

        db.classDao().get(classId)?.let { db.classDao().delete(it) }

        assertEquals(emptyList<StudentEntity>(), db.studentDao().getByClass(classId))
        assertEquals(emptyList<AttendanceEntity>(), db.attendanceDao().getForClassRange(classId, 0, 1000))
    }

    @Test fun upsertReplacesSameStudentAndDate() = runBlocking {
        val classId = db.classDao().insert(ClassEntity(name = "C", emoji = "📕", colorKey = "sage", createdAt = 0))
        val sId = db.studentDao().insert(StudentEntity(classId = classId, name = "Ann", rollNumber = "01", createdAt = 0))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.PRESENT)))
        db.attendanceDao().upsertAll(listOf(AttendanceEntity(studentId = sId, date = 100, status = AttendanceStatus.ABSENT)))

        val rows = db.attendanceDao().observeForClassRange(classId, 0, 1000).first()
        assertEquals(1, rows.size)
        assertEquals(AttendanceStatus.ABSENT, rows.single().status)
    }
}

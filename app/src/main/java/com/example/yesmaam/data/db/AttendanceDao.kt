package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceEntity>)

    /** All attendance for a class within an (inclusive) epoch-day range, via the student join. */
    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date BETWEEN :startDay AND :endDay
        """
    )
    fun observeForClassRange(classId: Long, startDay: Long, endDay: Long): Flow<List<AttendanceEntity>>

    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date BETWEEN :startDay AND :endDay
        """
    )
    suspend fun getForClassRange(classId: Long, startDay: Long, endDay: Long): List<AttendanceEntity>

    @Query(
        """
        SELECT a.* FROM attendance a
        INNER JOIN students s ON s.id = a.studentId
        WHERE s.classId = :classId AND a.date = :day
        """
    )
    fun observeForClassDay(classId: Long, day: Long): Flow<List<AttendanceEntity>>
}

package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    // rollNumber is a free-form String; CAST sorts numeric rolls naturally, the
    // rollNumber tiebreak keeps alphanumeric rolls (e.g. "A1") in a stable order.
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(rollNumber AS INTEGER), rollNumber, name")
    fun observeByClass(classId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY CAST(rollNumber AS INTEGER), rollNumber, name")
    suspend fun getByClass(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun get(id: Long): StudentEntity?

    @Insert suspend fun insert(s: StudentEntity): Long
    @Update suspend fun update(s: StudentEntity)
    @Delete suspend fun delete(s: StudentEntity)
}

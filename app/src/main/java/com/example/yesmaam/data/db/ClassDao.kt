package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    fun observe(id: Long): Flow<ClassEntity?>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun get(id: Long): ClassEntity?

    @Insert suspend fun insert(c: ClassEntity): Long
    @Update suspend fun update(c: ClassEntity)
    @Delete suspend fun delete(c: ClassEntity)
}

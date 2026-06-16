package com.example.yesmaam.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: HolidayEntity)

    @Query("DELETE FROM holidays WHERE classId = :classId AND date = :day")
    suspend fun remove(classId: Long, day: Long)

    @Query("SELECT * FROM holidays WHERE classId = :classId AND date BETWEEN :startDay AND :endDay")
    fun observeForClassRange(classId: Long, startDay: Long, endDay: Long): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE classId = :classId AND date BETWEEN :startDay AND :endDay")
    suspend fun getForClassRange(classId: Long, startDay: Long, endDay: Long): List<HolidayEntity>

    @Query("SELECT COUNT(*) FROM holidays WHERE classId = :classId AND date = :day")
    fun observeIsHoliday(classId: Long, day: Long): Flow<Int>

    @Query("SELECT classId FROM holidays WHERE date = :day")
    fun observeClassIdsOnHoliday(day: Long): kotlinx.coroutines.flow.Flow<List<Long>>
}

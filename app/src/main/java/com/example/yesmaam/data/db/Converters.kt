package com.example.yesmaam.data.db

import androidx.room.TypeConverter
import com.example.yesmaam.domain.model.AttendanceStatus

class Converters {
    @TypeConverter fun statusToString(s: AttendanceStatus): String = s.name
    @TypeConverter fun stringToStatus(s: String): AttendanceStatus = AttendanceStatus.valueOf(s)
}

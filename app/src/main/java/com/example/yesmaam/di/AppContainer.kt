package com.example.yesmaam.di

import android.content.Context
import com.example.yesmaam.data.AttendanceRepository
import com.example.yesmaam.data.SettingsStore
import com.example.yesmaam.data.db.AppDatabase

class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)
    val repository = AttendanceRepository(db)
    val settings = SettingsStore(context)
}

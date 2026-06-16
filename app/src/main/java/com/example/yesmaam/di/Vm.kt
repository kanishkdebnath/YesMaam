package com.example.yesmaam.di

import android.content.Context
import com.example.yesmaam.YesMaamApp

val Context.appContainer: AppContainer
    get() = (applicationContext as YesMaamApp).container

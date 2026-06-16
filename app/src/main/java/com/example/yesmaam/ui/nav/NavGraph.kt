package com.example.yesmaam.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun YesMaamNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CLASSES) {
        composable(Routes.CLASSES) { Placeholder("Classes") }
        composable(Routes.SETTINGS) { Placeholder("Settings") }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label) }
}

package com.example.yesmaam.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun YesMaamNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.CLASSES) {
        composable(Routes.CLASSES) {
            com.example.yesmaam.ui.classes.ClassesScreen(
                onOpenClass = { id -> nav.navigate(Routes.classHome(id)) },
                onNewClass = { nav.navigate(Routes.classEditor(null)) },
            )
        }
        composable(
            Routes.CLASS_EDITOR,
            arguments = listOf(navArgument("classId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val id = entry.arguments?.getLong("classId")?.takeIf { it >= 0 }
            com.example.yesmaam.ui.classes.ClassEditorScreen(classId = id, onDone = { nav.popBackStack() })
        }
        // class home, settings composables are added in later tasks.
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label) }
}

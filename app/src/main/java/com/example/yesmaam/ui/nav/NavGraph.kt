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
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.CLASS_EDITOR,
            arguments = listOf(navArgument("classId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val id = entry.arguments?.getLong("classId")?.takeIf { it >= 0 }
            com.example.yesmaam.ui.classes.ClassEditorScreen(classId = id, onDone = { nav.popBackStack() })
        }
        composable(
            Routes.CLASS_HOME,
            arguments = listOf(navArgument("classId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments!!.getLong("classId")
            com.example.yesmaam.ui.classroom.ClassHomeScreen(
                classId = id,
                onEditClass = { nav.navigate(Routes.classEditor(id)) },
                onEditStudent = { sid -> nav.navigate(Routes.studentEditor(id, sid)) },
            )
        }
        composable(
            Routes.STUDENT_EDITOR,
            arguments = listOf(
                navArgument("classId") { type = NavType.LongType },
                navArgument("studentId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val cid = entry.arguments!!.getLong("classId")
            val sid = entry.arguments?.getLong("studentId")?.takeIf { it >= 0 }
            com.example.yesmaam.ui.classroom.students.StudentEditorScreen(cid, sid, onDone = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            com.example.yesmaam.ui.settings.SettingsScreen(onDone = { nav.popBackStack() })
        }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label) }
}

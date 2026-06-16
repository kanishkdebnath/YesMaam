package com.example.yesmaam.ui.classroom.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.StudentRow
import com.example.yesmaam.ui.components.paletteFor

@Composable
fun StudentsScreen(classId: Long, colorKey: String, onEditStudent: (Long?) -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: StudentsViewModel = viewModel(factory = viewModelFactory { initializer { StudentsViewModel(container, classId) } })
    val rows by vm.rows.collectAsStateWithLifecycle()
    val avatar = paletteFor(colorKey).color

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Students", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(rows, key = { it.student.id }) { r ->
                val phone = r.student.guardianPhone?.let { "☎ guardian · " } ?: ""
                StudentRow(
                    name = r.student.name,
                    subtitle = "Roll ${r.student.rollNumber} · $phone${r.percentage}%",
                    avatarColor = avatar,
                    modifier = Modifier.clickable { onEditStudent(r.student.id) },
                    trailing = { Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
            }
        }
        PrimaryButton("＋  Add a student", onClick = { onEditStudent(null) }, modifier = Modifier.padding(bottom = 16.dp))
    }
}

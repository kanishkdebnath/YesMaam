package com.example.yesmaam.ui.classroom.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.Field
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton

@Composable
fun StudentEditorScreen(classId: Long, studentId: Long?, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: StudentEditorViewModel = viewModel(factory = viewModelFactory { initializer { StudentEditorViewModel(container, classId, studentId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (studentId == null) "Add a Student" else "Edit Student", style = MaterialTheme.typography.headlineMedium)
            Field("Full name", ui.name, vm::onName)
            Field("Roll number", ui.roll, vm::onRoll)
            Field("Guardian phone (optional)", ui.phone, vm::onPhone)
            PrimaryButton("Save student", onClick = { if (vm.canSave) vm.save(onDone) }, enabled = ui.name.isNotBlank() && ui.roll.isNotBlank())
            if (studentId != null) GhostButton("Delete student", onClick = { confirmDelete = true })
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this student?") },
            text = { Text("This permanently removes the student and all of their attendance records.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onDone) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

package com.example.yesmaam.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.ui.components.ClassEmojis
import com.example.yesmaam.ui.components.ClassPalettes
import com.example.yesmaam.ui.components.Field
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton

@Composable
fun ClassEditorScreen(classId: Long?, onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: ClassEditorViewModel = viewModel(factory = viewModelFactory { initializer { ClassEditorViewModel(container, classId) } })
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (classId == null) "New Class" else "Edit Class", style = MaterialTheme.typography.headlineMedium)
            Field("Class name", ui.name, vm::onName)
            Field("Note (optional)", ui.note, vm::onNote)

            Text("Emoji", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClassEmojis.take(6).forEach { e ->
                    Box(
                        Modifier.size(40.dp)
                            .border(if (e == ui.emoji) 2.dp else 1.dp, if (e == ui.emoji) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .clickable { vm.onEmoji(e) },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, style = MaterialTheme.typography.titleMedium) }
                }
            }

            Text("Colour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClassPalettes.forEach { p ->
                    Box(
                        Modifier.size(30.dp).background(p.color, CircleShape)
                            .border(if (p.key == ui.colorKey) 2.dp else 0.dp, if (p.key == ui.colorKey) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable { vm.onColor(p.key) },
                    )
                }
            }

            PrimaryButton(if (classId == null) "Create class" else "Save", onClick = { if (vm.canSave) vm.save(onDone) }, enabled = ui.name.isNotBlank())
            if (classId != null) GhostButton("Delete class", onClick = { vm.delete(onDone) })
        }
    }
}

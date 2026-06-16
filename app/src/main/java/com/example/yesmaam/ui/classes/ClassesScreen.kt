package com.example.yesmaam.ui.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.yesmaam.ui.components.ClassCard
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.paletteFor

@Composable
fun ClassesScreen(onOpenClass: (Long) -> Unit, onNewClass: () -> Unit, onOpenSettings: () -> Unit) {
    val container = LocalContext.current.appContainer
    val vm: ClassesViewModel = viewModel(factory = viewModelFactory { initializer { ClassesViewModel(container) } })
    val rows by vm.state.collectAsStateWithLifecycle()

    Scaffold { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your Classes", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onOpenSettings) { Text("⚙", style = MaterialTheme.typography.headlineMedium) }
            }
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(rows, key = { it.entity.id }) { row ->
                    val p = paletteFor(row.entity.colorKey)
                    ClassCard(
                        name = row.entity.name,
                        info = "${row.studentCount} students" + (row.entity.note?.let { " · $it" } ?: ""),
                        emoji = row.entity.emoji,
                        tint = p.tint,
                        state = row.state,
                        onClick = { onOpenClass(row.entity.id) },
                    )
                }
            }
            PrimaryButton("＋  New class", onClick = onNewClass, modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

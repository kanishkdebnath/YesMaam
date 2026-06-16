package com.example.yesmaam.ui.classroom.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.yesmaam.di.appContainer
import com.example.yesmaam.export.ExportDelivery
import com.example.yesmaam.export.PdfReportExporter
import com.example.yesmaam.export.XlsxReportExporter
import com.example.yesmaam.ui.components.GhostButton
import com.example.yesmaam.ui.components.PrimaryButton
import com.example.yesmaam.ui.components.SectionCard
import com.example.yesmaam.ui.components.StatCard
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(classId: Long) {
    val context = LocalContext.current
    val container = context.appContainer
    val vm: ReportsViewModel = viewModel(factory = viewModelFactory { initializer { ReportsViewModel(container, classId) } })
    val month by vm.month.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
        Text("Monthly Report", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::prevMonth) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        }

        report?.let { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("${r.summary.sessionCount}", "school days", Modifier.weight(1f))
                StatCard("${r.summary.averagePercentage}%", "avg present", Modifier.weight(1f))
                StatCard("${r.summary.holidayCount}", "holidays", Modifier.weight(1f))
            }
            SectionCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { ReportPreview(r) }
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Export Excel", onClick = {
                    ExportDelivery.shareReport(context, XlsxReportExporter(), r)
                }, modifier = Modifier.weight(1f))
                GhostButton("Export PDF", onClick = {
                    ExportDelivery.shareReport(context, PdfReportExporter(context), r)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

package com.example.yesmaam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.yesmaam.domain.model.AttendanceStatus
import com.example.yesmaam.ui.theme.LocalYesMaamColors

@Composable
fun StatusToggle(selected: AttendanceStatus, onSelect: (AttendanceStatus) -> Unit) {
    val c = LocalYesMaamColors.current
    Row {
        Seg("P", selected == AttendanceStatus.PRESENT, c.present, c.onPresent) { onSelect(AttendanceStatus.PRESENT) }
        Seg("A", selected == AttendanceStatus.ABSENT, c.absent, c.onAbsent) { onSelect(AttendanceStatus.ABSENT) }
        Seg("L", selected == AttendanceStatus.LATE, c.late, c.onLate) { onSelect(AttendanceStatus.LATE) }
    }
}

@Composable
private fun Seg(letter: String, on: Boolean, fill: Color, onColor: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) fill else Color.Transparent, label = "seg")
    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.padding(2.dp).size(44.dp),
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text(
                letter,
                style = MaterialTheme.typography.titleMedium,
                color = if (on) onColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

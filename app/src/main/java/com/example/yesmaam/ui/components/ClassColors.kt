package com.example.yesmaam.ui.components

import androidx.compose.ui.graphics.Color
import com.example.yesmaam.ui.theme.Blush
import com.example.yesmaam.ui.theme.BlushTint
import com.example.yesmaam.ui.theme.Lavender
import com.example.yesmaam.ui.theme.LavenderTint
import com.example.yesmaam.ui.theme.Peach
import com.example.yesmaam.ui.theme.PeachTint
import com.example.yesmaam.ui.theme.Sage
import com.example.yesmaam.ui.theme.SageTint

data class ClassPalette(val key: String, val color: Color, val tint: Color)

val ClassPalettes = listOf(
    ClassPalette("sage", Sage, SageTint),
    ClassPalette("peach", Peach, PeachTint),
    ClassPalette("blush", Blush, BlushTint),
    ClassPalette("lavender", Lavender, LavenderTint),
)

fun paletteFor(colorKey: String): ClassPalette =
    ClassPalettes.firstOrNull { it.key == colorKey } ?: ClassPalettes.first()

val ClassEmojis = listOf("📕", "📗", "🏺", "🎨", "🎵", "⚽", "✏️", "🔬", "💻", "🩰")

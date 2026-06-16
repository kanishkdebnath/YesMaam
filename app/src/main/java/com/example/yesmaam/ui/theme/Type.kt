package com.example.yesmaam.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.yesmaam.R

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
)

val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

private fun fraunces(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Fraunces, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun lora(size: Double, line: Int, weight: FontWeight = FontWeight.Normal, italic: Boolean = false) =
    TextStyle(
        fontFamily = Lora, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )

val YesMaamTypography = Typography(
    displayLarge = fraunces(54, 56),
    headlineMedium = fraunces(24, 28),
    headlineSmall = fraunces(23, 26),
    titleLarge = fraunces(19, 24),
    titleMedium = fraunces(16, 20),
    bodyLarge = lora(16.0, 24),
    bodyMedium = lora(14.5, 21),
    labelLarge = lora(14.5, 18, FontWeight.Medium),
    labelMedium = lora(12.5, 16, FontWeight.Medium),
    labelSmall = lora(11.5, 15, italic = true),
)

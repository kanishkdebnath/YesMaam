package com.example.yesmaam.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class YesMaamColors(
    val present: Color, val presentTint: Color, val onPresent: Color,
    val absent: Color, val absentTint: Color, val onAbsent: Color,
    val late: Color, val lateTint: Color, val onLate: Color,
    val holiday: Color, val holidayTint: Color, val onHoliday: Color,
)

val LightYesMaamColors = YesMaamColors(
    present = Sage, presentTint = SageTint, onPresent = PresentOnContainer,
    absent = Blush, absentTint = BlushTint, onAbsent = Color(0xFF6F2F33),
    late = Peach, lateTint = PeachTint, onLate = Color(0xFF7A4F17),
    holiday = Peach, holidayTint = PeachTint, onHoliday = PeachDeep,
)

val LocalYesMaamColors = staticCompositionLocalOf { LightYesMaamColors }

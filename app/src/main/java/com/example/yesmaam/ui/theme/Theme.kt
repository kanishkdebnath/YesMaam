package com.example.yesmaam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = SageDeep, onPrimary = Color.White,
    primaryContainer = SageTint, onPrimaryContainer = PresentOnContainer,
    secondary = LavenderDeep, secondaryContainer = LavenderTint,
    tertiary = BlushDeep, tertiaryContainer = BlushTint,
    error = ErrorRed, onError = Color.White,
    background = Cream, onBackground = Ink,
    surface = Paper, onSurface = Ink,
    surfaceVariant = SurfaceVariantWarm, onSurfaceVariant = InkSoft,
    outline = Line, outlineVariant = OutlineVariantWarm,
)

@Composable
fun YesMaamTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // v1 is light-only by design (design system §2.3).
    CompositionLocalProvider(LocalYesMaamColors provides LightYesMaamColors) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = YesMaamTypography,
            shapes = YesMaamShapes,
            content = content,
        )
    }
}

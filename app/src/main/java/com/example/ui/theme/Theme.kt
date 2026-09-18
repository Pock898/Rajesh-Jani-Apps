package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = StitchBlue,
    onPrimary = Color.White,
    primaryContainer = StitchBlueContainer,
    onPrimaryContainer = StitchBlueText,
    secondary = StitchBlueDark,
    onSecondary = Color.White,
    secondaryContainer = StitchBlueContainer,
    onSecondaryContainer = StitchBlueText,
    tertiary = StitchGreen,
    onTertiary = Color.White,
    tertiaryContainer = StitchGreenContainer,
    onTertiaryContainer = StitchGreen,
    error = StitchRed,
    onError = Color.White,
    errorContainer = StitchRedContainer,
    onErrorContainer = StitchRed,
    background = StitchBackground,
    onBackground = StitchTextPrimary,
    surface = StitchSurface,
    onSurface = StitchTextPrimary,
    surfaceVariant = StitchSurfaceContainer,
    onSurfaceVariant = StitchTextSecondary,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}


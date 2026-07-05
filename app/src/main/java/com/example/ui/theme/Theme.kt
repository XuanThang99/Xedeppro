package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val VibrantColorScheme =
  lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    secondary = VibrantSecondary,
    onSecondary = VibrantOnSecondary,
    tertiary = VibrantTertiary,
    onTertiary = VibrantOnTertiary,
    background = VibrantBackground,
    onBackground = VibrantOnBackground,
    surface = VibrantSurface,
    onSurface = VibrantOnSurface,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantOnSurfaceVariant,
    outline = VibrantOutline,
    outlineVariant = VibrantOutlineVariant,
    error = VibrantError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Use our gorgeous light theme by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = VibrantColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

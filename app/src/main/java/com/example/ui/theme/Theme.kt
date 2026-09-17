package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackHoleDarkColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = CosmicSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = CosmicSurfaceVariant,
    onSecondaryContainer = NeonPurple,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = CosmicBlack,
    onBackground = TextPrimary,
    surface = CosmicSurface,
    onSurface = TextPrimary,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CosmicBorder,
  )

private val BlackHoleLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0091EA),
    onPrimary = Color.White,
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
  )

@Composable
fun MyApplicationTheme(
  // Clean modern UI with dark mode default as requested
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) BlackHoleDarkColorScheme else BlackHoleLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}


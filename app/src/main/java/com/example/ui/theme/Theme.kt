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

private val DarkColorScheme = darkColorScheme(
  primary = DogCareGreenPrimaryDark,
  onPrimary = DogCareGreenOnPrimaryDark,
  primaryContainer = DogCareGreenContainerDark,
  onPrimaryContainer = DogCareGreenOnContainerDark,
  secondary = DogCareOrangeSecondaryDark,
  background = DogCareBackgroundDark,
  surface = DogCareSurfaceDark,
  tertiary = DogCareGoldTertiary
)

private val LightColorScheme = lightColorScheme(
  primary = DogCareGreenPrimary,
  onPrimary = DogCareGreenOnPrimary,
  primaryContainer = DogCareGreenContainer,
  onPrimaryContainer = DogCareGreenOnContainer,
  secondary = DogCareOrangeSecondary,
  onSecondary = DogCareOrangeOnSecondary,
  secondaryContainer = DogCareOrangeContainer,
  onSecondaryContainer = DogCareOrangeOnContainer,
  tertiary = DogCareGoldTertiary,
  tertiaryContainer = DogCareGoldContainer,
  onTertiaryContainer = DogCareGoldOnContainer,
  background = DogCareBackgroundLight,
  onBackground = DogCareOnBackgroundLight,
  surface = DogCareSurfaceLight,
  onSurface = DogCareOnSurfaceLight,
  surfaceVariant = DogCareSurfaceVariantLight,
  onSurfaceVariant = DogCareOnSurfaceVariantLight,
  error = DogCareUrgencyRed,
  errorContainer = DogCareUrgencyRedContainer
)

@Composable
fun DogCareTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep intentional brand identity
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  DogCareTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

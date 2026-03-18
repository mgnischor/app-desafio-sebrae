package tech.datatower.sebrae.desafio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme =
    lightColorScheme(
        primary = Primary40,
        onPrimary = Primary100,
        primaryContainer = Primary90,
        onPrimaryContainer = Primary10,
        secondary = Secondary40,
        onSecondary = Secondary100,
        secondaryContainer = Secondary90,
        onSecondaryContainer = Secondary10,
        tertiary = Tertiary40,
        onTertiary = Tertiary100,
        tertiaryContainer = Tertiary90,
        onTertiaryContainer = Tertiary10,
        error = Error40,
        onError = Error100,
        errorContainer = Error90,
        onErrorContainer = Error10,
        background = Neutral98,
        onBackground = Neutral10,
        surface = Neutral98,
        onSurface = Neutral10,
        surfaceVariant = NeutralVariant90,
        onSurfaceVariant = NeutralVariant30,
        outline = NeutralVariant50,
        outlineVariant = NeutralVariant80,
        inverseSurface = Neutral20,
        inverseOnSurface = Neutral95,
        inversePrimary = Primary80,
        surfaceDim = Neutral87,
        surfaceBright = Neutral98,
        surfaceContainerLowest = Neutral100,
        surfaceContainerLow = Neutral96,
        surfaceContainer = Neutral94,
        surfaceContainerHigh = Neutral92,
        surfaceContainerHighest = Neutral87,
    )

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme =
    darkColorScheme(
        primary = Primary80,
        onPrimary = Primary20,
        primaryContainer = Primary30,
        onPrimaryContainer = Primary90,
        secondary = Secondary80,
        onSecondary = Secondary20,
        secondaryContainer = Secondary30,
        onSecondaryContainer = Secondary90,
        tertiary = Tertiary80,
        onTertiary = Tertiary20,
        tertiaryContainer = Tertiary30,
        onTertiaryContainer = Tertiary90,
        error = Error80,
        onError = Error20,
        errorContainer = Error40,
        onErrorContainer = Error90,
        background = Neutral6,
        onBackground = Neutral87,
        surface = Neutral6,
        onSurface = Neutral87,
        surfaceVariant = NeutralVariant30,
        onSurfaceVariant = NeutralVariant80,
        outline = NeutralVariant60,
        outlineVariant = NeutralVariant30,
        inverseSurface = Neutral87,
        inverseOnSurface = Neutral20,
        inversePrimary = Primary40,
        surfaceDim = Neutral6,
        surfaceBright = Neutral24,
        surfaceContainerLowest = Neutral4,
        surfaceContainerLow = Neutral10,
        surfaceContainer = Neutral12,
        surfaceContainerHigh = Neutral17,
        surfaceContainerHighest = Neutral22,
    )

@Composable
fun AppDesafioSEBRAETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      @Suppress("DEPRECATION")
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

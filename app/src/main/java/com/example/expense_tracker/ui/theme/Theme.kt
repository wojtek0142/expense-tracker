package com.example.expense_tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    // Główny kolor aplikacji
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),

    // Kolor akcentowy / dopełniający
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),

    // Trzeci kolor (dekoracyjny / ostrzegawczy / rzadziej używany)
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251431),

    // Tła i powierzchnie kart/komponentów
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),

    // Błędy
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Obramowania i linie podziału
    outline = Color(0xFF73777F)
)

private val DarkColorScheme = darkColorScheme(
    // ===== PRIMARY =====
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),
    inversePrimary = Color(0xFF1565C0),

    // ===== SECONDARY =====
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF353535),
    secondaryContainer = Color(0xFF37474F),
    onSecondaryContainer = Color(0xFFECEFF1),

    // ===== TERTIARY =====
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF6A1B9A),
    onTertiaryContainer = Color(0xFFF3E5F5),

    // ===== BACKGROUND / SURFACE =====
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),           // karty
    onSurface = Color.White,                // tekst
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceTint = Color(0xFF90CAF9),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF1C1C1C),

    // ===== SURFACE CONTAINERS (Material 3) =====
    surfaceBright = Color(0xFF3A3A3A),
    surfaceDim = Color(0xFF0D0D0D),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),

    // ===== ERROR =====
    error = Color(0xFFE57373) ,
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // ===== OUTLINE / BORDER =====
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF444444),

    // ===== SCRIM =====
    scrim = Color.Black
)

@Composable
fun ExpensetrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    colorScheme = DarkColorScheme // usunac zeby miec 2 style a wyzej dac val
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
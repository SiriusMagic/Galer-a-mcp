package app.luma.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

object LumaColors {
    val Heart = Color(0xFFE35D6A)
}

/** Ajustes › Tema: light, dark or as the phone. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ajustes › Color: the accent (light / dark versions) and a hint of it in the backgrounds. Salvia is the concept's. */
enum class Accent(val light: Color, val dark: Color, val tintLight: Color, val tintDark: Color) {
    SALVIA(Color(0xFF466342), Color(0xFFA9C8A2), Color(0xFFE6EEDC), Color(0xFF2C3A2A)),
    OCEANO(Color(0xFF1F5F8B), Color(0xFF8FC3EA), Color(0xFFDDEBF6), Color(0xFF1E3446)),
    ATARDECER(Color(0xFFB0532E), Color(0xFFF2A07F), Color(0xFFF8E3D9), Color(0xFF45281D)),
    LAVANDA(Color(0xFF6A4FA3), Color(0xFFC6B3F0), Color(0xFFEAE3F7), Color(0xFF34284A)),
    GRAFITO(Color(0xFF3F4448), Color(0xFFD0D4D8), Color(0xFFE6E8EA), Color(0xFF2C2F32)),
}

@Composable fun LumaTheme(dark: Boolean = isSystemInDarkTheme(), accent: Accent = Accent.SALVIA, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(primary = accent.dark, secondaryContainer = accent.tintDark,
        background = Color(0xFF0E0F0E), surface = Color(0xFF0E0F0E), surfaceContainerHigh = Color(0xFF232523),
        onBackground = Color(0xFFECEDEA), onSurface = Color(0xFFECEDEA))
    else lightColorScheme(primary = accent.light, secondaryContainer = accent.tintLight,
        background = Color(0xFFFAFAF7), surface = Color(0xFFFAFAF7), surfaceContainerHigh = Color(0xFFECEEE8),
        onBackground = Color(0xFF20231F), onSurface = Color(0xFF20231F))
    MaterialTheme(colorScheme = colors) {
        // Icons and text outside a Surface (titles, the action pills) follow the theme: white in dark mode.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}

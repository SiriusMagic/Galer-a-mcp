package app.luma.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colors of the agreed concept (design/concepto.png): sage green on a warm white, and a dark version. */
object LumaColors {
    val Sage = Color(0xFF466342)
    val PaleSage = Color(0xFFE6EEDC)
    val Canvas = Color(0xFFFAFAF7)
    val Ink = Color(0xFF20231F)
    val Heart = Color(0xFFE35D6A)
    val Night = Color(0xFF121411)
    val NightSage = Color(0xFFA9C8A2)
}

@Composable fun LumaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(primary = LumaColors.NightSage, secondaryContainer = Color(0xFF2C3A2A),
        background = LumaColors.Night, surface = LumaColors.Night, onBackground = Color(0xFFE6E8E3), onSurface = Color(0xFFE6E8E3))
    else lightColorScheme(primary = LumaColors.Sage, secondaryContainer = LumaColors.PaleSage,
        background = LumaColors.Canvas, surface = LumaColors.Canvas, onBackground = LumaColors.Ink, onSurface = LumaColors.Ink)
    MaterialTheme(colorScheme = colors, content = content)
}

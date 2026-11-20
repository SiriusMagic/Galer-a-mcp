package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.ui.theme.Accent
import app.luma.gallery.ui.theme.ThemeMode

/** Tema: Sistema · Claro · Oscuro, and the accent color (round swatches). */
@Composable fun ThemePicker(mode: ThemeMode, accent: Accent, onMode: (ThemeMode) -> Unit, onAccent: (Accent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { m ->
            FilterChip(m == mode, { onMode(m) }, { Text(stringResource(when (m) {
                ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark
            })) })
        }
    }
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Accent.entries.forEach { a ->
            val color = if (dark) a.dark else a.light
            Box(Modifier.size(40.dp).clip(CircleShape).background(color)
                .border(if (a == accent) 3.dp else 0.dp, MaterialTheme.colorScheme.onBackground, CircleShape).clickable { onAccent(a) },
                contentAlignment = Alignment.Center) {
                if (a == accent) Icon(Icons.Default.Check, accentName(a), tint = if (dark) Color.Black else Color.White)
            }
        }
    }
    Text(accentName(accent), Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
}

@Composable private fun accentName(a: Accent) = stringResource(when (a) {
    Accent.SALVIA -> R.string.accent_sage; Accent.OCEANO -> R.string.accent_ocean; Accent.ATARDECER -> R.string.accent_sunset
    Accent.LAVANDA -> R.string.accent_lavender; Accent.GRAFITO -> R.string.accent_graphite
})

package app.luma.gallery.data

import android.content.Context
import androidx.core.content.edit
import app.luma.gallery.ui.theme.Accent
import app.luma.gallery.ui.theme.ThemeMode

/** Ajustes › Tema, kept on this phone. */
class Appearance(context: Context) {
    private val prefs = context.getSharedPreferences("luma", Context.MODE_PRIVATE)
    var mode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit { putString("theme", value.name) }
    var accent: Accent
        get() = runCatching { Accent.valueOf(prefs.getString("accent", null)!!) }.getOrDefault(Accent.SALVIA)
        set(value) = prefs.edit { putString("accent", value.name) }
}

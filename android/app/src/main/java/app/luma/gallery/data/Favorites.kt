package app.luma.gallery.data

import android.content.Context
import androidx.core.content.edit

/** Favorites stay on this phone (app preferences, no backup to the cloud: allowBackup is off). */
class Favorites(context: Context) {
    private val prefs = context.getSharedPreferences("luma", Context.MODE_PRIVATE)

    /** Favorites of 0.1.0 were bare ids (photos and videos mixed); they become photo keys once. */
    fun read(): Set<String> {
        val old = prefs.getStringSet(OLD_KEY, null)
        if (old != null) {
            write(prefs.getStringSet(KEY, emptySet()).orEmpty() + old.mapNotNull { it.toLongOrNull()?.let { id -> "i$id" } })
            prefs.edit { remove(OLD_KEY) }
        }
        return prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()
    }
    fun write(keys: Set<String>) = prefs.edit { putStringSet(KEY, keys) }

    private companion object { const val KEY = "favorites.v2"; const val OLD_KEY = "favorites" }
}

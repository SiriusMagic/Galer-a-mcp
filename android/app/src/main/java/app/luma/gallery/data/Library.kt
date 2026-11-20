package app.luma.gallery.data

/**
 * Pure rules over the loaded media (no Android calls, so they are unit tested): albums, search and filters.
 * Favorites are keyed by [Media.key].
 */
object Library {
    const val FAVORITES = "Favoritas"
    const val OTHERS = "Otros"

    /** «Favoritas» first (only when there is one), then each folder by name, each with its newest item as cover. */
    fun albums(media: List<Media>, favorites: Set<String>): List<Album> {
        val favs = media.filter { it.key in favorites }
        val folders = media.groupBy { it.folder.ifBlank { OTHERS } }
            .map { (name, items) -> Album(name, items.size, items.maxByOrNull { it.dateAdded }) }
            .sortedBy { it.name.lowercase() }
        return (if (favs.isEmpty()) emptyList() else listOf(Album(FAVORITES, favs.size, favs.maxByOrNull { it.dateAdded }, true))) + folders
    }

    /** Items of one album ([album] null = all), matching [query] in the name or folder, newest first. */
    fun filter(media: List<Media>, favorites: Set<String>, album: String?, query: String): List<Media> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return media.filter { item ->
            val inAlbum = when (album) {
                null -> true
                FAVORITES -> item.key in favorites
                else -> item.folder.ifBlank { OTHERS } == album
            }
            val text = "${item.name} ${item.folder}".lowercase()
            inAlbum && words.all { it in text }
        }.sortedByDescending { it.dateAdded }
    }

    fun toggle(favorites: Set<String>, item: Media): Set<String> =
        if (item.key in favorites) favorites - item.key else favorites + item.key
}

package app.luma.gallery.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.luma.gallery.data.Album
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media

enum class Page { PHOTOS, ALBUMS, SEARCH }

/** The one owner of what the screens show; screens read it and call its actions. */
class GalleryState(favorites: Set<String> = emptySet(), private val saveFavorites: (Set<String>) -> Unit = {}) {
    var media by mutableStateOf(emptyList<Media>()); private set
    var loaded by mutableStateOf(false); private set
    var favorites by mutableStateOf(favorites); private set
    var page by mutableStateOf(Page.PHOTOS)
    var album by mutableStateOf<String?>(null)
    var query by mutableStateOf("")
    var viewing by mutableStateOf<Media?>(null)

    fun show(items: List<Media>) { media = items; loaded = true; viewing = viewing?.let { v -> items.firstOrNull { it.key == v.key } } }

    val albums: List<Album> get() = Library.albums(media, favorites)
    /** What the grid shows: the open album (or all) on Fotos, the search results on Buscar. */
    val visible: List<Media> get() = if (page == Page.SEARCH) Library.filter(media, favorites, null, query)
        else Library.filter(media, favorites, album, "")

    fun toggleFavorite(item: Media) { favorites = Library.toggle(favorites, item); saveFavorites(favorites) }

    fun go(to: Page) { page = to; album = null }
    fun openAlbum(name: String) { album = name; page = Page.PHOTOS }
    /** Back: closes the viewer, then the album, then returns to Fotos. Returns false when there is nothing to close. */
    fun back(): Boolean = when {
        viewing != null -> { viewing = null; true }
        album != null -> { album = null; page = Page.ALBUMS; true }
        page != Page.PHOTOS -> { page = Page.PHOTOS; true }
        else -> false
    }
}

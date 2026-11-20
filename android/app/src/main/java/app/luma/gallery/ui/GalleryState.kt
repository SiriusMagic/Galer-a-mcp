package app.luma.gallery.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.luma.gallery.data.Album
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.index.ContentSearch
import app.luma.gallery.index.Facts

enum class Page { PHOTOS, ALBUMS, SEARCH, TRASH, SETTINGS }

/** The one owner of what the screens show; screens read it and call its actions. */
class GalleryState(favorites: Set<String> = emptySet(), private val saveFavorites: (Set<String>) -> Unit = {}) {
    var media by mutableStateOf(emptyList<Media>()); private set
    var trash by mutableStateOf(emptyList<Media>()); private set
    var loaded by mutableStateOf(false); private set
    var favorites by mutableStateOf(favorites); private set
    /** What the index knows, by [Media.key]. */
    var facts by mutableStateOf(emptyMap<String, Facts>()); private set
    var page by mutableStateOf(Page.PHOTOS); private set
    var album by mutableStateOf<String?>(null); private set
    var query by mutableStateOf("")
    var viewing by mutableStateOf<Media?>(null)
    /** Keys picked with a long press; selection mode while not empty. */
    var selected by mutableStateOf(emptySet<String>()); private set

    fun show(items: List<Media>, trashed: List<Media> = trash) {
        media = items; trash = trashed; loaded = true
        viewing = viewing?.let { v -> items.firstOrNull { it.key == v.key } }
        selected = selected.filterTo(HashSet()) { key -> items.any { it.key == key } || trashed.any { it.key == key } }
    }
    fun showFacts(all: List<Facts>) { facts = all.associateBy { it.key } }

    val albums: Pair<List<Album>, List<Album>> get() = Library.albums(media, favorites)
    /** Search by content (index) and, for photos not analyzed yet, by name or album. */
    val hits: List<ContentSearch.Hit> get() = ContentSearch.search(query, media, facts)
    val suggestions: List<String> get() = ContentSearch.suggestions(facts.values)
    /** What the grid shows on Fotos (or inside an album) and in the trash. */
    val visible: List<Media> get() = when (page) {
        Page.SEARCH -> hits.map { it.media }
        Page.TRASH -> trash
        else -> Library.filter(media, favorites, album)
    }
    val selection: List<Media> get() = (media + trash).filter { it.key in selected }

    fun toggleFavorite(item: Media) { favorites = Library.toggle(favorites, item); saveFavorites(favorites) }
    /** Heart on several: all become favorites, unless all already were (then none is). */
    fun favoriteSelection() {
        val keys = selection.map { it.key }.toSet()
        favorites = if (favorites.containsAll(keys)) favorites - keys else favorites + keys
        saveFavorites(favorites); selected = emptySet()
    }

    fun select(item: Media) { selected = if (item.key in selected) selected - item.key else selected + item.key }
    fun selectAll() { selected = visible.mapTo(HashSet()) { it.key } }
    fun clearSelection() { selected = emptySet() }
    /** A tap: picks while selecting, otherwise opens the viewer. */
    fun tap(item: Media) { if (selected.isNotEmpty()) select(item) else viewing = item }

    fun go(to: Page) { page = to; album = null; selected = emptySet() }
    fun openAlbum(id: String) { album = id; page = Page.PHOTOS; selected = emptySet() }
    /** Back: closes the viewer, the selection, the album, then returns to Fotos. False when there is nothing to close. */
    fun back(): Boolean = when {
        viewing != null -> { viewing = null; true }
        selected.isNotEmpty() -> { selected = emptySet(); true }
        album != null -> { album = null; page = Page.ALBUMS; true }
        page == Page.TRASH -> { page = Page.ALBUMS; true }
        page != Page.PHOTOS -> { page = Page.PHOTOS; true }
        else -> false
    }
}

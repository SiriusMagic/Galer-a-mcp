package app.luma.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.ui.screens.*

/** Fotos · Álbumes · Buscar (floating bar), Eliminado recientemente, Ajustes, and the full-screen viewer on top. */
@Composable fun LumaApp(state: GalleryState, hasPermission: Boolean, onGrant: () -> Unit, actions: GalleryActions,
    index: IndexStatus = IndexStatus(), version: String = "") {
    BackHandler(state.viewing != null || state.selected.isNotEmpty() || state.album != null || state.page != Page.PHOTOS) { state.back() }
    val select: @Composable RowScope.() -> Unit = { Action(Icons.Default.LibraryAddCheck, R.string.select) { state.selectAll() } }
    val settings: @Composable RowScope.() -> Unit = { Action(Icons.Default.Settings, R.string.settings) { state.go(Page.SETTINGS) } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            !hasPermission -> PermissionScreen(onGrant)
            !state.loaded -> EmptyState(stringResource(R.string.loading))
            state.page == Page.SETTINGS -> SettingsScreen(index, version, actions.indexNow, actions.stopIndex, actions.clearIndex) {
                Header(stringResource(R.string.settings), { state.back() }) {}
            }
            state.page == Page.ALBUMS -> {
                val (pinned, more) = state.albums
                AlbumsScreen(pinned, more, state.trash.size.takeIf { actions.trash != null }, state::openAlbum, { state.go(Page.TRASH) }) {
                    Header(stringResource(R.string.albums), null) { settings() }
                }
            }
            else -> Grid(state, select, settings, index)
        }
        if (hasPermission && state.page != Page.SETTINGS) {
            val picked = state.selection
            when {
                picked.isEmpty() -> FloatingNav(state.page) { state.go(it) }
                state.page == Page.TRASH -> SelectionBar(picked.size, listOf(
                    Triple(Icons.AutoMirrored.Filled.Undo, R.string.restore) { actions.restore(picked); state.clearSelection() },
                    Triple(Icons.Default.DeleteForever, R.string.delete_forever) { actions.deleteForever(picked); state.clearSelection() },
                    Triple(Icons.Default.Close, R.string.cancel) { state.clearSelection() }))
                else -> SelectionBar(picked.size, listOfNotNull(
                    Triple(Icons.Default.Share, R.string.share) { actions.share(picked) },
                    Triple(Icons.Default.Favorite, R.string.favorite) { state.favoriteSelection() },
                    actions.trash?.let { trash -> Triple(Icons.Default.Delete, R.string.delete) { trash(picked); state.clearSelection() } },
                    Triple(Icons.Default.Close, R.string.cancel) { state.clearSelection() }))
            }
        }
    }
    state.viewing?.let { item ->
        Viewer(state.visible.ifEmpty { listOf(item) }, item, state.favorites, state.facts,
            ViewerActions(state::toggleFavorite, { actions.share(listOf(it)) }, actions.edit, actions.trash?.let { t -> { m -> t(listOf(m)) } },
                actions.play, actions.wallpaper, actions.copy, actions.openWith),
            onShowing = { state.viewing = it }, onClose = { state.viewing = null })
    }
}

/** Fotos (by day), an album, the search results or the trash: one grid with its own header. */
@Composable private fun Grid(state: GalleryState, select: @Composable RowScope.() -> Unit, settings: @Composable RowScope.() -> Unit, index: IndexStatus) {
    val title = when (state.page) {
        Page.SEARCH -> stringResource(R.string.search)
        Page.TRASH -> stringResource(R.string.trash)
        else -> state.album?.let { id -> (state.albums.first + state.albums.second).firstOrNull { it.id == id }?.let { albumName(it) } ?: id }
            ?: stringResource(R.string.photos)
    }
    val empty = when (state.page) {
        Page.SEARCH -> if (state.query.isBlank()) "" else stringResource(R.string.search_none)
        Page.TRASH -> stringResource(R.string.trash_empty)
        else -> stringResource(R.string.no_photos)
    }
    MediaGrid(state.visible, state.favorites, state.selected, empty, byDay = state.page == Page.PHOTOS && state.album == null,
        onTap = state::tap, onLongPress = state::select) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(title, if (state.album != null || state.page == Page.TRASH) ({ state.back() }) else null) { select(); settings() }
                if (state.page == Page.SEARCH) SearchBox(state.query, { state.query = it }, state.suggestions, index, state.query.isBlank())
                if (state.page == Page.TRASH) Text(stringResource(R.string.trash_hint), Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
        }
    }
}

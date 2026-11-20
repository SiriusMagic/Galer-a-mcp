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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.data.AlbumKind
import app.luma.gallery.data.Media
import app.luma.gallery.ui.screens.*

/** Fotos · Álbumes · Memorias · Buscar, Eliminado recientemente, Bóveda, Ajustes, and the viewer on top. */
@Composable fun LumaApp(state: GalleryState, hasPermission: Boolean, onGrant: () -> Unit, actions: GalleryActions,
    index: IndexStatus = IndexStatus(), version: String = "") {
    BackHandler(state.viewing != null || state.selected.isNotEmpty() || state.album != null || state.page != Page.PHOTOS ||
        state.target != null) { state.back() }
    /** Photos waiting for «Mover / Copiar a…» (null: no dialog), and whether «+» only asks a name. */
    var toAlbum by remember { mutableStateOf<List<Media>?>(null) }
    var naming by remember { mutableStateOf(false) }
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
                AlbumsScreen(pinned, more, state.trash.size.takeIf { actions.trash != null }, state::openAlbum, { state.go(Page.TRASH) },
                    actions.openVault) {
                    Header(stringResource(R.string.albums), null) {
                        Action(Icons.Default.Add, R.string.new_album) { naming = true; toAlbum = emptyList() }; settings()
                    }
                }
            }
            state.page == Page.MEMORIES && state.memory == null -> MemoriesScreen(state.memories, { state.openMemory(it) }) {
                Header(stringResource(R.string.memories), null) { settings() }
            }
            state.page == Page.VAULT && state.vaultOpen -> VaultScreen(state.hidden, state.vaultPicked, state::pickHidden) {
                Header(stringResource(R.string.vault), { state.back() }) {}
            }
            else -> Grid(state, select, settings, index)
        }
        if (hasPermission && state.page != Page.SETTINGS) Bars(state, actions) { toAlbum = it; naming = false }
    }
    toAlbum?.let { items ->
        val folders = state.albums.second.map { it.name }
        AlbumPicker(folders, naming, onDismiss = { toAlbum = null },
            onMove = { name -> toAlbum = null; if (naming) state.startNewAlbum(name) else { actions.move(items, name); state.clearSelection() } },
            onCopy = { name -> toAlbum = null; actions.copy(items, name); state.clearSelection() })
    }
    state.viewing?.let { item ->
        Viewer(state.visible.ifEmpty { listOf(item) }, item, state.favorites, state.facts,
            ViewerActions(state::toggleFavorite, { actions.share(listOf(it)) }, actions.edit, actions.trash?.let { t -> { m -> t(listOf(m)) } },
                actions.play, actions.wallpaper, actions.copyClipboard, actions.openWith, toAlbum = { toAlbum = listOf(it); naming = false },
                hide = { actions.hide(listOf(it)) }, pdf = { actions.pdf(listOf(it)) }),
            onShowing = { state.viewing = it }, onClose = { state.viewing = null })
    }
}

/** The floating bar, or the selection bar with what can be done to the picked items (or the vault's). */
@Composable private fun BoxScope.Bars(state: GalleryState, actions: GalleryActions, onToAlbum: (List<Media>) -> Unit) {
    val picked = state.selection
    val target = state.target
    when {
        state.page == Page.VAULT && state.vaultPicked.isNotEmpty() -> SelectionBar(state.vaultPicked.size, listOf(
            Triple(Icons.Default.Visibility, R.string.unhide) { actions.unhide(state.hiddenPicked); state.clearHiddenPick() },
            Triple(Icons.Default.DeleteForever, R.string.delete_forever) { actions.eraseHidden(state.hiddenPicked); state.clearHiddenPick() },
            Triple(Icons.Default.Close, R.string.cancel) { state.clearHiddenPick() }))
        target != null -> SelectionBar(picked.size, listOf(
            Triple(Icons.Default.DriveFileMove, R.string.move) { actions.move(picked, target); state.finishNewAlbum() },
            Triple(Icons.Default.ContentCopy, R.string.copy) { actions.copy(picked, target); state.finishNewAlbum() },
            Triple(Icons.Default.Close, R.string.cancel) { state.finishNewAlbum() }))
        picked.isEmpty() -> FloatingNav(state.page) { state.go(it) }
        state.page == Page.TRASH -> SelectionBar(picked.size, listOf(
            Triple(Icons.AutoMirrored.Filled.Undo, R.string.restore) { actions.restore(picked); state.clearSelection() },
            Triple(Icons.Default.DeleteForever, R.string.delete_forever) { actions.deleteForever(picked); state.clearSelection() },
            Triple(Icons.Default.Close, R.string.cancel) { state.clearSelection() }))
        else -> SelectionBar(picked.size, listOfNotNull(
            Triple(Icons.Default.Share, R.string.share) { actions.share(picked) },
            Triple(Icons.Default.Favorite, R.string.favorite) { state.favoriteSelection() },
            Triple(Icons.Default.PhotoAlbum, R.string.to_album) { onToAlbum(picked) },
            actions.trash?.let { trash -> Triple(Icons.Default.Delete, R.string.delete) { trash(picked); state.clearSelection() } },
            Triple(Icons.Default.Close, R.string.cancel) { state.clearSelection() }),
            more = listOf(R.string.hide to { actions.hide(picked); state.clearSelection() },
                R.string.export_pdf to { actions.pdf(picked); state.clearSelection() }))
    }
}

/** Fotos (by day), an album, a memory, the search results or the trash: one grid with its own header. */
@Composable private fun Grid(state: GalleryState, select: @Composable RowScope.() -> Unit, settings: @Composable RowScope.() -> Unit, index: IndexStatus) {
    val title = when {
        state.target != null -> stringResource(R.string.pick_for, state.target!!)
        state.page == Page.SEARCH -> stringResource(R.string.search)
        state.page == Page.TRASH -> stringResource(R.string.trash)
        state.page == Page.MEMORIES -> state.memories.firstOrNull { it.id == state.memory }?.let { title(it) }.orEmpty()
        else -> state.album?.let { id -> (state.albums.first + state.albums.second).firstOrNull { it.id == id }?.let { albumName(it) } ?: id }
            ?: stringResource(R.string.photos)
    }
    val empty = when (state.page) {
        Page.SEARCH -> if (state.query.isBlank()) "" else stringResource(R.string.search_none)
        Page.TRASH -> stringResource(R.string.trash_empty)
        else -> stringResource(R.string.no_photos)
    }
    val back = state.album != null || state.page == Page.TRASH || state.memory != null || state.target != null
    MediaGrid(state.visible, state.favorites, state.selected, empty,
        byDay = state.page == Page.PHOTOS && (state.album == null || state.album == app.luma.gallery.data.Library.builtIn(AlbumKind.ALL)),
        onTap = { if (state.target != null) state.select(it) else state.tap(it) }, onLongPress = state::select) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(title, if (back) ({ state.back() }) else null) { if (state.target == null) { select(); settings() } }
                if (state.page == Page.SEARCH) SearchBox(state.query, { state.query = it }, state.suggestions, index, state.query.isBlank())
                if (state.page == Page.TRASH) Text(stringResource(R.string.trash_hint), Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
        }
    }
}

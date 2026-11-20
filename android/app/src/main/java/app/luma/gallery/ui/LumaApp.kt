package app.luma.gallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.luma.gallery.R
import app.luma.gallery.data.Library
import app.luma.gallery.ui.screens.*

/** Fotos · Álbumes · Buscar (bottom bar), the title of the concept, and the full-screen viewer on top. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LumaApp(state: GalleryState, hasPermission: Boolean, onGrant: () -> Unit, actions: ViewerActions) {
    BackHandler(state.viewing != null || state.album != null || state.page != Page.PHOTOS) { state.back() }
    val title = when {
        state.album == Library.FAVORITES -> stringResource(R.string.favorites)
        state.album != null -> state.album!!
        state.page == Page.ALBUMS -> stringResource(R.string.albums)
        state.page == Page.SEARCH -> stringResource(R.string.search)
        else -> stringResource(R.string.photos)
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.app_name), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }, navigationIcon = {
                if (state.album != null) IconButton({ state.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), actions = {
                IconButton({ state.go(Page.SEARCH) }) { Icon(Icons.Default.Search, stringResource(R.string.search)) }
            })
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavItem(state, Page.PHOTOS, Icons.Default.Image, R.string.photos)
                NavItem(state, Page.ALBUMS, Icons.Default.Folder, R.string.albums)
                NavItem(state, Page.SEARCH, Icons.Default.Search, R.string.search)
            }
        },
    ) { padding ->
        val inner = Modifier.padding(padding)
        when {
            !hasPermission -> PermissionScreen(onGrant, inner)
            !state.loaded -> EmptyState(stringResource(R.string.loading))
            state.page == Page.ALBUMS && state.album == null -> AlbumsScreen(state.albums, state::openAlbum, inner)
            state.page == Page.SEARCH -> SearchScreen(state.query, { state.query = it }, state.visible, state.favorites,
                { state.viewing = it }, inner)
            else -> MediaGrid(state.visible, state.favorites, stringResource(R.string.no_photos), { state.viewing = it }, inner)
        }
    }
    state.viewing?.let { item ->
        Viewer(state.visible.ifEmpty { listOf(item) }, item, state.favorites,
            ViewerActions(state::toggleFavorite, actions.share, actions.edit, actions.delete, actions.play),
            onShowing = { state.viewing = it }, onClose = { state.viewing = null })
    }
}

@Composable private fun androidx.compose.foundation.layout.RowScope.NavItem(state: GalleryState, page: Page,
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int) {
    NavigationBarItem(selected = state.page == page && (page != Page.PHOTOS || state.album == null), onClick = { state.go(page) },
        icon = { Icon(icon, null) }, label = { Text(stringResource(label)) })
}

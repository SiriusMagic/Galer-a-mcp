package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R
import app.luma.gallery.data.Album
import app.luma.gallery.data.AlbumKind
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage
import java.text.NumberFormat

/** Built-in albums, «Más álbumes» (folders) and, on Android 11+, «Eliminado recientemente». */
@Composable fun AlbumsScreen(pinned: List<Album>, more: List<Album>, trashCount: Int?, onOpen: (String) -> Unit, onTrash: () -> Unit,
    modifier: Modifier = Modifier, header: @Composable () -> Unit) {
    val number = NumberFormat.getIntegerInstance()
    LazyVerticalGrid(GridCells.Fixed(3), modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(pinned, key = { it.id }) { AlbumTile(it, number, onOpen) }
        if (more.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.more_albums), Modifier.padding(top = 18.dp, start = 4.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        items(more, key = { "f-" + it.id }) { AlbumTile(it, number, onOpen) }
        if (trashCount != null) item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onTrash).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, null)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(stringResource(R.string.trash), fontSize = 18.sp)
                    Text(number.format(trashCount), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    }
}

@Composable private fun AlbumTile(album: Album, number: NumberFormat, onOpen: (String) -> Unit) {
    Column(Modifier.clickable { onOpen(album.id) }) {
        val cover = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
        Box(cover, contentAlignment = Alignment.Center) {
            if (album.cover != null) AsyncImage(album.cover.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.PhotoAlbum, null, tint = MaterialTheme.colorScheme.primary)
            // Favoritas carries a big heart on its cover, as in the phone's gallery.
            if (album.kind == AlbumKind.FAVORITES) Icon(Icons.Default.Favorite, null, Modifier.size(40.dp), tint = LumaColors.Heart)
        }
        Text(albumName(album), Modifier.padding(top = 6.dp), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(number.format(album.count), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 14.sp)
    }
}

@Composable fun albumName(album: Album) = when (album.kind) {
    AlbumKind.ALL -> stringResource(R.string.album_all)
    AlbumKind.CAMERA -> stringResource(R.string.album_camera)
    AlbumKind.VIDEOS -> stringResource(R.string.album_videos)
    AlbumKind.SCREENSHOTS -> stringResource(R.string.album_screenshots)
    AlbumKind.FAVORITES -> stringResource(R.string.favorites)
    AlbumKind.FOLDER -> album.name
}

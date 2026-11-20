package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import app.luma.gallery.R
import app.luma.gallery.data.Album
import coil.compose.AsyncImage
import java.text.NumberFormat

/** Two columns of albums (folders, and «Favoritas» first), each with its newest item as cover. */
@Composable fun AlbumsScreen(albums: List<Album>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    if (albums.isEmpty()) return EmptyState(stringResource(R.string.no_albums))
    val number = NumberFormat.getIntegerInstance()
    LazyVerticalGrid(GridCells.Fixed(2), modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums, key = { it.name }) { album ->
            Column(Modifier.clickable { onOpen(album.name) }) {
                val cover = Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(16.dp))
                if (album.cover != null) AsyncImage(album.cover.uri, album.name,
                    cover.background(MaterialTheme.colorScheme.secondaryContainer), contentScale = ContentScale.Crop)
                else Box(cover.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(if (album.favorites) Icons.Default.Favorite else Icons.Default.PhotoAlbum, null, Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                Text(if (album.favorites) stringResource(R.string.favorites) else album.name, Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(number.format(album.count), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

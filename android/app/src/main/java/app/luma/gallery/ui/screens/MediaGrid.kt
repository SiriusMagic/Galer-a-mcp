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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.data.Media
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage

/** Three square columns, as in the concept; a heart marks favorites and a play sign marks videos. */
@Composable fun MediaGrid(items: List<Media>, favorites: Set<String>, empty: String, onOpen: (Media) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Text(pluralStringResource(R.plurals.items, items.size, items.size), Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), style = MaterialTheme.typography.bodySmall)
        if (items.isEmpty()) EmptyState(empty)
        else LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(items, key = { it.key }) { item ->
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer).clickable { onOpen(item) }) {
                    AsyncImage(item.uri, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (item.isVideo) Icon(Icons.Default.PlayCircle, null, Modifier.align(Alignment.Center).size(30.dp), tint = Color.White)
                    if (item.key in favorites) Icon(Icons.Default.Favorite, null, Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                        tint = LumaColors.Heart)
                }
            }
        }
    }
}

@Composable fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    }
}

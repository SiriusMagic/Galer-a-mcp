package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.data.Hidden
import coil.compose.AsyncImage

/** Bóveda: what you hid, only after the phone's lock. Tap to pick; then Restaurar or Borrar para siempre. */
@Composable fun VaultScreen(items: List<Hidden>, picked: Set<String>, onPick: (Hidden) -> Unit, modifier: Modifier = Modifier,
    header: @Composable () -> Unit) {
    LazyVerticalGrid(GridCells.Fixed(4), modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                header()
                Text(stringResource(if (items.isEmpty()) R.string.vault_empty else R.string.vault_hint), Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
        }
        items(items, key = { it.id }) { item ->
            val on = item.id in picked
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.secondaryContainer).clickable { onPick(item) }) {
                AsyncImage(item.file, item.name, Modifier.fillMaxSize().padding(if (on) 10.dp else 0.dp), contentScale = ContentScale.Crop)
                if (item.isVideo) Icon(Icons.Default.PlayCircle, null, Modifier.align(Alignment.Center), tint = Color.White)
                if (picked.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = .25f)).border(2.dp, Color.White, CircleShape)) {
                    if (on) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                }
            }
        }
    }
}

package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.luma.gallery.R
import app.luma.gallery.data.Media
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage

/** What the viewer's buttons do; the screen only reports them. */
class ViewerActions(val favorite: (Media) -> Unit, val share: (Media) -> Unit, val edit: (Media) -> Unit,
    val delete: ((Media) -> Unit)?, val play: (Media) -> Unit)

/** Full screen, black, swipe between items; back, and favorite · share · edit · delete at the bottom (as in the concept). */
@Composable fun Viewer(items: List<Media>, start: Media, favorites: Set<String>, actions: ViewerActions,
    onShowing: (Media) -> Unit, onClose: () -> Unit) {
    val first = items.indexOfFirst { it.key == start.key }.coerceAtLeast(0)
    val pager = rememberPagerState(first) { items.size }
    LaunchedEffect(pager, items) { snapshotFlow { pager.currentPage }.collect { items.getOrNull(it)?.let(onShowing) } }
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), key = { items[it].key }) { page ->
                val item = items[page]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(item.uri, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    if (item.isVideo) Icon(Icons.Default.PlayCircle, stringResource(R.string.play),
                        Modifier.size(72.dp).clip(CircleShape).clickable { actions.play(item) }, tint = Color.White)
                }
            }
            val item = items.getOrNull(pager.currentPage) ?: start
            Row(Modifier.fillMaxWidth().systemBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose, Modifier.clip(CircleShape).background(Color.Black.copy(alpha = .45f))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
                Text(item.name, Modifier.weight(1f).padding(horizontal = 12.dp), color = Color.White, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha = .55f)).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                val fav = item.key in favorites
                IconButton({ actions.favorite(item) }) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.favorite),
                        tint = if (fav) LumaColors.Heart else Color.White)
                }
                IconButton({ actions.share(item) }) { Icon(Icons.Default.Share, stringResource(R.string.share), tint = Color.White) }
                IconButton({ actions.edit(item) }) { Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = Color.White) }
                actions.delete?.let { delete ->
                    IconButton({ delete(item) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Color.White) }
                }
            }
        }
    }
}

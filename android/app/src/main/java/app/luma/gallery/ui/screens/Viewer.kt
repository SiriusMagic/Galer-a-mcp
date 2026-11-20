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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.luma.gallery.R
import app.luma.gallery.data.Media
import app.luma.gallery.index.Facts
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage
import java.text.DateFormat
import java.util.Date

/** What the viewer's buttons do; the screen only reports them. */
class ViewerActions(val favorite: (Media) -> Unit, val share: (Media) -> Unit, val edit: (Media) -> Unit,
    val delete: ((Media) -> Unit)?, val play: (Media) -> Unit, val wallpaper: (Media) -> Unit = {},
    val copy: (Media) -> Unit = {}, val openWith: (Media) -> Unit = {})

/** Full screen, black, swipe between items; date on top; share · heart · edit · delete · more at the bottom. */
@Composable fun Viewer(items: List<Media>, start: Media, favorites: Set<String>, facts: Map<String, Facts>, actions: ViewerActions,
    onShowing: (Media) -> Unit, onClose: () -> Unit) {
    val first = items.indexOfFirst { it.key == start.key }.coerceAtLeast(0)
    val pager = rememberPagerState(first) { items.size }
    var menu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
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
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .45f)).statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose, Modifier.clip(CircleShape).background(Color.White.copy(alpha = .12f))) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.takenAt)), color = Color.White, fontSize = 20.sp)
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.takenAt)), color = Color.White.copy(alpha = .7f), fontSize = 14.sp)
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth()
                .clip(RoundedCornerShape(32.dp)).background(Color(0xFF1E1E1E).copy(alpha = .9f)).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                val fav = item.key in favorites
                Bar(Icons.Default.Share, R.string.share) { actions.share(item) }
                IconButton({ actions.favorite(item) }) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.favorite),
                        Modifier.size(28.dp), tint = if (fav) LumaColors.Heart else Color.White)
                }
                Bar(Icons.Default.Edit, R.string.edit) { actions.edit(item) }
                actions.delete?.let { delete -> Bar(Icons.Default.Delete, R.string.delete) { delete(item) } }
                Box {
                    Bar(Icons.Default.MoreVert, R.string.more) { menu = true }
                    DropdownMenu(menu, { menu = false }) {
                        Item(R.string.details) { menu = false; details = true }
                        if (!item.isVideo) Item(R.string.wallpaper) { menu = false; actions.wallpaper(item) }
                        Item(R.string.copy_clipboard) { menu = false; actions.copy(item) }
                        Item(R.string.open_with) { menu = false; actions.openWith(item) }
                    }
                }
            }
            if (details) DetailsDialog(item, facts[item.key]) { details = false }
        }
    }
}

@Composable private fun Bar(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, onClick: () -> Unit) {
    IconButton(onClick) { Icon(icon, stringResource(label), Modifier.size(28.dp), tint = Color.White) }
}

@Composable private fun Item(label: Int, onClick: () -> Unit) = DropdownMenuItem({ Text(stringResource(label)) }, onClick)

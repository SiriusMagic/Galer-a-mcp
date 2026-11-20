package app.luma.gallery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Edge-to-edge grid like the phone's gallery: optional day headers, video length, heart, and checks while selecting. */
@Composable fun MediaGrid(items: List<Media>, favorites: Set<String>, selected: Set<String>, empty: String, byDay: Boolean,
    onTap: (Media) -> Unit, onLongPress: (Media) -> Unit, modifier: Modifier = Modifier, header: LazyGridScope.() -> Unit = {}) {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    LazyVerticalGrid(GridCells.Fixed(4), modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        header()
        if (items.isEmpty() && empty.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(empty, Modifier.fillMaxWidth().padding(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        val sections: List<Pair<LocalDate?, List<Media>>> = if (byDay) Library.byDay(items) else listOf(null to items)
        sections.forEach { (day, list) ->
            if (day != null) item(key = "day-$day", span = { GridItemSpan(maxLineSpan) }) {
                Text(dayLabel(day, format), Modifier.padding(start = 18.dp, top = 18.dp, bottom = 10.dp), fontSize = 18.sp,
                    fontWeight = FontWeight.Medium)
            }
            items(list, key = { it.key }) { item -> Tile(item, item.key in favorites, selected.isNotEmpty(), item.key in selected, onTap, onLongPress) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun Tile(item: Media, favorite: Boolean, selecting: Boolean, picked: Boolean, onTap: (Media) -> Unit, onLongPress: (Media) -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.secondaryContainer)
        .combinedClickable(onLongClick = { onLongPress(item) }) { onTap(item) }) {
        AsyncImage(item.uri, item.name, Modifier.fillMaxSize().padding(if (picked) 10.dp else 0.dp), contentScale = ContentScale.Crop)
        if (item.isVideo) Text(Library.duration(item.durationMs), Modifier.align(Alignment.BottomStart).padding(6.dp),
            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (favorite) Icon(Icons.Default.Favorite, null, Modifier.align(Alignment.BottomEnd).padding(6.dp).size(15.dp), tint = LumaColors.Heart)
        if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
            .background(if (picked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = .25f))
            .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            if (picked) Icon(Icons.Default.CheckCircle, null, Modifier.size(22.dp), tint = Color.White)
        }
    }
}

/** «Hoy», «Ayer», or the date. */
@Composable private fun dayLabel(day: LocalDate, format: DateTimeFormatter): String {
    val today = LocalDate.now()
    return when (day) {
        today -> androidx.compose.ui.res.stringResource(app.luma.gallery.R.string.today)
        today.minusDays(1) -> androidx.compose.ui.res.stringResource(app.luma.gallery.R.string.yesterday)
        else -> day.format(format)
    }
}

@Composable fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
    }
}

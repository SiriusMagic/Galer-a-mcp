package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import app.luma.gallery.data.Zoom
import app.luma.gallery.ui.theme.LumaColors
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Edge-to-edge grid like the phone's gallery: optional day / month / year headers ([zoom], changed by pinching when
 * [onZoom] is set), video length, heart, and checks while selecting. Long-press and slide to pick several.
 */
@Composable fun MediaGrid(items: List<Media>, favorites: Set<String>, selected: Set<String>, empty: String, byDay: Boolean,
    onTap: (Media) -> Unit, onSelection: (Set<String>) -> Unit, modifier: Modifier = Modifier, zoom: Zoom = Zoom.DAY,
    onZoom: ((Zoom) -> Unit)? = null, header: LazyGridScope.() -> Unit = {}) {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val level = if (byDay) zoom else Zoom.DAY
    val sections: List<Pair<LocalDate?, List<Media>>> = if (byDay) Library.sections(items, level) else listOf(null to items)
    val order = sections.flatMap { (_, list) -> list.map { it.key } }
    val latestOrder by rememberUpdatedState(order)
    val latestSelected by rememberUpdatedState(selected)
    var gestures = modifier.fillMaxSize().dragToSelect(grid, scope, { latestOrder }, { latestSelected }, onSelection)
    if (onZoom != null) gestures = gestures.pinchSteps({ onZoom(level.coarser()) }, { onZoom(level.finer()) })
    LazyVerticalGrid(GridCells.Fixed(level.columns), gestures, state = grid, contentPadding = PaddingValues(bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(if (level == Zoom.DAY) 2.dp else 1.dp),
        verticalArrangement = Arrangement.spacedBy(if (level == Zoom.DAY) 2.dp else 1.dp)) {
        header()
        if (items.isEmpty() && empty.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(empty, Modifier.fillMaxWidth().padding(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        sections.forEach { (day, list) ->
            if (day != null) item(key = "day-$day", span = { GridItemSpan(maxLineSpan) }) {
                Text(sectionLabel(day, level, format), Modifier.padding(start = 18.dp, top = 18.dp, bottom = 10.dp), fontSize = 18.sp,
                    fontWeight = FontWeight.Medium)
            }
            items(list, key = { it.key }) { item -> Tile(item, item.key in favorites, selected.isNotEmpty(), item.key in selected, level, onTap) }
        }
    }
}

@Composable private fun Tile(item: Media, favorite: Boolean, selecting: Boolean, picked: Boolean, zoom: Zoom, onTap: (Media) -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.secondaryContainer).clickable { onTap(item) }) {
        AsyncImage(item.uri, item.name, Modifier.fillMaxSize().padding(if (picked) 10.dp else 0.dp), contentScale = ContentScale.Crop)
        if (item.isVideo && zoom == Zoom.DAY) Text(Library.duration(item.durationMs), Modifier.align(Alignment.BottomStart).padding(6.dp),
            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (favorite && zoom == Zoom.DAY) Icon(Icons.Default.Favorite, null, Modifier.align(Alignment.BottomEnd).padding(6.dp).size(15.dp), tint = LumaColors.Heart)
        if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape)
            .background(if (picked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = .25f))
            .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            if (picked) Icon(Icons.Default.CheckCircle, null, Modifier.size(22.dp), tint = Color.White)
        }
    }
}

/** «noviembre 2026» or «2026» when pinched; otherwise «Hoy», «Ayer», or the date. */
@Composable private fun sectionLabel(day: LocalDate, zoom: Zoom, format: DateTimeFormatter): String {
    if (zoom == Zoom.YEAR) return day.year.toString()
    if (zoom == Zoom.MONTH) return DateTimeFormatter.ofPattern("LLLL yyyy", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(day)
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

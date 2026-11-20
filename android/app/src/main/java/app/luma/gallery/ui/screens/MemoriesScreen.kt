package app.luma.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R
import app.luma.gallery.data.Memory
import app.luma.gallery.index.Labels
import coil.compose.AsyncImage
import java.time.Month
import java.time.format.TextStyle

/** Memorias: big cards («Un día como hoy», the last months, things that appear a lot); a tap opens its photos. */
@Composable fun MemoriesScreen(memories: List<Memory>, onOpen: (String) -> Unit, modifier: Modifier = Modifier, header: @Composable () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item { header() }
        if (memories.isEmpty()) item {
            Text(stringResource(R.string.memories_empty), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        items(memories, key = { it.id }) { memory ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer).clickable { onOpen(memory.id) }) {
                AsyncImage(memory.cover.uri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(title(memory), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(pluralStringResource(R.plurals.photos_count, memory.items.size, memory.items.size), color = Color.White.copy(alpha = .8f))
                }
            }
        }
    }
}

@Composable fun title(memory: Memory): String = when (memory.kind) {
    Memory.Kind.ON_THIS_DAY -> stringResource(R.string.memory_on_this_day, memory.year)
    Memory.Kind.MONTH -> Month.of(memory.month).getDisplayName(TextStyle.FULL_STANDALONE, LocalConfiguration.current.locales[0])
        .replaceFirstChar { it.titlecase() } + " " + memory.year
    Memory.Kind.THEME -> Labels.spanish(memory.label).replaceFirstChar { it.titlecase() }
}

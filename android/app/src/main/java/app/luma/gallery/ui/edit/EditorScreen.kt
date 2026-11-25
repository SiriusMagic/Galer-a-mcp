package app.luma.gallery.ui.edit

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R
import app.luma.gallery.edit.Tool
import kotlinx.coroutines.launch

/** The editor's look: black, white text, the chosen section in blue. */
object EditColors {
    val Back = Color.Black
    val Button = Color(0xFF1E1E1E)
    val Muted = Color(0xFF8A8A8A)
    val Active = Color(0xFF6CC3F5)
}

/**
 * «Editar»: the photo in the middle; at the bottom the sections (Estudio IA, Recortar, Ajustes, Filtros, Dibujar,
 * Mosaico, Texto), or the open section with ✕ and ✓. [save] writes a copy and says whether it worked.
 */
@Composable fun EditorScreen(state: EditorState, onClose: () -> Unit, save: suspend (Bitmap) -> Boolean) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    BackHandler { if (state.tool != null) state.cancel() else onClose() }
    Column(Modifier.fillMaxSize().background(EditColors.Back).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
            Text(stringResource(R.string.edit), Modifier.padding(start = 8.dp).weight(1f), color = Color.White, fontSize = 22.sp)
            if (state.tool == null && state.changed) IconButton(state::undo) {
                Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.edit_undo), tint = Color.White)
            }
            val canSave = state.tool == null && state.changed && !state.busy && !saving
            IconButton({ state.picture?.let { p -> saving = true; scope.launch { if (save(p)) onClose(); saving = false } } }, enabled = canSave) {
                Icon(Icons.Default.Save, stringResource(R.string.edit_save), tint = if (canSave) Color.White else EditColors.Muted)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            state.picture?.let { EditCanvas(state, it) } ?: CircularProgressIndicator(color = Color.White)
            if (state.busy || saving) CircularProgressIndicator(Modifier.size(36.dp), color = EditColors.Active)
        }
        when (val tool = state.tool) {
            null -> Tools(state)
            else -> {
                ToolPanel(state, tool)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(state::cancel) { Icon(Icons.Default.Close, stringResource(R.string.cancel), Modifier.size(30.dp), tint = Color.White) }
                    Text(stringResource(label(tool)), Modifier.weight(1f), color = Color.White, fontSize = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(state::apply, enabled = !state.busy) { Icon(Icons.Default.Check, stringResource(R.string.edit_apply), Modifier.size(30.dp), tint = Color.White) }
                }
            }
        }
    }
}

@Composable private fun ToolPanel(state: EditorState, tool: Tool) = when (tool) {
    Tool.STUDIO -> StudioPanel(state)
    Tool.CROP -> CropPanel(state)
    Tool.ADJUST -> AdjustPanel(state)
    Tool.FILTERS -> FilterPanel(state)
    Tool.DRAW -> DrawPanel(state)
    Tool.MOSAIC -> MosaicPanel(state)
    Tool.TEXT -> TextPanel(state)
}

/** The sections, side by side (they scroll on narrow phones). */
@Composable private fun Tools(state: EditorState) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tool.entries.forEach { tool ->
            val tint = if (tool == Tool.STUDIO) EditColors.Active else Color.White
            Column(Modifier.width(78.dp).clickable(enabled = !state.busy) { state.open(tool) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon(tool), null, Modifier.size(30.dp), tint = tint)
                Text(stringResource(label(tool)), Modifier.padding(top = 6.dp), color = tint, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

fun label(tool: Tool) = when (tool) {
    Tool.STUDIO -> R.string.edit_studio; Tool.CROP -> R.string.edit_crop; Tool.ADJUST -> R.string.edit_adjust
    Tool.FILTERS -> R.string.edit_filters; Tool.DRAW -> R.string.edit_draw; Tool.MOSAIC -> R.string.edit_mosaic; Tool.TEXT -> R.string.edit_text
}

private fun icon(tool: Tool): ImageVector = when (tool) {
    Tool.STUDIO -> Icons.Default.AutoAwesome; Tool.CROP -> Icons.Default.Crop; Tool.ADJUST -> Icons.Default.Tune
    Tool.FILTERS -> Icons.Default.Gradient; Tool.DRAW -> Icons.Default.Gesture; Tool.MOSAIC -> Icons.Default.GridOn
    Tool.TEXT -> Icons.Default.TextFields
}

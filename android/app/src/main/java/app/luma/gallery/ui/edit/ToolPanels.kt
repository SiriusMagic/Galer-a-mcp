package app.luma.gallery.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import app.luma.gallery.R
import app.luma.gallery.edit.AdjustKind
import app.luma.gallery.edit.Aspect
import app.luma.gallery.edit.ColorMath
import app.luma.gallery.edit.Filter

/** «Estudio IA»: improvements worked out on the phone from the photo itself (no internet). */
@Composable fun StudioPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Hint(R.string.edit_studio_hint)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Big(Icons.Default.AutoFixHigh, R.string.edit_enhance, Modifier.weight(1f), !state.busy) { state.enhance() }
        Big(Icons.Default.FilterCenterFocus, R.string.edit_sharpen, Modifier.weight(1f), !state.busy) { state.sharpen() }
    }
}

/** «Recortar»: the frame's shape, and turn or mirror the photo. */
@Composable fun CropPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Chips(Aspect.entries, state.aspect, { stringResource(aspectName(it)) }) { state.chooseAspect(it) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Big(Icons.Default.RotateRight, R.string.edit_rotate, Modifier.weight(1f), !state.busy) { state.turn() }
        Big(Icons.Default.Flip, R.string.edit_flip, Modifier.weight(1f), !state.busy) { state.mirror() }
    }
}

/** «Ajustes»: pick brightness, contrast, saturation or warmth, then slide. */
@Composable fun AdjustPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Chips(AdjustKind.entries, state.adjustKind, { stringResource(adjustName(it)) }) { state.adjustKind = it }
    val value = ColorMath.get(state.adjust, state.adjustKind)
    Text("%+d".format((value * 100).toInt()), Modifier.fillMaxWidth(), color = Color.White, fontSize = 14.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Slider(value, state::setAdjust, Modifier.padding(horizontal = 24.dp), valueRange = -1f..1f,
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = EditColors.Active, inactiveTrackColor = EditColors.Button))
}

/** «Filtros»: each one shown on a small copy of the photo. */
@Composable fun FilterPanel(state: EditorState) {
    val small = remember(state.picture) { state.picture?.let { p -> val k = 120f / maxOf(p.width, p.height); p.scale((p.width * k).toInt().coerceAtLeast(1), (p.height * k).toInt().coerceAtLeast(1)).asImageBitmap() } }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Filter.entries.forEach { f ->
            val on = state.filter == f
            Column(Modifier.width(76.dp).clickable { state.filter = f }, horizontalAlignment = Alignment.CenterHorizontally) {
                small?.let {
                    Image(it, null, Modifier.size(68.dp).clip(RoundedCornerShape(10.dp)).border(if (on) 3.dp else 0.dp, Color.White, RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop, colorFilter = ColorFilter.colorMatrix(ColorMatrix(ColorMath.filter(f))))
                }
                Text(stringResource(filterName(f)), Modifier.padding(top = 4.dp), color = if (on) Color.White else EditColors.Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

/** «Texto»: write, «Añadir» puts it in the middle; drag it on the photo to move it. */
@Composable fun TextPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(text, { text = it.take(120) }, Modifier.weight(1f), singleLine = true,
            placeholder = { Text(stringResource(R.string.edit_text_hint)) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        TextButton({ state.addText(text); text = "" }, Modifier.padding(start = 8.dp), enabled = text.isNotBlank()) { Text(stringResource(R.string.edit_text_add)) }
    }
    ColorRow(state.color) { state.color = it }
    SizeSlider(state.size) { state.size = it }
}

@Composable private fun Hint(text: Int) =
    Text(stringResource(text), Modifier.padding(horizontal = 24.dp), color = EditColors.Muted, fontSize = 14.sp)

@Composable private fun Big(icon: ImageVector, label: Int, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(EditColors.Button).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(28.dp), tint = Color.White)
        Text(stringResource(label), Modifier.padding(top = 6.dp), color = Color.White, fontSize = 14.sp)
    }
}

@Composable private fun <T> Chips(items: List<T>, chosen: T, name: @Composable (T) -> String, onPick: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val on = item == chosen
            Text(name(item), Modifier.clip(CircleShape).background(if (on) Color.White else EditColors.Button).clickable { onPick(item) }
                .padding(horizontal = 16.dp, vertical = 9.dp), color = if (on) Color.Black else Color.White, fontSize = 14.sp)
        }
    }
}

private fun aspectName(a: Aspect) = when (a) {
    Aspect.FREE -> R.string.aspect_free; Aspect.SQUARE -> R.string.aspect_square; Aspect.R4_3 -> R.string.aspect_4_3
    Aspect.R3_4 -> R.string.aspect_3_4; Aspect.R16_9 -> R.string.aspect_16_9; Aspect.R9_16 -> R.string.aspect_9_16
}

private fun adjustName(k: AdjustKind) = when (k) {
    AdjustKind.BRIGHTNESS -> R.string.adjust_brightness; AdjustKind.CONTRAST -> R.string.adjust_contrast
    AdjustKind.SATURATION -> R.string.adjust_saturation; AdjustKind.WARMTH -> R.string.adjust_warmth
}

private fun filterName(f: Filter) = when (f) {
    Filter.NONE -> R.string.filter_none; Filter.VIVID -> R.string.filter_vivid; Filter.MONO -> R.string.filter_mono
    Filter.SEPIA -> R.string.filter_sepia; Filter.COOL -> R.string.filter_cool; Filter.WARM -> R.string.filter_warm
    Filter.VINTAGE -> R.string.filter_vintage; Filter.NOIR -> R.string.filter_noir
}

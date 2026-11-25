package app.luma.gallery.ui.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R
import app.luma.gallery.edit.Brush
import app.luma.gallery.edit.EditRules

/** «Dibujar»: thickness, colour and brush. */
@Composable fun DrawPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SizeSlider(state.size) { state.size = it }
    ColorRow(state.color) { state.color = it }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Brush.entries.forEach { b ->
            val on = state.brush == b
            val name = brushName(b)
            Canvas(Modifier.size(56.dp).clip(CircleShape).background(EditColors.Button).clickable { state.brush = b }
                .semantics { contentDescription = name; selected = on }.padding(15.dp)) {
                brushIcon(b, if (on) Color(EditRules.COLORS[state.color]) else Color.White)
            }
        }
    }
}

/** «Mosaico»: how big the squares are; the finger paints them over what should not be seen. */
@Composable fun MosaicPanel(state: EditorState) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(stringResource(R.string.edit_mosaic_hint), Modifier.padding(horizontal = 24.dp), color = EditColors.Muted, fontSize = 14.sp)
    SizeSlider(state.size) { state.size = it }
}

/** Five thicknesses on a track that gets wider, with a white ring on the chosen one (tap or slide). */
@Composable fun SizeSlider(index: Int, onChange: (Int) -> Unit) {
    val last = EditRules.SIZES.lastIndex
    fun at(x: Float, width: Int) = ((x / width) * last + 0.5f).toInt().coerceIn(0, last)
    Canvas(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 28.dp)
        .semantics { contentDescription = "${index + 1} / ${last + 1}" }
        .pointerInput(Unit) { detectTapGestures { onChange(at(it.x, size.width)) } }
        .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> onChange(at(change.position.x, size.width)) } }) {
        val y = size.height / 2
        val track = Path().apply {
            moveTo(0f, y - 3.dp.toPx()); lineTo(size.width, y - 12.dp.toPx()); lineTo(size.width, y + 12.dp.toPx()); lineTo(0f, y + 3.dp.toPx()); close()
        }
        drawPath(track, EditColors.Button)
        (0..last).forEach { i -> drawCircle(EditColors.Muted, 3.dp.toPx(), Offset(size.width * i / last, y)) }
        val knob = Offset(size.width * index / last, y)
        drawCircle(Color.Black, 15.dp.toPx(), knob)
        drawCircle(Color.White, 15.dp.toPx(), knob, style = Stroke(4.dp.toPx()))
    }
}

/** White on its own, then the rainbow; the chosen one has a white border. */
@Composable fun ColorRow(index: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Swatch(0, index == 0, Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)), onChange)
        Spacer(Modifier.width(18.dp))
        Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))) {
            (1..EditRules.COLORS.lastIndex).forEach { i -> Swatch(i, index == i, Modifier.weight(1f).height(36.dp), onChange) }
        }
    }
}

@Composable private fun Swatch(i: Int, on: Boolean, modifier: Modifier, onChange: (Int) -> Unit) {
    Box(modifier.background(Color(EditRules.COLORS[i])).then(if (on) Modifier.border(3.dp, Color.White, RoundedCornerShape(6.dp)) else Modifier)
        .clickable { onChange(i) }.semantics { contentDescription = "#%06X".format(EditRules.COLORS[i] and 0xFFFFFFL); selected = on })
}

@Composable private fun brushName(b: Brush) = stringResource(when (b) {
    Brush.ERASER -> R.string.brush_eraser; Brush.PEN -> R.string.brush_pen; Brush.DOTS -> R.string.brush_dots
    Brush.LINE -> R.string.brush_line; Brush.DASHED -> R.string.brush_dashed; Brush.ARROW -> R.string.brush_arrow
    Brush.OVAL -> R.string.brush_oval; Brush.RECT -> R.string.brush_rect
})

/** Each brush drawn as what it makes. */
private fun DrawScope.brushIcon(b: Brush, ink: Color) {
    val s = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
    val (w, h) = size.width to size.height
    when (b) {
        Brush.ERASER -> {
            drawRoundRect(Color.White, Offset(w * .25f, h * .1f), Size(w * .5f, h * .62f), androidx.compose.ui.geometry.CornerRadius(4f), style = s)
            drawLine(Color.White, Offset(w * .25f, h * .45f), Offset(w * .75f, h * .45f), s.width)
            drawLine(Color.White, Offset(0f, h * .95f), Offset(w, h * .95f), s.width)
        }
        Brush.PEN -> drawPath(Path().apply { moveTo(0f, h * .7f); cubicTo(w * .3f, 0f, w * .3f, h, w * .6f, h * .4f); quadraticTo(w * .8f, h * .1f, w, h * .8f) }, ink, style = s)
        Brush.DOTS -> drawPath(Path().apply { moveTo(0f, h * .6f); quadraticTo(w * .5f, 0f, w, h * .7f) }, ink,
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 7.dp.toPx()))))
        Brush.LINE -> drawLine(ink, Offset(w * .1f, h * .9f), Offset(w * .9f, h * .1f), s.width, StrokeCap.Round)
        Brush.DASHED -> drawLine(ink, Offset(w * .1f, h * .9f), Offset(w * .9f, h * .1f), s.width, StrokeCap.Round,
            PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
        Brush.ARROW -> {
            drawLine(ink, Offset(w * .1f, h * .9f), Offset(w * .9f, h * .1f), s.width, StrokeCap.Round)
            drawPath(Path().apply { moveTo(w * .45f, h * .1f); lineTo(w * .9f, h * .1f); lineTo(w * .9f, h * .55f) }, ink, style = s)
        }
        Brush.OVAL -> drawOval(ink, Offset(0f, h * .15f), Size(w, h * .7f), style = s)
        Brush.RECT -> drawRect(ink, Offset(w * .05f, h * .05f), Size(w * .9f, h * .9f), style = s)
    }
}

package app.luma.gallery.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.luma.gallery.edit.Pt
import app.luma.gallery.edit.Render
import app.luma.gallery.edit.Tool
import kotlin.math.hypot

/** The picture, fitted to the space, with what the open section draws on top; fingers draw, move texts or the frame. */
@Composable fun EditCanvas(state: EditorState, picture: Bitmap) = BoxWithConstraints {
    val density = LocalDensity.current
    val scale = minOf(constraints.maxWidth.toFloat() / picture.width, constraints.maxHeight.toFloat() / picture.height)
    val w = (picture.width * scale).toInt().coerceAtLeast(1); val h = (picture.height * scale).toInt().coerceAtLeast(1)
    val image = remember(picture) { picture.asImageBitmap() }
    val matrix = state.previewMatrix
    val tool = state.tool
    Canvas(Modifier.size(with(density) { w.toDp() }, with(density) { h.toDp() }).pointerInput(tool) { gestures(state, tool) }) {
        drawImage(image, IntOffset.Zero, IntSize(image.width, image.height), IntOffset.Zero, IntSize(w, h),
            colorFilter = matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) })
        drawIntoCanvas { c ->
            when (tool) {
                Tool.DRAW -> Render.strokes(c.nativeCanvas, state.strokes, w, h)
                Tool.MOSAIC -> Render.mosaic(c.nativeCanvas, picture, state.strokes, w, h)
                Tool.TEXT -> Render.texts(c.nativeCanvas, state.texts, w, h)
                else -> Unit
            }
        }
        if (tool == Tool.CROP) {
            val f = state.frame
            val r = Rect(f.left * w, f.top * h, f.right * w, f.bottom * h)
            val shade = Color.Black.copy(alpha = .55f)
            drawRect(shade, Offset.Zero, Size(w.toFloat(), r.top))
            drawRect(shade, Offset(0f, r.bottom), Size(w.toFloat(), h - r.bottom))
            drawRect(shade, Offset(0f, r.top), Size(r.left, r.height))
            drawRect(shade, Offset(r.right, r.top), Size(w - r.right, r.height))
            drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
            for (i in 1..2) {
                drawLine(Color.White.copy(alpha = .4f), Offset(r.left + r.width * i / 3, r.top), Offset(r.left + r.width * i / 3, r.bottom))
                drawLine(Color.White.copy(alpha = .4f), Offset(r.left, r.top + r.height * i / 3), Offset(r.right, r.top + r.height * i / 3))
            }
            corners(r).forEach { drawCircle(Color.White, 7.dp.toPx(), it) }
        }
    }
}

private fun corners(r: Rect) = listOf(r.topLeft, r.topRight, r.bottomRight, r.bottomLeft)

/** One finger: down, move, up. In «Recortar», near a corner moves that corner, anywhere else moves the frame. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.gestures(state: EditorState, tool: Tool?) {
    if (tool == null || tool == Tool.STUDIO || tool == Tool.ADJUST || tool == Tool.FILTERS) return
    fun pt(o: Offset) = Pt(o.x / size.width, o.y / size.height)
    awaitEachGesture {
        val down = awaitFirstDown()
        var corner = -1
        if (tool == Tool.CROP) {
            val f = state.frame
            val r = Rect(f.left * size.width, f.top * size.height, f.right * size.width, f.bottom * size.height)
            corner = corners(r).indexOfFirst { hypot(it.x - down.position.x, it.y - down.position.y) < 36.dp.toPx() }
        } else state.down(pt(down.position))
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (tool == Tool.CROP) {
                if (corner >= 0) state.dragCorner(corner, pt(change.position))
                else change.positionChange().let { d -> state.moveFrame(d.x / size.width, d.y / size.height) }
            } else state.drag(pt(change.position))
            change.consume()
        }
        if (tool != Tool.CROP) state.up()
    }
}

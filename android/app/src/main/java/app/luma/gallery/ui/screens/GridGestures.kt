package app.luma.gallery.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

/** Two fingers closing (pinch in) or opening: one step of zoom per gesture. The scroll never sees two-finger moves. */
fun Modifier.pinchSteps(onPinchIn: () -> Unit, onPinchOut: () -> Unit) = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var zoom = 1f
        var done = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                zoom *= event.calculateZoom()
                if (!done && zoom < .75f) { onPinchIn(); done = true }
                if (!done && zoom > 1.35f) { onPinchOut(); done = true }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Deslizar para seleccionar: long-press a photo, then without lifting slide over others; everything between the first
 * and the one under the finger is picked (added to what was already picked). Near the top or bottom edge it scrolls.
 */
fun Modifier.dragToSelect(grid: LazyGridState, scope: CoroutineScope, order: () -> List<String>, picked: () -> Set<String>,
    onChange: (Set<String>) -> Unit) = pointerInput(Unit) {
    var anchor = -1
    var before = emptySet<String>()
    fun keyAt(offset: Offset): String? = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        offset.x.toInt() in item.offset.x until item.offset.x + item.size.width &&
            offset.y.toInt() in item.offset.y until item.offset.y + item.size.height
    }?.key as? String
    fun sweep(to: Offset) {
        val keys = order()
        val index = keyAt(to)?.let(keys::indexOf) ?: return
        if (index < 0 || anchor < 0) return
        onChange(before + keys.subList(minOf(anchor, index), maxOf(anchor, index) + 1))
    }
    detectDragGesturesAfterLongPress(
        onDragStart = { start ->
            val keys = order()
            anchor = keyAt(start)?.let(keys::indexOf) ?: -1
            before = picked()
            if (anchor >= 0) {
                val key = keys[anchor]
                // A long press alone toggles that photo, like before.
                before = if (key in before) before - key else before
                onChange(if (key in picked()) before else before + key)
            }
        },
        onDrag = { change, _ ->
            sweep(change.position)
            val edge = 80.dp.toPx()
            when {
                change.position.y > size.height - edge -> scope.launch { grid.scrollBy(24f) }
                change.position.y < edge -> scope.launch { grid.scrollBy(-24f) }
            }
        },
        onDragEnd = { anchor = -1 }, onDragCancel = { anchor = -1 })
}


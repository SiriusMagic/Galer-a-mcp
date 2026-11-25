package app.luma.gallery.ui.edit

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.luma.gallery.edit.Adjust
import app.luma.gallery.edit.AdjustKind
import app.luma.gallery.edit.Aspect
import app.luma.gallery.edit.Box
import app.luma.gallery.edit.Brush
import app.luma.gallery.edit.ColorMath
import app.luma.gallery.edit.EditRules
import app.luma.gallery.edit.Filter
import app.luma.gallery.edit.Pt
import app.luma.gallery.edit.Render
import app.luma.gallery.edit.Stroke
import app.luma.gallery.edit.TextItem
import app.luma.gallery.edit.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one owner of the editor. Each section works on a draft over the current [picture]; ✓ paints it in (a new picture,
 * kept for «Deshacer»), ✕ throws it away. The original photo is never touched: «Guardar» makes a copy.
 */
class EditorState(private val scope: CoroutineScope, private val work: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default) {
    var picture by mutableStateOf<Bitmap?>(null); private set
    /** Earlier pictures, for «Deshacer» (the last few). */
    var history by mutableStateOf(emptyList<Bitmap>()); private set
    var tool by mutableStateOf<Tool?>(null); private set
    var busy by mutableStateOf(false); private set
    private var mark = 0

    // «Dibujar» and «Mosaico»
    var strokes by mutableStateOf(emptyList<Stroke>()); private set
    var brush by mutableStateOf(Brush.PEN)
    var color by mutableIntStateOf(EditRules.START_COLOR)
    var size by mutableIntStateOf(EditRules.START_SIZE)
    // «Ajustes» and «Filtros»
    var adjust by mutableStateOf(Adjust()); private set
    var adjustKind by mutableStateOf(AdjustKind.BRIGHTNESS)
    var filter by mutableStateOf(Filter.NONE)
    // «Recortar»
    var frame by mutableStateOf(Box.FULL); private set
    var aspect by mutableStateOf(Aspect.FREE); private set
    // «Texto»
    var texts by mutableStateOf(emptyList<TextItem>()); private set
    var moving by mutableStateOf<Int?>(null); private set

    val changed get() = history.isNotEmpty()
    /** The colours of «Ajustes» + «Filtros» while they are being chosen (null = none). */
    val previewMatrix: FloatArray? get() = when (tool) {
        Tool.ADJUST -> ColorMath.adjust(adjust); Tool.FILTERS -> ColorMath.filter(filter); else -> null
    }

    fun start(first: Bitmap) { picture = first; history = emptyList(); tool = null }

    fun open(next: Tool) {
        tool = next; mark = history.size
        strokes = emptyList(); texts = emptyList(); adjust = Adjust(); filter = Filter.NONE; frame = Box.FULL; aspect = Aspect.FREE
        if (next == Tool.MOSAIC) size = EditRules.START_SIZE
        if (next == Tool.DRAW && brush == Brush.ERASER) brush = Brush.PEN
    }

    /** ✕: back to the picture as it was when the section opened. */
    fun cancel() {
        if (history.size > mark) { picture = history[mark]; history = history.take(mark) }
        tool = null
    }

    /** ✓: paints the draft into the picture. */
    fun apply() {
        val base = picture ?: return
        val drawn = strokes; val words = texts; val box = frame; val m = ColorMath.combined(adjust, filter)
        when (tool) {
            Tool.DRAW -> if (drawn.isNotEmpty()) change { Render.on(base) { c, w, h -> Render.strokes(c, drawn, w, h) } }
            Tool.MOSAIC -> if (drawn.isNotEmpty()) change { Render.on(base) { c, w, h -> Render.mosaic(c, base, drawn, w, h) } }
            Tool.TEXT -> if (words.isNotEmpty()) change { Render.on(base) { c, w, h -> Render.texts(c, words, w, h) } }
            Tool.ADJUST, Tool.FILTERS -> if (!adjust.untouched || filter != Filter.NONE) change { Render.colors(base, m) }
            Tool.CROP -> if (box != Box.FULL) change { Render.crop(base, box, 0, false) }
            Tool.STUDIO, null -> Unit
        }
        tool = null
    }

    fun undo() { history.lastOrNull()?.let { picture = it; history = history.dropLast(1); mark = mark.coerceAtMost(history.size) } }

    // Estudio IA and the buttons of «Recortar» change the picture at once (✕ still takes them back).
    fun enhance() = change { Render.enhance(it) }
    fun sharpen() = change { Render.sharpen(it) }
    fun turn() = change { Render.crop(it, Box.FULL, 1, false) }.also { frame = Box.FULL; aspect = Aspect.FREE }
    fun mirror() = change { Render.crop(it, Box.FULL, 0, true) }

    fun chooseAspect(next: Aspect) { aspect = next; picture?.let { frame = EditRules.frame(next, it.width, it.height) } }
    fun dragCorner(corner: Int, p: Pt) { if (aspect == Aspect.FREE) frame = EditRules.dragCorner(frame, corner, p) }
    fun moveFrame(dx: Float, dy: Float) { frame = EditRules.moveFrame(frame, dx, dy) }

    fun setAdjust(value: Float) { adjust = ColorMath.set(adjust, adjustKind, value) }

    /** A finger went down at [p] on the picture. */
    fun down(p: Pt) {
        when (tool) {
            Tool.DRAW -> strokes = strokes + Stroke(brush, EditRules.COLORS[color], EditRules.SIZES[size], listOf(p))
            Tool.MOSAIC -> strokes = strokes + Stroke(Brush.PEN, 0, EditRules.SIZES[size], listOf(p))
            Tool.TEXT -> moving = texts.indices.minByOrNull { dist(texts[it].at, p) }?.takeIf { dist(texts[it].at, p) < 0.12f }
            else -> Unit
        }
    }

    fun drag(p: Pt) {
        val last = strokes.lastOrNull()
        when {
            tool == Tool.TEXT -> moving?.let { i -> texts = texts.toMutableList().also { it[i] = it[i].copy(at = p) } }
            last == null || (tool != Tool.DRAW && tool != Tool.MOSAIC) -> Unit
            last.brush == Brush.ERASER -> {
                val path = EditRules.addPoint(last.points, p)
                strokes = EditRules.erase(strokes.dropLast(1), path, last.size * 2) + last.copy(points = path)
            }
            else -> strokes = strokes.dropLast(1) + last.copy(points = if (last.brush.shape) EditRules.dragShape(last.points, p) else EditRules.addPoint(last.points, p))
        }
    }

    fun up() { moving = null; strokes = strokes.filterNot { it.brush == Brush.ERASER } }

    /** «Deshacer» inside a section: the last mark or text. */
    fun undoDraft() { if (tool == Tool.TEXT) texts = texts.dropLast(1) else strokes = strokes.dropLast(1) }

    fun addText(text: String) {
        val clean = EditRules.cleanText(text) ?: return
        texts = texts + TextItem(clean, EditRules.COLORS[color], TEXT_SIZES[size], Pt(0.5f, 0.5f))
    }

    private fun change(make: (Bitmap) -> Bitmap) {
        val base = picture ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                val next = withContext(work) { make(base) }
                history = (history + base).takeLast(KEEP); mark = mark.coerceAtMost(history.size)
                picture = next
            } finally { busy = false }
        }
    }

    private fun dist(a: Pt, b: Pt) = kotlin.math.hypot(a.x - b.x, a.y - b.y)

    companion object {
        const val KEEP = 8
        val TEXT_SIZES = listOf(0.04f, 0.06f, 0.08f, 0.11f, 0.15f)
    }
}

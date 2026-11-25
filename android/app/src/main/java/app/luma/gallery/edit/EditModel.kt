package app.luma.gallery.edit

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The photo editor's model and rules: pure Kotlin, no Android, with tests. Points are normalized to the picture being
 * edited (0..1 on each side), so they do not depend on the screen or on the picture's size.
 */

/** The editor's sections, in the order of the bottom bar. */
enum class Tool { STUDIO, CROP, ADJUST, FILTERS, DRAW, MOSAIC, TEXT }

/** «Dibujar»: the eraser, free hand, dotted free hand, line, dashed line, arrow, oval and rectangle. */
enum class Brush(val shape: Boolean) {
    ERASER(false), PEN(false), DOTS(false), LINE(true), DASHED(true), ARROW(true), OVAL(true), RECT(true)
}

data class Pt(val x: Float, val y: Float)

/** One mark on the picture. [size] is a fraction of the picture's shorter side; [color] is 0xAARRGGBB. */
data class Stroke(val brush: Brush, val color: Long, val size: Float, val points: List<Pt>)

/** A text on the picture; [at] is its centre and [size] a fraction of the picture's shorter side. */
data class TextItem(val text: String, val color: Long, val size: Float, val at: Pt)

/** A part of the picture (normalized). */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    companion object { val FULL = Box(0f, 0f, 1f, 1f) }
}

/** «Recortar»: the frame's shape. [ratio] is width / height (null = free). */
enum class Aspect(val ratio: Float?) { FREE(null), SQUARE(1f), R4_3(4f / 3f), R3_4(3f / 4f), R16_9(16f / 9f), R9_16(9f / 16f) }

object EditRules {
    /** The five thicknesses of the slider (a fraction of the shorter side); the middle one is chosen at first. */
    val SIZES = listOf(0.004f, 0.008f, 0.014f, 0.022f, 0.034f)
    const val START_SIZE = 2

    /** The colours: white, then the rainbow of the screenshot (red first, chosen at first). */
    val COLORS = listOf(0xFFFFFFFF, 0xFFE8483F, 0xFFEB7262, 0xFFEE8E42, 0xFFF2CC57, 0xFFD7B45B, 0xFFDDF58C, 0xFFA8F0D2,
        0xFF86BDF7, 0xFF3D7DF2, 0xFF7B55E8, 0xFFE7B5F2, 0xFFD45AE6)
    const val START_COLOR = 1

    /** The two ends of an arrow's head at [to], coming from [from]; [length] in the same units as the points. */
    fun arrowHead(from: Pt, to: Pt, length: Float): Pair<Pt, Pt> {
        val angle = atan2(to.y - from.y, to.x - from.x)
        val spread = Math.toRadians(28.0)
        fun end(a: Double) = Pt((to.x - length * cos(a)).toFloat(), (to.y - length * sin(a)).toFloat())
        return end(angle - spread) to end(angle + spread)
    }

    /** The box between two corners, whatever direction the finger went. */
    fun boxOf(a: Pt, b: Pt) = Box(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))

    /** A free-hand point is kept only if it moved far enough from the last one (fewer points, same line). */
    fun addPoint(points: List<Pt>, p: Pt, minStep: Float = 0.002f): List<Pt> {
        val last = points.lastOrNull() ?: return listOf(p.clamped())
        return if (hypot(p.x - last.x, p.y - last.y) < minStep) points else points + p.clamped()
    }

    /** A shape keeps only its first point and where the finger is now. */
    fun dragShape(points: List<Pt>, p: Pt): List<Pt> = listOf(points.firstOrNull() ?: p.clamped(), p.clamped())

    /** The eraser removes every mark it touches; [radius] is the eraser's half size. */
    fun erase(strokes: List<Stroke>, path: List<Pt>, radius: Float): List<Stroke> =
        strokes.filterNot { s -> s.brush != Brush.ERASER && touches(s, path, radius + s.size / 2) }

    private fun touches(s: Stroke, path: List<Pt>, r: Float): Boolean {
        val marks = if (s.brush.shape && s.points.size >= 2) outline(s) else s.points
        val pieces = if (marks.size == 1) listOf(marks[0] to marks[0]) else marks.zipWithNext()
        return path.any { e -> pieces.any { (a, b) -> distance(e, a, b) <= r } }
    }

    /** How far [p] is from the segment [a]–[b]. */
    fun distance(p: Pt, a: Pt, b: Pt): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val len = dx * dx + dy * dy
        val t = if (len == 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    /** A shape's edge as a line through its corners (for the eraser): the segment, or the four sides of its box. */
    private fun outline(s: Stroke): List<Pt> {
        val (a, b) = s.points.first() to s.points.last()
        val box = boxOf(a, b)
        return if (s.brush == Brush.LINE || s.brush == Brush.DASHED || s.brush == Brush.ARROW) listOf(a, b)
            else listOf(Pt(box.left, box.top), Pt(box.right, box.top), Pt(box.right, box.bottom), Pt(box.left, box.bottom), Pt(box.left, box.top))
    }

    /** The biggest centred frame of [aspect] inside a picture of [w]×[h] pixels (normalized). */
    fun frame(aspect: Aspect, w: Int, h: Int): Box {
        val ratio = aspect.ratio ?: return Box.FULL
        val picture = w.toFloat() / h
        return if (ratio > picture) { val hh = picture / ratio; Box(0f, (1 - hh) / 2, 1f, (1 + hh) / 2) }
            else { val ww = ratio / picture; Box((1 - ww) / 2, 0f, (1 + ww) / 2, 1f) }
    }

    /** Moves one corner of a free frame to [p], keeping it inside the picture and at least [min] wide and tall. */
    fun dragCorner(box: Box, corner: Int, p: Pt, min: Float = 0.08f): Box {
        val x = p.x.coerceIn(0f, 1f); val y = p.y.coerceIn(0f, 1f)
        return when (corner) {
            0 -> box.copy(left = x.coerceAtMost(box.right - min), top = y.coerceAtMost(box.bottom - min))
            1 -> box.copy(right = x.coerceAtLeast(box.left + min), top = y.coerceAtMost(box.bottom - min))
            2 -> box.copy(right = x.coerceAtLeast(box.left + min), bottom = y.coerceAtLeast(box.top + min))
            else -> box.copy(left = x.coerceAtMost(box.right - min), bottom = y.coerceAtLeast(box.top + min))
        }
    }

    /** Moves the whole frame by ([dx], [dy]) without leaving the picture. */
    fun moveFrame(box: Box, dx: Float, dy: Float): Box {
        val x = dx.coerceIn(-box.left, 1 - box.right); val y = dy.coerceIn(-box.top, 1 - box.bottom)
        return Box(box.left + x, box.top + y, box.right + x, box.bottom + y)
    }

    /** Pixel size of the picture after a quarter turn. */
    fun turned(w: Int, h: Int, quarterTurns: Int) = if (Math.floorMod(quarterTurns, 2) == 1) h to w else w to h

    /** The side of a mosaic square, in pixels, for a picture whose shorter side is [shortSide]. */
    fun mosaicCell(shortSide: Int, size: Float): Int = (shortSide * size * 1.6f).toInt().coerceAtLeast(6)

    /** A text that is only spaces is not added; long ones are cut. */
    fun cleanText(text: String): String? = text.trim().take(120).ifEmpty { null }

    private fun Pt.clamped() = Pt(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

package app.luma.gallery.edit

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Everything the editor draws, with Android's Canvas. The same functions paint the preview on screen and the saved
 * picture, so what is seen is what is saved. Each section works on the current picture and returns a new one.
 */
object Render {
    /** The biggest side the editor works with (enough for the phone's screen and for sharing; keeps memory low). */
    const val MAX_SIDE = 2560

    /** Reads [uri] turned the right way up (EXIF), at most [maxSide] pixels on its longer side, ready to draw on. */
    fun load(resolver: ContentResolver, uri: Uri, maxSide: Int = MAX_SIDE): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val (w, h) = info.size.width to info.size.height
            val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h))
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }

    /** «Recortar»: the [box] part, turned [quarterTurns] times clockwise and mirrored if [flip]. */
    fun crop(src: Bitmap, box: Box, quarterTurns: Int, flip: Boolean): Bitmap {
        val x = (box.left * src.width).toInt().coerceIn(0, src.width - 1)
        val y = (box.top * src.height).toInt().coerceIn(0, src.height - 1)
        val w = (box.width * src.width).toInt().coerceIn(1, src.width - x)
        val h = (box.height * src.height).toInt().coerceIn(1, src.height - y)
        val m = Matrix().apply { if (flip) postScale(-1f, 1f); postRotate(90f * Math.floorMod(quarterTurns, 4)) }
        return Bitmap.createBitmap(src, x, y, w, h, m, true)
    }

    /** «Ajustes», «Filtros» and «Mejorar»: the colours through [matrix]. */
    fun colors(src: Bitmap, matrix: FloatArray): Bitmap {
        val out = createBitmap(src.width, src.height)
        Canvas(out).drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) })
        return out
    }

    /** «Mejorar»: the light stretched by the picture's own histogram, all on the phone. */
    fun enhance(src: Bitmap): Bitmap = colors(src, ColorMath.autoEnhance(Pixels.histogram(pixels(src), step = 7)))

    /** «Nitidez». */
    fun sharpen(src: Bitmap, amount: Float = 0.6f): Bitmap {
        val out = createBitmap(src.width, src.height)
        out.setPixels(Pixels.sharpen(pixels(src), src.width, src.height, amount), 0, src.width, 0, 0, src.width, src.height)
        return out
    }

    /** A copy of [src] with [paint] applied by [draw] (marks, mosaic or text). */
    fun on(src: Bitmap, draw: (Canvas, Int, Int) -> Unit): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        draw(Canvas(out), out.width, out.height)
        return out
    }

    /** «Dibujar»: the marks, on a canvas of [w]×[h] (the picture, or its place on screen). */
    fun strokes(canvas: Canvas, strokes: List<Stroke>, w: Int, h: Int) {
        val short = minOf(w, h).toFloat()
        strokes.filter { it.brush != Brush.ERASER && it.points.isNotEmpty() }.forEach { s ->
            val width = (s.size * short).coerceAtLeast(1f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = s.color.toInt(); style = Paint.Style.STROKE; strokeWidth = width
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                pathEffect = when (s.brush) {
                    Brush.DOTS -> DashPathEffect(floatArrayOf(0.1f, width * 2.2f), 0f)
                    Brush.DASHED -> DashPathEffect(floatArrayOf(width * 3f, width * 2.2f), 0f)
                    else -> null
                }
            }
            val pts = s.points.map { it.x * w to it.y * h }
            val (ax, ay) = pts.first(); val (bx, by) = pts.last()
            when (s.brush) {
                Brush.PEN, Brush.DOTS -> canvas.drawPath(path(pts), paint)
                Brush.LINE, Brush.DASHED -> canvas.drawLine(ax, ay, bx, by, paint)
                Brush.ARROW -> {
                    canvas.drawLine(ax, ay, bx, by, paint)
                    val (p, q) = EditRules.arrowHead(Pt(ax, ay), Pt(bx, by), width * 3.5f + short * 0.02f)
                    canvas.drawPath(Path().apply { moveTo(p.x, p.y); lineTo(bx, by); lineTo(q.x, q.y) }, paint)
                }
                Brush.OVAL -> canvas.drawOval(RectF(minOf(ax, bx), minOf(ay, by), maxOf(ax, bx), maxOf(ay, by)), paint)
                Brush.RECT -> canvas.drawRect(RectF(minOf(ax, bx), minOf(ay, by), maxOf(ax, bx), maxOf(ay, by)), paint)
                Brush.ERASER -> Unit
            }
        }
    }

    /** «Mosaico»: big squares of [picture] wherever the finger went. [w]×[h] is where [picture] is drawn. */
    fun mosaic(canvas: Canvas, picture: Bitmap, strokes: List<Stroke>, w: Int, h: Int) {
        if (strokes.isEmpty()) return
        val short = minOf(picture.width, picture.height)
        strokes.forEach { s ->
            val blocky = blocky(picture, EditRules.mosaicCell(short, s.size), w, h)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(blocky, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                style = Paint.Style.STROKE; strokeWidth = s.size * minOf(w, h) * 3f
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path(s.points.map { it.x * w to it.y * h }), paint)
        }
    }

    /** «Texto»: each text centred on its point, with a soft shadow so it reads on any photo. */
    fun texts(canvas: Canvas, items: List<TextItem>, w: Int, h: Int) {
        val short = minOf(w, h).toFloat()
        items.forEach { t ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = t.color.toInt(); textSize = t.size * short; textAlign = Paint.Align.CENTER
                isFakeBoldText = true; setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0x99000000.toInt())
            }
            canvas.drawText(t.text, t.at.x * w, t.at.y * h - (paint.ascent() + paint.descent()) / 2, paint)
        }
    }

    /** The picture in squares of [cell] pixels, at [w]×[h]; the last one is kept, so the preview does not redo it each frame. */
    private fun blocky(picture: Bitmap, cell: Int, w: Int, h: Int): Bitmap {
        val key = "${System.identityHashCode(picture)}:${picture.generationId}:$cell:$w:$h"
        lastBlocky?.let { (k, b) -> if (k == key) return b }
        val small = picture.scale((picture.width / cell).coerceAtLeast(1), (picture.height / cell).coerceAtLeast(1), filter = true)
        return small.scale(w, h, filter = false).also { lastBlocky = key to it }
    }
    private var lastBlocky: Pair<String, Bitmap>? = null

    private fun path(pts: List<Pair<Float, Float>>) = Path().apply {
        pts.firstOrNull()?.let { (x, y) -> moveTo(x, y); if (pts.size == 1) lineTo(x + 0.1f, y) }
        pts.drop(1).forEach { (x, y) -> lineTo(x, y) }
    }

    private fun pixels(src: Bitmap) = IntArray(src.width * src.height).also { src.getPixels(it, 0, src.width, 0, 0, src.width, src.height) }
}

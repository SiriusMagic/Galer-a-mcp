package app.luma.gallery

import app.luma.gallery.data.Library
import app.luma.gallery.edit.Adjust
import app.luma.gallery.edit.AdjustKind
import app.luma.gallery.edit.Aspect
import app.luma.gallery.edit.Box
import app.luma.gallery.edit.Brush
import app.luma.gallery.edit.ColorMath
import app.luma.gallery.edit.EditRules
import app.luma.gallery.edit.Filter
import app.luma.gallery.edit.Pixels
import app.luma.gallery.edit.Pt
import app.luma.gallery.edit.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The editor's pure rules: shapes, eraser, frame, colours, enhance and sharpen. */
class EditTest {
    private val white = 0xFFFFFFFF.toInt()
    private val grey = 0xFF808080.toInt()
    private fun r(c: Int) = c shr 16 and 255
    private fun g(c: Int) = c shr 8 and 255
    private fun b(c: Int) = c and 255

    @Test fun paletteAndSizesLikeTheScreenshot() {
        assertEquals(0xFFFFFFFF, EditRules.COLORS.first())
        assertEquals(13, EditRules.COLORS.size)
        assertEquals(5, EditRules.SIZES.size)
        assertTrue(EditRules.SIZES.zipWithNext().all { (a, b) -> a < b })
        assertEquals(listOf(Brush.ERASER, Brush.PEN, Brush.DOTS, Brush.LINE, Brush.DASHED, Brush.ARROW, Brush.OVAL, Brush.RECT), Brush.entries)
    }

    @Test fun freeHandKeepsOnlyRealMovesAndShapesKeepTwoPoints() {
        var pts = EditRules.addPoint(emptyList(), Pt(0.5f, 0.5f))
        pts = EditRules.addPoint(pts, Pt(0.5001f, 0.5f))                 // too close: dropped
        pts = EditRules.addPoint(pts, Pt(0.6f, 1.4f))                    // kept, inside the picture
        assertEquals(listOf(Pt(0.5f, 0.5f), Pt(0.6f, 1f)), pts)
        val shape = EditRules.dragShape(EditRules.dragShape(listOf(Pt(0.1f, 0.1f)), Pt(0.3f, 0.3f)), Pt(0.9f, 0.8f))
        assertEquals(listOf(Pt(0.1f, 0.1f), Pt(0.9f, 0.8f)), shape)
        assertEquals(Box(0.2f, 0.1f, 0.6f, 0.7f), EditRules.boxOf(Pt(0.6f, 0.1f), Pt(0.2f, 0.7f)))
        assertEquals(0.1f, EditRules.distance(Pt(0.5f, 0.1f), Pt(0f, 0f), Pt(1f, 0f)), 1e-5f)
        assertEquals(0.5f, EditRules.distance(Pt(1.5f, 0f), Pt(0f, 0f), Pt(1f, 0f)), 1e-5f)
    }

    @Test fun arrowHeadPointsBack() {
        val (p, q) = EditRules.arrowHead(Pt(0f, 0f), Pt(10f, 0f), 2f)
        assertTrue(p.x < 10f && q.x < 10f)
        assertEquals(-p.y, q.y, 1e-4f)                                     // symmetric
    }

    @Test fun eraserRemovesOnlyWhatItTouches() {
        val line = Stroke(Brush.PEN, 0, 0.01f, listOf(Pt(0.1f, 0.1f), Pt(0.2f, 0.1f)))
        val box = Stroke(Brush.RECT, 0, 0.01f, listOf(Pt(0.5f, 0.5f), Pt(0.9f, 0.9f)))
        assertEquals(listOf(box), EditRules.erase(listOf(line, box), listOf(Pt(0.15f, 0.1f)), 0.01f))
        assertEquals(listOf(line), EditRules.erase(listOf(line, box), listOf(Pt(0.9f, 0.7f)), 0.01f))   // its right side
        assertEquals(listOf(line, box), EditRules.erase(listOf(line, box), listOf(Pt(0.7f, 0.7f)), 0.01f)) // inside: untouched
    }

    @Test fun cropFrames() {
        assertEquals(Box.FULL, EditRules.frame(Aspect.FREE, 400, 300))
        val square = EditRules.frame(Aspect.SQUARE, 400, 200)
        assertEquals(0.5f, square.width, 1e-4f); assertEquals(1f, square.height, 1e-4f); assertEquals(0.25f, square.left, 1e-4f)
        val wide = EditRules.frame(Aspect.R16_9, 100, 100)
        assertEquals(1f, wide.width, 1e-4f); assertEquals(9f / 16f, wide.height, 1e-4f)
        val dragged = EditRules.dragCorner(Box.FULL, 0, Pt(0.95f, -1f))
        assertEquals(0.92f, dragged.left, 1e-4f); assertEquals(0f, dragged.top, 1e-4f)      // never smaller than the minimum
        assertEquals(Box(0.5f, 0.5f, 1f, 1f), EditRules.moveFrame(Box(0.25f, 0.25f, 0.75f, 0.75f), 0.9f, 0.9f))
        assertEquals(300 to 400, EditRules.turned(400, 300, 1)); assertEquals(400 to 300, EditRules.turned(400, 300, 2))
    }

    @Test fun colourMatrices() {
        assertEquals(grey, ColorMath.apply(ColorMath.IDENTITY, grey))
        assertTrue(Adjust().untouched)
        assertEquals(grey, ColorMath.apply(ColorMath.adjust(Adjust()), grey))
        val brighter = ColorMath.apply(ColorMath.adjust(ColorMath.set(Adjust(), AdjustKind.BRIGHTNESS, 1f)), grey)
        assertTrue(r(brighter) > 128)
        assertEquals(1f, ColorMath.get(ColorMath.set(Adjust(), AdjustKind.WARMTH, 3f), AdjustKind.WARMTH))   // capped
        val warm = ColorMath.apply(ColorMath.filter(Filter.WARM), grey)
        assertTrue(r(warm) > b(warm))
        val mono = ColorMath.apply(ColorMath.filter(Filter.MONO), 0xFFFF0000.toInt())
        assertEquals(r(mono), g(mono)); assertEquals(g(mono), b(mono))
        val sepia = ColorMath.apply(ColorMath.filter(Filter.SEPIA), grey)
        assertTrue(r(sepia) > g(sepia) && g(sepia) > b(sepia))
        Filter.entries.forEach { assertEquals(0xFF, ColorMath.apply(ColorMath.filter(it), grey) ushr 24) }   // never transparent
    }

    @Test fun concatIsFirstThenSecond() {
        val m = ColorMath.concat(ColorMath.filter(Filter.MONO), ColorMath.filter(Filter.WARM))
        val c = ColorMath.apply(m, 0xFF2040C0.toInt())
        val twoSteps = ColorMath.apply(ColorMath.filter(Filter.WARM), ColorMath.apply(ColorMath.filter(Filter.MONO), 0xFF2040C0.toInt()))
        assertTrue(kotlin.math.abs(r(c) - r(twoSteps)) <= 1 && kotlin.math.abs(b(c) - b(twoSteps)) <= 1)
    }

    @Test fun enhanceStretchesADullPhoto() {
        val dull = IntArray(256).also { h -> for (i in 60..200) h[i] = 10 }
        val m = ColorMath.autoEnhance(dull)
        assertTrue(r(ColorMath.apply(m, 0xFF3C3C3C.toInt())) < 10)          // its darkest becomes nearly black
        assertTrue(r(ColorMath.apply(m, 0xFFC8C8C8.toInt())) > 240)         // its brightest nearly white
        val flat = IntArray(256).also { h -> for (i in 120..130) h[i] = 10 }
        val gentle = ColorMath.apply(ColorMath.autoEnhance(flat), 0xFF828282.toInt())
        assertTrue(r(gentle) < 255)                                         // a very flat photo is stretched at most ×2.2
        assertTrue(ColorMath.autoEnhance(IntArray(256)).contentEquals(ColorMath.IDENTITY))   // empty: untouched
    }

    @Test fun histogramAndSharpen() {
        assertEquals(255, Pixels.luma(white)); assertEquals(0, Pixels.luma(0xFF000000.toInt()))
        val h = Pixels.histogram(intArrayOf(white, white, 0xFF000000.toInt()))
        assertEquals(2, h[255]); assertEquals(1, h[0])
        val flat = IntArray(9) { grey }
        assertTrue(Pixels.sharpen(flat, 3, 3, 1f).contentEquals(flat))      // nothing to sharpen
        val dot = IntArray(9) { grey }.also { it[4] = 0xFFA0A0A0.toInt() }
        assertTrue(r(Pixels.sharpen(dot, 3, 3, 1f)[4]) > 0xA0)              // the bright dot gets brighter
    }

    @Test fun textsAndNames() {
        assertNull(EditRules.cleanText("   "))
        assertEquals("Hola", EditRules.cleanText("  Hola "))
        assertEquals(120, EditRules.cleanText("a".repeat(500))?.length)
        assertEquals("IMG_001 editada.jpg", Library.editedName("IMG_001.heic"))
        assertEquals("Foto editada.jpg", Library.editedName(".jpg"))
        assertTrue(EditRules.mosaicCell(1000, 0.004f) >= 6)
    }
}

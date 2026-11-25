package app.luma.gallery

import android.graphics.Bitmap
import app.luma.gallery.edit.Aspect
import app.luma.gallery.edit.Brush
import app.luma.gallery.edit.Filter
import app.luma.gallery.edit.Pt
import app.luma.gallery.edit.Tool
import app.luma.gallery.ui.edit.EditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** The editor: ✓ paints in, ✕ throws away, «Deshacer» goes back; the original bitmap is never changed. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorStateTest {
    private fun editor() = EditorState(CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)
    private fun photo() = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF336699.toInt()) }

    @Test fun drawApplyAndUndo() {
        val e = editor(); val original = photo(); e.start(original)
        e.open(Tool.DRAW)
        e.down(Pt(0.1f, 0.5f)); e.drag(Pt(0.9f, 0.5f)); e.up()
        assertEquals(1, e.strokes.size)
        e.apply()
        assertNull(e.tool); assertTrue(e.changed); assertNotSame(original, e.picture)
        assertEquals(0xFF336699.toInt(), original.getPixel(100, 50))      // the original is untouched
        e.undo()
        assertSame(original, e.picture); assertFalse(e.changed)
    }

    @Test fun eraserTakesMarksAwayAndLeavesNoMarkOfItsOwn() {
        val e = editor(); e.start(photo()); e.open(Tool.DRAW)
        e.down(Pt(0.1f, 0.5f)); e.drag(Pt(0.5f, 0.5f)); e.up()
        e.brush = Brush.ERASER
        e.down(Pt(0.3f, 0.5f)); e.drag(Pt(0.31f, 0.5f)); e.up()
        assertTrue(e.strokes.isEmpty())
    }

    @Test fun cancelThrowsAwayEvenWhatWasDoneInside() {
        val e = editor(); val original = photo(); e.start(original)
        e.open(Tool.CROP); e.turn()
        assertEquals(100, e.picture!!.width)                                   // turned
        e.cancel()
        assertSame(original, e.picture); assertFalse(e.changed)
    }

    @Test fun cropFilterAndStudio() {
        val e = editor(); e.start(photo())
        e.open(Tool.CROP); e.chooseAspect(Aspect.SQUARE); e.apply()
        assertEquals(e.picture!!.width, e.picture!!.height)
        e.open(Tool.FILTERS); e.filter = Filter.MONO
        assertTrue(e.previewMatrix != null)
        e.apply()
        val c = e.picture!!.getPixel(10, 10)
        assertEquals(c shr 16 and 255, c and 255)                               // grey now
        e.open(Tool.STUDIO); e.enhance(); e.sharpen(); e.apply()
        assertEquals(4, e.history.size)
    }

    @Test fun textsAreAddedAndMoved() {
        val e = editor(); e.start(photo()); e.open(Tool.TEXT)
        e.addText("   "); assertTrue(e.texts.isEmpty())
        e.addText("Hola")
        e.down(Pt(0.52f, 0.5f)); e.drag(Pt(0.2f, 0.8f)); e.up()
        assertEquals(Pt(0.2f, 0.8f), e.texts.single().at)
        e.undoDraft(); assertTrue(e.texts.isEmpty())
    }
}

package app.luma.gallery

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import app.luma.gallery.data.Media
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.LumaApp
import app.luma.gallery.ui.GalleryActions
import app.luma.gallery.ui.IndexStatus
import app.luma.gallery.ui.Page
import app.luma.gallery.ui.theme.LumaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screens of the concept with fake items (no real photos in tests): Fotos, Álbumes and the permission screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private val none = GalleryActions(trash = {})
    private val items = (1L..9L).map { Media(it, Uri.parse("content://media/$it"), "foto$it.jpg",
        listOf("Camera", "Viajes", "Familia")[(it % 3).toInt()], it, isVideo = it == 5L, durationMs = 69_000,
        takenAt = 1_763_000_000_000 - (it / 4) * 86_400_000) }

    @Test fun photosAndAlbums() {
        val state = GalleryState(setOf(items[0].key)).apply { show(items) }
        compose.setContent { LumaTheme(dark = false) { LumaApp(state, true, {}, none) } }
        compose.onNodeWithText("01:09").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-photos.png")
        compose.onAllNodesWithText("Álbumes").onFirst().performClick()
        compose.onNodeWithText("Favoritas").assertExists()
        compose.onNodeWithText("Más álbumes").assertExists()
        compose.onNodeWithText("Bóveda").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-albums.png")
        compose.runOnIdle { state.go(Page.MEMORIES) }
        compose.onAllNodesWithText("Memorias").onFirst().assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-memories.png")
        compose.runOnIdle { state.go(Page.SEARCH) }
        compose.onNodeWithText("Búsqueda inteligente").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-search.png")
        compose.runOnIdle { state.go(Page.PHOTOS); state.viewing = items[0] }
        compose.onNodeWithText("Detalles").assertDoesNotExist()
        compose.onNodeWithContentDescription("Más").performClick()
        compose.onNodeWithText("Detalles").performClick()
        compose.onNodeWithText("Aún sin analizar").assertExists()
        compose.onNode(androidx.compose.ui.test.isDialog()).captureRoboImage("build/screenshots/luma-details.png")
        compose.runOnIdle { state.viewing = null; state.select(items[0]); state.select(items[1]) }
        compose.onNodeWithContentDescription("2 seleccionadas").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-select.png")
    }

    @Test fun darkThemeHasLightIconsAndPinchedMonths() {
        val state = GalleryState().apply { show(items); zoom = app.luma.gallery.data.Zoom.MONTH; setSelection(setOf(items[0].key)) }
        compose.setContent { LumaTheme(dark = true, accent = app.luma.gallery.ui.theme.Accent.OCEANO) { LumaApp(state, true, {}, none) } }
        compose.onNodeWithContentDescription("1 seleccionada").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-dark-months.png")
    }

    /** Ajustes › Icono de la app: the five paintings, «Acuarela» chosen (the default). */
    @Test fun appIcons() {
        compose.setContent { LumaTheme(dark = true) {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(16.dp)) {
                val c = androidx.compose.material3.MaterialTheme.colorScheme
                IconCard(c.surfaceVariant, c.onSurface, c.onSurfaceVariant, c.primary)
            }
        } }
        compose.onNodeWithText("Acuarela").assertExists()
        compose.onNodeWithText("Óleo").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-icons.png")
    }

    /** «Editar»: the sections, then «Dibujar» with a few marks, «Recortar» and «Filtros» on a made-up photo. */
    @Test fun editor() {
        val photo = android.graphics.Bitmap.createBitmap(1200, 900, android.graphics.Bitmap.Config.ARGB_8888).also { b ->
            val c = android.graphics.Canvas(b)
            c.drawColor(0xFF2A4F7A.toInt())
            c.drawCircle(400f, 330f, 150f, android.graphics.Paint().apply { color = 0xFFF2B33D.toInt() })
            c.drawRect(0f, 620f, 1200f, 900f, android.graphics.Paint().apply { color = 0xFF2E7D4F.toInt() })
        }
        val e = app.luma.gallery.ui.edit.EditorState(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), kotlinx.coroutines.Dispatchers.Unconfined)
        e.start(photo)
        compose.setContent { LumaTheme(dark = true) { app.luma.gallery.ui.edit.EditorScreen(e, {}) { true } } }
        compose.onNodeWithText("Estudio IA").assertExists()
        compose.onNodeWithText("Mosaico").assertExists()
        compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/luma-edit.png")
        compose.onNodeWithText("Dibujar").performClick()
        e.down(app.luma.gallery.edit.Pt(0.2f, 0.75f)); e.drag(app.luma.gallery.edit.Pt(0.5f, 0.55f)); e.drag(app.luma.gallery.edit.Pt(0.8f, 0.8f)); e.up()
        e.brush = app.luma.gallery.edit.Brush.ARROW
        e.down(app.luma.gallery.edit.Pt(0.75f, 0.2f)); e.drag(app.luma.gallery.edit.Pt(0.45f, 0.35f)); e.up()
        e.brush = app.luma.gallery.edit.Brush.RECT
        e.down(app.luma.gallery.edit.Pt(0.15f, 0.15f)); e.drag(app.luma.gallery.edit.Pt(0.5f, 0.6f)); e.up()
        compose.onNodeWithContentDescription("Rectángulo").assertExists()
        compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/luma-edit-draw.png")
        compose.onNodeWithContentDescription("Cancelar").performClick()
        compose.onNodeWithText("Recortar").performClick()
        compose.onNodeWithText("1:1").performClick()
        assertEquals(app.luma.gallery.edit.Aspect.SQUARE, e.aspect)
        compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/luma-edit-crop.png")
        compose.onNodeWithContentDescription("Cancelar").performClick()
        compose.onNodeWithText("Filtros").performClick()
        compose.onNodeWithText("Sepia").performClick()
        assertEquals(app.luma.gallery.edit.Filter.SEPIA, e.filter)
        compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/luma-edit-filters.png")
    }

    @Test fun askForAccessFirst() {
        compose.setContent { LumaTheme(dark = true) { LumaApp(GalleryState(), false, {}, none) } }
        compose.onNodeWithText("Dar acceso a mis fotos").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-permission.png")
    }
}

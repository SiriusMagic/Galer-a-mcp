package app.luma.gallery

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import app.luma.gallery.data.Media
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.LumaApp
import app.luma.gallery.ui.screens.ViewerActions
import app.luma.gallery.ui.theme.LumaTheme
import com.github.takahirom.roborazzi.captureRoboImage
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
    private val none = ViewerActions({}, {}, {}, null, {})
    private val items = (1L..9L).map { Media(it, Uri.parse("content://media/$it"), "foto$it.jpg",
        listOf("Camera", "Viajes", "Familia")[(it % 3).toInt()], it, isVideo = it == 5L) }

    @Test fun photosAndAlbums() {
        val state = GalleryState(setOf(items[0].key)).apply { show(items) }
        compose.setContent { LumaTheme(dark = false) { LumaApp(state, true, {}, none) } }
        compose.onNodeWithText("9 elementos").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-photos.png")
        compose.onNodeWithText("Álbumes").performClick()
        compose.onNodeWithText("Favoritas").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-albums.png")
    }

    @Test fun askForAccessFirst() {
        compose.setContent { LumaTheme(dark = true) { LumaApp(GalleryState(), false, {}, none) } }
        compose.onNodeWithText("Dar acceso a mis fotos").assertExists()
        onView(isRoot()).captureRoboImage("build/screenshots/luma-permission.png")
    }
}

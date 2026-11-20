package app.luma.gallery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import app.luma.gallery.ui.screens.AlbumPicker
import app.luma.gallery.ui.theme.LumaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * «Mover o copiar a un álbum». No screenshot here: Robolectric's native graphics with a fixed screen size never gets
 * idle with a focused text field inside a dialog (a test-only problem).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es")
class AlbumPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun anExistingAlbumOrANewName() {
        var moved = ""; var copied = ""
        compose.setContent { LumaTheme { AlbumPicker(listOf("Viajes", "Familia"), false, {}, { moved = it }, { copied = it }) } }
        compose.onNodeWithText("Mover").assertIsNotEnabled()            // nothing chosen yet
        compose.onNodeWithText("Familia").performClick()
        compose.onNodeWithText("Copiar").performClick()
        assertEquals("Familia", copied)
        compose.onNode(hasSetTextAction()).performTextInput("  Playa 2026 ")
        compose.onNodeWithText("Mover").performClick()
        assertEquals("Playa 2026", moved)
    }
}

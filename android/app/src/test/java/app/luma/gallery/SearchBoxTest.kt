package app.luma.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.luma.gallery.ui.IndexStatus
import app.luma.gallery.ui.screens.SearchBox
import app.luma.gallery.ui.theme.LumaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Buscar: the broom shows only with something written, and one tap empties the search. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es")
class SearchBoxTest {
    @get:Rule val compose = createComposeRule()

    @Test fun broomClearsTheSearch() {
        var query by mutableStateOf("")
        compose.setContent { LumaTheme { SearchBox(query, { query = it }, emptyList(), IndexStatus(), showHelp = false) } }
        compose.onNodeWithContentDescription("Limpiar búsqueda").assertDoesNotExist()
        query = "celular"
        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        assertEquals("", query)
        compose.onNodeWithContentDescription("Limpiar búsqueda").assertDoesNotExist()
    }
}

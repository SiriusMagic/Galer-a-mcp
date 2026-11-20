package app.luma.gallery

import android.net.Uri
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryTest {
    private fun item(id: Long, name: String, folder: String, date: Long, video: Boolean = false) =
        Media(id, Uri.parse("content://media/$id"), name, folder, date, video)

    private val beach = item(1, "playa.jpg", "Viajes", 30)
    private val dog = item(2, "perro.jpg", "Camera", 20)
    private val clip = item(2, "cumple.mp4", "Camera", 40, video = true) // same id as a photo: allowed in MediaStore
    private val shot = item(3, "pantalla.png", "", 10)
    private val all = listOf(beach, dog, clip, shot)

    @Test fun albumsHaveFavoritesFirstAndTheNewestCover() {
        val albums = Library.albums(all, setOf(dog.key))
        assertEquals(listOf("Favoritas", "Camera", "Otros", "Viajes"), albums.map { it.name })
        assertEquals(dog, albums[0].cover)                       // the favorite itself, not any photo of its album
        assertEquals(clip, albums[1].cover)                      // newest of Camera
        assertEquals(2, albums[1].count)
        assertFalse(Library.albums(all, emptySet()).any { it.favorites }) // no empty «Favoritas»
    }

    @Test fun photosAndVideosWithTheSameIdAreDifferentFavorites() {
        val favorites = Library.toggle(emptySet(), clip)
        assertTrue(clip.key in favorites)
        assertFalse(dog.key in favorites)
        assertEquals(emptySet<String>(), Library.toggle(favorites, clip))
    }

    @Test fun searchNeedsEveryWordInNameOrAlbum() {
        assertEquals(listOf(beach), Library.filter(all, emptySet(), null, "PLAYA viajes"))
        assertEquals(emptyList<Media>(), Library.filter(all, emptySet(), null, "playa camera"))
        assertEquals(listOf(clip, beach, dog, shot), Library.filter(all, emptySet(), null, "  "))
        assertEquals(listOf(shot), Library.filter(all, emptySet(), "Otros", ""))
        assertEquals(listOf(clip), Library.filter(all, setOf(clip.key), "Favoritas", ""))
    }

    @Test fun backClosesViewerThenAlbumThenPage() {
        val saved = mutableListOf<Set<String>>()
        val state = GalleryState(saveFavorites = { saved += it })
        state.show(all)
        state.openAlbum("Camera"); state.viewing = dog
        assertEquals(listOf(clip, dog), state.visible)
        assertTrue(state.back()); assertEquals(null, state.viewing)
        assertTrue(state.back()); assertEquals(Page.ALBUMS, state.page)
        assertTrue(state.back()); assertEquals(Page.PHOTOS, state.page)
        assertFalse(state.back())
        state.toggleFavorite(beach)
        assertEquals(listOf(setOf(beach.key)), saved)
    }

    @Test fun aDeletedItemLeavesTheViewer() {
        val state = GalleryState()
        state.show(all); state.viewing = shot
        state.show(all - shot)
        assertEquals(null, state.viewing)
    }
}

package app.luma.gallery

import android.net.Uri
import app.luma.gallery.data.AlbumKind
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryTest {
    private val day = 86_400_000L
    private fun item(id: Long, name: String, folder: String, taken: Long, video: Boolean = false) =
        Media(id, Uri.parse("content://media/$id"), name, folder, taken / 1000, video, durationMs = if (video) 69_000 else 0, takenAt = taken)

    private val beach = item(1, "playa.jpg", "Viajes", 3 * day)
    private val dog = item(2, "perro.jpg", "Camera", 2 * day)
    private val clip = item(2, "cumple.mp4", "Camera", 4 * day, video = true) // same id as a photo: allowed in MediaStore
    private val shot = item(3, "pantalla.png", "Screenshots", 1 * day)
    private val all = listOf(beach, dog, clip, shot)

    @Test fun builtInAlbumsFirstThenFolders() {
        val (pinned, more) = Library.albums(all, setOf(dog.key))
        assertEquals(listOf(AlbumKind.ALL, AlbumKind.FAVORITES, AlbumKind.CAMERA, AlbumKind.VIDEOS, AlbumKind.SCREENSHOTS), pinned.map { it.kind })
        assertEquals(listOf(4, 1, 2, 1, 1), pinned.map { it.count })
        assertEquals(dog, pinned[1].cover)                        // a real favorite, right after Todas
        assertEquals(clip, pinned[2].cover)                       // newest of Cámara
        assertEquals(listOf("Viajes"), more.map { it.name })      // Camera and Screenshots are not repeated
        assertFalse(Library.albums(all, emptySet()).first.any { it.kind == AlbumKind.FAVORITES })
        assertEquals(listOf(clip), Library.filter(all, emptySet(), Library.builtIn(AlbumKind.VIDEOS)))
    }

    @Test fun photosAreGroupedByDayNewestFirstAndVideosShowTheirLength() {
        val days = Library.byDay(all, ZoneOffset.UTC)
        assertEquals(listOf(5L, 4L, 3L, 2L).map { LocalDate.ofEpochDay(it - 1) }, days.map { it.first })
        assertEquals("01:09", Library.duration(69_000))
        // Pinch in: months, then years (more and smaller photos); pinch out goes back.
        assertEquals(listOf(LocalDate.of(1970, 1, 1)), Library.sections(all, app.luma.gallery.data.Zoom.MONTH, ZoneOffset.UTC).map { it.first })
        assertEquals(4, Library.sections(all, app.luma.gallery.data.Zoom.YEAR, ZoneOffset.UTC).single().second.size)
        assertEquals(app.luma.gallery.data.Zoom.YEAR, app.luma.gallery.data.Zoom.DAY.coarser().coarser().coarser())
        assertEquals(app.luma.gallery.data.Zoom.DAY, app.luma.gallery.data.Zoom.MONTH.finer().finer())
        assertEquals("1:01:01", Library.duration(3_661_000))
    }

    @Test fun photosAndVideosWithTheSameIdAreDifferentFavorites() {
        val favorites = Library.toggle(emptySet(), clip)
        assertTrue(clip.key in favorites)
        assertFalse(dog.key in favorites)
    }

    @Test fun selectingFavoritesAndBack() {
        val saved = mutableListOf<Set<String>>()
        val state = GalleryState(saveFavorites = { saved += it })
        state.show(all)
        state.select(dog); state.select(beach)
        state.tap(shot)                                           // while selecting, a tap picks
        assertEquals(setOf(dog.key, beach.key, shot.key), state.selected)
        assertNull(state.viewing)
        state.favoriteSelection()
        assertEquals(setOf(dog.key, beach.key, shot.key), saved.last())
        assertTrue(state.selected.isEmpty())
        state.openAlbum("Viajes"); state.tap(beach)
        assertEquals(beach, state.viewing)
        assertTrue(state.back()); assertTrue(state.back()); assertEquals(Page.ALBUMS, state.page)
        state.go(Page.TRASH); assertTrue(state.back()); assertEquals(Page.ALBUMS, state.page)
        assertTrue(state.back()); assertFalse(state.back())
    }

    @Test fun trashedItemsLeaveTheViewerAndTheSelection() {
        val state = GalleryState()
        state.show(all); state.viewing = shot; state.select(dog)
        state.show(all - shot - dog)
        assertNull(state.viewing)
        assertTrue(state.selected.isEmpty())
        state.show(all, trashed = listOf(dog))
        state.go(Page.TRASH)
        assertEquals(listOf(dog), state.visible)
    }
}

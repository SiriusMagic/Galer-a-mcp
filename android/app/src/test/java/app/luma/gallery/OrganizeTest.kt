package app.luma.gallery

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.data.Memories
import app.luma.gallery.data.Memory
import app.luma.gallery.data.Vault
import app.luma.gallery.index.Facts
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.Page
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/** Memorias, Bóveda, new albums and safe folder names. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrganizeTest {
    private fun at(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun item(id: Long, date: LocalDate, folder: String = "Camera") =
        Media(id, Uri.parse("content://media/$id"), "IMG_$id.jpg", folder, at(date) / 1000, false, takenAt = at(date))

    @Test fun memoriesOnThisDayMonthsAndThemes() {
        val today = LocalDate.of(2026, 11, 20)
        val lastYear = (1L..2L).map { item(it, LocalDate.of(2025, 11, 19)) }
        val november = (10L..16L).map { item(it, LocalDate.of(2026, 11, 1)) }
        val dogs = (20L..24L).map { item(it, LocalDate.of(2026, 3, 1)) }
        val facts = dogs.associate { it.key to Facts(it.key, mapOf("dog" to .9f), "", 0) }
        val built = Memories.build(lastYear + november + dogs, facts, today, ZoneOffset.UTC)
        assertEquals(listOf(Memory.Kind.ON_THIS_DAY, Memory.Kind.MONTH, Memory.Kind.THEME), built.map { it.kind })
        assertEquals(2025, built[0].year)
        assertEquals(2026 to 11, built[1].year to built[1].month)        // March has 5 items: not a month memory
        assertEquals("dog", built[2].label)
        assertEquals(5, built[2].items.size)
        assertTrue(Memories.build(november.take(3), emptyMap(), today, ZoneOffset.UTC).isEmpty())
    }

    @Test fun albumNamesAreSafeFolders() {
        assertEquals("Viaje a Quito", Library.safeName("  Viaje/a\\Quito  "))
        assertEquals("Luma", Library.safeName("../"))
        assertEquals(60, Library.safeName("x".repeat(100)).length)
    }

    @Test fun hiddenFilesStayPrivateAndCanBeErased() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val vault = Vault(context) { 7 }
        val photo = item(1, LocalDate.of(2026, 1, 1))
        val added = vault.hide(listOf(photo to "image/jpeg")) { "bytes".byteInputStream() }
        assertEquals(1, added.size)
        assertTrue(added[0].file.path.startsWith(context.filesDir.path))  // inside Luma, not in the gallery
        assertEquals(listOf("IMG_1.jpg"), Vault(context).list().map { it.name })
        assertEquals(1, vault.hide(listOf(photo to "image/jpeg")) { null }.size + 1) // unreadable: skipped, not half-saved
        vault.erase(added)
        assertTrue(vault.list().isEmpty())
        assertFalse(added[0].file.exists())
    }

    @Test fun newAlbumThenPickPhotosAndTheVaultLocksWhenLeft() {
        val state = GalleryState()
        state.show(listOf(item(1, LocalDate.of(2026, 1, 1))))
        state.startNewAlbum("Viajes")
        assertEquals(Page.PHOTOS, state.page); assertEquals("Viajes", state.target)
        assertTrue(state.back()); assertEquals(null, state.target); assertEquals(Page.ALBUMS, state.page)
        state.unlockVault(); assertTrue(state.vaultOpen); assertEquals(Page.VAULT, state.page)
        state.go(Page.PHOTOS); assertFalse(state.vaultOpen)
        state.unlockVault(); state.lockVault(); assertEquals(Page.ALBUMS, state.page)
    }
}

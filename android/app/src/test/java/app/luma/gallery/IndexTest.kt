package app.luma.gallery

import android.net.Uri
import app.luma.gallery.data.Media
import app.luma.gallery.index.Analyzer
import app.luma.gallery.index.ContentSearch
import app.luma.gallery.index.Indexer
import app.luma.gallery.index.MemoryFacts
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Buscar sin ver: the index (with a fake ML Kit) and the search by content in Spanish. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IndexTest {
    private fun item(id: Long, name: String, date: Long) = Media(id, Uri.parse("content://media/$id"), name, "Camera", date, false)
    private val dog = item(1, "IMG_1.jpg", 30)
    private val beach = item(2, "IMG_2.jpg", 20)
    private val receipt = item(3, "IMG_3.jpg", 10)
    private val broken = item(4, "IMG_4.jpg", 5)
    private val seen = mapOf(
        dog.uri to (mapOf("dog" to .95f, "beach" to .7f) to ""),
        beach.uri to (mapOf("beach" to .9f, "sky" to .8f) to ""),
        receipt.uri to (mapOf("paper" to .8f) to "SUPERMERCADO TOTAL 23,50 Cumpleaños"))
    private val analyzer = Analyzer { uri -> seen[uri] ?: error("unreadable") }

    @Test fun analyzesNewPhotosOnceForgetsDeletedOnesAndSurvivesBadFiles() = runBlocking {
        val store = MemoryFacts()
        val first = Indexer(store, analyzer).run(listOf(dog, beach, receipt, broken), limit = 3)
        assertEquals(Indexer.Result(analyzed = 3, failed = 0, removed = 0, pending = 1), first)
        val second = Indexer(store, analyzer).run(listOf(dog, beach, receipt, broken))
        assertEquals(Indexer.Result(analyzed = 0, failed = 1, removed = 0, pending = 0), second) // not retried forever
        val third = Indexer(store, analyzer).run(listOf(dog, receipt))
        assertEquals(2, third.removed)
        assertEquals(setOf(dog.key, receipt.key), store.keys())
    }

    @Test fun searchesBySpanishWordsTextAndRanksByConfidence() = runBlocking {
        val store = MemoryFacts()
        Indexer(store, analyzer).run(listOf(dog, beach, receipt))
        val facts = store.all().associateBy { it.key }
        val media = listOf(dog, beach, receipt)
        assertEquals(listOf(beach, dog), ContentSearch.search("fotos de la playa", media, facts).map { it.media })
        assertEquals(listOf(dog), ContentSearch.search("perro en la playa", media, facts).map { it.media })
        assertEquals(listOf(receipt), ContentSearch.search("supermercado", media, facts).map { it.media })
        assertEquals(listOf(receipt), ContentSearch.search("CUMPLEANOS", media, facts).map { it.media })   // accents ignored
        assertEquals(listOf(dog), ContentSearch.search("dog", media, facts).map { it.media })               // English works too
        assertTrue(ContentSearch.search("gato", media, facts).isEmpty())
        assertEquals(listOf("perro"), ContentSearch.search("perro", media, facts).single().why)
        assertEquals("playa", ContentSearch.suggestions(facts.values).first())
    }
}

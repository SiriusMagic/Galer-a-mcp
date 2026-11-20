package app.luma.gallery.index

import app.luma.gallery.data.Media
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Brings the index up to date: forgets deleted photos, then analyzes new ones (newest first) up to [limit]. */
class Indexer(private val store: FactsStore, private val analyzer: Analyzer, private val now: () -> Long = System::currentTimeMillis) {
    data class Result(val analyzed: Int, val failed: Int, val removed: Int, val pending: Int)

    suspend fun run(media: List<Media>, limit: Int = Int.MAX_VALUE, progress: (done: Int, total: Int) -> Unit = { _, _ -> }): Result {
        val present = media.mapTo(HashSet()) { it.key }
        val known = store.keys()
        val gone = known - present
        if (gone.isNotEmpty()) store.remove(gone)
        val todo = media.filter { it.key !in known }.sortedByDescending { it.dateAdded }
        val batch = todo.take(limit)
        var analyzed = 0; var failed = 0
        batch.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive() // stops at once when charging ends or the user turns the screen on
            try {
                val (labels, text) = analyzer.analyze(item.uri)
                store.put(Facts(item.key, labels, text, now())); analyzed++
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) {
                // An unreadable file is remembered as empty, so it is not retried forever.
                store.put(Facts(item.key, emptyMap(), "", now())); failed++
            }
            progress(index + 1, batch.size)
        }
        return Result(analyzed, failed, gone.size, todo.size - batch.size)
    }
}

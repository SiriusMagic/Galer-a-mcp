package app.luma.gallery.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One hidden photo or video: a private file of Luma that no other app (nor the phone's gallery) can see. */
data class Hidden(val id: String, val name: String, val mime: String, val album: String, val hiddenAt: Long, val file: File) {
    val isVideo get() = mime.startsWith("video")
}

/**
 * «Ocultar» / Bóveda: the file is copied into Luma's private storage (never backed up), then Android deletes the
 * original after asking. Opening the vault asks for the phone's lock (screen). Restoring puts it back in its album.
 */
class Vault(context: Context, private val writer: MediaWriter? = null, private val now: () -> Long = System::currentTimeMillis) {
    private val dir = File(context.filesDir, "vault").apply { mkdirs() }
    private val index = File(dir, "index.json")

    fun list(): List<Hidden> = runCatching {
        val array = JSONArray(index.readText())
        (0 until array.length()).map { array.getJSONObject(it) }.map {
            Hidden(it.getString("id"), it.getString("name"), it.getString("mime"), it.getString("album"), it.getLong("at"), File(dir, it.getString("id")))
        }.filter { it.file.exists() }
    }.getOrDefault(emptyList())

    /** Copies [items] in; returns the new entries (the originals are deleted by the screen, with Android's confirmation). */
    suspend fun hide(items: List<Pair<Media, String>>, open: (Uri) -> java.io.InputStream?): List<Hidden> = withContext(Dispatchers.IO) {
        val added = items.mapNotNull { (item, mime) ->
            val id = UUID.randomUUID().toString()
            val file = File(dir, id)
            runCatching {
                requireNotNull(open(item.uri)).use { input -> file.outputStream().use { input.copyTo(it) } }
                Hidden(id, item.name, mime, item.folder, now(), file)
            }.getOrElse { file.delete(); null }
        }
        save(list() + added)
        added
    }

    /** Back to the phone's photos, in its album; then the private copy is erased. */
    suspend fun restore(entries: List<Hidden>): Int = withContext(Dispatchers.IO) {
        val w = requireNotNull(writer)
        val done = entries.filter { e -> runCatching { w.insertFrom(Uri.fromFile(e.file), e.name, e.mime, w.folderFor(e.album, e.isVideo)) }.isSuccess }
        erase(done); done.size
    }

    /** Gone for good (also used when the user cancels deleting the originals after hiding). */
    suspend fun erase(entries: List<Hidden>) = withContext(Dispatchers.IO) {
        val ids = entries.map { it.id }.toSet()
        entries.forEach { it.file.delete() }
        save(list().filterNot { it.id in ids })
    }

    private fun save(entries: List<Hidden>) {
        val tmp = File(dir, "index.tmp")
        tmp.writeText(JSONArray(entries.map {
            JSONObject().put("id", it.id).put("name", it.name).put("mime", it.mime).put("album", it.album).put("at", it.hiddenAt)
        }).toString())
        tmp.renameTo(index)
    }
}

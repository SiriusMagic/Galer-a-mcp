package app.luma.gallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Everything that writes to the phone's photos. New files (copies, PDFs, restored items) go through MediaStore, which
 * needs no permission; moving someone else's file needs Android's write confirmation first (asked by the screen).
 */
class MediaWriter(private val context: Context) {
    private val resolver = context.contentResolver

    /** Albums are folders: «Viajes» is Pictures/Viajes (or Movies/Viajes for videos). */
    fun folderFor(album: String, video: Boolean) =
        (if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/" + Library.safeName(album)

    /** Copies into [album]; returns how many were copied. */
    suspend fun copy(items: List<Media>, album: String): Int = withContext(Dispatchers.IO) {
        items.count { item -> runCatching { insertFrom(item.uri, item.name, mime(item), folderFor(album, item.isVideo)) }.isSuccess }
    }

    /** Moves into [album] (Android already confirmed write access to [items]). */
    suspend fun move(items: List<Media>, album: String): Int = withContext(Dispatchers.IO) {
        items.count { item ->
            runCatching {
                resolver.update(item.uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folderFor(album, item.isVideo) + "/")
                }, null, null) > 0
            }.getOrDefault(false)
        }
    }

    /** One PDF with one photo per page (A4, fitted), saved in Documents/Luma; videos are skipped. Returns its Uri. */
    suspend fun exportPdf(items: List<Media>, name: String): Uri? = withContext(Dispatchers.IO) {
        val photos = items.filterNot { it.isVideo }.ifEmpty { return@withContext null }
        val pdf = PdfDocument()
        try {
            photos.forEachIndexed { index, item ->
                val bitmap = resolver.loadThumbnail(item.uri, Size(1600, 1600), null)
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(A4_W, A4_H, index + 1).create())
                page.canvas.drawBitmap(bitmap, null, fit(bitmap, A4_W - 2 * MARGIN, A4_H - 2 * MARGIN), null)
                pdf.finishPage(page); bitmap.recycle()
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${Library.safeName(name)}.pdf")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Luma")
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use(pdf::writeTo)
            uri
        } finally { pdf.close() }
    }

    /** Writes [source] as a new MediaStore item in [folder]; used by copy and by the vault's restore. */
    fun insertFrom(source: Uri, name: String, mime: String, folder: String): Uri {
        val video = mime.startsWith("video")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/"); put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val target = requireNotNull(resolver.insert(collection, values)) { "insert refused" }
        try {
            resolver.openInputStream(source).use { input -> resolver.openOutputStream(target).use { output -> requireNotNull(input).copyTo(requireNotNull(output)) } }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return target
        } catch (e: Exception) { resolver.delete(target, null, null); throw e }
    }

    fun mime(item: Media): String = resolver.getType(item.uri) ?: if (item.isVideo) "video/mp4" else "image/jpeg"

    private fun fit(bitmap: Bitmap, w: Int, h: Int): Rect {
        val scale = minOf(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
        val bw = (bitmap.width * scale).toInt(); val bh = (bitmap.height * scale).toInt()
        val left = MARGIN + (w - bw) / 2; val top = MARGIN + (h - bh) / 2
        return Rect(left, top, left + bw, top + bh)
    }

    private companion object { const val A4_W = 595; const val A4_H = 842; const val MARGIN = 24 }
}

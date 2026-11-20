package app.luma.gallery.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads photos and videos from MediaStore, off the main thread. Nothing is written or uploaded. */
class MediaStoreSource(private val context: Context) {
    suspend fun load(): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH, MediaStore.Files.FileColumns.HEIGHT)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val result = ArrayList<Media>()
        context.contentResolver.query(MediaStore.Files.getContentUri("external"), projection, selection, args,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val date = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val type = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val path = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val size = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val width = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val height = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            while (c.moveToNext()) {
                val video = c.getInt(type) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                result += Media(c.getLong(id), ContentUris.withAppendedId(base, c.getLong(id)), c.getString(name).orEmpty(),
                    c.getString(path).orEmpty().trimEnd('/').substringAfterLast('/').ifBlank { Library.OTHERS },
                    c.getLong(date), video, c.getLong(size), c.getInt(width), c.getInt(height))
            }
        }
        result
    }
}

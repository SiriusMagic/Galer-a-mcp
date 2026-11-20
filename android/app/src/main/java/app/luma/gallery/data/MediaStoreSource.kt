package app.luma.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads photos and videos from MediaStore, off the main thread. Nothing is written or uploaded. */
class MediaStoreSource(private val context: Context) {
    /** Photos and videos; [trashed] = only those in Android's trash («Eliminado recientemente», Android 11+). */
    suspend fun load(trashed: Boolean = false): List<Media> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH, MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION, MediaStore.Files.FileColumns.DATE_TAKEN)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val result = ArrayList<Media>()
        val order = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val uri = MediaStore.Files.getContentUri("external")
        val cursor = if (Build.VERSION.SDK_INT >= 30) context.contentResolver.query(uri, projection, Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, order)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
        }, null) else if (trashed) null else context.contentResolver.query(uri, projection, selection, args, order)
        cursor?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val date = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val type = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val path = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
            val size = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val width = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val height = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val duration = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val taken = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            while (c.moveToNext()) {
                val video = c.getInt(type) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                result += Media(c.getLong(id), ContentUris.withAppendedId(base, c.getLong(id)), c.getString(name).orEmpty(),
                    c.getString(path).orEmpty().trimEnd('/').substringAfterLast('/').ifBlank { Library.OTHERS },
                    c.getLong(date), video, c.getLong(size), c.getInt(width), c.getInt(height), c.getLong(duration),
                    c.getLong(taken).takeIf { it > 0 } ?: c.getLong(date) * 1000)
            }
        }
        result
    }
}

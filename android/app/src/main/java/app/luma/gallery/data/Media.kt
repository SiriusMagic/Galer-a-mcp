package app.luma.gallery.data

import android.net.Uri

/** One photo or video of the phone (only what MediaStore says; the file itself is never copied). */
data class Media(
    val id: Long,
    val uri: Uri,
    val name: String,
    /** Last folder of its path ("Camera", "Screenshots"…); albums are built from it. */
    val folder: String,
    /** Seconds since 1970, as MediaStore gives it. */
    val dateAdded: Long,
    val isVideo: Boolean,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
) {
    /** Unique across photos and videos (MediaStore ids are shared by both tables). */
    val key: String get() = "${if (isVideo) "v" else "i"}$id"
}

/** An album of the Álbumes tab: a folder, or the special «Favoritas». */
data class Album(val name: String, val count: Int, val cover: Media?, val favorites: Boolean = false)

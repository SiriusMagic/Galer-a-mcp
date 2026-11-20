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
    /** Videos only. */
    val durationMs: Long = 0,
    /** When it was taken (ms); falls back to when it was added. */
    val takenAt: Long = dateAdded * 1000,
) {
    /** Unique across photos and videos (MediaStore ids are shared by both tables). */
    val key: String get() = "${if (isVideo) "v" else "i"}$id"
}

/** Built-in albums first (Todas, Cámara, Videos, Capturas, Favoritas), then «Más álbumes» (one per folder). */
enum class AlbumKind { ALL, CAMERA, VIDEOS, SCREENSHOTS, FAVORITES, FOLDER }

/** [id] is the folder name for FOLDER, or "@kind" for a built-in album (so a folder called «Videos» never clashes). */
data class Album(val id: String, val name: String, val count: Int, val cover: Media?, val kind: AlbumKind = AlbumKind.FOLDER)

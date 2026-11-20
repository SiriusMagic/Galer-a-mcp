package app.luma.gallery.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pinch levels of the Fotos grid: pinch in to see months, again for years (more, smaller photos). */
enum class Zoom(val columns: Int) {
    DAY(4), MONTH(6), YEAR(10);
    fun coarser() = entries.getOrElse(ordinal + 1) { this }
    fun finer() = entries.getOrElse(ordinal - 1) { this }
}

/** Pure rules over the loaded media (no Android calls, so they are unit tested). Favorites are keyed by [Media.key]. */
object Library {
    const val OTHERS = "Otros"
    fun builtIn(kind: AlbumKind) = "@" + kind.name.lowercase()

    fun isCamera(m: Media) = m.folder.equals("Camera", true) || m.folder.equals("DCIM", true)
    fun isScreenshot(m: Media) = m.folder.contains("screenshot", true) || m.folder.contains("captura", true)

    /** Built-in albums (Todas always, then Favoritas…, only those with something in them) and the folders, biggest first. */
    fun albums(media: List<Media>, favorites: Set<String>): Pair<List<Album>, List<Album>> {
        fun album(kind: AlbumKind, items: List<Media>) = Album(builtIn(kind), kind.name, items.size, items.maxByOrNull { it.takenAt }, kind)
        val pinned = listOf(album(AlbumKind.ALL, media)) + listOf(
            AlbumKind.FAVORITES to media.filter { it.key in favorites }, AlbumKind.CAMERA to media.filter(::isCamera),
            AlbumKind.VIDEOS to media.filter { it.isVideo }, AlbumKind.SCREENSHOTS to media.filter(::isScreenshot),
        ).filter { it.second.isNotEmpty() }.map { (kind, items) -> album(kind, items) }
        val more = media.filterNot { isCamera(it) || isScreenshot(it) }.groupBy { it.folder.ifBlank { OTHERS } }
            .map { (name, items) -> Album(name, name, items.size, items.maxByOrNull { it.takenAt }) }
            .sortedWith(compareByDescending<Album> { it.count }.thenBy { it.name.lowercase() })
        return pinned to more
    }

    /** Items of one album ([album] null = all), matching [query] in the name or folder, newest first. */
    fun filter(media: List<Media>, favorites: Set<String>, album: String?, query: String = ""): List<Media> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return media.filter { item ->
            val inAlbum = when (album) {
                null, builtIn(AlbumKind.ALL) -> true
                builtIn(AlbumKind.CAMERA) -> isCamera(item)
                builtIn(AlbumKind.VIDEOS) -> item.isVideo
                builtIn(AlbumKind.SCREENSHOTS) -> isScreenshot(item)
                builtIn(AlbumKind.FAVORITES) -> item.key in favorites
                else -> item.folder.ifBlank { OTHERS } == album
            }
            val text = "${item.name} ${item.folder}".lowercase()
            inAlbum && words.all { it in text }
        }.sortedByDescending { it.takenAt }
    }

    /** The Fotos tab: one section per day, newest first. */
    fun byDay(media: List<Media>, zone: ZoneId = ZoneId.systemDefault()) = sections(media, Zoom.DAY, zone)

    /** Sections for a pinch level: per day, month (first day of it) or year (1 January), newest first. */
    fun sections(media: List<Media>, zoom: Zoom, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, List<Media>>> =
        media.sortedByDescending { it.takenAt }.groupBy {
            val day = Instant.ofEpochMilli(it.takenAt).atZone(zone).toLocalDate()
            when (zoom) { Zoom.DAY -> day; Zoom.MONTH -> day.withDayOfMonth(1); Zoom.YEAR -> day.withDayOfYear(1) }
        }.toList()

    /** «01:09» or «1:02:03». */
    fun duration(ms: Long): String {
        val s = ms / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
    }

    /** A folder name Android accepts: no slashes or control characters, trimmed, at most 60 characters. */
    fun safeName(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex("\\s+"), " ")
        .trim().trim('.').take(60).ifBlank { "Luma" }

    fun toggle(favorites: Set<String>, item: Media): Set<String> =
        if (item.key in favorites) favorites - item.key else favorites + item.key
}

package app.luma.gallery.mcp

import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.index.ContentSearch
import app.luma.gallery.index.Facts
import app.luma.gallery.index.Labels
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** What the tools read and change; the app gives the real one, tests a fake. */
interface GalleryData {
    suspend fun media(): List<Media>
    suspend fun facts(): Map<String, Facts>
    fun favorites(): Set<String>
    fun setFavorite(key: String, on: Boolean)
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * Luma's MCP tools. The AI gets what is known about each photo (date, album, what it shows, its text), never the
 * picture itself: «buscar sin ver». Only gallery_favorite changes anything, and only with a WRITE token.
 */
object GalleryTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun str(d: String) = JSONObject().put("type", "string").put("description", d)
    private fun int(d: String, max: Int) = JSONObject().put("type", "integer").put("minimum", 1).put("maximum", max).put("description", d)

    val all = listOf(
        Tool("gallery_search", "Search the user's photos and videos by what they show or the text inside them, in Spanish or English " +
            "(e.g. 'perro en la playa', 'recibo supermercado'). Returns ids, date, album, what was recognised and a text snippet; " +
            "never the image.", false, schema("query" to str("Words to look for"), "limit" to int("Max results (default 20)", 50), required = listOf("query"))),
        Tool("gallery_recent", "The newest photos and videos, optionally of one album.", false,
            schema("album" to str("Album (folder) name; omit for all"), "limit" to int("Max results (default 20)", 50))),
        Tool("gallery_albums", "The albums (folders) with how many items each has, plus Favorites.", false, schema()),
        Tool("gallery_info", "Everything known about one photo or video by id: date, album, size, resolution, length, labels, text.",
            false, schema("id" to str("Id from another gallery tool (e.g. i1234)"), required = listOf("id"))),
        Tool("gallery_stats", "Totals: photos, videos, albums, favorites, how many were analysed for search and the most common things.",
            false, schema()),
        Tool("gallery_favorite", "Mark or unmark a photo as favorite.", true,
            schema("id" to str("Photo id"), "favorite" to JSONObject().put("type", "boolean"), required = listOf("id", "favorite"))),
    )

    fun available(access: Access) = all.filter { !it.write || access == Access.WRITE }

    suspend fun call(name: String, args: JSONObject, access: Access, data: GalleryData): JSONObject {
        val tool = all.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool called $name.")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token is read-only.")
        val media = data.media()
        return when (name) {
            "gallery_search" -> {
                val query = args.optString("query").trim().take(200).ifEmpty { throw ToolFailure("INVALID_ARGUMENT", "query is required.") }
                val facts = data.facts()
                val hits = ContentSearch.search(query, media, facts).take(limit(args))
                JSONObject().put("query", query).put("analysed", facts.size).put("total", media.size)
                    .put("results", JSONArray(hits.map { describe(it.media, facts[it.media.key], data).put("score", round(it.score)).put("matched", JSONArray(it.why)) }))
            }
            "gallery_recent" -> {
                val album = args.optString("album").takeIf { it.isNotBlank() }
                val facts = data.facts()
                JSONObject().put("results", JSONArray(Library.filter(media, data.favorites(), album).take(limit(args)).map { describe(it, facts[it.key], data) }))
            }
            "gallery_albums" -> {
                val (pinned, more) = Library.albums(media, data.favorites())
                JSONObject().put("albums", JSONArray((pinned + more).map { JSONObject().put("name", it.id).put("count", it.count) }))
            }
            "gallery_info" -> {
                val item = find(media, args)
                describe(item, data.facts()[item.key], data).put("size", item.size).put("width", item.width).put("height", item.height)
                    .apply { if (item.isVideo) put("duration", Library.duration(item.durationMs)) }
            }
            "gallery_stats" -> {
                val facts = data.facts()
                JSONObject().put("photos", media.count { !it.isVideo }).put("videos", media.count { it.isVideo })
                    .put("albums", Library.albums(media, data.favorites()).second.size).put("favorites", data.favorites().size)
                    .put("analysed", facts.size).put("common", JSONArray(ContentSearch.suggestions(facts.values, 15)))
            }
            else -> { // gallery_favorite
                val item = find(media, args)
                if (!args.has("favorite")) throw ToolFailure("INVALID_ARGUMENT", "favorite (true/false) is required.")
                data.setFavorite(item.key, args.getBoolean("favorite"))
                JSONObject().put("id", item.key).put("favorite", args.getBoolean("favorite"))
            }
        }
    }

    private fun limit(args: JSONObject) = args.optInt("limit", 20).coerceIn(1, 50)
    private fun round(f: Float) = Math.round(f * 100) / 100.0

    private fun find(media: List<Media>, args: JSONObject): Media {
        val id = args.optString("id").ifEmpty { throw ToolFailure("INVALID_ARGUMENT", "id is required.") }
        return media.firstOrNull { it.key == id } ?: throw ToolFailure("NOT_FOUND", "No photo or video with id $id.")
    }

    private fun describe(item: Media, facts: Facts?, data: GalleryData) = JSONObject().put("id", item.key).put("name", item.name)
        .put("type", if (item.isVideo) "video" else "photo").put("taken", Instant.ofEpochMilli(item.takenAt).toString())
        .put("album", item.folder).put("favorite", item.key in data.favorites())
        .put("sees", JSONArray(facts?.labels?.entries?.sortedByDescending { it.value }?.map { Labels.spanish(it.key) }.orEmpty()))
        .apply { facts?.text?.takeIf { it.isNotBlank() }?.let { put("text", it.take(200)) } }
}

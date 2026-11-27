package app.luma.gallery

import android.net.Uri
import app.luma.gallery.data.Media
import app.luma.gallery.index.Facts
import app.luma.gallery.mcp.Access
import app.luma.gallery.mcp.GalleryData
import app.luma.gallery.mcp.HubLink
import app.luma.gallery.mcp.McpHandler
import app.luma.gallery.mcp.McpMode
import app.luma.gallery.mcp.Reply
import app.luma.gallery.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Luma's MCP: tokens, tools (list, a good call, bad arguments, not found), limits and the hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val dog = Media(1, Uri.parse("content://media/1"), "IMG_1.jpg", "Camera", 30, false, takenAt = 30_000)
    private val receipt = Media(2, Uri.parse("content://media/2"), "IMG_2.jpg", "WhatsApp Images", 20, false, takenAt = 20_000)
    private val data = object : GalleryData {
        var favs = setOf<String>()
        override suspend fun media() = listOf(dog, receipt)
        override suspend fun facts() = mapOf(dog.key to Facts(dog.key, mapOf("dog" to .93f), "", 0),
            receipt.key to Facts(receipt.key, mapOf("paper" to .8f), "SUPERMERCADO TOTAL 23,50", 0))
        override fun favorites() = favs
        override fun setFavorite(key: String, on: Boolean) { favs = if (on) favs + key else favs - key }
    }
    private val reader = Token("lum_" + "r".repeat(40), Access.READ)
    private val writer = Token("lum_" + "w".repeat(40), Access.WRITE)
    private val handler = McpHandler({ listOf(reader, writer) }, data, "0.5.0")

    private fun call(token: Token?, method: String, params: JSONObject = JSONObject(), query: Map<String, String> = emptyMap()): Reply = runBlocking {
        val headers = token?.let { mapOf("Authorization" to "Bearer ${it.secret}") }.orEmpty()
        handler.handle("POST", "/mcp", headers, query, JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString())
    }
    private fun result(r: Reply) = JSONObject(r.body).getJSONObject("result")
    private fun tool(token: Token, name: String, args: JSONObject) = result(call(token, "tools/call", JSONObject().put("name", name).put("arguments", args)))

    @Test fun onlyValidTokensGetIn() {
        assertEquals(401, call(null, "ping").status)
        assertEquals(401, call(Token("wrong_" + "x".repeat(40)), "ping").status)
        assertEquals(200, call(null, "ping", query = mapOf("token" to reader.secret)).status)     // ?token= like Stone's links
        assertEquals(405, runBlocking { handler.handle("GET", "/mcp", mapOf("x-api-key" to reader.secret), emptyMap(), "") }.status)
        assertEquals(202, runBlocking { handler.handle("POST", "/mcp", mapOf("x-api-key" to reader.secret), emptyMap(), """{"jsonrpc":"2.0","method":"notifications/initialized"}""") }.status)
    }

    @Test fun initializeTellsTheHubWhichTokenItIsAndToolsFollowTheAccess() {
        val meta = result(call(reader, "initialize")).getJSONObject("_meta").getJSONObject(McpHandler.TOKEN_META)
        assertEquals("redirect", meta.getString("kind")); assertEquals("Arka", meta.getString("name")); assertEquals("read", meta.getString("mode"))
        assertFalse(meta.toString().contains(reader.secret))
        val readTools = result(call(reader, "tools/list")).getJSONArray("tools").toString()
        assertTrue(readTools.contains("gallery_search")); assertFalse(readTools.contains("gallery_favorite"))
        assertTrue(result(call(writer, "tools/list")).getJSONArray("tools").toString().contains("gallery_favorite"))
    }

    @Test fun searchFindsBySpanishWordsAndTextWithoutTheImage() {
        val found = tool(reader, "gallery_search", JSONObject().put("query", "perro")).getJSONObject("structuredContent").getJSONArray("results")
        assertEquals(1, found.length()); assertEquals("i1", found.getJSONObject(0).getString("id"))
        assertEquals("perro", found.getJSONObject(0).getJSONArray("sees").getString(0))
        val text = tool(reader, "gallery_search", JSONObject().put("query", "supermercado")).getJSONObject("structuredContent")
        assertTrue(text.getJSONArray("results").getJSONObject(0).getString("text").contains("TOTAL"))
        assertFalse(text.toString().contains("content://"))                                      // no way to the picture
    }

    @Test fun badArgumentsNotFoundAndReadOnlyAreErrorsTheAiCanRead() {
        fun code(token: Token, name: String, args: JSONObject) = tool(token, name, args).let {
            assertTrue(it.getBoolean("isError")); it.getJSONObject("structuredContent").getJSONObject("error").getString("code")
        }
        assertEquals("INVALID_ARGUMENT", code(reader, "gallery_search", JSONObject()))
        assertEquals("NOT_FOUND", code(reader, "gallery_info", JSONObject().put("id", "i999")))
        assertEquals("READ_ONLY", code(reader, "gallery_favorite", JSONObject().put("id", "i1").put("favorite", true)))
        assertEquals("UNKNOWN_TOOL", code(reader, "gallery_delete_all", JSONObject()))
        assertFalse(tool(writer, "gallery_favorite", JSONObject().put("id", "i1").put("favorite", true)).optBoolean("isError"))
        assertEquals(setOf("i1"), data.favs)
        val albums = tool(reader, "gallery_albums", JSONObject()).getJSONObject("structuredContent").getJSONArray("albums")
        assertEquals(listOf("@all", "@favorites", "@camera", "WhatsApp Images"), (0 until albums.length()).map { albums.getJSONObject(it).getString("name") })
    }

    @Test fun byDateTypeAndOrder() {
        fun ids(args: JSONObject) = tool(reader, "gallery_by_date", args).getJSONObject("structuredContent").getJSONArray("results")
            .let { r -> (0 until r.length()).map { r.getJSONObject(it).getString("id") } }
        val day = java.time.Instant.ofEpochMilli(30_000).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        assertEquals(listOf("i1", "i2"), ids(JSONObject().put("from", day)))
        assertEquals(listOf("i2", "i1"), ids(JSONObject().put("from", day).put("order", "oldest")))
        assertEquals(emptyList<String>(), ids(JSONObject().put("from", day).put("type", "video")))
        assertEquals(emptyList<String>(), ids(JSONObject().put("from", "2030-01-01")))
        val bad = tool(reader, "gallery_by_date", JSONObject().put("from", "ayer"))
        assertEquals("INVALID_ARGUMENT", bad.getJSONObject("structuredContent").getJSONObject("error").getString("code"))
        val recent = tool(reader, "gallery_recent", JSONObject().put("order", "oldest").put("limit", 1)).getJSONObject("structuredContent")
        assertEquals("i2", recent.getJSONArray("results").getJSONObject(0).getString("id"))
        assertEquals(2, recent.getInt("total"))
    }

    @Test fun tooManyRequestsAreRefused() {
        repeat(McpHandler.PER_MINUTE) { assertEquals(200, call(writer, "ping").status) }
        assertEquals(429, call(writer, "ping").status)
    }

    @Test fun theHubGetsTheLinkOnlyFromTheSameDeveloperWithTheSwitchOn() {
        McpMode.load(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        McpMode.turnRemote(false)
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.connect(false).getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.tool(false, "{}").getString("error"))
        // Only the same developer (Arka) may connect, disconnect, read the status or run a tool.
        listOf(HubLink.disconnect(false), HubLink.status(false)).forEach { assertEquals("NOT_SAME_DEVELOPER", it.getString("error")) }
        McpMode.turnRemote(true)
        val link = HubLink.answer(true).getString("link")!!
        assertTrue(link.matches(Regex("https://.+/d/[a-f0-9]{24}/mcp\\?token=lum_.+")))
        McpMode.turnRemote(false)
    }

    /** Lucía runs a tool on the phone through the same handler, with or without the public link; Arka turns that on and off. */
    @Test fun luciaRunsToolsOnThePhone() {
        val relay = app.luma.gallery.mcp.McpMode.relayUrl
        app.luma.gallery.mcp.McpMode.relayUrl = ""                                             // no relay in tests
        app.luma.gallery.mcp.McpMode.load(org.robolectric.RuntimeEnvironment.getApplication())
        app.luma.gallery.mcp.McpMode.turnRemote(false)
        val list = JSONObject(app.luma.gallery.mcp.McpMode.local("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")!!)
        assertTrue(list.getJSONObject("result").getJSONArray("tools").length() > 0)
        McpMode.allowLocal(false)
        assertEquals("LOCAL_DISABLED", HubLink.tool(true, "{}").getString("error"))
        McpMode.allowLocal(true)
        val viaProvider = JSONObject(HubLink.tool(true, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""").getString("response"))
        assertEquals(2, viaProvider.getInt("id"))
        assertEquals("BAD_REQUEST", HubLink.tool(true, "").getString("error"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.tool(false, "{}").getString("error"))
        assertTrue(HubLink.status(true).containsKey("remote"))
        HubLink.connect(true); assertTrue(app.luma.gallery.mcp.McpMode.remote)                 // Redireccionar a Hub
        assertFalse(HubLink.disconnect(true).getBoolean("remote")); assertFalse(app.luma.gallery.mcp.McpMode.remote)
        McpMode.allowLocal(false)
        app.luma.gallery.mcp.McpMode.relayUrl = relay                                          // as the other tests expect
    }

    /** The hub's token from before Arka took over is kept, so the hub's link keeps working. */
    @Test fun theOldHubTokenIsKept() {
        val old = """[{"id":"a","secret":"${"l".repeat(44)}","kind":"llm"},{"id":"b","secret":"${"h".repeat(44)}","kind":"redirect"}]"""
        assertEquals("h".repeat(44), app.luma.gallery.mcp.Legacy.redirectSecret(old))
        assertEquals(null, app.luma.gallery.mcp.Legacy.redirectSecret("[]"))
        assertEquals(null, app.luma.gallery.mcp.Legacy.redirectSecret(""))
    }
}

package app.luma.gallery.mcp

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import app.luma.gallery.BuildConfig
import app.luma.gallery.data.Favorites
import app.luma.gallery.data.Media
import app.luma.gallery.data.MediaStoreSource
import app.luma.gallery.index.Facts
import app.luma.gallery.index.SqlFacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The public relay may run for MCP or for redirection. Only redirection serves the current Arka token.
 * The phone grants local access separately through the signature-protected provider.
 */
object McpMode {
    var remote by mutableStateOf(false); private set
    var enabled by mutableStateOf(false); private set
    var localMcp by mutableStateOf(false); private set
    val active get() = enabled || remote
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** Called when an AI changed favorites, so the screen shows it. */
    var onFavorites: (Set<String>) -> Unit = {}

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    private var token = Token("")
    internal var relayUrl: String = BuildConfig.RELAY_URL
    val loaded get() = prefs != null

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        remote = p.getBoolean("redirect", true)
        enabled = p.getBoolean("enabled", false)
        localMcp = p.getBoolean("localMcp", false)
        token = Token(SecretBox.open(p.getString("arka", "") ?: "").ifBlank {
            (Legacy.redirectSecret(SecretBox.open(p.getString("tokens", "") ?: "")) ?: Token.secret())
                .also { p.edit { putString("arka", SecretBox.seal(it)); remove("tokens") } }
        })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        data = AppData(app)
        apply()
    }

    fun enable(on: Boolean) { enabled = on; prefs?.edit { putBoolean("enabled", on) }; apply() }
    fun allowLocal(on: Boolean) { localMcp = on; prefs?.edit { putBoolean("localMcp", on) } }

    fun turnRemote(on: Boolean) { remote = on; prefs?.edit { putBoolean("redirect", on) }; apply() }

    fun hubLink() = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=${token.secret}"

    private var data: GalleryData? = null

    /** One JSON-RPC request from Lucía on this phone, through the same handler as the relay. Null before [load]. */
    fun local(body: String): String? {
        val d = data ?: return null
        return kotlinx.coroutines.runBlocking {
            McpHandler({ listOf(token) }, d, BuildConfig.VERSION_NAME).handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(), body).body
        }
    }

    private fun apply() {
        val d = data ?: return
        if (active && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret,
                McpHandler({ if (remote) listOf(token) else emptyList() }, d, BuildConfig.VERSION_NAME)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }


    /** The real gallery: read fresh for each call (cached for 30 s), favorites shared with the screen. */
    private class AppData(context: Context) : GalleryData {
        private val source = MediaStoreSource(context)
        private val facts = SqlFacts(context)
        private val favorites = Favorites(context)
        private var cache: Pair<Long, List<Media>>? = null
        override suspend fun media(): List<Media> {
            cache?.takeIf { System.currentTimeMillis() - it.first < 30_000 }?.let { return it.second }
            return source.load().also { cache = System.currentTimeMillis() to it }
        }
        override suspend fun facts(): Map<String, Facts> = withContext(Dispatchers.IO) { facts.all().associateBy { it.key } }
        override fun favorites() = favorites.read()
        override fun setFavorite(key: String, on: Boolean) {
            val next = if (on) favorites.read() + key else favorites.read() - key
            favorites.write(next); onFavorites(next)
        }
    }
}

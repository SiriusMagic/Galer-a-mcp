package app.luma.gallery.index

/**
 * What Luma knows about one photo without anyone looking at it: the things it shows (English ML Kit labels with
 * their confidence, 0–1) and the text inside it. Together they are the photo's search vector.
 */
data class Facts(val key: String, val labels: Map<String, Float>, val text: String, val indexedAt: Long)

/** Where the facts live (SQLite on the phone; memory in tests). */
interface FactsStore {
    fun all(): List<Facts>
    fun keys(): Set<String>
    fun put(facts: Facts)
    fun remove(keys: Collection<String>)
    fun clear()
}

/** Reads one photo or video frame; never uploads anything. */
fun interface Analyzer { suspend fun analyze(uri: android.net.Uri): Pair<Map<String, Float>, String> }

class MemoryFacts : FactsStore {
    private val map = LinkedHashMap<String, Facts>()
    override fun all() = map.values.toList()
    override fun keys() = map.keys.toSet()
    override fun put(facts: Facts) { map[facts.key] = facts }
    override fun remove(keys: Collection<String>) { keys.forEach(map::remove) }
    override fun clear() = map.clear()
}

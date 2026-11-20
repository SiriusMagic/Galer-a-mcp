package app.luma.gallery.index

import app.luma.gallery.data.Media
import java.text.Normalizer

/**
 * Search by what a photo shows or says. Each photo is a sparse vector (label → confidence, plus its words); a query
 * is turned into the same space (Spanish words mapped to ML Kit labels) and photos are ranked by their score.
 */
object ContentSearch {
    data class Hit(val media: Media, val score: Float, val why: List<String>)

    fun search(query: String, media: List<Media>, facts: Map<String, Facts>): List<Hit> {
        val words = tokens(query)
        if (words.isEmpty()) return emptyList()
        return media.mapNotNull { item ->
            val f = facts[item.key]
            val text = fold("${f?.text.orEmpty()} ${item.name} ${item.folder}")
            var score = 0f
            val why = ArrayList<String>()
            for (word in words) {
                val labels = Labels.toEnglish(word)
                val label = f?.labels?.entries?.filter { (name, _) -> name in labels || fold(name) == word }?.maxByOrNull { it.value }
                val best = when {
                    label != null -> { why += Labels.spanish(label.key); label.value }
                    text.contains(word) -> { why += "\"$word\""; 0.9f }
                    else -> return@mapNotNull null // every word must match something
                }
                score += best
            }
            Hit(item, score / words.size, why.distinct())
        }.sortedWith(compareByDescending<Hit> { it.score }.thenByDescending { it.media.dateAdded })
    }

    /** The things that appear most in the library, in Spanish: the search suggestions. */
    fun suggestions(facts: Collection<Facts>, max: Int = 12): List<String> = facts.flatMap { it.labels.keys }
        .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(max).map { Labels.spanish(it.key) }

    fun tokens(query: String) = fold(query).split(Regex("[^a-z0-9]+")).filter { it.length >= 2 && it !in STOP }

    /** Lowercase without accents (ñ becomes n), so «Cumpleaños», «cumpleanos» and «CUMPLEAÑOS» match. */
    fun fold(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    private val STOP = setOf("de", "la", "el", "los", "las", "en", "con", "un", "una", "y", "mi", "mis", "fotos", "foto", "del", "al", "the", "of")
}

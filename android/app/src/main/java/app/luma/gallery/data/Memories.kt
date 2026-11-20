package app.luma.gallery.data

import app.luma.gallery.index.Facts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A collection Luma builds by itself for the Memorias tab. [label] is an ML Kit label for THEME memories. */
data class Memory(val id: String, val kind: Kind, val items: List<Media>, val year: Int = 0, val month: Int = 0, val label: String = "") {
    enum class Kind { ON_THIS_DAY, MONTH, THEME }
    val cover get() = items.first()
}

/** Pure rules (tested): «Un día como hoy», the last months, and themes found by the smart search. */
object Memories {
    fun build(media: List<Media>, facts: Map<String, Facts>, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): List<Memory> {
        fun date(m: Media) = Instant.ofEpochMilli(m.takenAt).atZone(zone).toLocalDate()
        val result = ArrayList<Memory>()
        // Same day (± 3) in earlier years, one memory per year.
        media.filter { val d = date(it); d.year < today.year && kotlin.math.abs(d.withYear(today.year).toEpochDay() - today.toEpochDay()) <= 3 }
            .groupBy { date(it).year }.toSortedMap(compareByDescending { it })
            .forEach { (year, items) -> result += Memory("day-$year", Memory.Kind.ON_THIS_DAY, items.sortedByDescending { it.takenAt }, year = year) }
        // The six most recent months with at least 6 items.
        media.groupBy { date(it).let { d -> d.year * 100 + d.monthValue } }.filterValues { it.size >= 6 }
            .toSortedMap(compareByDescending { it }).entries.take(6)
            .forEach { (key, items) -> result += Memory("month-$key", Memory.Kind.MONTH, items.sortedByDescending { it.takenAt }, key / 100, key % 100) }
        // Themes: labels seen with confidence in at least 5 photos (dogs, beach, food…), the 6 biggest.
        val byKey = media.associateBy { it.key }
        facts.values.flatMap { f -> f.labels.filterValues { it >= .75f }.keys.map { it to f.key } }
            .groupBy({ it.first }, { it.second }).filterValues { it.size >= 5 }
            .entries.sortedByDescending { it.value.size }.take(6)
            .forEach { (label, keys) -> result += Memory("theme-$label", Memory.Kind.THEME, keys.mapNotNull(byKey::get).sortedByDescending { it.takenAt }, label = label) }
        return result.filter { it.items.isNotEmpty() }
    }
}

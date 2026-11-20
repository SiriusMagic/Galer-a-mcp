package app.luma.gallery.index

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.json.JSONObject

/** The index on the phone: one row per photo, parameterized queries only. Excluded from every backup. */
class SqlFacts(context: Context) : SQLiteOpenHelper(context.applicationContext, "index.db", null, 1), FactsStore {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE facts (key TEXT PRIMARY KEY, labels TEXT NOT NULL, text TEXT NOT NULL, at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit

    override fun all(): List<Facts> = readableDatabase.query("facts", null, null, null, null, null, null).use { c ->
        buildList { while (c.moveToNext()) add(Facts(c.getString(0), parseLabels(c.getString(1)), c.getString(2), c.getLong(3))) }
    }

    override fun keys(): Set<String> = readableDatabase.query("facts", arrayOf("key"), null, null, null, null, null).use { c ->
        buildSet { while (c.moveToNext()) add(c.getString(0)) }
    }

    override fun put(facts: Facts) {
        writableDatabase.insertWithOnConflict("facts", null, ContentValues().apply {
            put("key", facts.key); put("labels", JSONObject(facts.labels.mapValues { it.value.toDouble() }).toString())
            put("text", facts.text); put("at", facts.indexedAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun remove(keys: Collection<String>) {
        writableDatabase.transaction { keys.forEach { delete("facts", "key = ?", arrayOf(it)) } }
    }

    override fun clear() { writableDatabase.delete("facts", null, null) }

    private fun parseLabels(json: String): Map<String, Float> = runCatching {
        val o = JSONObject(json); o.keys().asSequence().associateWith { o.getDouble(it).toFloat() }
    }.getOrDefault(emptyMap())
}

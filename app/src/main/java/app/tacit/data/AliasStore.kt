package app.tacit.data

import android.content.Context
import java.io.File

class AliasStore(context: Context) {

    private val file = File(context.filesDir, "aliases.csv")
    private val lock = Any()
    private var map: MutableMap<String, MutableList<String>>? = null

    fun all(): Map<String, List<String>> = synchronized(lock) { HashMap(load()) }

    fun get(key: String): List<String> = synchronized(lock) { load()[key].orEmpty() }

    fun set(key: String, aliases: List<String>) = synchronized(lock) {
        val data = load()
        if (aliases.isEmpty()) data.remove(key) else data[key] = aliases.toMutableList()
        save(data)
    }

    fun setMany(values: Map<String, List<String>>) = synchronized(lock) {
        val data = load()
        for ((key, aliases) in values) {
            if (aliases.isEmpty()) data.remove(key) else data[key] = aliases.toMutableList()
        }
        save(data)
    }

    fun keysForAlias(alias: String): List<String> = synchronized(lock) {
        val needle = alias.trim().lowercase()
        load().filterValues { list -> list.any { it.lowercase() == needle } }.keys.toList()
    }

    fun exportRows(): List<List<String>> = synchronized(lock) {
        load().entries.sortedBy { it.key }.map { listOf(it.key, AliasText.join(it.value)) }
    }

    private fun load(): MutableMap<String, MutableList<String>> {
        map?.let { return it }
        val loaded = HashMap<String, MutableList<String>>()
        if (file.exists()) {
            for (row in Csv.decode(file.readText())) {
                if (row.size >= 2 && row[0].isNotBlank()) loaded[row[0]] = AliasText.split(row[1]).toMutableList()
            }
        }
        map = loaded
        return loaded
    }

    private fun save(data: Map<String, List<String>>) {
        val temp = File(file.parentFile, "aliases.csv.tmp")
        temp.writeText(Csv.encode(data.entries.sortedBy { it.key }.map { listOf(it.key, AliasText.join(it.value)) }))
        temp.renameTo(file)
    }
}

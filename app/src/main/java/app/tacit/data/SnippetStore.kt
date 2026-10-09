package app.tacit.data

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Snippet(val id: String, val trigger: String, val title: String, val body: String)

class SnippetStore(context: Context) {

    private val file = File(context.filesDir, "snippets.csv")
    private val lock = Any()
    private var cache: MutableList<Snippet>? = null

    fun all(): List<Snippet> = synchronized(lock) { ArrayList(load()) }

    fun upsert(snippet: Snippet) = synchronized(lock) {
        val list = load()
        val index = list.indexOfFirst { it.id == snippet.id }
        if (index >= 0) list[index] = snippet else list.add(snippet)
        save(list)
    }

    fun replaceAll(snippets: List<Snippet>) = synchronized(lock) {
        val list = load()
        list.clear()
        list.addAll(snippets)
        save(list)
    }

    fun delete(id: String) = synchronized(lock) {
        val list = load()
        list.removeAll { it.id == id }
        save(list)
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun exportRows(): List<List<String>> = synchronized(lock) { load().map { listOf(it.trigger, it.title, it.body) } }

    fun importRows(rows: List<List<String>>) = synchronized(lock) {
        val list = load()
        for (row in rows) {
            if (row.size < 3 || row[2].isEmpty()) continue
            if (row[0] == "trigger" && row[1] == "title") continue
            val existing = list.indexOfFirst { it.trigger.isNotEmpty() && it.trigger == row[0] }
            val snippet = Snippet(if (existing >= 0) list[existing].id else newId(), row[0], row[1], row[2])
            if (existing >= 0) list[existing] = snippet else list.add(snippet)
        }
        save(list)
    }

    private fun load(): MutableList<Snippet> {
        cache?.let { return it }
        val loaded = ArrayList<Snippet>()
        if (file.exists()) {
            for (row in Csv.decode(file.readText())) {
                if (row.size >= 4) loaded.add(Snippet(row[0], row[1], row[2], row[3]))
            }
        }
        cache = loaded
        return loaded
    }

    private fun save(list: List<Snippet>) {
        val temp = File(file.parentFile, "snippets.csv.tmp")
        temp.writeText(Csv.encode(list.map { listOf(it.id, it.trigger, it.title, it.body) }))
        temp.renameTo(file)
    }
}

object SnippetExpander {
    private val placeholder = Regex("\\{(date|time|datetime|clipboard)(?::([^}]+))?\\}")

    fun expand(body: String, now: LocalDateTime, clipboard: String?): String =
        placeholder.replace(body) { match ->
            val format = match.groupValues[2]
            when (match.groupValues[1]) {
                "date" -> now.format(DateTimeFormatter.ofPattern(format.ifEmpty { "yyyy-MM-dd" }))
                "time" -> now.format(DateTimeFormatter.ofPattern(format.ifEmpty { "HH:mm" }))
                "datetime" -> now.format(DateTimeFormatter.ofPattern(format.ifEmpty { "yyyy-MM-dd HH:mm" }))
                else -> clipboard.orEmpty()
            }
        }
}

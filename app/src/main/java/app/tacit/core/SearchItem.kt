package app.tacit.core

enum class ItemKind { APP, CONTACT, SETTING, FILE, SNIPPET, CLIPBOARD, CALCULATOR, WEB, ACTION }

object IndexOptions {
    @Volatile
    var transliterate: Boolean = true
}

class SearchItem(
    val key: String,
    val kind: ItemKind,
    val label: String,
    val subtitle: String = "",
    val extraNames: List<String> = emptyList(),
    val payload: String = "",
    val extra: Any? = null
) {
    val names: Array<IndexedName>
    var aliases: Array<String> = emptyArray()
        private set
    var pinned: Boolean = false

    init {
        val collected = ArrayList<IndexedName>()
        for (name in listOf(label) + extraNames) {
            val normalized = Normalizer.normalize(Normalizer.splitCamel(name)).trim()
            if (normalized.isEmpty()) continue
            collected.add(IndexedName(normalized))
            if (IndexOptions.transliterate) {
                for (variant in ArabicLatin.variants(normalized)) collected.add(IndexedName(variant))
            }
        }
        names = collected.distinctBy { it.full }.toTypedArray()
    }

    fun withAliases(values: Collection<String>): SearchItem {
        aliases = values.map { Normalizer.normalize(it).trim() }.filter { it.isNotEmpty() }.toTypedArray()
        return this
    }
}

class IndexedName(val full: String) {
    val words: Array<String> = Normalizer.words(full).toTypedArray()
    val initials: String = words.joinToString("") { it.take(1) }
    val compact: String = words.joinToString("")
}

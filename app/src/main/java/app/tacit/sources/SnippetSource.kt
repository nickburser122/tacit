package app.tacit.sources

import app.tacit.core.ItemKind
import app.tacit.core.SearchItem
import app.tacit.data.SnippetStore

class SnippetSource(private val store: SnippetStore) {

    @Volatile
    private var cached: List<SearchItem> = emptyList()

    fun items(): List<SearchItem> = cached

    fun reload() {
        cached = store.all().map { snippet ->
            val label = snippet.title.ifEmpty { snippet.body.lineSequence().first().take(60) }
            val names = listOfNotNull(snippet.trigger.ifEmpty { null })
            SearchItem("snippet:${snippet.id}", ItemKind.SNIPPET, label,
                snippet.body.replace('\n', ' ').take(80), names, snippet.body)
        }
    }
}

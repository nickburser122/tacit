package app.tacit.ui

import android.app.Activity
import android.widget.Toast
import app.tacit.Graph
import app.tacit.R
import app.tacit.core.RefreshSources
import app.tacit.data.Snippet

object SnippetMaker {

    fun saveClip(activity: Activity, text: String): Boolean {
        if (text.isBlank()) return false
        if (Graph.snippets.all().any { it.body == text }) {
            Toast.makeText(activity, R.string.snippet_exists, Toast.LENGTH_SHORT).show()
            return false
        }
        Graph.snippets.upsert(Snippet(Graph.snippets.newId(), "", "", text))
        Graph.requestRefresh(RefreshSources.SNIPPETS)
        Toast.makeText(activity, R.string.snippet_saved, Toast.LENGTH_SHORT).show()
        return true
    }
}

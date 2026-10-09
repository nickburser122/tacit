package app.tacit.core

enum class Scope { ALL, CALCULATOR, CLIPBOARD, SETTINGS, CONTACTS, FILES, SNIPPETS, WEB }

data class WebEngine(val prefix: String, val name: String, val template: String) {
    fun urlFor(query: String): String = template.replace("%s", java.net.URLEncoder.encode(query, "UTF-8"))
}

data class ParsedQuery(
    val raw: String,
    val text: String,
    val scope: Scope,
    val engine: WebEngine? = null
)

class PrefixGrammar(
    var calculator: String = "=",
    var clipboard: String = "cb ",
    var settings: String = ">",
    var contacts: String = "@",
    var files: String = "/",
    var snippets: String = ";",
    var engines: List<WebEngine> = defaultEngines
) {
    fun parse(raw: String): ParsedQuery {
        val trimmedStart = raw.trimStart()
        val pairs = listOf(
            clipboard to Scope.CLIPBOARD,
            calculator to Scope.CALCULATOR,
            settings to Scope.SETTINGS,
            contacts to Scope.CONTACTS,
            files to Scope.FILES,
            snippets to Scope.SNIPPETS
        ).sortedByDescending { it.first.length }
        for ((prefix, scope) in pairs) {
            if (prefix.isNotEmpty() && trimmedStart.startsWith(prefix, ignoreCase = true)) {
                return ParsedQuery(raw, trimmedStart.substring(prefix.length).trim(), scope)
            }
        }
        for (engine in engines.sortedByDescending { it.prefix.length }) {
            val token = engine.prefix + " "
            if (engine.prefix.isNotEmpty() && trimmedStart.startsWith(token, ignoreCase = true)) {
                return ParsedQuery(raw, trimmedStart.substring(token.length).trim(), Scope.WEB, engine)
            }
        }
        return ParsedQuery(raw, raw.trim(), Scope.ALL)
    }

    fun allPrefixes(): List<String> =
        listOf(calculator, clipboard, settings, contacts, files, snippets) + engines.map { it.prefix + " " }

    companion object {
        val defaultEngines = listOf(
            WebEngine("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
            WebEngine("g", "Google", "https://www.google.com/search?q=%s"),
            WebEngine("w", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s"),
            WebEngine("yt", "YouTube", "https://www.youtube.com/results?search_query=%s"),
            WebEngine("map", "OpenStreetMap", "https://www.openstreetmap.org/search?query=%s")
        )
    }
}

sealed class Recognized {
    data class Phone(val number: String) : Recognized()
    data class Url(val url: String) : Recognized()
    data class Email(val address: String) : Recognized()
}

object InputRecognizer {
    private val phonePattern = Regex("^\\+?[0-9][0-9 ()\\-]{5,}$")
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
    private val urlPattern = Regex("^(https?://)?([a-z0-9-]+\\.)+[a-z]{2,}(/\\S*)?$", RegexOption.IGNORE_CASE)

    fun recognize(text: String): Recognized? {
        val value = text.trim()
        if (value.isEmpty() || value.contains(' ') && !phonePattern.matches(value)) return null
        val digitsNormalized = Normalizer.digitsOnly(value)
        if (phonePattern.matches(Normalizer.normalize(value)) && digitsNormalized.count { it.isDigit() } >= 6) {
            return Recognized.Phone(digitsNormalized)
        }
        if (emailPattern.matches(value)) return Recognized.Email(value)
        if (urlPattern.matches(value) && value.contains('.') && !value.endsWith('.')) {
            val url = if (value.startsWith("http", ignoreCase = true)) value else "https://$value"
            return Recognized.Url(url)
        }
        return null
    }
}

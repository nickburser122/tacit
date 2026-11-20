package app.tacit.core

import java.net.URLDecoder

object PathNames {

    fun name(path: String): String {
        val tail = path.substringAfterLast('/')
        if (!path.startsWith("content://")) return tail
        val decoded = decode(tail)
        return decoded.substringAfterLast('/').substringAfterLast(':').ifEmpty { decoded }
    }

    fun folder(path: String): String {
        if (path.startsWith("content://")) return ""
        return path.substringBeforeLast('/', "").replace("/storage/emulated/0", "~")
    }

    fun documentId(absolutePath: String): String {
        val path = absolutePath.trimEnd('/')
        for (root in listOf("/storage/emulated/0", "/storage/self/primary", "/sdcard")) {
            if (path == root) return "primary:"
            if (path.startsWith("$root/")) return "primary:" + path.substring(root.length + 1)
        }
        if (path.startsWith("/storage/")) {
            val rest = path.removePrefix("/storage/")
            val volume = rest.substringBefore('/')
            if (volume.isNotEmpty() && volume != "emulated" && volume != "self") return volume + ":" + rest.substringAfter('/', "")
        }
        return "primary:"
    }

    private fun decode(text: String): String =
        runCatching { URLDecoder.decode(text.replace("+", "%2B"), "UTF-8") }.getOrDefault(text)
}

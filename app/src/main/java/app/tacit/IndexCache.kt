package app.tacit

import android.content.Context
import app.tacit.core.ItemKind
import app.tacit.core.SearchItem
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

object IndexCache {

    private const val FILE = "index.bin"
    private const val VERSION = 3

    fun write(context: Context, items: List<SearchItem>) {
        runCatching {
            val temp = File(context.cacheDir, "$FILE.tmp")
            DataOutputStream(BufferedOutputStream(temp.outputStream(), 1 shl 16)).use { out ->
                out.writeInt(VERSION)
                out.writeInt(items.size)
                for (item in items) {
                    out.writeUTF(item.key)
                    out.writeByte(item.kind.ordinal)
                    out.writeUTF(item.label.take(4000))
                    out.writeUTF(item.subtitle.take(4000))
                    out.writeUTF(item.payload.take(16000))
                    out.writeBoolean(item.pinned)
                    out.writeShort(item.extraNames.size.coerceAtMost(64))
                    for (name in item.extraNames.take(64)) out.writeUTF(name.take(1000))
                    out.writeShort(item.aliases.size.coerceAtMost(64))
                    for (alias in item.aliases.take(64)) out.writeUTF(alias.take(200))
                }
            }
            temp.renameTo(File(context.cacheDir, FILE))
        }
    }

    fun read(context: Context): List<SearchItem>? {
        val file = File(context.cacheDir, FILE)
        if (!file.exists()) return null
        return runCatching { readFile(file) }.getOrNull()
    }

    private fun readFile(file: File): List<SearchItem>? {
        DataInputStream(BufferedInputStream(file.inputStream(), 1 shl 16)).use { input ->
            if (input.readInt() != VERSION) return null
            val count = input.readInt()
            val kinds = ItemKind.entries
            val result = ArrayList<SearchItem>(count)
            repeat(count) {
                val key = input.readUTF()
                val kind = kinds[input.readByte().toInt()]
                val label = input.readUTF()
                val subtitle = input.readUTF()
                val payload = input.readUTF()
                val pinned = input.readBoolean()
                val extra = List(input.readShort().toInt()) { input.readUTF() }
                val aliases = List(input.readShort().toInt()) { input.readUTF() }
                val item = SearchItem(key, kind, label, subtitle, extra, payload).withAliases(aliases)
                item.pinned = pinned
                result.add(item)
            }
            return result
        }
    }

    fun clear(context: Context) {
        File(context.cacheDir, FILE).delete()
    }
}

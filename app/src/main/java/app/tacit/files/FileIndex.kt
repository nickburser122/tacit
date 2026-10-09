package app.tacit.files

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.storage.StorageManager
import app.tacit.Prefs
import app.tacit.core.Normalizer
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object StorageAccess {
    fun hasAllFiles(): Boolean = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
}

class FileHit(val path: String, val name: String, val size: Long, val modified: Long, val score: Int, val folder: String = "")

object PathDecoder {
    fun decode(value: String): String = runCatching { java.net.URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }.getOrDefault(value)
}

class FileTable(
    val directories: Array<String>,
    val names: Array<String>,
    val normalized: Array<String>,
    val parent: IntArray,
    val sizes: LongArray,
    val modified: LongArray
) {
    val count: Int get() = names.size

    val searchableDirectories: Array<String> by lazy {
        Array(directories.size) { index ->
            val dir = directories[index]
            (if (dir.contains('%')) PathDecoder.decode(dir) else dir).lowercase()
        }
    }

    fun pathOf(index: Int): String {
        val name = names[index]
        TreeScanner.documentUri(name)?.let { return it.toString() }
        return directories[parent[index]] + "/" + name
    }

    fun displayNameOf(index: Int): String = TreeScanner.displayName(names[index])

    fun folderOf(index: Int): String {
        val dir = directories[parent[index]]
        if (!dir.startsWith(TreeScanner.TREE_PREFIX)) return dir
        val encodedId = dir.substringAfterLast("/document/", dir.substringAfterLast("/tree/", ""))
        val id = PathDecoder.decode(encodedId)
        return "~/" + id.substringAfter(':', id)
    }

    companion object {
        val EMPTY = FileTable(emptyArray(), emptyArray(), emptyArray(), IntArray(0), LongArray(0), LongArray(0))
    }
}

class FileIndex(private val context: Context, private val prefs: Prefs) {

    private val file = File(context.filesDir, "files.bin")
    private val thread = HandlerThread("tacit-files").apply { start() }
    private val handler = Handler(thread.looper)
    private val generation = AtomicInteger()

    @Volatile
    private var table: FileTable? = null

    @Volatile
    var indexing = false
        private set

    fun hasFullAccess(): Boolean = StorageAccess.hasAllFiles() ||
        (Build.VERSION.SDK_INT < 30 &&
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

    fun hasAccess(): Boolean = hasFullAccess() || GrantedTrees.list(context).isNotEmpty()

    fun count(): Int = table?.count ?: 0

    fun search(query: String, limit: Int, callback: (List<FileHit>) -> Unit) {
        val token = generation.incrementAndGet()
        handler.post {
            if (token != generation.get()) return@post
            val current = table ?: loadFromDisk().also { table = it }
            val hits = FileMatcher.search(current, query, limit) { token != generation.get() }
            if (token == generation.get()) callback(hits)
        }
    }

    fun cancel() {
        generation.incrementAndGet()
    }

    fun rescan(done: (() -> Unit)? = null) {
        handler.post {
            if (!prefs.filesEnabled || !hasAccess()) {
                done?.invoke()
                return@post
            }
            indexing = true
            val scanner = FileScanner(prefs.filesHidden)
            if (hasFullAccess()) scanner.scanRoots(roots())
            else for (tree in GrantedTrees.list(context)) scanner.scanTree(context.contentResolver, tree)
            val built = scanner.build()
            table = built
            persist(built)
            indexing = false
            done?.invoke()
        }
    }

    fun clear() {
        handler.post {
            table = FileTable.EMPTY
            file.delete()
        }
    }

    private fun roots(): List<File> {
        val storage = context.getSystemService(StorageManager::class.java)
        val result = ArrayList<File>()
        if (Build.VERSION.SDK_INT >= 30) {
            for (volume in storage.storageVolumes) {
                val dir = volume.directory ?: continue
                if (dir.canRead()) result.add(dir)
            }
        }
        if (result.isEmpty()) result.add(Environment.getExternalStorageDirectory())
        return result
    }

    private fun persist(data: FileTable) {
        runCatching {
            val temp = File(file.parentFile, "files.bin.tmp")
            DataOutputStream(BufferedOutputStream(temp.outputStream(), 1 shl 17)).use { out ->
                out.writeInt(VERSION)
                out.writeInt(data.directories.size)
                for (dir in data.directories) out.writeUTF(dir)
                out.writeInt(data.count)
                for (i in 0 until data.count) {
                    out.writeUTF(data.names[i])
                    out.writeInt(data.parent[i])
                    out.writeLong(data.sizes[i])
                    out.writeLong(data.modified[i])
                }
            }
            temp.renameTo(file)
        }
    }

    private fun loadFromDisk(): FileTable {
        if (!file.exists()) return FileTable.EMPTY
        return runCatching { readTable() }.getOrDefault(FileTable.EMPTY)
    }

    private fun readTable(): FileTable {
        DataInputStream(BufferedInputStream(file.inputStream(), 1 shl 17)).use { input ->
            if (input.readInt() != VERSION) return FileTable.EMPTY
            val directories = Array(input.readInt()) { input.readUTF() }
            val count = input.readInt()
            val names = arrayOfNulls<String>(count)
            val normalized = arrayOfNulls<String>(count)
            val parent = IntArray(count)
            val sizes = LongArray(count)
            val modified = LongArray(count)
            for (i in 0 until count) {
                val name = input.readUTF()
                names[i] = name
                normalized[i] = TreeScanner.normalizedName(name)
                parent[i] = input.readInt()
                sizes[i] = input.readLong()
                modified[i] = input.readLong()
            }
            return FileTable(directories, names.requireNoNulls(), normalized.requireNoNulls(), parent, sizes, modified)
        }
    }

    companion object {
        private const val VERSION = 1
    }
}

class FileScanner(private val includeHidden: Boolean) {

    private val directories = ArrayList<String>()
    private val names = ArrayList<String>()
    private val normalized = ArrayList<String>()
    private val parents = ArrayList<Int>()
    private val sizes = ArrayList<Long>()
    private val modified = ArrayList<Long>()

    private val treeDirectories = HashMap<String, Int>()

    fun scan(roots: List<File>): FileTable {
        scanRoots(roots)
        return build()
    }

    fun scanRoots(roots: List<File>) {
        for (root in roots) {
            if (names.size >= MAX_FILES) return
            walk(root)
        }
    }

    fun scanTree(resolver: android.content.ContentResolver, tree: android.net.Uri) {
        TreeScanner(resolver, includeHidden) { name, parentKey, size, time ->
            val parentIndex = treeDirectories.getOrPut(parentKey) {
                directories.add(parentKey)
                directories.size - 1
            }
            names.add(name)
            normalized.add(TreeScanner.normalizedName(name))
            parents.add(parentIndex)
            sizes.add(size)
            modified.add(time)
            names.size < MAX_FILES
        }.scan(tree)
    }

    fun build(): FileTable = FileTable(
        directories.toTypedArray(), names.toTypedArray(), normalized.toTypedArray(),
        parents.toIntArray(), sizes.toLongArray(), modified.toLongArray()
    )

    private fun walk(root: File) {
        val stack = ArrayDeque<File>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            val children = dir.listFiles() ?: continue
            val dirIndex = directories.size
            directories.add(dir.absolutePath)
            for (child in children) {
                val name = child.name
                if (!includeHidden && name.startsWith('.')) continue
                if (child.isDirectory) {
                    if (isRestricted(child)) continue
                    stack.addLast(child)
                    add(name + "/", dirIndex, 0L, child.lastModified())
                } else {
                    add(name, dirIndex, child.length(), child.lastModified())
                }
                if (names.size >= MAX_FILES) return
            }
        }
    }

    private fun add(name: String, parent: Int, size: Long, time: Long) {
        names.add(name)
        normalized.add(Normalizer.normalize(name))
        parents.add(parent)
        sizes.add(size)
        modified.add(time)
    }

    private fun isRestricted(dir: File): Boolean {
        val path = dir.absolutePath
        return path.endsWith("/Android/data") || path.endsWith("/Android/obb")
    }

    companion object {
        const val MAX_FILES = 500_000
    }
}

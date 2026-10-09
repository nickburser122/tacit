package app.tacit.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeNamesTest {

    private val stored = "Report.pdf" + TreeScanner.TREE_SEPARATOR + "content://com.android.externalstorage.documents/tree/primary%3ADocs/document/primary%3ADocs%2FReport.pdf"

    @Test
    fun displayNameStripsUri() {
        assertEquals("Report.pdf", TreeScanner.displayName(stored))
        assertEquals("report.pdf", TreeScanner.normalizedName(stored))
    }

    @Test
    fun plainNamesUnchanged() {
        assertEquals("photo.jpg", TreeScanner.displayName("photo.jpg"))
    }

    @Test
    fun matcherWorksOnTreeEntries() {
        val names = arrayOf(stored, "notes.txt")
        val table = FileTable(
            arrayOf(TreeScanner.TREE_PREFIX + "content://x/tree/primary%3ADocs", "/storage/emulated/0/Download"),
            names,
            names.map { TreeScanner.normalizedName(it) }.toTypedArray(),
            intArrayOf(0, 1),
            LongArray(2),
            LongArray(2)
        )
        val hits = FileMatcher.search(table, "report", 5) { false }
        assertEquals(1, hits.size)
        assertEquals("Report.pdf", hits[0].name)
        assertTrue(hits[0].path.startsWith("content://"))
        val docs = FileMatcher.search(table, "report in:docs", 5) { false }
        assertEquals(1, docs.size)
    }

    @Test
    fun pathDecoderHandlesPercentEscapes() {
        assertEquals("primary:Docs/a b", PathDecoder.decode("primary%3ADocs%2Fa%20b"))
    }
}

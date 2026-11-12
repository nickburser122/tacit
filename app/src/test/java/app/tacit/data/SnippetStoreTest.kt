package app.tacit.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetStoreTest {

    private fun store(): SnippetStore = SnippetStore(File(Files.createTempDirectory("snippets").toFile(), "snippets.csv"))

    private val rows = listOf(
        listOf("", "", "Hello there"),
        listOf("sig", "Signature", "Best regards")
    )

    @Test
    fun importingTheSameRowsTwiceAddsNothingTheSecondTime() {
        val store = store()
        assertEquals(2, store.importRows(rows))
        assertEquals(0, store.importRows(rows))
        assertEquals(2, store.all().size)
    }

    @Test
    fun aRowWithAKnownTriggerUpdatesThatSnippet() {
        val store = store()
        store.importRows(rows)
        assertEquals(1, store.importRows(listOf(listOf("sig", "Signature", "Kind regards"))))
        assertEquals(2, store.all().size)
        assertEquals("Kind regards", store.all().first { it.trigger == "sig" }.body)
    }

    @Test
    fun theHeaderRowAndRowsWithoutABodyAreSkipped() {
        val store = store()
        val applied = store.importRows(listOf(listOf("trigger", "title", "body"), listOf("x", "y", ""), listOf("a", "b")))
        assertEquals(0, applied)
        assertEquals(0, store.all().size)
    }

    @Test
    fun aHeaderWithAByteOrderMarkIsSkippedToo() {
        val store = store()
        val applied = store.importRows(Csv.decode("\uFEFFtrigger,title,body\n,,Saved text"))
        assertEquals(1, applied)
        assertEquals("Saved text", store.all().single().body)
    }

    @Test
    fun savedSnippetsAreReadBackByANewStoreOnTheSameFile() {
        val file = File(Files.createTempDirectory("snippets").toFile(), "snippets.csv")
        SnippetStore(file).importRows(rows)
        assertEquals(2, SnippetStore(file).all().size)
    }
}

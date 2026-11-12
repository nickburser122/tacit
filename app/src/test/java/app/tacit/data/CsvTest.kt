package app.tacit.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun aByteOrderMarkIsNotPartOfTheFirstCell() {
        val rows = Csv.decode("\uFEFFtrigger,title,body\nsig,Signature,Best")
        assertEquals("trigger", rows[0][0])
        assertEquals(2, rows.size)
    }

    @Test
    fun fieldsWithCommasQuotesAndNewlinesRoundTrip() {
        val rows = listOf(listOf("a,b", "say \"hi\"", "line1\nline2"), listOf("", "x", "y"))
        assertEquals(rows, Csv.decode(Csv.encode(rows)))
    }

    @Test
    fun textWithoutAMarkIsUnchanged() {
        assertEquals(listOf(listOf("a", "b")), Csv.decode("a,b"))
    }
}

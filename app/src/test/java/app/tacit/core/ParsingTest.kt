package app.tacit.core

import app.tacit.calc.Calculator
import app.tacit.clip.Sensitivity
import app.tacit.clip.SensitiveDetector
import app.tacit.data.AliasText
import app.tacit.data.Csv
import app.tacit.data.SnippetExpander
import app.tacit.files.FileMatcher
import app.tacit.files.FileTable
import app.tacit.actions.PhoneFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ParsingTest {

    private val grammar = PrefixGrammar()

    @Test
    fun prefixes() {
        assertEquals(Scope.CALCULATOR, grammar.parse("=2+2").scope)
        assertEquals(Scope.CLIPBOARD, grammar.parse("cb pass").scope)
        assertEquals(Scope.SETTINGS, grammar.parse(">wifi").scope)
        assertEquals(Scope.CONTACTS, grammar.parse("@asmaa").scope)
        assertEquals(Scope.FILES, grammar.parse("/report").scope)
        assertEquals(Scope.SNIPPETS, grammar.parse(";addr").scope)
        assertEquals("wifi", grammar.parse(">wifi").text)
    }

    @Test
    fun enginePrefixNeedsSpace() {
        val parsed = grammar.parse("yt lofi beats")
        assertEquals(Scope.WEB, parsed.scope)
        assertEquals("lofi beats", parsed.text)
        assertEquals(Scope.ALL, grammar.parse("ytmusic").scope)
        assertEquals(Scope.ALL, grammar.parse("cbs news").scope)
        assertEquals(Scope.ALL, grammar.parse("wiki").scope)
    }

    @Test
    fun webUrlEncodes() {
        val engine = WebEngine("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s")
        assertEquals("https://duckduckgo.com/?q=a+b%26c", engine.urlFor("a b&c"))
    }

    @Test
    fun recognizer() {
        assertEquals(Recognized.Phone("+201001234567"), InputRecognizer.recognize("+20 100 123 4567"))
        assertEquals(Recognized.Phone("01001234567"), InputRecognizer.recognize("٠١٠٠١٢٣٤٥٦٧"))
        assertEquals(Recognized.Email("a@b.com"), InputRecognizer.recognize("a@b.com"))
        assertEquals(Recognized.Url("https://example.org"), InputRecognizer.recognize("example.org"))
        assertNull(InputRecognizer.recognize("whatsapp"))
        assertNull(InputRecognizer.recognize("2+2"))
        assertNull(InputRecognizer.recognize("asmaa gouda"))
    }

    @Test
    fun composerPutsCalculatorFirstAndWebLast() {
        val engine = SearchEngine()
        engine.replaceItems(listOf(SearchItem("a", ItemKind.APP, "Calculator")))
        val composer = ResultComposer(engine, grammar, Calculator())
        val output = composer.compose(ComposeInput("2+2", 8, PrefixGrammar.defaultEngines[0]) { emptyList() })
        assertEquals(ItemKind.CALCULATOR, output.rows.first().kind)
        assertEquals("4", output.rows.first().label)
        assertEquals(ItemKind.WEB, output.rows.last().kind)
    }

    @Test
    fun composerNumberGivesActions() {
        val composer = ResultComposer(SearchEngine(), grammar, Calculator())
        val rows = composer.compose(ComposeInput("01001234567", 8, PrefixGrammar.defaultEngines[0]) { emptyList() }).rows
        assertTrue(rows.any { it.payload == "dial|01001234567" })
        assertTrue(rows.any { it.payload == "sms|01001234567" })
        assertTrue(rows.any { it.payload == "wa|01001234567" })
    }

    @Test
    fun normalizer() {
        assertEquals("cafe", Normalizer.normalize("Café"))
        assertEquals("اسماء", Normalizer.normalize("أسماء"))
        assertEquals("جوده", Normalizer.normalize("جودة"))
        assertEquals("محمد", Normalizer.normalize("مُحَمَّد"))
        assertEquals("123", Normalizer.normalize("١٢٣"))
        assertEquals("you tube", Normalizer.normalize(Normalizer.splitCamel("YouTube")))
    }

    @Test
    fun transliterationVariants() {
        val variants = ArabicLatin.variants(Normalizer.normalize("أسماء جودة"))
        assertTrue(variants.any { it.startsWith("asma") })
        assertTrue(variants.any { it.contains("god") || it.contains("goud") || it.contains("jod") })
    }

    @Test
    fun sensitive() {
        assertEquals(Sensitivity.CARD, SensitiveDetector.classify("4111 1111 1111 1111"))
        assertEquals(Sensitivity.IBAN, SensitiveDetector.classify("GB82 WEST 1234 5698 7654 32"))
        assertEquals(Sensitivity.OTP, SensitiveDetector.classify("482913"))
        assertEquals(Sensitivity.PRIVATE_KEY, SensitiveDetector.classify("-----BEGIN OPENSSH PRIVATE KEY-----\nabc"))
        assertEquals(Sensitivity.TOKEN, SensitiveDetector.classify("ghp_9fK2LmQ8xR4tZ7vB1nC3dE6hJ0pS5wYa"))
        assertEquals(Sensitivity.PASSWORD_FLAG, SensitiveDetector.classify("hello", flaggedByOs = true))
        assertEquals(Sensitivity.NONE, SensitiveDetector.classify("meet at 5pm near the station"))
        assertEquals(Sensitivity.NONE, SensitiveDetector.classify("4111 1111 1111 1112"))
        assertEquals(Sensitivity.NONE, SensitiveDetector.classify("https://example.org/some/path/page"))
    }

    @Test
    fun csvRoundTrip() {
        val rows = listOf(listOf("app:a", "x, y"), listOf("k\"q", "line\nbreak"), listOf("", ""))
        assertEquals(rows, Csv.decode(Csv.encode(rows)))
    }

    @Test
    fun aliasSplitting() {
        assertEquals(listOf("wa", "chat"), AliasText.split(" wa ,chat,, wa"))
        assertEquals(listOf("واتس", "wa"), AliasText.split("واتس، wa"))
    }

    @Test
    fun snippetPlaceholders() {
        val now = LocalDateTime.of(2026, 10, 8, 9, 5)
        assertEquals("2026-10-08 09:05 X", SnippetExpander.expand("{date} {time} {clipboard}", now, "X"))
        assertEquals("08/10/2026", SnippetExpander.expand("{date:dd/MM/yyyy}", now, null))
    }

    @Test
    fun phoneFormat() {
        assertEquals("201001234567", PhoneFormat.international("01001234567", "20"))
        assertEquals("201001234567", PhoneFormat.international("+201001234567", "20"))
        assertEquals("201001234567", PhoneFormat.international("00201001234567", ""))
        assertEquals("1001234567", PhoneFormat.international("1001234567", ""))
    }

    @Test
    fun fileMatcher() {
        val table = FileTable(
            arrayOf("/storage/emulated/0/Download", "/storage/emulated/0/Documents"),
            arrayOf("Report 2026.pdf", "report.docx", "holiday.jpg", "Quarterly-report.pdf"),
            arrayOf("report 2026.pdf", "report.docx", "holiday.jpg", "quarterly-report.pdf"),
            intArrayOf(0, 1, 0, 1),
            LongArray(4),
            LongArray(4)
        )
        val pdfs = FileMatcher.search(table, "report pdf", 10) { false }.map { it.name }
        assertEquals(setOf("Report 2026.pdf", "Quarterly-report.pdf"), pdfs.toSet())
        val ext = FileMatcher.search(table, "ext:docx", 10) { false }.map { it.name }
        assertEquals(listOf("report.docx"), ext)
        val inDocs = FileMatcher.search(table, "report in:documents", 10) { false }.map { it.name }
        assertEquals(setOf("report.docx", "Quarterly-report.pdf"), inDocs.toSet())
        assertTrue(FileMatcher.search(table, "zzz", 10) { false }.isEmpty())
    }

    @Test
    fun fileSearchScalesToTwoHundredThousand() {
        val count = 200_000
        val names = Array(count) { "file_${it}_photo.jpg" }
        val table = FileTable(arrayOf("/storage/emulated/0/DCIM"), names, names.copyOf(), IntArray(count), LongArray(count), LongArray(count))
        FileMatcher.search(table, "1234", 10) { false }
        val start = System.nanoTime()
        repeat(5) { FileMatcher.search(table, "photo 9999", 10) { false } }
        val ms = (System.nanoTime() - start) / 1_000_000.0 / 5
        assertTrue("file search $ms ms", ms < 400)
    }
}

package app.tacit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScorerTest {

    private lateinit var engine: SearchEngine

    private fun app(label: String, vararg extra: String) = SearchItem("app:$label", ItemKind.APP, label, extraNames = extra.toList())
    private fun contact(label: String) = SearchItem("contact:$label", ItemKind.CONTACT, label)
    private fun setting(label: String, vararg synonyms: String) = SearchItem("setting:$label", ItemKind.SETTING, label, extraNames = synonyms.toList())

    @Before
    fun setUp() {
        IndexOptions.transliterate = true
        engine = SearchEngine()
        engine.replaceItems(
            listOf(
                contact("Asmaa Gouda"),
                contact("أسماء جودة"),
                contact("Gamal Osman"),
                contact("Ahmed Galal"),
                contact("Mohamed Salah"),
                app("YouTube"),
                app("YouTube Music"),
                app("WhatsApp"),
                app("Gmail"),
                app("Google"),
                app("Maps"),
                app("Calculator"),
                app("Calendar"),
                app("Camera"),
                setting("Wi-Fi", "wifi", "wireless"),
                setting("Bluetooth", "bt"),
                setting("Battery saver"),
                setting("Display", "brightness", "dark mode")
            )
        )
    }

    private fun top(query: String): String = engine.search(query, 10).first().item.label
    private fun labels(query: String): List<String> = engine.search(query, 10).map { it.item.label }

    @Test
    fun gosFindsAsmaaGouda() {
        assertTrue(labels("gos").contains("Asmaa Gouda"))
    }

    @Test
    fun initialsFindAsmaaGouda() {
        assertTrue(labels("ag").take(3).contains("Asmaa Gouda"))
        assertTrue(labels("ag").take(3).contains("Ahmed Galal"))
    }

    @Test
    fun wordPrefixFindsGouda() {
        assertEquals("Asmaa Gouda", top("gou"))
    }

    @Test
    fun multiWordAnyOrder() {
        assertEquals("Asmaa Gouda", top("asm gou"))
        assertEquals("Asmaa Gouda", top("gou asm"))
    }

    @Test
    fun latinFindsArabicContact() {
        assertTrue(labels("asmaa").contains("أسماء جودة"))
    }

    @Test
    fun arabicQueryNormalizesHamza() {
        assertTrue(labels("اسماء").contains("أسماء جودة"))
    }

    @Test
    fun wfiFindsWifi() {
        assertEquals("Wi-Fi", top("wfi"))
    }

    @Test
    fun typoFindsBluetooth() {
        assertEquals("Bluetooth", top("blutooth"))
    }

    @Test
    fun ytFindsYoutubeFirst() {
        assertEquals("YouTube", top("yt"))
    }

    @Test
    fun exactBeatsPrefix() {
        assertEquals("Maps", top("maps"))
    }

    @Test
    fun shorterNameWinsTie() {
        assertEquals("YouTube", top("youtube"))
    }

    @Test
    fun aliasBeatsEverything() {
        val items = engine.all().toMutableList()
        items.first { it.label == "Calendar" }.withAliases(listOf("cal"))
        engine.replaceItems(items)
        assertEquals("Calendar", top("cal"))
    }

    @Test
    fun sharedAliasReturnsAllOwners() {
        val items = engine.all().toMutableList()
        items.first { it.label == "Gmail" }.withAliases(listOf("work"))
        items.first { it.label == "Calendar" }.withAliases(listOf("work"))
        engine.replaceItems(items)
        val top2 = labels("work").take(2).toSet()
        assertEquals(setOf("Gmail", "Calendar"), top2)
    }

    @Test
    fun shortQueriesHaveNoTypoNoise() {
        val result = labels("zq")
        assertTrue(result.isEmpty())
    }

    @Test
    fun typoRequiresFirstLetter() {
        assertTrue(labels("xouda").isEmpty())
    }

    @Test
    fun synonymFindsSetting() {
        assertEquals("Display", top("brightness"))
    }

    @Test
    fun emptyQueryShowsOnlyPins() {
        assertTrue(engine.search("", 10).isEmpty())
        engine.all().first { it.label == "Maps" }.pinned = true
        assertEquals("Maps", engine.search("", 10).first().item.label)
    }

    @Test
    fun prefixDistanceHandlesSwap() {
        val scorer = Scorer()
        assertEquals(1, scorer.prefixDistance("gmial", "gmail", 2))
        assertEquals(1, scorer.prefixDistance("gos", "gouda", 1))
    }

    @Test
    fun strictnessOffDisablesTypos() {
        engine.setStrictness(Strictness.OFF)
        assertTrue(!labels("blutooth").contains("Bluetooth") || top("blutooth") == "Bluetooth")
        assertTrue(!labels("gos").contains("Asmaa Gouda"))
    }

    @Test
    fun fuzzRandomQueriesNeverThrow() {
        val random = java.util.Random(7)
        val alphabet = "abcdefghijklmnopqrstuvwxyz أبتثجحخدذرزسشصضطظعغفقكلمنهوي0123456789-+*/%!()".toCharArray()
        repeat(2000) {
            val length = random.nextInt(12)
            val query = String(CharArray(length) { alphabet[random.nextInt(alphabet.size)] })
            engine.search(query, 8)
        }
    }

    @Test
    fun keystrokeSearchIsFastOnFiveThousandItems() {
        val many = ArrayList<SearchItem>()
        val random = java.util.Random(1)
        val syllables = listOf("ka", "lo", "mi", "ra", "su", "ten", "bo", "zan", "el", "dar", "fi", "gu")
        repeat(5000) {
            val name = (0 until 2 + random.nextInt(2)).joinToString(" ") {
                (0 until 2 + random.nextInt(2)).joinToString("") { syllables[random.nextInt(syllables.size)] }
            }
            many.add(SearchItem("k$it", ItemKind.CONTACT, name))
        }
        engine.replaceItems(many)
        repeat(200) { engine.search("kalo", 8) }
        val start = System.nanoTime()
        val queries = listOf("k", "ka", "kal", "kalo", "kalom", "kalomi", "dra", "sut", "zanel", "gufi")
        repeat(10) { for (q in queries) engine.search(q, 8) }
        val perQueryMs = (System.nanoTime() - start) / 1_000_000.0 / 100
        assertTrue("per query $perQueryMs ms", perQueryMs < 20.0)
    }
}

package app.tacit.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CalculatorTest {

    private val fixed = ZonedDateTime.of(2026, 10, 8, 12, 0, 0, 0, ZoneId.of("UTC"))
    private val calc = Calculator(clock = { fixed })

    private fun value(input: String): String? = calc.evaluate(input)?.copyValue

    @Test
    fun arithmeticCases() {
        val cases = mapOf(
            "1+1" to "2", "2*3+4" to "10", "2+3*4" to "14", "(2+3)*4" to "20", "10/4" to "2.5",
            "2^10" to "1024", "-5+3" to "-2", "5!" to "120", "10 mod 3" to "1", "sqrt 16" to "4",
            "sqrt(2)*sqrt(2)" to "2", "1e3+1" to "1001", "0.1+0.2" to "0.3", "2(3+4)" to "14",
            "abs(-7)" to "7", "round 2.5" to "3", "floor 2.9" to "2", "ceil 2.1" to "3",
            "2^-1" to "0.5", "100/3*3" to "100", "7 - -2" to "9", "3!" to "6"
        )
        for ((input, expected) in cases) assertEquals(input, expected, value(input))
    }

    @Test
    fun naturalLanguage() {
        val cases = mapOf(
            "5 plus 3 times 2" to "11", "15% of 340" to "51", "340 + 15%" to "391", "200 - 10%" to "180",
            "half of 90" to "45", "twice 12" to "24", "10 divided by 4" to "2.5", "3 squared" to "9",
            "2 to the power of 8" to "256", "6 x 7" to "42", "12 × 3" to "36", "9 ÷ 3" to "3",
            "50 is what % of 200" to "25"
        )
        for ((input, expected) in cases) assertEquals(input, expected, value(input))
    }

    @Test
    fun arabicDigits() {
        assertEquals("5", value("٢+٣"))
        assertEquals("12", value("۴*۳"))
    }

    @Test
    fun thousandsSeparatorsAndDecimalComma() {
        assertEquals("2000", value("1,000*2"))
        assertEquals("3", value("1,5*2"))
    }

    @Test
    fun units() {
        assertEquals("3.1068559612", value("5 km in miles"))
        assertEquals("22.2222222222", value("72f to c"))
        assertEquals("3000", value("3gb in mb"))
        assertEquals("212", value("100 c to f"))
        assertEquals("100", value("1 m in cm"))
        assertEquals("2.2046226218", value("1 kg in lb"))
        assertEquals("60", value("1 hour in minutes"))
        assertEquals("273.15", value("0 c in k"))
        assertEquals("4200", value("1 feddan in m2"))
        assertNull(value("5 km in kg"))
    }

    @Test
    fun bases() {
        assertEquals("0xFF", value("255 in hex"))
        assertEquals("0b11111111", value("0xff in binary"))
        assertEquals("255", value("0b11111111 in decimal"))
        assertEquals("0o10", value("8 in octal"))
    }

    @Test
    fun timeZones() {
        assertEquals("16:00", value("time in dubai"))
        assertEquals("21:00", value("time in tokyo"))
        assertEquals("22:00", value("5pm dubai in tokyo"))
        assertNotNull(value("time in cairo"))
        assertEquals("08:00", value("time in new york"))
        assertNull(value("time in atlantis"))
    }

    @Test
    fun dates() {
        assertEquals("78", value("days until dec 25"))
        assertEquals("2026-10-30", value("next friday + 3 weeks"))
        assertEquals("1011", value("today - 2024-01-01"))
        assertEquals("2026-10-09", value("tomorrow + 0 days"))
        assertEquals("7", value("days since oct 1"))
    }

    @Test
    fun malformedNeverCrashes() {
        val inputs = listOf("", "(", ")", "1/0", "+", "*5", "2^^3", "sqrt(-1)", "1000!", "abc", "5 km in", "((((", "1..2",
            "e^", "%%%", "10 mod 0", "days until", "time in", "0x", "9".repeat(500), "1e999999", "π*")
        for (input in inputs) calc.evaluate(input)
    }

    @Test
    fun plainNumbersAndWordsAreNotResults() {
        assertNull(value("42"))
        assertNull(value("whatsapp"))
        assertFalse(calc.looksLikeMath("youtube"))
        assertTrue(calc.looksLikeMath("2+2"))
    }

    @Test
    fun displayIncludesUnit() {
        val result = calc.evaluate("5 km in mi")
        assertNotNull(result)
        assertTrue(result!!.display.endsWith("mi"))
    }

    @Test
    fun fuzzCalculator() {
        val random = java.util.Random(3)
        val alphabet = "0123456789+-*/^%!(). abcdeimnoprstx".toCharArray()
        repeat(3000) {
            val text = String(CharArray(random.nextInt(16)) { alphabet[random.nextInt(alphabet.size)] })
            calc.evaluate(text)
        }
    }
}

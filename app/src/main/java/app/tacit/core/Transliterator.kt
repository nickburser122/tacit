package app.tacit.core

object ArabicLatin {

    private val table = mapOf(
        'ا' to "a", 'ب' to "b", 'ت' to "t", 'ث' to "th", 'ج' to "g", 'ح' to "h",
        'خ' to "kh", 'د' to "d", 'ذ' to "z", 'ر' to "r", 'ز' to "z", 'س' to "s",
        'ش' to "sh", 'ص' to "s", 'ض' to "d", 'ط' to "t", 'ظ' to "z", 'ع' to "a",
        'غ' to "gh", 'ف' to "f", 'ق' to "q", 'ك' to "k", 'ل' to "l", 'م' to "m",
        'ن' to "n", 'ه' to "a", 'و' to "o", 'ي' to "y", 'ء' to "a", 'پ' to "p",
        'چ' to "ch", 'گ' to "g", 'ڤ' to "v"
    )

    fun containsArabic(text: String): Boolean = text.any { it in '\u0600'..'\u06FF' }

    fun toLatin(normalizedArabic: String): String {
        val builder = StringBuilder(normalizedArabic.length * 2)
        for (ch in normalizedArabic) builder.append(table[ch] ?: ch.toString())
        return builder.toString()
    }

    fun variants(normalized: String): List<String> {
        if (!containsArabic(normalized)) return emptyList()
        val basic = toLatin(normalized)
        val collapsed = basic.replace("aa", "a").replace("yy", "y")
        val egyptian = basic
        val levantine = basic.replace('g', 'j')
        val softQ = basic.replace('q', 'k')
        val vowelled = collapsed.replace("my", "mi").replace("ya", "ia").replace("oo", "ou")
        return listOf(basic, collapsed, egyptian, levantine, softQ, vowelled).distinct()
    }
}

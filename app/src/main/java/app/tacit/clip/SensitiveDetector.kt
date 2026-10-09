package app.tacit.clip

import java.math.BigInteger

enum class Sensitivity { NONE, CARD, IBAN, OTP, TOKEN, PRIVATE_KEY, PASSWORD_FLAG }

object SensitiveDetector {

    private val cardCandidate = Regex("\\b(?:\\d[ -]?){13,19}\\b")
    private val ibanCandidate = Regex("\\b[A-Z]{2}\\d{2}(?: ?[A-Z0-9]){11,30}\\b")
    private val otpPattern = Regex("^\\s*\\d{4,8}\\s*$")
    private val tokenPattern = Regex("^[A-Za-z0-9_\\-+/=.]{24,}$")

    fun classify(text: String, flaggedByOs: Boolean = false): Sensitivity {
        if (flaggedByOs) return Sensitivity.PASSWORD_FLAG
        if (text.contains("-----BEGIN") && text.contains("PRIVATE KEY")) return Sensitivity.PRIVATE_KEY
        if (otpPattern.matches(text)) return Sensitivity.OTP
        for (match in ibanCandidate.findAll(text.uppercase())) {
            if (ibanValid(match.value)) return Sensitivity.IBAN
        }
        for (match in cardCandidate.findAll(text)) {
            val digits = match.value.filter { it.isDigit() }
            if (digits.length in 13..19 && luhn(digits)) return Sensitivity.CARD
        }
        val trimmed = text.trim()
        if (tokenPattern.matches(trimmed) && entropyPerChar(trimmed) >= 3.5 && hasMixedClasses(trimmed)) return Sensitivity.TOKEN
        return Sensitivity.NONE
    }

    fun luhn(digits: String): Boolean {
        var sum = 0
        var double = false
        for (i in digits.indices.reversed()) {
            var d = digits[i] - '0'
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }

    fun ibanValid(raw: String): Boolean {
        val iban = raw.replace(" ", "")
        if (iban.length < 15 || iban.length > 34) return false
        val rearranged = iban.substring(4) + iban.substring(0, 4)
        val numeric = StringBuilder()
        for (ch in rearranged) {
            when {
                ch.isDigit() -> numeric.append(ch)
                ch in 'A'..'Z' -> numeric.append(ch - 'A' + 10)
                else -> return false
            }
        }
        return BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)) == BigInteger.ONE
    }

    fun entropyPerChar(text: String): Double {
        if (text.isEmpty()) return 0.0
        val counts = HashMap<Char, Int>()
        for (ch in text) counts[ch] = (counts[ch] ?: 0) + 1
        var entropy = 0.0
        for (count in counts.values) {
            val p = count.toDouble() / text.length
            entropy -= p * Math.log(p) / Math.log(2.0)
        }
        return entropy
    }

    private fun hasMixedClasses(text: String): Boolean {
        val classes = listOf(text.any { it.isUpperCase() }, text.any { it.isLowerCase() }, text.any { it.isDigit() })
        return classes.count { it } >= 2 && !text.contains('/') || text.count { it.isDigit() } > 4
    }
}

package app.tacit.calc

import app.tacit.core.Normalizer
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class CalcResult(val display: String, val copyValue: String, val expression: String)

class Calculator(
    var precision: Int = 20,
    var displayScale: Int = 10,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() }
) {

    private val context get() = MathContext(precision, RoundingMode.HALF_EVEN)

    private val conversion = Regex("^(-?[0-9.,]+(?:e-?[0-9]+)?)\\s*([a-z°\"'/ 0-9]+?)\\s+(?:in|to|as|into|=|->)\\s+([a-z°\"'/ 0-9]+)$")
    private val baseConversion = Regex("^(.+?)\\s+(?:in|to|as)\\s+(hex|hexadecimal|binary|bin|octal|oct|decimal|dec)$")
    private val percentOf = Regex("^([0-9.]+)\\s*%\\s*of\\s+(.+)$")
    private val whatPercent = Regex("^(.+?)\\s+is\\s+what\\s*%\\s*of\\s+(.+)$")
    private val timeIn = Regex("^(?:time|now|what time is it)\\s+(?:in|at)\\s+(.+)$")
    private val timeConvert = Regex("^([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?\\s+([a-z ]+?)\\s+(?:in|to)\\s+([a-z ]+)$")
    private val daysUntil = Regex("^days?\\s+(?:until|till|to)\\s+(.+)$")
    private val daysSince = Regex("^days?\\s+since\\s+(.+)$")
    private val dateArithmetic = Regex("^(today|tomorrow|yesterday|now|next [a-z]+|last [a-z]+|[0-9]{4}-[0-9]{2}-[0-9]{2})\\s*([+-])\\s*([0-9]+)\\s*(days?|weeks?|months?|years?|d|w|m|y)$")
    private val dateDifference = Regex("^([0-9]{4}-[0-9]{2}-[0-9]{2}|today)\\s*-\\s*([0-9]{4}-[0-9]{2}-[0-9]{2}|today)$")

    private val wordReplacements = listOf(
        "multiplied by" to "*", "divided by" to "/", "to the power of" to "^", "squared" to "^2",
        "cubed" to "^3", "plus" to "+", "minus" to "-", "times" to "*", "over" to "/",
        "x" to "*", "×" to "*", "÷" to "/", "mod" to " mod ", "square root of" to "sqrt",
        "root of" to "sqrt", "half of" to "0.5*", "quarter of" to "0.25*", "third of" to "(1/3)*",
        "double" to "2*", "twice" to "2*", "triple" to "3*", "of" to "*"
    ).map { (word, symbol) -> Regex("(?<=[\\s\\d)])${Regex.escape(word)}(?=[\\s\\d(])") to symbol }

    fun evaluate(raw: String): CalcResult? {
        val text = prepare(raw)
        if (text.isEmpty()) return null
        return try {
            tryTime(text) ?: tryDates(text) ?: tryBase(text) ?: tryConversion(text) ?: tryPercent(text) ?: tryArithmetic(text)
        } catch (error: CalcException) {
            null
        } catch (error: ArithmeticException) {
            null
        } catch (error: NumberFormatException) {
            null
        } catch (error: java.time.DateTimeException) {
            null
        }
    }

    fun looksLikeMath(raw: String): Boolean {
        val text = prepare(raw)
        if (text.isEmpty() || text.length > 120) return false
        if (text.any { it.isDigit() } && text.any { it in "+-*/^%!()=" || it.isLetter() }) return true
        return text.startsWith("time ") || text.startsWith("days ") || text.startsWith("sqrt")
    }

    private fun prepare(raw: String): String {
        var text = Normalizer.normalize(raw).trim().removePrefix("=").trim().removeSuffix("=").trim()
        text = text.replace('٫', '.').replace("−", "-").replace("**", "^")
        text = text.replace(Regex("(?<=\\d),(?=\\d{3}(\\D|$))"), "")
        text = text.replace(Regex("(?<=\\d),(?=\\d)"), ".")
        return text.replace(Regex("\\s+"), " ")
    }

    private fun tryArithmetic(text: String): CalcResult? {
        var expression = " $text "
        for ((pattern, symbol) in wordReplacements) {
            expression = expression.replace(pattern, symbol)
        }
        expression = expression.replace(Regex("\\s+"), " ").trim()
        if (expression.none { it.isDigit() } && !expression.contains("pi")) return null
        if (expression.all { it.isDigit() || it == '.' || it == ' ' }) return null
        val value = Expression(expression, context).evaluate()
        val shown = format(value)
        return CalcResult(shown, shown, text)
    }

    private fun tryPercent(text: String): CalcResult? {
        percentOf.matchEntire(text)?.let { match ->
            val percent = match.groupValues[1].toBigDecimal()
            val base = Expression(match.groupValues[2], context).evaluate()
            val value = base.multiply(percent, context).divide(BigDecimal(100), context)
            return result(value, text)
        }
        whatPercent.matchEntire(text)?.let { match ->
            val part = Expression(match.groupValues[1], context).evaluate()
            val whole = Expression(match.groupValues[2], context).evaluate()
            if (whole.signum() == 0) throw CalcException("Division by zero")
            val value = part.multiply(BigDecimal(100)).divide(whole, context)
            val shown = format(value) + "%"
            return CalcResult(shown, format(value), text)
        }
        return null
    }

    private fun tryConversion(text: String): CalcResult? {
        val match = conversion.matchEntire(text) ?: return null
        val amount = match.groupValues[1].replace(",", "").toBigDecimal()
        val from = Units.find(match.groupValues[2]) ?: return null
        val to = Units.find(match.groupValues[3]) ?: return null
        val value = Units.convert(amount, from, to, context)
        val shown = format(value)
        return CalcResult("$shown ${to.symbol}", shown, text)
    }

    private fun tryBase(text: String): CalcResult? {
        val match = baseConversion.matchEntire(text) ?: return null
        val number = parseInteger(match.groupValues[1].trim()) ?: return null
        val target = match.groupValues[2]
        val shown = when (target) {
            "hex", "hexadecimal" -> "0x" + number.toString(16).uppercase()
            "binary", "bin" -> "0b" + number.toString(2)
            "octal", "oct" -> "0o" + number.toString(8)
            else -> number.toString()
        }
        return CalcResult(shown, shown, text)
    }

    private fun parseInteger(value: String): BigInteger? = when {
        value.startsWith("0x") -> value.substring(2).toBigIntegerOrNull(16)
        value.startsWith("0b") -> value.substring(2).toBigIntegerOrNull(2)
        value.startsWith("0o") -> value.substring(2).toBigIntegerOrNull(8)
        else -> {
            val evaluated = Expression(value, context).evaluate()
            if (evaluated.stripTrailingZeros().scale() > 0) null else evaluated.toBigIntegerExact()
        }
    }

    private fun tryTime(text: String): CalcResult? {
        timeIn.matchEntire(text)?.let { match ->
            val zone = zoneFor(match.groupValues[1]) ?: return null
            val now = clock().withZoneSameInstant(zone)
            val shown = now.format(DateTimeFormatter.ofPattern("HH:mm, EEE d MMM", Locale.getDefault()))
            return CalcResult(shown, now.format(DateTimeFormatter.ofPattern("HH:mm")), text)
        }
        timeConvert.matchEntire(text)?.let { match ->
            val fromZone = zoneFor(match.groupValues[4]) ?: return null
            val toZone = zoneFor(match.groupValues[5]) ?: return null
            var hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
            when (match.groupValues[3]) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            if (hour > 23 || minute > 59) return null
            val source = ZonedDateTime.of(clock().withZoneSameInstant(fromZone).toLocalDate(), LocalTime.of(hour, minute), fromZone)
            val target = source.withZoneSameInstant(toZone)
            val shown = target.format(DateTimeFormatter.ofPattern("HH:mm, EEE", Locale.getDefault()))
            return CalcResult(shown, target.format(DateTimeFormatter.ofPattern("HH:mm")), text)
        }
        return null
    }

    private fun zoneFor(name: String): ZoneId? {
        val key = name.trim()
        Cities.zones[key]?.let { return ZoneId.of(it) }
        if (!key.contains('/')) return null
        return try {
            ZoneId.of(key.split('/').joinToString("/") { part -> part.split('_', ' ').joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } } })
        } catch (error: java.time.DateTimeException) {
            null
        }
    }

    private fun tryDates(text: String): CalcResult? {
        val today = clock().toLocalDate()
        daysUntil.matchEntire(text)?.let { match ->
            val target = parseDate(match.groupValues[1], today, forward = true) ?: return null
            val days = ChronoUnit.DAYS.between(today, target)
            return CalcResult("$days days", days.toString(), text)
        }
        daysSince.matchEntire(text)?.let { match ->
            val target = parseDate(match.groupValues[1], today, forward = false) ?: return null
            val days = ChronoUnit.DAYS.between(target, today)
            return CalcResult("$days days", days.toString(), text)
        }
        dateDifference.matchEntire(text)?.let { match ->
            val first = parseDate(match.groupValues[1], today, true) ?: return null
            val second = parseDate(match.groupValues[2], today, true) ?: return null
            val days = ChronoUnit.DAYS.between(second, first)
            return CalcResult("$days days", days.toString(), text)
        }
        dateArithmetic.matchEntire(text)?.let { match ->
            val start = parseDate(match.groupValues[1], today, forward = true) ?: return null
            val sign = if (match.groupValues[2] == "-") -1L else 1L
            val amount = match.groupValues[3].toLong() * sign
            val unit = match.groupValues[4]
            val result = when {
                unit.startsWith("d") -> start.plusDays(amount)
                unit.startsWith("w") -> start.plusWeeks(amount)
                unit.startsWith("m") -> start.plusMonths(amount)
                else -> start.plusYears(amount)
            }
            val shown = result.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault()))
            return CalcResult(shown, result.toString(), text)
        }
        return null
    }

    private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val weekdays = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

    fun parseDate(value: String, today: LocalDate, forward: Boolean): LocalDate? {
        val text = value.trim()
        when (text) {
            "today", "now" -> return today
            "tomorrow" -> return today.plusDays(1)
            "yesterday" -> return today.minusDays(1)
            "christmas", "xmas" -> return rollYear(LocalDate.of(today.year, 12, 25), today, forward)
            "new year", "new years" -> return rollYear(LocalDate.of(today.year, 1, 1), today, forward)
        }
        Regex("^([0-9]{4})-([0-9]{2})-([0-9]{2})$").matchEntire(text)?.let {
            return LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
        }
        Regex("^(next|last|this)\\s+([a-z]+)$").matchEntire(text)?.let {
            val day = weekdayOf(it.groupValues[2]) ?: return null
            return when (it.groupValues[1]) {
                "next" -> today.with(TemporalAdjusters.next(day))
                "last" -> today.with(TemporalAdjusters.previous(day))
                else -> today.with(TemporalAdjusters.nextOrSame(day))
            }
        }
        Regex("^([a-z]+)\\s+([0-9]{1,2})(?:\\s+([0-9]{4}))?$").matchEntire(text)?.let {
            val month = monthOf(it.groupValues[1]) ?: return null
            val date = LocalDate.of(it.groupValues[3].ifEmpty { today.year.toString() }.toInt(), month, it.groupValues[2].toInt())
            return if (it.groupValues[3].isEmpty()) rollYear(date, today, forward) else date
        }
        Regex("^([0-9]{1,2})\\s+([a-z]+)(?:\\s+([0-9]{4}))?$").matchEntire(text)?.let {
            val month = monthOf(it.groupValues[2]) ?: return null
            val date = LocalDate.of(it.groupValues[3].ifEmpty { today.year.toString() }.toInt(), month, it.groupValues[1].toInt())
            return if (it.groupValues[3].isEmpty()) rollYear(date, today, forward) else date
        }
        weekdayOf(text)?.let { return today.with(if (forward) TemporalAdjusters.next(it) else TemporalAdjusters.previous(it)) }
        return null
    }

    private fun rollYear(date: LocalDate, today: LocalDate, forward: Boolean): LocalDate = when {
        forward && date.isBefore(today) -> date.plusYears(1)
        !forward && date.isAfter(today) -> date.minusYears(1)
        else -> date
    }

    private fun monthOf(name: String): Int? {
        val index = months.indexOfFirst { name.startsWith(it) }
        return if (index >= 0) index + 1 else null
    }

    private fun weekdayOf(name: String): DayOfWeek? {
        val index = weekdays.indexOfFirst { name.startsWith(it) }
        return if (index >= 0) DayOfWeek.of(index + 1) else null
    }

    private fun result(value: BigDecimal, text: String): CalcResult {
        val shown = format(value)
        return CalcResult(shown, shown, text)
    }

    fun format(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val magnitude = value.precision() - value.scale()
        if (magnitude > 15 || magnitude < -8) return value.round(MathContext(10)).stripTrailingZeros().toString()
        val plain = value.setScale(displayScale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return if (plain == "-0" || plain == "0.0") "0" else plain
    }
}

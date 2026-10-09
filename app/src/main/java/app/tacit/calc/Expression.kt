package app.tacit.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class CalcException(message: String) : Exception(message)

class Expression(private val input: String, private val context: MathContext) {

    private var position = 0

    private class Value(val number: BigDecimal, val percent: Boolean = false)

    fun evaluate(): BigDecimal {
        val result = parseAdditive()
        skipSpaces()
        if (position != input.length) throw CalcException("Unexpected '${input[position]}'")
        return result.number
    }

    private fun parseAdditive(): Value {
        var left = parseMultiplicative()
        while (true) {
            skipSpaces()
            val op = peek()
            if (op != '+' && op != '-') return left
            position++
            val right = parseMultiplicative()
            val delta = if (right.percent) left.number.multiply(right.number, context) else right.number
            left = Value(if (op == '+') left.number.add(delta, context) else left.number.subtract(delta, context))
        }
    }

    private fun parseMultiplicative(): Value {
        var left = parseUnary()
        while (true) {
            skipSpaces()
            when {
                peek() == '*' -> {
                    position++
                    left = Value(left.number.multiply(parseUnary().number, context))
                }
                peek() == '/' -> {
                    position++
                    val divisor = parseUnary().number
                    if (divisor.signum() == 0) throw CalcException("Division by zero")
                    left = Value(left.number.divide(divisor, context))
                }
                matchWord("mod") -> {
                    val divisor = parseUnary().number
                    if (divisor.signum() == 0) throw CalcException("Division by zero")
                    left = Value(left.number.remainder(divisor, context))
                }
                startsImplicitProduct() -> left = Value(left.number.multiply(parseUnary().number, context))
                else -> return left
            }
        }
    }

    private fun startsImplicitProduct(): Boolean {
        val c = peek()
        return c == '(' || (c != null && c.isLetter() && !lookingAtWord("mod"))
    }

    private fun parseUnary(): Value {
        skipSpaces()
        if (peek() == '-') {
            position++
            val inner = parseUnary()
            return Value(inner.number.negate(), inner.percent)
        }
        if (peek() == '+') {
            position++
            return parseUnary()
        }
        return parsePower()
    }

    private fun parsePower(): Value {
        val base = parsePostfix()
        skipSpaces()
        if (peek() == '^') {
            position++
            val exponent = parseUnary().number
            return Value(power(base.number, exponent))
        }
        return base
    }

    private fun parsePostfix(): Value {
        var value = parsePrimary()
        while (true) {
            skipSpaces()
            when (peek()) {
                '!' -> {
                    position++
                    value = Value(factorial(value.number))
                }
                '%' -> {
                    position++
                    value = Value(value.number.divide(BigDecimal(100), context), percent = true)
                }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Value {
        skipSpaces()
        val c = peek() ?: throw CalcException("Incomplete expression")
        if (c == '(') {
            position++
            val inner = parseAdditive()
            skipSpaces()
            if (peek() == ')') position++
            return Value(inner.number)
        }
        if (c.isDigit() || c == '.') return Value(parseNumber())
        if (c.isLetter()) return Value(parseIdentifier())
        throw CalcException("Unexpected '$c'")
    }

    private fun parseNumber(): BigDecimal {
        val start = position
        while (position < input.length && (input[position].isDigit() || input[position] == '.')) position++
        if (position < input.length && (input[position] == 'e' || input[position] == 'E') &&
            position + 1 < input.length && (input[position + 1].isDigit() || input[position + 1] == '-')) {
            position += 2
            while (position < input.length && input[position].isDigit()) position++
        }
        val text = input.substring(start, position)
        return text.toBigDecimalOrNull() ?: throw CalcException("Bad number '$text'")
    }

    private fun parseIdentifier(): BigDecimal {
        val start = position
        while (position < input.length && input[position].isLetter()) position++
        val name = input.substring(start, position)
        return when (name) {
            "pi", "π" -> BigDecimal(Math.PI, context)
            "e" -> BigDecimal(Math.E, context)
            "tau" -> BigDecimal(Math.PI * 2, context)
            else -> applyFunction(name, parseFunctionArgument())
        }
    }

    private fun parseFunctionArgument(): BigDecimal {
        skipSpaces()
        return if (peek() == '(') {
            position++
            val value = parseAdditive().number
            skipSpaces()
            if (peek() == ')') position++
            value
        } else {
            parseUnary().number
        }
    }

    private fun applyFunction(name: String, argument: BigDecimal): BigDecimal {
        val x = argument.toDouble()
        val result = when (name) {
            "sqrt", "√" -> {
                if (argument.signum() < 0) throw CalcException("Negative root")
                return squareRoot(argument)
            }
            "cbrt" -> Math.cbrt(x)
            "abs" -> return argument.abs()
            "sin" -> Math.sin(Math.toRadians(x))
            "cos" -> Math.cos(Math.toRadians(x))
            "tan" -> Math.tan(Math.toRadians(x))
            "asin" -> Math.toDegrees(Math.asin(x))
            "acos" -> Math.toDegrees(Math.acos(x))
            "atan" -> Math.toDegrees(Math.atan(x))
            "ln" -> Math.log(x)
            "log" -> Math.log10(x)
            "exp" -> Math.exp(x)
            "round" -> return argument.setScale(0, RoundingMode.HALF_UP)
            "floor" -> return argument.setScale(0, RoundingMode.FLOOR)
            "ceil" -> return argument.setScale(0, RoundingMode.CEILING)
            else -> throw CalcException("Unknown '$name'")
        }
        if (result.isNaN() || result.isInfinite()) throw CalcException("Out of range")
        return BigDecimal(result, context)
    }

    private fun squareRoot(value: BigDecimal): BigDecimal {
        if (value.signum() == 0) return BigDecimal.ZERO
        val two = BigDecimal(2)
        var guess = BigDecimal(Math.sqrt(value.toDouble()), context)
        if (guess.signum() == 0) guess = BigDecimal.ONE
        repeat(8) { guess = guess.add(value.divide(guess, context), context).divide(two, context) }
        return guess
    }

    private fun power(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val stripped = exponent.stripTrailingZeros()
        if (stripped.scale() <= 0 && stripped.abs() <= BigDecimal(9999)) {
            val n = stripped.toInt()
            return if (n >= 0) base.pow(n, context) else {
                if (base.signum() == 0) throw CalcException("Division by zero")
                BigDecimal.ONE.divide(base.pow(-n, context), context)
            }
        }
        val result = Math.pow(base.toDouble(), exponent.toDouble())
        if (result.isNaN() || result.isInfinite()) throw CalcException("Out of range")
        return BigDecimal(result, context)
    }

    private fun factorial(value: BigDecimal): BigDecimal {
        val stripped = value.stripTrailingZeros()
        if (stripped.scale() > 0 || value.signum() < 0 || value > BigDecimal(500)) throw CalcException("Bad factorial")
        var result = BigDecimal.ONE
        for (i in 2..value.toInt()) result = result.multiply(BigDecimal(i))
        return result.round(context)
    }

    private fun matchWord(word: String): Boolean {
        if (!lookingAtWord(word)) return false
        position += word.length
        return true
    }

    private fun lookingAtWord(word: String): Boolean {
        if (!input.startsWith(word, position)) return false
        val end = position + word.length
        return end >= input.length || !input[end].isLetter()
    }

    private fun peek(): Char? = if (position < input.length) input[position] else null

    private fun skipSpaces() {
        while (position < input.length && input[position] == ' ') position++
    }
}

package app.tacit.calc

import java.math.BigDecimal
import java.math.MathContext

enum class Dimension { LENGTH, MASS, VOLUME, AREA, SPEED, DATA, TIME, TEMPERATURE }

class UnitDef(val symbol: String, val dimension: Dimension, val factor: BigDecimal, val names: List<String>)

object Units {

    private fun u(symbol: String, dimension: Dimension, factor: String, vararg names: String) =
        UnitDef(symbol, dimension, BigDecimal(factor), listOf(symbol) + names)

    val all: List<UnitDef> = listOf(
        u("mm", Dimension.LENGTH, "0.001", "millimeter", "millimeters", "millimetre", "millimetres"),
        u("cm", Dimension.LENGTH, "0.01", "centimeter", "centimeters", "centimetre", "centimetres"),
        u("m", Dimension.LENGTH, "1", "meter", "meters", "metre", "metres"),
        u("km", Dimension.LENGTH, "1000", "kilometer", "kilometers", "kilometre", "kilometres", "kms"),
        u("in", Dimension.LENGTH, "0.0254", "inch", "inches", "\""),
        u("ft", Dimension.LENGTH, "0.3048", "foot", "feet", "'"),
        u("yd", Dimension.LENGTH, "0.9144", "yard", "yards"),
        u("mi", Dimension.LENGTH, "1609.344", "mile", "miles"),
        u("nmi", Dimension.LENGTH, "1852", "nautical mile", "nautical miles"),
        u("mg", Dimension.MASS, "0.000001", "milligram", "milligrams"),
        u("g", Dimension.MASS, "0.001", "gram", "grams", "gr"),
        u("kg", Dimension.MASS, "1", "kilogram", "kilograms", "kilo", "kilos"),
        u("t", Dimension.MASS, "1000", "tonne", "tonnes", "ton", "tons"),
        u("oz", Dimension.MASS, "0.028349523125", "ounce", "ounces"),
        u("lb", Dimension.MASS, "0.45359237", "lbs", "pound", "pounds"),
        u("st", Dimension.MASS, "6.35029318", "stone", "stones"),
        u("ml", Dimension.VOLUME, "0.001", "milliliter", "milliliters", "millilitre", "millilitres"),
        u("l", Dimension.VOLUME, "1", "liter", "liters", "litre", "litres"),
        u("tsp", Dimension.VOLUME, "0.00492892159375", "teaspoon", "teaspoons"),
        u("tbsp", Dimension.VOLUME, "0.01478676478125", "tablespoon", "tablespoons"),
        u("floz", Dimension.VOLUME, "0.0295735295625", "fl oz", "fluid ounce", "fluid ounces"),
        u("cup", Dimension.VOLUME, "0.2365882365", "cups"),
        u("pt", Dimension.VOLUME, "0.473176473", "pint", "pints"),
        u("qt", Dimension.VOLUME, "0.946352946", "quart", "quarts"),
        u("gal", Dimension.VOLUME, "3.785411784", "gallon", "gallons"),
        u("m2", Dimension.AREA, "1", "sqm", "square meter", "square meters", "square metre"),
        u("km2", Dimension.AREA, "1000000", "square kilometer", "square kilometers"),
        u("ft2", Dimension.AREA, "0.09290304", "sqft", "square foot", "square feet"),
        u("ha", Dimension.AREA, "10000", "hectare", "hectares"),
        u("acre", Dimension.AREA, "4046.8564224", "acres"),
        u("feddan", Dimension.AREA, "4200", "feddans"),
        u("kmh", Dimension.SPEED, "0.2777777777777778", "km/h", "kph", "kilometers per hour"),
        u("mph", Dimension.SPEED, "0.44704", "miles per hour"),
        u("ms", Dimension.SPEED, "1", "m/s", "meters per second"),
        u("kn", Dimension.SPEED, "0.514444", "knot", "knots"),
        u("b", Dimension.DATA, "1", "byte", "bytes"),
        u("kb", Dimension.DATA, "1000", "kilobyte", "kilobytes"),
        u("mb", Dimension.DATA, "1000000", "megabyte", "megabytes"),
        u("gb", Dimension.DATA, "1000000000", "gigabyte", "gigabytes"),
        u("tb", Dimension.DATA, "1000000000000", "terabyte", "terabytes"),
        u("kib", Dimension.DATA, "1024", "kibibyte"),
        u("mib", Dimension.DATA, "1048576", "mebibyte"),
        u("gib", Dimension.DATA, "1073741824", "gibibyte"),
        u("s", Dimension.TIME, "1", "sec", "secs", "second", "seconds"),
        u("min", Dimension.TIME, "60", "mins", "minute", "minutes"),
        u("h", Dimension.TIME, "3600", "hr", "hrs", "hour", "hours"),
        u("d", Dimension.TIME, "86400", "day", "days"),
        u("wk", Dimension.TIME, "604800", "week", "weeks"),
        u("yr", Dimension.TIME, "31557600", "year", "years"),
        u("c", Dimension.TEMPERATURE, "1", "°c", "celsius", "centigrade"),
        u("f", Dimension.TEMPERATURE, "1", "°f", "fahrenheit"),
        u("k", Dimension.TEMPERATURE, "1", "kelvin")
    )

    private val byName: Map<String, UnitDef> = buildMap {
        for (unit in all) for (name in unit.names) putIfAbsent(name.lowercase(), unit)
    }

    fun find(name: String): UnitDef? = byName[name.trim().lowercase()]

    fun convert(value: BigDecimal, from: UnitDef, to: UnitDef, context: MathContext): BigDecimal {
        if (from.dimension != to.dimension) throw CalcException("Incompatible units")
        if (from.dimension == Dimension.TEMPERATURE) return convertTemperature(value, from.symbol, to.symbol, context)
        return value.multiply(from.factor, context).divide(to.factor, context)
    }

    private fun convertTemperature(value: BigDecimal, from: String, to: String, context: MathContext): BigDecimal {
        val nineFifths = BigDecimal("1.8")
        val kelvinOffset = BigDecimal("273.15")
        val celsius = when (from) {
            "c" -> value
            "f" -> value.subtract(BigDecimal(32)).divide(nineFifths, context)
            else -> value.subtract(kelvinOffset)
        }
        return when (to) {
            "c" -> celsius
            "f" -> celsius.multiply(nineFifths, context).add(BigDecimal(32))
            else -> celsius.add(kelvinOffset)
        }
    }
}

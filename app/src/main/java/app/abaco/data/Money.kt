package app.abaco.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCents(cents: Long, currency: String): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    try {
        fmt.currency = Currency.getInstance(currency)
    } catch (_: Exception) {
        // Se queda la moneda de la región si el código no existe.
    }
    fmt.minimumFractionDigits = 2
    fmt.maximumFractionDigits = 2
    return fmt.format(BigDecimal.valueOf(cents, 2))
}

fun centsToInput(cents: Long): String {
    val abs = kotlin.math.abs(cents)
    val major = abs / 100
    val minor = (abs % 100).toInt()
    val text = if (minor == 0) major.toString() else "%d.%02d".format(major, minor)
    return if (cents < 0) "-$text" else text
}

fun parseMajorToCents(raw: String, allowZero: Boolean = false): Long? {
    var text = raw.trim()
        .replace(" ", "")
        .replace("$", "")
        .replace("€", "")
        .replace("£", "")
    if (text.isEmpty()) return if (allowZero) 0 else null
    val comma = text.lastIndexOf(',')
    val dot = text.lastIndexOf('.')
    text = when {
        comma >= 0 && dot >= 0 ->
            if (comma > dot) text.replace(".", "").replace(',', '.') else text.replace(",", "")
        comma >= 0 -> {
            val frac = text.substring(comma + 1)
            if (frac.length <= 2) text.replace(',', '.') else text.replace(",", "")
        }
        else -> text
    }
    val value = text.toBigDecimalOrNull() ?: return null
    if (value < BigDecimal.ZERO) return null
    if (!allowZero && value.compareTo(BigDecimal.ZERO) == 0) return null
    if (value > BigDecimal("1000000000")) return null
    return try {
        value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (_: Exception) {
        null
    }
}

package app.abaco.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class Tx(
    val id: String,
    val kind: String,
    val cents: Long,
    val category: String,
    val note: String = "",
    val date: String,
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val color: String,
    val createdOn: String,
)

@Serializable
data class Denom(
    val id: String,
    val label: String,
    val cents: Long,
    val kind: String,
    val selected: Boolean = true,
    val count: Int = 0,
)

@Serializable
data class CashSave(
    val id: String,
    val name: String,
    val savedAt: String,
    val currency: String,
    val note: String = "",
    val lines: List<Denom> = emptyList(),
    val totalCents: Long = 0,
)

@Serializable
data class AppData(
    val currency: String = "USD",
    val cashCurrency: String = "USD",
    val savingsGoalCents: Long = 50_000,
    val monthNotes: Map<String, String> = emptyMap(),
    val transactions: List<Tx> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val checks: Map<String, List<String>> = emptyMap(),
    val qrText: String = "",
    val qrFg: String = "#1A1814",
    val qrBg: String = "#FFFDF9",
    val qrSize: Int = 280,
    val qrEcc: String = "M",
    val denoms: List<Denom> = emptyList(),
    val cashSaves: List<CashSave> = emptyList(),
)

val CURRENCIES = listOf("USD", "EUR", "MXN", "COP", "ARS", "CLP", "PEN")

val INCOME_CATEGORIES = listOf("Salario", "Freelance", "Inversiones", "Reembolso", "Otro")

val EXPENSE_CATEGORIES = listOf(
    "Vivienda",
    "Comida",
    "Transporte",
    "Salud",
    "Servicios",
    "Ocio",
    "Educación",
    "Ahorro",
    "Otro",
)

val HABIT_COLORS = listOf(
    "pine" to "Pino",
    "clay" to "Arcilla",
    "gold" to "Ámbar",
    "ink" to "Tinta",
)

fun cashTotal(denoms: List<Denom>): Long =
    denoms.filter { it.selected }.sumOf { it.cents * it.count.toLong() }

private fun bill(label: String, cents: Long) = Denom(newId(), label, cents, "bill")

private fun coin(label: String, cents: Long) = Denom(newId(), label, cents, "coin")

fun usdDenoms(): List<Denom> = listOf(
    bill("100", 100_00),
    bill("50", 50_00),
    bill("20", 20_00),
    bill("10", 10_00),
    bill("5", 5_00),
    bill("1", 1_00),
    coin("0.25", 25),
    coin("0.10", 10),
    coin("0.05", 5),
    coin("0.01", 1),
)

fun mxnDenoms(): List<Denom> = listOf(
    bill("1000", 1000_00),
    bill("500", 500_00),
    bill("200", 200_00),
    bill("100", 100_00),
    bill("50", 50_00),
    bill("20", 20_00),
    coin("10", 10_00),
    coin("5", 5_00),
    coin("2", 2_00),
    coin("1", 1_00),
    coin("0.50", 50),
)

fun eurDenoms(): List<Denom> = listOf(
    bill("200", 200_00),
    bill("100", 100_00),
    bill("50", 50_00),
    bill("20", 20_00),
    bill("10", 10_00),
    bill("5", 5_00),
    coin("2", 2_00),
    coin("1", 1_00),
    coin("0.50", 50),
    coin("0.20", 20),
    coin("0.10", 10),
    coin("0.05", 5),
)

fun presetDenoms(code: String): List<Denom> = when (code) {
    "MXN" -> mxnDenoms()
    "EUR" -> eurDenoms()
    else -> usdDenoms()
}

fun seedData(): AppData {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    fun on(day: Int): String? {
        if (day > today.dayOfMonth) return null
        return month.atDay(day).toString()
    }

    val txs = mutableListOf<Tx>()
    fun push(day: Int, kind: String, cents: Long, category: String, note: String) {
        val date = on(day) ?: return
        txs += Tx(newId(), kind, cents, category, note, date)
    }
    push(1, "income", 2850_00, "Salario", "Nómina del mes")
    push(minOf(5, today.dayOfMonth), "income", 420_00, "Freelance", "Proyecto corto")
    push(2, "expense", 1100_00, "Vivienda", "Alquiler")
    push(3, "expense", 86_40, "Comida", "Mercado semanal")
    push(4, "expense", 42_00, "Transporte", "Gasolina")
    push(6, "expense", 64_15, "Servicios", "Internet")
    push(7, "expense", 28_00, "Ocio", "Cine")
    if (today.dayOfMonth >= 8) push(today.dayOfMonth, "expense", 18_50, "Comida", "Café y pan")
    if (txs.none { it.kind == "expense" }) {
        txs += Tx(newId(), "expense", 42_00, "Comida", "Mercado", today.toString())
    }

    fun daysBack(count: Int, skip: Set<Int> = emptySet()): List<String> =
        (0..count).filter { it !in skip }.map { today.minusDays(it.toLong()).toString() }

    return AppData(
        currency = "USD",
        cashCurrency = "USD",
        savingsGoalCents = 500_00,
        monthNotes = mapOf(
            month.toString() to "El alquiler sale al inicio. La meta de ahorro queda después de servicios.",
        ),
        transactions = txs.sortedByDescending { it.date },
        habits = listOf(
            Habit("habit-caminar", "Caminar 20 min", "pine", today.minusDays(12).toString()),
            Habit("habit-leer", "Leer 15 min", "gold", today.minusDays(12).toString()),
            Habit("habit-pantallas", "Sin pantallas al dormir", "clay", today.minusDays(8).toString()),
        ),
        checks = mapOf(
            "habit-caminar" to daysBack(7, setOf(3)),
            "habit-leer" to daysBack(5, setOf(2)),
            "habit-pantallas" to daysBack(3),
        ),
        qrText = "Ábaco",
        denoms = usdDenoms(),
    )
}

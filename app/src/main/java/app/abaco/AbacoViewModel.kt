package app.abaco

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.abaco.data.AbacoRepository
import app.abaco.data.AppData
import app.abaco.data.CashSave
import app.abaco.data.Denom
import app.abaco.data.Habit
import app.abaco.data.Tx
import app.abaco.data.cashTotal
import app.abaco.data.newId
import app.abaco.data.presetDenoms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime

class AbacoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AbacoRepository(app)
    private val _state = MutableStateFlow(repo.load())
    val state = _state.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()
    private val mutex = Mutex()
    private var persistJob: Job? = null

    init {
        if (!repo.exists()) schedulePersist()
    }

    fun message(text: String) {
        _messages.tryEmit(text)
    }

    private fun update(block: (AppData) -> AppData) {
        _state.value = block(_state.value)
        schedulePersist()
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(120)
            mutex.withLock {
                val latest = _state.value
                withContext(Dispatchers.IO) { repo.save(latest) }
            }
        }
    }

    override fun onCleared() {
        persistJob?.cancel()
        runBlocking {
            mutex.withLock {
                withContext(Dispatchers.IO) { repo.save(_state.value) }
            }
        }
    }

    fun setCurrency(code: String) = update { it.copy(currency = code) }

    fun setCashCurrency(code: String) = update { it.copy(cashCurrency = code) }

    fun setSavingsGoal(cents: Long) = update { it.copy(savingsGoalCents = cents.coerceAtLeast(0)) }

    fun setMonthNote(month: String, note: String) = update {
        it.copy(monthNotes = it.monthNotes + (month to note.take(1000)))
    }

    fun addTx(kind: String, cents: Long, category: String, note: String, date: String) = update {
        val tx = Tx(newId(), kind, cents, category.trim(), note.trim(), date)
        it.copy(transactions = listOf(tx) + it.transactions)
    }

    fun updateTx(id: String, kind: String, cents: Long, category: String, note: String, date: String) = update {
        it.copy(transactions = it.transactions.map { tx ->
            if (tx.id == id) tx.copy(kind = kind, cents = cents, category = category.trim(), note = note.trim(), date = date) else tx
        })
    }

    fun deleteTx(id: String) = update { data ->
        data.copy(transactions = data.transactions.filter { it.id != id })
    }

    fun addHabit(name: String, color: String) = update {
        val habit = Habit(newId(), name.trim().take(40), color, LocalDate.now().toString())
        it.copy(habits = it.habits + habit)
    }

    fun updateHabit(id: String, name: String, color: String) = update {
        it.copy(habits = it.habits.map { habit ->
            if (habit.id == id) habit.copy(name = name.trim().take(40), color = color) else habit
        })
    }

    fun deleteHabit(id: String) = update {
        it.copy(habits = it.habits.filter { habit -> habit.id != id }, checks = it.checks - id)
    }

    fun toggleCheck(habitId: String, date: String) = update {
        val current = it.checks[habitId].orEmpty().toMutableSet()
        if (!current.add(date)) current.remove(date)
        it.copy(checks = it.checks + (habitId to current.sorted()))
    }

    fun setQrText(value: String) = update { it.copy(qrText = value.take(2000)) }

    fun setQrFg(value: String) = update { it.copy(qrFg = value) }

    fun setQrBg(value: String) = update { it.copy(qrBg = value) }

    fun setQrSize(value: Int) = update { it.copy(qrSize = value.coerceIn(160, 480)) }

    fun setQrEcc(value: String) = update { it.copy(qrEcc = value) }

    fun applyCashPreset(code: String) = update {
        it.copy(cashCurrency = code, denoms = presetDenoms(code))
    }

    fun addDenom(label: String, cents: Long, kind: String) = update {
        val name = label.trim().ifEmpty { centsLabel(cents) }
        it.copy(denoms = it.denoms + Denom(newId(), name, cents, kind))
    }

    fun updateDenom(id: String, label: String, cents: Long, kind: String) = update {
        it.copy(denoms = it.denoms.map { denom ->
            if (denom.id == id) {
                denom.copy(label = label.trim().ifEmpty { centsLabel(cents) }, cents = cents, kind = kind)
            } else {
                denom
            }
        })
    }

    fun deleteDenom(id: String) = update { data -> data.copy(denoms = data.denoms.filter { it.id != id }) }

    fun deleteDenoms(ids: Set<String>) = update { data ->
        data.copy(denoms = data.denoms.filter { it.id !in ids })
    }

    fun setCount(id: String, count: Int) = update {
        it.copy(denoms = it.denoms.map { denom ->
            if (denom.id == id) denom.copy(count = count.coerceIn(0, 9999)) else denom
        })
    }

    fun toggleDenom(id: String) = update {
        it.copy(denoms = it.denoms.map { denom ->
            if (denom.id == id) denom.copy(selected = !denom.selected) else denom
        })
    }

    fun setDenomSelected(id: String, selected: Boolean) = update {
        it.copy(denoms = it.denoms.map { denom ->
            if (denom.id == id) denom.copy(selected = selected) else denom
        })
    }

    fun clearCounts() = update { it.copy(denoms = it.denoms.map { denom -> denom.copy(count = 0) }) }

    fun saveCash(name: String, note: String) = update {
        val save = CashSave(
            id = newId(),
            name = name.trim().ifEmpty { "Conteo" },
            savedAt = OffsetDateTime.now().toString(),
            currency = it.cashCurrency,
            note = note.trim().take(300),
            lines = it.denoms,
            totalCents = cashTotal(it.denoms),
        )
        it.copy(cashSaves = listOf(save) + it.cashSaves)
    }

    fun overwriteCash(id: String) = update {
        val total = cashTotal(it.denoms)
        it.copy(cashSaves = it.cashSaves.map { save ->
            if (save.id == id) {
                save.copy(
                    lines = it.denoms,
                    totalCents = total,
                    currency = it.cashCurrency,
                    savedAt = OffsetDateTime.now().toString(),
                )
            } else {
                save
            }
        })
    }

    fun deleteCash(id: String) = update { data -> data.copy(cashSaves = data.cashSaves.filter { it.id != id }) }

    fun deleteCashMany(ids: Set<String>) = update { data ->
        data.copy(cashSaves = data.cashSaves.filter { it.id !in ids })
    }

    fun loadDenoms(lines: List<Denom>, currency: String) = update {
        it.copy(denoms = lines, cashCurrency = currency)
    }

    fun exportJson(): String = repo.encode(_state.value)

    fun importJson(text: String): Boolean {
        return try {
            _state.value = repo.decode(text)
            schedulePersist()
            message("Copia restaurada en este teléfono")
            true
        } catch (_: Exception) {
            message("Ese archivo no es una copia de Ábaco")
            false
        }
    }

    private fun centsLabel(cents: Long): String {
        val major = cents / 100.0
        return if (cents % 100L == 0L) (cents / 100).toString() else String.format("%.2f", major)
    }
}

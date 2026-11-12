package app.abaco.ui.budget

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abaco.AbacoViewModel
import app.abaco.data.AppData
import app.abaco.data.CURRENCIES
import app.abaco.data.EXPENSE_CATEGORIES
import app.abaco.data.INCOME_CATEGORIES
import app.abaco.data.Tx
import app.abaco.data.centsToInput
import app.abaco.data.formatCents
import app.abaco.data.parseMajorToCents
import app.abaco.export.buildBudgetPdf
import app.abaco.export.savePdf
import app.abaco.export.shareBytes
import app.abaco.ui.AbacoDialog
import app.abaco.ui.theme.Clay
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Line
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Pine
import app.abaco.ui.theme.Surface
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "MX"))

@Composable
fun BudgetScreen(vm: AbacoViewModel, data: AppData) {
    val context = LocalContext.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var editor by remember { mutableStateOf<Tx?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Tx?>(null) }
    val monthKey = month.toString()
    val txs = data.transactions.filter { it.date.startsWith(monthKey) }.sortedByDescending { it.date }
    val income = txs.filter { it.kind == "income" }.sumOf { it.cents }
    val expense = txs.filter { it.kind == "expense" }.sumOf { it.cents }
    val balance = income - expense
    val rows = txs.filter { it.kind == "expense" }
        .groupBy { it.category }
        .map { (category, items) -> category to items.sumOf { it.cents } }
        .sortedByDescending { it.second }
    val history = data.transactions.mapNotNull { tx ->
        runCatching { YearMonth.from(LocalDate.parse(tx.date)) }.getOrNull()
    }.distinct().sortedDescending()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importBackup(context, uri, vm)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior")
            }
            Text(
                text = month.format(monthTitle).replaceFirstChar { it.titlecase(Locale("es", "MX")) },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                onClick = { if (month < YearMonth.now()) month = month.plusMonths(1) },
                enabled = month < YearMonth.now(),
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente")
            }
            CurrencyMenu(data.currency) { vm.setCurrency(it) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Ink)
                .padding(20.dp),
        ) {
            Text("Saldo restante", color = Color(0xFFD4CBBE), fontSize = 13.sp)
            Text(
                text = formatCents(balance, data.currency),
                color = if (balance >= 0) Color(0xFFB7E2CC) else Color(0xFFF2C2B8),
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniStat("Ingresos", formatCents(income, data.currency), Modifier.weight(1f))
                MiniStat("Gastos", formatCents(expense, data.currency), Modifier.weight(1f))
            }
        }

        GoalCard(data, vm, balance)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { creating = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Movimiento", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = {
                val bytes = buildBudgetPdf(data, month)
                val name = "abaco-$monthKey.pdf"
                val saved = savePdf(context, bytes, name)
                vm.message(if (saved) "PDF guardado en Descargas" else "No se pudo guardar el PDF")
            }) {
                Text("PDF")
            }
            IconButton(onClick = {
                val bytes = buildBudgetPdf(data, month)
                shareBytes(context, bytes, "abaco-$monthKey.pdf", "application/pdf", "Compartir presupuesto")
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Compartir PDF")
            }
        }

        CardBlock {
            Text("Gastos por categoría", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) {
                Text("Todavía no hay gastos este mes.", color = Muted)
            } else {
                val max = rows.maxOf { it.second }.coerceAtLeast(1L)
                rows.forEach { (category, cents) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category)
                        Text(formatCents(cents, data.currency), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Line),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((cents.toFloat() / max.toFloat()).coerceIn(0.04f, 1f))
                                .background(Clay),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        CardBlock {
            Text("Notas del mes", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = data.monthNotes[monthKey].orEmpty(),
                onValueChange = { if (it.length <= 1000) vm.setMonthNote(monthKey, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                placeholder = { Text("Algo que quieras recordar de este mes") },
            )
        }

        Text("Historial", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        if (txs.isEmpty()) {
            Text("Sin movimientos en este mes.", color = Muted)
        } else {
            txs.forEach { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface)
                        .padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.category, fontWeight = FontWeight.SemiBold)
                        val note = tx.note.ifBlank { tx.date }
                        Text("$note · ${tx.date}", color = Muted, fontSize = 13.sp)
                    }
                    Text(
                        text = (if (tx.kind == "income") "+" else "−") + formatCents(tx.cents, data.currency),
                        color = if (tx.kind == "income") Pine else Clay,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { editor = tx }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar ${tx.category}")
                    }
                    IconButton(onClick = { pendingDelete = tx }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${tx.category}")
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            Text("Otros meses", fontWeight = FontWeight.SemiBold)
            history.forEach { item ->
                val key = item.toString()
                val monthTx = data.transactions.filter { it.date.startsWith(key) }
                val inc = monthTx.filter { it.kind == "income" }.sumOf { it.cents }
                val exp = monthTx.filter { it.kind == "expense" }.sumOf { it.cents }
                TextButton(onClick = { month = item }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.format(monthTitle).replaceFirstChar { it.titlecase(Locale("es", "MX")) })
                        Text(formatCents(inc - exp, data.currency))
                    }
                }
            }
        }

        CardBlock {
            Text("Copia en este teléfono", fontWeight = FontWeight.SemiBold)
            Text(
                "Los datos ya se guardan solos. Esta copia sirve si cambias de aparato.",
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val json = vm.exportJson().toByteArray()
                    shareBytes(context, json, "abaco-copia.json", "application/json", "Guardar copia de Ábaco")
                }) { Text("Exportar") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                    Text("Restaurar")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (creating || editor != null) {
        TxDialog(
            initial = editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { kind, cents, category, note, date ->
                val current = editor
                if (current == null) vm.addTx(kind, cents, category, note, date)
                else vm.updateTx(current.id, kind, cents, category, note, date)
                creating = false
                editor = null
            },
        )
    }

    pendingDelete?.let { tx ->
        AbacoDialog(onDismiss = { pendingDelete = null }) {
            Text("¿Eliminar este movimiento?", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("${tx.category} · ${formatCents(tx.cents, data.currency)}", color = Muted)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
                Button(
                    onClick = {
                        vm.deleteTx(tx.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Clay, contentColor = Paper),
                ) { Text("Eliminar") }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = Color(0xFFD4CBBE), fontSize = 12.sp)
        Text(value, color = Paper, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GoalCard(data: AppData, vm: AbacoViewModel, balance: Long) {
    var text by remember(data.savingsGoalCents) { mutableStateOf(centsToInput(data.savingsGoalCents)) }
    val goal = data.savingsGoalCents
    val progress = if (goal <= 0L) 0f else (balance.coerceAtLeast(0).toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    CardBlock {
        Text("Meta de ahorro", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw
                val cents = if (raw.isBlank()) 0L else parseMajorToCents(raw, allowZero = true)
                if (cents != null && !raw.endsWith(".") && !raw.endsWith(",")) vm.setSavingsGoal(cents)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cantidad") },
            singleLine = true,
            suffix = { Text(data.currency) },
        )
        if (goal > 0) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = Pine,
                trackColor = Line,
            )
            Spacer(Modifier.height(8.dp))
            val caption = when {
                balance < 0 -> "Este mes no queda margen para la meta."
                balance >= goal -> "El saldo cubre la meta."
                else -> "Faltan ${formatCents(goal - balance, data.currency)} para la meta."
            }
            Text(caption, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun CurrencyMenu(current: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(current) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CURRENCIES.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onPick(code)
                        open = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TxDialog(
    initial: Tx?,
    onDismiss: () -> Unit,
    onSave: (kind: String, cents: Long, category: String, note: String, date: String) -> Unit,
) {
    var kind by remember { mutableStateOf(initial?.kind ?: "expense") }
    var amount by remember { mutableStateOf(initial?.let { centsToInput(it.cents) } ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Comida") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var error by remember { mutableStateOf("") }
    val suggestions = if (kind == "income") INCOME_CATEGORIES else EXPENSE_CATEGORIES

    AbacoDialog(onDismiss = onDismiss) {
        Text(if (initial == null) "Nuevo movimiento" else "Editar movimiento", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice("Gasto", kind == "expense") { kind = "expense" }
            Choice("Ingreso", kind == "income") { kind = "income" }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cantidad") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Categoría") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.forEach { item ->
                TextButton(onClick = { category = item }) { Text(item) }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(180) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nota") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Fecha (aaaa-mm-dd)") },
            singleLine = true,
        )
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Clay)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(
                onClick = {
                    val cents = parseMajorToCents(amount)
                    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                    when {
                        cents == null -> error = "Escribe una cantidad mayor que cero."
                        category.isBlank() -> error = "La categoría no puede ir vacía."
                        parsedDate == null -> error = "Usa la fecha como aaaa-mm-dd."
                        else -> onSave(kind, cents, category, note, parsedDate.toString())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) { Text("Guardar") }
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

private fun importBackup(context: Context, uri: Uri, vm: AbacoViewModel) {
    try {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }.orEmpty()
        vm.importJson(text)
    } catch (_: Exception) {
        vm.message("No se pudo leer ese archivo")
    }
}

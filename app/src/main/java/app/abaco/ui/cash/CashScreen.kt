package app.abaco.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abaco.AbacoViewModel
import app.abaco.data.AppData
import app.abaco.data.CashSave
import app.abaco.data.Denom
import app.abaco.data.cashTotal
import app.abaco.data.centsToInput
import app.abaco.data.formatCents
import app.abaco.data.parseMajorToCents
import app.abaco.ui.AbacoDialog
import app.abaco.ui.theme.Clay
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Surface
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val savedFormat = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale("es", "MX"))

@Composable
fun CashScreen(vm: AbacoViewModel, data: AppData) {
    val total = cashTotal(data.denoms)
    var editing by remember { mutableStateOf<Denom?>(null) }
    var creating by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var loadedId by remember { mutableStateOf<String?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var presetAsk by remember { mutableStateOf<String?>(null) }
    val loaded = data.cashSaves.find { it.id == loadedId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Ink)
                .padding(20.dp),
        ) {
            Text("Efectivo contado", color = Paper.copy(alpha = 0.72f), fontSize = 13.sp)
            Text(
                formatCents(total, data.cashCurrency),
                color = Paper,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            )
            Text(
                "${data.denoms.count { it.selected && it.count > 0 }} valores incluidos",
                color = Paper.copy(alpha = 0.72f),
                fontSize = 13.sp,
            )
        }

        Text("Plantillas de billetes", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("USD", "MXN", "EUR").forEach { code ->
                OutlinedButton(onClick = {
                    if (data.denoms.any { it.count > 0 }) presetAsk = code else vm.applyCashPreset(code)
                }) { Text(code) }
            }
        }
        Text("Puedes editar el valor de cada billete o moneda. Desmarca los que no quieras contar.", color = Muted, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.clearCounts() }) { Text("Poner en cero") }
            Button(
                onClick = { saving = true },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) { Text("Guardar") }
        }
        loaded?.let { save ->
            Text("Editando «${save.name}»", color = Muted, fontSize = 13.sp)
            TextButton(onClick = {
                vm.overwriteCash(save.id)
                vm.message("Conteo actualizado")
            }) { Text("Actualizar este conteo") }
        }

        if (data.denoms.isEmpty()) {
            Text("Agrega un billete o una moneda.", color = Muted)
        }
        data.denoms.forEach { denom ->
            DenomRow(
                denom = denom,
                currency = data.cashCurrency,
                onToggle = { vm.toggleDenom(denom.id) },
                onCount = { vm.setCount(denom.id, it) },
                onEdit = { editing = denom },
                onDelete = { vm.deleteDenom(denom.id) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" Valor")
            }
        }

        Text("Conteos guardados", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        if (data.cashSaves.isEmpty()) {
            Text("Cuando guardes un conteo, queda aquí en el teléfono.", color = Muted)
        } else {
            Row {
                TextButton(onClick = {
                    selecting = !selecting
                    picked = emptySet()
                }) { Text(if (selecting) "Cancelar selección" else "Seleccionar") }
                if (selecting && picked.isNotEmpty()) {
                    TextButton(onClick = {
                        vm.deleteCashMany(picked)
                        if (loadedId in picked) loadedId = null
                        picked = emptySet()
                        selecting = false
                    }) { Text("Borrar ${picked.size}") }
                }
            }
            data.cashSaves.forEach { save ->
                SavedRow(
                    save = save,
                    selecting = selecting,
                    picked = save.id in picked,
                    onPick = { checked ->
                        picked = if (checked) picked + save.id else picked - save.id
                    },
                    onLoad = {
                        vm.loadDenoms(save.lines, save.currency)
                        loadedId = save.id
                        vm.message("Conteo cargado")
                    },
                    onDelete = {
                        vm.deleteCash(save.id)
                        if (loadedId == save.id) loadedId = null
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (creating || editing != null) {
        DenomDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { label, cents, kind ->
                val current = editing
                if (current == null) vm.addDenom(label, cents, kind)
                else vm.updateDenom(current.id, label, cents, kind)
                creating = false
                editing = null
            },
        )
    }

    if (saving) {
        SaveDialog(
            onDismiss = { saving = false },
            onSave = { name, note ->
                vm.saveCash(name, note)
                saving = false
                vm.message("Conteo guardado")
            },
        )
    }

    presetAsk?.let { code ->
        AbacoDialog(onDismiss = { presetAsk = null }) {
            Text("¿Usar la plantilla $code?", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("Las cantidades de este conteo vuelven a cero. Los conteos ya guardados no cambian.", color = Muted)
            Spacer(Modifier.height(12.dp))
            Row {
                TextButton(onClick = { presetAsk = null }) { Text("Cancelar") }
                Button(
                    onClick = {
                        vm.applyCashPreset(code)
                        loadedId = null
                        presetAsk = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                ) { Text("Aplicar") }
            }
        }
    }
}

@Composable
private fun DenomRow(
    denom: Denom,
    currency: String,
    onToggle: () -> Unit,
    onCount: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val subtotal = if (denom.selected) denom.cents * denom.count else 0L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = denom.selected, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(denom.label, fontWeight = FontWeight.SemiBold)
            Text(
                "${if (denom.kind == "coin") "Moneda" else "Billete"} · ${formatCents(denom.cents, currency)}",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = { onCount(denom.count - 1) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Restar ${denom.label}")
        }
        Text(denom.count.toString(), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
        IconButton(onClick = { onCount(denom.count + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Sumar ${denom.label}")
        }
        Text(
            formatCents(subtotal, currency),
            fontWeight = FontWeight.Medium,
            color = if (denom.selected) Ink else Muted,
        )
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar ${denom.label}") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${denom.label}") }
    }
}

@Composable
private fun SavedRow(
    save: CashSave,
    selecting: Boolean,
    picked: Boolean,
    onPick: (Boolean) -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    val whenLabel = runCatching { OffsetDateTime.parse(save.savedAt).format(savedFormat) }.getOrDefault(save.savedAt)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) {
                Checkbox(checked = picked, onCheckedChange = onPick)
            }
            Column(Modifier.weight(1f)) {
                Text(save.name, fontWeight = FontWeight.SemiBold)
                Text("$whenLabel · ${save.currency}", color = Muted, fontSize = 12.sp)
            }
            Text(formatCents(save.totalCents, save.currency), fontWeight = FontWeight.SemiBold)
        }
        if (save.note.isNotBlank()) Text(save.note, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Row {
            TextButton(onClick = onLoad) { Text("Cargar") }
            TextButton(onClick = onDelete) { Text("Borrar", color = Clay) }
        }
    }
}

@Composable
private fun DenomDialog(
    initial: Denom?,
    onDismiss: () -> Unit,
    onSave: (label: String, cents: Long, kind: String) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var value by remember { mutableStateOf(initial?.let { centsToInput(it.cents) } ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: "bill") }
    var error by remember { mutableStateOf("") }
    AbacoDialog(onDismiss = onDismiss) {
        Text(if (initial == null) "Nuevo valor" else "Editar valor", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(24) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            singleLine = true,
            placeholder = { Text("100, o el nombre que uses") },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Valor") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (kind == "bill") {
                Button(onClick = { kind = "bill" }, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) { Text("Billete") }
            } else {
                OutlinedButton(onClick = { kind = "bill" }) { Text("Billete") }
            }
            if (kind == "coin") {
                Button(onClick = { kind = "coin" }, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) { Text("Moneda") }
            } else {
                OutlinedButton(onClick = { kind = "coin" }) { Text("Moneda") }
            }
        }
        if (error.isNotEmpty()) Text(error, color = Clay, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Row {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(
                onClick = {
                    val cents = parseMajorToCents(value)
                    if (cents == null) error = "Escribe un valor mayor que cero."
                    else onSave(label, cents, kind)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) { Text("Guardar") }
        }
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (name: String, note: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AbacoDialog(onDismiss = onDismiss) {
        Text("Guardar conteo", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            singleLine = true,
            placeholder = { Text("Caja del día") },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(300) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nota") },
        )
        Spacer(Modifier.height(12.dp))
        Row {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(
                onClick = { onSave(name, note) },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) { Text("Guardar") }
        }
    }
}

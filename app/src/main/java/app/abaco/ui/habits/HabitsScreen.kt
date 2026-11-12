package app.abaco.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abaco.AbacoViewModel
import app.abaco.data.AppData
import app.abaco.data.HABIT_COLORS
import app.abaco.data.Habit
import app.abaco.ui.AbacoDialog
import app.abaco.ui.habitColor
import app.abaco.ui.habitSoft
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Line
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Surface
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val weekLetters = listOf("L", "M", "X", "J", "V", "S", "D")
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "MX"))

@Composable
fun HabitsScreen(vm: AbacoViewModel, data: AppData) {
    val today = LocalDate.now()
    var mode by remember { mutableStateOf("week") }
    var weekOffset by remember { mutableIntStateOf(0) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var deleting by remember { mutableStateOf<Habit?>(null) }
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong()).plusWeeks(weekOffset.toLong())
    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val doneToday = data.habits.count { habit -> data.checks[habit.id].orEmpty().contains(today.toString()) }
    val motive = when {
        data.habits.isEmpty() -> "Un hábito pequeño, marcado cada día, basta para empezar."
        doneToday == data.habits.size -> "Hoy está completo."
        else -> "Hoy van $doneToday de ${data.habits.size}."
    }

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
            Text("Hoy", color = Paper.copy(alpha = 0.7f), fontSize = 13.sp)
            Text(motive, color = Paper, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ModeButton("Semana", mode == "week") { mode = "week" }
            ModeButton("Mes", mode == "month") { mode = "month" }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { creating = true },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Hábito", modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (mode == "week") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { weekOffset -= 1 }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Semana anterior")
                }
                Text(
                    if (weekOffset == 0) "Esta semana" else "Semana",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = { if (weekOffset < 0) weekOffset += 1 }, enabled = weekOffset < 0) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Semana siguiente")
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset -= 1 }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior")
                }
                Text(
                    month.format(monthTitle).replaceFirstChar { it.titlecase(Locale("es", "MX")) },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = { if (monthOffset < 0) monthOffset += 1 }, enabled = monthOffset < 0) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente")
                }
            }
        }
        if (data.habits.isEmpty()) {
            Text("Todavía no hay hábitos.", color = Muted)
        }
        data.habits.forEach { habit ->
            HabitCard(
                habit = habit,
                dates = data.checks[habit.id].orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
                today = today,
                mode = mode,
                weekStart = weekStart,
                month = month,
                onToggle = { date -> vm.toggleCheck(habit.id, date.toString()) },
                onEdit = { editing = habit },
                onDelete = { deleting = habit },
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (creating || editing != null) {
        HabitDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, color ->
                val current = editing
                if (current == null) vm.addHabit(name, color) else vm.updateHabit(current.id, name, color)
                creating = false
                editing = null
            },
        )
    }
    deleting?.let { habit ->
        AbacoDialog(onDismiss = { deleting = null }) {
            Text("¿Eliminar ${habit.name}?", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("También se borra su historial de marcas.", color = Muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { deleting = null }) { Text("Cancelar") }
                Button(
                    onClick = {
                        vm.deleteHabit(habit.id)
                        deleting = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = app.abaco.ui.theme.Clay, contentColor = Paper),
                ) { Text("Eliminar") }
            }
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    dates: Set<LocalDate>,
    today: LocalDate,
    mode: String,
    weekStart: LocalDate,
    month: YearMonth,
    onToggle: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val stats = habitStats(dates, today, month)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(habitColor(habit.color)),
            )
            Text(habit.name, modifier = Modifier.padding(start = 8.dp).weight(1f), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar ${habit.name}") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${habit.name}") }
        }
        Text(
            "Racha ${stats.streak} · mejor ${stats.best} · mes ${stats.percent}%",
            color = Muted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (mode == "week") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekLetters.forEachIndexed { index, letter ->
                    val date = weekStart.plusDays(index.toLong())
                    DayDot(
                        label = letter,
                        number = date.dayOfMonth.toString(),
                        checked = dates.contains(date),
                        today = date == today,
                        enabled = !date.isAfter(today),
                        color = habit.color,
                        onClick = { onToggle(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            val offset = month.atDay(1).dayOfWeek.value - 1
            Row(Modifier.fillMaxWidth()) {
                weekLetters.forEach { letter ->
                    Text(letter, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
                }
            }
            val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            DayDot(
                                label = "",
                                number = date.dayOfMonth.toString(),
                                checked = dates.contains(date),
                                today = date == today,
                                enabled = !date.isAfter(today),
                                color = habit.color,
                                onClick = { onToggle(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DayDot(
    label: String,
    number: String,
    checked: Boolean,
    today: Boolean,
    enabled: Boolean,
    color: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (checked) habitColor(color) else habitSoft(color)
    val fg = if (checked) Paper else Ink
    Column(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (today) Modifier.border(2.dp, Ink, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Checkbox
                selected = checked
            }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (label.isNotEmpty()) Text(label, color = fg.copy(alpha = 0.75f), fontSize = 11.sp)
        Text(number, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun HabitDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "pine") }
    var error by remember { mutableStateOf("") }
    AbacoDialog(onDismiss = onDismiss) {
        Text(if (initial == null) "Nuevo hábito" else "Editar hábito", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        Text("Color", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            HABIT_COLORS.forEach { (id, label) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { color = id }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(habitColor(id))
                            .then(if (color == id) Modifier.border(2.dp, Ink, CircleShape) else Modifier),
                    )
                    Text(label, fontSize = 11.sp, color = Muted)
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = app.abaco.ui.theme.Clay, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Row {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
            Button(
                onClick = {
                    if (name.isBlank()) error = "El hábito necesita un nombre."
                    else onSave(name, color)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
            ) { Text("Guardar") }
        }
    }
}

private data class HabitStats(val streak: Int, val best: Int, val percent: Int)

private fun habitStats(dates: Set<LocalDate>, today: LocalDate, month: YearMonth): HabitStats {
    val streakStart = if (dates.contains(today)) today else today.minusDays(1)
    var streak = 0
    if (dates.contains(streakStart)) {
        var cursor = streakStart
        while (dates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
    }
    val sorted = dates.sorted()
    var best = if (sorted.isEmpty()) 0 else 1
    var run = 1
    for (index in 1 until sorted.size) {
        val gap = ChronoUnit.DAYS.between(sorted[index - 1], sorted[index])
        if (gap == 1L) {
            run += 1
            best = maxOf(best, run)
        } else if (gap > 1L) {
            run = 1
        }
    }
    val elapsed = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
    val done = dates.count { YearMonth.from(it) == month && !it.isAfter(today) }
    val percent = if (elapsed == 0) 0 else ((done * 100f) / elapsed).toInt().coerceIn(0, 100)
    return HabitStats(streak, best, percent)
}

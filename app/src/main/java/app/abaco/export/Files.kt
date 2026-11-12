package app.abaco.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import app.abaco.data.AppData
import app.abaco.data.formatCents
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "MX"))
private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale("es", "MX"))

fun buildBudgetPdf(data: AppData, month: YearMonth): ByteArray {
    val doc = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = 52f
    val ink = android.graphics.Color.parseColor("#1A1814")
    val muted = android.graphics.Color.parseColor("#655E55")
    val pine = android.graphics.Color.parseColor("#1B6B4A")
    val clay = android.graphics.Color.parseColor("#A33B2B")
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 26f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 14f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 11f
        typeface = Typeface.SANS_SERIF
    }
    val quiet = Paint(body).apply { color = muted }

    fun newPage() {
        doc.finishPage(page)
        pageNumber += 1
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = 52f
    }

    fun ensure(space: Float) {
        if (y + space > pageHeight - 42) newPage()
    }

    fun draw(text: String, paint: Paint, gap: Float = 16f) {
        ensure(gap)
        canvas.drawText(text, 40f, y, paint)
        y += gap
    }

    val txs = data.transactions.filter { it.date.startsWith(month.toString()) }.sortedByDescending { it.date }
    val income = txs.filter { it.kind == "income" }.sumOf { it.cents }
    val expense = txs.filter { it.kind == "expense" }.sumOf { it.cents }
    val balance = income - expense
    val monthLabel = month.format(monthFormat).replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale("es", "MX")) else char.toString()
    }

    draw("Ábaco", title, 30f)
    draw("Presupuesto · $monthLabel", heading, 18f)
    draw("Generado en el teléfono · ${LocalDate.now()}", quiet, 22f)
    draw("Ingresos  ${formatCents(income, data.currency)}", body)
    draw("Gastos  ${formatCents(expense, data.currency)}", body)
    val balancePaint = Paint(heading).apply { color = if (balance >= 0) pine else clay }
    draw("Saldo restante  ${formatCents(balance, data.currency)}", balancePaint, 18f)
    draw("Meta de ahorro  ${formatCents(data.savingsGoalCents, data.currency)}", body, 22f)

    val note = data.monthNotes[month.toString()].orEmpty().trim()
    if (note.isNotEmpty()) {
        draw("Notas", heading, 18f)
        wrap(note).forEach { line -> draw(line, body, 15f) }
        y += 8f
    }

    draw("Historial del mes", heading, 20f)
    if (txs.isEmpty()) {
        draw("Sin movimientos.", quiet)
    } else {
        txs.forEach { tx ->
            val date = runCatching { LocalDate.parse(tx.date).format(dayFormat) }.getOrDefault(tx.date)
            val sign = if (tx.kind == "income") "+" else "−"
            val paint = Paint(body).apply { color = if (tx.kind == "income") pine else clay }
            draw("$date · ${tx.category}   $sign${formatCents(tx.cents, data.currency)}", paint, 15f)
            if (tx.note.isNotBlank()) draw(tx.note, quiet, 14f)
        }
    }

    val byCategory = txs.filter { it.kind == "expense" }
        .groupBy { it.category }
        .map { (category, items) -> category to items.sumOf { it.cents } }
        .sortedByDescending { it.second }
    if (byCategory.isNotEmpty()) {
        y += 8f
        draw("Gastos por categoría", heading, 20f)
        byCategory.forEach { (category, cents) ->
            draw("$category   ${formatCents(cents, data.currency)}", body)
        }
    }

    doc.finishPage(page)
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}

private fun wrap(text: String, max: Int = 88): List<String> {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val next = if (current.isEmpty()) word else "$current $word"
        if (next.length > max) {
            if (current.isNotEmpty()) lines += current
            current = word
        } else {
            current = next
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

fun savePdf(context: Context, bytes: ByteArray, displayName: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Abaco")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values,
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            true
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            File(dir, displayName).writeBytes(bytes)
            true
        }
    } catch (_: Exception) {
        false
    }
}

fun savePng(context: Context, bitmap: Bitmap, displayName: String): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Abaco")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = context.contentResolver.insert(collection, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: return false
        if (Build.VERSION.SDK_INT >= 29) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}

fun shareBytes(context: Context, bytes: ByteArray, fileName: String, mime: String, title: String) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    val uri = fileUri(context, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, title: String) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = fileUri(context, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun fileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.files", file)

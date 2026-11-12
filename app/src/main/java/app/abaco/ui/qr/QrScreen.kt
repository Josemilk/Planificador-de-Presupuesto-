package app.abaco.ui.qr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import app.abaco.AbacoViewModel
import app.abaco.data.AppData
import app.abaco.export.savePng
import app.abaco.export.shareBitmap
import app.abaco.qr.contrastRatio
import app.abaco.qr.encodeQr
import app.abaco.qr.formatCodeName
import app.abaco.qr.looksLikeUrl
import app.abaco.qr.parseHex
import app.abaco.scan.PortraitCaptureActivity
import app.abaco.ui.theme.Clay
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Line
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Surface
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val eccOptions = listOf(
    "L" to "Bajo · ~7%",
    "M" to "Medio · ~15%",
    "Q" to "Alto · ~25%",
    "H" to "Máximo · ~30%",
)

private val fgSwatches = listOf("#1A1814", "#1B6B4A", "#A33B2B", "#6E5316")
private val bgSwatches = listOf("#FFFDF9", "#F7F4EE", "#FFFFFF", "#E6F2EB")

@Composable
fun QrScreen(vm: AbacoViewModel, data: AppData) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf<String?>(null) }
    var scannedFormat by remember { mutableStateOf<String?>(null) }
    var fgText by remember(data.qrFg) { mutableStateOf(data.qrFg) }
    var bgText by remember(data.qrBg) { mutableStateOf(data.qrBg) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents
        if (!text.isNullOrBlank()) {
            scanned = text
            scannedFormat = result.formatName
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchScan(scanLauncher)
        else vm.message("Sin permiso de cámara no se puede escanear")
    }

    LaunchedEffect(data.qrText, data.qrFg, data.qrBg, data.qrSize, data.qrEcc) {
        val next = withContext(Dispatchers.Default) {
            encodeQr(data.qrText, data.qrSize, data.qrFg, data.qrBg, data.qrEcc)
        }
        bitmap = next
        failed = data.qrText.isNotBlank() && next == null
    }

    val fg = parseHex(data.qrFg, android.graphics.Color.parseColor("#1A1814"))
    val bg = parseHex(data.qrBg, android.graphics.Color.parseColor("#FFFDF9"))
    val ratio = contrastRatio(fg, bg)
    val status = when {
        data.qrText.isBlank() -> "Escribe un texto o una URL para ver el código."
        failed -> "No se pudo dibujar el código. Prueba con un texto más corto."
        ratio < 3.0 -> "Contraste ${"%.1f".format(ratio)} a 1. Puede que no se escanee."
        else -> "Listo · ${data.qrSize} px · corrección ${data.qrEcc}."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Código QR", fontWeight = FontWeight.SemiBold, fontSize = 28.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
        Text("La vista previa cambia mientras escribes. El escáner lee QR y códigos de barras sin internet.", color = Muted, fontSize = 14.sp)
        OutlinedTextField(
            value = data.qrText,
            onValueChange = vm::setQrText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Texto o URL") },
            minLines = 3,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Vista previa", fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(bg)),
                contentAlignment = Alignment.Center,
            ) {
                val current = bitmap
                if (current != null) {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = if (data.qrText.isBlank()) "Vista previa vacía" else "Código QR de ${data.qrText.take(80)}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(status, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val image = bitmap ?: return@Button
                        val ok = savePng(context, image, "abaco-qr.png")
                        vm.message(if (ok) "PNG guardado en Imágenes/Ábaco" else "No se pudo guardar el PNG")
                    },
                    enabled = bitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text(" PNG", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = {
                        val image = bitmap ?: return@OutlinedButton
                        shareBitmap(context, image, "abaco-qr.png", "Compartir QR")
                    },
                    enabled = bitmap != null,
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir PNG")
                }
            }
        }

        ColorField("Color del código", fgText, fgSwatches, { fgText = it }, { vm.setQrFg(it) })
        ColorField("Color de fondo", bgText, bgSwatches, { bgText = it }, { vm.setQrBg(it) })

        Text("Tamaño de descarga: ${data.qrSize} px", fontWeight = FontWeight.Medium)
        Slider(
            value = data.qrSize.toFloat(),
            onValueChange = { vm.setQrSize((it / 20f).toInt() * 20) },
            valueRange = 160f..480f,
        )
        Text("Corrección de errores", fontWeight = FontWeight.Medium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            eccOptions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (id, label) ->
                        val selected = data.qrEcc == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Ink else Surface)
                                .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(14.dp))
                                .clickable { vm.setQrEcc(id) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = if (selected) Paper else Ink, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (granted) launchScan(scanLauncher) else permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Text("Escanear QR o código de barras", modifier = Modifier.padding(start = 8.dp))
        }

        scanned?.let { text ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface)
                    .padding(16.dp),
            ) {
                Text(formatCodeName(scannedFormat), color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(text, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("código", text))
                        vm.message("Copiado")
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Text(" Copiar")
                    }
                    if (looksLikeUrl(text)) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text.trim())))
                                } catch (_: Exception) {
                                    vm.message("No hay una app para abrir ese enlace")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null)
                            Text(" Abrir")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ColorField(
    label: String,
    text: String,
    swatches: List<String>,
    onText: (String) -> Unit,
    onCommit: (String) -> Unit,
) {
    Column {
        Text(label, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val next = if (raw.startsWith("#") || raw.isEmpty()) raw else "#$raw"
                onText(next)
                val candidate = if (raw.startsWith("#")) raw else "#$raw"
                if (candidate.length == 7) onCommit(candidate.uppercase())
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Hex") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            swatches.forEach { hex ->
                val color = Color(parseHex(hex, android.graphics.Color.BLACK))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .border(1.dp, Line, RoundedCornerShape(10.dp))
                        .clickable {
                            onText(hex)
                            onCommit(hex)
                        },
                )
            }
        }
    }
}

private fun launchScan(launcher: androidx.activity.result.ActivityResultLauncher<ScanOptions>) {
    val options = ScanOptions().apply {
        setPrompt("Apunta al QR o al código de barras")
        setBeepEnabled(false)
        setOrientationLocked(true)
        setCaptureActivity(PortraitCaptureActivity::class.java)
        setDesiredBarcodeFormats(
            ScanOptions.QR_CODE,
            ScanOptions.EAN_13,
            ScanOptions.EAN_8,
            ScanOptions.UPC_A,
            ScanOptions.UPC_E,
            ScanOptions.CODE_128,
            ScanOptions.CODE_39,
            ScanOptions.CODE_93,
            ScanOptions.ITF,
            ScanOptions.DATA_MATRIX,
            ScanOptions.PDF_417,
            ScanOptions.RSS_14,
            ScanOptions.RSS_EXPANDED,
        )
    }
    launcher.launch(options)
}

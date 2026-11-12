package app.abaco.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.abaco.ui.theme.Clay
import app.abaco.ui.theme.ClaySoft
import app.abaco.ui.theme.Gold
import app.abaco.ui.theme.GoldSoft
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Pine
import app.abaco.ui.theme.PineSoft
import app.abaco.ui.theme.Surface as SurfaceColor

fun habitColor(id: String): Color = when (id) {
    "clay" -> Clay
    "gold" -> Gold
    "ink" -> Ink
    else -> Pine
}

fun habitSoft(id: String): Color = when (id) {
    "clay" -> ClaySoft
    "gold" -> GoldSoft
    "ink" -> Paper
    else -> PineSoft
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Muted,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun AbacoDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceColor,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .background(SurfaceColor),
                content = content,
            )
        }
    }
}

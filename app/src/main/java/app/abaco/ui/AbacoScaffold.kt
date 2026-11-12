package app.abaco.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.abaco.AbacoViewModel
import app.abaco.ui.budget.BudgetScreen
import app.abaco.ui.cash.CashScreen
import app.abaco.ui.habits.HabitsScreen
import app.abaco.ui.qr.QrScreen
import app.abaco.ui.theme.Ink
import app.abaco.ui.theme.Muted
import app.abaco.ui.theme.Paper
import app.abaco.ui.theme.Pine
import app.abaco.ui.theme.PineSoft
import app.abaco.ui.theme.Surface

private enum class Tab(val label: String, val icon: ImageVector) {
    Budget("Presupuesto", Icons.Filled.AccountBalanceWallet),
    Habits("Hábitos", Icons.Filled.CalendarMonth),
    Qr("QR", Icons.Filled.QrCode2),
    Cash("Efectivo", Icons.Filled.Payments),
}

@Composable
fun AbacoScaffold(vm: AbacoViewModel) {
    val data by vm.state.collectAsState()
    val snackbars = remember { SnackbarHostState() }
    var tabName by rememberSaveable { mutableStateOf(Tab.Budget.name) }
    val tab = Tab.entries.find { it.name == tabName } ?: Tab.Budget

    LaunchedEffect(vm) {
        vm.messages.collect { snackbars.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            androidx.compose.foundation.layout.Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            ) {
                Text(
                    "Ábaco",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    color = Ink,
                )
                Text(
                    "Sin cuenta y sin internet. Todo queda en este teléfono.",
                    color = Muted,
                    fontSize = 13.sp,
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tabName = item.name },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Pine,
                            selectedTextColor = Pine,
                            indicatorColor = PineSoft,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Budget -> BudgetScreen(vm, data)
                Tab.Habits -> HabitsScreen(vm, data)
                Tab.Qr -> QrScreen(vm, data)
                Tab.Cash -> CashScreen(vm, data)
            }
        }
    }
}

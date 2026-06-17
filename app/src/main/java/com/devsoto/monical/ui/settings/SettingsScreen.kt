package com.devsoto.monical.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.ui.components.PercentageCalculator
import com.devsoto.monical.ui.components.PercentageField
import com.devsoto.monical.ui.components.Rule
import com.devsoto.monical.ui.components.pressable
import com.devsoto.monical.ui.theme.Moni

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val percentEditorOpen = remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Moni.paper)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(onBack = onBack)

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PercentageField("% de devolución", settings.returnPercent) { percentEditorOpen.value = true }
                Text(
                    "Se aplica a los gastos pendientes por devolver. Los ya devueltos conservan el " +
                        "porcentaje con el que se registraron.",
                    fontFamily = Moni.font, fontSize = 11.5.sp, color = Moni.inkSoft,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }

    if (percentEditorOpen.value) PercentageCalculator(
        initialValue = settings.returnPercent,
        onConfirm = { viewModel.setReturnPercent(it); percentEditorOpen.value = false },
        onClose = { percentEditorOpen.value = false },
    )
}

@Composable
private fun ScreenHeader(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Moni.paper).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).pressable(onBack).border(1.5.dp, Moni.inkFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", fontFamily = Moni.font, fontSize = 18.sp, color = Moni.ink)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("AJUSTES", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, letterSpacing = 1.sp, color = Moni.ink)
            Text("Configuración de la app", fontFamily = Moni.font, fontSize = 11.sp, color = Moni.inkSoft,
                modifier = Modifier.padding(top = 1.dp))
        }
    }
    Rule()
}

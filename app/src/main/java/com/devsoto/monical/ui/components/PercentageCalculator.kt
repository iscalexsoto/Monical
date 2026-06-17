package com.devsoto.monical.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devsoto.monical.ui.theme.FontKey
import com.devsoto.monical.ui.theme.Moni

/**
 * Simplified pocket-calculator dialog for whole-percent entry. Unlike [PhysicalCalculator] it only
 * accepts integer digits plus clear / backspace / confirm — no operators or decimals. The buffer is
 * clamped to 0–100 by [applyPercentKey]. Reuses the [Calc] palette and [CalcKey] from the main
 * calculator for a consistent look.
 */
@Composable
fun PercentageCalculator(
    initialValue: Int,
    onConfirm: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var digits by remember { mutableStateOf(initialValue.coerceIn(0, 100).toString()) }
    val value = digits.toIntOrNull()?.coerceIn(0, 100) ?: 0

    @Suppress("AssignedValueIsNeverRead")
    fun press(k: String) { digits = applyPercentKey(digits, k) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(322.dp)
                .background(
                    Brush.linearGradient(listOf(Calc.bevelHi, Calc.body, Calc.body)),
                    RoundedCornerShape(26.dp),
                )
                .border(1.dp, Calc.trim, RoundedCornerShape(26.dp))
                .padding(16.dp),
        ) {
            // brand plate
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("MONICAL", fontFamily = FontKey, fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp, letterSpacing = 3.sp, color = Color(0xFFD8D0BE))
                    Text("M-250 · PORCENTAJE", fontFamily = FontKey,
                        fontSize = 8.5.sp, letterSpacing = 2.sp, color = Color(0xFF8C8472))
                }
                Text("%", fontFamily = FontKey, fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp, color = Color(0xFF8C8472))
            }
            Spacer(Modifier.height(9.dp))

            // LCD
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Calc.lcdHi, Calc.lcd)), RoundedCornerShape(8.dp))
                    .border(3.dp, Calc.trim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text("DEVOLUCIÓN", fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 2.sp,
                    color = Color(0xB32E3A22), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$value", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                        fontSize = 40.sp, letterSpacing = 1.sp, color = Calc.lcdInk)
                    Text("%", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                        fontSize = 22.sp, color = Calc.lcdInk.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 5.dp))
                }
            }
            Spacer(Modifier.height(14.dp))

            // keypad
            val gap = 9.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("7", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("7") }
                    CalcKey("8", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("8") }
                    CalcKey("9", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("9") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("4", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("4") }
                    CalcKey("5", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("5") }
                    CalcKey("6", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("6") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("1", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("1") }
                    CalcKey("2", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("2") }
                    CalcKey("3", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("3") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("C", Calc.clear, Color.White, Modifier.weight(1f)) { press("C") }
                    CalcKey("0", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("0") }
                    CalcKey("⌫", Calc.clear, Color.White, Modifier.weight(1f)) { press("⌫") }
                }
            }

            // confirm bar (enter)
            Spacer(Modifier.height(13.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .pressable({ onConfirm(value) })
                    .background(Brush.verticalGradient(listOf(Color(0xFFD5663F), Calc.eq)), RoundedCornerShape(12.dp))
                    .padding(15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓ GUARDAR %", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, letterSpacing = 2.sp, color = Color.White)
            }
            Text("TOCA FUERA PARA CERRAR", fontFamily = FontKey, fontSize = 8.5.sp,
                letterSpacing = 1.5.sp, color = Color(0xFF6F6757), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
        }
    }
}

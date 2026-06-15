package com.devsoto.monical.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devsoto.monical.data.model.round2
import com.devsoto.monical.ui.theme.FontKey
import com.devsoto.monical.ui.theme.Moni
import com.devsoto.monical.ui.theme.fmtPlain

/**
 * Evaluate a calculator expression. Honors × ÷ precedence over + −. Returns null on error.
 */
fun evalExpr(raw: String): Double? {
    if (raw.isBlank()) return null
    var s = raw.replace('×', '*').replace('÷', '/').replace('−', '-')
    s = s.trimEnd(' ', '+', '-', '*', '/', '.')
    if (s.isEmpty() || !Regex("^[-+*/.0-9 ]+$").matches(s)) return null
    return try {
        val v = evalArithmetic(s)
        if (v.isFinite()) round2(v) else null
    } catch (e: Exception) {
        null
    }
}

private fun evalArithmetic(expr: String): Double {
    val output = ArrayDeque<Double>()
    val ops = ArrayDeque<Char>()
    val prec = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2)
    fun apply() {
        val op = ops.removeLast()
        val b = output.removeLast()
        val a = output.removeLast()
        output.addLast(when (op) { '+' -> a + b; '-' -> a - b; '*' -> a * b; else -> a / b })
    }
    var i = 0
    var prevWasNum = false
    while (i < expr.length) {
        val c = expr[i]
        when {
            c == ' ' -> i++
            c.isDigit() || c == '.' || (c == '-' && !prevWasNum) -> {
                val sb = StringBuilder()
                if (c == '-') { sb.append('-'); i++ }
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) { sb.append(expr[i]); i++ }
                output.addLast(sb.toString().toDouble())
                prevWasNum = true
            }
            c in prec -> {
                while (ops.isNotEmpty() && (prec[ops.last()] ?: 0) >= prec.getValue(c)) apply()
                ops.addLast(c); i++; prevWasNum = false
            }
            else -> i++
        }
    }
    while (ops.isNotEmpty()) apply()
    return output.last()
}

private object Calc {
    val body = Color(0xFF39342C)
    val bevelHi = Color(0xFF5C5648)
    val trim = Color(0xFF26221C)
    val lcd = Color(0xFFB7C595)
    val lcdHi = Color(0xFFC7D3A8)
    val lcdInk = Color(0xFF2E3A22)
    val num = Color(0xFFE9E3D3)
    val numInk = Color(0xFF2C2720)
    val op = Color(0xFF7C7261)
    val clear = Color(0xFFA9543A)
    val eq = Color(0xFFC2532E)
}

/** Physical pocket calculator dialog. */
@Composable
fun PhysicalCalculator(
    initialValue: Double?,
    onConfirm: (Double) -> Unit,
    onClose: () -> Unit,
) {
    var expr by remember { mutableStateOf((initialValue?.let { fmtPlainNoSep(it) }) ?: "0") }
    var fresh by remember { mutableStateOf(true) }

    val live = evalExpr(expr)
    val bigText = if (live != null) fmtPlain(live) else "ERROR"

    fun press(k: String) {
        when {
            k[0].isDigit() -> {
                expr = if (fresh || expr == "0") k else expr + k; fresh = false
            }
            k == "." -> {
                expr = when {
                    fresh -> "0."
                    expr.split('+', '-', '×', '÷').last().contains('.') -> expr
                    else -> "$expr."
                }; fresh = false
            }
            k in listOf("+", "−", "×", "÷") -> {
                expr = (if (Regex("[+\\-×÷]$").containsMatchIn(expr)) expr.dropLast(1) else expr) + k
                fresh = false
            }
            k == "=" -> evalExpr(expr)?.let { expr = fmtPlainNoSep(it); fresh = true }
            k == "C" -> { expr = "0"; fresh = true }
            k == "⌫" -> expr = if (expr.length <= 1) "0" else expr.dropLast(1)
        }
    }

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
                    Text("M-250 · ELECTRONIC CALCULATOR", fontFamily = FontKey,
                        fontSize = 8.5.sp, letterSpacing = 2.sp, color = Color(0xFF8C8472))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("SOLAR", fontFamily = FontKey, fontSize = 7.5.sp, color = Color(0xFF8C8472),
                        modifier = Modifier.padding(end = 3.dp))
                    repeat(4) {
                        Box(Modifier.width(8.dp).height(14.dp)
                            .background(Color(0xFF23271C), RoundedCornerShape(1.dp))
                            .border(1.dp, Color(0xFF14160F), RoundedCornerShape(1.dp)))
                    }
                }
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
                Text(expr, fontFamily = Moni.font, fontSize = 14.sp, letterSpacing = 1.sp,
                    color = Color(0xB32E3A22), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$", fontFamily = Moni.font, fontSize = 13.sp, color = Calc.lcdInk.copy(alpha = 0.7f))
                    Text(bigText, fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                        fontSize = 34.sp, letterSpacing = 1.sp, color = Calc.lcdInk)
                }
            }
            Spacer(Modifier.height(14.dp))

            // keypad
            val gap = 9.dp
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("C", Calc.clear, Color.White, Modifier.weight(1f)) { press("C") }
                    CalcKey("⌫", Calc.clear, Color.White, Modifier.weight(1f)) { press("⌫") }
                    CalcKey("÷", Calc.op, Color.White, Modifier.weight(1f)) { press("÷") }
                    CalcKey("×", Calc.op, Color.White, Modifier.weight(1f)) { press("×") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("7", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("7") }
                    CalcKey("8", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("8") }
                    CalcKey("9", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("9") }
                    CalcKey("−", Calc.op, Color.White, Modifier.weight(1f)) { press("−") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    CalcKey("4", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("4") }
                    CalcKey("5", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("5") }
                    CalcKey("6", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("6") }
                    CalcKey("+", Calc.op, Color.White, Modifier.weight(1f)) { press("+") }
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            CalcKey("1", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("1") }
                            CalcKey("2", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("2") }
                            CalcKey("3", Calc.num, Calc.numInk, Modifier.weight(1f)) { press("3") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            CalcKey("0", Calc.num, Calc.numInk, Modifier.weight(2f)) { press("0") }
                            CalcKey(".", Calc.num, Calc.numInk, Modifier.weight(1f), big = true) { press(".") }
                        }
                    }
                    CalcKey("=", Calc.eq, Color.White, Modifier.weight(1f).fillMaxHeight()) { press("=") }
                }
            }

            // confirm bar
            Spacer(Modifier.height(13.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .pressable({ onConfirm(evalExpr(expr) ?: 0.0) })
                    .background(Brush.verticalGradient(listOf(Color(0xFFD5663F), Calc.eq)), RoundedCornerShape(12.dp))
                    .padding(15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓ CONFIRMAR MONTO", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, letterSpacing = 2.sp, color = Color.White)
            }
            Text("TOCA FUERA PARA CERRAR", fontFamily = FontKey, fontSize = 8.5.sp,
                letterSpacing = 1.5.sp, color = Color(0xFF6F6757), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp))
        }
    }
}

@Composable
private fun CalcKey(
    label: String, bg: Color, ink: Color, modifier: Modifier = Modifier,
    big: Boolean = false, onClick: () -> Unit,
) {
    Box(
        modifier
            .defaultMinSize(minHeight = 50.dp)
            .pressable(onClick, scale = 0.94f)
            .background(bg, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontFamily = FontKey, fontWeight = FontWeight.SemiBold,
            fontSize = if (big) 26.sp else 22.sp, color = ink)
    }
}

/** Plain number with no thousands separator — for the editable expression buffer. */
private fun fmtPlainNoSep(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

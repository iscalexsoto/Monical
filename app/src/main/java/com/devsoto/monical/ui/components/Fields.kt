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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsoto.monical.data.model.ReturnStatus
import com.devsoto.monical.data.model.UNCATEGORIZED
import com.devsoto.monical.data.model.categoryCode
import com.devsoto.monical.ui.theme.Moni
import com.devsoto.monical.ui.theme.TODAY
import com.devsoto.monical.ui.theme.YESTERDAY
import com.devsoto.monical.ui.theme.fmt
import com.devsoto.monical.ui.theme.fmtShort
import java.time.LocalDate

/** Small uppercase field label. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 1.5.sp,
        color = Moni.inkSoft, modifier = modifier.padding(start = 2.dp, bottom = 7.dp),
    )
}

/** Outlined text field with a floating label notch. */
@Composable
fun OutlinedField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    var focus by remember { mutableStateOf(false) }
    val borderColor = if (focus) Moni.accent else Moni.inkFaint
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Moni.font, fontSize = 16.sp, color = Moni.ink),
            cursorBrush = SolidColor(Moni.accent),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                .onFocusChanged { focus = it.isFocused }
                .padding(horizontal = 14.dp, vertical = 15.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, fontFamily = Moni.font, fontSize = 16.sp, color = Moni.inkFaint)
                }
                inner()
            },
        )
        Text(
            label.uppercase(), fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 1.sp,
            color = if (focus) Moni.accent else Moni.inkSoft,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = (-8).dp)
                .background(Moni.paper)
                .padding(horizontal = 6.dp),
        )
    }
}

/** Tape-style field that opens the physical calculator. */
@Composable
fun CalculatorField(label: String, value: Double?, onOpen: () -> Unit) {
    val has = value != null
    val display = if (has) fmt(value!!) else "$0.00"
    Column(Modifier.fillMaxWidth()) {
        FieldLabel(label)
        Box(
            Modifier.fillMaxWidth().pressable(onOpen)
                .clip(RoundedCornerShape(10.dp))
                .background(Moni.paperHi)
                .border(1.5.dp, Moni.accent, RoundedCornerShape(10.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).rotate(-6f).background(Moni.accent, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center) { CalcGlyph(size = 19.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("TOCA PARA CALCULAR →", fontFamily = Moni.font, fontSize = 10.sp,
                        letterSpacing = 1.5.sp, color = Moni.accent)
                    Text(display, fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                        fontSize = 23.sp, color = if (has) Moni.ink else Moni.inkFaint)
                }
            }
        }
    }
}

/** Date chips Hoy / Ayer / Otra. */
@Composable
fun DateChips(
    selected: String, customDate: LocalDate?,
    onHoy: () -> Unit, onAyer: () -> Unit, onCustom: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel("Fecha")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(selected == "hoy", onHoy, "Hoy", fmtShort(TODAY), Modifier.weight(1f))
            Chip(selected == "ayer", onAyer, "Ayer", fmtShort(YESTERDAY), Modifier.weight(1f))
            Chip(
                selected == "custom", onCustom,
                customDate?.let { fmtShort(it) } ?: "Otra",
                customDate?.year?.toString() ?: "▸ elegir",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(active: Boolean, onClick: () -> Unit, top: String, bottom: String, modifier: Modifier) {
    Column(
        modifier
            .pressable(onClick)
            .border(1.5.dp, if (active) Moni.accent else Moni.inkFaint, RoundedCornerShape(9.dp))
            .background(if (active) Moni.accent else Color.Transparent, RoundedCornerShape(9.dp))
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(top, fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
            color = if (active) Color.White else Moni.ink)
        Text(bottom, fontFamily = Moni.font, fontSize = 9.5.sp, letterSpacing = 0.5.sp,
            color = if (active) Color.White.copy(alpha = 0.8f) else Moni.inkFaint)
    }
}

/** Return-status selector (3-option segmented). */
@Composable
fun ReturnStatusSelector(value: ReturnStatus, onChange: (ReturnStatus) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel("Estado de devolución")
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .border(1.5.dp, Moni.inkFaint, RoundedCornerShape(9.dp)),
        ) {
            ReturnStatus.entries.forEachIndexed { i, e ->
                val active = value == e
                if (i > 0) Box(Modifier.width(1.5.dp).height(48.dp).background(Moni.inkFaint))
                Column(
                    Modifier.weight(1f).pressable({ onChange(e) })
                        .background(if (active) Moni.accent else Color.Transparent)
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(e.mark, fontFamily = Moni.font, fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else Moni.ink)
                    Text(e.label, fontFamily = Moni.font, fontSize = 10.sp, letterSpacing = 0.3.sp,
                        textAlign = TextAlign.Center,
                        color = if (active) Color.White.copy(alpha = 0.85f) else Moni.inkSoft,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

/** Category field that opens the picker. */
@Composable
fun CategoryField(value: String, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel("Categoría")
        Row(
            Modifier.fillMaxWidth().pressable(onOpen)
                .border(1.5.dp, Moni.inkFaint, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryTag(categoryCode(value))
            Spacer(Modifier.width(10.dp))
            Text(value, fontFamily = Moni.font, fontSize = 16.sp,
                color = if (value == UNCATEGORIZED) Moni.inkFaint else Moni.ink,
                modifier = Modifier.weight(1f))
            Text("▾", fontFamily = Moni.font, fontSize = 13.sp, color = Moni.inkSoft)
        }
    }
}

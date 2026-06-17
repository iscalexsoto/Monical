package com.devsoto.monical.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devsoto.monical.data.model.CATEGORIES
import com.devsoto.monical.data.model.UNCATEGORIZED
import com.devsoto.monical.ui.theme.MON_FULL
import com.devsoto.monical.ui.theme.Moni
import com.devsoto.monical.ui.theme.TODAY
import com.devsoto.monical.ui.theme.fmt
import com.devsoto.monical.ui.theme.isToday
import java.time.LocalDate

/** Full-screen scrim that hosts a custom bottom sheet, sliding in from the bottom on appear. */
@Composable
private fun BottomSheetScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(state, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x73221C12)))
        }
        AnimatedVisibility(
            state,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Moni.paper)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
            ) { content() }
        }
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(40.dp).height(4.dp).background(Moni.inkFaint, RoundedCornerShape(2.dp)))
    }
}

/** Add sheet (FAB → choose capture method). */
@Composable
fun AddSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onManual: () -> Unit,
    onClose: () -> Unit,
) {
    BottomSheetScrim(onClose) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            SheetGrabber()
            Text("— NUEVO GASTO —", fontFamily = Moni.font, fontSize = 12.sp, letterSpacing = 2.sp,
                color = Moni.inkSoft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
            AddOption("▣", "Escanear ticket", "Toma una foto", Moni.accent, onCamera)
            Spacer(Modifier.height(12.dp))
            AddOption("▦", "Escanear desde galería", "Elige una foto desde la galería", Moni.accent, onGallery)
            Spacer(Modifier.height(12.dp))
            AddOption("✎", "Capturar manual", "Escribir los datos a mano", Moni.ink, onManual)
        }
    }
}

@Composable
private fun AddOption(mark: String, title: String, sub: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick)
            .border(1.5.dp, accent, RoundedCornerShape(12.dp))
            .background(Moni.paperHi, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).background(accent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Text(mark, fontSize = 22.sp, color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = Moni.ink)
            Text(sub, fontFamily = Moni.font, fontSize = 11.5.sp, color = Moni.inkSoft,
                modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontFamily = Moni.font, fontSize = 18.sp, color = accent)
    }
}

/** Confirm dialog (mark all returned). */
@Composable
fun ConfirmDialog(count: Int, amount: Double, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Moni.paper, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Stamp("✓ pagado", color = Moni.paid, angle = -6f)
            Spacer(Modifier.height(16.dp))
            Text("¿Marcar todo como devuelto?", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 17.sp, color = Moni.ink, textAlign = TextAlign.Center)
            val plural = if (count == 1) "gasto pasará" else "gastos pasarán"
            Text(
                "$count $plural a Devuelto. El total pendiente de ${fmt(amount)} quedará en cero.",
                fontFamily = Moni.font, fontSize = 13.sp, color = Moni.inkSoft,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).pressable(onCancel)
                        .border(1.5.dp, Moni.inkFaint, RoundedCornerShape(10.dp))
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancelar", fontFamily = Moni.font, fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp, color = Moni.ink)
                }
                Box(
                    Modifier.weight(1f).pressable(onConfirm)
                        .background(Moni.paid, RoundedCornerShape(10.dp))
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Sí, marcar", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

/** Category picker (bottom sheet list). */
@Composable
fun CategoryPicker(value: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    BottomSheetScrim(onClose) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 22.dp)) {
            SheetGrabber()
            Text("— CATEGORÍA —", fontFamily = Moni.font, fontSize = 12.sp, letterSpacing = 2.sp,
                color = Moni.inkSoft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            CATEGORIES.forEachIndexed { i, c ->
                val active = value == c.id
                Row(
                    Modifier.fillMaxWidth().pressable({ onPick(c.id) }).padding(vertical = 13.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTag(c.code, textColor = if (active) Moni.accent else Moni.inkSoft,
                        border = if (active) Moni.accent else Moni.inkFaint)
                    Spacer(Modifier.width(12.dp))
                    Text(c.id, fontFamily = Moni.font, fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (c.id == UNCATEGORIZED) Moni.inkFaint else Moni.ink,
                        modifier = Modifier.weight(1f))
                    if (active) Text("✓", fontFamily = Moni.font, color = Moni.accent)
                }
                if (i < CATEGORIES.lastIndex) Rule(RuleVariant.Dot, Moni.inkFaint)
            }
        }
    }
}

/** Date picker (compact calendar dialog). */
@Composable
fun DatePicker(value: LocalDate?, onPick: (LocalDate) -> Unit, onClose: () -> Unit) {
    val init = value ?: TODAY
    val view = remember { mutableStateOf(init.year to init.monthValue) } // (year, month 1..12)
    val (vy, vm) = view.value
    val sel = value ?: TODAY

    val firstDow = LocalDate.of(vy, vm, 1).dayOfWeek.value % 7   // 0=Sun
    val daysInMonth = LocalDate.of(vy, vm, 1).lengthOfMonth()
    val cells = buildList<Int?> { repeat(firstDow) { add(null) }; for (d in 1..daysInMonth) add(d) }

    fun shift(n: Int) {
        var m = vm + n; var y = vy
        if (m < 1) { m = 12; y-- }; if (m > 12) { m = 1; y++ }
        view.value = y to m
    }

    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().background(Moni.paper, RoundedCornerShape(16.dp)).padding(18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontFamily = Moni.font, fontSize = 18.sp, color = Moni.ink,
                    modifier = Modifier.pressable({ shift(-1) }).padding(6.dp))
                Text("${MON_FULL[vm - 1]} $vy", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, letterSpacing = 1.sp, color = Moni.ink)
                // No future: disable advancing once the visible month is the current one or later.
                val canGoNext = vy < TODAY.year || (vy == TODAY.year && vm < TODAY.monthValue)
                Text("›", fontFamily = Moni.font, fontSize = 18.sp,
                    color = if (canGoNext) Moni.ink else Moni.inkFaint,
                    modifier = (if (canGoNext) Modifier.pressable({ shift(1) }) else Modifier).padding(6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("D", "L", "M", "M", "J", "V", "S").forEach {
                    Text(it, fontFamily = Moni.font, fontSize = 10.sp, color = Moni.inkFaint,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 2.dp))
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    for (idx in 0 until 7) {
                        val d = week.getOrNull(idx)
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val cellDate = LocalDate.of(vy, vm, d)
                                val isSel = sel.year == vy && sel.monthValue == vm && sel.dayOfMonth == d
                                val isTod = isToday(cellDate)
                                val future = cellDate.isAfter(TODAY) // not selectable
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(50))
                                        .background(if (isSel) Moni.accent else Color.Transparent)
                                        .border(1.5.dp, if (isTod && !isSel) Moni.accent else Color.Transparent, RoundedCornerShape(50))
                                        .then(if (future) Modifier else Modifier.pressable({ onPick(cellDate) })),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("$d", fontFamily = Moni.font, fontSize = 13.sp,
                                        color = if (isSel) Color.White else if (future) Moni.inkFaint else Moni.ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

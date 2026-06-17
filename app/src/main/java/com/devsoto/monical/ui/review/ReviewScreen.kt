package com.devsoto.monical.ui.review

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.data.model.RETURN_SHARE
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.data.refine.CorrectionField
import com.devsoto.monical.data.refine.FieldCorrection
import com.devsoto.monical.ui.components.CalculatorField
import com.devsoto.monical.ui.components.CategoryField
import com.devsoto.monical.ui.components.CategoryPicker
import com.devsoto.monical.ui.components.DateChips
import com.devsoto.monical.ui.components.DatePicker
import com.devsoto.monical.ui.components.FieldLabel
import com.devsoto.monical.ui.components.OutlinedField
import com.devsoto.monical.ui.components.PhysicalCalculator
import com.devsoto.monical.ui.components.ReturnStatusSelector
import com.devsoto.monical.ui.components.Rule
import com.devsoto.monical.ui.components.RuleVariant
import com.devsoto.monical.ui.components.pressable
import com.devsoto.monical.ui.scan.ScanViewModel
import com.devsoto.monical.ui.theme.Moni
import com.devsoto.monical.ui.theme.TODAY
import com.devsoto.monical.ui.theme.YESTERDAY
import com.devsoto.monical.ui.theme.fmt
import com.devsoto.monical.ui.theme.isToday
import com.devsoto.monical.ui.theme.isYesterday
import com.devsoto.monical.ui.theme.toLocalDate
import com.devsoto.monical.ui.theme.toUtcMillis

/** Which value the physical calculator is editing. */
private sealed interface CalcTarget {
    data object Total : CalcTarget
    data class Item(val index: Int) : CalcTarget
}

@Composable
fun ReviewScreen(viewModel: ScanViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft ?: return
    val mode = uiState.mode

    val calcTarget = remember { mutableStateOf<CalcTarget?>(null) }
    val dateOpen = remember { mutableStateOf(false) }
    val catOpen = remember { mutableStateOf(false) }

    val date = draft.dateMillis?.toLocalDate()
    val dateSelected = when {
        date == null -> ""
        isToday(date) -> "hoy"
        isYesterday(date) -> "ayer"
        else -> "custom"
    }

    Box(modifier.fillMaxSize().background(Moni.paper)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(mode.title, mode.sub, onBack = viewModel::reset)

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (mode == ReviewMode.SCAN) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Moni.accentWash, RoundedCornerShape(9.dp))
                            .border(1.dp, Moni.accent, RoundedCornerShape(9.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✦", fontFamily = Moni.font, fontSize = 15.sp, color = Moni.accent)
                        Spacer(Modifier.width(9.dp))
                        Text("Datos prellenados por Gemini. Verifica el total y la fecha antes de guardar.",
                            fontFamily = Moni.font, fontSize = 11.5.sp, color = Moni.accentDark)
                    }
                }

                if (mode == ReviewMode.SCAN && uiState.corrections.isNotEmpty()) {
                    CorrectionsBanner(
                        corrections = uiState.corrections,
                        onRevert = { c -> revertCorrection(viewModel, c, uiState.rawParsedDraft, draft.items) },
                    )
                }

                DateChips(
                    selected = dateSelected, customDate = if (dateSelected == "custom") date else null,
                    onHoy = { viewModel.updateDate(TODAY.toUtcMillis()) },
                    onAyer = { viewModel.updateDate(YESTERDAY.toUtcMillis()) },
                    onCustom = { dateOpen.value = true },
                )

                OutlinedField("Comercio", draft.merchant.orEmpty(), viewModel::updateMerchant, "Nombre del comercio")

                CalculatorField("Total", draft.total) { calcTarget.value = CalcTarget.Total }

                ReturnStatusSelector(draft.returnStatus, viewModel::updateReturnStatus)

                if (draft.returnStatus == com.devsoto.monical.data.model.ReturnStatus.PENDING && draft.total != null) {
                    Row(
                        Modifier.fillMaxWidth().background(Moni.accentWash, RoundedCornerShape(9.dp))
                            .border(1.dp, Moni.accentSoft, RoundedCornerShape(9.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Devolución (75%)", fontFamily = Moni.font, fontSize = 12.sp, color = Moni.accentDark)
                        Text(fmt(draft.total * RETURN_SHARE), fontFamily = Moni.font,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Moni.accent)
                    }
                }

                CategoryField(draft.category) { catOpen.value = true }

                ItemsSection(
                    items = draft.items,
                    onNameChange = { i, name -> viewModel.updateItem(i, draft.items[i].copy(name = name)) },
                    onAmountTap = { i -> calcTarget.value = CalcTarget.Item(i) },
                    onRemove = viewModel::removeItem,
                    onAdd = viewModel::addItem,
                )

                if (mode == ReviewMode.EDIT) {
                    Text("✕ eliminar gasto", fontFamily = Moni.font, fontSize = 12.sp,
                        letterSpacing = 1.sp, color = Moni.accentDark, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().pressable(viewModel::deleteCurrent).padding(12.dp))
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Moni.paper).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().pressable({ if (!uiState.isSaving) viewModel.save() })
                    .background(Moni.accent, RoundedCornerShape(13.dp))
                    .padding(17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (uiState.isSaving) "GUARDANDO…" else "✓ GUARDAR GASTO",
                    fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, letterSpacing = 2.sp, color = Color.White)
            }
        }

        uiState.error?.let { error ->
            Text(error, fontFamily = Moni.font, fontSize = 12.sp, color = Moni.accentDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp, start = 16.dp, end = 16.dp))
        }
    }

    when (val t = calcTarget.value) {
        CalcTarget.Total -> PhysicalCalculator(
            initialValue = draft.total,
            onConfirm = { viewModel.updateTotal(it); calcTarget.value = null },
            onClose = { calcTarget.value = null },
        )
        is CalcTarget.Item -> PhysicalCalculator(
            initialValue = draft.items.getOrNull(t.index)?.lineTotal,
            onConfirm = { value ->
                draft.items.getOrNull(t.index)?.let { viewModel.updateItem(t.index, it.copy(lineTotal = value)) }
                calcTarget.value = null
            },
            onClose = { calcTarget.value = null },
        )
        null -> {}
    }
    if (dateOpen.value) DatePicker(
        value = date, onClose = { dateOpen.value = false },
        onPick = { viewModel.updateDate(it.toUtcMillis()); dateOpen.value = false },
    )
    if (catOpen.value) CategoryPicker(
        value = draft.category, onClose = { catOpen.value = false },
        onPick = { viewModel.updateCategory(it); catOpen.value = false },
    )
}

@Composable
private fun ScreenHeader(title: String, sub: String, onBack: () -> Unit) {
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
            Text(title.uppercase(), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, letterSpacing = 1.sp, color = Moni.ink)
            Text(sub, fontFamily = Moni.font, fontSize = 11.sp, color = Moni.inkSoft,
                modifier = Modifier.padding(top = 1.dp))
        }
    }
    Rule()
}

@Composable
private fun ItemsSection(
    items: List<ReceiptItem>,
    onNameChange: (Int, String) -> Unit,
    onAmountTap: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel("Partidas")
        Rule()
        items.forEachIndexed { index, item ->
            if (item.isAdjustment) {
                AdjustmentRow(item)
            } else {
                ItemRow(
                    item = item,
                    onNameChange = { onNameChange(index, it) },
                    onAmountTap = { onAmountTap(index) },
                    onRemove = { onRemove(index) },
                )
            }
            Rule(RuleVariant.Dot, Moni.inkFaint)
        }
        Row(
            Modifier.fillMaxWidth().pressable(onAdd).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("＋ agregar partida", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp, letterSpacing = 1.sp, color = Moni.accent)
        }
    }
}

@Composable
private fun ItemRow(
    item: ReceiptItem,
    onNameChange: (String) -> Unit,
    onAmountTap: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = item.name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = Moni.font, fontSize = 14.sp, color = Moni.ink),
            cursorBrush = SolidColor(Moni.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (item.name.isEmpty()) {
                    Text("Concepto", fontFamily = Moni.font, fontSize = 14.sp, color = Moni.inkFaint)
                }
                inner()
            },
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.pressable(onAmountTap)
                .border(1.dp, Moni.accent, RoundedCornerShape(7.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(fmt(item.lineTotal ?: 0.0), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, color = Moni.ink)
        }
        Text("✕", fontFamily = Moni.font, fontSize = 14.sp, color = Moni.inkSoft,
            modifier = Modifier.pressable(onRemove).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

/**
 * Lists the auto-applied post-processing corrections (learned aliases + future-year fixes) so the
 * user sees what changed and can revert any single one back to the raw parser value.
 */
@Composable
private fun CorrectionsBanner(
    corrections: List<FieldCorrection>,
    onRevert: (FieldCorrection) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .background(Moni.paidWash, RoundedCornerShape(9.dp))
            .border(1.dp, Moni.paid, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓", fontFamily = Moni.font, fontSize = 14.sp, color = Moni.paid)
            Spacer(Modifier.width(8.dp))
            Text("Correcciones automáticas", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp, letterSpacing = 0.5.sp, color = Moni.paid)
        }
        corrections.forEach { c ->
            Row(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(correctionLabel(c.field), fontFamily = Moni.font, fontSize = 10.sp,
                        letterSpacing = 0.5.sp, color = Moni.inkSoft)
                    Text("${c.original.ifBlank { "—" }}  →  ${c.corrected}", fontFamily = Moni.font,
                        fontSize = 12.sp, color = Moni.ink)
                }
                Text("revertir", fontFamily = Moni.font, fontSize = 11.sp, color = Moni.accentDark,
                    modifier = Modifier.pressable({ onRevert(c) }).padding(horizontal = 6.dp, vertical = 4.dp))
            }
        }
    }
}

private fun correctionLabel(field: CorrectionField): String = when (field) {
    CorrectionField.Merchant -> "COMERCIO"
    CorrectionField.Date -> "FECHA"
    is CorrectionField.Item -> "PARTIDA ${field.index + 1}"
}

private fun revertCorrection(
    viewModel: ScanViewModel,
    correction: FieldCorrection,
    rawParsed: com.devsoto.monical.data.model.ReceiptDraft?,
    items: List<ReceiptItem>,
) {
    when (val f = correction.field) {
        CorrectionField.Merchant -> viewModel.updateMerchant(rawParsed?.merchant ?: correction.original)
        CorrectionField.Date -> rawParsed?.dateMillis?.let { viewModel.updateDate(it) }
        is CorrectionField.Item -> {
            val current = items.getOrNull(f.index) ?: return
            val rawName = rawParsed?.items?.getOrNull(f.index)?.name ?: correction.original
            viewModel.updateItem(f.index, current.copy(name = rawName))
        }
    }
}

@Composable
private fun AdjustmentRow(item: ReceiptItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Ajuste", fontFamily = Moni.font, fontSize = 14.sp, color = Moni.inkSoft,
            modifier = Modifier.weight(1f))
        Text("(automático)", fontFamily = Moni.font, fontSize = 10.sp, color = Moni.inkFaint)
        Spacer(Modifier.width(10.dp))
        Text(fmt(item.lineTotal ?: 0.0), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 14.sp, color = Moni.accent)
    }
}

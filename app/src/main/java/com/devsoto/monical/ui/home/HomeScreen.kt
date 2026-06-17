package com.devsoto.monical.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.data.model.MonthlyRollup
import com.devsoto.monical.data.model.ReceiptCard
import com.devsoto.monical.data.model.categoryCode
import com.devsoto.monical.data.model.pendingRefund
import com.devsoto.monical.data.model.returnAmount
import com.devsoto.monical.ui.components.AddSheet
import com.devsoto.monical.ui.components.Barcode
import com.devsoto.monical.ui.components.CategoryTag
import com.devsoto.monical.ui.components.ConfirmDialog
import com.devsoto.monical.ui.components.Rule
import com.devsoto.monical.ui.components.RuleVariant
import com.devsoto.monical.ui.components.TornEdge
import com.devsoto.monical.ui.components.pressable
import com.devsoto.monical.ui.capture.createImageUri
import com.devsoto.monical.ui.theme.MON_FULL
import com.devsoto.monical.ui.theme.Moni
import com.devsoto.monical.ui.theme.TODAY
import com.devsoto.monical.ui.theme.fmt
import com.devsoto.monical.ui.theme.fmtLong
import com.devsoto.monical.ui.theme.fmtShort
import com.devsoto.monical.ui.theme.toLocalDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImageCaptured: (Uri) -> Unit,
    onManual: () -> Unit,
    onOpenReceipt: (String, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var addOpen by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }

    val pendingCameraUri = remember { arrayOfNulls<Uri>(1) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri[0]
        if (ok && uri != null) onImageCaptured(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImageCaptured(uri)
    }

    val summary = uiState.summary

    Box(modifier.fillMaxSize().background(Moni.desk)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HomeContent(
                summary = summary,
                share = uiState.returnShare,
                open = uiState.sectionOpen,
                onToggle = viewModel::toggleSection,
                onOpenReceipt = onOpenReceipt,
                onMarkAll = { confirmOpen = true },
                onOpenSettings = onOpenSettings,
            )
        }

        ExtendedFab(onClick = { addOpen = true }, modifier = Modifier.align(Alignment.BottomEnd))

        if (addOpen) AddSheet(
            onCamera = {
                addOpen = false
                val uri = createImageUri(context)
                pendingCameraUri[0] = uri
                cameraLauncher.launch(uri)
            },
            onGallery = {
                addOpen = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onManual = { addOpen = false; onManual() },
            onClose = { addOpen = false },
        )
        if (confirmOpen) ConfirmDialog(
            count = summary.active.size, amount = summary.pendingRefund(uiState.returnShare),
            onConfirm = { viewModel.markAllReturned(); confirmOpen = false },
            onCancel = { confirmOpen = false },
        )
    }
}

@Composable
private fun ExtendedFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(end = 16.dp, bottom = 20.dp)
            .pressable(onClick)
            .background(Moni.accent, RoundedCornerShape(16.dp))
            .padding(start = 17.dp, end = 20.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+", fontSize = 24.sp, color = Color.White)
        Spacer(Modifier.width(9.dp))
        Text("GASTO", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 14.sp, letterSpacing = 1.sp, color = Color.White)
    }
}

@Composable
private fun HomeContent(
    summary: com.devsoto.monical.data.model.ReceiptSummary,
    share: Double,
    open: Map<HomeSection, Boolean>,
    onToggle: (HomeSection) -> Unit,
    onOpenReceipt: (String, Boolean) -> Unit,
    onMarkAll: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pending = summary.active
    val months = summary.archivedMonthly.entries.sortedByDescending { it.key }
    val archivedCount = months.sumOf { it.value.count }
    val archivedTotal = months.sumOf { it.value.total }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().background(Moni.paper)) {
            TornEdge(top = true)
            Column(Modifier.padding(horizontal = 20.dp)) {
                ReceiptHeader(share = share, onOpenSettings = onOpenSettings)
                TotalBlock(summary.pendingRefund(share), pending.size)
                if (pending.isNotEmpty()) MarkAllButton(onMarkAll)
                Rule(modifier = Modifier.padding(top = 10.dp))

                // ── Pending ──
                val pendingOpen = open[HomeSection.PENDING] == true
                SectionHeader(
                    mark = "※", label = "Por Devolver", count = pending.size,
                    subtotal = pending.takeIf { it.isNotEmpty() }?.sumOf { it.returnAmount(share) },
                    accent = Moni.accent, open = pendingOpen, onToggle = { onToggle(HomeSection.PENDING) },
                )
                AnimatedVisibility(pendingOpen) {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        if (pending.isEmpty()) {
                            EmptyRow()
                        } else pending.forEachIndexed { i, card ->
                            ReceiptCardRow(card, share) { onOpenReceipt(card.id, false) }
                            if (i < pending.lastIndex) Rule(RuleVariant.Dot, Moni.inkFaint)
                        }
                    }
                }
                Rule()

                // ── Archive (monthly rollups, cheap to keep & show) ──
                val archiveOpen = open[HomeSection.ARCHIVE] == true
                SectionHeader(
                    mark = "✓", label = "Archivado", count = archivedCount,
                    subtotal = months.takeIf { it.isNotEmpty() }?.let { archivedTotal },
                    accent = Moni.paid, open = archiveOpen, onToggle = { onToggle(HomeSection.ARCHIVE) },
                )
                AnimatedVisibility(archiveOpen) {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        if (months.isEmpty()) {
                            EmptyRow()
                        } else months.forEachIndexed { i, (key, rollup) ->
                            MonthRow(key, rollup)
                            if (i < months.lastIndex) Rule(RuleVariant.Dot, Moni.inkFaint)
                        }
                    }
                }
                Rule()

                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("GRACIAS POR LLEVAR LA CUENTA", fontFamily = Moni.font, fontSize = 11.sp,
                        letterSpacing = 2.sp, color = Moni.inkSoft)
                    Text("* conserva este ticket *", fontFamily = Moni.font, fontSize = 10.sp,
                        color = Moni.inkFaint, modifier = Modifier.padding(top = 3.dp))
                }
                Barcode()
            }
            TornEdge(top = false)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun EmptyRow() {
    Text("— sin movimientos —", fontFamily = Moni.font, fontSize = 12.sp,
        color = Moni.inkFaint, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp))
}

@Composable
private fun ReceiptHeader(share: Double, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("MONICAL", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 27.sp, letterSpacing = 8.sp, color = Moni.ink)
            Text("CONTROL DE GASTOS", fontFamily = Moni.font,
                fontSize = 10.5.sp, letterSpacing = 3.sp, color = Moni.inkSoft,
                modifier = Modifier.padding(top = 2.dp))
            Rule(modifier = Modifier.padding(vertical = 10.dp))
            Column(Modifier.fillMaxWidth()) {
                MetaRow("CLIENTE", "Alejandro Soto Puerto")
                MetaRow("DEVOLUCIÓN", "${(100 * share).toInt()}%")
                MetaRow(fmtLong(TODAY), "")
            }
            Rule(RuleVariant.Double, modifier = Modifier.padding(top = 9.dp))
        }
        Text("⚙", fontFamily = Moni.font, fontSize = 19.sp, color = Moni.inkSoft,
            modifier = Modifier.align(Alignment.TopEnd).pressable(onOpenSettings).padding(4.dp))
    }
}

@Composable
private fun MetaRow(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Moni.inkSoft)
        Text(right, fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Moni.inkSoft)
    }
}

@Composable
private fun TotalBlock(amount: Double, count: Int) {
    val word = if (count == 1) "gasto" else "gastos"
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("★ TOTAL A COBRAR ★", fontFamily = Moni.font, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp, letterSpacing = 4.sp, color = Moni.ink)
        Text(fmt(amount), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 50.sp, color = Moni.accent, letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(vertical = 7.dp))
        Text("PENDIENTE POR COBRAR · $count $word", fontFamily = Moni.font, fontSize = 11.sp,
            letterSpacing = 1.5.sp, color = Moni.inkSoft)
        Rule(RuleVariant.Double, modifier = Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp))
    }
}

@Composable
private fun MarkAllButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .pressable(onClick)
            .border(1.5.dp, Moni.paid, RoundedCornerShape(9.dp))
            .background(Moni.paidWash, RoundedCornerShape(9.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓", fontFamily = Moni.font, fontSize = 16.sp, color = Moni.paid)
        Spacer(Modifier.width(9.dp))
        Text("MARCAR TODO COMO DEVUELTO", fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp, letterSpacing = 1.5.sp, color = Moni.paid)
    }
}

@Composable
private fun SectionHeader(
    mark: String, label: String, count: Int, subtotal: Double?, accent: Color,
    open: Boolean, onToggle: () -> Unit,
) {
    val rot by animateFloatAsState(if (open) 0f else -90f, label = "chevron")
    Row(
        Modifier.fillMaxWidth().pressable(onToggle).padding(top = 11.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mark, fontFamily = Moni.font, fontSize = 15.sp, color = accent,
            textAlign = TextAlign.Center, modifier = Modifier.width(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label.uppercase(), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, letterSpacing = 2.sp, color = Moni.ink)
        Spacer(Modifier.width(8.dp))
        Text("($count)", fontFamily = Moni.font, fontSize = 12.sp, color = Moni.inkFaint)
        Spacer(Modifier.weight(1f))
        if (subtotal != null) {
            Text(fmt(subtotal), fontFamily = Moni.font, fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp, color = accent)
            Spacer(Modifier.width(8.dp))
        }
        Text("▾", fontFamily = Moni.font, fontSize = 15.sp, color = Moni.inkSoft,
            modifier = Modifier.width(18.dp).rotate(rot), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReceiptCardRow(card: ReceiptCard, share: Double, onOpen: () -> Unit) {
    val ret = card.returnAmount(share)
    val merchant = card.merchant?.takeIf { it.isNotBlank() } ?: "Sin nombre"
    val dateText = card.dateMillis?.let { fmtShort(it.toLocalDate()) } ?: "—"
    Column(
        Modifier.fillMaxWidth().pressable(onOpen).padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            CategoryTag(categoryCode(card.category))
            Spacer(Modifier.width(9.dp))
            Text(
                merchant, fontFamily = Moni.font, fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp, color = Moni.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(9.dp))
            Text(dateText, fontFamily = Moni.font, fontSize = 12.sp, color = Moni.inkFaint)
        }
        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(39.dp))
            Text(card.category, fontFamily = Moni.font, fontSize = 12.sp, color = Moni.inkSoft,
                modifier = Modifier.weight(1f))
            Text(fmt(card.total ?: 0.0), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, color = Moni.ink)
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
            Spacer(Modifier.width(39.dp))
            Text("» a devolver (${(100 * share).toInt()}%)", fontFamily = Moni.font, fontSize = 11.5.sp,
                color = Moni.accent, modifier = Modifier.weight(1f))
            Text(fmt(ret), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp, color = Moni.accent)
        }
    }
}

@Composable
private fun MonthRow(key: String, rollup: MonthlyRollup) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(monthLabel(key), fontFamily = Moni.font, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = Moni.ink)
            Text("${rollup.count} ${if (rollup.count == 1) "gasto" else "gastos"} · devuelto ${fmt(rollup.refund)}",
                fontFamily = Moni.font, fontSize = 11.sp, color = Moni.inkSoft,
                modifier = Modifier.padding(top = 2.dp))
        }
        Text(fmt(rollup.total), fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, color = Moni.inkSoft)
    }
}

/** "2026-06" → "Junio 2026". Falls back to the raw key if it can't be parsed. */
private fun monthLabel(key: String): String {
    val parts = key.split("-")
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return key
    val year = parts.getOrNull(0) ?: return key
    return "${MON_FULL.getOrElse(month - 1) { key }} $year"
}

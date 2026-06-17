package com.devsoto.monical.ui.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.ui.components.Rule
import com.devsoto.monical.ui.components.pressable
import com.devsoto.monical.ui.theme.Moni
import kotlin.math.roundToInt

private val NIGHT = Color(0xFF1A1712)
private val LIGHT_INK = Color(0xFFEDE4D0)
private val BAR_LIGHT = Color(0xFFD9D2C2)
private val BAR_DARK = Color(0xFFBDB4A0)

/**
 * "Analizando" screen. The image was already captured from the Home sheet (camera or gallery), so
 * this screen only shows the scanning animation during [ScanPhase.PROCESSING] over a featureless
 * receipt placeholder, plus an error state if OCR/parsing fails.
 */
@Composable
fun ScanScreen(viewModel: ScanViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val processing = uiState.phase == ScanPhase.PROCESSING

    Column(modifier.fillMaxSize().background(NIGHT)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✕", fontFamily = Moni.font, fontSize = 22.sp, color = LIGHT_INK,
                modifier = Modifier.pressable(viewModel::reset))
            Spacer(Modifier.width(12.dp))
            Text("ESCANEAR TICKET", fontFamily = Moni.font, fontSize = 13.sp,
                letterSpacing = 2.sp, color = LIGHT_INK)
        }

        val boxHeightPx = remember { mutableStateOf(0) }
        Box(
            Modifier.weight(1f).fillMaxWidth().onSizeChanged { boxHeightPx.value = it.height },
            contentAlignment = Alignment.Center,
        ) {
            MockTicket()

            if (processing) {
                val transition = rememberInfiniteTransition(label = "beam")
                val frac by transition.animateFloat(
                    initialValue = 0.16f, targetValue = 0.70f,
                    animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
                    label = "beamY",
                )
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 30.dp)
                        .offset { IntOffset(0, ((frac - 0.5f) * boxHeightPx.value).roundToInt()) }
                        .height(3.dp).background(Moni.accent),
                )
            }

            Box(Modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
                if (processing) {
                    Row(
                        Modifier.background(Color(0xE62C2720), CircleShape).padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spinner()
                        Spacer(Modifier.width(9.dp))
                        Text("Analizando ticket…", fontFamily = Moni.font,
                            fontSize = 12.sp, color = LIGHT_INK)
                    }
                }
            }
        }

        if (!processing && uiState.error != null) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(uiState.error!!, fontFamily = Moni.font, fontSize = 12.sp, color = Moni.accent,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.pressable(viewModel::reset)
                        .border(1.5.dp, LIGHT_INK, RoundedCornerShape(10.dp))
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text("VOLVER", fontFamily = Moni.font, fontSize = 12.sp,
                        letterSpacing = 1.5.sp, color = LIGHT_INK)
                }
            }
        }
    }
}

/** Featureless receipt: gray placeholder bars standing in for text while scanning. */
@Composable
private fun MockTicket() {
    Column(
        Modifier.width(200.dp).rotate(-2f)
            .background(Color(0xFFF5EEDE), RoundedCornerShape(2.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        SkeletonBar(0.6f, 10.dp, BAR_LIGHT, Alignment.CenterHorizontally)
        Spacer(Modifier.height(6.dp))
        SkeletonBar(0.45f, 10.dp, BAR_LIGHT, Alignment.CenterHorizontally)
        Rule(modifier = Modifier.padding(vertical = 8.dp))
        listOf(0.5f, 0.6f, 0.45f, 0.55f).forEach { w ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.fillMaxWidth(w).height(7.dp).background(BAR_LIGHT, RoundedCornerShape(3.dp)))
                Box(Modifier.fillMaxWidth(0.18f).height(7.dp).background(BAR_LIGHT, RoundedCornerShape(3.dp)))
            }
        }
        Rule(modifier = Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.fillMaxWidth(0.25f).height(9.dp).background(BAR_DARK, RoundedCornerShape(3.dp)))
            Box(Modifier.fillMaxWidth(0.22f).height(9.dp).background(BAR_DARK, RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(10.dp))
        SkeletonBar(0.5f, 6.dp, BAR_LIGHT, Alignment.CenterHorizontally)
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float, height: Dp, color: Color, align: Alignment.Horizontal) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = when (align) {
            Alignment.End -> Alignment.CenterEnd
            Alignment.CenterHorizontally -> Alignment.Center
            else -> Alignment.CenterStart
        },
    ) {
        Box(Modifier.fillMaxWidth(widthFraction).height(height).background(color, RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart), label = "spinA",
    )
    Box(Modifier.size(14.dp).rotate(angle).border(2.dp, Moni.accent, CircleShape)) {
        Box(Modifier.align(Alignment.TopCenter).size(4.dp).background(Color(0xE62C2720)))
    }
}

package com.devsoto.monical.ui.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devsoto.monical.ui.capture.createImageUri
import com.devsoto.monical.ui.components.Rule
import com.devsoto.monical.ui.components.pressable
import com.devsoto.monical.ui.theme.Moni

private val NIGHT = Color(0xFF1A1712)
private val LIGHT_INK = Color(0xFFEDE4D0)

/**
 * Capture screen styled like a viewfinder. The camera itself is the system [TakePicture] intent,
 * so there's no live preview; the receipt-paper mock is a placeholder and the scanning animation
 * plays during the real [ScanPhase.PROCESSING] phase (OCR + Gemini).
 */
@Composable
fun ScanScreen(viewModel: ScanViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val processing = uiState.phase == ScanPhase.PROCESSING

    val pendingCameraUri = remember { arrayOfNulls<Uri>(1) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri[0]
        if (success && uri != null) viewModel.processImage(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.processImage(uri)
    }

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

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MockTicket()
            if (!processing) CornerBrackets()

            if (processing) {
                val transition = rememberInfiniteTransition(label = "beam")
                val frac by transition.animateFloat(
                    initialValue = 0.16f, targetValue = 0.70f,
                    animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
                    label = "beamY",
                )
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 30.dp)
                        .offset(y = maxHeight * (frac - 0.5f))
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
                        Text("Analizando · ML Kit + Gemini…", fontFamily = Moni.font,
                            fontSize = 12.sp, color = LIGHT_INK)
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Text(error, fontFamily = Moni.font, fontSize = 12.sp, color = Moni.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp))
        }

        Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
            if (!processing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(70.dp)
                            .pressable({
                                val uri = createImageUri(context)
                                pendingCameraUri[0] = uri
                                cameraLauncher.launch(uri)
                            })
                            .border(4.dp, LIGHT_INK, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(54.dp).background(Moni.accent, CircleShape))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("ELEGIR DE LA GALERÍA", fontFamily = Moni.font, fontSize = 11.sp,
                        letterSpacing = 1.5.sp, color = LIGHT_INK,
                        modifier = Modifier.pressable({
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }).padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MockTicket() {
    Column(
        Modifier.width(200.dp).rotate(-2f)
            .background(Color(0xFFF5EEDE), RoundedCornerShape(2.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Text("ABARROTES", fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            letterSpacing = 1.sp, color = Color(0xFF2C2720), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Text("LA ESQUINA", fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            letterSpacing = 1.sp, color = Color(0xFF2C2720), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
        Rule(modifier = Modifier.padding(vertical = 8.dp))
        listOf("Leche 1L" to "28.00", "Pan caja" to "46.50", "Huevo 18p" to "79.00", "Detergente" to "131.00")
            .forEach { (name, price) ->
                Row(Modifier.fillMaxWidth().padding(bottom = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontFamily = Moni.font, fontSize = 10.sp, color = Color(0xFF2C2720))
                    Text(price, fontFamily = Moni.font, fontSize = 10.sp, color = Color(0xFF2C2720))
                }
            }
        Rule(modifier = Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL", fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2C2720))
            Text("284.50", fontFamily = Moni.font, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2C2720))
        }
        Text("GRACIAS POR SU COMPRA", fontFamily = Moni.font, fontSize = 8.sp, color = Moni.inkSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun CornerBrackets() {
    Box(Modifier.size(220.dp)) {
        val br = 3.dp
        listOf(
            Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd,
        ).forEach { align ->
            Box(Modifier.align(align).size(30.dp)) {
                val top = align == Alignment.TopStart || align == Alignment.TopEnd
                val start = align == Alignment.TopStart || align == Alignment.BottomStart
                Box(Modifier.fillMaxWidth().height(br).align(if (top) Alignment.TopStart else Alignment.BottomStart).background(Moni.accent))
                Box(Modifier.width(br).fillMaxSize().align(if (start) Alignment.TopStart else Alignment.TopEnd).background(Moni.accent))
            }
        }
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

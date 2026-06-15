package com.devsoto.monical.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devsoto.monical.ui.theme.Moni

/** Press wrapper — replicates the prototype's `:active { scale(.972) }`. */
@Composable
fun Modifier.pressable(onClick: () -> Unit, scale: Float = 0.972f): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, label = "press")
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = interaction, indication = null) { onClick() }
}

/** Dashed / dotted / double rule. */
enum class RuleVariant { Dash, Dot, Double }

@Composable
fun Rule(
    variant: RuleVariant = RuleVariant.Dash,
    color: Color = Moni.rule,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(if (variant == RuleVariant.Double) 3.dp else 2.dp)
    ) {
        val w = size.width
        val stroke = 1.5.dp.toPx()
        when (variant) {
            RuleVariant.Double -> {
                drawLine(color, Offset(0f, 0f), Offset(w, 0f), stroke)
                drawLine(color, Offset(0f, size.height), Offset(w, size.height), stroke)
            }
            RuleVariant.Dot -> drawLine(
                color, Offset(0f, 0f), Offset(w, 0f), stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 4f)),
            )
            RuleVariant.Dash -> drawLine(
                color, Offset(0f, 0f), Offset(w, 0f), stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
            )
        }
    }
}

/** Ink stamp. */
@Composable
fun Stamp(
    text: String,
    color: Color = Moni.accent,
    angle: Float = -8f,
    fontSize: Int = 13,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .rotate(angle)
            .graphicsLayer { alpha = 0.92f }
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text.uppercase(),
            color = color,
            fontFamily = Moni.font,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

/** Torn paper edge. */
@Composable
fun TornEdge(
    top: Boolean,
    color: Color = Moni.paper,
    modifier: Modifier = Modifier,
    height: Dp = 11.dp,
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val teeth = 24
        val toothW = size.width / (teeth * 2)
        val h = size.height
        val path = Path().apply {
            val base = if (top) h else 0f
            moveTo(0f, base)
            for (i in 0..teeth * 2) {
                val y = if (top) (if (i % 2 == 0) 0f else h) else (if (i % 2 == 0) h else 0f)
                lineTo(i * toothW, y)
            }
            lineTo(size.width, base)
            close()
        }
        drawPath(path, color)
    }
}

/** A teller-code chip e.g. [COM]. */
@Composable
fun CategoryTag(
    code: String,
    textColor: Color = Moni.inkSoft,
    border: Color = Moni.inkFaint,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            code, fontFamily = Moni.font, fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp, letterSpacing = 1.sp, color = textColor,
        )
    }
}

/** Small calculator glyph (rect + dot grid). */
@Composable
fun CalcGlyph(size: Dp = 18.dp, color: Color = Color.White) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.075f
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.167f, s * 0.104f),
            size = Size(s * 0.667f, s * 0.79f),
            cornerRadius = CornerRadius(s * 0.1f),
            style = Stroke(stroke),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.29f, s * 0.23f),
            size = Size(s * 0.42f, s * 0.167f),
            cornerRadius = CornerRadius(s * 0.04f),
        )
        val xs = listOf(0.34f, 0.5f, 0.66f)
        val ys = listOf(0.54f, 0.71f)
        for (y in ys) for (x in xs) drawCircle(color, s * 0.046f, Offset(s * x, s * y))
    }
}

/** Barcode footer flourish. */
@Composable
fun Barcode(code: String = "0 24061 40000 6") {
    val bars = remember {
        var seed = 7
        IntArray(58) { seed = (seed * 9301 + 49297) % 233280; 1 + (seed % 4) }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            modifier = Modifier.height(46.dp),
        ) {
            bars.forEachIndexed { i, w ->
                Box(
                    Modifier
                        .width(w.dp)
                        .height(if (i % 7 == 0) 46.dp else 40.dp)
                        .background(if (i % 5 == 2) Color.Transparent else Moni.ink)
                )
            }
        }
        Text(code, fontFamily = Moni.font, fontSize = 11.sp, letterSpacing = 3.sp, color = Moni.inkSoft)
    }
}

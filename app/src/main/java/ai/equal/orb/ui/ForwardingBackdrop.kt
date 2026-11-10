package ai.equal.orb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The scene behind call forwarding while Equal sets it up: a deep green night with two slow aurora
 * glows and soft bokeh rising through it, while faint “incoming call” pills drift in from both sides
 * towards the middle (where the orb is) and fade out — the calls that will now come to Equal.
 * Drawn live; nothing to load.
 */
@Composable
fun ForwardingBackdrop(modifier: Modifier = Modifier) {
    val clock = rememberWallClock()
    // The glows and bokeh move slowly enough that 30 redraws a second look the same as every frame.
    val t by remember { derivedStateOf { kotlin.math.floor(clock.value * 30f) / 30f } }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to hex(0x03110A), 0.5f to hex(0x082A19), 1f to hex(0x020C07))),
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            // Aurora: two big soft pools drifting slowly.
            for ((i, c) in listOf(hex(0x00B140), hex(0x2BC6A0)).withIndex()) {
                val a = 2 * PI.toFloat() * t / (24f + 6 * i) + i * 2f
                val center = Offset(w * (0.3f + 0.4f * i + 0.12f * sin(a)), h * (0.32f + 0.3f * i + 0.06f * sin(a * 1.3f)))
                val r = w * (0.85f + 0.08f * sin(a * 0.7f))
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.32f), Color.Transparent), center, r), r, center)
            }
            // Bokeh rising, each on its own loop.
            for (b in bokeh) {
                val p = ((t / b.period + b.phase) % 1f)
                val y = h * (1.1f - 1.25f * p)
                val x = w * b.x + 18.dp.toPx() * sin(2 * PI.toFloat() * (t / (b.period * 0.6f) + b.phase))
                val fade = sin(PI.toFloat() * p)
                val r = b.size.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(hex(b.color, b.alpha * fade), Color.Transparent), Offset(x, y), r),
                    r, Offset(x, y),
                )
            }
        }
        // Calls drifting in towards the middle.
        for (c in calls) {
            CallPill(
                c.label,
                Modifier.align(Alignment.TopStart).graphicsLayer {
                    val p = ((clock.value / c.period + c.phase) % 1f)
                    val fromLeft = c.fromLeft
                    // Starts off the edge, glides most of the way to the middle, easing as it goes.
                    val eased = 1 - (1 - p) * (1 - p)
                    val startX = if (fromLeft) -size.width else w
                    val endX = w / 2 - size.width / 2 + (if (fromLeft) -40.dp.toPx() else 40.dp.toPx())
                    translationX = startX + (endX - startX) * eased
                    translationY = h * c.y - size.height / 2 + 10.dp.toPx() * sin(2 * PI.toFloat() * p)
                    val s = 1f - 0.35f * eased
                    scaleX = s; scaleY = s
                    alpha = 0.85f * sin(PI.toFloat() * p).coerceAtLeast(0f) * (1 - 0.4f * eased)
                },
            )
        }
    }
}

/** A faint glass pill: a green call glyph and who's calling. */
@Composable
private fun CallPill(label: String, modifier: Modifier) {
    Row(
        modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(26.dp).background(EQ.green, CircleShape), contentAlignment = Alignment.Center) {
            Txt("📞", dm(12f))
        }
        Txt(label, dm(14f, FontWeight.Medium, Color.White.copy(alpha = 0.9f)), Modifier.padding(start = 8.dp))
    }
}

private class Bokeh(val x: Float, val size: Float, val period: Float, val phase: Float, val color: Long, val alpha: Float)

private val bokeh = Random(11).let { rng ->
    val colors = listOf(0xBAFF29L, 0x8CEA35L, 0x00B140L, 0x4FD6B8L)
    List(14) {
        Bokeh(
            x = rng.nextFloat(), size = 10f + rng.nextFloat() * 26f, period = 9f + rng.nextFloat() * 9f,
            phase = rng.nextFloat(), color = colors[it % colors.size], alpha = 0.25f + rng.nextFloat() * 0.3f,
        )
    }
}

private class DriftingCall(val label: String, val fromLeft: Boolean, val y: Float, val period: Float, val phase: Float)

private val calls = listOf(
    DriftingCall("Unknown number", true, 0.16f, 7.0f, 0.0f),
    DriftingCall("Delivery rider", false, 0.24f, 7.8f, 0.35f),
    DriftingCall("Bank", true, 0.70f, 8.4f, 0.6f),
    DriftingCall("Courier", false, 0.78f, 7.4f, 0.15f),
    DriftingCall("Clinic", true, 0.86f, 9.0f, 0.8f),
)

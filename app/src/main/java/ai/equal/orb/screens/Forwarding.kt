package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Step
import ai.equal.orb.ui.CelebrationBurst
import ai.equal.orb.ui.CheckBadge
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.afterOrb
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.rememberTalk
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.cos
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.math.PI
import ai.equal.orb.ui.rememberWallClock
import ai.equal.orb.ui.selectableLift
import ai.equal.orb.ui.glassCardFace
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.State

/** A light circling the orb: its orbit (dp, tilted ellipse), speed, size and colour. */
private class Orbiter(val rx: Float, val ry: Float, val tilt: Float, val period: Float, val phase: Float, val size: Float, val color: Long)

private val orbiters = listOf(
    Orbiter(132f, 52f, -20f, 3.0f, 0.0f, 9f, 0x00B140),
    Orbiter(118f, 64f, 28f, 3.7f, 2.1f, 8f, 0x8CEA35),
    Orbiter(146f, 44f, 6f, 4.4f, 4.0f, 7f, 0x4FD6B8),
    Orbiter(108f, 72f, -48f, 3.3f, 1.2f, 6.5f, 0xBAFF29),
    Orbiter(138f, 58f, 58f, 4.0f, 3.3f, 7.5f, 0x22C76A),
)

/** How long Equal takes to turn call forwarding on (it's done for the user; this is just the wait). */
private const val SETUP_MS = 4200L

/**
 * Call forwarding, which Equal turns on by itself on Android, as a calm loading moment over a dark
 * scene of calls drifting in ([ai.equal.orb.ui.ForwardingBackdrop], behind the page, from the app's root). The big orb asks, its dots turning; a few soft lights circle it on tilted orbits while a glow breathes behind
 * it. When it's on, the lights fold into the orb, confetti bursts, the orb says so, and the flow
 * moves on.
 */
@Composable
fun AccessScreen(model: AppModel) {
    val talk = rememberTalk()
    val title = Catalog.title(Catalog.Ask.Access)
    /** The lights coming out round the orb (0 → 1), and folding back into it when done (→ 0). */
    val spread = remember { Animatable(0f) }
    var burst by remember { mutableIntStateOf(0) }
    val clock = rememberWallClock()

    // Where the orb is, in this screen's coordinates.
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var orbCenter by remember { mutableStateOf<Offset?>(null) }
    val orbRadius = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(Unit) {
        talk.intro(title)
        while (!talk.said) delay(50)
        if (!model.fwdDone) {
            spread.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 60f))
            delay(SETUP_MS)
            spread.animateTo(0f, tween(450, easing = EQ.push))
            model.fwdDone = true
            Haptics.success()
            burst++
        }
        talk.replace(Catalog.forwardingDone)
        delay(1300)
        if (model.step == Step.Access) model.next()
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { root = it }) {
        val center = orbCenter
        val breathe = rememberInfiniteTransition(label = "breathe")
        val b by breathe.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = EQ.ease), RepeatMode.Reverse), label = "b")

        // Behind the orb: a soft glow that breathes while it works, and the lights passing behind it.
        if (center != null) Canvas(Modifier.fillMaxSize()) {
            val s = spread.value
            if (s > 0f) {
                val r = orbRadius * (1.9f + 0.35f * b)
                drawCircle(
                    Brush.radialGradient(
                        0f to hex(0xBAFF29, (0.45f + 0.15f * b) * s), 0.45f to EQ.green.copy(alpha = 0.16f * s), 1f to Color.Transparent,
                        center = center, radius = r,
                    ),
                    r, center,
                )
            }
            drawOrbiters(center, clock.value, s, behind = true)
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            OrbWithBubble(
                talk, title,
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).onGloballyPositioned { c ->
                    // The orb sits at the bottom of the block, centred.
                    val r = root ?: return@onGloballyPositioned
                    orbCenter = r.localPositionOf(c, Offset(c.size.width / 2f, c.size.height - orbRadius))
                },
                spinning = !model.fwdDone, rings = false,
            )
            Spacer(Modifier.weight(1f))
            StatusLine(
                if (model.fwdDone) "Call forwarding is on" else "Setting up call forwarding…",
                done = model.fwdDone,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp).afterOrb(talk, index = 1),
            )
        }
        if (center != null) {
            // The lights passing in front of the orb.
            Canvas(Modifier.fillMaxSize()) { drawOrbiters(center, clock.value, spread.value, behind = false) }
            // Confetti from the orb once it's on.
            CelebrationBurst(
                burst,
                Modifier.size(240.dp).offset { IntOffset((center.x - 120.dp.toPx()).roundToInt(), (center.y - 120.dp.toPx()).roundToInt()) },
                spread = 110.dp,
            )
        }
    }
}

/**
 * The lights on their tilted orbits round [center], scaled out by [spread]. Each orbit's far half
 * passes behind the orb ([behind]) and its near half in front, so they read as circling it.
 */
private fun DrawScope.drawOrbiters(center: Offset, t: Float, spread: Float, behind: Boolean) {
    if (spread <= 0f) return
    val k = spread.coerceIn(0f, 1f)
    for (o in orbiters) {
        val head = 2 * PI.toFloat() * t / o.period + o.phase
        val tilt = o.tilt * PI.toFloat() / 180f
        val color = hex(o.color)
        fun at(a: Float) = run {
            val x0 = cos(a) * o.rx * spread
            val y0 = sin(a) * o.ry * spread
            Offset(center.x + (x0 * cos(tilt) - y0 * sin(tilt)).dp.toPx(), center.y + (x0 * sin(tilt) + y0 * cos(tilt)).dp.toPx())
        }
        // A comet's tail behind each light, fading along its orbit.
        for (j in 14 downTo 1) {
            val a = head - j * 0.07f
            if ((sin(a) > 0) == behind) continue
            val f = 1 - j / 15f
            drawCircle(color.copy(alpha = 0.35f * f * k), o.size.dp.toPx() * (0.25f + 0.55f * f) * (0.75f + 0.25f * sin(a)), at(a))
        }
        if ((sin(head) > 0) == behind) continue
        val p = at(head)
        // Nearer the viewer: a touch bigger and brighter.
        val depth = 0.75f + 0.25f * sin(head)
        val r = o.size.dp.toPx() * depth
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * k), Color.Transparent), p, r * 3.2f), r * 3.2f, p)
        drawCircle(color.copy(alpha = k * depth), r, p)
        drawCircle(Color.White.copy(alpha = 0.8f * k), r * 0.38f, p)
    }
}

/** What's happening, in a soft frosted capsule over the dark scene: a spinner while working, a tick once on. */
@Composable
private fun StatusLine(text: String, done: Boolean, modifier: Modifier) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
    Row(
        modifier.fillMaxWidth().height(56.dp).background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (done) Txt("✓", dm(18f, FontWeight.Bold, EQ.lime))
        else Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = angle }) {
            val w = 2.5.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.25f), radius = size.minDimension / 2 - w / 2, style = Stroke(w))
            drawArc(EQ.lime, -90f, 100f, false, topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Spacer(Modifier.size(10.dp))
        Txt(text, dm(16f, FontWeight.SemiBold, Color.White))
    }
}

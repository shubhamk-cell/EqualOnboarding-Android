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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One case call forwarding covers: its icon, name and what it means. */
private class FwdCase(val emoji: String, val title: String, val detail: String)

private val cases = listOf(
    FwdCase("📞", "Busy", "On another call"),
    FwdCase("🔔", "No answer", "It rings out"),
    FwdCase("📵", "Unreachable", "Off or no signal"),
)

/**
 * Call forwarding, which Equal turns on by itself on Android. The big orb asks, its dots turning
 * while it works; under it, the three cases light up one at a time, and as each comes on a call
 * flies out of its card into the orb, the orb pulses and the ring round it fills a third. When all
 * three are on, the orb celebrates, says so, and the flow moves on.
 */
@Composable
fun AccessScreen(model: AppModel) {
    val talk = rememberTalk()
    val title = Catalog.title(Catalog.Ask.Access)
    /** The case being turned on (−1: none). */
    var working by remember { mutableIntStateOf(-1) }
    /** The ring round the orb, 0 → 1 as the cases come on. */
    val ring = remember { Animatable(0f) }
    /** Each case's call flying into the orb, 0 → 1. */
    val flights = remember { List(cases.size) { Animatable(0f) } }
    /** A ripple off the orb as a call lands. */
    val pulse = remember { Animatable(1f) }
    var burst by remember { mutableIntStateOf(0) }

    // Where things are, in this screen's coordinates.
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var orbCenter by remember { mutableStateOf<Offset?>(null) }
    val cardTops = remember { mutableStateMapOf<Int, Offset>() }
    val density = LocalDensity.current
    val orbRadius = with(density) { 64.dp.toPx() }

    LaunchedEffect(Unit) {
        talk.intro(title)
        while (!talk.said) delay(50)
        if (!model.fwdDone) {
            for (i in model.forwarded.indices) model.forwarded[i] = false
            delay(500)
            for (i in cases.indices) {
                working = i
                Haptics.select()
                delay(800)
                // Done: its call flies into the orb.
                launch { flights[i].snapTo(0f); flights[i].animateTo(1f, tween(620, easing = EQ.push)) }
                delay(560)
                model.forwarded[i] = true
                Haptics.select()
                launch { ring.animateTo((i + 1f) / cases.size, spring(dampingRatio = 0.8f, stiffness = 120f)) }
                launch { pulse.snapTo(0f); pulse.animateTo(1f, tween(700)) }
                delay(300)
            }
            working = -1
            model.fwdDone = true
            Haptics.success()
            burst++
        } else ring.snapTo(1f)
        talk.replace(Catalog.forwardingDone)
        delay(1300)
        if (model.step == Step.Access) model.next()
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { root = it }) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            OrbWithBubble(
                talk, title,
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).onGloballyPositioned { c ->
                    // The orb sits at the bottom of the block, centred.
                    val r = root ?: return@onGloballyPositioned
                    orbCenter = r.localPositionOf(c, Offset(c.size.width / 2f, c.size.height - orbRadius))
                },
                spinning = !model.fwdDone,
            )
            Spacer(Modifier.height(44.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).afterOrb(talk, index = 1),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cases.forEachIndexed { i, c ->
                    val done = model.forwarded[i] || model.fwdDone
                    CaseCard(
                        c, working = working == i, done = done,
                        modifier = Modifier.weight(1f).onGloballyPositioned { lc ->
                            val r = root ?: return@onGloballyPositioned
                            cardTops[i] = r.localPositionOf(lc, Offset(lc.size.width / 2f, 0f))
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1.2f))
            StatusLine(
                when {
                    model.fwdDone -> "Call forwarding is on"
                    working >= 0 -> "Turning on ${working + 1} of ${cases.size}…"
                    else -> "Setting up call forwarding…"
                },
                done = model.fwdDone,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp).afterOrb(talk, index = 2),
            )
        }

        // Over everything: the ring round the orb, its ripple, and the calls flying in.
        val center = orbCenter
        if (center != null) {
            Canvas(Modifier.fillMaxSize()) {
                val r = orbRadius + 14.dp.toPx()
                val w = 4.dp.toPx()
                val topLeft = Offset(center.x - r, center.y - r)
                drawCircle(EQ.green.copy(alpha = 0.12f), r, center, style = Stroke(w))
                if (ring.value > 0f) drawArc(
                    EQ.green, -90f, 360f * ring.value, false, topLeft, Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round),
                )
                if (pulse.value < 1f) {
                    val p = pulse.value
                    drawCircle(EQ.green.copy(alpha = 0.35f * (1 - p)), r + 26.dp.toPx() * p, center, style = Stroke(3.dp.toPx() * (1 - p) + 1f))
                }
                flights.forEachIndexed { i, f ->
                    val t = f.value
                    val start = cardTops[i] ?: return@forEachIndexed
                    if (t <= 0f || t >= 1f) return@forEachIndexed
                    // A curve that bows outward on the side cards, ending in the orb's middle.
                    val ctrl = Offset((start.x + center.x) / 2 + (start.x - center.x) * 0.6f, (start.y + center.y) / 2)
                    fun at(u: Float): Offset {
                        val a = (1 - u) * (1 - u); val b = 2 * (1 - u) * u; val c = u * u
                        return Offset(a * start.x + b * ctrl.x + c * center.x, a * start.y + b * ctrl.y + c * center.y)
                    }
                    // A short fading trail behind the call.
                    for (k in 6 downTo 1) {
                        val u = (t - k * 0.035f).coerceAtLeast(0f)
                        drawCircle(EQ.green.copy(alpha = 0.10f * (7 - k)), (7 - k * 0.6f).dp.toPx(), at(u))
                    }
                    val head = at(t)
                    val fade = if (t > 0.85f) (1 - t) / 0.15f else 1f
                    drawCircle(hex(0xBAFF29, 0.5f * fade), 14.dp.toPx(), head)
                    drawCircle(EQ.green.copy(alpha = fade), 8.dp.toPx(), head)
                    drawCircle(Color.White.copy(alpha = fade), 3.dp.toPx(), head)
                }
            }
            // Confetti from the orb once all three are on.
            Box(Modifier.offset { IntOffset(center.x.roundToInt(), center.y.roundToInt()) }) {
                CelebrationBurst(burst, Modifier.requiredSize(240.dp), spread = 110.dp)
            }
        }
    }
}

/** One case: its icon in a tinted circle, name and meaning. Dim while waiting, pulsing green while it's being turned on, mint with a tick once on. */
@Composable
private fun CaseCard(case: FwdCase, working: Boolean, done: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    val glow = rememberInfiniteTransition(label = "glow")
    val beat by glow.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "beat")
    val lift by animateFloatAsState(if (working) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "lift")
    val fill by animateColorAsState(if (done) hex(0xEAF8EE) else Color.White, tween(300), label = "fill")
    val edge = when {
        done -> EQ.green.copy(alpha = 0.45f)
        working -> EQ.green.copy(alpha = 0.35f + 0.5f * beat)
        else -> EQ.edge
    }
    val active = working || done
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(124.dp)
                .graphicsLayer {
                    val s = 1 + 0.05f * lift
                    scaleX = s; scaleY = s
                    translationY = -4 * density * lift
                    alpha = if (active) 1f else 0.6f
                }
                .clip(shape)
                .background(fill, shape)
                .border(if (working) 2.dp else 1.dp, edge, shape)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(46.dp).background(if (active) EQ.green.copy(alpha = 0.14f) else hex(0xF1F3F2), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // The icon gives a little shake while its case is being turned on.
                Txt(case.emoji, dm(22f), Modifier.graphicsLayer { rotationZ = if (working) (beat - 0.5f) * 24f else 0f })
            }
            Spacer(Modifier.height(10.dp))
            Txt(case.title, dm(15f, FontWeight.SemiBold, EQ.ink, align = TextAlign.Center), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Txt(case.detail, dm(12f, color = EQ.secondary, align = TextAlign.Center, lineHeight = 15f), maxLines = 2)
        }
        CheckBadge(done, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
    }
}

/** What's happening, in a soft capsule at the bottom: a spinner while working, a tick once on. */
@Composable
private fun StatusLine(text: String, done: Boolean, modifier: Modifier) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
    Row(
        modifier.fillMaxWidth().height(56.dp).background(EQ.green.copy(alpha = 0.1f), RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (done) Txt("✓", dm(18f, FontWeight.Bold, EQ.green))
        else Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = angle }) {
            val w = 2.5.dp.toPx()
            drawCircle(EQ.green.copy(alpha = 0.2f), radius = size.minDimension / 2 - w / 2, style = Stroke(w))
            drawArc(EQ.green, -90f, 100f, false, topLeft = Offset(w / 2, w / 2),
                size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
        }
        Spacer(Modifier.size(10.dp))
        Txt(text, dm(16f, FontWeight.SemiBold, EQ.greenText))
    }
}

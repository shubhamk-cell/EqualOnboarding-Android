package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Catalog
import ai.equal.orb.ui.CheckBadge
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.glassCardFace
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.selectableLift
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.aspectRatio

/** A SIM in the phone: its slot, operator, number and the operator's colour (for its logo and band). */
private class SimCard(val slot: Int, val operator: String, val number: String, val brand: Long, val logo: String)

/**
 * Before call forwarding: which SIM Equal answers on. The orb asks; the phone's two SIMs rise in
 * side by side as square glass cards. Tapping one picks it; “Activate on this SIM” then moves on.
 * (A prototype: SIM 1 carries the number signed up with, SIM 2 is a pretend second SIM.)
 */
@Composable
fun SimScreen(model: AppModel) {
    val talk = rememberTalk()
    val title = Catalog.title(Catalog.Ask.Sim)
    val sims = remember(model.phone) {
        listOf(
            SimCard(1, "Jio", model.phone.ifBlank { "9876543210" }, 0x0F3CC9, "Jio"),
            SimCard(2, "Airtel", "8987470059", 0xE40000, "airtel"),
        )
    }
    /** 0 → 1 as the cards open under the orb (the orb glides up to make room). */
    val cards = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        talk.intro(title)
        // Once the orb has asked, the cards rise in.
        snapshotFlow { talk.typing }.collect { typing ->
            if (typing || cards.targetValue == 1f) return@collect
            delay(350)
            launch { cards.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 65f)) }
            delay(120)
            revealed = true
        }
    }

    val buttonAlpha by animateFloatAsState(if (revealed) 1f else 0f, tween(400, delayMillis = 400), label = "cta")
    val p = cards.value

    Column(Modifier.fillMaxSize()) {
        // Before the cards open the orb sits in the middle of the screen.
        Spacer(Modifier.weight((1f - p).coerceAtLeast(0.0001f)))
        OrbWithBubble(talk, title, Modifier.fillMaxWidth().padding(top = 10.dp).padding(horizontal = 24.dp))
        if (p > 0f) Column(
            Modifier
                .padding(horizontal = 24.dp)
                .layout { m, c ->
                    val pl = m.measure(c)
                    val gap = 30.dp.roundToPx()
                    layout(pl.width, ((pl.height + gap) * p).roundToInt().coerceAtLeast(0)) { pl.place(0, gap) }
                }
                .graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = 40 * density * (1 - p) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                sims.forEachIndexed { i, s ->
                    SimOption(s, model.sim == i, Modifier.weight(1f).revealIn(revealed, i)) {
                        Haptics.select()
                        model.sim = i
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            model.nextCta, enabled = model.sim != null,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).graphicsLayer { alpha = buttonAlpha },
        ) { model.next() }
    }
}

/**
 * One SIM on a square glass card: the operator's logo, which slot it's in, the operator and the
 * number. Picked: like every other choice card — a light green fill, a green edge, a soft lift and a tick.
 */
@Composable
private fun SimOption(sim: SimCard, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.aspectRatio(1f)) {
        Column(
            Modifier
                .matchParentSize()
                .pressable(scale = 0.97f) { onClick() }
                .selectableLift(on, shape)
                .clip(shape)
                .glassCardFace(shape, on)
                .border(if (on) 1.5.dp else 1.dp, if (on) EQ.green else EQ.edge, shape)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Logo(sim, 56.dp)
            Txt("SIM ${sim.slot}", dm(13f, FontWeight.Medium, EQ.secondary), Modifier.padding(top = 12.dp))
            Txt(sim.operator, dm(18f, FontWeight.SemiBold, EQ.ink), Modifier.padding(top = 2.dp))
            Txt(sim.number, dm(14f, color = EQ.secondary, tracking = 0.3f), Modifier.padding(top = 2.dp), maxLines = 1)
        }
        CheckBadge(on, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
    }
}

/** The operator's mark: its name in white on a disc of its colour. */
@Composable
private fun Logo(sim: SimCard, size: Dp) {
    Box(Modifier.size(size).background(hex(sim.brand), CircleShape), contentAlignment = Alignment.Center) {
        Txt(sim.logo, dm(size.value * (if (sim.logo.length > 3) 0.24f else 0.36f), FontWeight.Bold, Color.White, tracking = -0.3f))
    }
}

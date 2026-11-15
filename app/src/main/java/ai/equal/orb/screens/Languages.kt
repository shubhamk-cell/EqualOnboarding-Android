package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.ui.BrickRow
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.OrderBadge
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.WrapRows
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.glassCardFace
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.selectableLift
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** How wide each option is in its row (like the picks: the pair splits the row unevenly). */
private val gridWeights = listOf(1.5f, 1f, 1f, 1.3f, 1.35f, 1f, 1f, 1.45f)

/**
 * The languages Equal answers in — both questions on one screen. The orb asks which languages the
 * user's callers speak and up to three are picked from a grid of uneven cards, numbered in the
 * order they were picked. As soon as there's one, a strip opens under the grid holding the picks as
 * pills, and that strip carries the second question: which one Equal greets callers in. It is never
 * answered for the user — nothing is picked to begin with, unpicking the greeting language doesn't
 * hand it to another, and the way on stays shut until they tap a pill themselves.
 */
@Composable
fun LanguagesScreen(model: AppModel) {
    val talk = rememberTalk()
    var revealed by remember { mutableStateOf(false) }
    /** 0 → 1 as the cards open under the orb (the orb glides up to make room). */
    val room = remember { Animatable(0f) }
    val title = Catalog.languagesAsk
    val atMax = model.languages.size >= CallLanguage.MAX_PICKED

    fun toggle(l: CallLanguage) {
        val on = l in model.languages
        if (!on && atMax) { Haptics.warning(); model.showToast("Pick up to 3 — tap one to swap it out"); return }
        Haptics.select()
        if (on) {
            model.languages.remove(l)
            // The greeting doesn't pass to another language: it goes back to being unanswered.
            if (model.primaryLanguage == l) model.primaryLanguage = null
        } else model.languages.add(l)
    }

    LaunchedEffect(Unit) {
        talk.intro(title)
        snapshotFlow { talk.typing }.collect { typing ->
            if (typing || room.targetValue == 1f) return@collect
            delay(350)
            launch { room.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 65f)) }
            delay(120)
            revealed = true
        }
    }

    val buttonAlpha by animateFloatAsState(if (revealed) 1f else 0f, tween(400, delayMillis = 400), label = "cta")
    val p = room.value

    Column(Modifier.fillMaxSize()) {
        // Before the cards open the orb sits in the middle of the screen.
        Spacer(Modifier.weight((1f - p).coerceAtLeast(0.0001f)))
        OrbWithBubble(talk, title, Modifier.fillMaxWidth().padding(top = 10.dp).padding(horizontal = 24.dp))
        if (p > 0f) Column(
            Modifier
                .padding(horizontal = 24.dp)
                .layout { m, c ->
                    val pl = m.measure(c)
                    val gap = 20.dp.roundToPx()
                    layout(pl.width, ((pl.height + gap) * p).roundToInt().coerceAtLeast(0)) { pl.place(0, gap) }
                }
                .graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = 40 * density * (1 - p) },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CallLanguage.options.chunked(2).forEachIndexed { r, row ->
                    BrickRow(row.indices.map { gridWeights[r * 2 + it] }, spacing = 10.dp, height = 62.dp) {
                        row.forEachIndexed { c, l ->
                            val on = l in model.languages
                            LanguageCard(
                                l, on, dimmed = atMax && !on,
                                place = model.languages.indexOf(l) + 1,
                                modifier = Modifier.revealIn(revealed, r * 2 + c),
                            ) { toggle(l) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // The second question sits with the way on, at the foot of the page, as soon as there's
        // anything to answer it with — not trailing the grid, where it would move with every pick.
        AnimatedVisibility(
            model.languages.isNotEmpty(),
            enter = fadeIn(tween(260)) + expandVertically(tween(320, easing = EQ.ease)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(260, easing = EQ.ease)),
        ) {
            GreetingStrip(model, Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp))
        }
        PrimaryButton(
            model.nextCta,
            // Nothing moves on until they've said which language to greet callers in.
            enabled = model.primaryLanguage != null,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).graphicsLayer { alpha = buttonAlpha },
        ) { model.next() }
    }
}

/**
 * At the foot of the page, just above the way on: the languages picked so far as pills, under the
 * question they answer. While it's unanswered the heading asks it, in Equal's deep green; once a
 * pill is tapped it says what Equal will do instead.
 */
@Composable
private fun GreetingStrip(model: AppModel, modifier: Modifier = Modifier) {
    val asking = model.primaryLanguage == null
    val shape = RoundedCornerShape(20.dp)
    val heading by animateColorAsState(
        if (asking) EQ.greenText else EQ.ink.copy(alpha = 0.75f), tween(200), label = "heading",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.5f), shape)
            .border(1.dp, EQ.edge, shape)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Txt(
            if (asking) Catalog.greetingAsk else Catalog.greetingSet,
            dm(13.5f, FontWeight.SemiBold, heading),
            maxLines = 1,
        )
        WrapRows(8.dp, centered = true) {
            model.languages.forEach { l ->
                GreetingPill(l, model.primaryLanguage == l) {
                    Haptics.select()
                    model.primaryLanguage = l
                }
            }
        }
    }
}

/** One picked language in the strip: its name, and a tick once it's the one callers are greeted in. */
@Composable
private fun GreetingPill(l: CallLanguage, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val fill by animateColorAsState(
        if (on) EQ.green.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.7f), tween(200), label = "pillFill",
    )
    val rim by animateColorAsState(if (on) EQ.green else EQ.edge, tween(200), label = "pillRim")
    val ink by animateColorAsState(if (on) EQ.greenDeep else EQ.ink.copy(alpha = 0.7f), tween(200), label = "pillInk")
    Row(
        Modifier
            .pressable(scale = 0.95f) { onClick() }
            .height(36.dp)
            .clip(shape)
            .background(fill, shape)
            .border(if (on) 1.3.dp else 1.dp, rim, shape)
            .padding(start = if (on) 6.dp else 13.dp, end = 13.dp)
            .semantics {
                contentDescription = "Greet callers in ${l.english}"
                selected = on
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The tick sits inside the pill, pushing the name along as it appears.
        AnimatedVisibility(on, enter = scaleIn(initialScale = 0.4f) + fadeIn(), exit = scaleOut(targetScale = 0.4f) + fadeOut()) {
            Box(Modifier.size(16.dp).background(EQ.green, CircleShape), contentAlignment = Alignment.Center) {
                Image(
                    rememberVectorPainter(Icons.Filled.Check), null,
                    Modifier.size(11.dp), colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
        Txt(l.label, dm(13.5f, if (on) FontWeight.SemiBold else FontWeight.Medium, ink), maxLines = 1)
    }
}

/**
 * One language: its name in its own script with the English name under it, and its own letter
 * large and faint behind, running off the card's edge. Picked: a light green fill, a green edge, a
 * soft lift and its place in the order. Once three are picked the rest fade back a little.
 */
@Composable
private fun LanguageCard(
    l: CallLanguage, on: Boolean, dimmed: Boolean, place: Int, modifier: Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val fade by animateFloatAsState(if (dimmed) 0.55f else 1f, tween(250), label = "dim")
    val nameColor by animateColorAsState(if (on) EQ.greenText else EQ.ink, tween(200), label = "name")
    val mark by animateColorAsState(
        if (on) EQ.green.copy(alpha = 0.14f) else EQ.ink.copy(alpha = 0.055f), tween(200), label = "mark",
    )
    Box(
        modifier
            .graphicsLayer { alpha = fade }
            .pressable(scale = 0.97f) { onClick() }
            .semantics {
                // The badge's number, said out loud: “Tamil, pick 2 of 3”.
                contentDescription = if (on) "${l.english}, pick $place of ${CallLanguage.MAX_PICKED}" else l.english
                selected = on
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .selectableLift(on, shape)
                .clip(shape)
                .glassCardFace(shape, on)
                .border(if (on) 1.5.dp else 1.dp, if (on) EQ.green else EQ.edge, shape),
        ) {
            // The language's own letter, large and faint, cut by the card's edge.
            Txt(
                l.initial, dm(60f, FontWeight.Bold, mark),
                Modifier.align(Alignment.CenterEnd).offset(x = 8.dp),
                maxLines = 1,
            )
            Column(
                Modifier.matchParentSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Txt(l.label, dm(17f, FontWeight.SemiBold, nameColor), maxLines = 1)
                if (l.label != l.english) Txt(l.english, dm(12.5f, color = EQ.secondary), maxLines = 1)
            }
        }
        OrderBadge(if (on) place else 0, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp))
    }
}

package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Step
import ai.equal.orb.ui.BrickRow
import ai.equal.orb.ui.CheckBadge
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.glassCardFace
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.selectableLift
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The two questions this page asks, one after the other. */
private enum class LangStep { Picking, Greeting }

/** How wide each option is in its row (like the picks: the pair splits the row unevenly). */
private val gridWeights = listOf(1.5f, 1f, 1f, 1.3f, 1.35f, 1f, 1f, 1.45f)

/** The picked cards growing into the greeting cards. */
private val cardBounds = { _: Rect, _: Rect -> spring(0.86f, 90f, Rect.VisibilityThreshold) }

/**
 * The languages Equal answers in, in two steps on one page (like the name, then the gender).
 * First the orb asks which languages the user's callers speak and up to three are picked from a
 * grid of uneven cards; then the three picked cards grow, in place, into the greeting cards —
 * each showing how Equal will answer in that language — while the rest fade away, and the orb
 * asks which one callers are greeted in. Picking only one language skips the second question.
 * Back on the second question returns to the grid, on this page.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LanguagesScreen(model: AppModel) {
    val talk = rememberTalk()
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(LangStep.Picking) }
    var revealed by remember { mutableStateOf(false) }
    var greetingShown by remember { mutableStateOf(false) }
    /** 0 → 1 as the cards open under the orb (the orb glides up to make room). */
    val room = remember { Animatable(0f) }
    val asking = phase == LangStep.Greeting
    val title = if (asking) Catalog.primaryAsk else Catalog.languagesAsk
    val atMax = model.languages.size >= CallLanguage.MAX_PICKED

    fun toggle(l: CallLanguage) {
        val on = l in model.languages
        if (!on && atMax) { Haptics.warning(); model.showToast("Pick up to 3 — tap one to swap it out"); return }
        Haptics.select()
        if (on) {
            model.languages.remove(l)
            if (model.primaryLanguage == l) model.primaryLanguage = null
        } else model.languages.add(l)
    }

    /** On to the greeting question: the picked cards grow into greeting cards as the orb asks. */
    fun askGreeting() {
        if (model.languages.size == 1) {
            // Nothing to choose between: the one language is the greeting.
            model.primaryLanguage = model.languages.first()
            model.next()
            return
        }
        if (model.primaryLanguage !in model.languages) model.primaryLanguage = null
        phase = LangStep.Greeting
        greetingShown = false
        talk.full = Catalog.primaryAsk
        scope.launch { talk.think() }
        scope.launch {
            delay(900)
            if (phase == LangStep.Greeting) { talk.show(Catalog.primaryAsk); greetingShown = true }
        }
    }

    /** Back to the grid, the cards shrinking back into their places. */
    fun backToGrid() {
        phase = LangStep.Picking
        greetingShown = false
        talk.full = Catalog.languagesAsk
        scope.launch { talk.think() }
        scope.launch {
            delay(800)
            if (phase == LangStep.Picking) talk.show(Catalog.languagesAsk)
        }
    }

    DisposableEffect(Unit) {
        model.localBack = Step.Languages to { if (phase == LangStep.Picking) false else { backToGrid(); true } }
        onDispose { if (model.localBack?.first == Step.Languages) model.localBack = null }
    }

    LaunchedEffect(Unit) {
        // Back from a later step with the greeting language already chosen: straight to that question.
        if (model.primaryLanguage != null && model.languages.size > 1) {
            phase = LangStep.Greeting
            talk.intro(Catalog.primaryAsk)
        } else talk.intro(Catalog.languagesAsk)
        snapshotFlow { talk.typing }.collect { typing ->
            if (typing) return@collect
            if (phase == LangStep.Greeting) greetingShown = true
            if (room.targetValue == 1f) return@collect
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
        if (p > 0f) Box(
            Modifier
                .padding(horizontal = 24.dp)
                .layout { m, c ->
                    val pl = m.measure(c)
                    val gap = 26.dp.roundToPx()
                    layout(pl.width, ((pl.height + gap) * p).roundToInt().coerceAtLeast(0)) { pl.place(0, gap) }
                }
                .graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = 40 * density * (1 - p) },
        ) {
            SharedTransitionLayout(Modifier.fillMaxWidth()) {
                AnimatedContent(
                    phase,
                    transitionSpec = {
                        // The greeting cards aren't faded in: they're the picked cards, grown. The
                        // ones left behind fade as the picked ones travel.
                        val out = if (targetState == LangStep.Greeting) fadeOut(tween(320)) else fadeOut(tween(160))
                        EnterTransition.None togetherWith out using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.TopCenter,
                    label = "languages",
                ) { step ->
                    when (step) {
                        LangStep.Picking -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CallLanguage.options.chunked(2).forEachIndexed { r, row ->
                                BrickRow(row.indices.map { gridWeights[r * 2 + it] }, spacing = 12.dp, height = 70.dp) {
                                    row.forEachIndexed { c, l ->
                                        val on = l in model.languages
                                        LanguageCard(
                                            l, on, greeting = false, dimmed = atMax && !on,
                                            modifier = Modifier
                                                .revealIn(revealed, r * 2 + c)
                                                .sharedElement(rememberSharedContentState(l.name), this@AnimatedContent, cardBounds),
                                        ) { toggle(l) }
                                    }
                                }
                            }
                        }
                        // The three picked languages as square cards, two side by side and the
                        // third centred under them.
                        LangStep.Greeting -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val side = (maxWidth - 12.dp) / 2
                            @Composable
                            fun square(l: CallLanguage) = LanguageCard(
                                l, model.primaryLanguage == l, greeting = true, dimmed = false,
                                modifier = Modifier.size(side)
                                    .sharedElement(rememberSharedContentState(l.name), this@AnimatedContent, cardBounds),
                                greetingText = Catalog.greetingWord(l),
                            ) {
                                Haptics.select()
                                model.primaryLanguage = l
                            }
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    model.languages.take(2).forEach { square(it) }
                                }
                                model.languages.drop(2).forEach { square(it) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            if (asking || model.languages.size == 1) model.nextCta else "Continue",
            enabled = if (asking) model.primaryLanguage != null else model.languages.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).graphicsLayer { alpha = buttonAlpha },
        ) {
            if (asking) model.next() else askGreeting()
        }
    }
}

/**
 * One language: its name in its own script with the English name under it, and its own letter
 * large and faint behind, running off the card's edge. Grown into a greeting card it becomes a
 * square, its name centred over how Equal will answer callers in that language. Picked: a light
 * green fill, a green edge, a soft lift and a tick, like every other choice card. Once three are
 * picked the rest fade back a little.
 */
@Composable
private fun LanguageCard(
    l: CallLanguage, on: Boolean, greeting: Boolean, dimmed: Boolean,
    modifier: Modifier, greetingText: String = "", onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (greeting) 22.dp else 18.dp)
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
                contentDescription = if (greeting) "${l.english}. Greets callers with $greetingText" else l.english
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
                l.initial, dm(if (greeting) 76f else 66f, FontWeight.Bold, mark),
                Modifier
                    .align(if (greeting) Alignment.BottomEnd else Alignment.CenterEnd)
                    .offset(x = if (greeting) 14.dp else 8.dp, y = if (greeting) 18.dp else 0.dp),
                maxLines = 1,
            )
            if (greeting) {
                // The greeting fades in as the card grows into it, rather than crowding it on the way.
                var settled by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(140); settled = true }
                val lineAlpha by animateFloatAsState(if (settled) 1f else 0f, tween(260), label = "greeting")
                // Fixed rows, so all three cards read the same whatever the script does to the
                // text's height: how Equal opens the call, then the language it's in.
                Column(
                    Modifier.matchParentSize().padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.height(34.dp).graphicsLayer { alpha = lineAlpha },
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt("“$greetingText”", dm(16f, FontWeight.SemiBold, nameColor, align = TextAlign.Center), maxLines = 1)
                    }
                    Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
                        Txt(l.label, dm(14f, FontWeight.Medium, EQ.ink.copy(alpha = 0.75f), align = TextAlign.Center), maxLines = 1)
                    }
                    Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                        Txt(
                            if (l.label == l.english) "" else l.english,
                            dm(11.5f, color = EQ.secondary, align = TextAlign.Center), maxLines = 1,
                        )
                    }
                }
            } else Column(
                Modifier.matchParentSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Txt(l.label, dm(17f, FontWeight.SemiBold, nameColor), maxLines = 1)
                if (l.label != l.english) Txt(l.english, dm(12.5f, color = EQ.secondary), maxLines = 1)
            }
        }
        CheckBadge(on, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp))
    }
}

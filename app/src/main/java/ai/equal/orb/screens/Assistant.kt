package ai.equal.orb.screens

import ai.equal.orb.ui.glassCardFace
import ai.equal.orb.ui.selectableLift
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Gender
import ai.equal.orb.model.Voice
import ai.equal.orb.ui.CheckBadge
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.GenderMark
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberClock
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.rememberVoicePreview
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * After the sample calls: “Now that you’ve seen me in action, how would you like me to
 * sound?” Four voices in a 2×2 grid, two female and two male, on neutral glass cards marked ♀ / ♂
 * like the gender cards. Tapping a card picks that voice and plays its hello; the way on appears
 * once one is picked.
 */
@Composable
fun AssistantScreen(model: AppModel) {
    val talk = rememberTalk()
    val preview = rememberVoicePreview()
    var chosen by remember { mutableStateOf<Voice?>(null) }
    var revealed by remember { mutableStateOf(false) }
    /** 0 → 1 as the grid opens under the orb (the orb glides up to make room). */
    val grid = remember { Animatable(0f) }
    val title = Catalog.assistantAsk
    // Female and male voices on the diagonals.
    val rows = listOf(listOf(Voice.Aanya, Voice.Kabir), listOf(Voice.Arjun, Voice.Meera))

    fun pick(v: Voice) {
        Haptics.select()
        chosen = v
        model.voice = v
        model.voiceChosen = true
        preview.toggle(v, model.primaryLanguage ?: CallLanguage.English, model.addressName)
    }

    LaunchedEffect(Unit) {
        // Always opens with nothing picked, even coming back: the way on waits for a tap.
        chosen = null
        model.voiceChosen = false
        talk.intro(title)
        // Once the orb has asked, the grid opens and the cards rise in.
        snapshotFlow { talk.typing }.collect { typing ->
            if (typing || grid.targetValue == 1f) return@collect
            delay(350)
            launch { grid.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 65f)) }
            delay(120)
            revealed = true
        }
    }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    val buttonAlpha by animateFloatAsState(if (revealed) 1f else 0f, tween(400, delayMillis = 400), label = "cta")
    val p = grid.value

    Column(Modifier.fillMaxSize()) {
        // Before the grid opens the orb sits in the middle of the screen; the space above it
        // closes as the grid takes its room.
        Spacer(Modifier.weight((1f - p).coerceAtLeast(0.0001f)))
        OrbWithBubble(talk, title, Modifier.fillMaxWidth().padding(top = 10.dp).padding(horizontal = 24.dp))
        if (p > 0f) Column(
            Modifier
                .padding(horizontal = 24.dp)
                .layout { m, c ->
                    val pl = m.measure(c)
                    val h = ((pl.height + 30.dp.roundToPx()) * p).roundToInt()
                    layout(pl.width, h.coerceAtLeast(0)) { pl.place(0, 30.dp.roundToPx()) }
                }
                .graphicsLayer { alpha = p.coerceIn(0f, 1f); translationY = 40 * density * (1 - p) },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rows.forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEachIndexed { c, v ->
                        VoiceCard(v, chosen == v, preview.speaking == v, Modifier.weight(1f).revealIn(revealed, r * 2 + c)) { pick(v) }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // The button below roughly balances the top bar above, so nothing more is needed here.
        PrimaryButton(
            model.nextCta, enabled = chosen != null,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).graphicsLayer { alpha = buttonAlpha },
        ) {
            preview.stop()
            model.next()
        }
    }
}

/**
 * One voice: styled like the gender cards — a pale tint, a soft top sheen and a faint ♀ / ♂
 * behind — with a play button (bars while it speaks), the name and its vibe.
 */
@Composable
private fun VoiceCard(v: Voice, on: Boolean, speaking: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val gender = if (v.isFemale) Gender.Female else Gender.Male
    val sel by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 440f), label = "voice")
    val nameColor by animateColorAsState(if (on) EQ.greenText else EQ.ink, tween(200), label = "name")
    Box(
        modifier
            .height(148.dp)
            .pressable(scale = 0.97f) { onClick() }
            .semantics { contentDescription = "${v.name}, ${v.gender} voice, ${v.vibe}"; selected = on },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .selectableLift(on, shape)
                .clip(shape)
                .glassCardFace(shape, on)
                .border(if (on) 1.5.dp else 1.dp, if (on) EQ.green else EQ.edge, shape),
        ) {
            // In the lower-right corner, whole enough to read as ♀ (ring and cross) or ♂ (ring and
            // arrow), just cut by the edge.
            GenderMark(
                gender,
                Modifier.align(Alignment.BottomEnd).offset(if (v.isFemale) 38.dp else 30.dp, if (v.isFemale) (-32).dp else 22.dp),
            )
            Column(Modifier.matchParentSize().padding(14.dp)) {
                // Plays its hello: bars while it speaks, a play glyph otherwise.
                Box(
                    Modifier.align(Alignment.End).size(30.dp).background(EQ.green.copy(alpha = if (speaking) 0.2f else 0.13f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (speaking) SpeakingBars(Modifier.size(14.dp, 12.dp))
                    else Image(rememberVectorPainter(Icons.Filled.PlayArrow), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(EQ.green))
                }
                Spacer(Modifier.weight(1f))
                Txt(v.name, dm(21f, FontWeight.SemiBold, nameColor))
                Txt(v.vibe, dm(13f, color = EQ.ink.copy(alpha = 0.55f)), Modifier.padding(top = 2.dp))
            }
        }
        CheckBadge(on, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp))
    }
}

/** Three little bars bouncing while a voice speaks. */
@Composable
private fun SpeakingBars(modifier: Modifier) {
    val clock = rememberClock()
    Canvas(modifier) {
        val w = 3.dp.toPx()
        val gap = 2.dp.toPx()
        val left = (size.width - 3 * w - 2 * gap) / 2
        val t = clock.value
        for (i in 0 until 3) {
            val h = (4 + 8 * abs(sin(t * 7 + i * 1.1f))).dp.toPx()
            drawRoundRect(
                EQ.green, Offset(left + i * (w + gap), (size.height - h) / 2), Size(w, h), CornerRadius(w / 2),
            )
        }
    }
}

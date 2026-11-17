package ai.equal.orb.screens

import ai.equal.orb.ui.glassCardFace
import ai.equal.orb.ui.selectableLift
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Gender
import ai.equal.orb.model.PermissionGate
import ai.equal.orb.model.Voice
import ai.equal.orb.ui.CheckBadge
import ai.equal.orb.ui.Device
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
    val context = LocalContext.current
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
    // Nothing came out: say why, rather than leaving a card that looks broken.
    LaunchedEffect(preview.failed) {
        if (preview.failed) { Haptics.warning(); model.showToast("Couldn’t play that preview. Tap the card to try again.") }
    }
    // The ring switch is off, so a preview would play into silence.
    val silent = remember { Device.silent(context) }

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
        if (silent && chosen != null) Txt(
            "🔇  Your phone is on silent — turn it up to hear the voices",
            dm(13f, color = EQ.ink.copy(alpha = 0.6f), align = TextAlign.Center),
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        // The button below roughly balances the top bar above, so nothing more is needed here.
        PrimaryButton(
            model.voiceCta, enabled = chosen != null,
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).graphicsLayer { alpha = buttonAlpha },
        ) {
            preview.stop()
            // Contacts and call handling are asked for here; the flow waits until they're granted.
            // Then the quiet attempt behind this page decides whether the walkthrough is needed.
            model.askPermissions(PermissionGate.CallsAndContacts) { model.finishVoiceStep() }
        }
    }
}

/**
 * One voice, as a quiet piece of glass: a pale face lit from the top-left, washing to a faint ink
 * at the foot, inside a lit top edge; its gender small and tracked opposite a white play button,
 * then the name, its vibe, and the voice's own waveform along the bottom — still while it's quiet,
 * green and breathing while it speaks. The ♀ / ♂ mark stays in the corner, fainter than the gender
 * cards', cut by the edge.
 */
@Composable
private fun VoiceCard(v: Voice, on: Boolean, speaking: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val gender = if (v.isFemale) Gender.Female else Gender.Male
    val face by animateColorAsState(
        if (on) EQ.mintAlt.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.62f), tween(250), label = "face",
    )
    val nameColor by animateColorAsState(if (on) EQ.greenText else EQ.ink, tween(200), label = "name")
    val rim by animateColorAsState(if (on) EQ.green else EQ.edge, tween(200), label = "rim")
    val sheenRadius = with(LocalDensity.current) { 170.dp.toPx() }
    Box(
        modifier
            .height(152.dp)
            .pressable(scale = 0.97f) { onClick() }
            .semantics { contentDescription = "${v.name}, ${v.gender} voice, ${v.vibe}"; selected = on },
    ) {
        Box(
            Modifier
                .matchParentSize()
                // A wide, soft shadow under a closer one: the card sits above the page rather than
                // being drawn on it. Picked, a green one joins them (from selectableLift).
                .shadow(22.dp, shape, ambientColor = hex(0x0B1F12, 0.05f), spotColor = hex(0x0B1F12, 0.07f))
                .selectableLift(on, shape)
                .clip(shape)
                .background(face, shape)
                // The light falls from the top-left corner…
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset.Zero, radius = sheenRadius,
                    ),
                    shape,
                )
                // …and the foot settles into a faint ink.
                .background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to EQ.ink.copy(alpha = 0.055f)),
                    shape,
                )
                .border(if (on) 1.5.dp else 1.dp, rim, shape),
        ) {
            // In the lower-right corner, whole enough to read as ♀ (ring and cross) or ♂ (ring and
            // arrow), just cut by the edge — fainter here than on the gender cards.
            GenderMark(
                gender,
                Modifier.align(Alignment.BottomEnd).offset(if (v.isFemale) 38.dp else 30.dp, if (v.isFemale) (-32).dp else 22.dp),
                opacity = 0.035f,
            )
            // The lit top edge, just inside the outline.
            Box(
                Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(0f to Color.White.copy(alpha = 0.9f), 0.45f to Color.Transparent),
                        RoundedCornerShape(21.dp),
                    ),
            )
            Column(Modifier.matchParentSize().padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Txt(v.gender.uppercase(), dm(9f, FontWeight.Bold, EQ.ink.copy(alpha = 0.4f), tracking = 1.2f))
                    PlayDisc(speaking)
                }
                Spacer(Modifier.weight(1f))
                Txt(v.name, dm(22f, FontWeight.SemiBold, nameColor))
                Txt(v.vibe, dm(12.5f, color = EQ.ink.copy(alpha = 0.55f)), Modifier.padding(top = 1.dp))
                VoiceWave(v, speaking, Modifier.fillMaxWidth().padding(top = 9.dp).height(14.dp))
            }
        }
        CheckBadge(on, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp))
    }
}

/** The small white disc that plays a voice's hello: a play glyph, a pause while it speaks. */
@Composable
private fun PlayDisc(speaking: Boolean) {
    Box(
        Modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape, ambientColor = hex(0x0B1F12, 0.10f), spotColor = hex(0x0B1F12, 0.14f))
            .background(Color.White, CircleShape)
            .border(1.dp, EQ.ink.copy(alpha = 0.07f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (speaking) Canvas(Modifier.size(11.dp)) {
            val w = 3.2.dp.toPx()
            val gap = 3.dp.toPx()
            val left = (size.width - 2 * w - gap) / 2
            repeat(2) { i ->
                drawRoundRect(EQ.green, Offset(left + i * (w + gap), 0f), Size(w, size.height), CornerRadius(w / 2))
            }
        } else Image(
            rememberVectorPainter(Icons.Filled.PlayArrow), null,
            Modifier.size(16.dp).offset(x = 1.dp), colorFilter = ColorFilter.tint(EQ.green),
        )
    }
}

/**
 * The voice's own waveform: fourteen thin capsules whose shape is fixed per voice (so each card
 * keeps its own silhouette), faint ink and still while it's quiet, green and breathing while that
 * voice is speaking.
 */
@Composable
private fun VoiceWave(v: Voice, speaking: Boolean, modifier: Modifier) {
    val clock = rememberClock()
    val bars = remember(v) { Random(v.ordinal * 31 + 7).let { r -> List(14) { 0.3f + r.nextFloat() * 0.7f } } }
    val live by animateFloatAsState(if (speaking) 1f else 0f, tween(280), label = "wave")
    val colour = lerp(EQ.ink.copy(alpha = 0.14f), EQ.green, live)
    Canvas(modifier) {
        val w = 2.5.dp.toPx()
        val gap = (size.width - bars.size * w) / (bars.size - 1)
        // The clock is only read while it speaks, so a quiet card isn't redrawn every frame.
        val t = if (live > 0f) clock.value else 0f
        bars.forEachIndexed { i, base ->
            val breathe = if (live > 0f) 1f + live * (0.45f * abs(sin(t * 6f + i * 0.7f)) - 0.2f) else 1f
            val h = (size.height * base * breathe).coerceAtLeast(w)
            drawRoundRect(colour, Offset(i * (w + gap), (size.height - h) / 2), Size(w, h), CornerRadius(w / 2))
        }
    }
}

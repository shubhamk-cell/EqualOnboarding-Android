package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Step
import ai.equal.orb.model.UseCase
import ai.equal.orb.ui.BrickRow
import ai.equal.orb.ui.CallPlayer
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.WrapRows
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberCallPlayer
import ai.equal.orb.ui.rememberTalk
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.equal.orb.ui.OrbBesideBubble
import ai.equal.orb.ui.Talk
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import kotlin.math.min

private enum class Pick3 { Picking, Curating, Ready }

/** Where the picks are: in the grid, gathered at the top, or grown into voice notes. */
private enum class Stage { Grid, Gathered, Notes }

private const val NEEDED = 3

/**
 * The picks: “Select any 3”, with no Skip and no Continue. The third pick sets everything off on
 * the same page: the other options fade, the three gather at the top and shimmer while the orb
 * curates, then each turns into a voice note: its photo grows into the note's cover, its emoji
 * and name glide into place, and its player pops in beside them. Continue arrives after.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PickThreeScreen(model: AppModel) {
    val talk = rememberTalk()
    val player = rememberCallPlayer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var phase by remember { mutableStateOf(Pick3.Picking) }
    /** The picks have moved out of the grid to the top, in wrapping rows. */
    var rearranged by remember { mutableStateOf(false) }
    /** The picks have grown into call cards; their insides pop in a beat later. */
    var expanded by remember { mutableStateOf(false) }
    var cardContent by remember { mutableStateOf(false) }
    var shimmer by remember { mutableStateOf(false) }
    var curating by remember { mutableStateOf<Job?>(null) }
    /**
     * Just before the picks turn into voice notes they let go of what doesn't carry over (outline,
     * number, glow, their own copy of the name), so only the photo, emoji and name are seen moving.
     */
    var handing by remember { mutableStateOf(false) }
    /** Each pill's width in the grid, so the picks keep their size when they move. */
    val pillWidths = remember { mutableStateMapOf<String, Dp>() }
    /** Each pick's call length, worked out once the picks are made, for the time on its button. */
    val lengths = remember { mutableStateMapOf<String, Float>() }
    var showOptions by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    val lift = remember { Animatable(0f) }
    val scroll = rememberScrollState()
    val title = Catalog.pickThree
    val rows = remember { Catalog.useCases.chunked(2) }

    /** Back to choosing, on this page, starting fresh: all three picks are cleared. */
    fun backToPicking() {
        curating?.cancel()
        player.stop()
        shimmer = false; handing = false; cardContent = false
        expanded = false
        rearranged = false
        phase = Pick3.Picking
        // The numbers come off just after the options are back in place.
        scope.launch { delay(350); model.picks.clear() }
        scope.launch { talk.replace(title) }
    }

    suspend fun curate() = kotlinx.coroutines.coroutineScope {
        // A beat to see the third pick land (and to untick it, if it was a slip).
        delay(550)
        if (model.picks.size != NEEDED || phase != Pick3.Picking) return@coroutineScope
        Haptics.step()
        phase = Pick3.Curating
        launch { scroll.animateScrollTo(0, tween(600, easing = EQ.push)) }
        talk.clear()
        talk.full = Catalog.curating
        launch { talk.say(Catalog.curating) }
        // Once the others have faded, the picks glide up and gather at the top.
        delay(850)
        if (model.step != Step.Cases) return@coroutineScope
        rearranged = true
        shimmer = true
        val picks = model.picks.toList()
        launch {
            for (id in picks) {
                val u = Catalog.useCase(id) ?: continue
                lengths[id] = withContext(Dispatchers.IO) {
                    callLength(context, CallPlayer.make(context, u, voice = model.voice, name = model.addressName))
                }
            }
        }
        delay(1900)
        if (model.step != Step.Cases) return@coroutineScope
        // Each pick turns into its voice note: what stays behind goes first, then the photo, emoji
        // and name travel.
        handing = true
        delay(120)
        if (model.step != Step.Cases) return@coroutineScope
        phase = Pick3.Ready
        expanded = true
        Haptics.land()
        delay(400)
        cardContent = true
        // The orb has its say only once the notes have fully formed (players drawn in).
        delay(950)
        if (model.step != Step.Cases) return@coroutineScope
        talk.replace(Catalog.curated(NEEDED))
    }

    fun toggle(u: UseCase) {
        if (phase != Pick3.Picking) return
        // Never more than three, even with quick taps during the beat before curating (unticking
        // one still works then).
        val adding = u.id !in model.picks
        if (adding && model.picks.size >= NEEDED) return
        Haptics.select()
        if (adding) model.picks.add(u.id) else model.picks.remove(u.id)
        if (model.picks.size == NEEDED) { curating?.cancel(); curating = scope.launch { curate() } }
    }

    fun play(u: UseCase) {
        Haptics.select()
        if (player.id == u.id) {
            if (player.playing) player.pause() else player.play()
            return
        }
        player.load(CallPlayer.make(context, u, voice = model.voice, name = model.addressName), u.id)
        player.play()
    }

    LaunchedEffect(Unit) {
        // Back from a later step: start over with nothing picked.
        model.picks.clear()
        talk.intro(title)
    }
    // Back on the sample calls (or while they're being made) returns to picking.
    DisposableEffect(Unit) {
        model.localBack = Step.Cases to { if (phase == Pick3.Picking) false else { backToPicking(); true } }
        onDispose { player.stop() }
    }
    LaunchedEffect(Unit) { revealOptions(talk, { showOptions }, { showOptions = true }, lift) { revealed = true } }
    LaunchedEffect(player.finished) { if (player.finished) Haptics.success() }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight((1f - lift.value).coerceAtLeast(0.0001f)))
        // The orb stays centred under its bubble throughout, picks to voice notes.
        OrbWithBubble(
            talk, if (phase == Pick3.Ready) Catalog.curated(NEEDED) else title,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 24.dp, end = 24.dp),
            spinning = phase == Pick3.Curating,
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val fadeBottom by animateDpAsState(if (expanded) 20.dp else 40.dp, label = "fadeBottom")
            OptionsArea(showOptions, fadeBottom) {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    val padTop by animateDpAsState(
                        if (expanded) 8.dp else if (phase == Pick3.Picking) 40.dp else 44.dp, tween(450, easing = EQ.push), label = "padTop",
                    )
                    val padBottom by animateDpAsState(if (expanded) 20.dp else 44.dp, tween(450, easing = EQ.push), label = "padBottom")
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scroll)
                            .padding(horizontal = 24.dp).padding(top = padTop, bottom = padBottom)
                    ) {
                        val stage = if (expanded) Stage.Notes else if (rearranged) Stage.Gathered else Stage.Grid
                        AnimatedContent(
                            stage,
                            transitionSpec = {
                                // The notes don't fade in: they're built from the picks' own parts. The
                                // gathered picks fade as they hand over; going back is instant.
                                val out = when {
                                    targetState == Stage.Notes -> fadeOut(tween(350))
                                    initialState == Stage.Notes -> ExitTransition.None
                                    else -> fadeOut(tween(180))
                                }
                                EnterTransition.None togetherWith out using SizeTransform(clip = false)
                            },
                            contentAlignment = Alignment.TopCenter,
                            label = "picks",
                        ) { s ->
                            val pickState = PickState(phase, handing, shimmer, revealed)
                            when (s) {
                                Stage.Notes -> Column {
                                    model.picks.forEachIndexed { i, id ->
                                        val u = Catalog.useCase(id) ?: return@forEachIndexed
                                        NoteCard(u, i, player, lengths[id] ?: 0f, cardContent, this@AnimatedContent) { play(u) }
                                    }
                                }
                                Stage.Gathered -> WrapRows(spacing = 10.dp, modifier = Modifier.fillMaxWidth(), centered = true) {
                                    model.picks.forEachIndexed { i, id ->
                                        val u = Catalog.useCase(id) ?: return@forEachIndexed
                                        Box(
                                            Modifier
                                                .sharedElement(rememberSharedContentState(id), this@AnimatedContent, gatherBounds)
                                                .width(pillWidths[id] ?: Dp.Unspecified).height(64.dp)
                                        ) {
                                            ThreePill(u, i, model.picks, pickState, parts = true, this@AnimatedContent) {}
                                        }
                                    }
                                }
                                Stage.Grid -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rows.forEachIndexed { r, row ->
                                        BrickRow(row.map { (it.label.length * it.label.length).toFloat() }, height = 64.dp) {
                                            row.forEachIndexed { j, u ->
                                                Box(
                                                    Modifier
                                                        .onSizeChanged { pillWidths[u.id] = with(density) { it.width.toDp() } }
                                                        .sharedElement(rememberSharedContentState(u.id), this@AnimatedContent, gatherBounds)
                                                ) {
                                                    ThreePill(u, r * 2 + j, model.picks, pickState, parts = false, this@AnimatedContent) { toggle(u) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            cardContent,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400, easing = EQ.push)) { with(density) { 30.dp.roundToPx() } },
            exit = ExitTransition.None,
        ) {
            PrimaryButton(model.nextCta, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                player.stop()
                model.next()
            }
        }
    }
}

/** What every pill needs to know about the page. */
private data class PickState(val phase: Pick3, val handing: Boolean, val shimmer: Boolean, val revealed: Boolean)

/** The picks' glide up to the top. */
private val gatherBounds = { _: Rect, _: Rect -> spring(0.84f, 110f, Rect.VisibilityThreshold) }

/** The picks' parts travelling into their voice notes. */
private val noteBounds = { _: Rect, _: Rect -> spring(0.86f, 80f, Rect.VisibilityThreshold) }

/**
 * One option: who's on the call as a small round photo (or the emoji), and the job; picked = green
 * outline, tint and its number (1, 2, 3) in the order it was picked. With [parts], its photo, emoji
 * and name are matched to the voice note's, so they travel there.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ThreePill(
    u: UseCase, index: Int, picks: List<String>, page: PickState, parts: Boolean,
    scope: AnimatedVisibilityScope, onClick: () -> Unit,
) {
    val on = u.id in picks
    val fading = page.phase != Pick3.Picking && !on
    val letGo by animateFloatAsState(if (page.handing && on) 1f else 0f, if (page.handing) tween(140) else snap(), label = "letGo")
    val badge by animateFloatAsState(
        if (on && !page.handing) 1f else 0f, spring(0.55f, 440f), label = "badge",
    )
    val fill by animateColorAsState(if (on) EQ.mintAlt else Color.White.copy(alpha = 0.7f), tween(200), label = "fill")
    val rim by animateColorAsState(if (on) EQ.green else EQ.edge, tween(200), label = "rim")
    val shape = RoundedCornerShape(20.dp)
    val photo = Catalog.cover(u.id)
    val order = picks.indexOf(u.id)
    var number by remember { mutableStateOf(1) }
    if (order >= 0) number = order + 1

    // Cascade in one by one, in reading order, as the options arrive.
    val arrive by animateFloatAsState(
        if (page.revealed) 1f else 0f, spring(0.8f, 158f), label = "arrive",
    )
    var arrived by remember { mutableStateOf(page.revealed) }
    LaunchedEffect(page.revealed) {
        if (page.revealed && !arrived) { delay(minOf(index, 12) * 100L); arrived = true }
        if (!page.revealed) arrived = false
    }
    val a = if (arrived) arrive else 0f

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = a.coerceIn(0f, 1f)
                translationY = 18 * density * (1 - a)
                val s = 0.94f + 0.06f * a; scaleX = s; scaleY = s
            }
            .curateFate(on && page.phase != Pick3.Picking, fading, index)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .pressable(enabled = page.phase == Pick3.Picking, scale = 0.96f) { onClick() }
                .drawBehind {
                    // The soft green glow around a pick.
                    val g = 3.dp.toPx()
                    drawRoundRect(
                        EQ.green.copy(alpha = if (on) 0.12f * (1 - letGo) else 0f),
                        Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(20.dp.toPx() + g),
                    )
                }
                .shadow(
                    (if (on) 7.dp else 6.dp) * (1 - letGo), shape,
                    ambientColor = hex(0x003214, 0.06f), spotColor = hex(if (on) 0x00782D else 0x003214, if (on) 0.12f else 0.06f),
                )
                .background(fill.copy(alpha = fill.alpha * (1 - letGo)), shape)
                .border(if (on) 1.5.dp else 1.dp, rim.copy(alpha = rim.alpha * (1 - letGo)), shape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (photo != null) {
                Image(
                    painterResource(photo), null,
                    Modifier
                        .then(if (parts) Modifier.sharedElement(rememberSharedContentState("${u.id}.cover"), scope, noteBounds) else Modifier)
                        .size(36.dp).clip(CircleShape).border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Txt(
                    u.emoji, dm(18f),
                    Modifier
                        .then(if (parts) Modifier.sharedElement(rememberSharedContentState("${u.id}.emoji"), scope, noteBounds).skipToLookaheadSize() else Modifier)
                        .graphicsLayer { alpha = 1 - letGo },
                )
            }
            Txt(
                u.label,
                dm(14.5f, FontWeight.Medium, if (on) EQ.greenText else EQ.ink, lineHeight = 18f, tracking = -0.22f),
                Modifier
                    .then(if (parts) Modifier.sharedElement(rememberSharedContentState("${u.id}.label"), scope, noteBounds).skipToLookaheadSize() else Modifier)
                    .graphicsLayer { alpha = 1 - letGo },
                maxLines = 2,
            )
        }
        // While curating, a sheen sweeps across the picks.
        if (on && page.phase == Pick3.Curating && page.shimmer) Sheen(Modifier.matchParentSize().clip(shape))
        // The pick's number, rather than a tick.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-6).dp)
                .size(20.dp)
                .graphicsLayer { val s = 0.4f + 0.6f * badge; scaleX = s; scaleY = s; alpha = badge.coerceIn(0f, 1f) }
                .border(2.dp, EQ.page, CircleShape)
                .padding(1.dp)
                .background(EQ.green, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Txt("$number", dm(11.5f, FontWeight.Bold, Color.White).copy(fontFeatureSettings = "tnum"))
        }
    }
}

/** A band of light sweeping left to right, over and over (after a short wait). */
@Composable
private fun Sheen(modifier: Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(400); started = true }
    val t by rememberInfiniteTransition(label = "sheen").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "sweep",
    )
    Canvas(modifier) {
        if (!started) return@Canvas
        val w = size.width * 0.5f
        val x = -size.width * 0.7f + (size.width * 1.9f) * t
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent), x, x + w),
            Offset(x, 0f), Size(w, size.height),
        )
    }
}

/**
 * A voice note with no container: a large round cover on the left (a photo of who's on the
 * call, or the emoji on a tint), and beside it the call's name over one light button to hear it.
 * A hairline sits between notes.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.NoteCard(
    u: UseCase, index: Int, player: CallPlayer, length: Float, cardContent: Boolean,
    scope: AnimatedVisibilityScope, onPlay: () -> Unit,
) {
    val current = player.id == u.id
    val playing = current && player.playing
    val started = current && (playing || player.elapsed > 0f)
    val progress = if (current) (player.elapsed / maxOf(0.1f, player.duration)).coerceAtMost(1f) else 0f
    val fill by animateFloatAsState(progress, tween(100, easing = LinearEasing), label = "fill")
    val tint = hex(Catalog.cardTints[index % Catalog.cardTints.size])
    val photo = Catalog.cover(u.id)

    // The hairline, and then the button, pop in once the note has formed, one after another.
    val line = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(cardContent) {
        if (!cardContent) { line.snapTo(0f); pop.snapTo(0f); return@LaunchedEffect }
        launch { delay(index * 80L); line.animateTo(1f, tween(400)) }
        delay(index * 80L)
        pop.animateTo(1f, spring(0.7f, 247f))
    }

    Box(Modifier.fillMaxWidth()) {
        if (index > 0) {
            Box(Modifier.fillMaxWidth().height(1.dp).graphicsLayer { alpha = line.value }.background(EQ.ink.copy(alpha = 0.09f)))
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Out of the pick: its photo grows into this cover, and its emoji and name glide over.
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .matchParentSize()
                        .sharedElement(rememberSharedContentState("${u.id}.cover"), scope, noteBounds)
                        .shadow(6.dp, CircleShape, ambientColor = hex(0x003214, 0.1f), spotColor = hex(0x003214, 0.1f))
                        .clip(CircleShape)
                        .background(tint)
                        .border(1.5.dp, Color.White, CircleShape)
                ) {
                    if (photo != null) Image(painterResource(photo), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                }
                if (photo == null) {
                    Txt(
                        u.emoji, dm(32f),
                        Modifier.sharedElement(rememberSharedContentState("${u.id}.emoji"), scope, noteBounds).skipToLookaheadSize(),
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Whole names only: a long one takes a second line rather than cutting off.
                Txt(
                    u.label, dm(16f, FontWeight.SemiBold, EQ.ink, lineHeight = 21f),
                    Modifier.sharedElement(rememberSharedContentState("${u.id}.label"), scope, noteBounds).skipToLookaheadSize().fillMaxWidth(),
                    maxLines = 2,
                )
                // One light button to hear the call: it fills green behind its label as the call
                // plays, and shows the length until then (the time so far once it's going).
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            val s = 0.6f + 0.4f * pop.value; scaleX = s; scaleY = s
                            alpha = pop.value.coerceIn(0f, 1f)
                        }
                        .pressable(enabled = cardContent, scale = 0.96f) { onPlay() }
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(EQ.green.copy(alpha = 0.13f))
                        .drawBehind { drawRect(EQ.green.copy(alpha = 0.16f), size = Size(size.width * fill, size.height)) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    if (playing) {
                        Canvas(Modifier.size(11.dp)) {   // pause
                            val w = size.width * 0.3f
                            drawRoundRect(EQ.greenDeep, Offset(size.width * 0.1f, 0f), Size(w, size.height), CornerRadius(1.5f))
                            drawRoundRect(EQ.greenDeep, Offset(size.width * 0.6f, 0f), Size(w, size.height), CornerRadius(1.5f))
                        }
                    } else {
                        Image(rememberVectorPainter(Icons.Filled.PlayArrow), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(EQ.greenDeep))
                    }
                    Txt(if (playing) "Pause" else if (started) "Resume" else "Listen to this call", dm(14f, FontWeight.SemiBold, EQ.greenDeep))
                    Txt(
                        player.timeLabel(if (started) player.elapsed else length),
                        dm(13f, FontWeight.Medium, EQ.greenDeep.copy(alpha = 0.65f)).copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
    }
}

/**
 * A call's length before it's loaded: its recordings' lengths (or an estimate from the line)
 * plus the gaps between lines, in seconds.
 */
private fun callLength(context: Context, s: CallPlayer.Script): Float =
    s.lines.mapIndexed { i, (_, line) ->
        s.audio.getOrNull(i)?.let { rawSeconds(context, it) } ?: (line.length * 0.065f + 0.3f)
    }.sum() + s.lines.size * 0.35f

/** A raw recording's length in seconds, or null if it can't be read. */
private fun rawSeconds(context: Context, id: Int): Float? = runCatching {
    context.resources.openRawResourceFd(id).use { fd ->
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toFloat()?.div(1000f)
        } finally { r.release() }
    }
}.getOrNull()

/**
 * Waits for the orb to finish typing its question, then brings the options in: they push the
 * orb up ([lift] glides 0 → 1) and, a beat later, cascade in ([reveal]).
 */
private suspend fun revealOptions(talk: Talk, shown: () -> Boolean, show: () -> Unit, lift: Animatable<Float, *>, reveal: () -> Unit) {
    var was = false
    snapshotFlow { talk.typing }.collect { typing ->
        if (was && !typing && !shown()) {
            delay(450)
            show()
            kotlinx.coroutines.coroutineScope {
                launch { lift.animateTo(1f, spring(0.86f, 65f)) }
                delay(120)
                reveal()
            }
        }
        was = typing
    }
}

/** The scrolling options: rise in from below as they arrive, fade quickly when they hand over; edges fade. */
@Composable
private fun OptionsArea(visible: Boolean, fadeBottom: Dp, content: @Composable () -> Unit) {
    val rise = with(LocalDensity.current) { 40.dp.roundToPx() }
    androidx.compose.animation.AnimatedVisibility(
        visible, Modifier.fillMaxSize(),
        enter = fadeIn(spring(0.86f, 65f)) + slideInVertically(spring(0.86f, 65f)) { rise },
        exit = fadeOut(tween(180)),
    ) {
        Box(Modifier.fillMaxSize().fadeEdges(14.dp.value, fadeBottom.value)) { content() }
    }
}

/**
 * A pick's part in curating: the kept ones swell a touch with a little overshoot; the rest
 * shrink, blur and fade, one after another in reading order.
 */
private fun Modifier.curateFate(kept: Boolean, fading: Boolean, index: Int): Modifier = composed {
    val keep by animateFloatAsState(
        if (kept) 1f else 0f, tween(400, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)), label = "keep",
    )
    val fade by animateFloatAsState(
        if (fading) 1f else 0f, tween(450, delayMillis = min(index, 12) * 30, easing = EQ.push), label = "fade",
    )
    this
        .graphicsLayer {
            val s = 1 + 0.03f * keep - 0.1f * fade
            scaleX = s; scaleY = s
            alpha = (1 - fade).coerceIn(0f, 1f)
        }
        .blur((2 * fade).dp, BlurredEdgeTreatment.Unbounded)
}

/** Fades the top and bottom edges of a scrolling area. */
private fun Modifier.fadeEdges(topDp: Float, bottomDp: Float) = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val t = topDp * density / size.height
        val b = 1 - bottomDp * density / size.height
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, t to Color.Black, b to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

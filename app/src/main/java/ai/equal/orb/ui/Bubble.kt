package ai.equal.orb.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Language
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * What the orb is saying in its bubble. Like a chat message: a bubble pops in with the typing
 * dots, then a new bubble pops in with the whole line. Every line takes a fresh token, so a newer
 * one silently stops an older one mid-way. Lines are given in English and shown in
 * [Language.current]; when the language changes, the bubble swaps its line for the new language's.
 */
class Talk(private val scope: CoroutineScope) {
    var text by mutableStateOf("")
    /** The line the bubble rests on, in English. */
    var full by mutableStateOf("")
    /** True from the dots until the line is showing; screens wait for it to fall to move on. */
    var typing by mutableStateOf(false)
    /** The three typing dots, shown for a beat before a line appears. */
    var thinking by mutableStateOf(false)
    /** Mint fill while the bubble switches language. */
    var alt by mutableStateOf(false)
    /** Bumps each time the bubble should pop in again. */
    var popId by mutableIntStateOf(0)
    /** The bubble has shrunk back into its orb, between one message and the next. */
    var out by mutableStateOf(false)
    /** The first line has shown. Everything else on the page waits for this ([afterOrb]). */
    var said by mutableStateOf(false)

    private var token = 0
    private var switchJob: Job? = null

    init {
        scope.launch {
            snapshotFlow { Language.current }.drop(1).distinctUntilChanged().collect { retype() }
        }
    }

    /** Pops the bubble in with the typing dots (once the orb has landed), then shows [line]. */
    fun intro(line: String, delayMs: Long = 420) {
        full = line
        text = ""
        thinking = true
        typing = true
        out = false
        popId++
        val my = ++token
        scope.launch {
            OrbFlight.untilLanded()
            delay(delayMs + THINKING_MS)
            if (my == token) show(line)
        }
    }

    /** Shows the typing dots for a beat, then [line]. Returns false if something newer took over. */
    suspend fun say(line: String): Boolean {
        if (!think()) return false
        val my = token
        delay(THINKING_MS)
        if (my != token) return false
        return show(line)
    }

    /** Whatever is showing goes back into the orb, and the typing dots come out of it. */
    suspend fun think(): Boolean {
        val my = ++token
        // Already showing the dots: keep them, rather than send them back only to bring out another set.
        if (thinking && !out) { typing = true; return true }
        if (!leave(my)) return false
        text = ""
        typing = true
        thinking = true
        out = false
        popId++
        return true
    }

    /**
     * Shows [line] in full, replacing whatever is there. Returns false if something newer took
     * over. `leaving = false` swaps the line in place (a language switch) instead of going via the orb.
     */
    suspend fun show(line: String, leaving: Boolean = true): Boolean {
        val my = ++token
        if (leaving && !leave(my)) return false
        thinking = false
        text = Catalog.translate(line, Language.current)
        out = false
        if (leaving) popId++
        // Let the bubble settle before anything waiting on the line moves on.
        delay(400)
        if (my != token) return false
        typing = false
        said = true
        return true
    }

    /** Takes the bubble away, back into the orb. Returns false if something newer took over. */
    suspend fun clear(): Boolean {
        val my = ++token
        if (!leave(my)) return false
        text = ""
        thinking = false
        return my == token
    }

    /** The bubble on show shrinks and fades back into its orb. */
    private suspend fun leave(my: Int): Boolean {
        if (out || (!thinking && text.isEmpty())) return my == token
        out = true
        delay(200)
        return my == token
    }

    /** Clear, then say a new resting line (dots first). */
    suspend fun replace(line: String) {
        full = line
        if (!clear()) return
        say(line)
    }

    /** The language changed: swap the resting line for the new language's (mint meanwhile). */
    private fun retype() {
        if (full.isEmpty() || text.isEmpty()) return
        switchJob?.cancel()
        switchJob = scope.launch {
            alt = true
            if (!show(full, leaving = false)) return@launch
            delay(500)
            alt = false
        }
    }

    companion object {
        /** How long the typing dots show before a line appears. */
        const val THINKING_MS = 800L
    }
}

/** Bubbles everywhere step back into their orbs while the page changes. */
object BubbleStage {
    var away by mutableStateOf(false)
}

@Composable
fun rememberTalk(): Talk {
    val scope = rememberCoroutineScope()
    return remember { Talk(scope) }
}

enum class Tail { Bottom, Left, TopLeft, TopRight, None }

/**
 * Rounded bubble with a comic-book pointer (narrow base, sides curving slightly in, sharp tip),
 * unioned so the overlap never shows a seam.
 */
private fun bubblePath(size: Size, tail: Tail, radius: Float, d: Float): Path {
    val body = Path().apply {
        val r = min(radius, size.height / 2)
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
    }
    val t = Path()
    fun quad(ox: Float, oy: Float, vararg pts: Float) {
        // pts: start, then (control, end) pairs — all in dp from (ox, oy).
        t.moveTo(ox + pts[0] * d, oy + pts[1] * d)
        var i = 2
        while (i + 3 < pts.size) {
            t.quadraticTo(ox + pts[i] * d, oy + pts[i + 1] * d, ox + pts[i + 2] * d, oy + pts[i + 3] * d)
            i += 4
        }
        t.close()
    }
    when (tail) {
        // 24 wide at the bubble, tip 15 below it, pointing straight down at the orb.
        Tail.Bottom -> quad(size.width / 2, size.height, -12f, -2f, -3f, 3f, 0f, 15f, 3f, 3f, 12f, -2f)
        // 18 tall at the bubble, tip 12 out to the left, pointing at the orb beside it.
        Tail.Left -> quad(0f, size.height / 2, 2f, -9f, -3f, -2f, -12f, 1f, -3f, 3f, 2f, 9f)
        // Off the top-left corner, its sharp tip out to the left near the top.
        Tail.TopLeft -> quad(0f, 0f, 8f, 8f, -2f, 10f, -14f, 4f, -4f, 13f, 6f, 26f)
        // The user's side: TopLeft mirrored, its tip out to the right near the top.
        Tail.TopRight -> quad(size.width, 0f, -8f, 8f, 2f, 10f, 14f, 4f, 4f, 13f, -6f, 26f)
        Tail.None -> {}
    }
    return Path().apply { op(body, t, PathOperation.Union) }
}

internal fun DrawScope.drawBubble(tail: Tail, radius: Dp, fill: Color) {
    val path = bubblePath(size, tail, radius.toPx(), density).asAndroidPath()
    drawIntoCanvas { c ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = fill.toArgb()
            setShadowLayer(11 * density, 0f, 8 * density, hex(0x00280F, 0.13f).toArgb())
        }
        c.nativeCanvas.drawPath(path, paint)
        paint.setShadowLayer(1 * density, 0f, 0.5f * density, hex(0x001E0A, 0.10f).toArgb())
        c.nativeCanvas.drawPath(path, paint)
    }
}

/** The bubble's content: the typing dots, or the line itself. */
@Composable
private fun TypedText(talk: Talk, size: Float, lineHeight: Float, align: TextAlign, modifier: Modifier = Modifier) {
    if (talk.thinking) TypingDots((size * 0.36f).dp, modifier)
    else Txt(talk.text, dm(size, FontWeight.SemiBold, EQ.ink, lineHeight = lineHeight, tracking = -0.02f * size, align = align), modifier)
}

/** The standard “someone is typing” dots: three dots rising and brightening in turn. */
@Composable
fun TypingDots(size: Dp, modifier: Modifier = Modifier) {
    val clock = rememberWallClock()
    Row(modifier.padding(vertical = size * 0.5f), horizontalArrangement = Arrangement.spacedBy(size * 0.7f)) {
        repeat(3) { i ->
            Box(
                Modifier.size(size).graphicsLayer {
                    // Each dot pulses on a 1.2 s loop, a fifth of a loop after the one before.
                    var phase = (clock.value / 1.2f - i * 0.2f) % 1f
                    if (phase < 0) phase += 1f
                    val p = maxOf(0f, kotlin.math.sin(Math.PI.toFloat() * minOf(phase, 0.5f) * 2))
                    translationY = -size.toPx() * 0.45f * p
                    alpha = (0.28f + 0.4f * p) / 0.68f
                }.background(EQ.ink.copy(alpha = 0.68f), CircleShape)
            )
        }
    }
}

/**
 * Pops a bubble in, out of its orb, whenever [id] changes (once the orb has landed): it scales up
 * from small, slides away from the orb and fades from nothing. [fromLeft]: the orb is to its left
 * (the small orb), otherwise below. [away] (or a page change) sends it back the same way.
 */
@Composable
private fun BubblePop(id: Int, away: Boolean, origin: TransformOrigin, fromLeft: Boolean = false, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        shown = false
        p.snapTo(0f)
        OrbFlight.untilLanded()
        shown = true
    }
    val on = shown && !away && !BubbleStage.away
    LaunchedEffect(on) {
        if (on) p.animateTo(1f, tween(500, easing = EQ.pop)) else p.animateTo(0f, tween(180, easing = androidx.compose.animation.core.FastOutLinearInEasing))
    }
    Box(Modifier.graphicsLayer {
        val v = p.value
        scaleX = 0.3f + 0.7f * v; scaleY = scaleX
        if (fromLeft) translationX = -22 * density * (1 - v) else translationY = 22 * density * (1 - v)
        alpha = v.coerceIn(0f, 1f)
        transformOrigin = origin
    }) { content() }
}

/**
 * White speech bubble that rises out of the orb, tail pointing down at it. It hugs the text as
 * it types; the layout reserves room for the full line up front so typing never shifts the page.
 */
@Composable
fun TopBubble(talk: Talk, reserve: String, fontSize: Float = 18f, lineHeight: Float = 23f, maxWidth: Dp = 300.dp) {
    val fill by animateColorAsState(if (talk.alt) EQ.mintAlt else Color.White, tween(250), label = "bubble")
    val pad = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 15.dp)
    Layout(content = {
        Box(pad) { Txt(Catalog.translate(reserve, Language.current), dm(fontSize, FontWeight.SemiBold, lineHeight = lineHeight, tracking = -0.02f * fontSize, align = TextAlign.Center)) }
        BubblePop(talk.popId, talk.out, TransformOrigin(0.42f, 1.2f)) {
            Box(Modifier.drawBehind { drawBubble(Tail.Bottom, 22.dp, fill) }.then(pad)) {
                TypedText(talk, fontSize, lineHeight, TextAlign.Center)
            }
        }
    }) { ms, c ->
        val cc = c.copy(minWidth = 0, minHeight = 0, maxWidth = min(c.maxWidth, maxWidth.roundToPx()))
        val r = ms[0].measure(cc)
        val v = ms[1].measure(cc)
        layout(r.width, r.height) { v.place((r.width - v.width) / 2, r.height - v.height) }
    }
}

/**
 * The compact bubble beside a small orb (permissions and setup screens). The pointer comes out
 * of the middle, the top corner (first of a stack of messages), or not at all (the messages after
 * it); stacked messages hang from the top and grow down as they type. [hug] sizes it to its text.
 */
@Composable
fun SideBubble(talk: Talk, reserve: String, modifier: Modifier = Modifier, fontSize: Float = 17f, lineHeight: Float = 22f,
               padH: Dp = 16.dp, padV: Dp = 12.dp, radius: Dp = 18.dp, tail: Tail = Tail.Left, hug: Boolean = false) {
    val fill by animateColorAsState(if (talk.alt) EQ.mintAlt else Color.White, tween(250), label = "bubble")
    val pad = Modifier.padding(horizontal = padH, vertical = padV)
    val middle = tail == Tail.Left
    Box(modifier, contentAlignment = if (middle) Alignment.CenterStart else Alignment.TopStart) {
        Box(pad.alpha(0f)) { Txt(Catalog.translate(reserve, Language.current), dm(fontSize, FontWeight.SemiBold, lineHeight = lineHeight, tracking = -0.02f * fontSize)) }
        BubblePop(talk.popId, talk.out, TransformOrigin(-0.05f, if (middle) 0.5f else 0.1f), fromLeft = true) {
            // While the dots show, the bubble hugs them rather than stretching across.
            val wide = !hug && !talk.thinking
            Box((if (wide) Modifier.fillMaxWidth() else Modifier).drawBehind { drawBubble(tail, radius, fill) }.then(pad)) {
                TypedText(talk, fontSize, lineHeight, TextAlign.Start, if (wide) Modifier.fillMaxWidth() else Modifier)
            }
        }
    }
}

/** Big orb with the bubble above it (number, code, name, picks, welcome). */
@Composable
fun OrbWithBubble(
    talk: Talk, reserve: String,
    modifier: Modifier = Modifier, orbDiameter: Dp = 128.dp, spinning: Boolean = false,
    fontSize: Float = 18f, lineHeight: Float = 23f,
) {
    val clock = rememberWallClock()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.orbHover(orbDiameter, clock).zIndex(2f)) { TopBubble(talk, reserve, fontSize, lineHeight) }
        OrbSlot(orbDiameter, spinning = spinning)
    }
}

/**
 * Small orb (56 dp) with the bubble beside it: the bubble hangs from the orb's top, its pointer
 * on the top-left corner, and hugs its text (wrapping when it runs long).
 */
@Composable
fun OrbBesideBubble(
    talk: Talk, reserve: String, modifier: Modifier = Modifier, rings: Boolean = false,
    fontSize: Float = 17f, lineHeight: Float = 22f, padH: Dp = 16.dp, padV: Dp = 12.dp, radius: Dp = 18.dp,
) {
    val clock = rememberWallClock()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        OrbSlot(56.dp, rings = rings)
        SideBubble(talk, reserve, Modifier.weight(1f, fill = false).orbHover(56.dp, clock), fontSize, lineHeight, padH, padV, radius, Tail.TopLeft, hug = true)
    }
}

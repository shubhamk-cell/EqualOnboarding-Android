package ai.equal.orb.ui

// Shared pieces for the onboarding: voice previews, the check badge, a celebration burst and the
// gender mark.

import ai.equal.orb.model.AudioClips
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Gender
import ai.equal.orb.model.UseCase
import ai.equal.orb.model.Voice
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** SwiftUI's `.spring(response:dampingFraction:)` as a Compose spring. */
internal fun <T> iosSpring(response: Float, damping: Float): SpringSpec<T> =
    spring(dampingRatio = damping, stiffness = (2 * PI.toFloat() / response).pow(2))

/** True when the system's animations are off (Android's Reduce Motion). */
internal fun reduceMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

// MARK: - Speech

/**
 * The system's text-to-speech, one line at a time, its callbacks brought back to the main thread.
 * The engine starts on first use; a line asked for before it's ready waits for it.
 */
internal class Speech(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    /** null while the engine starts, then whether it can speak. */
    private var ready: Boolean? = null
    private var waiting: (() -> Unit)? = null
    private var counter = 0
    private var current: String? = null
    private var onRange: ((Int) -> Unit)? = null
    private var onEnd: ((Boolean) -> Unit)? = null

    private val tts = TextToSpeech(context.applicationContext) { status ->
        main.post {
            ready = status == TextToSpeech.SUCCESS
            waiting?.invoke(); waiting = null
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { main.post { end(id) } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { main.post { end(id) } }
            override fun onStop(id: String?, interrupted: Boolean) { main.post { end(id) } }
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                main.post { if (id == current) onRange?.invoke(end) }
            }
        })
    }

    private fun end(id: String?) {
        if (id == null || id != current) return
        val done = onEnd
        current = null; onRange = null; onEnd = null
        done?.invoke(true)
    }

    /**
     * Says [text] in [code] (falling back to Indian English) at [pitch] and [rate]. [onRange] gets
     * the end of each word as it starts; [onEnd] gets true once it's said, or false straight away
     * when there's no speech engine. A line that's stopped never ends.
     */
    fun speak(text: String, code: String, pitch: Float, rate: Float, onRange: (Int) -> Unit = {}, onEnd: (Boolean) -> Unit = {}) {
        stop()
        val go: () -> Unit = {
            if (ready != true) onEnd(false)
            else {
                val locale = Locale.forLanguageTag(code)
                if (tts.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) tts.setLanguage(Locale.forLanguageTag("en-IN"))
                tts.setPitch(pitch)
                tts.setSpeechRate(rate)
                val id = "line${++counter}"
                current = id
                this.onRange = onRange
                this.onEnd = onEnd
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
        if (ready == null) waiting = go else go()
    }

    fun stop() {
        waiting = null
        current = null; onRange = null; onEnd = null
        if (ready == true) tts.stop()
    }

    fun release() {
        stop()
        tts.shutdown()
    }
}

// MARK: - Voice previews

/**
 * Previews Equal's voices, one at a time and only on tap: Aanya and Kabir from their recordings
 * (`AudioClips.hello`, English and Hindi), Meera and Arjun (and languages without a recording)
 * with the system's text-to-speech at the voice's pitch and rate.
 * [speaking] is the voice playing (null when quiet); [cardLanguage] the language card that started it.
 */
class VoicePreview(private val context: Context) {
    var speaking by mutableStateOf<Voice?>(null)
    /** The language card playing, when a preview was started from one (not from a voice card). */
    var cardLanguage by mutableStateOf<CallLanguage?>(null)
    /** 0…1 through the line. */
    var progress by mutableFloatStateOf(0f)
    /** The part of the line said so far. */
    var spoken by mutableStateOf("")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var speech: Speech? = null
    private var recording: MediaPlayer? = null
    private var ticker: Job? = null
    private var line = ""

    private fun speech() = speech ?: Speech(context).also { speech = it }

    /** Plays [v] (or stops it if it's the one playing). */
    fun toggle(v: Voice, language: CallLanguage, name: String) {
        if (speaking == v && cardLanguage == null) { stop(); return }
        play(v, language, name)
    }

    /** A language card's play button: [l] in [voice] (or stops it). */
    fun toggleCard(l: CallLanguage, voice: Voice, name: String) {
        if (cardLanguage == l && speaking != null) { stop(); return }
        play(voice, l, name)
        if (speaking != null) cardLanguage = l
    }

    fun play(v: Voice, language: CallLanguage, name: String) {
        cardLanguage = null
        halt()
        val text = Catalog.voiceHello(v, language, name)
        line = text
        val p = AudioClips.hello(context, v, language)?.let { MediaPlayer.create(context, it) }
        if (p != null) {
            recording = p
            speaking = v
            spoken = text
            progress = 0f
            p.setOnCompletionListener {
                if (recording !== p) return@setOnCompletionListener
                progress = 1f
                halt()
                speaking = null; cardLanguage = null
            }
            p.start()
            ticker = scope.launch {
                while (recording === p) {
                    val d = p.duration
                    progress = if (d > 0) p.currentPosition.toFloat() / d else 0f
                    delay(50)
                }
            }
            return
        }
        speaking = v
        spoken = ""
        progress = 0f
        speech().speak(
            text, language.speechCode, v.pitch, v.rate,
            onRange = { end ->
                spoken = text.take(end)
                progress = end.toFloat() / text.length.coerceAtLeast(1)
            },
            onEnd = {
                if (line == text) {
                    spoken = text
                    speaking = null; cardLanguage = null
                }
            },
        )
    }

    fun stop() {
        halt()
        speaking = null
        cardLanguage = null
    }

    private fun halt() {
        ticker?.cancel(); ticker = null
        recording?.release(); recording = null
        speech?.stop()
    }

    /** Frees the speech engine and player (on dispose). */
    fun release() {
        halt()
        speech?.release(); speech = null
        scope.cancel()
    }
}

/** A [VoicePreview] tied to the composition (stopped and released when it leaves). */
@Composable
fun rememberVoicePreview(): VoicePreview {
    val c = LocalContext.current
    val preview = remember { VoicePreview(c) }
    DisposableEffect(preview) { onDispose { preview.stop(); preview.release() } }
    return preview
}

// MARK: - Check badge

/** Round green tick badge on a picked card; hidden (shrunk away) when off. */
@Composable
fun CheckBadge(on: Boolean, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(if (on) 1f else 0f, iosSpring(0.3f, 0.55f), label = "check")
    Box(
        modifier
            .size(20.dp)
            .graphicsLayer {
                val s = 0.4f + 0.6f * p
                scaleX = s; scaleY = s
                alpha = p.coerceIn(0f, 1f)
            }
            .background(EQ.green, CircleShape)
            .border(2.dp, EQ.page, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(rememberVectorPainter(Icons.Filled.Check), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(Color.White))
    }
}

// MARK: - Celebration

private val burstColours = listOf(0xBAFF29L, 0x8CEA35L, 0x00B140L, 0xFFE25CL, 0x5BC8F5L, 0xFFC59EL)

/**
 * A one-shot burst of confetti dots from the middle of the view each time [trigger] changes (not
 * on first show). Skipped when the system's animations are off.
 */
@Composable
fun CelebrationBurst(trigger: Int, modifier: Modifier = Modifier, spread: Dp = 120.dp) {
    val context = LocalContext.current
    var t by remember { mutableFloatStateOf(-1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first) { first = false; return@LaunchedEffect }
        if (reduceMotion(context)) return@LaunchedEffect
        val start = withFrameNanos { it }
        do {
            withFrameNanos { t = (it - start) / 1e9f }
        } while (t < 1.2f)
        t = -1f
    }
    Canvas(modifier) {
        if (t < 0f || t >= 1.2f) return@Canvas
        val e = 1 - (1 - min(1f, t / 0.9f)).pow(3)
        val reach = spread.toPx()
        val fade = (1 - t / 1.2f).coerceIn(0f, 1f)
        for (i in 0 until 22) {
            val a = i / 22.0 * 2 * PI + trigger
            val dist = reach * (0.55f + 0.45f * abs(sin(i * 2.3)).toFloat()) * e
            val p = Offset(
                center.x + cos(a).toFloat() * dist,
                center.y + sin(a).toFloat() * dist + 30.dp.toPx() * t * t,
            )
            val r = (if (i % 3 == 0) 4 else 3).dp.toPx()
            drawCircle(hex(burstColours[i % burstColours.size], fade), r, p)
        }
    }
}

// MARK: - Gender mark

/**
 * The faint gender symbol in a card's corner, drawn from the design's vectors and partly cut off by
 * the card's edge: ♂ at the top left, ♀ at the bottom left (the gender and voice cards), in the
 * gender's mark colour. Put it in the card's top-start corner, clipped by the card.
 */
@Composable
fun GenderMark(gender: Gender, modifier: Modifier = Modifier) {
    // Neutral on every card: the shape alone says which is which.
    val colour = EQ.ink
    val male = gender == Gender.Male
    Canvas(
        modifier
            .offset(if (male) (-22).dp else (-30).dp, if (male) (-10).dp else 40.dp)
            .size(if (male) 95.dp else 109.dp, if (male) 103.dp else 118.5.dp)
            .graphicsLayer { alpha = 0.05f },
    ) {
        val u = density
        fun pt(x: Double, y: Double) = Offset((x * u).toFloat(), (y * u).toFloat())
        if (male) {
            val arrow = Path().apply {
                val start = pt(76.0, 11.9)
                moveTo(start.x, start.y)
                for ((x, y) in listOf(58.2 to 31.8, 68.0 to 39.2, 84.6 to 20.5, 84.6 to 34.1, 95.0 to 34.1, 95.0 to 0.0, 61.2 to 0.0, 61.2 to 11.9)) {
                    val p = pt(x, y); lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(arrow, colour)
            drawCircle(colour, 31.8f * u, pt(38.9, 64.1), style = Stroke(14.25f * u))
        } else {
            drawCircle(colour, 36.5f * u, pt(54.3, 44.7), style = Stroke(16.4f * u))
            drawLine(colour, pt(56.0, 80.6), pt(56.0, 118.5), 10.9f * u, StrokeCap.Butt)
            drawLine(colour, pt(34.8, 100.7), pt(75.8, 100.7), 10.9f * u, StrokeCap.Butt)
        }
    }
}

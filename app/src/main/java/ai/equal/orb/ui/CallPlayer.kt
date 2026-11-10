package ai.equal.orb.ui

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.AudioClips
import ai.equal.orb.model.CallLanguage
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.UseCase
import ai.equal.orb.model.Voice
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Plays the sample calls on the picks page: each line's recording, or text-to-speech.

/**
 * Plays a sample call line by line: each line's natural recording (res/raw, see [AudioClips]) when
 * there is one, otherwise text-to-speech; a short gap between lines. Progress and the clock follow
 * the recordings' own lengths (text-to-speech lines are estimated from their length). Pausing
 * restarts the current line on resume. All state is Compose state, changed on the main thread.
 */
class CallPlayer(private val context: Context) {
    /** A call's lines (speaker, text) and each line's recording (raw resource id, or null); [language] for text-to-speech. */
    data class Script(
        val lines: List<Pair<String, String>>, val audio: List<Int?>, val voice: Voice,
        val language: CallLanguage = CallLanguage.English,
    )

    var id by mutableStateOf<String?>(null); private set
    var playing by mutableStateOf(false); private set
    var finished by mutableStateOf(false); private set
    /** 0…1 through the call. */
    var progress by mutableFloatStateOf(0f); private set
    /** Seconds into the call. */
    var elapsed by mutableFloatStateOf(0f); private set
    var lineIndex by mutableIntStateOf(0); private set
    /** 1, 1.5 or 2. */
    var speed by mutableFloatStateOf(1f); private set

    private var script = Script(emptyList(), emptyList(), Voice.Aanya)
    /** Each line's length in seconds (the recording's, or an estimate). */
    private var lengths by mutableStateOf<List<Float>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var task: Job? = null
    private var clock: Job? = null
    private var audio: MediaPlayer? = null
    private var speech: Speech? = null

    private fun speech() = speech ?: Speech(context).also { speech = it }

    private val total: Float get() = maxOf(0.1f, lengths.sum())
    /** The call's length in seconds at the current speed. */
    val duration: Float get() = (total + lengths.size * GAP) / speed
    /** “1x”, “1.5x”, “2x”. */
    val speedLabel: String get() = if (speed == 1f) "1x" else if (speed == 1.5f) "1.5x" else "2x"

    /** “0:07”. */
    fun timeLabel(seconds: Float): String {
        val s = seconds.roundToInt()
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun load(script: Script, id: String) {
        stop()
        this.script = script
        this.id = id
        lengths = script.lines.mapIndexed { i, l ->
            script.audio.getOrNull(i)?.let { AudioClips.seconds(context, it) }?.takeIf { it > 0f }
                ?: (l.second.length * 0.065f + 0.3f)
        }
        finished = false
        progress = 0f; elapsed = 0f; lineIndex = 0
    }

    fun play() {
        if (script.lines.isEmpty()) return
        if (finished) { finished = false; progress = 0f; elapsed = 0f; lineIndex = 0 }
        playing = true
        val from = lineIndex
        task = scope.launch { run(from) }
        clock = scope.launch {
            while (true) {
                delay(100)
                elapsed = minOf(duration, elapsed + 0.1f)
            }
        }
    }

    fun pause() { playing = false; halt() }
    fun stop() { playing = false; halt() }

    fun cycleSpeed() {
        speed = if (speed == 1f) 1.5f else if (speed == 1.5f) 2f else 1f
        // Pick the new pace up from the current line.
        if (playing) { halt(); play() }
    }

    /** Frees the player and speech engine (on dispose). */
    fun release() {
        stop()
        speech?.release(); speech = null
        scope.cancel()
    }

    private fun halt() {
        task?.cancel(); task = null
        clock?.cancel(); clock = null
        audio?.release(); audio = null
        speech?.stop()
    }

    private suspend fun run(start: Int) {
        for (i in start until script.lines.size) {
            lineIndex = i
            val before = lengths.take(i).sum()
            val res = script.audio.getOrNull(i)
            if (res != null) playFile(res, before) else speak(script.lines[i], before, lengths[i])
            progress = minOf(1f, (before + lengths[i]) / total)
            delay((GAP / speed * 1000).toLong())
        }
        playing = false
        clock?.cancel(); clock = null
        elapsed = duration
        progress = 1f
        finished = true
    }

    private suspend fun playFile(res: Int, before: Float) {
        val p = MediaPlayer.create(context, res) ?: return
        audio = p
        val done = CompletableDeferred<Unit>()
        p.setOnCompletionListener { done.complete(Unit) }
        p.setOnErrorListener { _, _, _ -> done.complete(Unit); true }
        try {
            p.start()
            if (speed != 1f) p.playbackParams = p.playbackParams.setSpeed(speed)
            while (!done.isCompleted) {
                runCatching { p.currentPosition }.getOrNull()?.let { progress = minOf(1f, (before + it / 1000f) / total) }
                delay(50)
            }
        } finally {
            if (audio === p) { audio = null; p.release() }
        }
    }

    // MARK: System voice (fallback)

    private suspend fun speak(l: Pair<String, String>, before: Float, length: Float) {
        val isEqual = l.first == "Equal AI"
        val text = AudioClips.spoken(l.second)
        val v = script.voice
        val pitch = if (isEqual) v.pitch else if (v.pitch < 1f) 1.15f else 0.85f
        val rate = (if (isEqual) v.rate else 1f) * (if (speed == 1f) 1f else if (speed == 1.5f) 1.2f else 1.38f)
        val spoke = suspendCancellableCoroutine { c ->
            speech().speak(
                text, script.language.speechCode, pitch, rate,
                onRange = { end ->
                    if (playing) progress = minOf(1f, (before + end.toFloat() / text.length.coerceAtLeast(1) * length) / total)
                },
                onEnd = { ok -> if (c.isActive) c.resume(ok) },
            )
        }
        // No speech engine: let the line's time pass in silence so the call still runs.
        if (!spoke) delay((length / speed * 1000).toLong())
    }

    companion object {
        private const val GAP = 0.35f

        /** The call's script and recordings for [u]. */
        fun make(context: Context, u: UseCase, voice: Voice, name: String): Script {
            val lines = Catalog.script(u, name)
            val audio = lines.mapIndexed { i, l -> AudioClips.raw(context, u.id, i, l.first == "Equal AI", voice) }
            return Script(lines, audio, voice)
        }
    }
}

/** A [CallPlayer] tied to the composition (stopped and released when it leaves). */
@Composable
fun rememberCallPlayer(): CallPlayer {
    val c = LocalContext.current
    val player = remember { CallPlayer(c) }
    DisposableEffect(player) { onDispose { player.stop(); player.release() } }
    return player
}

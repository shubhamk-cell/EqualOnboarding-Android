package ai.equal.orb.ui

import ai.equal.orb.R
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The background behind every onboarding screen (over the app's white). Restrained on purpose: white, lit by three large,
 * soft pools of Equal's greens (emerald behind the orb, lime low on the left, teal on the right)
 * that drift and breathe slowly, with a fine film grain that keeps the colour from looking flat or
 * digital. Equal's doodles sit over the top, fading out by the middle of the screen.
 */
@Composable
fun Sky(modifier: Modifier = Modifier) {
    // One cached layer: the page is only redrawn when the pools move, not every time the orb does.
    BoxWithConstraints(modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        Spotlight()
        val grain = remember { grainBrush() }
        // Drawn once on its own layer; the pools moving under it don't redraw it.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { drawRect(grain, alpha = 0.9f) }
        Box(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.6f)
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.35f to Color.Black.copy(alpha = 0.6f), 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Image(
                painterResource(R.drawable.doodles), null,
                Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                colorFilter = ColorFilter.tint(hex(0x0B4A30, 0.09f)),
            )
        }
    }
}

private fun wave(t: Float, period: Float, phase: Float) = sin(t / period * 2 * PI.toFloat() + phase)

/** One pool of light: its colour and strength, where it sits (fractions of the screen), its size (of the width) and drift. */
private class Pool(val color: Long, val alpha: Float, val x: Float, val y: Float, val r: Float, val drift: Float, val period: Float, val phase: Float)

private val pools = listOf(
    Pool(0x22C76A, 0.20f, 0.55f, 0.30f, 0.95f, 0.10f, 18f, 0f),
    Pool(0xB9EE5A, 0.22f, 0.05f, 0.85f, 0.90f, 0.12f, 22f, 2f),
    Pool(0x4FD6B8, 0.16f, 1.00f, 0.60f, 0.85f, 0.12f, 20f, 4f),
)

/** Large, smooth pools of light on white, drifting and breathing slowly. */
@Composable
private fun Spotlight() {
    val clock = rememberWallClock()
    // They move so slowly that 12 redraws a second look the same as every frame.
    val t by remember { derivedStateOf { kotlin.math.floor(clock.value * 12f) / 12f } }
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val w = size.width
        for (p in pools) {
            val r = w * p.r * (1 + 0.08f * wave(t, p.period * 0.7f, p.phase))
            val c = Offset(
                w * (p.x + p.drift * wave(t, p.period, p.phase)),
                size.height * p.y + w * p.drift * wave(t, p.period * 1.3f, p.phase + 1),
            )
            val color = hex(p.color)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = p.alpha), color.copy(alpha = 0f)), c, r), r, c)
        }
    }
}

/** Fine film grain, made once: tiny light and dark specks on a repeating tile. */
private fun grainBrush(): ShaderBrush {
    val size = 256
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rng = Random(7)
    repeat(size * size / 9) {
        val x = rng.nextInt(size)
        val y = rng.nextInt(size)
        bmp.setPixel(x, y, if (rng.nextBoolean()) android.graphics.Color.argb(13, 0, 0, 0) else android.graphics.Color.argb(128, 255, 255, 255))
    }
    return ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

package ai.equal.orb.ui

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.BgSetup
import ai.equal.orb.model.Step
import ai.equal.orb.model.Variant
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The SIM-first variant only: what Equal is quietly doing behind the sample calls and the voice
 * step. One slim strip under the top bar, never over the way on and never in the way. It works
 * while the user plays, says so when it's done and takes itself away, and if it couldn't, it says
 * that too and offers the walkthrough there and then. Nothing on the page waits for it.
 *
 * For demos, a long-press on the strip gives up on the attempt, so the failure path can be shown
 * on a phone where it would otherwise succeed.
 */
@Composable
fun BackgroundSetupStrip(model: AppModel) {
    val context = LocalContext.current
    val onSetupPages = model.step == Step.Cases || model.step == Step.Assistant

    // Started when the sample calls open — and only then, so coming back to them doesn't restart
    // it. By now the user has said which SIM Equal answers on, so there's nothing left to guess.
    LaunchedEffect(model.step, model.variant) {
        if (model.variant == Variant.SimFirst && model.step == Step.Cases) {
            model.startBackgroundSetup(Device.forwardingBlock(context, simChosen = model.sim != null))
        }
    }

    val show = onSetupPages && model.bgSetup != BgSetup.Off &&
        !(model.bgSetup == BgSetup.Done && model.bgStripGone)
    AnimatedVisibility(
        show,
        enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = EQ.ease)),
        exit = fadeOut(tween(400)) + shrinkVertically(tween(400, easing = EQ.ease)),
    ) {
        val failed = model.bgSetup == BgSetup.Failed
        val done = model.bgSetup == BgSetup.Done
        val text = when {
            done -> "Call forwarding is on"
            failed -> "Couldn’t set this up — we’ll do it at the end"
            else -> "Setting you up in the background"
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .height(32.dp)
                    .pointerInput(failed) {
                        detectTapGestures(
                            // Tapping the failure goes and does it now, rather than at the end.
                            onTap = { if (failed) { Haptics.step(); model.fallBackToForwarding() } },
                            // For demos: give up on an attempt that would otherwise succeed.
                            onLongPress = { model.failBackgroundSetup() },
                        )
                    }
                    .glass(RoundedCornerShape(50), tint = 0.42f, shadow = false)
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = text },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    done -> Image(
                        rememberVectorPainter(Icons.Filled.Check), null,
                        Modifier.size(13.dp), colorFilter = ColorFilter.tint(EQ.green),
                    )
                    failed -> Box(Modifier.size(7.dp).background(hex(0xE09200), CircleShape))
                    else -> PulsingDot()
                }
                Txt(text, dm(13f, FontWeight.Medium, EQ.ink.copy(alpha = if (failed) 0.75f else 0.6f)))
            }
        }
    }
}

/** A green dot breathing while the attempt is under way. */
@Composable
private fun PulsingDot() {
    val pulse = rememberInfiniteTransition(label = "dot")
    val a by pulse.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900, easing = EQ.ease), RepeatMode.Reverse), label = "a",
    )
    Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.background(EQ.green, CircleShape))
}

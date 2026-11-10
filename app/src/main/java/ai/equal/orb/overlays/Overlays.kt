package ai.equal.orb.overlays

import androidx.compose.ui.res.painterResource
import ai.equal.orb.model.art
import ai.equal.orb.model.Permission
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Overlay
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Draws whichever modal surface the model is presenting. Each one animates itself in and,
 * when closed, animates out before handing its result back to the flow.
 */
@Composable
fun OverlayHost(model: AppModel) {
    when (val o = model.overlay) {
        is Overlay.Decline -> DeclineSheet(model, o.missing)
        null -> {}
    }
}

/** Closing an overlay: a success tap when the user allows, a plain step otherwise. */
private fun closeHaptic(result: String) { if (result == "allow") Haptics.success() else Haptics.step() }

/**
 * Shown once both system prompts have been answered (or from Home's banner), in one of three
 * versions: notifications refused, contacts refused, or both.
 */
@Composable
private fun DeclineSheet(model: AppModel, missing: List<Permission>) {
    val both = missing.size > 1
    val notifications = missing == listOf(Permission.Notifications)
    val title = when {
        both -> "Enable Permissions"
        notifications -> "Notification permission required"
        else -> "Contact permission required"
    }
    val message = when {
        both -> "Allow notifications and contacts access to recognise calls and update you in real time."
        notifications -> "Allow Equal AI to send you notifications to avoid missing important updates"
        else -> "Allow Equal AI to access your contacts so I can recognise the people you know when they call"
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(420, easing = EQ.push)) }
    fun close(r: String) = scope.launch { closeHaptic(r); p.animateTo(0f, tween(380, easing = EQ.push)); model.resolve(r) }
    BackHandler { close("dismiss") }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = p.value }.background(Color.Black.copy(alpha = 0.38f))
                .clickable(remember { MutableInteractionSource() }, indication = null) { close("dismiss") }
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = size.height * 1.05f * (1 - p.value) }
                .background(EQ.page, RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(missing.art), null,
                    Modifier.padding(top = 4.dp, bottom = 14.dp).size(112.dp),
                )
                Txt(
                    title,
                    dm(22f, FontWeight.Bold, EQ.ink, lineHeight = 28f, tracking = -0.44f, align = TextAlign.Center),
                )
                Txt(
                    message,
                    dm(16f, color = EQ.ink.copy(alpha = 0.6f), lineHeight = 23f, align = TextAlign.Center),
                    Modifier.widthIn(max = 320.dp).padding(top = 10.dp, bottom = 24.dp),
                )
                Row(
                    Modifier.fillMaxWidth().height(56.dp)
                        .pressable {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            model.showToast("Allowed in Settings")
                            close("settings")
                        }
                        .shadow(10.dp, RoundedCornerShape(50), spotColor = EQ.green.copy(alpha = 0.4f))
                        .background(EQ.green, RoundedCornerShape(50)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(rememberVectorPainter(Icons.Filled.Settings), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                    Txt("Open settings", dm(17f, FontWeight.SemiBold, Color.White))
                }
                Box(Modifier.padding(top = 14.dp).pressable { close("skip") }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Txt("Continue anyway", dm(16f, FontWeight.SemiBold, EQ.ink).copy(textDecoration = TextDecoration.Underline))
                }
            }
            Box(
                Modifier.align(Alignment.TopEnd).size(48.dp).pressable { close("dismiss") }
                    .shadow(4.dp, CircleShape).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Image(rememberVectorPainter(Icons.Filled.Close), "Close", Modifier.size(20.dp), colorFilter = ColorFilter.tint(EQ.ink)) }
        }
    }
}

/** Toast pill above the bottom. */
@Composable
fun ToastHost(text: String?) {
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(text != null, enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.9f), exit = fadeOut(tween(250))) {
            var last by remember { mutableStateOf("") }
            if (text != null) last = text
            Box(
                Modifier.shadow(12.dp, RoundedCornerShape(50)).background(hex(0x161618, 0.92f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Txt(last, dm(13f, FontWeight.Medium, Color.White, align = TextAlign.Center)) }
        }
    }
}

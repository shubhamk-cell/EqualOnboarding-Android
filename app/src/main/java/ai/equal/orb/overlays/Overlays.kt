package ai.equal.orb.overlays

import androidx.compose.ui.res.painterResource
import ai.equal.orb.model.art
import ai.equal.orb.model.Permission
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Overlay
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.Orb
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Sky
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.afterOrb
import ai.equal.orb.ui.TopBubble
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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

/**
 * Draws whichever modal surface the model is presenting. Each one animates itself in and,
 * when closed, animates out before handing its result back to the flow.
 */
@Composable
fun OverlayHost(model: AppModel) {
    when (val o = model.overlay) {
        is Overlay.Blocked -> BlockedScreen(model, o.missing, o.dismissible, o.demo)
        null -> {}
    }
}

/**
 * The hard block: a permission Equal can't work without was refused, so this stands in the flow's
 * way on the page's own background, lists what's missing and offers Settings. It watches for the
 * answer changing while the user is away, and as soon as everything is granted the flow carries
 * on by itself — there is no way past it otherwise.
 */
@Composable
private fun BlockedScreen(model: AppModel, missing: List<Permission>, dismissible: Boolean, demo: Boolean = false) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(380, easing = EQ.push)) }
    // Coming back from Settings: read what Android says now, and go on if it's all there.
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && !demo) {
                model.refreshPermissions(context)
                if (model.missingPermissions.none { it in missing }) {
                    if (dismissible) model.overlay = null else model.permissionsSatisfied()
                }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // Nothing goes past this but Settings.
    BackHandler(enabled = !dismissible) {}

    // The orb asks for it in its own voice, as it does on every other page.
    val one = missing.size == 1
    val line = when {
        missing == listOf(Permission.Notifications) -> "Turn notifications on and I’ll tell you about the calls I take"
        one && missing[0] == Permission.Contacts -> "Let me see your contacts and I’ll know who’s calling"
        one && missing[0] == Permission.Calls -> "Let me make and manage calls, and I can take them for you"
        missing.size == 2 -> "I need a couple of things switched on before I can answer calls for you"
        else -> "I need these switched on before I can answer calls for you"
    }
    val talk = rememberTalk()
    // Whatever was being typed underneath loses focus: nothing here is a field.
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(line) { keyboard?.hide(); focus.clearFocus(force = true); talk.intro(line) }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = p.value }
            // The same page as every other step, so the block doesn't read as another app. The
            // background runs edge to edge; only its contents keep clear of the system bars.
            .background(Color.White)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Sky()
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
        if (dismissible) Box(
            Modifier.align(Alignment.TopEnd).size(44.dp).pressable { Haptics.step(); model.overlay = null }
                .shadow(4.dp, CircleShape).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Image(rememberVectorPainter(Icons.Filled.Close), "Close", Modifier.size(20.dp), colorFilter = ColorFilter.tint(EQ.ink)) }

        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().graphicsLayer { translationY = 24 * density * (1 - p.value) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The orb and its message, as on every other page; the flow's own orb is on the page
            // underneath, so this one is its own.
            TopBubble(talk, line)
            Spacer(Modifier.height(16.dp))
            Orb(diameter = 124.dp, interactive = false)
            Spacer(Modifier.height(28.dp))
            // What's still off: what it is, why Equal needs it, and that it isn't on.
            Column(Modifier.fillMaxWidth().afterOrb(talk), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (m in missing) PermissionCard(m)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().afterOrb(talk, index = 1), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().height(56.dp)
                    .pressable {
                        Haptics.step()
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .shadow(10.dp, RoundedCornerShape(50), spotColor = EQ.green.copy(alpha = 0.4f))
                    .background(EQ.green, RoundedCornerShape(50)),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(rememberVectorPainter(Icons.Filled.Settings), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(Color.White))
                Txt("Open settings", dm(17f, FontWeight.SemiBold, Color.White))
            }
            Txt(
                "This carries on by itself once they're on.",
                dm(13.5f, color = EQ.ink.copy(alpha = 0.5f), align = TextAlign.Center),
                Modifier.padding(top = 12.dp),
            )
        }
        }
    }
}

/** One permission Equal is waiting on: its icon, what it is, why it's needed, and that it's off. */
@Composable
private fun PermissionCard(permission: Permission) {
    val icon = when (permission) {
        Permission.Notifications -> Icons.Filled.Notifications
        Permission.Contacts -> Icons.Filled.Person
        Permission.Calls -> Icons.Filled.Phone
    }
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = hex(0x0B1F12, 0.05f), spotColor = hex(0x0B1F12, 0.06f))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, EQ.edge, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(EQ.mintAlt, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(rememberVectorPainter(icon), null, Modifier.size(21.dp), colorFilter = ColorFilter.tint(EQ.greenText))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(permission.label, dm(15.5f, FontWeight.SemiBold, EQ.ink))
            Txt(permission.reason, dm(12.5f, color = EQ.ink.copy(alpha = 0.55f), lineHeight = 16f))
        }
        Box(
            Modifier.background(EQ.danger.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
        ) { Txt("Off", dm(12.5f, FontWeight.SemiBold, EQ.danger)) }
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

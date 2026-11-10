package ai.equal.orb.screens

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Step
import ai.equal.orb.ui.CelebrationBurst
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.LinkButton
import ai.equal.orb.ui.Orb
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.SideBubble
import ai.equal.orb.ui.Tail
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.WrapRows
import ai.equal.orb.ui.bezierEase
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.orbHover
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberClock
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.rememberWallClock
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// The profile tab, where the onboarding is started.

@Composable
private fun Icon(v: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) =
    Image(rememberVectorPainter(v), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))

// MARK: - Status

/** Whether Equal is taking calls: a green “Live” with a breathing dot, or a muted “Off”. */
@Composable
fun StatusPill(live: Boolean, modifier: Modifier = Modifier) {
    val orange = hex(0xE06500)
    val spring = spring<Color>(dampingRatio = 0.7f, stiffness = 247f)
    val fill by animateColorAsState(if (live) EQ.mintAlt else hex(0xFFF1E6), spring, label = "pill")
    val edge by animateColorAsState(if (live) DS.primary.copy(alpha = 0.35f) else orange.copy(alpha = 0.35f), spring, label = "edge")
    val dot by animateColorAsState(if (live) DS.primary else orange, spring, label = "dot")
    val clock = rememberWallClock()
    val shape = RoundedCornerShape(50)
    Row(
        modifier.height(34.dp).background(fill, shape).border(1.dp, edge, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(8.dp)) {
            val r = size.minDimension / 2
            if (live) {
                val p = (clock.value / 1.6f) % 1f
                drawCircle(DS.primary.copy(alpha = 0.4f * (1 - p)), r * (1 + p * 1.4f))
            }
            drawCircle(dot, r)
        }
        Txt(if (live) "Live" else "Off", dm(14f, FontWeight.SemiBold, if (live) EQ.greenText else hex(0xB45309)))
    }
}

// MARK: - Profile

/**
 * The profile tab: who you are, and the onboarding with its building blocks. Tapping it runs it
 * from the splash; it ends back here on Home.
 */
@Composable
fun ProfileScreen(model: AppModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DS.listBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 24.dp, bottom = NAV_CLEARANCE),
    ) {
        ProfileHeader(model, Modifier.padding(bottom = 28.dp))
        Txt("ONBOARDING", dm(12f, FontWeight.SemiBold, DS.meta, tracking = 0.6f))
        Txt(
            "Tap to run it from the start. It ends back here on Home.",
            dm(14f, color = DS.meta, lineHeight = 19f), Modifier.padding(top = 4.dp, bottom = 14.dp),
        )
        OnboardingRow {
            Haptics.step()
            model.restart()
        }
    }
}

@Composable
private fun ProfileHeader(model: AppModel, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.size(64.dp).background(Brush.linearGradient(listOf(EQ.voiceTop, DS.primary)), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Txt(model.displayName.take(1), dm(26f, FontWeight.Bold, Color.White)) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(model.displayName, dm(22f, FontWeight.Bold, DS.onBackground))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Txt(if (model.phone.isEmpty()) "+91 98765 43210" else "+91 ${model.phone}", dm(14f, color = DS.meta))
                StatusPill(true, Modifier.graphicsLayer { scaleX = 0.85f; scaleY = 0.85f })
            }
        }
    }
}

/** The onboarding: its name and what it's about, and its blocks in order as coloured chips. */
@Composable
private fun OnboardingRow(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(scale = 0.98f) { onClick() }
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
            .background(Color.White, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("Pick 3, hear them in place", dm(16.5f, FontWeight.SemiBold, DS.name, lineHeight = 21f))
            Txt(
                "A 6-digit code, then name and how to address you on one page. Pick exactly 3 calls; they turn into " +
                    "playable samples right where they are. Then choose how your assistant sounds.",
                dm(13.5f, color = DS.meta, lineHeight = 18f),
            )
            val blocks = listOf("Sign-up" to hex(0x1F1F1F), "Show value" to hex(0x16A34A), "Set up" to hex(0x2563EB), "Home" to hex(0x8A8A8A))
            WrapRows(3.dp, Modifier.padding(top = 2.dp)) {
                blocks.forEachIndexed { i, (title, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (i > 0) Icon(Icons.Filled.KeyboardArrowRight, DS.meta.copy(alpha = 0.5f), 10.dp)
                        Box(Modifier.height(19.dp).background(color, RoundedCornerShape(50)).padding(horizontal = 6.5.dp), contentAlignment = Alignment.Center) {
                            Txt(title, dm(10.5f, FontWeight.SemiBold, Color.White))
                        }
                    }
                }
            }
        }
        // `play.circle.fill`, green with a mint triangle.
        Box(
            Modifier.padding(top = 8.dp).size(24.dp).background(DS.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.PlayArrow, EQ.mintAlt, 16.dp) }
    }
}

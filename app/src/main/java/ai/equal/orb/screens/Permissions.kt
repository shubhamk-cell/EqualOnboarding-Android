package ai.equal.orb.screens

import androidx.compose.ui.draw.alpha
import ai.equal.orb.ui.rememberWallClock
import ai.equal.orb.ui.orbHover
import ai.equal.orb.ui.Tail
import ai.equal.orb.ui.SideBubble
import ai.equal.orb.ui.OrbSlot
import ai.equal.orb.ui.OrbFlight
import ai.equal.orb.model.Permission
import ai.equal.orb.R
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Overlay
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.afterOrb
import ai.equal.orb.ui.CelebrationBurst
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.requiredSize
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.rich
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import ai.equal.orb.model.Step
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight

/** Asks for one Android runtime permission and suspends until the user answers. */
@Composable
private fun rememberPermissionAsker(): suspend (String) -> Boolean {
    val context = LocalContext.current
    val pending = remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        pending.value?.complete(ok); pending.value = null
    }
    val launch by rememberUpdatedState(launcher)
    return remember {
        val asker: suspend (String) -> Boolean = { perm ->
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) true
            else {
                val d = CompletableDeferred<Boolean>()
                pending.value = d
                launch.launch(perm)
                d.await()
            }
        }
        asker
    }
}

// MARK: - Permissions

/**
 * The permission popups, right after the code is verified ([AppModel.permissionsDue]), over the
 * name page: Android's notifications prompt, then contacts. If either was refused, Equal's own
 * sheet asks once for whatever is missing.
 * Draws nothing itself; lives at the app's root so it isn't tied to a screen.
 */
@Composable
fun PermissionPrompts(model: AppModel) {
    val ask = rememberPermissionAsker()
    val context = LocalContext.current
    LaunchedEffect(model.permissionsDue) {
        if (!model.permissionsDue) return@LaunchedEffect
        // Let the name page arrive first.
        delay(900)
        val notifications = if (Build.VERSION.SDK_INT >= 33) ask(Manifest.permission.POST_NOTIFICATIONS)
        else NotificationManagerCompat.from(context).areNotificationsEnabled()
        delay(250)
        val contacts = ask(Manifest.permission.READ_CONTACTS)

        val missing = buildList {
            if (!notifications) add(Permission.Notifications)
            if (!contacts) add(Permission.Contacts)
        }
        val fixed = missing.isNotEmpty() && model.present(Overlay.Decline(missing)) == "settings"
        model.perms = listOf(notifications || fixed, contacts || fixed)
        if (notifications && contacts) Haptics.success()

        model.permissionsDue = false
    }
}

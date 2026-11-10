package ai.equal.orb

import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Lang
import ai.equal.orb.model.Language
import ai.equal.orb.model.Step
import ai.equal.orb.overlays.OverlayHost
import ai.equal.orb.overlays.ToastHost
import ai.equal.orb.screens.*
import ai.equal.orb.ui.*
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

/**
 * Stages the app: Home (where it opens), the green splash, and the onboarding flow on its white
 * page. Modal surfaces (consent, decline sheets) and the toast sit on top.
 */
@Composable
fun OrbApp(startStep: String?) {
    val scope = rememberCoroutineScope()
    val model = remember {
        AppModel(scope).apply {
            Step.entries.firstOrNull { it.name.equals(startStep, ignoreCase = true) }?.let {
                step = it; name = "Shubham"
                if (it != Step.Phone) phone = "9876543210"
                if (it == Step.Otp) startOtp()
                picks.addAll(listOf("task_door", "bank", "task_med"))
            }
        }
    }
    val view = LocalView.current
    SideEffect { Haptics.view = view }

    // Light status-bar icons on the green splash and over the dimmed Home, dark everywhere else.
    val dark = model.step == Step.Splash
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    BackHandler(enabled = model.overlay == null && model.step.onSky) {
        if (model.step == Step.Phone) model.closeFlow() else if (model.step.hasBack) model.back()
    }

    val slidePx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val onSky = model.step.onSky
    val showsHome = model.step == Step.Home
    Box(Modifier.fillMaxSize().background(Color.White)) {
        val skyAlpha by animateFloatAsState(if (onSky) 1f else 0f, tween(450), label = "sky")
        if (skyAlpha > 0f) Sky(Modifier.graphicsLayer { alpha = skyAlpha })

        // Home: slides in 24 dp from the right as it fades up.
        AnimatedVisibility(
            showsHome,
            enter = fadeIn(tween(320, easing = EQ.ease)) + slideInHorizontally(tween(320, easing = EQ.ease)) { slidePx },
            exit = fadeOut(tween(300)),
        ) { HomeScreen(model) }

        if (model.step != Step.Splash && model.step != Step.Home) FlowContainer(model)

        AnimatedVisibility(model.step == Step.Splash, enter = fadeIn(tween(350)),
            exit = fadeOut(tween(760, easing = EQ.push)) + scaleOut(tween(760, easing = EQ.push), targetScale = 1.06f)) {
            SplashScreen(model)
        }

        OrbDockLayer(model)
        PermissionPrompts(model)
        OverlayHost(model)
        ToastHost(model.toast)
    }
}

/**
 * The onboarding screens: a fixed top bar (back / close / skip) over the current screen, which
 * pushes in from the side. The orb isn't part of the screens: one shared orb glides between them.
 * Tapping anywhere that isn't a control puts the keyboard away.
 */
@Composable
private fun FlowContainer(model: AppModel) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopBar(model)
        Box(Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { model.contentOrigin = it.positionInRoot() }) {
            OrbStage(model)
            AnimatedContent(
                targetState = model.step,
                transitionSpec = {
                    // iOS-style push: the new screen slides in a full width; the old one drifts a third of the way and fades.
                    val dir = model.direction
                    if (model.arrivingFromSplash && targetState == Step.Phone) EnterTransition.None togetherWith ExitTransition.None
                    else slideInHorizontally(tween(560, easing = EQ.push)) { it * dir } togetherWith
                        (slideOutHorizontally(tween(504, easing = EQ.push)) { -(it * 0.32f * dir).toInt() } + fadeOut(tween(504, easing = EQ.push)))
                },
                modifier = Modifier.fillMaxSize(),
                label = "push",
            ) { st ->
                FlowScreen(model, st) {
                    when (st) {
                        Step.Phone -> PhoneScreen(model)
                        Step.Otp -> OtpScreen(model)
                        Step.Name -> NameScreen(model)
                        Step.Cases -> PickThreeScreen(model)
                        Step.Assistant -> AssistantScreen(model)
                        Step.Access -> AccessScreen(model)
                        Step.Welcome -> WelcomeScreen(model)
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Close (first screen) or Back on the left,
 * and — once the orb has been nudged — a language menu in the middle.
 */
@Composable
private fun TopBar(model: AppModel) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .riseIn(model.arrivingFromSplash && model.step == Step.Phone, delayMs = 350, distance = (-8).dp, durationMs = 600),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            when {
                model.step == Step.Phone -> CloseButton { model.closeFlow() }
                model.step.hasBack -> BackButton { model.back() }
                else -> Spacer(Modifier.size(48.dp))
            }
        }
        AnimatedVisibility(
            Language.menuUnlocked && model.step != Step.Welcome,
            Modifier.align(Alignment.Center).padding(top = 10.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.6f), exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) { LanguageMenu() }
    }
}

/** A glass pill with the orb's current language, opening a menu with a tick next to it. */
@Composable
private fun LanguageMenu() {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(36.dp).pressable(scale = 0.96f) { open = true }
                .glass(RoundedCornerShape(50), tint = 0.42f, shadow = false).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Txt("🌐", dm(13f))
            Txt(Language.current.label, dm(15f, FontWeight.SemiBold, EQ.ink.copy(alpha = 0.8f)))
            Txt("▾", dm(12f, FontWeight.Bold, EQ.ink.copy(alpha = 0.5f)))
        }
        if (open) Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, 120), onDismissRequest = { open = false },
            properties = PopupProperties(focusable = true)) {
            Column(
                Modifier.width(200.dp).shadow(16.dp, RoundedCornerShape(14.dp)).background(Color.White, RoundedCornerShape(14.dp))
                    .padding(vertical = 6.dp)
            ) {
                Lang.entries.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().clickable { Haptics.select(); Language.current = l; open = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(24.dp)) { if (l == Language.current) Txt("✓", dm(15f, FontWeight.SemiBold, EQ.ink)) }
                        Txt(l.label, dm(16f, color = EQ.ink))
                    }
                }
            }
        }
    }
}

/**
 * The orb between Welcome and Home: drawn above both, flying into Home's Equal button and turning
 * into it on the way — the orb fades out as the button's artwork fades in.
 */
@Composable
private fun OrbDockLayer(model: AppModel) {
    val dock = model.orbDock ?: return
    val density = LocalDensity.current
    val cx = remember { Animatable(dock.frame.center.x) }
    val cy = remember { Animatable(dock.frame.center.y) }
    val w = remember { Animatable(dock.frame.width) }
    val morph = remember { Animatable(dock.morph) }
    LaunchedEffect(dock) {
        // Critically damped, so it settles exactly on the button without overshooting.
        val flight = spring<Float>(dampingRatio = 1f, stiffness = 60f)
        launch { cx.animateTo(dock.frame.center.x, flight) }
        launch { cy.animateTo(dock.frame.center.y, flight) }
        launch { w.animateTo(dock.frame.width, flight) }
        launch { morph.animateTo(dock.morph, flight) }
    }
    val basePx = with(density) { OrbFlight.base.toPx() }
    Box(Modifier.fillMaxSize()) {
        OrbBody(false, true, Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).graphicsLayer {
            translationX = cx.value - size.width / 2; translationY = cy.value - size.height / 2
            val s = w.value / basePx; scaleX = s; scaleY = s
            alpha = 1 - morph.value
        })
        val artPx = 56f * density.density
        Box(
            Modifier.graphicsLayer {
                translationX = cx.value - artPx / 2; translationY = cy.value - artPx / 2
                val s = w.value / artPx; scaleX = s; scaleY = s
                alpha = morph.value
            }
        ) { EqualArt(56.dp) }
    }
}

package ai.equal.orb.screens

import ai.equal.orb.R
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Overlay
import ai.equal.orb.model.Permission
import ai.equal.orb.model.art
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.Wordmark
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.glass
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home, matched to the shipping Equal AI app (`MainTabView` + `ConversationView`): a tab bar with
 * Chats and You, the floating Equal button in its middle, and the Chats screen — logo header,
 * dashboard cards, search bar, then the chats. The app opens here; long-press the logo to replay
 * the last onboarding.
 */
@Composable
fun HomeScreen(model: AppModel) {
    var tab by remember { mutableIntStateOf(0) }
    val list = rememberLazyListState()
    val context = LocalContext.current

    // The cards follow what Android has actually granted, including changes made in Settings.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        model.refreshPermissions(context)
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) model.refreshPermissions(context)
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize().background(DS.listBackground)) {
        // The lists run under the floating nav.
        if (tab == 0) ChatsScreen(model, list) else ProfileScreen(model)
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                // The button's artwork has a margin round its green disc (64 of 80): the gaps are optical.
                .padding(start = 46.dp, end = 39.dp).padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TabBar(model, tab, Modifier.weight(1f)) { if (it != tab) { tab = it; Haptics.select() } }
            EqualButton(model, Modifier)
        }
    }
}

/** The Equal button's artwork size (its green disc is 0.8 of it), and the room the floating nav takes over the lists. */
private val BUTTON_SIZE = 72.dp
val NAV_CLEARANCE = 100.dp

/**
 * The Equal button's artwork (`Avatar.assistant` from the production DesignSystem package),
 * shared with the orb's flight from Welcome so the hand-over is pixel-identical.
 */
@Composable
fun EqualArt(size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        // The shadow follows the artwork's green disc (64 of its 80 pt).
        Box(
            Modifier.size(size * 0.8f).offset(y = 2.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
        )
        Image(painterResource(R.drawable.avatar_assistant), null, Modifier.size(size), contentScale = ContentScale.Fit)
    }
}

/**
 * `GlobalMYAButton`: the assistant avatar, centred on the tab bar row. Hidden while the orb from
 * Welcome flies in and becomes it; the swap is invisible.
 */
@Composable
private fun EqualButton(model: AppModel, modifier: Modifier) {
    val docking = model.orbDock != null
    var bounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(docking) { if (docking) bounds?.let(model::dockTarget) }
    Box(
        modifier
            .onGloballyPositioned { c ->
                val r = c.boundsInRoot()
                if (r == bounds) return@onGloballyPositioned
                bounds = r
                if (model.orbDock != null) model.dockTarget(r)
            }
            .graphicsLayer { alpha = if (docking) 0f else 1f }
            .pressable(scale = 0.94f) { Haptics.step(); model.showToast("Global MYA — not in this prototype") }
    ) { EqualArt(BUTTON_SIZE) }
}

/**
 * The Android app's bottom nav: a floating white pill with Chats (a mint highlight when it's the
 * tab, and its unread badge) and You (your avatar). The Equal button floats beside it.
 */
@Composable
private fun TabBar(model: AppModel, tab: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.height(64.dp)
            .shadow(18.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.14f))
            .background(Color.White, shape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem("Chats", tab == 0, badge = 5, Modifier.weight(1f), { onSelect(0) }) { tint ->
            ChatGlyph(tint, filled = true)
        }
        TabItem("You", tab == 1, badge = 0, Modifier.weight(1f), { onSelect(1) }) { _ ->
            Image(
                painterResource(model.gender?.avatar ?: R.drawable.avatar_male), null,
                Modifier.size(26.dp).clip(CircleShape).background(hex(0xFFE9C8), CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun TabItem(label: String, on: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit, icon: @Composable (Color) -> Unit) {
    val tint = if (on) DS.primary else hex(0x6B6B6B)
    val fill by androidx.compose.animation.animateColorAsState(if (on) hex(0xEAF7EE) else Color.Transparent, tween(200), label = "tab")
    Column(
        modifier.fillMaxHeight().background(fill, RoundedCornerShape(50))
            .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Box(Modifier.size(28.dp, 26.dp), contentAlignment = Alignment.Center) { icon(tint) }
            if (badge > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp).defaultMinSize(18.dp, 18.dp)
                    .border(1.5.dp, Color.White, RoundedCornerShape(50))
                    .background(hex(0xE53935), RoundedCornerShape(50)).padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) { Txt("$badge", dm(11f, FontWeight.SemiBold, Color.White)) }
        }
        Txt(label, dm(13f, if (on) FontWeight.SemiBold else FontWeight.Medium, tint))
    }
}

/** `message` / `message.fill`: a speech bubble with its tail at the bottom left. */
@Composable
private fun ChatGlyph(color: Color, filled: Boolean) {
    Canvas(Modifier.size(26.dp, 24.dp)) {
        val d = density
        val body = Path().apply {
            addOval(Rect(Offset(2 * d, 2 * d), Size(22 * d, 17 * d)))
        }
        val tail = Path().apply { moveTo(5 * d, 14 * d); lineTo(3 * d, 22 * d); lineTo(11 * d, 17.5f * d); close() }
        if (filled) {
            drawPath(body, color); drawPath(tail, color)
        } else {
            drawPath(body, color, style = Stroke(1.8f * d))
            drawPath(tail, color)
            drawPath(Path().apply { addOval(Rect(Offset(3 * d, 3 * d), Size(20 * d, 15 * d))) }, Color.White)
        }
    }
}

// MARK: - Design tokens (DesignSystem package)

object DS {
    val primary = hex(0x00B140)
    val onBackground = Color.Black
    val listBackground = hex(0xF7F7F7)
    val pure = Color.White
    val name = hex(0x313131)
    val meta = hex(0x636363)
    val separator = Color.Gray.copy(alpha = 0.2f)
    val gold = hex(0xB17300)
}

@Composable
private fun Icon(v: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) =
    Image(rememberVectorPainter(v), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))

/** One line, cut with an ellipsis. */
@Composable
private fun Line(text: String, style: TextStyle, modifier: Modifier = Modifier) =
    BasicText(text, modifier, style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)

// MARK: - Chats

/** `ConversationView`: white logo header over the grey list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsScreen(model: AppModel, list: LazyListState) {
    val rows = ChatRow.samples
    val cards = dashboardCards(model)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(DS.listBackground)) {
        Header(model)
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(top = 10.dp, bottom = NAV_CLEARANCE + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            item("cards") {
                DashboardCarousel(cards, model.missingPermissions) { card ->
                    Haptics.step()
                    if (card.isPermission) scope.launch { model.present(Overlay.Decline(model.missingPermissions)) }
                    else model.showToast("Not in this prototype")
                }
            }
            stickyHeader("search") {
                SearchRow(Modifier.fillMaxWidth().background(DS.listBackground).padding(top = 12.dp, bottom = 4.dp))
            }
            itemsIndexed(rows, key = { _, r -> r.id }) { i, row ->
                Column(Modifier.animateItem()) {
                    if (i > 0) Box(
                        Modifier.padding(start = ChatRowSpec.separatorInset, end = ChatRowSpec.rowPadding)
                            .fillMaxWidth().height(0.5.dp).background(DS.separator)
                    )
                    ChatRowView(row, Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

/** What the carousel leads with: whatever permission is missing, then the trial and the assistant. */
private fun dashboardCards(model: AppModel): List<DashboardCard> {
    val tail = listOf(DashboardCard.SoftTrial, DashboardCard.MeetAssistant)
    val lead = when (model.missingPermissions) {
        listOf(Permission.Notifications) -> listOf(DashboardCard.Notifications)
        listOf(Permission.Contacts) -> listOf(DashboardCard.Contacts)
        emptyList<Permission>() -> emptyList()
        else -> listOf(DashboardCard.Permissions)
    }
    return lead + tail
}

/** Logo (long-press replays the last onboarding) and the trial pill, white, over the list. */
@Composable
private fun Header(model: AppModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(3.dp, RectangleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(DS.pure)
            .statusBarsPadding()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(130.dp, 28.dp).pointerInput(Unit) {
                detectTapGestures(onLongPress = { Haptics.step(); model.restart() })
            },
            contentAlignment = Alignment.CenterStart,
        ) { Wordmark(27.dp) }
        Spacer(Modifier.weight(1f))
        TrialDaysPill("7 Days Left")
    }
}

/** `SearchBarLauncherView` + `ConversationMoreMenu`. */
@Composable
private fun SearchRow(modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val muted = Color.Black.copy(alpha = 0.4f)
        Row(
            Modifier.weight(1f).height(52.dp).background(Color.White, RoundedCornerShape(50))
                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(50)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Search, muted, 20.dp)
            RotatingPlaceholder("Search for", listOf("names", "numbers", "categories", "keywords"), muted)
        }
        Box {
            Box(
                Modifier.size(42.dp).pressable(scale = 0.94f) { Haptics.select(); menu = true }
                    .glass(CircleShape, tint = 0.7f, stroke = Color.White.copy(alpha = 0.2f), shadow = false),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.MoreVert, DS.onBackground.copy(alpha = 0.8f), 22.dp, Modifier.rotate(90f)) }
            if (menu) Popup(alignment = Alignment.TopEnd, offset = androidx.compose.ui.unit.IntOffset(0, 140),
                onDismissRequest = { menu = false }, properties = PopupProperties(focusable = true)) {
                Column(
                    Modifier.width(200.dp).shadow(16.dp, RoundedCornerShape(14.dp)).background(Color.White, RoundedCornerShape(14.dp))
                        .padding(vertical = 6.dp)
                ) {
                    listOf("Select Chats", "Read All").forEach { label ->
                        Txt(
                            label, dm(16f, color = DS.onBackground),
                            Modifier.fillMaxWidth().clickable { menu = false }.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** “Search for names / numbers / …”, the word spinning up every 2 s. */
@Composable
private fun RotatingPlaceholder(prefix: String, words: List<String>, color: Color) {
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2000); i = (i + 1) % words.size } }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Line(prefix, dm(17f, color = color))
        AnimatedContent(
            i,
            transitionSpec = {
                (slideInVertically(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                    (slideOutVertically(tween(350)) { -it } + fadeOut(tween(350)))
            },
            label = "placeholder",
        ) { Line(words[it], dm(17f, color = color)) }
    }
}

// MARK: Trial pill

@Composable
private fun TrialDaysPill(text: String) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(34.dp)
            .shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(Brush.horizontalGradient(listOf(hex(0xFFFDF3), hex(0xF6F0E6))), shape)
            .border(1.dp, DS.gold, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { CrownGlyph(13.dp) }
        Line(text, dm(14f, FontWeight.Medium, DS.gold, tracking = -0.42f))
    }
}

/** `crown.fill`, in a gold gradient. */
@Composable
private fun CrownGlyph(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val brush = Brush.verticalGradient(listOf(hex(0xF5C342), DS.gold))
        drawPath(Path().apply {
            moveTo(0.04f * w, 0.24f * h); lineTo(0.3f * w, 0.5f * h); lineTo(0.5f * w, 0.1f * h)
            lineTo(0.7f * w, 0.5f * h); lineTo(0.96f * w, 0.24f * h); lineTo(0.86f * w, 0.76f * h); lineTo(0.14f * w, 0.76f * h); close()
        }, brush)
        drawRoundRect(brush, Offset(0.14f * w, 0.82f * h), Size(0.72f * w, 0.14f * h), CornerRadius(0.05f * w))
    }
}

// MARK: Dashboard cards

private enum class DashboardCard(val title: String, val description: String, val cta: String) {
    Permissions("Enable Permissions", "Allow notifications and contacts for real-time call recognition", "Open settings"),
    Notifications("Notification access", "Allow notifications access to avoid missing updates", "Open settings"),
    Contacts("Contact access", "Without contact access, assistant can't manage your calls", "Open settings"),
    MeetAssistant("Meet Your Assistant", "Get to know your assistant better.", "Call me"),
    SoftTrial("Gold, on us", "Everything is unlocked. Nothing to pay yet.", "Subscribe");

    val ctaIcon: ImageVector?
        get() = when (this) {
            MeetAssistant -> Icons.Filled.Phone
            SoftTrial -> null
            else -> Icons.Filled.Settings
        }

    val isPermission get() = this == Permissions || this == Notifications || this == Contacts

    val gradient: List<Color>
        get() = when (this) {
            MeetAssistant -> listOf(hex(0xEAF7EC), hex(0xD8F0DD))
            SoftTrial -> listOf(hex(0xFFFDF3), hex(0xF6F0E6))
            else -> listOf(hex(0xF8E2CC), hex(0xFDD2B2))
        }

    val stroke: Color
        get() = when (this) {
            MeetAssistant -> DS.primary
            SoftTrial -> DS.gold
            else -> hex(0xE06500)
        }

    val accent: Color get() = if (this == SoftTrial) DS.gold.copy(alpha = 0.28f) else stroke.copy(alpha = 0.3f)

    val radius: Dp get() = if (this == SoftTrial) 16.dp else 20.dp
}

/** `CardPagerView`: one card a page with a 3 % peek, auto-advancing every 4 s, dots below. */
@Composable
private fun DashboardCarousel(cards: List<DashboardCard>, missing: List<Permission>, onTap: (DashboardCard) -> Unit) {
    val pager = rememberPagerState { cards.size }
    LaunchedEffect(pager.currentPage, cards.size) {
        if (cards.size <= 1) return@LaunchedEffect
        delay(4000)
        pager.animateScrollToPage((pager.currentPage + 1) % cards.size, animationSpec = tween(350))
    }
    Column(
        Modifier.fillMaxWidth().height(200.dp).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val single = cards.size == 1
            val width = maxWidth - 32.dp - (if (single) 0.dp else maxWidth * 0.03f + 24.dp)
            HorizontalPager(
                pager, Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSize = PageSize.Fixed(width),
                pageSpacing = 24.dp,
                key = { cards[it] },
            ) { i -> DashboardCardView(cards[i], missing, Modifier.fillMaxSize()) { onTap(cards[i]) } }
        }
        if (cards.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            cards.indices.forEach { i ->
                val on = i == pager.currentPage
                val d by animateDpAsState(if (on) 8.dp else 6.dp, tween(200), label = "dot")
                Box(Modifier.size(d).background(if (on) DS.primary.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.3f), CircleShape))
            }
        }
    }
}

/** `DashboardCardView`. Only its pill button acts. */
@Composable
private fun DashboardCardView(card: DashboardCard, missing: List<Permission>, modifier: Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(card.radius)
    Row(
        modifier
            .heightIn(min = 160.dp)
            .clip(shape)
            .background(Brush.linearGradient(card.gradient), shape)
            .drawBehind {
                val d = density
                if (card == DashboardCard.SoftTrial) {
                    // A soft gold glow out of the bottom-right corner.
                    val c = Offset(size.width - 59 * d + 30 * d, size.height - 59 * d + 45 * d)
                    drawCircle(Brush.radialGradient(listOf(DS.gold.copy(alpha = 0.45f), Color.Transparent), c, 120 * d), 120 * d, c)
                }
                drawCircle(card.accent, 70 * d, Offset(size.width - 70 * d + 30 * d, 70 * d - 40 * d))
            }
            .then(
                if (card == DashboardCard.SoftTrial) Modifier.border(1.dp, card.stroke, shape)
                else Modifier.border(2.dp, Brush.linearGradient(listOf(card.stroke, Color.White.copy(alpha = 0.8f), card.stroke.copy(alpha = 0.8f))), shape)
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Line(card.title, dm(17f, FontWeight.Bold, DS.onBackground), Modifier.padding(top = 4.dp))
            BasicText(
                card.description, style = dm(13f, color = DS.onBackground.copy(alpha = 0.7f), lineHeight = 16f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.pressable(scale = 0.96f) { onTap() }.background(card.stroke, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                card.ctaIcon?.let { Icon(it, Color.White, 14.dp) }
                Line(card.cta, dm(12f, FontWeight.SemiBold, Color.White))
            }
        }
        Spacer(Modifier.width(10.dp))
        CardArt(card, missing)
    }
}

@Composable
private fun CardArt(card: DashboardCard, missing: List<Permission>) {
    when (card) {
        // Production `HomeCards.softTrialCrown`, sparkles included.
        DashboardCard.SoftTrial -> Image(painterResource(R.drawable.home_card_crown), null, Modifier.size(110.dp))
        // Production `HomeCards.assistant`; its art has wide margins, so it's drawn larger without taking room from the text.
        DashboardCard.MeetAssistant -> Image(
            painterResource(R.drawable.home_card_assistant), null,
            Modifier.size(100.dp).graphicsLayer { scaleX = 1.25f; scaleY = 1.25f },
        )
        else -> Image(painterResource(missing.art), null, Modifier.size(100.dp))
    }
}

// MARK: Chat rows

/** A chat in the list. */
private class ChatRow(
    val id: String,
    val emoji: String?,   // null = the assistant's avatar
    val name: String,
    val verified: Boolean = false,
    val time: String,
    val message: String,
    val unread: Int = 0,
    val chips: List<String>,
) {
    companion object {
        val samples: List<ChatRow>
            get() = listOf(
                ChatRow(
                    "equal", null, "Equal AI", verified = true, time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                    message = "Saved. I’ll follow it from now on.", unread = 1, chips = listOf("Add instruction", "Talk to assistant"),
                ),
                ChatRow(
                    "rider", "🛵", "Delivery rider", time = "Example",
                    message = "Rider left your order at the door.", chips = listOf("Call back", "Reply on WhatsApp"),
                ),
            )
    }
}

private object ChatRowSpec {
    val rowPadding = 12.dp
    val avatarSize = 42.dp
    val separatorInset = rowPadding + avatarSize
}

/** `ConversationItemView`. */
@Composable
private fun ChatRowView(row: ChatRow, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = ChatRowSpec.rowPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.padding(horizontal = ChatRowSpec.rowPadding),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChatAvatar(row)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Line(row.name, dm(17f, FontWeight.SemiBold, DS.name), Modifier.weight(1f, fill = false))
                    if (row.verified) Icon(Icons.Filled.CheckCircle, DS.primary, 18.dp, Modifier.padding(top = 1.dp))
                    Spacer(Modifier.weight(1f))
                    Txt(row.time, dm(13f, if (row.unread > 0) FontWeight.SemiBold else FontWeight.Normal, if (row.unread > 0) DS.primary else DS.meta), maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Line(row.message, dm(15f, color = DS.meta), Modifier.weight(1f))
                    if (row.unread > 0) Box(
                        Modifier.padding(start = 8.dp).defaultMinSize(20.dp, 20.dp).background(DS.primary, RoundedCornerShape(50)).padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) { Txt("${row.unread}", dm(12f, FontWeight.SemiBold, Color.White)) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = ChatRowSpec.separatorInset + 12.dp, end = ChatRowSpec.rowPadding, top = 3.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) { row.chips.forEach { ActionChip(it, highlighted = row.unread > 0) } }
    }
}

@Composable
private fun ChatAvatar(row: ChatRow) {
    val size = ChatRowSpec.avatarSize
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        if (row.emoji == null) {
            // The artwork's green disc is 64 of its 80 pt; enlarge it so the disc alone fills the circle.
            Image(
                painterResource(R.drawable.avatar_assistant), "Equal AI",
                Modifier.wrapContentSize(unbounded = true).size(size * 80f / 64f),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)))
            Txt(row.emoji, dm(20f))
        }
    }
}

/** `ActionChipView`. */
@Composable
private fun ActionChip(label: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .shadow(1.5.dp, shape, ambientColor = if (highlighted) hex(0xB1E1BD, 0.45f) else Color.Black.copy(alpha = 0.08f),
                spotColor = if (highlighted) hex(0xB1E1BD, 0.45f) else Color.Black.copy(alpha = 0.08f))
            .background(Brush.verticalGradient(if (highlighted) listOf(Color.White, hex(0xF3FBDF)) else listOf(Color.White, hex(0xEEEEEE))), shape)
            .border(
                1.5.dp,
                Brush.verticalGradient(if (highlighted) listOf(hex(0xB1E1BD), hex(0xC9E9D1)) else listOf(hex(0xE3E3E3), hex(0xEFEFEF))),
                shape,
            )
            .pressable(scale = 0.96f) { Haptics.select() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Line(label, dm(15f, color = DS.onBackground))
        Icon(Icons.Filled.KeyboardArrowRight, DS.onBackground.copy(alpha = 0.8f), 16.dp)
    }
}

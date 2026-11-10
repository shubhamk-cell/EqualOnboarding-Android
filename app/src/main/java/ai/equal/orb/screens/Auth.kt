package ai.equal.orb.screens

import androidx.compose.material.icons.filled.Edit
import ai.equal.orb.model.AppModel
import ai.equal.orb.model.Catalog
import ai.equal.orb.model.Gender
import ai.equal.orb.model.Step
import ai.equal.orb.ui.EQ
import ai.equal.orb.ui.GenderMark
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbWithBubble
import ai.equal.orb.ui.PrimaryButton
import ai.equal.orb.ui.Talk
import ai.equal.orb.ui.Txt
import ai.equal.orb.ui.afterOrb
import ai.equal.orb.ui.dm
import ai.equal.orb.ui.dmFamily
import ai.equal.orb.ui.glass
import ai.equal.orb.ui.hex
import ai.equal.orb.ui.pressable
import ai.equal.orb.ui.rememberTalk
import ai.equal.orb.ui.riseIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Standard layout of a question screen: content centred in the space between the top bar and
 * the footer, footer pinned to the bottom.
 */
@Composable
fun QuestionLayout(arriving: Boolean = false, main: @Composable BoxScope.() -> Unit, footer: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) { main() }
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 8.dp).riseIn(arriving, 520, 40.dp, 750)) { footer() }
    }
}

/** Runs [action] each time the orb finishes a line (its typing falls from true to false). */
private suspend fun Talk.onLineShown(action: () -> Unit) {
    var was = typing
    snapshotFlow { typing }.collect { t ->
        if (was && !t) action()
        was = t
    }
}

/**
 * Fades and rises into place when [on] turns true, [index] × 0.1 s after the others (cards and
 * links arriving one after another).
 */
fun Modifier.revealIn(on: Boolean, index: Int): Modifier = composed {
    val p = remember { Animatable(if (on) 1f else 0f) }
    LaunchedEffect(on) {
        delay(index * 100L)
        p.animateTo(if (on) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 158f))
    }
    graphicsLayer { alpha = p.value.coerceIn(0f, 1f); translationY = 18 * density * (1 - p.value) }
}

// MARK: - Number

@Composable
fun PhoneScreen(model: AppModel) {
    val talk = rememberTalk()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val arriving = model.arrivingFromSplash
    val title = Catalog.title(Catalog.Ask.Phone)

    LaunchedEffect(Unit) {
        talk.intro(title, if (arriving) 750 else 420)
        // The keyboard comes up once the orb has finished asking, not while it's typing.
        var asked = false
        talk.onLineShown {
            if (asked) return@onLineShown
            asked = true
            launch { delay(400); runCatching { focus.requestFocus() } }
        }
    }
    fun send() {
        if (model.phone.length != 10) return
        focusManager.clearFocus()
        model.go(Step.Otp)
    }

    QuestionLayout(arriving, main = {
        // No rise-in here: the orb already flies in from the splash, and offsetting its slot
        // would make the flight land low and then jump up when the slot took over.
        OrbWithBubble(talk, title)
    }) {
        Column(Modifier.afterOrb(talk), horizontalAlignment = Alignment.CenterHorizontally) {
            InputPill(filled = model.phone.isNotEmpty(), canSend = model.phone.length == 10, onSend = ::send, sendLabel = "Send OTP") {
                Txt("🇮🇳 +91", dm(17f, FontWeight.Medium, EQ.ink))
                Box(Modifier.padding(horizontal = 12.dp).width(1.dp).height(26.dp).background(EQ.ink.copy(alpha = 0.15f)))
                Field(
                    value = model.phone, placeholder = "Mobile number", focus = focus,
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    onDone = ::send,
                ) { v -> model.phone = tenDigits(v, model.phone) }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.pressable { Haptics.select(); model.whatsapp = !model.whatsapp },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(18.dp)
                        .background(if (model.whatsapp) EQ.green else Color.Transparent, RoundedCornerShape(5.dp))
                        .border(1.5.dp, if (model.whatsapp) EQ.green else EQ.ink.copy(alpha = 0.35f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (model.whatsapp) Image(rememberVectorPainter(Icons.Filled.Check), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Color.White))
                }
                Txt("Send me updates on WhatsApp", dm(12f, color = EQ.ink.copy(alpha = 0.7f)))
            }
            Spacer(Modifier.height(10.dp))
            val link = SpanStyle(color = EQ.greenDeep, fontFamily = dmFamily(12f, FontWeight.Medium), fontWeight = FontWeight.Medium)
            Txt(
                buildAnnotatedString {
                    append("You agree to our ")
                    withStyle(link) { append("Terms") }
                    append(" & ")
                    withStyle(link) { append("Privacy Policy") }
                },
                dm(12f, color = EQ.ink.copy(alpha = 0.62f), align = TextAlign.Center),
            )
        }
    }
}

/**
 * Digits only, at most 10. Autofill and paste bring the whole number, often with its country
 * code (“+91 98765 43210”), so when several digits land at once keep the last ten; typing past
 * ten just stops.
 */
private fun tenDigits(text: String, old: String): String {
    val digits = text.filter(Char::isDigit)
    if (digits.length <= 10) return digits
    val pasted = digits.length - old.count(Char::isDigit) > 1
    return if (pasted) digits.takeLast(10) else digits.take(10)
}

// MARK: - Code

/** The code fills itself in from the pretend SMS ([AppModel.startOtp]); Confirm works once it's in. */
@Composable
fun OtpScreen(model: AppModel) {
    val talk = rememberTalk()
    val title = Catalog.title(Catalog.Ask.Otp)
    val digits = model.otpDigits
    val verifying = model.otpVerifying
    val length = 6
    val long = true
    val spaced = model.phone.let { if (it.length == 10) "${it.take(5)} ${it.takeLast(5)}" else it }

    LaunchedEffect(Unit) {
        talk.intro(title)
        // Belt and braces: if the code never started filling (e.g. opened another way), start it.
        if (model.otpDigits.isEmpty() && !model.otpVerifying) model.startOtp()
    }

    QuestionLayout(main = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrbWithBubble(talk, title)
            Spacer(Modifier.height(32.dp))
            Column(Modifier.afterOrb(talk), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (long) 8.dp else 12.dp)) {
                    for (i in 0 until length) {
                        val filled = i < digits.size
                        OtpCell(if (filled) digits[i] else "", filled, long, Modifier.weight(1f))
                    }
                }
                Txt(
                    if (verifying) "Verifying…" else "Reading the SMS automatically…",
                    dm(14f, color = EQ.ink.copy(alpha = 0.62f)), Modifier.padding(top = 14.dp),
                )
                Box(Modifier.padding(top = 6.dp)) {
                    // The number, with a small pencil to go back and change it.
                    Row(
                        Modifier.pressable { model.go(Step.Phone, -1) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt("+91 $spaced", dm(15f, FontWeight.SemiBold, EQ.ink))
                        Box(Modifier.size(26.dp).background(EQ.green.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                            Image(rememberVectorPainter(Icons.Filled.Edit), "Edit number", Modifier.size(14.dp), colorFilter = ColorFilter.tint(EQ.greenDeep))
                        }
                    }
                }
            }
        }
    }) {
        // Ready to tap once the code is in, in case verifying is ever slow.
        PrimaryButton(
            if (verifying) "Verifying…" else "Confirm", enabled = digits.size >= length, lookEnabled = verifying,
            modifier = Modifier.afterOrb(talk, index = 1),
        ) { model.confirmOtp() }
    }
}

@Composable
private fun OtpCell(text: String, filled: Boolean, long: Boolean, modifier: Modifier) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            pop.animateTo(1.14f, tween(130)); pop.animateTo(1f, tween(170))
        }
    }
    val stroke by animateColorAsState(if (filled) EQ.green else EQ.edge, tween(200), label = "otp")
    Box(
        modifier.height(if (long) 60.dp else 68.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .glass(RoundedCornerShape(if (long) 16.dp else 18.dp), tint = 0.62f, stroke = stroke),
        contentAlignment = Alignment.Center,
    ) { Txt(text, dm(if (long) 24f else 26f, FontWeight.SemiBold, EQ.ink)) }
}

// MARK: - Name

/**
 * The name, typed into the pill. Then, on the same page, it asks how to address them: the pill
 * drops away with the keyboard, the orb asks, and two gender cards rise in under it. Back (or
 * “Edit name”) steps back from the gender question to the name.
 */
@Composable
fun NameScreen(model: AppModel) {
    val talk = rememberTalk()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val title = Catalog.title(Catalog.Ask.Name)
    var text by remember { mutableStateOf(model.name) }
    var askedForName by remember { mutableStateOf(false) }
    /** The name is in, and the same page now asks how to address them. */
    var askingGender by remember { mutableStateOf(false) }
    var gendersShown by remember { mutableStateOf(false) }
    /** Back from the picks, with a gender already chosen. */
    var cameBack by remember { mutableStateOf(false) }
    val trimmed = text.trim()

    /**
     * Back to typing the name, on the same page and the same way in reverse: the cards drop and
     * fade as the name pill rises back, and the orb asks again once it's there.
     */
    fun editName() {
        gendersShown = false
        askingGender = false
        cameBack = false
        talk.full = title
        scope.launch { talk.think() }
        scope.launch { delay(350); runCatching { focus.requestFocus() } }
        scope.launch {
            delay(800)
            if (!askingGender) talk.show(title)
        }
    }

    fun submit() {
        if (trimmed.isEmpty()) return
        model.name = trimmed
        focusManager.clearFocus()
        // How to address them is asked on the same page.
        model.gender = null
        val ask = Catalog.genderAsk(trimmed)
        talk.full = ask
        // All on the same page: the keyboard goes down as the name pill drops and fades; the old
        // question goes back into the orb and the typing dots come out; the orb asks; and only
        // once its question is showing do the cards and the edit link rise in.
        askingGender = true
        scope.launch { talk.think() }
        scope.launch {
            delay(950)
            if (askingGender) talk.show(ask)
        }
    }

    fun pick(g: Gender) {
        Haptics.select()
        model.gender = g
        scope.launch {
            delay(350)
            if (model.step == Step.Name) model.next()
        }
    }

    DisposableEffect(Unit) {
        // Back on the gender question returns to the name, on this page.
        model.localBack = Step.Name to {
            if (!askingGender) false else { editName(); true }
        }
        onDispose { if (model.localBack?.first == Step.Name) model.localBack = null }
    }

    LaunchedEffect(Unit) {
        // Back from the picks: straight to the gender question, the earlier answer marked.
        if (model.gender != null && trimmed.isNotEmpty()) {
            askingGender = true
            askedForName = true
            cameBack = true
            talk.intro(Catalog.genderAsk(trimmed))
        } else talk.intro(title)
        // As on the number screen: the keyboard waits for the orb to finish asking.
        talk.onLineShown {
            if (askingGender) gendersShown = true
            if (askedForName) return@onLineShown
            askedForName = true
            launch { delay(400); runCatching { focus.requestFocus() } }
        }
    }

    // Once the name is in, the page stops making room for the keyboard: the orb and cards go
    // straight to where they'll rest, in one move, while the keyboard slides away underneath.
    val ignoreKeyboard by animateFloatAsState(if (askingGender) 1f else 0f, tween(300, easing = EQ.ease), label = "ime")
    val imeBottom = WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(LocalDensity.current)
    val px = LocalDensity.current.density

    Box(
        Modifier.fillMaxSize().layout { m, c ->
            val extra = (imeBottom * ignoreKeyboard).roundToInt()
            val p = m.measure(c.copy(minHeight = c.maxHeight + extra, maxHeight = c.maxHeight + extra))
            layout(c.maxWidth, c.maxHeight) { p.place(0, 0) }
        }
    ) {
        QuestionLayout(main = {
            // The gender cards sit right under the orb, the two centred together. Their room
            // opens the moment the name is in, so the orb glides up once, with the answer.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OrbWithBubble(talk, if (askingGender) Catalog.genderAsk(trimmed) else title)
                AnimatedVisibility(
                    askingGender,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300, easing = EQ.ease), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300, easing = EQ.ease), shrinkTowards = Alignment.Top),
                ) {
                    Genders(model, trimmed, gendersShown, Modifier.padding(top = 30.dp), onPick = ::pick) {
                        Haptics.select(); editName()
                    }
                }
            }
        }) {
            Column {
                // Drops away with the keyboard, quickly, before the cards arrive below the orb (and
                // rises back to edit the name).
                AnimatedVisibility(
                    !askingGender,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = EQ.ease)) { (24 * px).toInt() }
                        + expandVertically(tween(300, easing = EQ.ease), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(180, easing = FastOutLinearInEasing)) + slideOutVertically(tween(180)) { (110 * px).toInt() }
                        + shrinkVertically(tween(300, easing = EQ.ease), shrinkTowards = Alignment.Top),
                ) {
                    InputPill(filled = text.isNotEmpty(), canSend = trimmed.isNotEmpty(), onSend = ::submit, sendLabel = "Continue",
                        modifier = Modifier.afterOrb(talk)) {
                        Field(
                            value = text, placeholder = "Your name", focus = focus,
                            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                            onDone = ::submit,
                        ) { text = it }
                    }
                }
                // Back here with a choice already ticked: tapping it again isn't obvious, so the
                // way on is spelled out (another card still moves on by itself).
                AnimatedVisibility(askingGender && cameBack && model.gender != null, enter = fadeIn(), exit = fadeOut()) {
                    PrimaryButton(model.nextCta, modifier = Modifier.revealIn(gendersShown, 3)) { Haptics.step(); model.next() }
                }
            }
        }
    }
}

/** Two cards side by side, over a link back to the name. */
@Composable
private fun Genders(model: AppModel, name: String, shown: Boolean, modifier: Modifier, onPick: (Gender) -> Unit, onEdit: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Gender.entries.forEachIndexed { i, g ->
                GenderCard(g, model.gender == g, Modifier.weight(1f).revealIn(shown, i)) { onPick(g) }
            }
        }
        Row(
            Modifier.revealIn(shown, 2).pressable { onEdit() }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Txt("Not $name?", dm(14f, color = EQ.ink.copy(alpha = 0.6f)))
            Txt("Edit name", dm(14f, FontWeight.SemiBold, EQ.greenDeep))
        }
    }
}

/**
 * As in the design: a tinted card, the name of the choice on top, a 3D bust rising from the
 * bottom edge, and a faint ♂ / ♀ in the corner. The chosen card keeps a tick on its corner.
 */
@Composable
private fun GenderCard(g: Gender, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val sel by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 440f), label = "gender")
    Box(modifier.height(128.dp).pressable(scale = 0.96f) { onClick() }) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val i = 4.dp.toPx()
                    drawRoundRect(EQ.green.copy(alpha = 0.14f * sel.coerceIn(0f, 1f)), Offset(-i, -i), Size(size.width + 2 * i, size.height + 2 * i), CornerRadius(22.dp.toPx()))
                }
                .shadow(8.dp, shape, ambientColor = hex(0x1E2A6B, if (on) 0.12f else 0.07f), spotColor = hex(0x1E2A6B, if (on) 0.12f else 0.07f))
                .clip(shape)
                .background(hex(g.fill))
                .border(if (on) 2.dp else 1.dp, if (on) EQ.green else EQ.edge, shape),
        ) {
            GenderMark(g, Modifier.align(if (g == Gender.Male) Alignment.TopStart else Alignment.BottomStart))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0f)))))
            Column(Modifier.matchParentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(g.label, dm(17f, FontWeight.Medium, EQ.ink), Modifier.padding(top = 18.dp))
                Spacer(Modifier.weight(1f).height(6.dp))
                Image(
                    painterResource(g.avatar), null,
                    Modifier.height(76.dp).graphicsLayer {
                        val s = 1f + 0.06f * sel; scaleX = s; scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(22.dp)
                .graphicsLayer { val s = 0.4f + 0.6f * sel; scaleX = s; scaleY = s; alpha = sel.coerceIn(0f, 1f) }
                .background(EQ.green, CircleShape).border(2.dp, EQ.page, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Image(rememberVectorPainter(Icons.Filled.Check), null, Modifier.size(13.dp), colorFilter = ColorFilter.tint(Color.White)) }
    }
}

// MARK: - Shared input

/**
 * White 60 dp pill holding a field, with the screen's main action merged in as a round green
 * send button at its right end. Gains a green rim once something is typed; until then a faint
 * outline keeps it visible on the white page.
 */
@Composable
fun InputPill(
    filled: Boolean, canSend: Boolean, onSend: () -> Unit, sendLabel: String = "Send", modifier: Modifier = Modifier,
    field: @Composable RowScope.() -> Unit,
) {
    val rim by animateColorAsState(if (filled) EQ.green else EQ.edge, tween(200), label = "rim")
    val ring by animateColorAsState(if (filled) EQ.green.copy(alpha = 0.12f) else Color.Transparent, tween(200), label = "ring")
    val send by animateColorAsState(if (canSend) EQ.green else hex(0xBFE6CB), tween(200), label = "send")
    val pill = RoundedCornerShape(50)
    Row(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                val i = 3.dp.toPx()
                drawRoundRect(ring, Offset(-i, -i), Size(size.width + 2 * i, size.height + 2 * i), CornerRadius(size.height / 2 + i))
            }
            .shadow(10.dp, pill, ambientColor = hex(0x003214, 0.08f), spotColor = hex(0x003214, 0.12f))
            .background(Color.White, pill)
            .border(if (filled) 1.5.dp else 1.dp, rim, pill)
            .padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        field()
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(44.dp)
                .pressable(canSend, 0.92f) { onSend() }
                .then(if (canSend) Modifier.shadow(6.dp, CircleShape, spotColor = hex(0x008C32, 0.4f)) else Modifier)
                .background(send, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(rememberVectorPainter(Icons.Filled.ArrowForward), sendLabel, Modifier.size(22.dp), colorFilter = ColorFilter.tint(Color.White))
        }
    }
}

@Composable
private fun RowScope.Field(
    value: String, placeholder: String, focus: FocusRequester, keyboard: KeyboardOptions, onDone: () -> Unit, onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.weight(1f).focusRequester(focus),
        singleLine = true,
        textStyle = dm(18f, FontWeight.Medium, EQ.ink),
        cursorBrush = SolidColor(EQ.green),
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { onDone() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Txt(placeholder, dm(18f, FontWeight.Medium, EQ.ink.copy(alpha = 0.55f)))
                inner()
            }
        },
    )
}

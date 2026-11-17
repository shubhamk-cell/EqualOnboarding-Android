package ai.equal.orb.model

import ai.equal.orb.ui.BubbleStage
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbFlight
import ai.equal.orb.ui.OrbSpot
import ai.equal.orb.ui.SetupBlock
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Every stop on the onboarding, in order: a 6-digit code,
 * name and how to address you on one page, the languages callers speak (up to 3) and which one
 * to greet them in, pick exactly 3 calls that turn into playable samples in
 * place, choose the assistant's voice, then set up. Android has no tracking prompt and no Live
 * Voicemail step; [Access] is call forwarding, which Equal turns on by itself.
 */
enum class Step {
    Splash, Phone, Otp, Name, Languages, Cases, Assistant, Sim, Access, Welcome, Home;

    val hasBack get() = this != Phone && this != Welcome

    /**
     * Whether the top bar draws a back arrow. Only the sample calls do: that page is worth
     * returning to, and everywhere else the arrow invited a detour out of a question that has
     * to be answered anyway. The system back gesture still follows [hasBack] throughout.
     */
    val showsBack get() = this == Cases
    val onSky get() = this != Splash && this != Home
}

/**
 * Which shape of the onboarding is being run, chosen on the profile page.
 *
 * [Standard] is the flow as it stands: name, languages, picks, voice, then the SIM and call
 * forwarding at the end. [SimFirst] moves the SIM question up to right after the code — so the
 * permission popups land there too — and then, with the SIM already settled, sets call forwarding
 * up quietly in the background while the user is busy with the sample calls and the voice.
 */
enum class Variant { Standard, SimFirst }

/** The languages the orb can speak. Tapping the orb steps through them in this order. */
enum class Lang(val label: String) { En("English"), Hi("हिन्दी"), Ta("தமிழ்"), Bn("বাংলা") }

/** Which language the orb's bubbles are in. Shared by every bubble on every screen. */
object Language {
    var current by mutableStateOf(Lang.En)

    /** The orb was tapped or dragged: the next language, wrapping round to the first. */
    fun next() {
        current = Lang.entries[(current.ordinal + 1) % Lang.entries.size]
    }

    fun reset() { current = Lang.En }
}

/** How Equal addresses the user on calls (asked on the name screen). */
enum class Gender(val label: String, val honorific: String, val fill: Long, val mark: Long) {
    Male("Male", "sir", 0xEEF2FF, 0x8487FF),
    Female("Female", "ma’am", 0xFAF1FF, 0xFA95FF);

    /** The 3D bust on its card. */
    val avatar: Int get() = if (this == Male) ai.equal.orb.R.drawable.avatar_male else ai.equal.orb.R.drawable.avatar_female
}

/**
 * Equal's voices: two female, two male.
 * Aanya and Kabir have natural recordings; Meera and Arjun preview with the system's speech voice.
 */
enum class Voice(val vibe: String, val pitch: Float, val rate: Float) {
    Aanya("Warm & calm", 1.1f, 0.96f),
    Meera("Bright & cheerful", 1.25f, 1.04f),
    Kabir("Deep & steady", 0.72f, 0.9f),
    Arjun("Crisp & upbeat", 0.9f, 1.0f);

    val title get() = name
    val isFemale get() = this == Aanya || this == Meera
    val gender get() = if (isFemale) "Female" else "Male"
    val emoji get() = if (isFemale) "🌸" else "🌊"
    /** A rose-to-peach wash for the female voices, sky-to-teal for the male. */
    val tint: Long get() = if (isFemale) 0xFFE9EF else 0xE3EEFF
    val wash: List<Long> get() = if (isFemale) listOf(0xFFE3EC, 0xFFF1E4) else listOf(0xDCEBFF, 0xDDF7F1)
    val accent: Long get() = if (isFemale) 0xE0507A else 0x2F6FE0
}

/** The languages Equal speaks on calls; a voice is previewed in the one it greets callers in. */
enum class CallLanguage(val label: String, val initial: String, val speechCode: String) {
    English("English", "E", "en-IN"),
    Hindi("हिन्दी", "ह", "hi-IN"),
    Hinglish("Hinglish", "H", "en-IN"),
    Tamil("தமிழ்", "த", "ta-IN"),
    Telugu("తెలుగు", "తె", "te-IN"),
    Kannada("ಕನ್ನಡ", "ಕ", "kn-IN"),
    Bengali("বাংলা", "বা", "bn-IN"),
    Marathi("मराठी", "म", "mr-IN"),
    Malayalam("മലയാളം", "മ", "ml-IN");

    /** The English name, under the native one. */
    val english get() = name

    companion object {
        /** The languages offered, in order. */
        val options = listOf(English, Hindi, Tamil, Telugu, Kannada, Bengali, Marathi, Malayalam)
        /** How many can be picked at once. */
        const val MAX_PICKED = 3
    }
}

enum class Permission(val label: String, val reason: String) {
    Notifications("Notifications", "So I can tell you about the calls I take"),
    Contacts("Contacts", "So I know who’s calling and greet them by name"),
    /** Making and managing calls: what dialling the forwarding code needs. */
    Calls("Phone calls", "So I can turn on forwarding and answer for you"),
}

/** The 3D art for whatever is missing: a bell, a contacts book, or a gear for both. */
val List<Permission>.art: Int
    get() = when {
        size > 1 -> ai.equal.orb.R.drawable.perm_gear
        firstOrNull() == Permission.Notifications -> ai.equal.orb.R.drawable.perm_bell
        else -> ai.equal.orb.R.drawable.perm_contacts
    }

/** Modal surfaces drawn above the flow. */
sealed interface Overlay {
    /**
     * The flow is held: these permissions were refused and Equal can't go on without them. A full
     * screen rather than a sheet, with no way past it but Settings. [dismissible] only from Home,
     * where nothing is waiting on them.
     */
    data class Blocked(
        val missing: List<Permission>,
        val dismissible: Boolean = false,
        /** Shown for a demo on a phone that has already granted them: it stays put. */
        val demo: Boolean = false,
    ) : Overlay
}

/** Which batch of permissions the flow is waiting on. */
enum class PermissionGate { None, Notifications, CallsAndContacts }

/** The orb's last flight, from Welcome into Home's Equal button (root coordinates, px). */
data class OrbDock(val frame: Rect, val morph: Float = 0f)

/** The code the pretend SMS carries, and the only one that verifies. */
private const val THE_CODE = "482715"

/**
 * Call forwarding set up quietly behind the sample calls ([Variant.SimFirst] only), instead of on
 * a loading screen of its own. [Off] = not being tried, so nothing is shown and the ordinary
 * walkthrough follows.
 */
enum class BgSetup { Off, Working, Done, Failed }

/** How long the quiet attempt is given before it counts as failed. */
private const val BG_TIMEOUT_MS = 20_000L

class AppModel(val scope: CoroutineScope) {
    /** The app opens on Home; the onboarding is started from the profile tab. */
    var step by mutableStateOf(Step.Home)
    /** Which shape of the onboarding this run is. */
    var variant by mutableStateOf(Variant.Standard)
    var voice by mutableStateOf(Voice.Aanya)
    /** A voice has been picked on the assistant page (so coming back shows it marked). */
    var voiceChosen by mutableStateOf(false)
    /** +1 pushes forward (new screen from the right), −1 goes back. */
    var direction by mutableIntStateOf(1)
    /** True only for the first screen after the splash, which arrives with its own choreography. */
    var arrivingFromSplash by mutableStateOf(false)
    /** Where the splash's Equal mark was on screen when it left (root coordinates, px). */
    var splashMark: Offset? = null

    var phone by mutableStateOf("")
    /** The code screen's digits as the pretend SMS fills them, and whether it's verifying. */
    val otpDigits = mutableStateListOf<String>()
    var otpVerifying by mutableStateOf(false)
    /** The code typed by hand doesn't match: the cells turn red until the next keystroke. */
    var otpWrong by mutableStateOf(false)
    private var otpJob: Job? = null
    var name by mutableStateOf("")
    /** How Equal addresses the user on calls. */
    var gender by mutableStateOf<Gender?>(null)
    var whatsapp by mutableStateOf(true)
    val picks = mutableStateListOf<String>()
    /** The languages the user's callers speak, in the order picked (up to three). */
    val languages = mutableStateListOf<CallLanguage>()
    /** The one of [languages] Equal greets callers in (null until chosen). */
    var primaryLanguage by mutableStateOf<CallLanguage?>(null)
    /** The code is verified: the permission popups are due (they open over the name page). */
    /** The batch of permissions the flow is asking for right now. */
    var permissionGate by mutableStateOf(PermissionGate.None)
    /** Where the flow goes once that batch is granted. */
    var afterPermissions: (() -> Unit)? = null
    var fwdDone by mutableStateOf(false)
    /** The quiet attempt at call forwarding, started when the sample calls open (SIM-first only). */
    var bgSetup by mutableStateOf(BgSetup.Off)
    /** The “it's on” strip has been held long enough and has faded away. */
    var bgStripGone by mutableStateOf(false)
    /** Demo switch: the quiet attempt fails on purpose (long-press the strip, or a launch extra). */
    var bgDemoFail = false
    private var bgJob: Job? = null
    /**
     * The forwarding walkthrough is being reached because the quiet attempt didn't get there: the
     * orb introduces it as one last thing rather than as the next stage of setting up.
     */
    var fwdFallback by mutableStateOf(false)
    /** The SIM Equal takes calls on (0 or 1; null until one is picked). */
    var sim by mutableStateOf<Int?>(null)
    /** Notifications, contacts: granted? (null = not asked yet, which counts as off too.) */
    var perms by mutableStateOf(listOf<Boolean?>(null, null, null))

    /** The flow's one orb: where it sits. */
    var orbSpot by mutableStateOf<OrbSpot?>(null)
    /** The orb has settled into the current screen's slot, which now draws it (see `OrbSlot`). */
    var orbInSlot by mutableStateOf(false)
    private var settleJob: Job? = null
    /** After Welcome the orb flies down into Home's Equal button and becomes it. */
    var orbDock by mutableStateOf<OrbDock?>(null)
    private var dockLanding: Job? = null
    /** Top-left of the flow's screen area in root coordinates (to place the splash mark in it). */
    var contentOrigin = Offset.Zero

    var overlay by mutableStateOf<Overlay?>(null)
    var toast by mutableStateOf<String?>(null)
    private var toastJob: Job? = null
    private var pending: CompletableDeferred<String>? = null

    val displayName get() = name.ifBlank { "Keshav" }
    /** The name as Equal says it on calls: “Shubham sir”, “Shubham ma’am”, or just the name. */
    val addressName get() = gender?.let { "$displayName ${it.honorific}" } ?: displayName

    // MARK: Navigation

    /** A page change is under way: taps (Back, Continue) meanwhile are ignored. */
    private var navigating = false

    /**
     * A page with states of its own (name then gender, the picks then their sample calls)
     * steps back through them before Back leaves it. Returns true if it took the tap.
     */
    var localBack: Pair<Step, () -> Boolean>? = null

    /**
     * Moves to [to]. `haptic = false` when the caller marks the moment itself (a verified code).
     * On a flow page, its message first shrinks back into the orb, then the page changes.
     */
    fun go(to: Step, dir: Int = 1, haptic: Boolean = true) {
        if (navigating) return
        navigating = true
        if (step == Step.Splash || step == Step.Home) {
            goNow(to, dir, haptic)
            scope.launch { delay(600); navigating = false }
            return
        }
        if (haptic) Haptics.step()
        BubbleStage.away = true
        scope.launch {
            delay(170)
            goNow(to, dir, false)
            // Held until the old page has slid away, so its message can't reappear on the way out.
            delay(550)
            BubbleStage.away = false
            navigating = false
        }
    }

    private fun goNow(to: Step, dir: Int, haptic: Boolean) {
        if (haptic) Haptics.step()
        direction = dir
        arrivingFromSplash = false
        if (to != Step.Home) OrbFlight.departing()
        if (to == Step.Otp) startOtp() else otpJob?.cancel()
        // The stage takes the orb back for the flight to the next screen.
        settleJob?.cancel()
        orbInSlot = false
        step = to
    }

    /** The screens, in order — the SIM sits in a different place in each variant. */
    private val route: List<Step>
        get() = when (variant) {
            Variant.Standard ->
                listOf(Step.Phone, Step.Otp, Step.Name, Step.Languages, Step.Cases, Step.Assistant, Step.Sim, Step.Access, Step.Welcome, Step.Home)
            // The SIM right after the code (the permission popups land over it), and no forwarding
            // step of its own unless the quiet attempt doesn't get there.
            Variant.SimFirst ->
                listOf(Step.Phone, Step.Otp, Step.Sim, Step.Name, Step.Languages, Step.Cases, Step.Assistant, Step.Access, Step.Welcome, Step.Home)
        }

    /** The step after the current one. */
    fun next() {
        val i = route.indexOf(step)
        if (i < 0 || i + 1 >= route.size) return
        go(route[i + 1])
    }

    /**
     * The main button's label on a step that just moves on: says what comes next, so each tap
     * reads as the next stage of setting up rather than a bare “Continue”.
     */
    val nextCta: String
        get() {
            val i = route.indexOf(step)
            if (i < 0 || i + 1 >= route.size) return "Continue"
            return cta(route[i + 1])
        }

    private fun cta(s: Step) = when (s) {
        Step.Languages -> "Choose languages"
        // Names the next three screens. “Show me how”, straight after the greeting question, read
        // as a promise to play the greeting.
        Step.Cases -> "See Equal in action"
        Step.Assistant -> "Choose a voice"
        Step.Sim -> "Choose your SIM"
        Step.Name -> "Add your name"
        Step.Access -> "Activate on this SIM"
        Step.Welcome -> "Finish setup"
        Step.Home -> "Go to Home"
        else -> "Continue"
    }

    // MARK: Call forwarding in the background (SIM-first variant)

    /**
     * Starts setting up call forwarding while the user is picking sample calls, if the phone is in
     * a state where that could work at all ([block]). It is started once: coming back to the picks
     * screen doesn't restart it, and nothing waits on it.
     *
     * In the app this would dial the carrier's forwarding code; here it's a timer, because the
     * real thing puts the carrier's own response sheet over whatever is on screen.
     */
    fun startBackgroundSetup(block: SetupBlock) {
        if (variant != Variant.SimFirst || bgSetup != BgSetup.Off || bgJob != null || fwdDone) return
        if (block != SetupBlock.None) return
        bgSetup = BgSetup.Working
        bgJob = scope.launch {
            // Carrier rejected the code, the user dismissed the carrier's sheet, or it simply
            // never came back: all of them land here as one soft failure.
            val ok = withTimeoutOrNull(BG_TIMEOUT_MS) {
                delay((9_000..14_000).random().toLong())
                !bgDemoFail
            } ?: false
            bgJob = null
            if (ok) {
                fwdDone = true
                bgSetup = BgSetup.Done
                Haptics.land()
                // Held long enough to be read, then out of the way.
                delay(2000)
                bgStripGone = true
            } else {
                bgSetup = BgSetup.Failed
                Haptics.warning()
            }
        }
    }

    /** The demo's failure path: a long-press on the strip gives up on the quiet attempt. */
    fun failBackgroundSetup() {
        if (bgSetup != BgSetup.Working) return
        bgJob?.cancel(); bgJob = null
        bgSetup = BgSetup.Failed
        Haptics.warning()
    }

    /**
     * Leaving the voice step. Forwarding already on → straight to the welcome; still going → the
     * welcome holds for it; failed or never tried → the walkthrough, which is all this variant
     * ever risks losing.
     */
    fun finishVoiceStep() {
        when (bgSetup) {
            BgSetup.Done, BgSetup.Working -> go(Step.Welcome)
            BgSetup.Failed -> { fwdFallback = true; next() }
            BgSetup.Off -> next()
        }
    }

    /** The voice step's button, which says where that tap will actually land. */
    val voiceCta: String
        get() = cta(if (bgSetup == BgSetup.Done || bgSetup == BgSetup.Working) Step.Welcome else route.getOrNull(route.indexOf(Step.Assistant) + 1) ?: Step.Welcome)

    /** The quiet attempt didn't get there: the walkthrough, introduced as one last thing. */
    fun fallBackToForwarding() {
        fwdFallback = true
        go(Step.Access)
    }

    /** Back: first to the page's own earlier state, if it has one; otherwise the page before. */
    fun back() {
        if (navigating) return
        localBack?.let { (s, handle) -> if (s == step && handle()) { Haptics.select(); return } }
        val i = route.indexOf(step)
        if (i <= 0) return
        var prev = route[i - 1]
        // Name goes back past the code, which fills itself in.
        if (prev == Step.Otp) prev = Step.Phone
        go(prev, -1)
    }


    /** Closes the onboarding (the ✕ on its first screen). */
    fun closeFlow() {
        Haptics.step()
        settleJob?.cancel(); orbInSlot = false; orbSpot = null
        Language.reset()
        step = Step.Home
    }

    // MARK: Code

    /** The pretend SMS: the code fills in a digit at a time, verifies, and moves on to Name. */
    fun startOtp() {
        otpJob?.cancel()
        otpDigits.clear()
        otpVerifying = false
        otpWrong = false
        val code = THE_CODE.map { it.toString() }
        otpJob = scope.launch {
            // Give the SMS a moment to “arrive” (and the orb to ask) before the code fills in.
            delay(3000)
            for (d in code) {
                delay(380)
                if (step != Step.Otp) return@launch
                otpDigits.add(d)
                Haptics.select()
            }
            otpVerifying = true
            delay(700)
            confirmOtp()
        }
    }

    /**
     * The code typed by hand: the pretend SMS stops, and once six digits are in they're checked.
     * A wrong code turns the cells red; the next keystroke clears them.
     */
    fun typeOtp(text: String) {
        otpJob?.cancel()
        val all = text.filter(Char::isDigit)
        // Typing over a wrong code starts a fresh one, rather than running into the six-digit cap.
        val digits = if (otpWrong && all.length > otpDigits.size) all.takeLast(1) else all.take(6)
        otpWrong = false
        otpDigits.clear()
        digits.forEach { otpDigits.add(it.toString()) }
        if (digits.length < 6) return
        if (digits == THE_CODE) {
            otpVerifying = true
            Haptics.select()
            scope.launch { delay(650); confirmOtp() }
        } else {
            otpWrong = true
            Haptics.warning()
        }
    }

    /** Code verified (automatically, or Confirm tapped once it's in): on to Name. */
    fun confirmOtp() {
        if (step != Step.Otp || otpWrong) return
        otpJob?.cancel()
        Haptics.success()
        // Whatever the variant puts after the code: the name, or the SIM question.
        go(route.getOrNull(route.indexOf(Step.Otp) + 1) ?: Step.Name, haptic = false)
    }

    fun exitToHome() {
        if (step != Step.Welcome) Haptics.step()   // Welcome hands over to the orb's landing instead
        step = Step.Home
    }

    /** Starts the onboarding from the splash (the profile tab, or a long-press on the Home logo). */
    fun restart(shape: Variant = Variant.Standard) {
        variant = shape
        voice = Voice.Aanya; voiceChosen = false
        phone = ""; name = ""; gender = null; whatsapp = true; picks.clear()
        languages.clear(); primaryLanguage = null
        perms = listOf(null, null, null); permissionGate = PermissionGate.None; afterPermissions = null; fwdDone = false; sim = null
        bgJob?.cancel(); bgJob = null
        bgSetup = BgSetup.Off; bgStripGone = false; fwdFallback = false
        overlay = null; toast = null
        orbSpot = null; orbInSlot = false; settleJob?.cancel(); orbDock = null; dockLanding?.cancel(); dockLanding = null
        localBack = null
        Language.reset()
        arrivingFromSplash = false
        direction = 1
        step = Step.Splash
    }

    // MARK: Orb

    /** Moves the orb to a screen's slot: a glide between screens, or, the first time, a rise out of the splash's mark. */
    fun place(spot: OrbSpot) {
        if (spot == orbSpot) return
        // Settled in its slot, the orb moves with the screen; just keep the stage's spot current.
        if (orbInSlot) { orbSpot = spot; return }
        val first = orbSpot == null
        orbSpot = spot
        settleIntoSlot(if (first && splashMark == null) 0 else if (first) 1100 else 1000)
    }

    /** The screen is moving the orb to a different slot on the same screen: the stage takes it back so it glides there. */
    fun releaseOrb() {
        settleJob?.cancel()
        OrbFlight.departing()
        orbInSlot = false
    }

    /** Hands the orb to its slot once the glide has come to rest. */
    private fun settleIntoSlot(afterMs: Long) {
        settleJob?.cancel()
        settleJob = scope.launch { delay(afterMs); orbInSlot = true }
    }

    /** The splash mark in the flow's coordinates, once (the orb's first flight starts there). */
    fun takeSplashStart(): Offset? = splashMark?.let { it - contentOrigin }.also { splashMark = null }

    /** Welcome is done: lift the orb out of the flow (root coordinates) so it can fly into Home's Equal button. */
    fun dockOrb() {
        val spot = orbSpot ?: return
        val c = spot.center + contentOrigin
        val r = spot.diameter / 2
        orbDock = OrbDock(Rect(c.x - r, c.y - r, c.x + r, c.y + r))
        dockLanding?.cancel(); dockLanding = null
    }

    /** Home's Equal button is at [fab] (root px): fly there while turning into its artwork, then swap for the real button. */
    fun dockTarget(fab: Rect) {
        val dock = orbDock ?: return
        if (fab.width <= 0f) return
        orbDock = dock.copy(frame = fab, morph = 1f)
        dockLanding?.cancel()
        dockLanding = scope.launch {
            delay(1100)
            orbDock = null
            Haptics.land()
        }
    }

    // MARK: Permissions

    /** Permissions still off. */
    val missingPermissions: List<Permission>
        get() = Permission.entries.filterIndexed { i, _ -> perms.getOrNull(i) != true }

    /**
     * Asks for a batch of permissions and holds the flow until they're granted: the notifications
     * one as the number is sent, contacts and calls once a voice is chosen. [then] runs when they
     * are all in — never before.
     */
    fun askPermissions(gate: PermissionGate, then: () -> Unit) {
        if (gate == PermissionGate.None) { then(); return }
        afterPermissions = then
        permissionGate = gate
    }

    /** The batch came back granted: on with the flow. */
    fun permissionsSatisfied() {
        val next = afterPermissions
        afterPermissions = null
        permissionGate = PermissionGate.None
        overlay = null
        next?.invoke()
    }

    /** Reads what Android actually has granted (it may have changed in Settings). */
    fun refreshPermissions(context: Context) {
        val notifications = if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else NotificationManagerCompat.from(context).areNotificationsEnabled()
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val calls = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        perms = listOf(notifications, contacts, calls)
    }

    /** Whether a batch is fully granted, read from what Android says right now. */
    fun granted(gate: PermissionGate): Boolean = when (gate) {
        PermissionGate.None -> true
        PermissionGate.Notifications -> perms.getOrNull(0) == true
        PermissionGate.CallsAndContacts -> perms.getOrNull(1) == true && perms.getOrNull(2) == true
    }

    // MARK: Overlays

    /** Shows an overlay and waits until it is resolved with a result string. */
    suspend fun present(o: Overlay): String {
        pending?.complete("superseded")
        val d = CompletableDeferred<String>()
        pending = d
        if (o is Overlay.Blocked) Haptics.warning()
        overlay = o
        return d.await()
    }

    fun resolve(result: String) {
        val d = pending
        pending = null
        overlay = null
        d?.complete(result)
    }

    fun showToast(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = scope.launch { delay(1800); toast = null }
    }
}

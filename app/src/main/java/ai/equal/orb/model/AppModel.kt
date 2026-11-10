package ai.equal.orb.model

import ai.equal.orb.ui.BubbleStage
import ai.equal.orb.ui.Haptics
import ai.equal.orb.ui.OrbFlight
import ai.equal.orb.ui.OrbSpot
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

/**
 * Every stop on the onboarding, in order: a 6-digit code,
 * name and how to address you on one page, pick exactly 3 calls that turn into playable samples in
 * place, choose the assistant's voice, then set up. Android has no tracking prompt and no Live
 * Voicemail step; [Access] is call forwarding, which Equal turns on by itself.
 */
enum class Step {
    Splash, Phone, Otp, Name, Cases, Assistant, Sim, Access, Welcome, Home;

    val hasBack get() = this != Phone && this != Welcome
    val onSky get() = this != Splash && this != Home
}

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

/** The languages Equal speaks; a voice is previewed in English. */
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

enum class Permission { Notifications, Contacts }

/** The 3D art for whatever is missing: a bell, a contacts book, or a gear for both. */
val List<Permission>.art: Int
    get() = when {
        size > 1 -> ai.equal.orb.R.drawable.perm_gear
        firstOrNull() == Permission.Notifications -> ai.equal.orb.R.drawable.perm_bell
        else -> ai.equal.orb.R.drawable.perm_contacts
    }

/** Modal surfaces drawn above the flow. */
sealed interface Overlay {
    /** Equal's sheet after the system prompts, for whichever permissions were refused. */
    data class Decline(val missing: List<Permission>) : Overlay
}

/** The orb's last flight, from Welcome into Home's Equal button (root coordinates, px). */
data class OrbDock(val frame: Rect, val morph: Float = 0f)

class AppModel(val scope: CoroutineScope) {
    /** The app opens on Home; the onboarding is started from the profile tab. */
    var step by mutableStateOf(Step.Home)
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
    private var otpJob: Job? = null
    var name by mutableStateOf("")
    /** How Equal addresses the user on calls. */
    var gender by mutableStateOf<Gender?>(null)
    var whatsapp by mutableStateOf(true)
    val picks = mutableStateListOf<String>()
    /** The code is verified: the permission popups are due (they open over the name page). */
    var permissionsDue by mutableStateOf(false)
    var fwdDone by mutableStateOf(false)
    /** The SIM Equal takes calls on (0 or 1; null until one is picked). */
    var sim by mutableStateOf<Int?>(null)
    /** Notifications, contacts: granted? (null = not asked yet, which counts as off too.) */
    var perms by mutableStateOf(listOf<Boolean?>(null, null))

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

    /** The screens, in order. */
    private val route = listOf(Step.Phone, Step.Otp, Step.Name, Step.Cases, Step.Assistant, Step.Sim, Step.Access, Step.Welcome, Step.Home)

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
        Step.Cases -> "Show me how"
        Step.Assistant -> "Choose a voice"
        Step.Sim -> "Choose your SIM"
        Step.Access -> "Activate on this SIM"
        Step.Welcome -> "Finish setup"
        Step.Home -> "Go to Home"
        else -> "Continue"
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
        val code = listOf("4", "8", "2", "7", "1", "5")
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

    /** Code verified (automatically, or Confirm tapped once it's in): on to Name. */
    fun confirmOtp() {
        if (step != Step.Otp) return
        otpJob?.cancel()
        Haptics.success()
        go(Step.Name, haptic = false)
        if (perms.any { it == null }) permissionsDue = true
    }

    fun exitToHome() {
        if (step != Step.Welcome) Haptics.step()   // Welcome hands over to the orb's landing instead
        step = Step.Home
    }

    /** Starts the onboarding from the splash (the profile tab, or a long-press on the Home logo). */
    fun restart() {
        voice = Voice.Aanya; voiceChosen = false
        phone = ""; name = ""; gender = null; whatsapp = true; picks.clear()
        perms = listOf(null, null); permissionsDue = false; fwdDone = false; sim = null
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
        get() = (if (perms[0] == true) emptyList() else listOf(Permission.Notifications)) +
            (if (perms[1] == true) emptyList() else listOf(Permission.Contacts))

    /** Reads what Android actually has granted (it may have changed in Settings). */
    fun refreshPermissions(context: Context) {
        val notifications = if (Build.VERSION.SDK_INT >= 33)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else NotificationManagerCompat.from(context).areNotificationsEnabled()
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        perms = listOf(notifications, contacts)
    }

    // MARK: Overlays

    /** Shows an overlay and waits until it is resolved with a result string. */
    suspend fun present(o: Overlay): String {
        pending?.complete("superseded")
        val d = CompletableDeferred<String>()
        pending = d
        if (o is Overlay.Decline) Haptics.warning()
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

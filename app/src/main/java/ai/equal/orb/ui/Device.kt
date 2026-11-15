package ai.equal.orb.ui

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/**
 * What the phone itself can tell us, for the states that aren't the happy path: whether there's a
 * connection, whether it's a mobile one (call forwarding needs the network, not Wi-Fi), and
 * whether the ring switch is off (so nothing would be heard).
 */
object Device {
    private fun caps(context: Context): NetworkCapabilities? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return cm.getNetworkCapabilities(cm.activeNetwork ?: return null)
    }

    /** Any usable connection at all. */
    fun online(context: Context): Boolean =
        caps(context)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    /** On the mobile network (what the forwarding codes travel over). */
    fun onMobileNetwork(context: Context): Boolean =
        caps(context)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    /** The ring switch is off, or the phone is on vibrate: a preview would play into silence. */
    fun silent(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    /**
     * Whether Equal could quietly set up call forwarding now, while the user is busy with the
     * sample calls — and if not, what stands in the way. Everything here is read without asking
     * for anything: a system dialog in the middle of the demo is exactly what this is avoiding.
     *
     * [simChosen] is true once the user has said which SIM Equal answers on, which settles the one
     * question a dual-SIM phone would otherwise leave open.
     */
    fun forwardingBlock(context: Context, simChosen: Boolean): SetupBlock = runCatching {
        if (Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0) return SetupBlock.Airplane
        if (!online(context)) return SetupBlock.Offline
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return SetupBlock.NoSim
        val slots = if (Build.VERSION.SDK_INT >= 30) tm.activeModemCount else @Suppress("DEPRECATION") tm.phoneCount
        val ready = (0 until slots.coerceAtLeast(1)).count { tm.getSimState(it) == TelephonyManager.SIM_STATE_READY }
        when {
            ready == 0 -> SetupBlock.NoSim
            // Two SIMs, no default for calls and nothing picked in the app: which one Equal should
            // answer on is a question, not a guess.
            ready > 1 && !simChosen && SubscriptionManager.getDefaultVoiceSubscriptionId() ==
                SubscriptionManager.INVALID_SUBSCRIPTION_ID -> SetupBlock.TwoSims
            else -> SetupBlock.None
        }
    }.getOrDefault(SetupBlock.NoSim)
}

/**
 * Why call forwarding can't be set up quietly in the background. Anything but [None] means the
 * ordinary walkthrough, with no status strip shown at all.
 */
enum class SetupBlock { None, Offline, NoSim, Airplane, TwoSims }

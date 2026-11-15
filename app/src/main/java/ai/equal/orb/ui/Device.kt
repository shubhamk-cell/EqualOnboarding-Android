package ai.equal.orb.ui

import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

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
}

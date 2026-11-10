package ai.equal.orb.model

import android.content.Context
import android.media.MediaMetadataRetriever

/**
 * Natural-sounding recordings of the sample calls and voice hellos, rendered ahead of time with
 * the open Kokoro-82M voice model (Apache 2.0) and bundled as AAC in `res/raw`. Equal's lines exist
 * in both voices (`_f` Aanya, `_m` Kabir); each caller has a voice of their own (`_c`).
 * The recordings can't know the user's name, so they say “they” / “you” where the scripts on
 * screen use it. Anything without a recording falls back to the system's text-to-speech.
 */
object AudioClips {
    /** A script line as it's recorded: the name placeholder `{n}` reworded away. */
    fun spoken(line: String): String {
        var s = line
        val swaps = listOf(
            "Hi {n}, " to "Hi there, ",
            "I’ll let {n} know." to "I’ll pass it on.",
            "{n} never shares" to "they never share",
            "I’m {n}’s assistant" to "I’m the assistant on this number",
            "is this {n}?" to "is this the right number?",
            "Is {n} free to talk?" to "Are you free to talk?",
            "calling for {n}" to "calling on their behalf",
            "pass this to {n}" to "pass this on",
            "{n} is " to "they’re ",
            "{n} isn’t" to "they aren’t",
            "{n} will" to "they’ll",
            "{n} never" to "they never",
            "{n} asked" to "they asked",
            "{n}" to "them",
        )
        for ((a, b) in swaps) s = s.replace(a, b)
        // “They” at the start of a sentence.
        val out = StringBuilder()
        var capitalise = true
        for (ch in s) {
            out.append(if (capitalise) ch.uppercaseChar() else ch)
            if (ch in ".!?") capitalise = true else if (ch != ' ') capitalise = false
        }
        return out.toString()
    }

    /** “call_bank_1_f”: line [line] of call [id], in Equal's [voice] or the caller's. */
    fun name(call: String, line: Int, equal: Boolean, voice: Voice): String =
        "call_${call}_${line}_${if (equal) (if (voice.isFemale) "f" else "m") else "c"}"

    /** The recording of a call line, as a raw resource id (null when there isn't one). */
    fun raw(context: Context, call: String, line: Int, equal: Boolean, voice: Voice): Int? =
        resource(context, name(call, line, equal, voice))

    /** The language-card hello, recorded for English and Hindi, in Aanya's and Kabir's voices only (it says their name). */
    fun hello(context: Context, voice: Voice, l: CallLanguage): Int? {
        if (l != CallLanguage.English && l != CallLanguage.Hindi) return null
        if (voice != Voice.Aanya && voice != Voice.Kabir) return null
        return resource(context, "hello_${if (l == CallLanguage.English) "en" else "hi"}_${if (voice == Voice.Aanya) "f" else "m"}")
    }

    private val lengths = HashMap<Int, Float>()

    /** A recording's length in seconds (read once, then remembered). */
    fun seconds(context: Context, res: Int): Float = lengths.getOrPut(res) {
        val r = MediaMetadataRetriever()
        try {
            context.resources.openRawResourceFd(res).use { r.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toFloatOrNull() ?: 0f) / 1000f
        } catch (_: Exception) {
            0f
        } finally {
            r.release()
        }
    }

    private fun resource(context: Context, name: String): Int? =
        context.resources.getIdentifier(name, "raw", context.packageName).takeIf { it != 0 }
}

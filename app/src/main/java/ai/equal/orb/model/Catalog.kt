package ai.equal.orb.model

/** One everyday job Equal can take on, with the pretend call used to demo it. */
data class UseCase(
    val id: String,
    val emoji: String,
    val label: String,
    /** Who is on the other end of the sample call. */
    val caller: String,
    /** Caller's first line and Equal's reply. `{n}` is the user's name. */
    val callerLine: String,
    val equalLine: String,
    /** One-line summary shown after the call. */
    val summary: String,
    /** True when Equal places the call (reminders) rather than answering it. */
    val outgoing: Boolean,
)

object Catalog {
    /** The situations on the “What can I handle for you?” screen, in display order. */
    val useCases = listOf(
        UseCase("task_door", "🛵", "Guide delivery riders to my door", "Delivery rider",
            "I’m outside the society, which tower is it?",
            "Tower B, flat 402. Please use gate 2 — the guard will let you in.",
            "I guided them to your door and shared the gate number.", false),
        UseCase("bank", "🏦", "Say no to loan offers", "Bank",
            "About a pre-approved loan offer…",
            "{n} is busy right now. Please call back after 6 pm.",
            "I asked them to call back after 6 PM.", false),
        UseCase("task_bill", "⚡", "Remind me before bills", "You",
            "Hello?",
            "Hi {n}, quick reminder — your electricity bill is due in 2 days.",
            "I reminded you the electricity bill is due in 2 days.", true),
        UseCase("task_med", "💊", "Remind mom to take medicine", "Mom",
            "Hello? Who’s calling?",
            "Hi Aunty, it’s Equal calling for {n}. Just a reminder to take your 9 o’clock medicine.",
            "I reminded her to take her 9 o’clock medicine.", true),
        UseCase("unknown", "❓", "Screen calls from strangers", "Unknown number",
            "Hello, is this {n}?",
            "I’m {n}’s assistant. May I know who’s calling and why?",
            "I asked who they are and why they called.", false),
        UseCase("task_appt", "🩺", "Book doctor appointments", "Clinic",
            "We have slots tomorrow — what time works?",
            "Tomorrow at 6 PM works for {n}. Please book it under their name.",
            "I booked tomorrow at 6 PM under your name.", false),
        UseCase("hm_svc", "🔧", "Schedule AC repair", "AC technician",
            "I can come at 3 today.",
            "3 pm works. I’ll let {n} know you’re coming.",
            "I confirmed they’ll come at 3 PM today.", false),
        UseCase("task_resched", "📦", "Reschedule Amazon couriers", "Courier",
            "Sir, I’m at your address — is anyone home?",
            "No one’s home right now. Could you come back after 7 PM?",
            "I asked them to come back after 7 PM.", false),
        UseCase("hm_school", "🏫", "Note my kid’s school calls", "School",
            "Calling about the PTM on Friday.",
            "Thank you! I’ll pass this to {n} — they’ll call back soon.",
            "I noted it’s about the PTM on Friday.", false),
        UseCase("pro_meet", "💼", "Take messages in meetings", "Caller",
            "Is {n} free to talk?",
            "{n} is in a meeting — they’ll call you back within the hour.",
            "I said you’re in a meeting and will call back within the hour.", false),
        UseCase("cab", "🚕", "Guide cab drivers to my pickup", "Cab driver",
            "Madam, I’m at the main road — where exactly are you?",
            "Please take the second left after the petrol pump. {n} is waiting at the blue gate.",
            "I guided them to your pickup at the blue gate.", false),
        UseCase("gas", "🔥", "Book my gas cylinder refill", "Gas agency",
            "Your cylinder booking is due — shall we confirm it?",
            "Yes, please book it for {n} and deliver after 5 pm.",
            "I booked your refill for delivery after 5 PM.", false),
        UseCase("card", "💳", "Decline credit card offers", "Card sales",
            "Sir, you’re eligible for a lifetime-free credit card…",
            "Thanks, but {n} isn’t interested. Please remove this number from your list.",
            "I declined and asked them not to call again.", false),
        UseCase("insurance", "🛡️", "Handle insurance renewal calls", "Insurance agent",
            "Your car insurance expires next week.",
            "Please send the renewal quote on WhatsApp — {n} will check it tonight.",
            "I asked them to send the quote on WhatsApp.", false),
        UseCase("guard", "💂", "Talk to my building guard", "Security guard",
            "Someone from the bank is at the gate for you.",
            "Please ask them to leave the documents with you — {n} will collect them.",
            "I asked the guard to keep the documents.", false),
        UseCase("rent", "🏠", "Remind me to pay rent", "You",
            "Hello?",
            "Hi {n}, your rent is due on the 5th — just a heads-up.",
            "I reminded you rent is due on the 5th.", true),
        UseCase("bday", "🎂", "Wish family on their birthdays", "Cousin",
            "Hello?",
            "Happy birthday! {n} asked me to call and wish you a wonderful year.",
            "I wished them happy birthday from you.", true),
        UseCase("dad", "👴", "Remind dad about his checkup", "Dad",
            "Haan, who is this?",
            "Uncle, it’s Equal calling for {n}. Your checkup is tomorrow at 10 am.",
            "I reminded him about tomorrow’s 10 AM checkup.", true),
        UseCase("drive", "🚗", "Answer calls while I drive", "Friend",
            "Hey, are you free to talk?",
            "{n} is driving right now. Shall I ask them to call you back in 30 minutes?",
            "I said you’re driving and will call back.", false),
        UseCase("broker", "🏢", "Screen property broker calls", "Property broker",
            "We have a great 2BHK in your area…",
            "{n} isn’t looking right now. Please don’t call again.",
            "I said you’re not looking and declined.", false),
        UseCase("salon", "💇", "Confirm salon appointments", "Salon",
            "Calling to confirm your 4 pm haircut.",
            "Yes, {n} will be there at 4. Thank you!",
            "I confirmed your 4 PM appointment.", false),
        UseCase("tutor", "📚", "Take my kid’s tuition calls", "Tutor",
            "Tomorrow’s class is moved to 6 pm.",
            "Got it — 6 pm tomorrow. I’ll let {n} know.",
            "I noted tomorrow’s class moved to 6 PM.", false),
        UseCase("wifi", "📡", "Schedule broadband repairs", "Broadband technician",
            "I can come between 2 and 4 today.",
            "That works. Please call this number when you’re downstairs.",
            "I scheduled the visit for 2–4 PM today.", false),
        UseCase("help", "🧹", "Coordinate with my house help", "House help",
            "Didi, I’ll come late today, around 11.",
            "No problem, 11 is fine. I’ll tell {n}.",
            "I noted she’ll come at 11 today.", false),
        UseCase("recharge", "📱", "Remind me to recharge my phone", "You",
            "Hello?",
            "Hi {n}, your prepaid plan ends in 2 days — time to recharge.",
            "I reminded you your plan ends in 2 days.", true)
    )

    fun useCase(id: String) = useCases.firstOrNull { it.id == id }

    /**
     * The picks' voice-note covers: a profile photo of who's on the call (AI-generated, fictional
     * people), one per kind of caller and shared by the options they cover.
     */
    fun cover(id: String): Int? = when (id) {
        "task_door", "task_resched", "ev_rider" -> ai.equal.orb.R.drawable.cover_delivery
        "bank", "card", "insurance", "broker", "ev_kyc", "ev_loan" -> ai.equal.orb.R.drawable.cover_sales
        "task_bill", "rent", "recharge" -> ai.equal.orb.R.drawable.cover_you
        "task_med" -> ai.equal.orb.R.drawable.cover_mom
        "dad" -> ai.equal.orb.R.drawable.cover_dad
        "unknown", "ev_unknown" -> ai.equal.orb.R.drawable.cover_unknown
        "task_appt", "salon" -> ai.equal.orb.R.drawable.cover_reception
        "hm_svc", "wifi", "gas" -> ai.equal.orb.R.drawable.cover_technician
        "hm_school", "tutor" -> ai.equal.orb.R.drawable.cover_teacher
        "pro_meet", "drive", "bday" -> ai.equal.orb.R.drawable.cover_friend
        "cab" -> ai.equal.orb.R.drawable.cover_cab
        "guard" -> ai.equal.orb.R.drawable.cover_guard
        "help" -> ai.equal.orb.R.drawable.cover_help
        else -> null
    }

    /** Tints for the emoji tiles on the three sample-call cards. */
    val cardTints = listOf(0xFFE6D5L, 0xE2F8E6L, 0xECE6FFL)

    /** The four lines of a sample call. */
    fun script(u: UseCase, name: String): List<Pair<String, String>> {
        fun f(s: String) = s.replace("{n}", name)
        return listOf(u.caller to f(u.callerLine), "Equal AI" to f(u.equalLine),
            u.caller to "Okay, got it. Thank you!", "Equal AI" to "You’re welcome! I’ll let $name know.")
    }

    fun summary(u: UseCase): String {
        val lead = if (u.outgoing) "I called ${if (u.caller == "You") "you" else u.caller} as scheduled." else "${u.caller} called just now."
        return "$lead ${u.summary} Nothing for you to do."
    }

    // MARK: Orb lines

    /** What the orb says on each screen. */
    enum class Ask { Phone, Otp, Name, Cases, Fwd, Access, Welcome }

    fun title(ask: Ask, name: String = "") = when (ask) {
        Ask.Phone -> "What number do you get calls on?"
        Ask.Otp -> "Enter the code I just texted you"
        Ask.Name -> "What should I call you?"
        Ask.Cases -> "I can handle all kinds of calls! Tap the ones that matter most to you"
        Ask.Fwd -> "One code, and all calls you can’t or don’t want to take come to me"
        Ask.Access -> "Setting up call forwarding, so missed calls come to me"
        Ask.Welcome -> "Welcome to Equal, $name!"
    }

    const val curating = "Curating sample calls from your picks…"
    const val forwardingDone = "Done! Missed calls now come to me"

    /** The three call-forwarding cases Equal sets up on Android. */
    val forwardingCases = listOf("When you’re busy", "When you don’t answer", "When you’re unreachable")

    /** On the name screen once the name is in. */
    fun genderAsk(name: String) = "Nice to meet you, $name! What’s your gender? I’ll use it to address you on calls"
    /** After the sample calls. */
    const val assistantAsk = "Now that you’ve seen me in action, how would you like me to\u00A0sound?"
    /** The picks: exactly three, and they turn into sample calls on the spot. */
    const val pickThree = "I can handle all your calls. Select any 3 to get a\u00A0glimpse"
    /** Said over the sample-call cards; counts the picks shown (up to three). */
    fun curated(count: Int) =
        if (count <= 1) "Here’s how I’ll handle your pick" else "Here’s how I’ll handle your top\u00A0${minOf(count, 3)}\u00A0picks"

    /** The orb's lines in the other languages, keyed by the English line. `{name}` stands in for the user's name. */
    private val translations: Map<String, Map<Lang, String>> by lazy {
        mapOf(
        title(Ask.Phone, "") to mapOf(Lang.Hi to "आपके कॉल किस नंबर पर आते हैं?",
                                      Lang.Ta to "உங்களுக்கு எந்த எண்ணில் அழைப்புகள் வரும்?",
                                      Lang.Bn to "আপনার কল কোন নম্বরে আসে?"),
            title(Ask.Otp, "") to mapOf(Lang.Hi to "अभी भेजा हुआ कोड डालिए",
                                    Lang.Ta to "நான் இப்போது அனுப்பிய குறியீட்டை உள்ளிடுங்கள்",
                                    Lang.Bn to "এইমাত্র পাঠানো কোডটি লিখুন"),
            title(Ask.Name, "") to mapOf(Lang.Hi to "मैं आपको क्या कहकर बुलाऊँ?",
                                     Lang.Ta to "உங்களை நான் எப்படி அழைக்கட்டும்?",
                                     Lang.Bn to "আপনাকে কী নামে ডাকব?"),
            title(Ask.Cases, "") to mapOf(Lang.Hi to "मैं हर तरह के कॉल संभाल सकती हूँ! आपके लिए जो सबसे ज़रूरी हैं, उन पर टैप कीजिए",
                                      Lang.Ta to "எல்லா வகை அழைப்புகளையும் நான் கவனிப்பேன்! உங்களுக்கு முக்கியமானவற்றைத் தட்டுங்கள்",
                                      Lang.Bn to "সব ধরনের কল আমি সামলাতে পারি! আপনার কাছে যেগুলো জরুরি, সেগুলোয় ট্যাপ করুন"),
            curating to mapOf(Lang.Hi to "आपकी पसंद से सैंपल कॉल तैयार हो रहे हैं…",
                       Lang.Ta to "உங்கள் தேர்வுகளிலிருந்து மாதிரி அழைப்புகளைத் தயாரிக்கிறேன்…",
                       Lang.Bn to "আপনার বাছাই থেকে নমুনা কল তৈরি হচ্ছে…"),
            curated(3) to mapOf(Lang.Hi to "देखिए, आपकी टॉप 3 पसंद कैसे संभाली जाएँगी",
                         Lang.Ta to "உங்கள் முதல் 3 தேர்வுகளை இப்படிக் கையாள்வேன்",
                         Lang.Bn to "আপনার সেরা ৩টি বাছাই আমি এভাবে সামলাব"),
            curated(2) to mapOf(Lang.Hi to "देखिए, आपकी टॉप 2 पसंद कैसे संभाली जाएँगी",
                         Lang.Ta to "உங்கள் முதல் 2 தேர்வுகளை இப்படிக் கையாள்வேன்",
                         Lang.Bn to "আপনার সেরা ২টি বাছাই আমি এভাবে সামলাব"),
            curated(1) to mapOf(Lang.Hi to "देखिए, आपकी पसंद कैसे संभाली जाएगी",
                         Lang.Ta to "உங்கள் தேர்வை இப்படிக் கையாள்வேன்",
                         Lang.Bn to "আপনার বাছাইটি আমি এভাবে সামলাব"),
            title(Ask.Fwd, "") to mapOf(Lang.Hi to "बस एक कोड, और जो कॉल आप नहीं ले सकते या नहीं लेना चाहते, वो मेरे पास आएँगे",
                                    Lang.Ta to "ஒரே ஒரு குறியீடு — நீங்கள் எடுக்க முடியாத அல்லது விரும்பாத அழைப்புகள் என்னிடம் வரும்",
                                    Lang.Bn to "একটাই কোড, আর যে কল আপনি ধরতে পারবেন না বা চান না, সব আমার কাছে আসবে"),
            title(Ask.Access, "") to mapOf(Lang.Hi to "अब वॉइसमेल की जगह मुझे जवाब देने दीजिए",
                                       Lang.Ta to "இனி வாய்ஸ்மெயிலுக்குப் பதிலாக நான் பதிலளிக்கிறேன்",
                                       Lang.Bn to "এবার ভয়েসমেলের বদলে আমাকে উত্তর দিতে দিন"),
            genderAsk("{name}") to mapOf(Lang.Hi to "आपसे मिलकर अच्छा लगा, {name}! आपका जेंडर क्या है? कॉल पर आपको उसी हिसाब से संबोधित करूँगी",
                                  Lang.Ta to "உங்களைச் சந்தித்ததில் மகிழ்ச்சி, {name}! உங்கள் பாலினம் என்ன? அழைப்புகளில் உங்களை அப்படியே குறிப்பிடுவேன்",
                                  Lang.Bn to "আপনার সঙ্গে আলাপ করে ভালো লাগল, {name}! আপনার লিঙ্গ কী? কলে আপনাকে সেভাবেই সম্বোধন করব"),
            assistantAsk to mapOf(Lang.Hi to "अब आपने मुझे काम करते देख लिया, बताइए मेरी आवाज़ कैसी हो?",
                           Lang.Ta to "என்னைச் செயலில் பார்த்துவிட்டீர்கள், என் குரல் எப்படி இருக்க வேண்டும்?",
                           Lang.Bn to "আমাকে কাজ করতে দেখলেন, এবার বলুন আমার গলা কেমন হবে?"),
            pickThree to mapOf(Lang.Hi to "मैं आपके सारे कॉल संभाल सकती हूँ। एक झलक के लिए कोई भी 3 चुनिए",
                        Lang.Ta to "உங்கள் எல்லா அழைப்புகளையும் நான் கவனிப்பேன். ஒரு பார்வைக்கு ஏதேனும் 3-ஐத் தேர்ந்தெடுங்கள்",
                        Lang.Bn to "আপনার সব কল আমি সামলাতে পারি। এক ঝলক দেখতে যেকোনো ৩টে বেছে নিন"),
            title(Ask.Welcome, "{name}") to mapOf(Lang.Hi to "Equal में आपका स्वागत है, {name}!",
                                              Lang.Ta to "Equal-க்கு வரவேற்கிறோம், {name}!",
                                              Lang.Bn to "Equal-এ স্বাগতম, {name}!"),
        )
    }

    /** [line] (English) in [lang]; lines without a translation stay in English. */
    fun translate(line: String, lang: Lang): String {
        if (lang == Lang.En) return line
        translations[line]?.get(lang)?.let { return it }
        // The welcome and gender lines carry the user's name.
        for (key in listOf(title(Ask.Welcome, "{name}"), genderAsk("{name}"))) {
            val (pre, post) = key.split("{name}")
            if (line.startsWith(pre) && line.endsWith(post) && line.length >= pre.length + post.length) {
                val t = translations[key]?.get(lang) ?: continue
                return t.replace("{name}", line.substring(pre.length, line.length - post.length))
            }
        }
        return line
    }

    /** What a voice says when previewed, in the chosen language. */
    fun voiceHello(voice: Voice, l: CallLanguage, name: String): String {
        val v = voice.title
        return when (l) {
            CallLanguage.English -> "Hi $name, I’m $v. I’ll take your calls from now on."
            CallLanguage.Hindi -> "नमस्ते $name, मैं $v हूँ। अब से आपके कॉल मैं ${if (voice.isFemale) "संभालूँगी" else "संभालूँगा"}।"
            CallLanguage.Hinglish -> "Hi $name, main $v hoon. Ab se aapke calls main ${if (voice.isFemale) "sambhaalungi" else "sambhaalunga"}."
            CallLanguage.Tamil -> "வணக்கம் $name, நான் $v. இனி உங்கள் அழைப்புகளை நான் பார்த்துக்கொள்கிறேன்."
            CallLanguage.Telugu -> "నమస్తే $name, నేను $v. ఇకపై మీ కాల్స్ నేను చూసుకుంటాను."
            CallLanguage.Kannada -> "ನಮಸ್ಕಾರ $name, ನಾನು $v. ಇನ್ನು ಮುಂದೆ ನಿಮ್ಮ ಕರೆಗಳನ್ನು ನಾನು ನೋಡಿಕೊಳ್ಳುತ್ತೇನೆ."
            CallLanguage.Bengali -> "নমস্কার $name, আমি $v। এখন থেকে আপনার কল আমি সামলাব।"
            CallLanguage.Marathi -> "नमस्कार $name, मी $v. आतापासून तुमचे कॉल मी सांभाळेन."
            CallLanguage.Malayalam -> "നമസ്കാരം $name, ഞാൻ $v. ഇനി നിങ്ങളുടെ കോളുകൾ ഞാൻ നോക്കിക്കൊള്ളാം."
        }
    }
}

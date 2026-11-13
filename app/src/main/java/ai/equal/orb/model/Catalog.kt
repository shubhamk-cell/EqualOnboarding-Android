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
        UseCase("task_appt", "🩺", "Book doctor visits", "Clinic",
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
        UseCase("pro_meet", "💼", "Take calls in meetings", "Caller",
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
        UseCase("guard", "💂", "Talk to my gate guard", "Security guard",
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
     * The twelve options on the picks page, two to a row, in this order: each row pairs a long label
     * with a short one, the long side alternating, so every row's widths are uneven.
     */
    val pickOptions = listOf(
        "task_door", "bank", "task_bill", "task_med", "unknown", "guard",
        "drive", "cab", "task_resched", "pro_meet", "task_appt", "bday",
    ).map { id -> useCases.first { it.id == id } }

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
    enum class Ask { Phone, Otp, Name, Cases, Sim, Fwd, Access, Welcome }

    fun title(ask: Ask, name: String = "") = when (ask) {
        Ask.Phone -> "What number do you get calls on?"
        Ask.Otp -> "Enter the code I just texted you"
        Ask.Name -> "What should I call you?"
        Ask.Cases -> "I can handle all kinds of calls! Tap the ones that matter most to you"
        Ask.Sim -> "Which SIM should I answer calls on? Only one can be active at a time"
        Ask.Fwd -> "One code, and all calls you can’t or don’t want to take come to me"
        Ask.Access -> "Setting up call forwarding, so missed calls come to me"
        Ask.Welcome -> "Welcome to Equal, $name!"
    }

    const val curating = "Curating sample calls from your picks…"
    const val forwardingDone = "Done! Missed calls now come to me"


    /** The languages page: first which ones callers speak, then which one to greet them in. */
    const val languagesAsk = "Which languages do your callers speak? Pick up to\u00A03"
    const val primaryAsk = "Which one should I greet your callers\u00A0in?"

    /** How Equal answers a caller in [l], for the user called [name]. */
    fun greeting(l: CallLanguage, name: String) = when (l) {
        CallLanguage.English -> "Hello! I’m $name’s assistant. How can I help?"
        CallLanguage.Hindi -> "नमस्ते! मैं $name की असिस्टेंट हूँ। बताइए, कैसे मदद करूँ?"
        CallLanguage.Hinglish -> "Hello! Main $name ki assistant hoon. Bataiye, kaise help karun?"
        CallLanguage.Tamil -> "வணக்கம்! நான் $name-இன் உதவியாளர். எப்படி உதவலாம்?"
        CallLanguage.Telugu -> "నమస్తే! నేను $name అసిస్టెంట్‌ని. ఎలా సహాయం చేయగలను?"
        CallLanguage.Kannada -> "ನಮಸ್ಕಾರ! ನಾನು $name ಅವರ ಅಸಿಸ್ಟೆಂಟ್. ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"
        CallLanguage.Bengali -> "নমস্কার! আমি $name-এর অ্যাসিস্ট্যান্ট। কীভাবে সাহায্য করতে পারি?"
        CallLanguage.Marathi -> "नमस्कार! मी $name यांची असिस्टंट. काय मदत करू?"
        CallLanguage.Malayalam -> "നമസ്കാരം! ഞാൻ $name-ന്റെ അസിസ്റ്റന്റ് ആണ്. എങ്ങനെ സഹായിക്കാം?"
    }

    /**
     * The same call in [l]. The recordings are English, so every other language is the same four
     * turns spoken by the device: the caller asks for the user, Equal answers for them. [voice]
     * only matters where the verb carries the speaker's gender.
     */
    fun script(u: UseCase, name: String, l: CallLanguage, voice: Voice): List<Pair<String, String>> {
        if (l == CallLanguage.English) return script(u, name)
        val f = voice.isFemale
        val lines = when (l) {
            CallLanguage.Hindi -> listOf(
                "नमस्ते, क्या यह $name का नंबर है?",
                "मैं $name की असिस्टेंट हूँ। बताइए, किस बारे में कॉल किया है?",
                "ठीक है, समझ गया। धन्यवाद!",
                "कोई बात नहीं! मैं $name को ${if (f) "बता दूँगी" else "बता दूँगा"}।",
            )
            CallLanguage.Hinglish -> listOf(
                "Hello, kya ye $name ka number hai?",
                "Main $name ki assistant hoon. Bataiye, kis baare mein call kiya hai?",
                "Theek hai, samajh gaya. Thank you!",
                "Koi baat nahi! Main $name ko bata ${if (f) "dungi" else "dunga"}.",
            )
            CallLanguage.Tamil -> listOf(
                "வணக்கம், இது $name அவர்களின் எண்ணா?",
                "நான் $name அவர்களின் உதவியாளர். எதற்காக அழைத்தீர்கள் என்று சொல்லுங்கள்.",
                "சரி, புரிந்தது. நன்றி!",
                "பரவாயில்லை! நான் $name அவர்களிடம் சொல்கிறேன்.",
            )
            CallLanguage.Telugu -> listOf(
                "నమస్తే, ఇది $name నంబరేనా?",
                "నేను $name అసిస్టెంట్‌ని. ఎందుకు కాల్ చేశారో చెప్పండి.",
                "సరే, అర్థమైంది. ధన్యవాదాలు!",
                "ఫర్వాలేదు! నేను ${name}కి చెబుతాను.",
            )
            CallLanguage.Kannada -> listOf(
                "ನಮಸ್ಕಾರ, ಇದು $name ಅವರ ನಂಬರ್ ಅಲ್ಲವೇ?",
                "ನಾನು $name ಅವರ ಅಸಿಸ್ಟೆಂಟ್. ಯಾವ ವಿಷಯಕ್ಕೆ ಕರೆ ಮಾಡಿದ್ದೀರಿ ಹೇಳಿ.",
                "ಸರಿ, ಅರ್ಥವಾಯಿತು. ಧನ್ಯವಾದಗಳು!",
                "ಪರವಾಗಿಲ್ಲ! ನಾನು $name ಅವರಿಗೆ ತಿಳಿಸುತ್ತೇನೆ.",
            )
            CallLanguage.Bengali -> listOf(
                "নমস্কার, এটা কি $name-এর নম্বর?",
                "আমি $name-এর অ্যাসিস্ট্যান্ট। বলুন, কী বিষয়ে ফোন করেছেন?",
                "ঠিক আছে, বুঝেছি। ধন্যবাদ!",
                "কোনো ব্যাপার না! আমি $name-কে জানিয়ে দেব।",
            )
            CallLanguage.Marathi -> listOf(
                "नमस्कार, हा $name यांचा नंबर आहे का?",
                "मी $name यांची असिस्टंट. सांगा, कशासाठी फोन केला आहे?",
                "ठीक आहे, समजलं. धन्यवाद!",
                "हरकत नाही! मी $name यांना सांगेन.",
            )
            CallLanguage.Malayalam -> listOf(
                "നമസ്കാരം, ഇത് $name-ന്റെ നമ്പർ അല്ലേ?",
                "ഞാൻ $name-ന്റെ അസിസ്റ്റന്റ് ആണ്. എന്താവശ്യത്തിനാണ് വിളിച്ചതെന്ന് പറയൂ.",
                "ശരി, മനസ്സിലായി. നന്ദി!",
                "സാരമില്ല! ഞാൻ $name-നോട് പറയാം.",
            )
            CallLanguage.English -> script(u, name).map { it.second }
        }
        return listOf(u.caller to lines[0], "Equal AI" to lines[1], u.caller to lines[2], "Equal AI" to lines[3])
    }

    /** How Equal opens a call in [l]: the greeting alone, the same length on every card. */
    fun greetingWord(l: CallLanguage) = when (l) {
        CallLanguage.English, CallLanguage.Hinglish -> "Hello!"
        CallLanguage.Hindi -> "नमस्ते!"
        CallLanguage.Tamil -> "வணக்கம்!"
        CallLanguage.Telugu -> "నమస్తే!"
        CallLanguage.Kannada -> "ನಮಸ್ಕಾರ!"
        CallLanguage.Bengali -> "নমস্কার!"
        CallLanguage.Marathi -> "नमस्कार!"
        CallLanguage.Malayalam -> "നമസ്കാരം!"
    }

    /** On the name screen once the name is in. */
    fun genderAsk(name: String) = "Nice to meet you, $name! What’s your gender?"
    /** After the sample calls. */
    const val assistantAsk = "Now that you’ve seen me in action, how would you like me to\u00A0sound?"
    /** The picks: exactly three, and they turn into sample calls on the spot. */
    const val pickThree = "Select any 3 to get a glimpse of how I handle all your\u00A0calls"
    /** Said over the sample-call cards; counts the picks shown (up to three). */
    fun curated(count: Int) =
        if (count <= 1) "Here’s how I’ll handle your pick" else "Here’s how I’ll handle your top\u00A0${minOf(count, 3)}\u00A0picks"

    /** The orb's lines in the other languages, keyed by the English line. `{name}` stands in for the user's name. */
    private val translations: Map<String, Map<Lang, String>> by lazy {
        mapOf(
        title(Ask.Phone, "") to mapOf(Lang.Hi to "आपके कॉल किस नंबर पर आते हैं?",
                                      Lang.Ta to "உங்களுக்கு எந்த எண்ணில் அழைப்புகள் வரும்?",
                                      Lang.Bn to "আপনার কল কোন নম্বরে আসে?"),
            title(Ask.Sim, "") to mapOf(Lang.Hi to "मैं किस सिम पर कॉल का जवाब दूँ? एक समय में एक ही सिम चालू रह सकती है",
                Lang.Ta to "எந்த சிம்மில் நான் அழைப்புகளுக்குப் பதிலளிக்கட்டும்? ஒரே நேரத்தில் ஒன்று மட்டுமே இயங்கும்",
                Lang.Bn to "কোন সিমে আমি কল ধরব? একসঙ্গে একটাই সিম চালু থাকতে পারে"),
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
            genderAsk("{name}") to mapOf(Lang.Hi to "आपसे मिलकर अच्छा लगा, {name}! आपका जेंडर क्या है?",
                                  Lang.Ta to "உங்களைச் சந்தித்ததில் மகிழ்ச்சி, {name}! உங்கள் பாலினம் என்ன?",
                                  Lang.Bn to "আপনার সঙ্গে আলাপ করে ভালো লাগল, {name}! আপনার লিঙ্গ কী?"),
            assistantAsk to mapOf(Lang.Hi to "अब आपने मुझे काम करते देख लिया, बताइए मेरी आवाज़ कैसी हो?",
                           Lang.Ta to "என்னைச் செயலில் பார்த்துவிட்டீர்கள், என் குரல் எப்படி இருக்க வேண்டும்?",
                           Lang.Bn to "আমাকে কাজ করতে দেখলেন, এবার বলুন আমার গলা কেমন হবে?"),
            languagesAsk to mapOf(Lang.Hi to "आपको कॉल करने वाले कौन-सी भाषाएँ बोलते हैं? ज़्यादा से ज़्यादा 3 चुनिए",
                Lang.Ta to "உங்களை அழைப்பவர்கள் எந்த மொழிகள் பேசுவார்கள்? 3 வரை தேர்ந்தெடுங்கள்",
                Lang.Bn to "আপনাকে যাঁরা কল করেন, তাঁরা কোন ভাষায় কথা বলেন? ৩টে পর্যন্ত বেছে নিন"),
            primaryAsk to mapOf(Lang.Hi to "इनमें से किस भाषा में मैं कॉल करने वालों का स्वागत करूँ?",
                Lang.Ta to "இவற்றில் எந்த மொழியில் அழைப்பவர்களை நான் வரவேற்கட்டும்?",
                Lang.Bn to "এর মধ্যে কোন ভাষায় আমি কলারদের স্বাগত জানাব?"),
            pickThree to mapOf(Lang.Hi to "कोई भी 3 चुनिए और झलक देखिए कि मैं आपके सारे कॉल कैसे संभालती हूँ",
                        Lang.Ta to "ஏதேனும் 3-ஐத் தேர்ந்தெடுங்கள், உங்கள் அழைப்புகளை நான் எப்படிக் கவனிப்பேன் என்று பாருங்கள்",
                        Lang.Bn to "যেকোনো ৩টে বেছে নিন, দেখুন আপনার সব কল আমি কীভাবে সামলাই"),
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

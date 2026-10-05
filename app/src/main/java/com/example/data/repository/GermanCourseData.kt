package com.example.data.repository

import com.example.data.model.Dialogue
import com.example.data.model.DialogueLine
import com.example.data.model.GermanNumber
import com.example.data.model.Lesson

object GermanCourseData {

    val lessons: List<Lesson> = listOf(
        Lesson(
            id = 1,
            titleDari = "درس ۱: سلام و معرفی خود",
            titleGerman = "Lektion 1: Begrüßung & Vorstellen",
            descriptionDari = "یادگیری سلام، خداحافظی، معرفی نام و محل زندگی، کشورها و اعداد ۰ تا ۱۰",
            iconEmoji = "👋",
            grammarTopicDari = "افعال مهم: heißen, sein, kommen, wohnen",
            grammarExplanationDari = "در زبان آلمانی، افعال برای اشخاص مختلف پسوند می‌گیرند:\nIch heiße... (من نامیده می‌شوم)\nDu heißt... (تو نامیده می‌شوی)\nEr/Sie heißt... (او نامیده می‌شود)\nIch komme aus... (من از ... می‌آیم)\nIch wohne in... (من در ... زندگی می‌کنم)",
            keyVerbs = listOf("heißen", "sein", "kommen", "wohnen")
        ),
        Lesson(
            id = 2,
            titleDari = "درس ۲: راه‌های تماس و شغل‌ها",
            titleGerman = "Lektion 2: Kontakte & Berufe",
            descriptionDari = "معرفی شغل‌ها، هجی کردن ایمیل و شماره تلفن، اعداد ۱۱ تا ۱۰۰",
            iconEmoji = "💼",
            grammarTopicDari = "افعال: haben, lernen, sprechen و مؤنث کردن شغل‌ها با in-",
            grammarExplanationDari = "برای شغل خانم‌ها، معمولاً پسوند in- اضافه می‌شود:\nder Lehrer (معلم آقا) -> die Lehrerin (معلم خانم)\nder Arzt (داکتر آقا) -> die Ärztin (داکتر خانم)\nفعل sprechen نامنظم است: ich spreche, du sprichst, er spricht.",
            keyVerbs = listOf("haben", "lernen", "sprechen")
        ),
        Lesson(
            id = 3,
            titleDari = "درس ۳: اشیا و وسایل روزمره",
            titleGerman = "Lektion 3: Gegenstände im Alltag",
            descriptionDari = "نام وسایل، حروف تعریف (der, die, das) و پرسش: Wie heißt das auf Deutsch?",
            iconEmoji = "📦",
            grammarTopicDari = "حروف تعریف معین (der/die/das) و نامعین (ein/eine) و منفی (kein/keine)",
            grammarExplanationDari = "در آلمانی هر اسم یک جنسیت دارد:\nder = مذکر (رنگ آبی)\ndie = مؤنث (رنگ سرخ)\ndas = خنثی (رنگ سبز)\nحرف تعریف منفی: Das ist kein Stift (این قلم نیست)، Das ist keine Tasche (این بکس نیست).",
            keyVerbs = listOf("brauchen", "finden", "kosten")
        ),
        Lesson(
            id = 4,
            titleDari = "درس ۴: در کافه و سفارش دادن",
            titleGerman = "Lektion 4: Im Café & Bestellen",
            descriptionDari = "سفارش چای، قهوه، نوشیدنی‌ها، قیمت‌ها و پرداخت حساب (Einen Kaffee, bitte)",
            iconEmoji = "☕",
            grammarTopicDari = "حالت مفعولی (Akkusativ) با فعل möchten و nehmen",
            grammarExplanationDari = "وقتی چیزی را سفارش می‌دهید (حالت اکوزاتیو)، حرف تعریف مذکر تغییر می‌کند:\nder Kaffee -> Ich möchte einen Kaffee.\nاما خنثی و مؤنث تغییر نمی‌کنند: ein Wasser, eine Cola.\nجمله کلیدی: Zusammen oder getrennt? (یکجا حساب می‌کنید یا جداگانه؟)",
            keyVerbs = listOf("möchten", "trinken", "essen", "bezahlen")
        ),
        Lesson(
            id = 5,
            titleDari = "درس ۵: روزهای هفته و کارهای روزمره",
            titleGerman = "Lektion 5: Wochentage & Tagesablauf",
            descriptionDari = "نام روزهای هفته، ساعات روز، افعال جداشدنی و برنامه روزانه",
            iconEmoji = "📅",
            grammarTopicDari = "افعال جداشدنی (Trennbare Verben) و حروف اضافه زمان",
            grammarExplanationDari = "روزهای هفته همیشه با حرف اضافه am می‌آیند: am Montag (روز دوشنبه).\nافعال جداشدنی پیشوندشان به آخر جمله می‌رود:\naufstehen (بیدار شدن/بلند شدن) -> Ich stehe um 7 Uhr auf.\neinkaufen (خرید کردن) -> Ich kaufe am Samstag ein.",
            keyVerbs = listOf("aufstehen", "frühstücken", "arbeiten", "einkaufen")
        ),
        Lesson(
            id = 6,
            titleDari = "درس ۶: غذاها و طعم‌ها",
            titleGerman = "Lektion 6: Essen & Geschmack",
            descriptionDari = "خوراکی‌ها، میوه‌ها، سبزیجات، بیان سلیقه و طعم‌ها (Das schmeckt gut!)",
            iconEmoji = "🍎",
            grammarTopicDari = "فعل‌های essen, schmecken و استفاده از gern و nicht gern",
            grammarExplanationDari = "فعل schmecken با مفعول به کار می‌رود: Das schmeckt gut (این مزه خوبی دارد/خوشمزه است).\nبرای گفتن علاقه از gern استفاده می‌کنیم:\nIch esse gern Fisch. (من ماهی دوست دارم)\nIch trinke nicht gern Tee. (من چای دوست ندارم)",
            keyVerbs = listOf("essen", "schmecken", "kochen", "mögen")
        ),
        Lesson(
            id = 7,
            titleDari = "درس ۷: خانواده من",
            titleGerman = "Lektion 7: Meine Familie",
            descriptionDari = "اعضای خانواده، صفت‌های ملکی (mein/meine, dein/deine) و نسبت‌های فامیلی",
            iconEmoji = "👨‍👩‍👧‍👦",
            grammarTopicDari = "صفات ملکی: mein/meine (مال من) و dein/deine (مال تو)",
            grammarExplanationDari = "برای اسم‌های مذکر و خنثی: mein Vater (پدر من), mein Kind (کودک من).\nبرای اسم‌های مؤنث و جمع یک -e اضافه می‌شود:\nmeine Mutter (مادر من), meine Eltern (والدین من).\nهمین قاعده برای dein و deine صدق می‌کند.",
            keyVerbs = listOf("haben", "leben", "lieben")
        ),
        Lesson(
            id = 8,
            titleDari = "درس ۸: خانه و مسکن",
            titleGerman = "Lektion 8: Wohnen & Möbel",
            descriptionDari = "اتاق‌ها، وسایل خانه، توصیف آپارتمان (بزرگ، کوچک، روشن، ارزان) و اسباب‌کشی",
            iconEmoji = "🏡",
            grammarTopicDari = "توصیف با صفت‌ها و ساختار: Das Zimmer ist...",
            grammarExplanationDari = "توصیف آپارتمان: Die Wohnung ist groß und hell (آپارتمان بزرگ و روشن است).\nاستفاده از es gibt (وجود دارد):\nEs gibt eine Küche und ein Bad. (یک آشپزخانه و یک حمام وجود دارد)",
            keyVerbs = listOf("wohnen", "mieten", "liegen")
        )
    )

    val dialogues: List<Dialogue> = listOf(
        Dialogue(
            id = "d1",
            lessonId = 1,
            titleDari = "احوال‌پرسی و معرفی خود",
            titleGerman = "Begrüßung & Kennenlernen",
            contextDari = "دو نفر در صنف زبان آلمانی با یکدیگر آشنا می‌شوند.",
            lines = listOf(
                DialogueLine("سارا", true, "Guten Tag! Wie heißen Sie?", "گوتِن تاگ! وی هایسِن زی؟", "روز بخیر! نام شما چیست؟"),
                DialogueLine("احمد", false, "Guten Tag! Ich heiße Ahmad. Und wer sind Sie?", "گوتِن تاگ! ایش هایسه احمد. اونت وِر زینت زی؟", "روز بخیر! نام من احمد است. و شما کیستید؟"),
                DialogueLine("سارا", true, "Ich bin Sarah. Woher kommen Sie, Herr Ahmad?", "ایش بین سارا. ووهِر کومِن زی، هِر احمد؟", "من سارا هستم. شما از کجا می‌آیید (اهل کجایید)، آقای احمد؟"),
                DialogueLine("احمد", false, "Ich komme aus Afghanistan, aus Kabul. Und Sie?", "ایش کومه آوس افغانستان، آوس کابل. اونت زی؟", "من از افغانستان هستم، از کابل. و شما؟"),
                DialogueLine("سارا", true, "Ich komme aus Deutschland. Wo wohnen Sie jetzt?", "ایش کومه آوس دویچلند. وو وونِن زی یِتست؟", "من اهل آلمان هستم. اکنون کجا زندگی می‌کنید؟"),
                DialogueLine("احمد", false, "Ich wohne jetzt in Berlin. Freut mich!", "ایش وونه یِتست این برلین. فرویت میش!", "من اکنون در برلین زندگی می‌کنم. از آشنایی‌تان خوشحالم!"),
                DialogueLine("سارا", true, "Sehr angenehm! Willkommen in Deutschland.", "زر آن‌گِنِم! ویلکومِن این دویچلند.", "خیلی خوشوقتم! به آلمان خوش آمدید.")
            )
        ),
        Dialogue(
            id = "d2",
            lessonId = 2,
            titleDari = "تبادل شماره تماس و ایمیل",
            titleGerman = "Kontaktdaten austauschen",
            contextDari = "احمد و هم‌کلاسی‌اش شماره تلفن و ایمیل یکدیگر را یادداشت می‌کنند.",
            lines = listOf(
                DialogueLine("لوکاس", true, "Ahmad, hast du eine Handynummer?", "احمد، هاست دو آینه هندی‌نومِر؟", "احمد، شماره تلفن همراه داری؟"),
                DialogueLine("احمد", false, "Ja, natürlich. Meine Nummer ist 0176 45 89 23.", "یا، ناتورلیش. ماینه نومِر ایست نول آینس زیبِن زِکس فیر فونس اخت نوین تسوای درای.", "بله، البته. شماره من ۰۱۷۶۴۵۸۹۲۳ است."),
                DialogueLine("لوکاس", true, "Und wie ist deine E-Mail-Adresse?", "اونت وی ایست داینه ایمیل آدرِسه؟", "و آدرس ایمیل تو چیست؟"),
                DialogueLine("احمد", false, "ahmad.kabul@email.de. ahmad Punkt kabul ät email Punkt de.", "احمد پونکت کابل اَت ایمیل پونکت دِ اِ.", "ahmad.kabul@email.de (احمد نقطه کابل اَت ایمیل دات دِ اِ)."),
                DialogueLine("لوکاس", true, "Danke! Ich schreibe dir auf WhatsApp.", "دانکه! ایش شرایبه دیر آوف واتس‌اپ.", "تشکر! در واتس‌اپ به تو پیام می‌دهم."),
                DialogueLine("احمد", false, "Super, bis morgen!", "زوپِر، بیس مورگِن!", "عالی است، تا فردا!")
            )
        ),
        Dialogue(
            id = "d3",
            lessonId = 4,
            titleDari = "سفارش در کافه تریا",
            titleGerman = "Bestellung im Café",
            contextDari = "مشتری در یک کافه آلمانی نوشیدنی و کیک سفارش می‌دهد.",
            lines = listOf(
                DialogueLine("پیشخدمت", true, "Guten Tag! Was möchten Sie bestellen?", "گوتِن تاگ! واس مِشتِن زی بشتِلِن؟", "روز بخیر! چه چیزی میل دارید سفارش دهید؟"),
                DialogueLine("مشتری", false, "Guten Tag! Einen Kaffee mit Milch, bitte.", "گوتِن تاگ! آینِن کافه میت میلش، بیته.", "روز بخیر! یک قهوه با شیر، لطفاً."),
                DialogueLine("پیشخدمت", true, "Möchten Sie auch etwas essen?", "مِشتِن زی آوخ اِتواس اِسِن؟", "آیا چیزی برای خوردن هم میل دارید؟"),
                DialogueLine("مشتری", false, "Ja, ein Stück Apfelkuchen, bitte.", "یا، آین شتوک آپفل‌کوخِن، بیته.", "بله، یک توته کیک سیب، لطفاً."),
                DialogueLine("پیشخدمت", true, "Sehr gern. Kommt sofort!", "زر گِرن. کومت زوفورت!", "با کمال میل. فوراً می‌آورم!"),
                DialogueLine("مشتری", false, "Entschuldigung, zahlen bitte! Zusammen.", "اِنت‌شولدینگونگ، تسالِن بیته! تسوزامِن.", "ببخشید، حساب لطفاً! یکجا."),
                DialogueLine("پیشخدمت", true, "Das macht zusammen 5 Euro 80.", "داس ماخت تسوزامِن فونس اویو اختسیش.", "مجموعاً می‌شود ۵ یورو و ۸۰ سنت.")
            )
        ),
        Dialogue(
            id = "d4",
            lessonId = 7,
            titleDari = "معرفی خانواده به دوست",
            titleGerman = "Die Familie vorstellen",
            contextDari = "مریم آلبوم عکس خانوادگی‌اش را به دوست آلمانی‌اش نشان می‌دهد.",
            lines = listOf(
                DialogueLine("آنا", true, "Maryam, wer ist das auf dem Foto?", "مریم، وِر ایست داس آوف دِم فوتو؟", "مریم، این شخص در عکس کیست؟"),
                DialogueLine("مریم", false, "Das ist meine Mutter und das ist mein Vater.", "داس ایست ماینه موتر اونت داس ایست ماین فاتر.", "این مادرم است و این پدرم."),
                DialogueLine("آنا", true, "Hast du auch Geschwister?", "هاست دو آوخ گِشویشتِر؟", "آیا خواهر و برادر هم داری؟"),
                DialogueLine("مریم", false, "Ja, ich habe einen Bruder und zwei Schwestern.", "یا، ایش هابه آینِن برودر اونت تسوای شوِستِرن.", "بله، من یک برادر و دو خواهر دارم."),
                DialogueLine("آنا", true, "Wie alt sind deine Schwestern?", "وی آلت زینت داینه شوِستِرن؟", "خواهرهایت چند ساله هستند؟"),
                DialogueLine("مریم", false, "Sie sind zehn und vierzehn Jahre alt.", "زی زینت تسن اونت فیرتسن یاره آلت.", "آن‌ها ۱۰ و ۱۴ ساله هستند.")
            )
        ),
        Dialogue(
            id = "d5",
            lessonId = 8,
            titleDari = "دیدن آپارتمان برای کرایه",
            titleGerman = "Wohnungsbesichtigung",
            contextDari = "مستأجر در حال بازدید از یک خانه و پرسیدن شرایط آن است.",
            lines = listOf(
                DialogueLine("صاحب‌خانه", true, "Hier ist die Wohnung. Treten Sie ein!", "هیر ایست دی وونونگ. ترِتِن زی آین!", "اینجا آپارتمان است. بفرمایید داخل!"),
                DialogueLine("مستأجر", false, "Das Wohnzimmer ist sehr groß und hell!", "داس وون‌تسیمِر ایست زر گروس اونت هِل!", "اتاق نشیمن بسیار کلان (بزرگ) و روشن است!"),
                DialogueLine("صاحب‌خانه", true, "Ja, und hier sind die Küche und das Bad mit Dusche.", "یا، اونت هیر زینت دی کوشه اونت داس باد میت دوشه.", "بله، و اینجا آشپزخانه و حمام با دوش قرار دارند."),
                DialogueLine("مستأجر", false, "Gibt es auch einen Balkon?", "گیبت اِس آوخ آینِن بالکون؟", "آیا بالکن هم دارد؟"),
                DialogueLine("صاحب‌خانه", true, "Ja, im Schlafzimmer gibt es einen kleinen Balkon.", "یا، ایم شلاف‌تسیمِر گیبت اِس آینِن کلاینِن بالکون.", "بله، در اتاق خواب یک بالکن کوچک وجود دارد."),
                DialogueLine("مستأجر", false, "Wie hoch ist die Miete pro Monat?", "وی هوخ ایست دی میته پرو مونات؟", "کرایه در هر ماه چقدر است؟"),
                DialogueLine("صاحب‌خانه", true, "Die Warmmiete ist 650 Euro inklusive Nebenkosten.", "دی وارم‌میته ایست زکس‌هوندِرت فونسیش اویو اینکلوزیو نِبِن‌کوستِن.", "کرایه گرم ۶۵۰ یورو با مصرف برق و گرمایش است.")
            )
        )
    )

    fun getNumbersList(): List<GermanNumber> {
        val basic = listOf(
            GermanNumber(0, "null", "نول", "۰"),
            GermanNumber(1, "eins", "آینس", "۱"),
            GermanNumber(2, "zwei", "تسوای", "۲"),
            GermanNumber(3, "drei", "درای", "۳"),
            GermanNumber(4, "vier", "فیر", "۴"),
            GermanNumber(5, "fünf", "فونس", "۵"),
            GermanNumber(6, "sechs", "زِکس", "۶"),
            GermanNumber(7, "sieben", "زیبِن", "۷"),
            GermanNumber(8, "acht", "آخت", "۸"),
            GermanNumber(9, "neun", "نوین", "۹"),
            GermanNumber(10, "zehn", "تسن", "۱۰"),
            GermanNumber(11, "elf", "اِلف", "۱۱"),
            GermanNumber(12, "zwölf", "تسوِلف", "۱۲"),
            GermanNumber(13, "dreizehn", "درای‌تسن", "۱۳"),
            GermanNumber(14, "vierzehn", "فیر‌تسن", "۱۴"),
            GermanNumber(15, "fünfzehn", "فونس‌تسن", "۱۵"),
            GermanNumber(16, "sechzehn", "زِش‌تسن", "۱۶"),
            GermanNumber(17, "siebzehn", "زیب‌تسن", "۱۷"),
            GermanNumber(18, "achtzehn", "آخت‌تسن", "۱۸"),
            GermanNumber(19, "neunzehn", "نوین‌تسن", "۱۹"),
            GermanNumber(20, "zwanzig", "تسوانتسیش", "۲۰"),
            GermanNumber(21, "einundzwanzig", "آین‌اونت‌تسوانتسیش", "۲۱"),
            GermanNumber(22, "zweiundzwanzig", "تسوای‌اونت‌تسوانتسیش", "۲۲"),
            GermanNumber(25, "fünfundzwanzig", "فونس‌اونت‌تسوانتسیش", "۲۵"),
            GermanNumber(30, "dreißig", "درای‌سیش", "۳۰"),
            GermanNumber(35, "fünfunddreißig", "فونس‌اونت‌درای‌سیش", "۳۵"),
            GermanNumber(40, "vierzig", "فیرتسیش", "۴۰"),
            GermanNumber(50, "fünfzig", "فونس‌تسیش", "۵۰"),
            GermanNumber(60, "sechzig", "زِش‌تسیش", "۶۰"),
            GermanNumber(70, "siebzig", "زیب‌تسیش", "۷۰"),
            GermanNumber(80, "achtzig", "آخت‌تسیش", "۸۰"),
            GermanNumber(90, "neunzig", "نوین‌تسیش", "۹۰"),
            GermanNumber(100, "hundert", "هوندِرت", "۱۰۰")
        )
        return basic
    }

    fun numberToGerman(n: Int): String {
        val units = arrayOf("null", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn",
            "elf", "zwölf", "dreizehn", "vierzehn", "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn")
        val tens = arrayOf("", "", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig")
        
        return when {
            n < 20 -> units[n]
            n == 100 -> "hundert (oder einhundert)"
            n % 10 == 0 -> tens[n / 10]
            else -> {
                val u = n % 10
                val t = n / 10
                val unitWord = if (u == 1) "ein" else units[u]
                "${unitWord}und${tens[t]}"
            }
        }
    }
}

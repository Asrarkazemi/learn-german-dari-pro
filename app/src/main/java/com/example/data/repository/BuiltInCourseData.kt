package com.example.data.repository

import com.example.data.model.DialogueItem
import com.example.data.model.DialogueLineItem
import com.example.data.model.ExampleSentence
import com.example.data.model.ExerciseItem
import com.example.data.model.GrammarSection
import com.example.data.model.LessonData
import com.example.data.model.QaPair
import com.example.data.model.VocabularyItem

object BuiltInCourseData {

    val lessons: List<LessonData> = listOf(
        // ================= LESSON 1 =================
        LessonData(
            id = "lesson_1",
            number = 1,
            titleGerman = "Begrüßung & Vorstellen",
            titleDari = "درس ۱: سلام و معرفی خود",
            vocabulary = listOf(
                VocabularyItem("", "Guten Tag", "گوتِن تاگ", "روز بخیر (سلام رسمی)"),
                VocabularyItem("", "Hallo", "هالو", "سلام (صمیمانه)"),
                VocabularyItem("", "Guten Morgen", "گوتِن مورگِن", "صبح بخیر"),
                VocabularyItem("", "Guten Abend", "گوتِن آبِند", "شام بخیر / عصر بخیر"),
                VocabularyItem("", "Gute Nacht", "گوته ناخت", "شب بخیر"),
                VocabularyItem("", "Auf Wiedersehen", "آوف ویدِرزین", "به امید دیدار / خداحافظ (رسمی)"),
                VocabularyItem("", "Tschüss", "چوس", "خداحافظ (دوستانه)"),
                VocabularyItem("", "heißen", "هایسِن", "نامیده شدن / نام داشتن"),
                VocabularyItem("", "sein", "زاین", "بودن (هست)"),
                VocabularyItem("", "kommen", "کومِن", "آمدن / اهل جایی بودن"),
                VocabularyItem("", "wohnen", "وونِن", "زندگی کردن / سکونت داشتن"),
                VocabularyItem("das", "Deutschland", "دویچلَند", "کشور آلمان"),
                VocabularyItem("das", "Afghanistan", "افغانستان", "کشور افغانستان"),
                VocabularyItem("die", "Schweiz", "دی شوایتس", "کشور سوئیس"),
                VocabularyItem("das", "Österreich", "اوستِرایش", "کشور اتریش"),
                VocabularyItem("", "Danke", "دانکه", "تشکر / ممنون"),
                VocabularyItem("", "Bitte", "بیته", "لطفاً / خواهش می‌کنم")
            ),
            exampleSentences = listOf(
                ExampleSentence("Guten Tag, wie geht es Ihnen?", "گوتِن تاگ، وی گِیت اِس اینِن؟", "روز بخیر، حال شما چطور است؟"),
                ExampleSentence("Ich heiße Ahmad und komme aus Kabul.", "ایش هایسه احمد اونت کومه آوس کابل.", "نام من احمد است و از کابل می‌آیم."),
                ExampleSentence("Ich wohne jetzt in Berlin.", "ایش وونه یِتست این برلین.", "من اکنون در برلین زندگی می‌کنم.")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "fill-blank",
                    question = "Ich ___ aus Afghanistan.",
                    pronunciation = "ایش کومه آوس افغانستان.",
                    translationDari = "من از افغانستان می‌آیم (اهل افغانستان هستم).",
                    options = listOf("komme", "kommst", "kommt", "kommen"),
                    correctAnswer = "komme",
                    explanationDari = "برای فاعل Ich (من)، فعل kommen به پسوند -e ختم می‌شود: Ich komme."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Wie ___ du?",
                    pronunciation = "وی هایست دو؟",
                    translationDari = "نام تو چیست؟",
                    options = listOf("heißt", "heiße", "heißen", "heiß"),
                    correctAnswer = "heißt",
                    explanationDari = "برای ضمیر du، فعل heißen شکل heißt به خود می‌گیرد."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Er ___ Ahmad.",
                    pronunciation = "اِر ایست احمد.",
                    translationDari = "او احمد است.",
                    options = listOf("ist", "bin", "bist", "sind"),
                    correctAnswer = "ist",
                    explanationDari = "برای سوم شخص مفرد (Er)، شکل صحیح فعل sein واژه ist است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Auf Wiedersehen!",
                    pronunciation = "آوف ویدِرزین!",
                    translationDari = "به امید دیدار / خداحافظ!",
                    options = listOf("به امید دیدار / خداحافظ", "صبح بخیر", "حال شما چطور است", "تشکر بسیار"),
                    correctAnswer = "به امید دیدار / خداحافظ",
                    explanationDari = "عبارت Auf Wiedersehen یک خداحافظی رسمی به معنای 'به امید دیدار' است."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("سارا", "Guten Tag! Wie heißen Sie?", "گوتِن تاگ! وی هایسِن زی؟", "روز بخیر! نام شما چیست؟"),
                        DialogueLineItem("احمد", "Ich heiße Ahmad. Und wer sind Sie?", "ایش هایسه احمد. اونت وِر زینت زی؟", "نام من احمد است. و شما کیستید؟"),
                        DialogueLineItem("سارا", "Ich bin Sarah. Woher kommen Sie?", "ایش بین سارا. ووهِر کومِن زی؟", "من سارا هستم. شما اهل کجایید؟"),
                        DialogueLineItem("احمد", "Ich komme aus Afghanistan. Und Sie?", "ایش کومه آوس افغانستان. اونت زی؟", "من اهل افغانستان هستم. و شما؟"),
                        DialogueLineItem("سارا", "Ich komme aus Deutschland. Willkommen!", "ایش کومه آوس دویچلند. ویلکومِن!", "من اهل آلمان هستم. خوش آمدید!")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "افعال مهم برای سلام و معرفی (heißen, sein, kommen, wohnen)",
                    bodyDari = "در زبان آلمانی، افعال برای اشخاص مختلف پسوند می‌گیرند:\n\n• فعل heißen (نامیده شدن):\n- Ich heiße... (من نامیده می‌شوم / نام من ... است)\n- Du heißt... (تو نامیده می‌شوی)\n- Er/Sie heißt... (او نامیده می‌شود)\n- Wie heißen Sie? (نام شما چیست؟ - رسمی)\n\n• فعل sein (بودن):\n- Ich bin... (من هستم)\n- Du bist... (تو هستی)\n- Er/Sie ist... (او هست)\n- Wir/Sie sind (ما هستیم / شما هستید)\n\n• فعل kommen (آمدن / اهل جایی بودن):\n- Ich komme aus Afghanistan. (من از افغانستان می‌آیم)\n- Woher kommen Sie? (شما اهل کجا هستید؟)\n\n• فعل wohnen (سکونت داشتن):\n- Ich wohne in Berlin. (من در برلین زندگی می‌کنم)"
                ),
                GrammarSection(
                    title = "احوال‌پرسی رسمی و دوستانه",
                    bodyDari = "• Guten Tag (روز بخیر) و Auf Wiedersehen (به امید دیدار) رسمی هستند.\n• Hallo (سلام) و Tschüss (خداحافظ) دوستانه و صمیمانه هستند."
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Wie heißen Sie?",
                    questionPronunciation = "وی هایسِن زی؟",
                    questionDari = "نام شما چیست؟ (رسمی)",
                    answerGerman = "Ich heiße Ahmad.",
                    answerPronunciation = "ایش هایسه احمد.",
                    answerDari = "نام من احمد است."
                ),
                QaPair(
                    questionGerman = "Woher kommen Sie?",
                    questionPronunciation = "ووهِر کومِن زی؟",
                    questionDari = "شما اهل کجا هستید؟",
                    answerGerman = "Ich komme aus Afghanistan.",
                    answerPronunciation = "ایش کومه آوس افغانستان.",
                    answerDari = "من اهل افغانستان هستم."
                ),
                QaPair(
                    questionGerman = "Wo wohnen Sie jetzt?",
                    questionPronunciation = "وو وونِن زی یِتست؟",
                    questionDari = "اکنون در کجا زندگی می‌کنید؟",
                    answerGerman = "Ich wohne in Berlin.",
                    answerPronunciation = "ایش وونه این برلین.",
                    answerDari = "من در برلین زندگی می‌کنم."
                ),
                QaPair(
                    questionGerman = "Wie geht es Ihnen?",
                    questionPronunciation = "وی گِیت اِس اینِن؟",
                    questionDari = "حال شما چطور است؟",
                    answerGerman = "Danke, sehr gut!",
                    answerPronunciation = "دانکه، زِر گوت!",
                    answerDari = "تشکر، بسیار خوبم!"
                )
            )
        ),

        // ================= LESSON 2 =================
        LessonData(
            id = "lesson_2",
            number = 2,
            titleGerman = "Kontakte & Berufe",
            titleDari = "درس ۲: راه‌های تماس و شغل‌ها",
            vocabulary = listOf(
                VocabularyItem("der", "Lehrer", "لِرِر", "استاد / معلم (مرد)"),
                VocabularyItem("die", "Lehrerin", "لِرِرین", "معلمه / استاد (زن)"),
                VocabularyItem("der", "Arzt", "آرتست", "داکتر / پزشک (مرد)"),
                VocabularyItem("die", "Ärztin", "اِرتستین", "داکتر / پزشک (زن)"),
                VocabularyItem("der", "Ingenieur", "اینجِنیور", "انجنیر / مهندس"),
                VocabularyItem("der", "Student", "شتودِنت", "محصل / دانشجو (پسر)"),
                VocabularyItem("die", "Studentin", "شتودِنتین", "محصله / دانشجو (دختر)"),
                VocabularyItem("die", "Handynummer", "هَندی‌نومِر", "شماره مبایل / تلفن همراه"),
                VocabularyItem("die", "E-Mail-Adresse", "ایمیل آدرِسه", "آدرس ایمیل"),
                VocabularyItem("der", "Punkt", "پونکت", "نقطه (دات .)"),
                VocabularyItem("", "haben", "هابِن", "داشتن"),
                VocabularyItem("", "lernen", "لِرنِن", "یاد گرفتن / آموختن"),
                VocabularyItem("", "sprechen", "شپرِشِن", "صحبت کردن / گپ زدن"),
                VocabularyItem("der", "Beruf", "بِروف", "شغل / مسلک"),
                VocabularyItem("die", "Arbeit", "آربایت", "کار / وظیفه"),
                VocabularyItem("der", "Verkäufer", "فِرکویفِر", "فروشنده / دکاندار")
            ),
            exampleSentences = listOf(
                ExampleSentence("Was bist du von Beruf?", "واس بیست دو فون بِروف؟", "شغل تو چیست؟"),
                ExampleSentence("Ich arbeite als Ingenieur.", "ایش آربایته آلس اینجِنیور.", "من به عنوان انجنیر کار می‌کنم."),
                ExampleSentence("Wie ist deine Handynummer?", "وی ایست داینه هندی‌نومِر؟", "شماره مبایل تو چیست؟")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "fill-blank",
                    question = "___ du Deutsch?",
                    pronunciation = "شپریخست دو دویچ؟",
                    translationDari = "آیا آلمانی صحبت می‌کنی؟",
                    options = listOf("Sprichst", "Spreche", "Sprecht", "Sprechen"),
                    correctAnswer = "Sprichst",
                    explanationDari = "فعل sprechen نامنظم است: برای du تبدیل به Sprichst می‌شود."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Wir ___ eine Handynummer.",
                    pronunciation = "ویر هابن آینه هندی‌نومِر.",
                    translationDari = "ما یک شماره مبایل داریم.",
                    options = listOf("haben", "habe", "hast", "hat"),
                    correctAnswer = "haben",
                    explanationDari = "برای فاعل wir (ما)، فعل haben به صورت مصدری صرف می‌شود."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Sie ist Ärztin von Beruf.",
                    pronunciation = "زی ایست اِرتستین فون بِروف.",
                    translationDari = "او شغلش داکتری (پزشکی زن) است.",
                    options = listOf("او داکتر زن است", "او معلم است", "او دانشجو است", "او مهندس است"),
                    correctAnswer = "او داکتر زن است",
                    explanationDari = "واژه Ärztin مؤنث پزشک (داکتر زن) است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "معادل 'دانشجو / محصل آقا' در آلمانی چیست؟",
                    pronunciation = "دِر شتودِنت",
                    translationDari = "محصل آقا",
                    options = listOf("der Student", "die Studentin", "der Lehrer", "die Arbeit"),
                    correctAnswer = "der Student",
                    explanationDari = "کلمه der Student برای دانشجوی مرد و die Studentin برای دانشجوی زن است."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("لوکاس", "Hast du eine Handynummer?", "هاست دو آینه هندی‌نومِر؟", "آیا شماره تلفن همراه داری؟"),
                        DialogueLineItem("احمد", "Ja, meine Nummer ist 0176 45 89.", "یا، ماینه نومِر ایست نول آینس زیبِن زِکس...", "بله، شماره من ۰۱۷۶۴۵۸۹ است."),
                        DialogueLineItem("لوکاس", "Und wie ist deine E-Mail?", "اونت وی ایست داینه ایمیل؟", "و ایمیل تو چیست؟"),
                        DialogueLineItem("احمد", "ahmad Punkt kabul ät email Punkt de.", "احمد پونکت کابل اَت ایمیل پونکت دِ اِ.", "ahmad.kabul@email.de")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "افعال haben, lernen و فعل نامنظم sprechen",
                    bodyDari = "• فعل haben (داشتن):\n- Ich habe (من دارم)\n- Du hast (تو داری)\n- Er/Sie hat (او دارد)\n- Wir haben (ما داریم)\n\n• فعل نامنظم sprechen (صحبت کردن / گپ زدن):\nدر این فعل حرف e به i تبدیل می‌شود:\n- Ich spreche (من گپ می‌زنم)\n- Du sprichst (تو گپ می‌زنی)\n- Er/Sie spricht (او گپ می‌زند)\n- Wir sprechen (ما گپ می‌زنیم)"
                ),
                GrammarSection(
                    title = "مؤنث کردن شغل‌ها با پسوند in-",
                    bodyDari = "در آلمانی برای شغل خانم‌ها، معمولاً پسوند in- اضافه می‌شود:\n• der Lehrer (معلم آقا) ← die Lehrerin (معلم خانم)\n• der Arzt (داکتر آقا) ← die Ärztin (داکتر خانم)\n• der Student (محصل آقا) ← die Studentin (محصله خانم)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Was sind Sie von Beruf?",
                    questionPronunciation = "واس زینت زی فون بِروف؟",
                    questionDari = "شغل شما چیست؟",
                    answerGerman = "Ich bin Lehrer von Beruf.",
                    answerPronunciation = "ایش بین لِرِر فون بِروف.",
                    answerDari = "شغل من معلمی است."
                ),
                QaPair(
                    questionGerman = "Welche Sprachen sprichst du?",
                    questionPronunciation = "وِلخه شپراخِن شپریخست دو؟",
                    questionDari = "به چه زبان‌هایی صحبت می‌کنی؟",
                    answerGerman = "Ich spreche Dari und lerne Deutsch.",
                    answerPronunciation = "ایش شپرِخه دری اونت لِرنه دویچ.",
                    answerDari = "من دری گپ می‌زنم و آلمانی یاد می‌گیرم."
                ),
                QaPair(
                    questionGerman = "Wie ist deine Handynummer?",
                    questionPronunciation = "وی ایست داینه هندی‌نومِر؟",
                    questionDari = "شماره مبایل تو چیست؟",
                    answerGerman = "Meine Handynummer ist 0176 12345.",
                    answerPronunciation = "ماینه هندی‌نومِر ایست نول آینس زیبِن...",
                    answerDari = "شماره من ۰۱۷۶۱۲۳۴۵ است."
                )
            )
        ),

        // ================= LESSON 3 =================
        LessonData(
            id = "lesson_3",
            number = 3,
            titleGerman = "Gegenstände im Alltag",
            titleDari = "درس ۳: اشیا و وسایل روزمره",
            vocabulary = listOf(
                VocabularyItem("der", "Tisch", "تیش", "میز (مذکر: der)"),
                VocabularyItem("der", "Stuhl", "شتول", "چوکی / صندلی (مذکر: der)"),
                VocabularyItem("der", "Stift", "شتیفت", "قلم / خودکار (مذکر: der)"),
                VocabularyItem("der", "Computer", "کامپیوتر", "کمپیوتر (مذکر: der)"),
                VocabularyItem("der", "Schlüssel", "شلوُسل", "کلید (مذکر: der)"),
                VocabularyItem("das", "Buch", "بوخ", "کتاب (خنثی: das)"),
                VocabularyItem("das", "Handy", "هَندی", "تلفن مبایل (خنثی: das)"),
                VocabularyItem("das", "Heft", "هِفت", "کتابچه / دفترچه (خنثی: das)"),
                VocabularyItem("das", "Bild", "بیلد", "عکس / تصویر (خنثی: das)"),
                VocabularyItem("die", "Tasche", "تاشه", "بکس / کیف (مؤنث: die)"),
                VocabularyItem("die", "Brille", "بریله", "عینک (مؤنث: die)"),
                VocabularyItem("die", "Flasche", "فلاشه", "بوتل آب (مؤنث: die)"),
                VocabularyItem("die", "Uhr", "اوور", "ساعت (مؤنث: die)"),
                VocabularyItem("die", "Lampe", "لامپه", "چراغ (مؤنث: die)"),
                VocabularyItem("das", "Wörterbuch", "وورتِربوخ", "دیکشنری / فرهنگ لغت"),
                VocabularyItem("das", "Papier", "پاپیِر", "کاغذ")
            ),
            exampleSentences = listOf(
                ExampleSentence("Wie heißt das auf Deutsch?", "وی هایست داس آوف دویچ؟", "این به آلمانی چه نام دارد؟"),
                ExampleSentence("Das ist ein Buch und das ist eine Tasche.", "داس ایست آین بوخ اونت داس ایست آینه تاشه.", "این یک کتاب است و این یک بکس است."),
                ExampleSentence("Das ist kein Stift, das ist ein Schlüssel.", "داس ایست کاین شتیفت، داس ایست آین شلوسل.", "این قلم نیست، این یک کلید است.")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "multiple-choice",
                    question = "___ Tisch ist sehr groß.",
                    pronunciation = "دِر تیش ایست زر گروس.",
                    translationDari = "میز بسیار بزرگ است.",
                    options = listOf("Der", "Die", "Das", "Ein"),
                    correctAnswer = "Der",
                    explanationDari = "واژه Tisch مذکر است و با حرف تعریف Der می‌آید."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "___ Buch ist neu.",
                    pronunciation = "داس بوخ ایست نوی.",
                    translationDari = "کتاب نو است.",
                    options = listOf("Das", "Der", "Die", "Den"),
                    correctAnswer = "Das",
                    explanationDari = "واژه Buch خنثی است و حرف تعریف آن Das است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Wo ist ___ Tasche?",
                    pronunciation = "وو ایست دی تاشه؟",
                    translationDari = "بکس (کیف) کجاست؟",
                    options = listOf("die", "der", "das", "ein"),
                    correctAnswer = "die",
                    explanationDari = "واژه Tasche مؤنث است و با حرف تعریف die می‌آید."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Wie heißt das auf Deutsch?",
                    pronunciation = "وی هایست داس آوف دویچ؟",
                    translationDari = "این به آلمانی چه نامیده می‌شود؟",
                    options = listOf("این به آلمانی چه نام دارد؟", "قیمت این چقدر است؟", "کجا زندگی می‌کنید؟", "ساعت چند است؟"),
                    correctAnswer = "این به آلمانی چه نام دارد؟",
                    explanationDari = "این جمله برای پرسیدن نام اشیا در آلمانی است."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("شاگرد", "Entschuldigung, wie heißt das auf Deutsch?", "اِنت‌شولدینگونگ، وی هایست داس آوف دویچ؟", "ببخشید، این به آلمانی چه نام دارد؟"),
                        DialogueLineItem("معلم", "Das ist ein Wörterbuch.", "داس ایست آین وورتِربوخ.", "این یک دیکشنری است."),
                        DialogueLineItem("شاگرد", "Und wie schreibt man das?", "اونت وی شرایبت مان داس؟", "و چطور نوشته می‌شود؟"),
                        DialogueLineItem("معلم", "W-Ö-R-T-E-R-B-U-C-H.", "و-او-ار-ت-ا-ار-ب-او-ث-ها.", "W-Ö-R-T-E-R-B-U-C-H")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "حروف تعریف معین (der, die, das)",
                    bodyDari = "در زبان آلمانی هر اسم یک جنسیت و حرف تعریف مشخص دارد:\n• der (آبی): مذکر مانند der Tisch (میز)، der Stuhl (چوکی)\n• die (سرخ): مؤنث مانند die Tasche (بکس)، die Brille (عینک)\n• das (سبز): خنثی مانند das Buch (کتاب)، das Handy (مبایل)\n• die (جمع): برای حالت جمع تمام کلمات از die استفاده می‌شود."
                ),
                GrammarSection(
                    title = "حروف تعریف نامعین (ein / eine) و منفی (kein / keine)",
                    bodyDari = "• برای مذکر و خنثی: ein (مثبت) و kein (منفی):\n- Das ist ein Stift. (این یک قلم است)\n- Das ist kein Stift. (این قلم نیست)\n\n• برای مؤنث: eine (مثبت) و keine (منفی):\n- Das ist eine Lampe. (این یک چراغ است)\n- Das ist keine Lampe. (این چراغ نیست)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Wie heißt das auf Deutsch?",
                    questionPronunciation = "وی هایست داس آوف دویچ؟",
                    questionDari = "این به آلمانی چه نام دارد؟",
                    answerGerman = "Das ist ein Schlüssel.",
                    answerPronunciation = "داس ایست آین شلوسل.",
                    answerDari = "این یک کلید است."
                ),
                QaPair(
                    questionGerman = "Ist das ein Computer?",
                    questionPronunciation = "ایست داس آین کامپیوتر؟",
                    questionDari = "آیا این یک کمپیوتر است؟",
                    answerGerman = "Nein, das ist kein Computer.",
                    answerPronunciation = "ناین، داس ایست کاین کامپیوتر.",
                    answerDari = "خیر، این کمپیوتر نیست."
                )
            )
        ),

        // ================= LESSON 4 =================
        LessonData(
            id = "lesson_4",
            number = 4,
            titleGerman = "Im Café & Bestellen",
            titleDari = "درس ۴: در کافه و سفارش دادن",
            vocabulary = listOf(
                VocabularyItem("der", "Kaffee", "کافه", "قهوه (مذکر: der)"),
                VocabularyItem("der", "Tee", "تِه", "چای (مذکر: der)"),
                VocabularyItem("der", "Saft", "زافت", "آب‌میوه (مذکر: der)"),
                VocabularyItem("das", "Wasser", "واسِر", "آب (خنثی: das)"),
                VocabularyItem("das", "Brötchen", "بروتشِن", "نان ساندویچی کوچک"),
                VocabularyItem("der", "Kuchen", "کوخِن", "کیک (مذکر: der)"),
                VocabularyItem("die", "Rechnung", "رِشنونگ", "صورتحساب / بل کافه"),
                VocabularyItem("der", "Zucker", "تسوکِر", "شکر / بوره"),
                VocabularyItem("die", "Milch", "میلش", "شیر"),
                VocabularyItem("", "möchten", "مِشتِن", "میل داشتن / خواستن"),
                VocabularyItem("", "bezahlen", "بِتسالِن", "پرداخت کردن"),
                VocabularyItem("", "kosten", "کوستِن", "قیمت داشتن"),
                VocabularyItem("", "zusammen", "تسوزامِن", "با هم / یکجا"),
                VocabularyItem("", "getrennt", "گِترِنت", "جداگانه / علیحده"),
                VocabularyItem("der", "Euro", "اویو", "یورو"),
                VocabularyItem("der", "Cent", "سِنت", "سنت")
            ),
            exampleSentences = listOf(
                ExampleSentence("Einen Kaffee mit Milch, bitte.", "آینِن کافه میت میلش، بیته.", "یک قهوه با شیر، لطفاً."),
                ExampleSentence("Was möchten Sie trinken?", "واس مِشتِن زی ترینکِن؟", "چه چیزی میل دارید بنوشید؟"),
                ExampleSentence("Zusammen oder getrennt?", "تسوزامِن اودِر گِترِنت؟", "یکجا حساب می‌کنید یا جداگانه؟")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Einen Kaffee, bitte!",
                    pronunciation = "آینِن کافه، بیته!",
                    translationDari = "یک قهوه، لطفاً!",
                    options = listOf("یک قهوه، لطفاً!", "یک گیلاس چای، لطفاً!", "یک بوتل آب، لطفاً!", "حساب، لطفاً!"),
                    correctAnswer = "یک قهوه، لطفاً!",
                    explanationDari = "کلمه der Kaffee در حالت مفعولی اکوزاتیو تبدیل به einen Kaffee می‌شود."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Zusammen oder getrennt?",
                    pronunciation = "تسوزامِن اودِر گِترِنت؟",
                    translationDari = "با هم یا جداگانه؟",
                    options = listOf("با هم یا جداگانه؟", "چای یا قهوه؟", "شکر یا شیر؟", "نقد یا کارت؟"),
                    correctAnswer = "با هم یا جداگانه؟",
                    explanationDari = "واژه zusammen یعنی با هم/یکجا، و getrennt یعنی جداگانه."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Was ___ Sie trinken?",
                    pronunciation = "واس مِشتِن زی ترینکِن؟",
                    translationDari = "چه چیزی میل دارید بنوشید؟",
                    options = listOf("möchten", "möchte", "möchtest", "möchtet"),
                    correctAnswer = "möchten",
                    explanationDari = "برای ضمیر محترمانه Sie، فعل به صورت möchten صرف می‌شود."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Wir möchten bitte ___.",
                    pronunciation = "ویر مِشتِن بیته بِتسالِن.",
                    translationDari = "ما می‌خواهیم حساب را پرداخت کنیم.",
                    options = listOf("bezahlen", "bezahle", "bezahlt", "trinken"),
                    correctAnswer = "bezahlen",
                    explanationDari = "فعل bezahlen به معنای پرداخت کردن صورتحساب است."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("پیشخدمت", "Guten Tag! Was möchten Sie?", "گوتِن تاگ! واس مِشتِن زی؟", "روز بخیر! چه میل دارید؟"),
                        DialogueLineItem("مشتری", "Einen Kaffee und ein Wasser, bitte.", "آینِن کافه اونت آین واسر، بیته.", "یک قهوه و یک آب، لطفاً."),
                        DialogueLineItem("مشتری", "Zahlen, bitte!", "تسالِن، بیته!", "حساب، لطفاً!"),
                        DialogueLineItem("پیشخدمت", "Das macht zusammen 4 Euro 50.", "داس ماخت تسوزامِن فیر اویو فونسیش.", "مجموعاً می‌شود ۴ یورو و ۵۰ سنت.")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "حالت مفعولی (Akkusativ) در سفارش دادن",
                    bodyDari = "هنگام سفارش غذا و نوشیدنی با فعل möchten (خواستن / میل داشتن):\n• اسم مذکر der تبدیل به den یا einen می‌شود:\n- Ich möchte einen Kaffee. (من یک قهوه می‌خواهم)\n• اسم خنثی و مؤنث بدون تغییر می‌مانند:\n- ein Wasser (یک آب)\n- eine Cola (یک نوشابه)"
                ),
                GrammarSection(
                    title = "پرداخت و صورت‌حساب در کافه",
                    bodyDari = "• Zahlen, bitte! (حساب لطفاً!)\n• Zusammen oder getrennt? (یکجا پرداخت می‌کنید یا جداگانه؟)\n• Stimmt so! (باقی پول را نگه دارید / انعام)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Was möchten Sie trinken?",
                    questionPronunciation = "واس موشتِن زی ترینکِن؟",
                    questionDari = "چه میل دارید بنوشید؟",
                    answerGerman = "Einen Kaffee mit Milch, bitte.",
                    answerPronunciation = "آینِن کافه میت میلش، بیته.",
                    answerDari = "یک قهوه با شیر، لطفاً."
                ),
                QaPair(
                    questionGerman = "Zusammen oder getrennt?",
                    questionPronunciation = "تسوزامِن اودِر گِترِنت؟",
                    questionDari = "یکجا حساب می‌کنید یا جدا؟",
                    answerGerman = "Getrennt, bitte.",
                    answerPronunciation = "گِترِنت، بیته.",
                    answerDari = "جداگانه، لطفاً."
                )
            )
        ),

        // ================= LESSON 5 =================
        LessonData(
            id = "lesson_5",
            number = 5,
            titleGerman = "Wochentage & Tagesablauf",
            titleDari = "درس ۵: روزهای هفته و کارهای روزمره",
            vocabulary = listOf(
                VocabularyItem("der", "Montag", "مونتاگ", "روز دوشنبه"),
                VocabularyItem("der", "Dienstag", "دینستاگ", "روز سه‌شنبه"),
                VocabularyItem("der", "Mittwoch", "میت‌وُوخ", "روز چهارشنبه"),
                VocabularyItem("der", "Donnerstag", "دونِرستاگ", "روز پنج‌شنبه"),
                VocabularyItem("der", "Freitag", "فرایتاگ", "روز جمعه"),
                VocabularyItem("der", "Samstag", "زامستاگ", "روز شنبه"),
                VocabularyItem("der", "Sonntag", "زونتاگ", "روز یکشنبه (تعطیلی)"),
                VocabularyItem("die", "Woche", "ووخه", "هفته"),
                VocabularyItem("das", "Wochenende", "ووخِن‌اِنده", "آخر هفته"),
                VocabularyItem("der", "Morgen", "مورگِن", "صبح"),
                VocabularyItem("der", "Abend", "آبِند", "شام / شب"),
                VocabularyItem("", "aufstehen", "آوف‌شتین", "از خواب برخاستن"),
                VocabularyItem("", "frühstücken", "فروشتوکِن", "صبحانه خوردن"),
                VocabularyItem("", "arbeiten", "آربایتِن", "کار کردن"),
                VocabularyItem("", "einkaufen", "آین‌کویفِن", "خرید کردن"),
                VocabularyItem("", "schlafen", "شلافِن", "خوابیدن")
            ),
            exampleSentences = listOf(
                ExampleSentence("Am Montag arbeite ich von 8 bis 16 Uhr.", "آم مونتاگ آربایته ایش فون اخت بیس زِش‌تسن اوور.", "دوشنبه از ساعت ۸ تا ۱۶ کار می‌کنم."),
                ExampleSentence("Ich stehe um 6 Uhr auf.", "ایش شتیه اوم زِکس اوور آوف.", "من ساعت ۶ صبح بیدار می‌شوم."),
                ExampleSentence("Am Wochenende lerne ich Deutsch.", "آم ووخِن‌اِنده لِرنه ایش دویچ.", "آخر هفته آلمانی یاد می‌گیرم.")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "fill-blank",
                    question = "Ich stehe um 7 Uhr ___.",
                    pronunciation = "ایش شتیه اوم زیبِن اوور آوف.",
                    translationDari = "من ساعت ۷ بیدار می‌شوم.",
                    options = listOf("auf", "an", "aus", "ein"),
                    correctAnswer = "auf",
                    explanationDari = "فعل aufstehen جداشدنی است و پیشوند auf به آخر جمله می‌رود."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Am Montag habe ich Schule.",
                    pronunciation = "آم مونتاگ هابه ایش شوله.",
                    translationDari = "روز دوشنبه مکتب دارم.",
                    options = listOf("روز دوشنبه مکتب دارم", "روز جمعه کار دارم", "صبح‌ها زود بیدار می‌شوم", "آخر هفته تعطیل است"),
                    correctAnswer = "روز دوشنبه مکتب دارم",
                    explanationDari = "واژه Montag یعنی دوشنبه و با حرف اضافه am می‌آید."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "کدام روز هفته به معنی 'چهارشنبه' است؟",
                    pronunciation = "میت‌وُوخ",
                    translationDari = "چهارشنبه",
                    options = listOf("Mittwoch", "Dienstag", "Donnerstag", "Samstag"),
                    correctAnswer = "Mittwoch",
                    explanationDari = "روز Mittwoch وسط هفته آلمانی یعنی چهارشنبه است."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Wann ___ du am Morgen?",
                    pronunciation = "وان فروشتوکست دو آم مورگن؟",
                    translationDari = "چه زمانی صبحانه می‌خوری؟",
                    options = listOf("frühstückst", "frühstücken", "frühstücke", "frühstückt"),
                    correctAnswer = "frühstückst",
                    explanationDari = "برای ضمیر du، فعل frühstücken پسوند -st می‌گیرد."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("علی", "Wann stehst du am Montag auf?", "وان شتیست دو آم مونتاگ آوف؟", "دوشنبه چه ساعتی بیدار می‌شوی؟"),
                        DialogueLineItem("احمد", "Ich stehe um 6 Uhr auf und frühstücke.", "ایش شتیه اوم زِکس اوور آوف اونت فروشتوکه.", "ساعت ۶ بیدار می‌شوم و ناشتا می‌خورم."),
                        DialogueLineItem("علی", "Und am Wochenende?", "اونت آم ووخِن‌اِنده؟", "و در آخر هفته چطور؟"),
                        DialogueLineItem("احمد", "Am Sonntag schlafe ich bis 9 Uhr.", "آم زونتاگ شلافه ایش بیس نوین اوور.", "یکشنبه تا ساعت ۹ می‌خوابم.")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "افعال جداشدنی (Trennbare Verben)",
                    bodyDari = "در این افعال، پیشوند جدا شده و به انتهای جمله منتقل می‌شود:\n• aufstehen (بیدار شدن):\nIch stehe um 7 Uhr auf.\n• einkaufen (خرید کردن):\nEr kauft am Samstag ein."
                ),
                GrammarSection(
                    title = "حروف اضافه زمان (am و um)",
                    bodyDari = "• برای روزهای هفته از am استفاده می‌شود: am Montag (دوشنبه), am Freitag (جمعه)\n• برای ساعات دقیق از um استفاده می‌شود: um 8 Uhr (در ساعت ۸)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Wann stehst du auf?",
                    questionPronunciation = "وان شتیست دو آوف؟",
                    questionDari = "چه ساعتی بیدار می‌شوی؟",
                    answerGerman = "Ich stehe um 7 Uhr auf.",
                    answerPronunciation = "ایش شتیه اوم زیبِن اوور آوف.",
                    answerDari = "من ساعت ۷ بیدار می‌شوم."
                ),
                QaPair(
                    questionGerman = "Wie spät ist es?",
                    questionPronunciation = "وی شپِیت ایست اِس؟",
                    questionDari = "ساعت چند است؟",
                    answerGerman = "Es ist genau 8 Uhr.",
                    answerPronunciation = "اِس ایست گِناو آخت اوور.",
                    answerDari = "دقیقاً ساعت ۸ است."
                )
            )
        ),

        // ================= LESSON 6 =================
        LessonData(
            id = "lesson_6",
            number = 6,
            titleGerman = "Essen & Geschmack",
            titleDari = "درس ۶: غذاها و طعم‌ها",
            vocabulary = listOf(
                VocabularyItem("das", "Brot", "بروت", "نان (خنثی: das)"),
                VocabularyItem("der", "Apfel", "آپفِل", "سیب (مذکر: der)"),
                VocabularyItem("die", "Banane", "بانانه", "کیله / موز"),
                VocabularyItem("der", "Käse", "کِزه", "پنیر"),
                VocabularyItem("das", "Fleisch", "فلایش", "گوشت"),
                VocabularyItem("das", "Hähnchen", "هِینشِن", "گوشت مرغ"),
                VocabularyItem("der", "Reis", "رایس", "برنج / پلو"),
                VocabularyItem("das", "Gemüse", "گِموزه", "ترکاری / سبزیجات"),
                VocabularyItem("das", "Obst", "اوبست", "میوه‌جات"),
                VocabularyItem("", "lecker", "لِکِر", "مزه‌دار / لذیذ"),
                VocabularyItem("", "süß", "زوس", "شیرین"),
                VocabularyItem("", "salzig", "زالتسیش", "شور / نمکی"),
                VocabularyItem("", "schmecken", "شمِکِن", "طعم و مزه دادن"),
                VocabularyItem("", "essen", "اِسِن", "غذا خوردن"),
                VocabularyItem("", "trinken", "ترینکِن", "نوشیدن"),
                VocabularyItem("", "gern", "گِرن", "با علاقه و میل")
            ),
            exampleSentences = listOf(
                ExampleSentence("Das Essen schmeckt sehr gut!", "داس اِسِن شمِکت زر گوت!", "غذا بسیار مزه‌دار است!"),
                ExampleSentence("Ich esse gern Obst und Gemüse.", "ایش اِسه گِرن اوبست اونت گِموزه.", "من میوه و سبزیجات را با علاقه می‌خورم."),
                ExampleSentence("Dieser Tee ist zu süß.", "دیزِر تِه ایست تسو زوس.", "این چای بیش از حد شیرین است.")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Das Essen schmeckt sehr gut!",
                    pronunciation = "داس اِسِن شمِکت زر گوت!",
                    translationDari = "غذا بسیار مزه‌دار است!",
                    options = listOf("غذا بسیار مزه‌دار است!", "غذا خیلی شور است", "قیمت غذا بالاست", "من گرسنه نیستم"),
                    correctAnswer = "غذا بسیار مزه‌دار است!",
                    explanationDari = "فعل schmecken gut یعنی طعم بسیار خوبی دارد."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Er ___ gern Fisch.",
                    pronunciation = "اِر ایست گِرن فیش.",
                    translationDari = "او ماهی دوست دارد.",
                    options = listOf("isst", "esse", "esst", "essen"),
                    correctAnswer = "isst",
                    explanationDari = "فعل essen برای سوم شخص er به صورت isst تغییر می‌کند."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "کدام کلمه به معنی 'شیرین' است؟",
                    pronunciation = "زوس",
                    translationDari = "شیرین",
                    options = listOf("süß", "salzig", "sauer", "lecker"),
                    correctAnswer = "süß",
                    explanationDari = "واژه süß در آلمانی یعنی شیرین."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Ich trinke nicht ___ Milch.",
                    pronunciation = "ایش ترینکه نیشت گِرن میلش.",
                    translationDari = "من شیر دوست ندارم (با میل نمی‌نوشم).",
                    options = listOf("gern", "gut", "sehr", "viel"),
                    correctAnswer = "gern",
                    explanationDari = "ترکیب nicht gern برای بیان بی‌میلی یا دوست نداشتن کاری است."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("میزبان", "Wie schmeckt das Essen?", "وی شمِکت داس اِسِن؟", "غذا چطور مزه می‌دهد؟"),
                        DialogueLineItem("مهمان", "Es schmeckt wirklich lecker! Danke.", "اِس شمِکت ویرکلیش لِکِر! دانکه.", "واقعاً خوشمزه است! تشکر."),
                        DialogueLineItem("میزبان", "Möchtest du noch etwas Reis?", "مِشتِست دو نوخ اِتواس رایس؟", "آیا کمی دیگر برنج میل داری؟"),
                        DialogueLineItem("مهمان", "Nein danke, ich bin satt.", "ناین دانکه، ایش بین زات.", "نه ممنون، من سیر هستم.")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "بیان علاقه و طعم غذا (schmecken, gern, mögen)",
                    bodyDari = "• فعل schmecken (مزه دادن):\n- Das schmeckt sehr gut! (بسیار خوشمزه است!)\n• استفاده از gern (با علاقه):\n- Ich esse gern Reis. (من با علاقه برنج می‌خورم)\n• فعل mögen (دوست داشتن):\n- Ich mag Obst. (من میوه دوست دارم)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Wie schmeckt das Essen?",
                    questionPronunciation = "وی شمِکت داس اِسِن؟",
                    questionDari = "طعم غذا چطور است؟",
                    answerGerman = "Es schmeckt fantastisch!",
                    answerPronunciation = "اِس شمِکت فانتَستیش!",
                    answerDari = "مزه فوق‌العاده‌ای دارد!"
                ),
                QaPair(
                    questionGerman = "Was isst du gern?",
                    questionPronunciation = "واس ایست دو گِرن؟",
                    questionDari = "چه غذایی دوست داری؟",
                    answerGerman = "Ich esse gern Fisch und Reis.",
                    answerPronunciation = "ایش اِسه گِرن فیش اونت رایس.",
                    answerDari = "من ماهی و برنج را با علاقه می‌خورم."
                )
            )
        ),

        // ================= LESSON 7 =================
        LessonData(
            id = "lesson_7",
            number = 7,
            titleGerman = "Meine Familie",
            titleDari = "درس ۷: خانواده من",
            vocabulary = listOf(
                VocabularyItem("der", "Vater", "فاتِر", "پدر (مذکر: der)"),
                VocabularyItem("die", "Mutter", "موتِر", "مادر (مؤنث: die)"),
                VocabularyItem("die (Plural)", "Eltern", "اِلتِرن", "والدین / پدر و مادر"),
                VocabularyItem("der", "Sohn", "زون", "پسر (فرزند مذکر)"),
                VocabularyItem("die", "Tochter", "توختِر", "دختر (فرزند مؤنث)"),
                VocabularyItem("der", "Bruder", "برودِر", "برادر (مذکر: der)"),
                VocabularyItem("die", "Schwester", "شووِستِر", "خواهر (مؤنث: die)"),
                VocabularyItem("die (Plural)", "Geschwister", "گِشویشتِر", "خواهران و برادران"),
                VocabularyItem("der", "Großvater", "گروس‌فاتِر (اوپا)", "پدرکلان / پدربزرگ"),
                VocabularyItem("die", "Großmutter", "گروس‌موتِر (اوما)", "مادرکلان / مادربزرگ"),
                VocabularyItem("das", "Kind", "کیند", "طِفل / کودک"),
                VocabularyItem("das", "Baby", "بِیبی", "نوزاد"),
                VocabularyItem("der", "Mann", "مان", "شوهر / مرد"),
                VocabularyItem("die", "Frau", "فراو", "خانم / همسر"),
                VocabularyItem("", "mein", "ماین", "مال من (برای مذکر و خنثی)"),
                VocabularyItem("", "meine", "ماینه", "مال من (برای مؤنث و جمع)")
            ),
            exampleSentences = listOf(
                ExampleSentence("Das ist mein Vater und das ist meine Mutter.", "داس ایست ماین فاتِر اونت داس ایست ماینه موتِر.", "این پدر من و این مادر من است."),
                ExampleSentence("Ich habe einen Bruder und zwei Schwestern.", "ایش هابه آینِن برودِر اونت تسوای شِوِستِرن.", "من یک برادر و دو خواهر دارم."),
                ExampleSentence("Meine Großeltern leben in Afghanistan.", "ماینه گروس‌اِلتِرن لِبِن این افغانستان.", "پدرکلان و مادرکلانم در افغانستان زندگی می‌کنند.")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "fill-blank",
                    question = "Das ist ___ Mutter.",
                    pronunciation = "داس ایست ماینه موتِر.",
                    translationDari = "این مادر من است.",
                    options = listOf("meine", "mein", "meinen", "meines"),
                    correctAnswer = "meine",
                    explanationDari = "واژه Mutter مؤنث است و صفت ملکی آن meine می‌شود."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Wo wohnt ___ Bruder?",
                    pronunciation = "وو وونت داین برودِر؟",
                    translationDari = "برادر تو کجا زندگی می‌کند؟",
                    options = listOf("dein", "deine", "deinen", "deiner"),
                    correctAnswer = "dein",
                    explanationDari = "واژه Bruder مذکر است و صفت ملکی حالت فاعلی آن dein است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Meine Eltern leben in Deutschland.",
                    pronunciation = "ماینه اِلتِرن لِبِن این دویچلند.",
                    translationDari = "والدینم در آلمان زندگی می‌کنند.",
                    options = listOf("والدین (پدر و مادرم)", "خواهرانم", "برادرانم", "کودکانم"),
                    correctAnswer = "والدین (پدر و مادرم)",
                    explanationDari = "واژه Eltern به معنای والدین (پدر و مادر) است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "کدام کلمه به معنی 'خواهران و برادران' است؟",
                    pronunciation = "گِشویشتِر",
                    translationDari = "خواهران و برادران",
                    options = listOf("Geschwister", "Eltern", "Kinder", "Freunde"),
                    correctAnswer = "Geschwister",
                    explanationDari = "کلمه Geschwister شامل همه برادرها و خواهرهای یک فرد می‌شود."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("آنا", "Wer ist das auf dem Foto?", "وِر ایست داس آوف دِم فوتو؟", "این شخص در عکس کیست؟"),
                        DialogueLineItem("مریم", "Das ist meine Schwester.", "داس ایست ماینه شوِستِر.", "این خواهرم است."),
                        DialogueLineItem("آنا", "Wie alt ist sie?", "وی آلت ایست زی؟", "او چند ساله است؟"),
                        DialogueLineItem("مریم", "Sie ist fünfzehn Jahre alt.", "زی ایست فونس‌تسن یاره آلت.", "او ۱۵ ساله است.")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "ضمایر ملکی (mein / dein)",
                    bodyDari = "• برای اسم مذکر و خنثی: mein (مال من) / dein (مال تو):\n- mein Vater (پدرم), mein Bruder (برادرم), mein Kind (فرزندم)\n• برای اسم مؤنث و جمع: meine (مال من) / deine (مال تو):\n- meine Mutter (مادرم), meine Schwester (خواهرم), meine Eltern (والدینم)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Hast du Geschwister?",
                    questionPronunciation = "هاست دو گِشوِستِر؟",
                    questionDari = "آیا خواهر یا برادر داری؟",
                    answerGerman = "Ja, ich habe zwei Brüder.",
                    answerPronunciation = "یا، ایش هابه تسوای برودِر.",
                    answerDari = "بله، من دو برادر دارم."
                ),
                QaPair(
                    questionGerman = "Wer ist das?",
                    questionPronunciation = "وِر ایست داس؟",
                    questionDari = "این شخص کیست؟",
                    answerGerman = "Das ist meine Mutter.",
                    answerPronunciation = "داس ایست ماینه موتِر.",
                    answerDari = "این مادر من است."
                )
            )
        ),

        // ================= LESSON 8 =================
        LessonData(
            id = "lesson_8",
            number = 8,
            titleGerman = "Wohnen & Möbel",
            titleDari = "درس ۸: خانه و مسکن",
            vocabulary = listOf(
                VocabularyItem("die", "Wohnung", "وونونگ", "آپارتمان / خانه"),
                VocabularyItem("das", "Haus", "هاوس", "خانه ویلایی / حویلی"),
                VocabularyItem("das", "Zimmer", "تسیمِر", "اتاق"),
                VocabularyItem("die", "Küche", "کوشه", "آشپزخانه"),
                VocabularyItem("das", "Bad", "باد", "حمام و تشناب"),
                VocabularyItem("das", "Wohnzimmer", "وون‌تسیمِر", "اتاق سالون / نشیمن"),
                VocabularyItem("das", "Schlafzimmer", "شلاف‌تسیمِر", "اتاق خواب"),
                VocabularyItem("der", "Balkon", "بالکون", "بالکن / ایوان"),
                VocabularyItem("das", "Bett", "بِت", "تخت خواب / چپرکت"),
                VocabularyItem("das", "Sofa", "زوفا", "کوچ / کاناپه"),
                VocabularyItem("der", "Schrank", "شرانک", "الماری / کمد"),
                VocabularyItem("", "groß", "گروس", "کلان / بزرگ"),
                VocabularyItem("", "klein", "کلاین", "خُرد / کوچک"),
                VocabularyItem("", "hell", "هِل", "روشن / آفتاب‌گیر"),
                VocabularyItem("die", "Miete", "میته", "کرایه خانه"),
                VocabularyItem("", "teuer", "تویِر", "گران"),
                VocabularyItem("", "billig", "بیلیش", "ارزان")
            ),
            exampleSentences = listOf(
                ExampleSentence("Die Wohnung hat drei Zimmer und eine Küche.", "دی وونونگ هات درای تسیمِر اونت آینه کوشه.", "آپارتمان سه اتاق و یک آشپزخانه دارد."),
                ExampleSentence("Das Wohnzimmer ist sehr groß und hell.", "داس وون‌تسیمِر ایست زر گروس اونت هِل.", "سالون بسیار کلان و روشن است."),
                ExampleSentence("Wie hoch ist die Miete pro Monat?", "وی هوخ ایست دی میته پرو مونات؟", "کرایه خانه در هر ماه چقدر است؟")
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Die Wohnung ist hell und billig.",
                    pronunciation = "دی وونونگ ایست هِل اونت بیلیش.",
                    translationDari = "آپارتمان روشن و ارزان است.",
                    options = listOf("روشن و ارزان", "تاریک و گران", "کوچک و پرسروصدا", "قدیمی و دور"),
                    correctAnswer = "روشن و ارزان",
                    explanationDari = "واژه hell یعنی روشن، و billig یعنی ارزان قیمت."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Die Wohnung hat ___ schönes Bad.",
                    pronunciation = "دی وونونگ هات آین شونِس باد.",
                    translationDari = "آپارتمان یک حمام قشنگ دارد.",
                    options = listOf("ein", "eine", "einen", "keine"),
                    correctAnswer = "ein",
                    explanationDari = "واژه Bad خنثی است (das Bad) و حرف تعریف نامعین آن ein است."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "کدام کلمه به معنی 'آشپزخانه' است؟",
                    pronunciation = "دی کوشه",
                    translationDari = "آشپزخانه",
                    options = listOf("die Küche", "das Zimmer", "das Bad", "der Balkon"),
                    correctAnswer = "die Küche",
                    explanationDari = "آشپزخانه در زبان آلمانی die Küche (مؤنث) نام دارد."
                ),
                ExerciseItem(
                    type = "fill-blank",
                    question = "Hier ___ wir seit zwei Jahren.",
                    pronunciation = "هیر وونن ویر زایت تسوای یارن.",
                    translationDari = "ما دو سال است که اینجا زندگی می‌کنیم.",
                    options = listOf("wohnen", "wohne", "wohnst", "wohnt"),
                    correctAnswer = "wohnen",
                    explanationDari = "برای فاعل wir (ما)، فعل به شکل wohnen می‌آید."
                )
            ),
            dialogues = listOf(
                DialogueItem(
                    lines = listOf(
                        DialogueLineItem("مستأجر", "Wie viele Zimmer hat die Wohnung?", "وی فیلِه تسیمِر هات دی وونونگ؟", "آپارتمان چند اتاق دارد؟"),
                        DialogueLineItem("صاحب‌خانه", "Sie hat zwei Zimmer, eine Küche und ein Bad.", "زی هات تسوای تسیمِر، آینه کوشه اونت آین باد.", "دو اتاق، یک آشپزخانه و یک حمام دارد."),
                        DialogueLineItem("مستأجر", "Gibt es auch einen Balkon?", "گیبت اِس آوخ آینِن بالکون؟", "آیا بالکن هم دارد؟"),
                        DialogueLineItem("صاحب‌خانه", "Ja, einen schönen großen Balkon.", "یا، آینِن شونِن گروسِن بالکون.", "بله، یک بالکن قشنگ و بزرگ.")
                    )
                )
            ),
            grammarSections = listOf(
                GrammarSection(
                    title = "نام اتاق‌ها و وسایل خانه",
                    bodyDari = "• اتاق‌ها در خانه:\n- das Wohnzimmer (اتاق نشیمن / هال)\n- das Schlafzimmer (اتاق خواب)\n- die Küche (آشپزخانه)\n- das Bad (حمام)\n• صفت‌های توصیف خانه:\n- groß (بزرگ), hell (روشن), ruhig (آرام)"
                )
            ),
            qaPairs = listOf(
                QaPair(
                    questionGerman = "Wie ist deine Wohnung?",
                    questionPronunciation = "وی ایست داینه وونونگ؟",
                    questionDari = "خانه‌ات چطور است؟",
                    answerGerman = "Sie ist sehr groß und ruhig.",
                    answerPronunciation = "زی ایست زِر گروس اونت روهیش.",
                    answerDari = "بسیار بزرگ و آرام است."
                ),
                QaPair(
                    questionGerman = "Wie viele Zimmer hat das Haus?",
                    questionPronunciation = "وی فیلِه تسیمِر هات داس هاوس؟",
                    questionDari = "خانه چند اتاق دارد؟",
                    answerGerman = "Das Haus hat vier Zimmer.",
                    answerPronunciation = "داس هاوس هات فیر تسیمِر.",
                    answerDari = "خانه دارای چهار اتاق است."
                )
            )
        )
    )
}

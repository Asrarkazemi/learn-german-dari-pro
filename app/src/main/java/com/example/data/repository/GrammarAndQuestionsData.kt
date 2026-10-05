package com.example.data.repository

import com.example.data.model.ConjugationRow
import com.example.data.model.PracticeQuestion
import com.example.data.model.QuestionType
import com.example.data.model.VerbConjugation

object GrammarAndQuestionsData {

    val verbConjugations: List<VerbConjugation> = listOf(
        VerbConjugation(
            verbInfinitive = "sein",
            meaningDari = "بودن (هست)",
            isIrregular = true,
            tipDari = "فعل sein یکی از مهم‌ترین افعال بی‌قاعده در زبان آلمانی است و باید کاملاً حفظ شود.",
            conjugations = listOf(
                ConjugationRow("ich", "من", "bin"),
                ConjugationRow("du", "تو", "bist"),
                ConjugationRow("er / sie / es", "او (مذکر/مؤنث/خنثی)", "ist"),
                ConjugationRow("wir", "ما", "sind"),
                ConjugationRow("ihr", "شماها", "seid"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "sind")
            )
        ),
        VerbConjugation(
            verbInfinitive = "haben",
            meaningDari = "داشتن",
            isIrregular = true,
            tipDari = "در صیغه‌های du و er/sie/es، حرف b حذف می‌شود (du hast, er hat).",
            conjugations = listOf(
                ConjugationRow("ich", "من", "habe"),
                ConjugationRow("du", "تو", "hast"),
                ConjugationRow("er / sie / es", "او", "hat"),
                ConjugationRow("wir", "ما", "haben"),
                ConjugationRow("ihr", "شماها", "habt"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "haben")
            )
        ),
        VerbConjugation(
            verbInfinitive = "heißen",
            meaningDari = "نامیده شدن / نام داشتن",
            isIrregular = false,
            tipDari = "چون ریشه فعل به ß ختم می‌شود، برای صیغه du به جای st فقط t می‌گیرد (du heißt).",
            conjugations = listOf(
                ConjugationRow("ich", "من", "heiße"),
                ConjugationRow("du", "تو", "heißt"),
                ConjugationRow("er / sie / es", "او", "heißt"),
                ConjugationRow("wir", "ما", "heißen"),
                ConjugationRow("ihr", "شماها", "heißt"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "heißen")
            )
        ),
        VerbConjugation(
            verbInfinitive = "kommen",
            meaningDari = "آمدن / اهل جایی بودن",
            isIrregular = false,
            tipDari = "یک فعل کاملاً باقاعده: پسوندهای e, st, t, en, t, en به ریشه komm اضافه می‌شوند.",
            conjugations = listOf(
                ConjugationRow("ich", "من", "komme"),
                ConjugationRow("du", "تو", "kommst"),
                ConjugationRow("er / sie / es", "او", "kommt"),
                ConjugationRow("wir", "ما", "kommen"),
                ConjugationRow("ihr", "شماها", "kommt"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "kommen")
            )
        ),
        VerbConjugation(
            verbInfinitive = "wohnen",
            meaningDari = "زندگی کردن / سکونت داشتن",
            isIrregular = false,
            tipDari = "فعل باقاعده: ریشه wohn + پسوندهای فاعلی.",
            conjugations = listOf(
                ConjugationRow("ich", "من", "wohne"),
                ConjugationRow("du", "تو", "wohnst"),
                ConjugationRow("er / sie / es", "او", "wohnt"),
                ConjugationRow("wir", "ما", "wohnen"),
                ConjugationRow("ihr", "شماها", "wohnt"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "wohnen")
            )
        ),
        VerbConjugation(
            verbInfinitive = "sprechen",
            meaningDari = "صحبت کردن / گپ زدن",
            isIrregular = true,
            tipDari = "فعل تغییر صدادار (e تبدیل به i می‌شود در صیغه‌های du sprichst و er spricht).",
            conjugations = listOf(
                ConjugationRow("ich", "من", "spreche"),
                ConjugationRow("du", "تو", "sprichst"),
                ConjugationRow("er / sie / es", "او", "spricht"),
                ConjugationRow("wir", "ما", "sprechen"),
                ConjugationRow("ihr", "شماها", "sprecht"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "sprechen")
            )
        ),
        VerbConjugation(
            verbInfinitive = "lernen",
            meaningDari = "یاد گرفتن / آموختن",
            isIrregular = false,
            tipDari = "فعل باقاعده برای درس خواندن و مهارت یاد گرفتن: ریشه lern + پسوندها.",
            conjugations = listOf(
                ConjugationRow("ich", "من", "lerne"),
                ConjugationRow("du", "تو", "lernst"),
                ConjugationRow("er / sie / es", "او", "lernt"),
                ConjugationRow("wir", "ما", "lernen"),
                ConjugationRow("ihr", "شماها", "lernt"),
                ConjugationRow("sie / Sie", "آن‌ها / شما (رسمی)", "lernen")
            )
        )
    )

    val practiceQuestions: List<PracticeQuestion> = listOf(
        // Lektion 1
        PracticeQuestion(
            id = "q1_1", lessonId = 1, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Ich ___ aus Afghanistan.",
            promptDari = "جای خالی را با شکل درست فعل kommen پر کنید:",
            options = listOf("komme", "kommst", "kommt", "kommen"),
            correctIndex = 0,
            explanationDari = "برای ضمیر اول شخص (Ich)، فعل پسوند -e می‌گیرد: Ich komme."
        ),
        PracticeQuestion(
            id = "q1_2", lessonId = 1, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Wie ___ du?",
            promptDari = "جای خالی را با فعل heißen پر کنید:",
            options = listOf("heiße", "heißt", "heißen", "heißt du"),
            correctIndex = 1,
            explanationDari = "برای ضمیر du، فعل heißen به شکل heißt صرف می‌شود: Wie heißt du?"
        ),
        PracticeQuestion(
            id = "q1_3", lessonId = 1, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Er ___ Ahmad und kommt aus Herat.",
            promptDari = "جای خالی را با فعل sein پر کنید:",
            options = listOf("bin", "bist", "ist", "sind"),
            correctIndex = 2,
            explanationDari = "برای سوم شخص مفرد (Er)، شکل صحیح فعل sein کلمه ist می‌باشد."
        ),
        PracticeQuestion(
            id = "q1_4", lessonId = 1, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Auf Wiedersehen!",
            promptDari = "معنای این عبارت در زبان دری چیست؟",
            options = listOf("صبح بخیر!", "به امید دیدار / خداحافظ!", "حال شما چطور است؟", "تشکر بسیار!"),
            correctIndex = 1,
            explanationDari = "عبارت Auf Wiedersehen یک خداحافظی رسمی به معنای 'به امید دیدار' است."
        ),
        PracticeQuestion(
            id = "q1_5", lessonId = 1, questionType = QuestionType.NUMBER_PRACTICE,
            promptGerman = "کدام کلمه معادل عدد ۷ (هفت) است؟",
            promptDari = "انتخاب عدد در زبان آلمانی:",
            options = listOf("sechs", "sieben", "acht", "zehn"),
            correctIndex = 1,
            explanationDari = "عدد ۷ در زبان آلمانی sieben (زیبِن) نامیده می‌شود."
        ),

        // Lektion 2
        PracticeQuestion(
            id = "q2_1", lessonId = 2, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "___ du Deutsch?",
            promptDari = "جای خالی را با شکل درست sprechen پر کنید:",
            options = listOf("Spreche", "Sprichst", "Sprecht", "Sprechen"),
            correctIndex = 1,
            explanationDari = "فعل sprechen نامنظم است و برای du تبدیل به sprichst می‌شود."
        ),
        PracticeQuestion(
            id = "q2_2", lessonId = 2, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Wir ___ eine Handynummer.",
            promptDari = "جای خالی را با فعل haben پر کنید:",
            options = listOf("habe", "hast", "hat", "haben"),
            correctIndex = 3,
            explanationDari = "برای ضمیر ما (wir)، فعل به صورت کامل و مصدری می‌آید: Wir haben."
        ),
        PracticeQuestion(
            id = "q2_3", lessonId = 2, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Sie ist Ärztin von Beruf.",
            promptDari = "ترجمه این جمله به دری کدام است؟",
            options = listOf("او معلم است.", "او داکتر (پزشک زن) است.", "او محصل است.", "او فروشنده است."),
            correctIndex = 1,
            explanationDari = "واژه Ärztin مؤنث داکتر (پزشک) است. پسوند in- نشان‌دهنده شغل بانوان است."
        ),
        PracticeQuestion(
            id = "q2_4", lessonId = 2, questionType = QuestionType.TRANSLATION_FA_TO_DE,
            promptGerman = "کدام گزینه به معنی 'دانشجو / محصل آقا' است؟",
            promptDari = "معادل آلمانی:",
            options = listOf("die Studentin", "der Student", "der Lehrer", "die Arbeit"),
            correctIndex = 1,
            explanationDari = "محصل مرد در آلمانی der Student و محصل زن die Studentin است."
        ),

        // Lektion 3
        PracticeQuestion(
            id = "q3_1", lessonId = 3, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "___ Tisch ist sehr groß.",
            promptDari = "حرف تعریف درست برای Tisch (میز) چیست؟",
            options = listOf("Der", "Die", "Das", "Ein"),
            correctIndex = 0,
            explanationDari = "کلمه Tisch در زبان آلمانی مذکر است و با حرف تعریف der می‌آید."
        ),
        PracticeQuestion(
            id = "q3_2", lessonId = 3, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "___ Buch ist neu.",
            promptDari = "حرف تعریف درست برای Buch (کتاب) چیست؟",
            options = listOf("Der", "Die", "Das", "Den"),
            correctIndex = 2,
            explanationDari = "کلمه Buch در زبان آلمانی خنثی است و حرف تعریف آن das می‌باشد."
        ),
        PracticeQuestion(
            id = "q3_3", lessonId = 3, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "Wo ist ___ Tasche?",
            promptDari = "حرف تعریف درست برای Tasche (بکس/کیف) چیست؟",
            options = listOf("der", "die", "das", "ein"),
            correctIndex = 1,
            explanationDari = "کلمه Tasche مؤنث است و با حرف تعریف die می‌آید."
        ),
        PracticeQuestion(
            id = "q3_4", lessonId = 3, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Wie heißt das auf Deutsch?",
            promptDari = "معنای این سوال مهم چیست؟",
            options = listOf("قیمت این چند است؟", "این به آلمانی چه نام دارد؟", "شما کجا زندگی می‌کنید؟", "ساعت چند است؟"),
            correctIndex = 1,
            explanationDari = "این جمله کاربردی به معنای 'این به آلمانی چه نامیده می‌شود/چیست؟' است."
        ),

        // Lektion 4
        PracticeQuestion(
            id = "q4_1", lessonId = 4, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Einen Kaffee, bitte!",
            promptDari = "معنای عبارت را مشخص کنید:",
            options = listOf("یک گیلاس چای، لطفاً!", "یک قهوه، لطفاً!", "یک بوتل آب، لطفاً!", "حساب، لطفاً!"),
            correctIndex = 1,
            explanationDari = "کلمه der Kaffee در حالت مفعولی اکوزاتیو تبدیل به einen Kaffee می‌شود."
        ),
        PracticeQuestion(
            id = "q4_2", lessonId = 4, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Zusammen oder getrennt?",
            promptDari = "پیشخدمت در رستوران چه چیزی می‌پرسد؟",
            options = listOf("چای میل دارید یا قهوه؟", "با هم یکجا حساب می‌کنید یا جداگانه؟", "شکر می‌خواهید یا شیر؟", "پول نقد یا کارت بانکی؟"),
            correctIndex = 1,
            explanationDari = "کلمه zusammen یعنی با هم/یکجا، و getrennt یعنی جداگانه."
        ),
        PracticeQuestion(
            id = "q4_3", lessonId = 4, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Was ___ Sie trinken?",
            promptDari = "جای خالی را با فعل möchten پر کنید:",
            options = listOf("möchte", "möchtest", "möchtet", "möchten"),
            correctIndex = 3,
            explanationDari = "برای ضمیر محترمانه Sie (شما)، فعل به صورت möchten صرف می‌شود."
        ),

        // Lektion 5
        PracticeQuestion(
            id = "q5_1", lessonId = 5, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Am Montag arbeite ich nicht.",
            promptDari = "معنای این جمله چیست؟",
            options = listOf("روز دوشنبه کار نمی‌کنم.", "روز جمعه مکتب ندارم.", "آخر هفته درس می‌خوانم.", "صبح‌ها زود بیدار می‌شوم."),
            correctIndex = 0,
            explanationDari = "واژه Montag یعنی روز دوشنبه و با حرف اضافه am می‌آید."
        ),
        PracticeQuestion(
            id = "q5_2", lessonId = 5, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Ich stehe um 7 Uhr ___.",
            promptDari = "بخش دوم فعل جداشدنی aufstehen چیست؟",
            options = listOf("ein", "an", "auf", "aus"),
            correctIndex = 2,
            explanationDari = "فعل aufstehen جداشدنی است. در جمله پیشوند auf به آخر جمله منتقل می‌شود."
        ),
        PracticeQuestion(
            id = "q5_3", lessonId = 5, questionType = QuestionType.NUMBER_PRACTICE,
            promptGerman = "کدام روز هفته به معنی 'چهارشنبه' است؟",
            promptDari = "روزهای هفته:",
            options = listOf("Dienstag", "Mittwoch", "Donnerstag", "Samstag"),
            correctIndex = 1,
            explanationDari = "روز Mittwoch وسط هفته آلمانی یعنی چهارشنبه است."
        ),

        // Lektion 6
        PracticeQuestion(
            id = "q6_1", lessonId = 6, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Das Essen schmeckt sehr gut!",
            promptDari = "معنای عبارت را انتخاب کنید:",
            options = listOf("غذا خیلی شور است.", "غذا بسیار مزه‌دار / خوشمزه است!", "من گرسنه نیستم.", "قیمت غذا مناسب است."),
            correctIndex = 1,
            explanationDari = "فعل schmecken به معنای طعم دادن است: Das schmeckt gut یعنی این مزه خوبی دارد."
        ),
        PracticeQuestion(
            id = "q6_2", lessonId = 6, questionType = QuestionType.FILL_IN_BLANK_VERB,
            promptGerman = "Er ___ gern Obst und Gemüse.",
            promptDari = "شکل سوم شخص فعل essen چیست؟",
            options = listOf("esse", "isst", "esst", "essen"),
            correctIndex = 1,
            explanationDari = "فعل essen برای er/sie/es به isst تغییر می‌کند (e تبدیل به i می‌شود)."
        ),
        PracticeQuestion(
            id = "q6_3", lessonId = 6, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Ich trinke nicht gern Milch.",
            promptDari = "کلمه 'nicht gern' چه مفهومی دارد؟",
            options = listOf("با علاقه و میل زیاد", "بدون علاقه / دوست ندارم", "هر روز صبح", "همیشه با شکر"),
            correctIndex = 1,
            explanationDari = "ترکیب nicht gern نشان‌دهنده عدم علاقه به انجام کاری است."
        ),

        // Lektion 7
        PracticeQuestion(
            id = "q7_1", lessonId = 7, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "Das ist ___ Mutter.",
            promptDari = "صفت ملکی درست برای Mutter (مادر، مؤنث) چیست؟",
            options = listOf("mein", "meine", "meinen", "meines"),
            correctIndex = 1,
            explanationDari = "برای اسامی مؤنث و جمع از meine استفاده می‌شود: meine Mutter."
        ),
        PracticeQuestion(
            id = "q7_2", lessonId = 7, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "Wo wohnt ___ Bruder?",
            promptDari = "صفت ملکی درست برای Bruder (برادر، مذکر) چیست؟",
            options = listOf("dein", "deine", "deinen", "deiner"),
            correctIndex = 0,
            explanationDari = "برای اسامی مذکر فاعلی از dein بدون e استفاده می‌شود: dein Bruder."
        ),
        PracticeQuestion(
            id = "q7_3", lessonId = 7, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Meine Eltern leben in Deutschland.",
            promptDari = "ترجمه کلمه Eltern چیست؟",
            options = listOf("خواهران و برادران", "والدین (پدر و مادر)", "پدرکلان و مادرکلان", "اطفال"),
            correctIndex = 1,
            explanationDari = "کلمه Eltern به معنای پدر و مادر (والدین) و اسمی همیشه جمع است."
        ),

        // Lektion 8
        PracticeQuestion(
            id = "q8_1", lessonId = 8, questionType = QuestionType.ARTICLE_SELECTION,
            promptGerman = "Die Wohnung hat ___ schönes Bad.",
            promptDari = "حرف تعریف نامعین برای Bad (خنثی: das Bad):",
            options = listOf("ein", "eine", "einen", "keine"),
            correctIndex = 0,
            explanationDari = "برای اسامی خنثی در حالت فاعلی و مفعولی از ein استفاده می‌شود: ein Bad."
        ),
        PracticeQuestion(
            id = "q8_2", lessonId = 8, questionType = QuestionType.TRANSLATION_DE_TO_FA,
            promptGerman = "Die Wohnung ist hell und billig.",
            promptDari = "توصیف این آپارتمان چیست؟",
            options = listOf("تاریک و گران", "روشن و ارزان", "کوچک و پرسروصدا", "قدیمی و دور"),
            correctIndex = 1,
            explanationDari = "کلمه hell یعنی روشن/نورگیر، و billig یعنی ارزان."
        ),
        PracticeQuestion(
            id = "q8_3", lessonId = 8, questionType = QuestionType.TRANSLATION_FA_TO_DE,
            promptGerman = "کدام کلمه به معنی 'آشپزخانه' است؟",
            promptDari = "انتخاب کلمه آلمانی:",
            options = listOf("das Zimmer", "die Küche", "das Bad", "der Balkon"),
            correctIndex = 1,
            explanationDari = "آشپزخانه در زبان آلمانی die Küche (مؤنث) نام دارد."
        )
    )

    fun getQuestionsForLesson(lessonId: Int): List<PracticeQuestion> {
        return practiceQuestions.filter { it.lessonId == lessonId }
    }

    fun getRandomQuiz(count: Int = 10): List<PracticeQuestion> {
        return practiceQuestions.shuffled().take(count)
    }
}

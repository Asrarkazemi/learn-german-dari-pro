package com.example.data.repository

import com.example.data.model.ExampleSentence
import com.example.data.model.ExerciseItem
import com.example.data.model.GrammarSection
import com.example.data.model.GrammarTopic

object BuiltInGrammarData {
    val topics: List<GrammarTopic> = listOf(
        GrammarTopic(
            id = "grammar_1",
            number = 1,
            titleGerman = "Präsens der Verben",
            titleDari = "حال سادهٔ فعل‌ها (Präsens)",
            sections = listOf(
                GrammarSection(
                    title = "۱. تعریف و کاربرد زمان حال ساده",
                    bodyDari = "زمان حال ساده (Präsens) اصلی‌ترین و پرکاربردترین زمان در زبان آلمانی است. از این زمان برای بیان موارد زیر استفاده می‌شود:\n\n۱. کارهای جاری در زمان حال: Ich lerne Deutsch (من آلمانی یاد می‌گیرم).\n۲. حقایق همیشگی و عادات روزمره: Die Sonne scheint (آفتاب می‌تابد).\n۳. رویدادهای مشخص آینده همراه با قید زمان: Morgen fahre ich nach Berlin (فردا به برلین سفر می‌کنم)."
                ),
                GrammarSection(
                    title = "۲. صرف فعل‌های باقاعده و شناسه‌ها",
                    bodyDari = "برای صرف فعل‌های باقاعده در زمان حال ساده، پسوند مصدر یعنی «-en» را از انتهای فعل برمی‌داریم تا ریشه فعل به دست آید. سپس شناسه‌های زیر را به ریشه اضافه می‌نماییم:\n\n• ich (من): شناسه «-e»  ←  ich lerne\n• du (تو): شناسه «-st»  ←  du lernst\n• er / sie / es (او): شناسه «-t»  ←  er lernt\n• wir (ما): شناسه «-en»  ←  wir lernen\n• ihr (شماها): شناسه «-t»  ←  ihr lernt\n• sie / Sie (آن‌ها / شما رسمی): شناسه «-en»  ←  sie lernen"
                )
            ),
            exampleSentences = listOf(
                ExampleSentence(
                    german = "Ich lerne jeden Tag Deutsch.",
                    pronunciation = "ایش لِرنِه یِدِن تاک دویتش.",
                    meaningDari = "من هر روز آلمانی می‌آموزم."
                ),
                ExampleSentence(
                    german = "Du wohnst in Berlin.",
                    pronunciation = "دو وُنست این برلین.",
                    meaningDari = "تو در برلین زندگی می‌کنی."
                ),
                ExampleSentence(
                    german = "Er arbeitet in einem Büro.",
                    pronunciation = "اِر آربایتِت این آینِم بورو.",
                    meaningDari = "او در یک دفتر کار می‌کند."
                ),
                ExampleSentence(
                    german = "Wir sprechen Persisch und Deutsch.",
                    pronunciation = "ویر شپریشِن پِرزیش اوند دویتش.",
                    meaningDari = "ما فارسی و آلمانی صحبت می‌کنیم."
                ),
                ExampleSentence(
                    german = "Ihr kommt heute Abend.",
                    pronunciation = "ایهر کُمت هویتِه آبِند.",
                    meaningDari = "شماها امروز شام می‌آیید."
                ),
                ExampleSentence(
                    german = "Sie trinken gerne Kaffee.",
                    pronunciation = "زی ترینکِن گِرنِه کافِه.",
                    meaningDari = "آن‌ها با علاقه قهوه می‌نوشند."
                )
            ),
            exercises = listOf(
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Ich ___ in Kabul.",
                    pronunciation = "ایش ... این کابل.",
                    translationDari = "من در کابل زندگی می‌کنم.",
                    options = listOf("wohne", "wohnst", "wohnt", "wohnen"),
                    correctAnswer = "wohne",
                    explanationDari = "برای ضمیر فاعلی ich (من)، شناسه فعل «-e» است: ich wohne."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Er ___ Deutsch und Englisch.",
                    pronunciation = "اِر ... دویتش اوند انگلیش.",
                    translationDari = "او آلمانی و انگلیسی یاد می‌گیرد.",
                    options = listOf("lerne", "lernst", "lernt", "lernen"),
                    correctAnswer = "lernt",
                    explanationDari = "برای ضمایر سوم شخص مفرد (er/sie/es)، شناسه فعل «-t» است: er lernt."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Wir ___ jeden Morgen Tee.",
                    pronunciation = "ویر ... یِدِن مورگِن تِه.",
                    translationDari = "ما هر صبح چای می‌نوشیم.",
                    options = listOf("trinke", "trinkst", "trinkt", "trinken"),
                    correctAnswer = "trinken",
                    explanationDari = "برای ضمیر فاعلی wir (ما)، شناسه فعل «-en» است: wir trinken."
                ),
                ExerciseItem(
                    type = "multiple-choice",
                    question = "Du ___ sehr gut Deutsch.",
                    pronunciation = "دو ... زِر گوت دویتش.",
                    translationDari = "تو بسیار عالی آلمانی صحبت می‌کنی.",
                    options = listOf("spreche", "sprichst", "sprecht", "sprechen"),
                    correctAnswer = "sprichst",
                    explanationDari = "فعل sprechen برای ضمیر du به صورت بی‌قاعده صرف می‌شود و e به i تبدیل می‌شود: du sprichst."
                )
            ),
            source = "built-in"
        )
    )
}

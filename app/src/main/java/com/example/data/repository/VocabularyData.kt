package com.example.data.repository

import com.example.data.model.ArticleType
import com.example.data.model.WordItem

object VocabularyData {

    val allWords: List<WordItem> = listOf(
        // ================= LESSON 1: Greetings & Introductions =================
        WordItem(
            id = "w1_1", lessonId = 1, german = "Guten Tag", article = ArticleType.NONE,
            pronunciationPersian = "گوتِن تاگ", dariMeaning = "روز بخیر (سلام رسمی)",
            exampleGerman = "Guten Tag, wie geht es Ihnen?", exampleDari = "روز بخیر، حال شما چطور است؟", category = "احوال‌پرسی"
        ),
        WordItem(
            id = "w1_2", lessonId = 1, german = "Hallo", article = ArticleType.NONE,
            pronunciationPersian = "هالو", dariMeaning = "سلام (صمیمانه)",
            exampleGerman = "Hallo Ahmad, alles gut?", exampleDari = "سلام احمد، همه چیز روبه‌راه است؟", category = "احوال‌پرسی"
        ),
        WordItem(
            id = "w1_3", lessonId = 1, german = "Guten Morgen", article = ArticleType.NONE,
            pronunciationPersian = "گوتِن مورگِن", dariMeaning = "صبح بخیر",
            exampleGerman = "Guten Morgen zusammen!", exampleDari = "صبح بخیر به همه!", category = "احوال‌پرسی"
        ),
        WordItem(
            id = "w1_4", lessonId = 1, german = "Guten Abend", article = ArticleType.NONE,
            pronunciationPersian = "گوتِن آبِند", dariMeaning = "عصر بخیر / شام بخیر",
            exampleGerman = "Guten Abend, Herr Müller.", exampleDari = "عصر بخیر، آقای مولر.", category = "احوال‌پرسی"
        ),
        WordItem(
            id = "w1_5", lessonId = 1, german = "Gute Nacht", article = ArticleType.NONE,
            pronunciationPersian = "گوته ناخت", dariMeaning = "شب بخیر",
            exampleGerman = "Schlaf gut und gute Nacht!", exampleDari = "خوب بخوابی و شب بخیر!", category = "احوال‌پرسی"
        ),
        WordItem(
            id = "w1_6", lessonId = 1, german = "Auf Wiedersehen", article = ArticleType.NONE,
            pronunciationPersian = "آوف ویدِرزین", dariMeaning = "به امید دیدار / خداحافظ (رسمی)",
            exampleGerman = "Auf Wiedersehen, bis Montag!", exampleDari = "خداحافظ، تا روز دوشنبه!", category = "خداحافظی"
        ),
        WordItem(
            id = "w1_7", lessonId = 1, german = "Tschüss", article = ArticleType.NONE,
            pronunciationPersian = "چوس", dariMeaning = "خداحافظ (دوستانه)",
            exampleGerman = "Tschüss, bis später!", exampleDari = "خداحافظ، تا بعد!", category = "خداحافظی"
        ),
        WordItem(
            id = "w1_8", lessonId = 1, german = "heißen", article = ArticleType.NONE,
            pronunciationPersian = "هایسِن", dariMeaning = "نامیده شدن / نام داشتن",
            exampleGerman = "Ich heiße Maryam.", exampleDari = "نام من مریم است.", category = "افعال"
        ),
        WordItem(
            id = "w1_9", lessonId = 1, german = "sein", article = ArticleType.NONE,
            pronunciationPersian = "زاین", dariMeaning = "بودن (هست)",
            exampleGerman = "Ich bin Ali.", exampleDari = "من علی هستم.", category = "افعال"
        ),
        WordItem(
            id = "w1_10", lessonId = 1, german = "kommen", article = ArticleType.NONE,
            pronunciationPersian = "کومِن", dariMeaning = "آمدن / اهل جایی بودن",
            exampleGerman = "Woher kommst du?", exampleDari = "تو اهل کجایی؟", category = "افعال"
        ),
        WordItem(
            id = "w1_11", lessonId = 1, german = "wohnen", article = ArticleType.NONE,
            pronunciationPersian = "وونِن", dariMeaning = "سکونت داشتن / زندگی کردن",
            exampleGerman = "Ich wohne in Frankfurt.", exampleDari = "من در فرانکفورت زندگی می‌کنم.", category = "افعال"
        ),
        WordItem(
            id = "w1_12", lessonId = 1, german = "Deutschland", article = ArticleType.DAS,
            pronunciationPersian = "دویچلَند", dariMeaning = "کشور آلمان",
            exampleGerman = "Deutschland liegt in Europa.", exampleDari = "آلمان در اروپا واقع شده است.", category = "کشورها"
        ),
        WordItem(
            id = "w1_13", lessonId = 1, german = "Afghanistan", article = ArticleType.DAS,
            pronunciationPersian = "افغانستان", dariMeaning = "کشور افغانستان",
            exampleGerman = "Ich komme aus Afghanistan.", exampleDari = "من از افغانستان هستم.", category = "کشورها"
        ),
        WordItem(
            id = "w1_14", lessonId = 1, german = "die Schweiz", article = ArticleType.DIE,
            pronunciationPersian = "دی شوایتس", dariMeaning = "سوئیس (با حرف تعریف die)",
            exampleGerman = "Ich komme aus der Schweiz.", exampleDari = "من اهل سوئیس هستم.", category = "کشورها"
        ),
        WordItem(
            id = "w1_15", lessonId = 1, german = "Österreich", article = ArticleType.DAS,
            pronunciationPersian = "اوستِرایش", dariMeaning = "کشور اتریش",
            exampleGerman = "Wien ist die Hauptstadt von Österreich.", exampleDari = "وین پایتخت اتریش است.", category = "کشورها"
        ),
        WordItem(
            id = "w1_16", lessonId = 1, german = "Danke", article = ArticleType.NONE,
            pronunciationPersian = "دانکه", dariMeaning = "تشکر / ممنون",
            exampleGerman = "Vielen Dank für die Hilfe!", exampleDari = "بسیار تشکر برای کمک شما!", category = "اصطلاحات"
        ),
        WordItem(
            id = "w1_17", lessonId = 1, german = "Bitte", article = ArticleType.NONE,
            pronunciationPersian = "بیته", dariMeaning = "لطفاً / خواهش می‌کنم",
            exampleGerman = "Bitte schön!", exampleDari = "خواهش می‌کنم (قابل شما را ندارد)!", category = "اصطلاحات"
        ),

        // ================= LESSON 2: Contacts & Professions =================
        WordItem(
            id = "w2_1", lessonId = 2, german = "Lehrer", article = ArticleType.DER,
            pronunciationPersian = "لِرِر", dariMeaning = "استاد / معلم (مرد)",
            exampleGerman = "Er ist Deutschlehrer von Beruf.", exampleDari = "او شغلش معلمی زبان آلمانی است.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_2", lessonId = 2, german = "Lehrerin", article = ArticleType.DIE,
            pronunciationPersian = "لِرِرین", dariMeaning = "معلمه / استاد (زن)",
            exampleGerman = "Meine Lehrerin ist sehr freundlich.", exampleDari = "معلم زن من بسیار مهربان است.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_3", lessonId = 2, german = "Arzt", article = ArticleType.DER,
            pronunciationPersian = "آرتست", dariMeaning = "داکتر / پزشک (مرد)",
            exampleGerman = "Der Arzt untersucht den Patienten.", exampleDari = "داکتر مریض را معاینه می‌کند.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_4", lessonId = 2, german = "Ärztin", article = ArticleType.DIE,
            pronunciationPersian = "اِرتستین", dariMeaning = "داکتر / پزشک (زن)",
            exampleGerman = "Sie arbeitet als Ärztin im Krankenhaus.", exampleDari = "او به عنوان داکتر در شفاخانه کار می‌کند.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_5", lessonId = 2, german = "Ingenieur", article = ArticleType.DER,
            pronunciationPersian = "اینجِنیور", dariMeaning = "انجنیر / مهندس",
            exampleGerman = "Ahmad ist Ingenieur von Beruf.", exampleDari = "احمد شغلش انجنیری است.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_6", lessonId = 2, german = "Student", article = ArticleType.DER,
            pronunciationPersian = "شتودِنت", dariMeaning = "محصل / دانشجو (پسر)",
            exampleGerman = "Er ist Student an der Universität.", exampleDari = "او محصل پوهنتون (دانشگاه) است.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_7", lessonId = 2, german = "Studentin", article = ArticleType.DIE,
            pronunciationPersian = "شتودِنتین", dariMeaning = "محصله / دانشجو (دختر)",
            exampleGerman = "Fatima ist Studentin.", exampleDari = "فاطمه محصله است.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_8", lessonId = 2, german = "Handynummer", article = ArticleType.DIE,
            pronunciationPersian = "هَندی‌نومِر", dariMeaning = "شماره مبایل / تلفن همراه",
            exampleGerman = "Wie ist deine Handynummer?", exampleDari = "شماره مبایل تو چند است؟", category = "ارتباطات"
        ),
        WordItem(
            id = "w2_9", lessonId = 2, german = "E-Mail-Adresse", article = ArticleType.DIE,
            pronunciationPersian = "ایمیل آدرِسه", dariMeaning = "آدرس ایمیل",
            exampleGerman = "Schreib mir deine E-Mail-Adresse.", exampleDari = "آدرس ایمیلت را برایم بنویس.", category = "ارتباطات"
        ),
        WordItem(
            id = "w2_10", lessonId = 2, german = "Punkt", article = ArticleType.DER,
            pronunciationPersian = "پونکت", dariMeaning = "نقطه (دات .)",
            exampleGerman = "gmail Punkt com", exampleDari = "جیمیل دات کام", category = "ارتباطات"
        ),
        WordItem(
            id = "w2_11", lessonId = 2, german = "haben", article = ArticleType.NONE,
            pronunciationPersian = "هابِن", dariMeaning = "داشتن",
            exampleGerman = "Ich habe eine Frage.", exampleDari = "من یک سوال دارم.", category = "افعال"
        ),
        WordItem(
            id = "w2_12", lessonId = 2, german = "lernen", article = ArticleType.NONE,
            pronunciationPersian = "لِرنِن", dariMeaning = "یاد گرفتن / آموختن",
            exampleGerman = "Wir lernen Deutsch.", exampleDari = "ما زبان آلمانی می‌آموزیم.", category = "افعال"
        ),
        WordItem(
            id = "w2_13", lessonId = 2, german = "sprechen", article = ArticleType.NONE,
            pronunciationPersian = "شپرِشِن", dariMeaning = "صحبت کردن / گپ زدن",
            exampleGerman = "Sprichst du Dari?", exampleDari = "آیا دری صحبت می‌کنی؟", category = "افعال"
        ),
        WordItem(
            id = "w2_14", lessonId = 2, german = "Beruf", article = ArticleType.DER,
            pronunciationPersian = "بِروف", dariMeaning = "شغل / مسلک",
            exampleGerman = "Was bist du von Beruf?", exampleDari = "شغل تو چیست؟", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_15", lessonId = 2, german = "Arbeit", article = ArticleType.DIE,
            pronunciationPersian = "آربایت", dariMeaning = "کار / وظیفه",
            exampleGerman = "Ich suche eine Arbeit.", exampleDari = "من به دنبال یک کار هستم.", category = "شغل‌ها"
        ),
        WordItem(
            id = "w2_16", lessonId = 2, german = "Verkäufer", article = ArticleType.DER,
            pronunciationPersian = "فِرکویفِر", dariMeaning = "فروشنده / دکاندار (مرد)",
            exampleGerman = "Der Verkäufer hilft mir.", exampleDari = "فروشنده به من کمک می‌کند.", category = "شغل‌ها"
        ),

        // ================= LESSON 3: Objects & Articles =================
        WordItem(
            id = "w3_1", lessonId = 3, german = "Tisch", article = ArticleType.DER,
            pronunciationPersian = "تیش", dariMeaning = "میز (مذکر: der)",
            exampleGerman = "Der Tisch ist aus Holz.", exampleDari = "میز از چوب است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_2", lessonId = 3, german = "Stuhl", article = ArticleType.DER,
            pronunciationPersian = "شتول", dariMeaning = "چوکی / صندلی (مذکر: der)",
            exampleGerman = "Hier ist ein freier Stuhl.", exampleDari = "اینجا یک چوکی خالی است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_3", lessonId = 3, german = "Stift", article = ArticleType.DER,
            pronunciationPersian = "شتیفت", dariMeaning = "قلم / خودکار (مذکر: der)",
            exampleGerman = "Hast du einen Stift?", exampleDari = "آیا قلم داری؟", category = "وسایل"
        ),
        WordItem(
            id = "w3_4", lessonId = 3, german = "Computer", article = ArticleType.DER,
            pronunciationPersian = "کامپیوتر", dariMeaning = "کمپیوتر (مذکر: der)",
            exampleGerman = "Der Computer ist neu.", exampleDari = "کمپیوتر نو است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_5", lessonId = 3, german = "Schlüssel", article = ArticleType.DER,
            pronunciationPersian = "شلوُسل", dariMeaning = "کلید (مذکر: der)",
            exampleGerman = "Wo ist mein Schlüssel?", exampleDari = "کلید من کجاست؟", category = "وسایل"
        ),
        WordItem(
            id = "w3_6", lessonId = 3, german = "Buch", article = ArticleType.DAS,
            pronunciationPersian = "بوخ", dariMeaning = "کتاب (خنثی: das)",
            exampleGerman = "Das Buch ist sehr interessant.", exampleDari = "کتاب بسیار جالب است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_7", lessonId = 3, german = "Handy", article = ArticleType.DAS,
            pronunciationPersian = "هَندی", dariMeaning = "تلفن مبایل (خنثی: das)",
            exampleGerman = "Mein Handy klingelt.", exampleDari = "مبایل من زنگ می‌خورد.", category = "وسایل"
        ),
        WordItem(
            id = "w3_8", lessonId = 3, german = "Heft", article = ArticleType.DAS,
            pronunciationPersian = "هِفت", dariMeaning = "کتابچه / دفترچه (خنثی: das)",
            exampleGerman = "Ich schreibe die Wörter ins Heft.", exampleDari = "کلمات را در کتابچه می‌نویسم.", category = "وسایل"
        ),
        WordItem(
            id = "w3_9", lessonId = 3, german = "Bild", article = ArticleType.DAS,
            pronunciationPersian = "بیلد", dariMeaning = "عکس / تصویر (خنثی: das)",
            exampleGerman = "Das Bild an der Wand ist schön.", exampleDari = "تصویر روی دیوار مقبول است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_10", lessonId = 3, german = "Tasche", article = ArticleType.DIE,
            pronunciationPersian = "تاشه", dariMeaning = "بکس / کیف دستی (مؤنث: die)",
            exampleGerman = "Die Tasche ist schwer.", exampleDari = "بکس سنگین است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_11", lessonId = 3, german = "Brille", article = ArticleType.DIE,
            pronunciationPersian = "بریله", dariMeaning = "عینک (مؤنث: die)",
            exampleGerman = "Ich brauche meine Brille.", exampleDari = "من به عینکم نیاز دارم.", category = "وسایل"
        ),
        WordItem(
            id = "w3_12", lessonId = 3, german = "Flasche", article = ArticleType.DIE,
            pronunciationPersian = "فلاشه", dariMeaning = "بوتل / شیشه آب (مؤنث: die)",
            exampleGerman = "Eine Flasche Wasser, bitte.", exampleDari = "یک بوتل آب، لطفاً.", category = "وسایل"
        ),
        WordItem(
            id = "w3_13", lessonId = 3, german = "Uhr", article = ArticleType.DIE,
            pronunciationPersian = "اوور", dariMeaning = "ساعت (مؤنث: die)",
            exampleGerman = "Wie viel Uhr ist es?", exampleDari = "ساعت چند است؟", category = "وسایل"
        ),
        WordItem(
            id = "w3_14", lessonId = 3, german = "Lampe", article = ArticleType.DIE,
            pronunciationPersian = "لامپه", dariMeaning = "چراغ / گروپ (مؤنث: die)",
            exampleGerman = "Die Lampe ist sehr hell.", exampleDari = "چراغ بسیار روشن است.", category = "وسایل"
        ),
        WordItem(
            id = "w3_15", lessonId = 3, german = "Wörterbuch", article = ArticleType.DAS,
            pronunciationPersian = "وورتِربوخ", dariMeaning = "دیکشنری / فرهنگ لغت (خنثی: das)",
            exampleGerman = "Ich schlage das Wort im Wörterbuch nach.", exampleDari = "کلمه را در دیکشنری جستجو می‌کنم.", category = "وسایل"
        ),
        WordItem(
            id = "w3_16", lessonId = 3, german = "Papier", article = ArticleType.DAS,
            pronunciationPersian = "پاپیِر", dariMeaning = "کاغذ (خنثی: das)",
            exampleGerman = "Haben Sie ein Blatt Papier?", exampleDari = "آیا یک ورق کاغذ دارید؟", category = "وسایل"
        ),

        // ================= LESSON 4: Ordering in Cafe =================
        WordItem(
            id = "w4_1", lessonId = 4, german = "Kaffee", article = ArticleType.DER,
            pronunciationPersian = "کافه", dariMeaning = "قهوه (مذکر: der)",
            exampleGerman = "Einen Kaffee mit Zucker, bitte.", exampleDari = "یک قهوه با شکر، لطفاً.", category = "نوشیدنی"
        ),
        WordItem(
            id = "w4_2", lessonId = 4, german = "Tee", article = ArticleType.DER,
            pronunciationPersian = "تِه", dariMeaning = "چای (مذکر: der)",
            exampleGerman = "Trinkst du schwarzen oder grünen Tee?", exampleDari = "چای سیاه می‌نوشی یا سبز؟", category = "نوشیدنی"
        ),
        WordItem(
            id = "w4_3", lessonId = 4, german = "Saft", article = ArticleType.DER,
            pronunciationPersian = "زافت", dariMeaning = "آب‌میوه (مذکر: der)",
            exampleGerman = "Ich trinke gern Orangensaft.", exampleDari = "من آب نارنج (پرتقال) دوست دارم.", category = "نوشیدنی"
        ),
        WordItem(
            id = "w4_4", lessonId = 4, german = "Wasser", article = ArticleType.DAS,
            pronunciationPersian = "واسِر", dariMeaning = "آب (خنثی: das)",
            exampleGerman = "Ein Glas Wasser mit Gas, bitte.", exampleDari = "یک گیلاس آب گازدار، لطفاً.", category = "نوشیدنی"
        ),
        WordItem(
            id = "w4_5", lessonId = 4, german = "Brötchen", article = ArticleType.DAS,
            pronunciationPersian = "بروتشِن", dariMeaning = "نان کوچک گرد (ساندویچی) (خنثی: das)",
            exampleGerman = "Zwei Brötchen bitte!", exampleDari = "دو دانه نان کوچک لطفاً!", category = "خوراکی"
        ),
        WordItem(
            id = "w4_6", lessonId = 4, german = "Kuchen", article = ArticleType.DER,
            pronunciationPersian = "کوخِن", dariMeaning = "کیک (مذکر: der)",
            exampleGerman = "Der Schokoladenkuchen ist lecker.", exampleDari = "کیک شکلاتی مزه‌دار است.", category = "خوراکی"
        ),
        WordItem(
            id = "w4_7", lessonId = 4, german = "Rechnung", article = ArticleType.DIE,
            pronunciationPersian = "رِشنونگ", dariMeaning = "صورتحساب / بل کافه (مؤنث: die)",
            exampleGerman = "Die Rechnung, bitte!", exampleDari = "صورتحساب، لطفاً!", category = "کافه"
        ),
        WordItem(
            id = "w4_8", lessonId = 4, german = "Zucker", article = ArticleType.DER,
            pronunciationPersian = "تسوکِر", dariMeaning = "شکر / بوره (مذکر: der)",
            exampleGerman = "Kaffee ohne Zucker, bitte.", exampleDari = "قهوه بدون بوره (شکر)، لطفاً.", category = "خوراکی"
        ),
        WordItem(
            id = "w4_9", lessonId = 4, german = "Milch", article = ArticleType.DIE,
            pronunciationPersian = "میلش", dariMeaning = "شیر (مؤنث: die)",
            exampleGerman = "Kaffee mit etwas Milch.", exampleDari = "قهوه با کمی شیر.", category = "نوشیدنی"
        ),
        WordItem(
            id = "w4_10", lessonId = 4, german = "möchten", article = ArticleType.NONE,
            pronunciationPersian = "مِشتِن", dariMeaning = "میل داشتن / خواستن",
            exampleGerman = "Was möchten Sie trinken?", exampleDari = "چه چیزی میل دارید بنوشید؟", category = "افعال"
        ),
        WordItem(
            id = "w4_11", lessonId = 4, german = "bezahlen", article = ArticleType.NONE,
            pronunciationPersian = "بِتسالِن", dariMeaning = "پرداخت کردن / پول دادن",
            exampleGerman = "Wir möchten bitte bezahlen.", exampleDari = "ما می‌خواهیم حساب را بپردازیم.", category = "افعال"
        ),
        WordItem(
            id = "w4_12", lessonId = 4, german = "kosten", article = ArticleType.NONE,
            pronunciationPersian = "کوستِن", dariMeaning = "قیمت داشتن / ارزیدن",
            exampleGerman = "Wie viel kostet der Tee?", exampleDari = "قیمت چای چقدر است؟", category = "افعال"
        ),
        WordItem(
            id = "w4_13", lessonId = 4, german = "zusammen", article = ArticleType.NONE,
            pronunciationPersian = "تسوزامِن", dariMeaning = "با هم / یکجا",
            exampleGerman = "Wir zahlen zusammen.", exampleDari = "ما با هم یکجا پرداخت می‌کنیم.", category = "اصطلاحات"
        ),
        WordItem(
            id = "w4_14", lessonId = 4, german = "getrennt", article = ArticleType.NONE,
            pronunciationPersian = "گِترِنت", dariMeaning = "جداگانه / علیحده",
            exampleGerman = "Getrennt oder zusammen?", exampleDari = "جداگانه یا یکجا؟", category = "اصطلاحات"
        ),
        WordItem(
            id = "w4_15", lessonId = 4, german = "Euro", article = ArticleType.DER,
            pronunciationPersian = "اویو", dariMeaning = "یورو (واحد پول اروپا)",
            exampleGerman = "Das kostet drei Euro.", exampleDari = "این سه یورو قیمت دارد.", category = "پول"
        ),
        WordItem(
            id = "w4_16", lessonId = 4, german = "Cent", article = ArticleType.DER,
            pronunciationPersian = "سِنت", dariMeaning = "سنت (یک صدم یورو)",
            exampleGerman = "Fünfzig Cent Rückgeld.", exampleDari = "پنجاه سنت باقی پول.", category = "پول"
        ),

        // ================= LESSON 5: Days & Routine =================
        WordItem(
            id = "w5_1", lessonId = 5, german = "Montag", article = ArticleType.DER,
            pronunciationPersian = "مونتاگ", dariMeaning = "روز دوشنبه",
            exampleGerman = "Am Montag habe ich Schule.", exampleDari = "روز دوشنبه مکتب (مدرسه) دارم.", category = "روزها"
        ),
        WordItem(
            id = "w5_2", lessonId = 5, german = "Dienstag", article = ArticleType.DER,
            pronunciationPersian = "دینستاگ", dariMeaning = "روز سه‌شنبه",
            exampleGerman = "Dienstag gehe ich zum Arzt.", exampleDari = "سه‌شنبه نزد داکتر می‌روم.", category = "روزها"
        ),
        WordItem(
            id = "w5_3", lessonId = 5, german = "Mittwoch", article = ArticleType.DER,
            pronunciationPersian = "میت‌وُوخ", dariMeaning = "روز چهارشنبه",
            exampleGerman = "Mittwoch ist die Mitte der Woche.", exampleDari = "چهارشنبه وسط هفته است.", category = "روزها"
        ),
        WordItem(
            id = "w5_4", lessonId = 5, german = "Donnerstag", article = ArticleType.DER,
            pronunciationPersian = "دونِرستاگ", dariMeaning = "روز پنج‌شنبه",
            exampleGerman = "Am Donnerstag lerne ich Deutsch.", exampleDari = "روز پنج‌شنبه آلمانی می‌خوانم.", category = "روزها"
        ),
        WordItem(
            id = "w5_5", lessonId = 5, german = "Freitag", article = ArticleType.DER,
            pronunciationPersian = "فرایتاگ", dariMeaning = "روز جمعه",
            exampleGerman = "Freitag ist der letzte Arbeitstag.", exampleDari = "جمعه آخرین روز کاری است.", category = "روزها"
        ),
        WordItem(
            id = "w5_6", lessonId = 5, german = "Samstag", article = ArticleType.DER,
            pronunciationPersian = "زامستاگ", dariMeaning = "روز شنبه",
            exampleGerman = "Am Samstag schlafe ich lange.", exampleDari = "روز شنبه تا دیر وقت می‌خوابم.", category = "روزها"
        ),
        WordItem(
            id = "w5_7", lessonId = 5, german = "Sonntag", article = ArticleType.DER,
            pronunciationPersian = "زونتاگ", dariMeaning = "روز یکشنبه (تعطیلی عمومی در آلمان)",
            exampleGerman = "Sonntags sind die Geschäfte zu.", exampleDari = "روزهای یکشنبه دکان‌ها بسته هستند.", category = "روزها"
        ),
        WordItem(
            id = "w5_8", lessonId = 5, german = "Woche", article = ArticleType.DIE,
            pronunciationPersian = "ووخه", dariMeaning = "هفته",
            exampleGerman = "Eine schöne Woche wünsche ich dir!", exampleDari = "یک هفته خوب برایت آرزو دارم!", category = "زمان"
        ),
        WordItem(
            id = "w5_9", lessonId = 5, german = "Wochenende", article = ArticleType.DAS,
            pronunciationPersian = "ووخِن‌اِنده", dariMeaning = "آخر هفته (شنبه و یکشنبه)",
            exampleGerman = "Schönes Wochenende!", exampleDari = "آخر هفته خوشی داشته باشید!", category = "زمان"
        ),
        WordItem(
            id = "w5_10", lessonId = 5, german = "Morgen", article = ArticleType.DER,
            pronunciationPersian = "مورگِن", dariMeaning = "صبح",
            exampleGerman = "Am Morgen trinke ich Kaffee.", exampleDari = "صبح‌ها قهوه می‌نوشم.", category = "زمان"
        ),
        WordItem(
            id = "w5_11", lessonId = 5, german = "Abend", article = ArticleType.DER,
            pronunciationPersian = "آبِند", dariMeaning = "شام / شب",
            exampleGerman = "Am Abend lerne ich Vokabeln.", exampleDari = "شب‌ها لغت یاد می‌گیرم.", category = "زمان"
        ),
        WordItem(
            id = "w5_12", lessonId = 5, german = "aufstehen", article = ArticleType.NONE,
            pronunciationPersian = "آوف‌شتین", dariMeaning = "از خواب بیدار شدن / برخاستن",
            exampleGerman = "Ich stehe um 6 Uhr auf.", exampleDari = "من ساعت ۶ صبح از خواب بلند می‌شوم.", category = "افعال"
        ),
        WordItem(
            id = "w5_13", lessonId = 5, german = "frühstücken", article = ArticleType.NONE,
            pronunciationPersian = "فروشتوکِن", dariMeaning = "صبحانه / ناشتا خوردن",
            exampleGerman = "Wann frühstückst du?", exampleDari = "چه وقت ناشتا می‌خوری؟", category = "افعال"
        ),
        WordItem(
            id = "w5_14", lessonId = 5, german = "arbeiten", article = ArticleType.NONE,
            pronunciationPersian = "آربایتِن", dariMeaning = "کار کردن",
            exampleGerman = "Er arbeitet acht Stunden pro Tag.", exampleDari = "او هشت ساعت در روز کار می‌کند.", category = "افعال"
        ),
        WordItem(
            id = "w5_15", lessonId = 5, german = "einkaufen", article = ArticleType.NONE,
            pronunciationPersian = "آین‌کویفِن", dariMeaning = "سودا / خرید کردن",
            exampleGerman = "Ich kaufe im Supermarkt ein.", exampleDari = "من از سوپرمارکت خرید می‌کنم.", category = "افعال"
        ),
        WordItem(
            id = "w5_16", lessonId = 5, german = "schlafen", article = ArticleType.NONE,
            pronunciationPersian = "شلافِن", dariMeaning = "خوابیدن",
            exampleGerman = "Ich gehe um 22 Uhr schlafen.", exampleDari = "ساعت ۱۰ شب به خواب می‌روم.", category = "افعال"
        ),

        // ================= LESSON 6: Food & Taste =================
        WordItem(
            id = "w6_1", lessonId = 6, german = "Brot", article = ArticleType.DAS,
            pronunciationPersian = "بروت", dariMeaning = "نان (خنثی: das)",
            exampleGerman = "Frisches Brot riecht wunderbar.", exampleDari = "نان تازه بوی عالی دارد.", category = "خوراکی"
        ),
        WordItem(
            id = "w6_2", lessonId = 6, german = "Apfel", article = ArticleType.DER,
            pronunciationPersian = "آپفِل", dariMeaning = "سیب (مذکر: der)",
            exampleGerman = "Ein Apfel am Tag ist gesund.", exampleDari = "یک دانه سیب در روز برای صحت خوب است.", category = "میوه"
        ),
        WordItem(
            id = "w6_3", lessonId = 6, german = "Banane", article = ArticleType.DIE,
            pronunciationPersian = "بانانه", dariMeaning = "کیله / موز (مؤنث: die)",
            exampleGerman = "Die Kinder essen Bananen.", exampleDari = "اطفال کیله می‌خورند.", category = "میوه"
        ),
        WordItem(
            id = "w6_4", lessonId = 6, german = "Käse", article = ArticleType.DER,
            pronunciationPersian = "کِزه", dariMeaning = "پنیر (مذکر: der)",
            exampleGerman = "Ich mag Brot mit Käse.", exampleDari = "من نان با پنیر را دوست دارم.", category = "لبنیات"
        ),
        WordItem(
            id = "w6_5", lessonId = 6, german = "Fleisch", article = ArticleType.DAS,
            pronunciationPersian = "فلایش", dariMeaning = "گوشت (خنثی: das)",
            exampleGerman = "Ich esse nur Halal-Fleisch.", exampleDari = "من فقط گوشت حلال می‌خورم.", category = "خوراکی"
        ),
        WordItem(
            id = "w6_6", lessonId = 6, german = "Hähnchen", article = ArticleType.DAS,
            pronunciationPersian = "هِینشِن", dariMeaning = "گوشت مرغ (خنثی: das)",
            exampleGerman = "Heute koche ich Reis mit Hähnchen.", exampleDari = "امروز برنج با مرغ می‌پزم.", category = "خوراکی"
        ),
        WordItem(
            id = "w6_7", lessonId = 6, german = "Reis", article = ArticleType.DER,
            pronunciationPersian = "رایس", dariMeaning = "برنج / پلو (مذکر: der)",
            exampleGerman = "In Afghanistan essen wir gern Reis.", exampleDari = "در افغانستان ما برنج را بسیار دوست داریم.", category = "خوراکی"
        ),
        WordItem(
            id = "w6_8", lessonId = 6, german = "Gemüse", article = ArticleType.DAS,
            pronunciationPersian = "گِموزه", dariMeaning = "ترکاری / سبزیجات (خنثی: das)",
            exampleGerman = "Gemüse hat viele Vitamine.", exampleDari = "ترکاری (سبزیجات) ویتامین‌های زیادی دارد.", category = "سبزیجات"
        ),
        WordItem(
            id = "w6_9", lessonId = 6, german = "Obst", article = ArticleType.DAS,
            pronunciationPersian = "اوبست", dariMeaning = "میوه‌جات (خنثی: das)",
            exampleGerman = "Frisches Obst schmeckt am besten.", exampleDari = "میوه تازه از همه خوشمزه‌تر است.", category = "میوه"
        ),
        WordItem(
            id = "w6_10", lessonId = 6, german = "lecker", article = ArticleType.NONE,
            pronunciationPersian = "لِکِر", dariMeaning = "مزه‌دار / لذیذ",
            exampleGerman = "Das Essen ist wirklich lecker!", exampleDari = "غذا واقعاً مزه‌دار است!", category = "طعم"
        ),
        WordItem(
            id = "w6_11", lessonId = 6, german = "süß", article = ArticleType.NONE,
            pronunciationPersian = "زوس", dariMeaning = "شیرین",
            exampleGerman = "Dieser Tee ist sehr süß.", exampleDari = "این چای بسیار شیرین است.", category = "طعم"
        ),
        WordItem(
            id = "w6_12", lessonId = 6, german = "salzig", article = ArticleType.NONE,
            pronunciationPersian = "زالتسیش", dariMeaning = "شور / نمکی",
            exampleGerman = "Die Suppe ist ein bisschen salzig.", exampleDari = "شوربا (سوپ) کمی نمکی است.", category = "طعم"
        ),
        WordItem(
            id = "w6_13", lessonId = 6, german = "schmecken", article = ArticleType.NONE,
            pronunciationPersian = "شمِکِن", dariMeaning = "طعم و مزه دادن",
            exampleGerman = "Schmeckt es dir?", exampleDari = "آیا خوشمزه است؟ (مزه می‌دهد؟)", category = "افعال"
        ),
        WordItem(
            id = "w6_14", lessonId = 6, german = "essen", article = ArticleType.NONE,
            pronunciationPersian = "اِسِن", dariMeaning = "غذا خوردن",
            exampleGerman = "Was isst du zum Mittagessen?", exampleDari = "برای نان چاشت چه می‌خوری؟", category = "افعال"
        ),
        WordItem(
            id = "w6_15", lessonId = 6, german = "trinken", article = ArticleType.NONE,
            pronunciationPersian = "ترینکِن", dariMeaning = "نوشیدن",
            exampleGerman = "Trinkst du genug Wasser?", exampleDari = "آیا به اندازه کافی آب می‌نوشی؟", category = "افعال"
        ),
        WordItem(
            id = "w6_16", lessonId = 6, german = "gern", article = ArticleType.NONE,
            pronunciationPersian = "گِرن", dariMeaning = "با کمال میل / با علاقه (دوست داشتن کاری)",
            exampleGerman = "Ich trinke gern Tee.", exampleDari = "من نوشیدن چای را دوست دارم.", category = "اصطلاحات"
        ),

        // ================= LESSON 7: My Family =================
        WordItem(
            id = "w7_1", lessonId = 7, german = "Vater", article = ArticleType.DER,
            pronunciationPersian = "فاتِر", dariMeaning = "پدر (مذکر: der)",
            exampleGerman = "Mein Vater ist 50 Jahre alt.", exampleDari = "پدر من ۵۰ ساله است.", category = "خانواده"
        ),
        WordItem(
            id = "w7_2", lessonId = 7, german = "Mutter", article = ArticleType.DIE,
            pronunciationPersian = "موتِر", dariMeaning = "مادر (مؤنث: die)",
            exampleGerman = "Meine Mutter kocht sehr gut.", exampleDari = "مادرم بسیار خوب آشپزی می‌کند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_3", lessonId = 7, german = "Eltern", article = ArticleType.PLURAL_DIE,
            pronunciationPersian = "اِلتِرن", dariMeaning = "والدین / پدر و مادر (جمع)",
            exampleGerman = "Meine Eltern leben in Kabul.", exampleDari = "والدینم در کابل زندگی می‌کنند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_4", lessonId = 7, german = "Sohn", article = ArticleType.DER,
            pronunciationPersian = "زون", dariMeaning = "پسر (فرزند مذکر: der)",
            exampleGerman = "Mein Sohn geht in die Schule.", exampleDari = "پسرم به مکتب می‌رود.", category = "خانواده"
        ),
        WordItem(
            id = "w7_5", lessonId = 7, german = "Tochter", article = ArticleType.DIE,
            pronunciationPersian = "توختِر", dariMeaning = "دختر (فرزند مؤنث: die)",
            exampleGerman = "Meine Tochter lernt fleißig.", exampleDari = "دخترم با پشتکار درس می‌خواند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_6", lessonId = 7, german = "Bruder", article = ArticleType.DER,
            pronunciationPersian = "برودِر", dariMeaning = "برادر (مذکر: der)",
            exampleGerman = "Ich habe einen älteren Bruder.", exampleDari = "من یک برادر بزرگتر دارم.", category = "خانواده"
        ),
        WordItem(
            id = "w7_7", lessonId = 7, german = "Schwester", article = ArticleType.DIE,
            pronunciationPersian = "شووِستِر", dariMeaning = "خواهر (مؤنث: die)",
            exampleGerman = "Meine Schwester studiert Medizin.", exampleDari = "خواهرم طب می‌خواند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_8", lessonId = 7, german = "Geschwister", article = ArticleType.PLURAL_DIE,
            pronunciationPersian = "گِشویشتِر", dariMeaning = "خواهران و برادران (جمع)",
            exampleGerman = "Hast du Geschwister?", exampleDari = "آیا خواهر و برادر داری؟", category = "خانواده"
        ),
        WordItem(
            id = "w7_9", lessonId = 7, german = "Großvater", article = ArticleType.DER,
            pronunciationPersian = "گروس‌فاتِر (اوپا)", dariMeaning = "پدرکلان / پدربزرگ",
            exampleGerman = "Mein Opa erzählt tolle Geschichten.", exampleDari = "پدرکلانم داستان‌های عالی تعریف می‌کند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_10", lessonId = 7, german = "Großmutter", article = ArticleType.DIE,
            pronunciationPersian = "گروس‌موتِر (اوما)", dariMeaning = "مادرکلان / مادربزرگ",
            exampleGerman = "Meine Oma backt leckere Kekse.", exampleDari = "مادرکلانم کلچه‌های مزه‌دار می‌پزد.", category = "خانواده"
        ),
        WordItem(
            id = "w7_11", lessonId = 7, german = "Kind", article = ArticleType.DAS,
            pronunciationPersian = "کیند", dariMeaning = "طِفل / کودک (خنثی: das)",
            exampleGerman = "Das Kind spielt im Garten.", exampleDari = "طفل در حویلی (باغچه) بازی می‌کند.", category = "خانواده"
        ),
        WordItem(
            id = "w7_12", lessonId = 7, german = "Baby", article = ArticleType.DAS,
            pronunciationPersian = "بِیبی", dariMeaning = "نوزاد / کودک خردسال (خنثی: das)",
            exampleGerman = "Das Baby schläft ruhig.", exampleDari = "نوزاد آرام خوابیده است.", category = "خانواده"
        ),
        WordItem(
            id = "w7_13", lessonId = 7, german = "Mann", article = ArticleType.DER,
            pronunciationPersian = "مان", dariMeaning = "شوهر / مرد (مذکر: der)",
            exampleGerman = "Das ist mein Mann.", exampleDari = "این شوهر من است.", category = "خانواده"
        ),
        WordItem(
            id = "w7_14", lessonId = 7, german = "Frau", article = ArticleType.DIE,
            pronunciationPersian = "فراو", dariMeaning = "خانم / همسر / زن (مؤنث: die)",
            exampleGerman = "Das ist meine Frau.", exampleDari = "این خانم من است.", category = "خانواده"
        ),
        WordItem(
            id = "w7_15", lessonId = 7, german = "mein", article = ArticleType.NONE,
            pronunciationPersian = "ماین", dariMeaning = "از من / مال من (برای مذکر و خنثی)",
            exampleGerman = "Mein Bruder ist hier.", exampleDari = "برادرم اینجا است.", category = "صفات ملکی"
        ),
        WordItem(
            id = "w7_16", lessonId = 7, german = "meine", article = ArticleType.NONE,
            pronunciationPersian = "ماینه", dariMeaning = "از من (برای مؤنث و جمع)",
            exampleGerman = "Meine Familie ist groß.", exampleDari = "فامیل من کلان (بزرگ) است.", category = "صفات ملکی"
        ),

        // ================= LESSON 8: Housing =================
        WordItem(
            id = "w8_1", lessonId = 8, german = "Wohnung", article = ArticleType.DIE,
            pronunciationPersian = "وونونگ", dariMeaning = "آپارتمان / خانه (مؤنث: die)",
            exampleGerman = "Die Wohnung hat drei Zimmer.", exampleDari = "آپارتمان سه اتاق دارد.", category = "مسکن"
        ),
        WordItem(
            id = "w8_2", lessonId = 8, german = "Haus", article = ArticleType.DAS,
            pronunciationPersian = "هاوس", dariMeaning = "حویلی / خانه ویلایی (خنثی: das)",
            exampleGerman = "Das Haus hat einen schönen Garten.", exampleDari = "خانه یک حویلی (باغچه) قشنگ دارد.", category = "مسکن"
        ),
        WordItem(
            id = "w8_3", lessonId = 8, german = "Zimmer", article = ArticleType.DAS,
            pronunciationPersian = "تسیمِر", dariMeaning = "اتاق (خنثی: das)",
            exampleGerman = "Mein Zimmer ist gemütlich.", exampleDari = "اتاق من راحت و دنج است.", category = "مسکن"
        ),
        WordItem(
            id = "w8_4", lessonId = 8, german = "Küche", article = ArticleType.DIE,
            pronunciationPersian = "کوشه", dariMeaning = "آشپزخانه (مؤنث: die)",
            exampleGerman = "In der Küche kochen wir.", exampleDari = "در آشپزخانه غذا می‌پزیم.", category = "مسکن"
        ),
        WordItem(
            id = "w8_5", lessonId = 8, german = "Bad", article = ArticleType.DAS,
            pronunciationPersian = "باد", dariMeaning = "تشناب و حمام (خنثی: das)",
            exampleGerman = "Das Bad hat eine Badewanne.", exampleDari = "حمام وان دارد.", category = "مسکن"
        ),
        WordItem(
            id = "w8_6", lessonId = 8, german = "Wohnzimmer", article = ArticleType.DAS,
            pronunciationPersian = "وون‌تسیمِر", dariMeaning = "اتاق نشیمن / سالون (خنثی: das)",
            exampleGerman = "Im Wohnzimmer steht ein Fernseher.", exampleDari = "در سالون یک تلویزیون قرار دارد.", category = "مسکن"
        ),
        WordItem(
            id = "w8_7", lessonId = 8, german = "Schlafzimmer", article = ArticleType.DAS,
            pronunciationPersian = "شلاف‌تسیمِر", dariMeaning = "اتاق خواب (خنثی: das)",
            exampleGerman = "Das Schlafzimmer ist sehr ruhig.", exampleDari = "اتاق خواب بسیار آرام است.", category = "مسکن"
        ),
        WordItem(
            id = "w8_8", lessonId = 8, german = "Balkon", article = ArticleType.DER,
            pronunciationPersian = "بالکون", dariMeaning = "بالکن / ایوان (مذکر: der)",
            exampleGerman = "Wir sitzen gern auf dem Balkon.", exampleDari = "ما نشستن در بالکن را دوست داریم.", category = "مسکن"
        ),
        WordItem(
            id = "w8_9", lessonId = 8, german = "Bett", article = ArticleType.DAS,
            pronunciationPersian = "بِت", dariMeaning = "تخت خواب / چپرکت (خنثی: das)",
            exampleGerman = "Das Bett ist sehr bequem.", exampleDari = "تخت خواب بسیار آرام‌بخش است.", category = "وسایل خانه"
        ),
        WordItem(
            id = "w8_10", lessonId = 8, german = "Sofa", article = ArticleType.DAS,
            pronunciationPersian = "زوفا", dariMeaning = "کوچ / کاناپه (خنثی: das)",
            exampleGerman = "Ein gemütliches Sofa.", exampleDari = "یک کوچ راحت.", category = "وسایل خانه"
        ),
        WordItem(
            id = "w8_11", lessonId = 8, german = "Schrank", article = ArticleType.DER,
            pronunciationPersian = "شرانک", dariMeaning = "الماری / کمد (مذکر: der)",
            exampleGerman = "Der Schrank für Kleidung.", exampleDari = "الماری برای لباس‌ها.", category = "وسایل خانه"
        ),
        WordItem(
            id = "w8_12", lessonId = 8, german = "groß", article = ArticleType.NONE,
            pronunciationPersian = "گروس", dariMeaning = "کلان / بزرگ",
            exampleGerman = "Die Wohnung ist sehr groß.", exampleDari = "آپارتمان بسیار کلان است.", category = "توصیف"
        ),
        WordItem(
            id = "w8_13", lessonId = 8, german = "klein", article = ArticleType.NONE,
            pronunciationPersian = "کلاین", dariMeaning = "خُرد / کوچک",
            exampleGerman = "Das Zimmer ist etwas klein.", exampleDari = "اتاق کمی خُرد است.", category = "توصیف"
        ),
        WordItem(
            id = "w8_14", lessonId = 8, german = "hell", article = ArticleType.NONE,
            pronunciationPersian = "هِل", dariMeaning = "روشن / آفتاب‌گیر",
            exampleGerman = "Große Fenster machen die Wohnung hell.", exampleDari = "پنجره‌های کلان خانه را روشن می‌کنند.", category = "توصیف"
        ),
        WordItem(
            id = "w8_15", lessonId = 8, german = "Miete", article = ArticleType.DIE,
            pronunciationPersian = "میته", dariMeaning = "کرایه خانه (مؤنث: die)",
            exampleGerman = "Wie viel ist die Miete?", exampleDari = "کرایه خانه چقدر است؟", category = "مسکن"
        ),
        WordItem(
            id = "w8_16", lessonId = 8, german = "teuer", article = ArticleType.NONE,
            pronunciationPersian = "تویِر", dariMeaning = "گران / قیمتی",
            exampleGerman = "München ist eine teure Stadt.", exampleDari = "مونیخ یک شهر گران است.", category = "توصیف"
        ),
        WordItem(
            id = "w8_17", lessonId = 8, german = "billig", article = ArticleType.NONE,
            pronunciationPersian = "بیلیش", dariMeaning = "ارزان",
            exampleGerman = "Das Zimmer ist ziemlich billig.", exampleDari = "اتاق نسبتاً ارزان است.", category = "توصیف"
        )
    )

    fun getWordsForLesson(lessonId: Int): List<WordItem> =
        allWords.filter { it.lessonId == lessonId }
}

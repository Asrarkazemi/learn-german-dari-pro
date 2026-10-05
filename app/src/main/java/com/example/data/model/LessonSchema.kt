package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class VocabularyItem(
    val article: String, // "der" | "die" | "das" | "die (Plural)" | ""
    val word: String,
    val pronunciationPersianScript: String,
    val meaningDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("article", article)
        put("word", word)
        put("pronunciationPersianScript", pronunciationPersianScript)
        put("meaningDari", meaningDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): VocabularyItem {
            val article = obj.optString("article", "")
            val word = obj.getString("word").trim()
            val pronunciation = obj.getString("pronunciationPersianScript").trim()
            val meaningDari = obj.getString("meaningDari").trim()

            require(word.isNotEmpty()) { "واژه آلمانی (word) نمی‌تواند خالی باشد." }
            require(pronunciation.isNotEmpty()) { "تلفظ به خط فارسی (pronunciationPersianScript) برای واژه $word الزامی است." }
            require(meaningDari.isNotEmpty()) { "معنی دری (meaningDari) برای واژه $word الزامی است." }

            return VocabularyItem(
                article = article,
                word = word,
                pronunciationPersianScript = pronunciation,
                meaningDari = meaningDari
            )
        }
    }
}

data class ExampleSentence(
    val german: String,
    val pronunciation: String,
    val meaningDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("german", german)
        put("pronunciation", pronunciation)
        put("meaningDari", meaningDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ExampleSentence {
            val german = obj.getString("german").trim()
            val pronunciation = obj.getString("pronunciation").trim()
            val meaningDari = obj.getString("meaningDari").trim()

            require(german.isNotEmpty()) { "جمله آلمانی (german) نمی‌تواند خالی باشد." }
            require(pronunciation.isNotEmpty()) { "تلفظ جمله آلمانی به خط فارسی (pronunciation) الزامی است." }
            require(meaningDari.isNotEmpty()) { "ترجمه دری جمله (meaningDari) الزامی است." }

            return ExampleSentence(
                german = german,
                pronunciation = pronunciation,
                meaningDari = meaningDari
            )
        }
    }
}

data class ExerciseItem(
    val type: String, // "multiple-choice" | "fill-blank"
    val question: String,
    val pronunciation: String,
    val translationDari: String,
    val options: List<String>, // Exactly 4 options!
    val correctAnswer: String,
    val explanationDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("type", type)
        put("question", question)
        put("pronunciation", pronunciation)
        put("translationDari", translationDari)
        val optsArray = JSONArray()
        options.forEach { optsArray.put(it) }
        put("options", optsArray)
        put("correctAnswer", correctAnswer)
        put("explanationDari", explanationDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ExerciseItem {
            val type = obj.optString("type", "multiple-choice")
            val question = obj.getString("question").trim()
            val pronunciation = obj.getString("pronunciation").trim()
            val translationDari = obj.getString("translationDari").trim()
            val correctAnswer = obj.getString("correctAnswer").trim()
            val explanationDari = obj.getString("explanationDari").trim()

            require(question.isNotEmpty()) { "صورت سوال (question) الزامی است." }
            require(pronunciation.isNotEmpty()) { "تلفظ سوال به خط فارسی (pronunciation) الزامی است." }
            require(translationDari.isNotEmpty()) { "ترجمه سوال به دری (translationDari) الزامی است." }
            require(correctAnswer.isNotEmpty()) { "پاسخ صحیح (correctAnswer) الزامی است." }
            require(explanationDari.isNotEmpty()) { "توضیح آموزشی پاسخ (explanationDari) الزامی است." }

            val optionsJson = obj.getJSONArray("options")
            require(optionsJson.length() == 4) {
                "هر سوال باید دقیقاً دارای ۴ گزینه (options) باشد. تعداد دریافت شده: ${optionsJson.length()}"
            }

            val optionsList = mutableListOf<String>()
            for (i in 0 until optionsJson.length()) {
                val opt = optionsJson.getString(i).trim()
                require(opt.isNotEmpty()) { "گزینه ${i + 1} نمی‌تواند خالی باشد." }
                optionsList.add(opt)
            }

            require(optionsList.contains(correctAnswer)) {
                "پاسخ صحیح ($correctAnswer) باید حتماً یکی از گزینه‌های ۴ گانه باشد."
            }

            return ExerciseItem(
                type = type,
                question = question,
                pronunciation = pronunciation,
                translationDari = translationDari,
                options = optionsList,
                correctAnswer = correctAnswer,
                explanationDari = explanationDari
            )
        }
    }
}

data class DialogueLineItem(
    val speaker: String,
    val german: String,
    val pronunciation: String,
    val meaningDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("speaker", speaker)
        put("german", german)
        put("pronunciation", pronunciation)
        put("meaningDari", meaningDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): DialogueLineItem {
            val speaker = obj.getString("speaker").trim()
            val german = obj.getString("german").trim()
            val pronunciation = obj.getString("pronunciation").trim()
            val meaningDari = obj.getString("meaningDari").trim()

            require(speaker.isNotEmpty()) { "نام گوینده (speaker) الزامی است." }
            require(german.isNotEmpty()) { "متن آلمانی مکالمه (german) الزامی است." }
            require(pronunciation.isNotEmpty()) { "تلفظ آلمانی به خط فارسی (pronunciation) الزامی است." }
            require(meaningDari.isNotEmpty()) { "ترجمه دری خط مکالمه (meaningDari) الزامی است." }

            return DialogueLineItem(
                speaker = speaker,
                german = german,
                pronunciation = pronunciation,
                meaningDari = meaningDari
            )
        }
    }
}

data class DialogueItem(
    val lines: List<DialogueLineItem>
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        val linesArray = JSONArray()
        lines.forEach { linesArray.put(it.toJsonObject()) }
        put("lines", linesArray)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): DialogueItem {
            val linesJson = obj.getJSONArray("lines")
            val linesList = mutableListOf<DialogueLineItem>()
            for (i in 0 until linesJson.length()) {
                linesList.add(DialogueLineItem.fromJsonObject(linesJson.getJSONObject(i)))
            }
            return DialogueItem(lines = linesList)
        }
    }
}

data class GrammarSection(
    val title: String,
    val bodyDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("title", title)
        put("bodyDari", bodyDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GrammarSection {
            val title = obj.getString("title").trim()
            val bodyDari = obj.getString("bodyDari").trim()

            require(title.isNotEmpty()) { "عنوان بخش گرامر (title) نمی‌تواند خالی باشد." }
            require(bodyDari.isNotEmpty()) { "متن توضیح گرامر (bodyDari) نمی‌تواند خالی باشد." }

            return GrammarSection(
                title = title,
                bodyDari = bodyDari
            )
        }
    }
}

data class QaPair(
    val questionGerman: String,
    val questionPronunciation: String,
    val questionDari: String,
    val answerGerman: String,
    val answerPronunciation: String,
    val answerDari: String
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("questionGerman", questionGerman)
        put("questionPronunciation", questionPronunciation)
        put("questionDari", questionDari)
        put("answerGerman", answerGerman)
        put("answerPronunciation", answerPronunciation)
        put("answerDari", answerDari)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): QaPair {
            val questionGerman = obj.getString("questionGerman").trim()
            val questionPronunciation = obj.getString("questionPronunciation").trim()
            val questionDari = obj.getString("questionDari").trim()
            val answerGerman = obj.getString("answerGerman").trim()
            val answerPronunciation = obj.getString("answerPronunciation").trim()
            val answerDari = obj.getString("answerDari").trim()

            require(questionGerman.isNotEmpty()) { "سوال آلمانی (questionGerman) الزامی است." }
            require(questionPronunciation.isNotEmpty()) { "تلفظ سوال به خط فارسی (questionPronunciation) الزامی است." }
            require(questionDari.isNotEmpty()) { "ترجمه سوال به دری (questionDari) الزامی است." }
            require(answerGerman.isNotEmpty()) { "پاسخ آلمانی (answerGerman) الزامی است." }
            require(answerPronunciation.isNotEmpty()) { "تلفظ پاسخ به خط فارسی (answerPronunciation) الزامی است." }
            require(answerDari.isNotEmpty()) { "ترجمه پاسخ به دری (answerDari) الزامی است." }

            return QaPair(
                questionGerman = questionGerman,
                questionPronunciation = questionPronunciation,
                questionDari = questionDari,
                answerGerman = answerGerman,
                answerPronunciation = answerPronunciation,
                answerDari = answerDari
            )
        }
    }
}

data class LessonData(
    val id: String,
    val number: Int,
    val titleGerman: String,
    val titleDari: String,
    val vocabulary: List<VocabularyItem>,
    val exampleSentences: List<ExampleSentence>,
    val exercises: List<ExerciseItem>,
    val dialogues: List<DialogueItem>,
    val grammarSections: List<GrammarSection> = emptyList(),
    val qaPairs: List<QaPair> = emptyList(),
    val source: String? = null
) {
    fun toJson(): String {
        val root = JSONObject().apply {
            put("id", id)
            put("number", number)
            put("titleGerman", titleGerman)
            put("titleDari", titleDari)
            if (source != null) {
                put("source", source)
            }

            val vocabArray = JSONArray()
            vocabulary.forEach { vocabArray.put(it.toJsonObject()) }
            put("vocabulary", vocabArray)

            val exArray = JSONArray()
            exampleSentences.forEach { exArray.put(it.toJsonObject()) }
            put("exampleSentences", exArray)

            val exercisesArray = JSONArray()
            exercises.forEach { exercisesArray.put(it.toJsonObject()) }
            put("exercises", exercisesArray)

            val dialoguesArray = JSONArray()
            dialogues.forEach { dialoguesArray.put(it.toJsonObject()) }
            put("dialogues", dialoguesArray)

            if (grammarSections.isNotEmpty()) {
                val gArray = JSONArray()
                grammarSections.forEach { gArray.put(it.toJsonObject()) }
                put("grammarSections", gArray)
            }

            if (qaPairs.isNotEmpty()) {
                val qaArray = JSONArray()
                qaPairs.forEach { qaArray.put(it.toJsonObject()) }
                put("qaPairs", qaArray)
            }
        }
        return root.toString(2)
    }

    companion object {
        fun parseAndValidate(jsonString: String): Result<LessonData> {
            return runCatching {
                val cleaned = jsonString.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val root = JSONObject(cleaned)

                val id = root.optString("id", "lesson_${System.currentTimeMillis()}").trim()
                val number = root.optInt("number", 1)
                val titleGerman = root.getString("titleGerman").trim()
                val titleDari = root.getString("titleDari").trim()
                val source = if (root.has("source")) root.optString("source").trim().ifEmpty { null } else null

                require(titleGerman.isNotEmpty()) { "عنوان آلمانی درس (titleGerman) الزامی است." }
                require(titleDari.isNotEmpty()) { "عنوان دری درس (titleDari) الزامی است." }

                val vocabArray = root.getJSONArray("vocabulary")
                require(vocabArray.length() > 0) { "درس باید حداقل دارای یک واژه باشد." }
                val vocabList = mutableListOf<VocabularyItem>()
                for (i in 0 until vocabArray.length()) {
                    vocabList.add(VocabularyItem.fromJsonObject(vocabArray.getJSONObject(i)))
                }

                val examplesList = mutableListOf<ExampleSentence>()
                if (root.has("exampleSentences")) {
                    val exArray = root.getJSONArray("exampleSentences")
                    for (i in 0 until exArray.length()) {
                        examplesList.add(ExampleSentence.fromJsonObject(exArray.getJSONObject(i)))
                    }
                }

                val exercisesList = mutableListOf<ExerciseItem>()
                if (root.has("exercises")) {
                    val exercisesArray = root.getJSONArray("exercises")
                    for (i in 0 until exercisesArray.length()) {
                        exercisesList.add(ExerciseItem.fromJsonObject(exercisesArray.getJSONObject(i)))
                    }
                }

                val dialoguesList = mutableListOf<DialogueItem>()
                if (root.has("dialogues")) {
                    val dialoguesArray = root.getJSONArray("dialogues")
                    for (i in 0 until dialoguesArray.length()) {
                        dialoguesList.add(DialogueItem.fromJsonObject(dialoguesArray.getJSONObject(i)))
                    }
                }

                val grammarList = mutableListOf<GrammarSection>()
                if (root.has("grammarSections")) {
                    val gArray = root.getJSONArray("grammarSections")
                    for (i in 0 until gArray.length()) {
                        grammarList.add(GrammarSection.fromJsonObject(gArray.getJSONObject(i)))
                    }
                }

                val qaList = mutableListOf<QaPair>()
                if (root.has("qaPairs")) {
                    val qaArray = root.getJSONArray("qaPairs")
                    for (i in 0 until qaArray.length()) {
                        qaList.add(QaPair.fromJsonObject(qaArray.getJSONObject(i)))
                    }
                }

                LessonData(
                    id = id,
                    number = number,
                    titleGerman = titleGerman,
                    titleDari = titleDari,
                    vocabulary = vocabList,
                    exampleSentences = examplesList,
                    exercises = exercisesList,
                    dialogues = dialoguesList,
                    grammarSections = grammarList,
                    qaPairs = qaList,
                    source = source
                )
            }
        }
    }
}

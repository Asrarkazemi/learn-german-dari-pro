package com.example.data.model

enum class ArticleType(val label: String, val dariGender: String) {
    DER("der", "مذکر"),
    DIE("die", "مؤنث"),
    DAS("das", "خنثی"),
    PLURAL_DIE("die (جمع)", "جمع"),
    NONE("", "")
}

data class WordItem(
    val id: String,
    val lessonId: Int,
    val german: String,
    val article: ArticleType = ArticleType.NONE,
    val pronunciationPersian: String,
    val dariMeaning: String,
    val exampleGerman: String = "",
    val exampleDari: String = "",
    val category: String = ""
)

data class Lesson(
    val id: Int,
    val titleDari: String,
    val titleGerman: String,
    val descriptionDari: String,
    val iconEmoji: String,
    val grammarTopicDari: String,
    val grammarExplanationDari: String,
    val keyVerbs: List<String> = emptyList()
)

data class ConjugationRow(
    val pronounGerman: String,
    val pronounDari: String,
    val conjugatedForm: String
)

data class VerbConjugation(
    val verbInfinitive: String,
    val meaningDari: String,
    val isIrregular: Boolean,
    val tipDari: String,
    val conjugations: List<ConjugationRow>
)

data class PracticeQuestion(
    val id: String,
    val lessonId: Int,
    val questionType: QuestionType,
    val promptGerman: String,
    val promptDari: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationDari: String
)

enum class QuestionType {
    TRANSLATION_DE_TO_FA,
    TRANSLATION_FA_TO_DE,
    FILL_IN_BLANK_VERB,
    ARTICLE_SELECTION,
    NUMBER_PRACTICE
}

data class DialogueLine(
    val speakerName: String,
    val isSpeakerA: Boolean,
    val germanText: String,
    val pronunciationPersian: String,
    val dariText: String
)

data class Dialogue(
    val id: String,
    val lessonId: Int,
    val titleDari: String,
    val titleGerman: String,
    val contextDari: String,
    val lines: List<DialogueLine>
)

data class GermanNumber(
    val value: Int,
    val germanWord: String,
    val persianPronunciation: String,
    val dariDigits: String
)

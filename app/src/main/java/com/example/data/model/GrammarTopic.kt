package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class GrammarTopic(
    val id: String,
    val number: Int,
    val titleGerman: String,
    val titleDari: String,
    val sections: List<GrammarSection>,
    val exampleSentences: List<ExampleSentence> = emptyList(),
    val exercises: List<ExerciseItem> = emptyList(),
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

            val secArray = JSONArray()
            sections.forEach { secArray.put(it.toJsonObject()) }
            put("sections", secArray)

            if (exampleSentences.isNotEmpty()) {
                val exArray = JSONArray()
                exampleSentences.forEach { exArray.put(it.toJsonObject()) }
                put("exampleSentences", exArray)
            }

            if (exercises.isNotEmpty()) {
                val exercisesArray = JSONArray()
                exercises.forEach { exercisesArray.put(it.toJsonObject()) }
                put("exercises", exercisesArray)
            }
        }
        return root.toString(2)
    }

    companion object {
        fun fromJsonObject(root: JSONObject): GrammarTopic {
            val id = root.optString("id", "").trim()
            val number = root.optInt("number", 1)
            val titleGerman = root.getString("titleGerman").trim()
            val titleDari = root.getString("titleDari").trim()
            val source = if (root.has("source")) root.optString("source").trim().ifEmpty { null } else null

            require(titleGerman.isNotEmpty()) { "عنوان آلمانی مبحث گرامر (titleGerman) الزامی است." }
            require(titleDari.isNotEmpty()) { "عنوان دری مبحث گرامر (titleDari) الزامی است." }

            val sectionsArray = root.getJSONArray("sections")
            require(sectionsArray.length() > 0) { "مبحث گرامر باید حداقل دارای یک بخش توضیح (sections) باشد." }

            val sectionsList = mutableListOf<GrammarSection>()
            for (i in 0 until sectionsArray.length()) {
                sectionsList.add(GrammarSection.fromJsonObject(sectionsArray.getJSONObject(i)))
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

            val finalId = if (id.isEmpty()) "grammar_$number" else id

            return GrammarTopic(
                id = finalId,
                number = number,
                titleGerman = titleGerman,
                titleDari = titleDari,
                sections = sectionsList,
                exampleSentences = examplesList,
                exercises = exercisesList,
                source = source
            )
        }

        fun parseAndValidate(jsonString: String): Result<GrammarTopic> {
            return runCatching {
                val cleaned = jsonString.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val root = JSONObject(cleaned)
                fromJsonObject(root)
            }
        }
    }
}

package com.example.data.repository

import android.content.Context
import com.example.data.model.LessonData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * FIX H: Repository for the third library: «کتاب گرامر» (independent grammar book).
 * - Stored in its own file: custom_grammar_book_lessons.json
 * - Built-in content: NONE (empty state by default)
 * - Complete lessons following LessonSchema
 * - Replaces by number within this library only (supports numbers 101+)
 */
class GrammarBookRepository private constructor(private val context: Context) {

    private val grammarBookFile = File(context.filesDir, "custom_grammar_book_lessons.json")

    private val _lessonsFlow = MutableStateFlow<List<LessonData>>(emptyList())
    val lessonsFlow: StateFlow<List<LessonData>> = _lessonsFlow.asStateFlow()

    init {
        reloadAllLessons()
    }

    fun reloadAllLessons() {
        val loaded = loadFromDisk()
        val sorted = loaded.values.sortedBy { it.number }
        _lessonsFlow.value = sorted
    }

    private fun loadFromDisk(): Map<Int, LessonData> {
        if (!grammarBookFile.exists()) return emptyMap()
        return try {
            val text = grammarBookFile.readText().trim()
            if (text.startsWith("[")) {
                val array = JSONArray(text)
                val map = mutableMapOf<Int, LessonData>()
                for (i in 0 until array.length()) {
                    val parsed = LessonData.parseAndValidate(array.getJSONObject(i).toString())
                    parsed.getOrNull()?.let { map[it.number] = it }
                }
                map
            } else if (text.startsWith("{")) {
                val obj = JSONObject(text)
                val map = mutableMapOf<Int, LessonData>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val num = key.toIntOrNull() ?: continue
                    val lessonJson = obj.getJSONObject(key).toString()
                    val parsed = LessonData.parseAndValidate(lessonJson)
                    parsed.getOrNull()?.let { map[num] = it }
                }
                map
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveToDisk(lessons: Map<Int, LessonData>) {
        try {
            val root = JSONObject()
            lessons.forEach { (number, lesson) ->
                root.put(number.toString(), JSONObject(lesson.toJson()))
            }
            grammarBookFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Imports a single lesson JSON object or an array of lesson JSON objects into the Grammar Book library.
     * Replaces by number within this library only.
     */
    fun importJson(jsonString: String): Result<BatchImportResult> {
        return runCatching {
            val cleaned = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            if (cleaned.startsWith("[")) {
                val jsonArray = JSONArray(cleaned)
                require(jsonArray.length() > 0) { "فایل انتخابی حاوی هیچ درسی نیست (آرایه خالی است)." }

                val currentLessons = loadFromDisk().toMutableMap()
                var successCount = 0
                var failureCount = 0
                val importedList = mutableListOf<LessonData>()

                for (i in 0 until jsonArray.length()) {
                    val elementObj = jsonArray.optJSONObject(i)
                    if (elementObj == null) {
                        failureCount++
                        continue
                    }
                    val parseResult = LessonData.parseAndValidate(elementObj.toString())
                    if (parseResult.isSuccess) {
                        val parsed = parseResult.getOrThrow()
                        val finalLesson = parsed.copy(
                            id = if (parsed.id.isBlank()) "grammar_book_lesson_${parsed.number}" else parsed.id,
                            source = parsed.source ?: "grammar_book"
                        )
                        currentLessons[finalLesson.number] = finalLesson
                        importedList.add(finalLesson)
                        successCount++
                    } else {
                        failureCount++
                    }
                }

                require(successCount > 0) {
                    "هیچ درسی با ساختار معتبر در فایل یافت نشد."
                }

                saveToDisk(currentLessons)
                reloadAllLessons()

                val successStr = toDariDigits(successCount)
                val failureStr = toDariDigits(failureCount)
                val summary = if (failureCount > 0) {
                    "$successStr درس با موفقیت وارد/جایگزین شد ($failureStr مورد نامعتبر رد شد)"
                } else {
                    "$successStr درس با موفقیت وارد/جایگزین شد"
                }

                BatchImportResult(
                    totalProcessed = jsonArray.length(),
                    successCount = successCount,
                    failureCount = failureCount,
                    singleLesson = if (importedList.size == 1) importedList.first() else null,
                    summaryMessage = summary
                )
            } else if (cleaned.startsWith("{")) {
                val parseResult = LessonData.parseAndValidate(cleaned)
                val parsedLesson = parseResult.getOrThrow()
                val finalLesson = parsedLesson.copy(
                    id = if (parsedLesson.id.isBlank()) "grammar_book_lesson_${parsedLesson.number}" else parsedLesson.id,
                    source = parsedLesson.source ?: "grammar_book"
                )

                val currentLessons = loadFromDisk().toMutableMap()
                currentLessons[finalLesson.number] = finalLesson
                saveToDisk(currentLessons)
                reloadAllLessons()

                val lessonNumStr = toDariDigits(finalLesson.number)
                BatchImportResult(
                    totalProcessed = 1,
                    successCount = 1,
                    failureCount = 0,
                    singleLesson = finalLesson,
                    summaryMessage = "درس گرامر $lessonNumStr («${finalLesson.titleGerman}») با موفقیت وارد/جایگزین شد."
                )
            } else {
                throw IllegalArgumentException("قالب فایل نامعتبر است؛ متن باید یک شیء JSON {...} یا آرایه [...] از درس‌ها باشد.")
            }
        }
    }

    private fun toDariDigits(num: Int): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return num.toString().map { if (it in '0'..'9') persianDigits[it - '0'] else it }.joinToString("")
    }

    fun deleteLesson(lessonIdOrNumber: String): Boolean {
        val currentLessons = loadFromDisk().toMutableMap()
        val foundEntry = currentLessons.entries.find {
            it.value.id == lessonIdOrNumber || it.key.toString() == lessonIdOrNumber
        } ?: return false

        currentLessons.remove(foundEntry.key)
        saveToDisk(currentLessons)
        reloadAllLessons()
        return true
    }

    companion object {
        @Volatile
        private var instance: GrammarBookRepository? = null

        fun getInstance(context: Context): GrammarBookRepository {
            return instance ?: synchronized(this) {
                instance ?: GrammarBookRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

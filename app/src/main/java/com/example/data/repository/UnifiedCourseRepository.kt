package com.example.data.repository

import android.content.Context
import com.example.data.model.LessonData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BatchImportResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val singleLesson: LessonData? = null,
    val summaryMessage: String
)

class UnifiedCourseRepository private constructor(private val context: Context) {

    private val customLessonsFile = File(context.filesDir, "custom_lessons.json")

    private val _lessonsFlow = MutableStateFlow<List<LessonData>>(emptyList())
    val lessonsFlow: StateFlow<List<LessonData>> = _lessonsFlow.asStateFlow()

    private val _overrideNumbersFlow = MutableStateFlow<Set<Int>>(emptySet())
    val overrideNumbersFlow: StateFlow<Set<Int>> = _overrideNumbersFlow.asStateFlow()

    init {
        reloadAllLessons()
    }

    fun reloadAllLessons() {
        val builtIn = BuiltInCourseData.lessons
        val overrides = loadOverrides() // Map<Int, LessonData>

        val builtInNumbers = builtIn.map { it.number }.toSet()
        val merged = mutableListOf<LessonData>()

        // For built-in lessons: if an override exists for this number, use the override; else use built-in
        for (b in builtIn) {
            val override = overrides[b.number]
            if (override != null) {
                merged.add(override)
            } else {
                merged.add(b)
            }
        }

        // For any custom lesson whose number is NOT in builtIn (e.g. lesson 9, 10...):
        for ((num, customLesson) in overrides) {
            if (num !in builtInNumbers) {
                merged.add(customLesson)
            }
        }

        merged.sortBy { it.number }
        _lessonsFlow.value = merged
        _overrideNumbersFlow.value = overrides.keys
    }

    private fun loadOverrides(): Map<Int, LessonData> {
        if (!customLessonsFile.exists()) return emptyMap()
        return try {
            val text = customLessonsFile.readText().trim()
            if (text.startsWith("[")) {
                // Backward-compat if file was previously a JSON array
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

    private fun saveOverrides(overrides: Map<Int, LessonData>) {
        try {
            val root = JSONObject()
            overrides.forEach { (number, lesson) ->
                root.put(number.toString(), JSONObject(lesson.toJson()))
            }
            customLessonsFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // CHANGE 5: Multi-lesson import for single object OR JSON array
    fun importJson(jsonString: String): Result<BatchImportResult> {
        return runCatching {
            val cleaned = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            if (cleaned.startsWith("[")) {
                // JSON ARRAY of lesson objects
                val jsonArray = JSONArray(cleaned)
                require(jsonArray.length() > 0) { "فایل انتخابی حاوی هیچ درسی نیست (آرایه خالی است)." }

                val currentOverrides = loadOverrides().toMutableMap()
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
                        val parsedLesson = parseResult.getOrThrow()
                        val finalLesson = parsedLesson.copy(
                            id = if (parsedLesson.id.isBlank()) "lesson_${parsedLesson.number}" else parsedLesson.id
                        )
                        // Replace-by-number rule
                        currentOverrides[finalLesson.number] = finalLesson
                        importedList.add(finalLesson)
                        successCount++
                    } else {
                        failureCount++
                    }
                }

                if (successCount > 0) {
                    saveOverrides(currentOverrides)
                    reloadAllLessons()
                } else {
                    val failMsg = toDariDigits(failureCount)
                    throw IllegalArgumentException("هیچ‌کدام از $failMsg درس موجود در فایل معتبر نبود.")
                }

                val successStr = toDariDigits(successCount)
                val failureStr = toDariDigits(failureCount)
                val summary = if (failureCount > 0) {
                    "$successStr درس با موفقیت وارد/جایگزین شد ($failureStr درس به دلیل خطای قالب رد شد)"
                } else {
                    "$successStr درس با موفقیت وارد/جایگزین شد"
                }

                BatchImportResult(
                    totalProcessed = jsonArray.length(),
                    successCount = successCount,
                    failureCount = failureCount,
                    singleLesson = if (importedList.size == 1 && jsonArray.length() == 1) importedList.first() else null,
                    summaryMessage = summary
                )
            } else {
                // Single JSON Object
                val singleResult = importLesson(cleaned)
                if (singleResult.isFailure) {
                    throw singleResult.exceptionOrNull() ?: Exception("قالب درس نامعتبر است.")
                }
                val lesson = singleResult.getOrThrow()
                BatchImportResult(
                    totalProcessed = 1,
                    successCount = 1,
                    failureCount = 0,
                    singleLesson = lesson,
                    summaryMessage = "درس «${lesson.titleDari}» با موفقیت افزوده/جایگزین شد! ✓"
                )
            }
        }
    }

    fun importLesson(jsonString: String): Result<LessonData> {
        val cleaned = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (cleaned.startsWith("[")) {
            val batchResult = importJson(cleaned)
            if (batchResult.isFailure) return Result.failure(batchResult.exceptionOrNull() ?: Exception("خطا در واردسازی درس‌ها"))
            val batch = batchResult.getOrThrow()
            val lesson = batch.singleLesson ?: _lessonsFlow.value.first()
            return Result.success(lesson)
        }

        val parseResult = LessonData.parseAndValidate(cleaned)
        if (parseResult.isFailure) {
            return parseResult
        }

        val parsedLesson = parseResult.getOrThrow()

        // CHANGE 3: When imported lesson's "number" equals an existing lesson's number,
        // REPLACES that lesson everywhere instead of being renumbered and appended as a duplicate.
        // The replacement persists across app restarts (stored as overrides keyed by lesson number).
        val finalLesson = parsedLesson.copy(
            id = if (parsedLesson.id.isBlank()) "lesson_${parsedLesson.number}" else parsedLesson.id
        )

        val currentOverrides = loadOverrides().toMutableMap()
        currentOverrides[finalLesson.number] = finalLesson
        saveOverrides(currentOverrides)

        reloadAllLessons()
        return Result.success(finalLesson)
    }

    fun deleteCustomLesson(identifier: String): Boolean {
        val currentOverrides = loadOverrides().toMutableMap()
        val num = identifier.toIntOrNull()
        var removedNumber: Int? = null

        if (num != null && currentOverrides.containsKey(num)) {
            currentOverrides.remove(num)
            removedNumber = num
        } else {
            // Find by id
            val entry = currentOverrides.entries.find { it.value.id == identifier }
            if (entry != null) {
                currentOverrides.remove(entry.key)
                removedNumber = entry.key
            } else {
                // If identifier is like "lesson_3", try parsing suffix
                val suffixNum = identifier.removePrefix("lesson_").toIntOrNull()
                if (suffixNum != null && currentOverrides.containsKey(suffixNum)) {
                    currentOverrides.remove(suffixNum)
                    removedNumber = suffixNum
                }
            }
        }

        if (removedNumber != null) {
            saveOverrides(currentOverrides)
            reloadAllLessons()
            return true
        }
        return false
    }

    fun isCustomLesson(lessonNumber: Int): Boolean {
        return _overrideNumbersFlow.value.contains(lessonNumber)
    }

    private fun toDariDigits(num: Int): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return num.toString().map { if (it in '0'..'9') persianDigits[it - '0'] else it }.joinToString("")
    }

    companion object {
        @Volatile
        private var instance: UnifiedCourseRepository? = null

        fun getInstance(context: Context): UnifiedCourseRepository {
            return instance ?: synchronized(this) {
                instance ?: UnifiedCourseRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

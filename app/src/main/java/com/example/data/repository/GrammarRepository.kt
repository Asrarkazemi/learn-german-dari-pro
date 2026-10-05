package com.example.data.repository

import android.content.Context
import com.example.data.model.GrammarTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class GrammarBatchImportResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val singleTopic: GrammarTopic? = null,
    val summaryMessage: String
)

class GrammarRepository private constructor(private val context: Context) {

    private val customGrammarFile = File(context.filesDir, "custom_grammar_topics.json")

    private val _grammarTopicsFlow = MutableStateFlow<List<GrammarTopic>>(emptyList())
    val grammarTopicsFlow: StateFlow<List<GrammarTopic>> = _grammarTopicsFlow.asStateFlow()

    private val _overrideNumbersFlow = MutableStateFlow<Set<Int>>(emptySet())
    val overrideNumbersFlow: StateFlow<Set<Int>> = _overrideNumbersFlow.asStateFlow()

    init {
        reloadAllTopics()
    }

    fun reloadAllTopics() {
        val builtIn = BuiltInGrammarData.topics
        val overrides = loadOverrides()

        val builtInNumbers = builtIn.map { it.number }.toSet()
        val merged = mutableListOf<GrammarTopic>()

        for (b in builtIn) {
            val override = overrides[b.number]
            if (override != null) {
                merged.add(override)
            } else {
                merged.add(b)
            }
        }

        for ((num, customTopic) in overrides) {
            if (num !in builtInNumbers) {
                merged.add(customTopic)
            }
        }

        merged.sortBy { it.number }
        _grammarTopicsFlow.value = merged
        _overrideNumbersFlow.value = overrides.keys
    }

    private fun loadOverrides(): Map<Int, GrammarTopic> {
        if (!customGrammarFile.exists()) return emptyMap()
        return try {
            val text = customGrammarFile.readText().trim()
            if (text.startsWith("[")) {
                val array = JSONArray(text)
                val map = mutableMapOf<Int, GrammarTopic>()
                for (i in 0 until array.length()) {
                    val parsed = GrammarTopic.parseAndValidate(array.getJSONObject(i).toString())
                    parsed.getOrNull()?.let { map[it.number] = it }
                }
                map
            } else if (text.startsWith("{")) {
                val obj = JSONObject(text)
                val map = mutableMapOf<Int, GrammarTopic>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val num = key.toIntOrNull() ?: continue
                    val topicJson = obj.getJSONObject(key).toString()
                    val parsed = GrammarTopic.parseAndValidate(topicJson)
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

    private fun saveOverrides(overrides: Map<Int, GrammarTopic>) {
        try {
            val root = JSONObject()
            overrides.forEach { (number, topic) ->
                root.put(number.toString(), JSONObject(topic.toJson()))
            }
            customGrammarFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importJson(jsonString: String): Result<GrammarBatchImportResult> {
        return runCatching {
            val cleaned = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            if (cleaned.startsWith("[")) {
                val jsonArray = JSONArray(cleaned)
                require(jsonArray.length() > 0) { "فایل انتخابی حاوی هیچ مبحثی نیست (آرایه خالی است)." }

                val currentOverrides = loadOverrides().toMutableMap()
                var successCount = 0
                var failureCount = 0
                val importedList = mutableListOf<GrammarTopic>()

                for (i in 0 until jsonArray.length()) {
                    val elementObj = jsonArray.optJSONObject(i)
                    if (elementObj == null) {
                        failureCount++
                        continue
                    }
                    val parseResult = GrammarTopic.parseAndValidate(elementObj.toString())
                    if (parseResult.isSuccess) {
                        val parsedTopic = parseResult.getOrThrow()
                        val finalTopic = parsedTopic.copy(
                            id = if (parsedTopic.id.isBlank()) "grammar_${parsedTopic.number}" else parsedTopic.id
                        )
                        currentOverrides[finalTopic.number] = finalTopic
                        importedList.add(finalTopic)
                        successCount++
                    } else {
                        failureCount++
                    }
                }

                if (successCount > 0) {
                    saveOverrides(currentOverrides)
                    reloadAllTopics()
                } else {
                    val failMsg = toDariDigits(failureCount)
                    throw IllegalArgumentException("هیچ‌کدام از $failMsg مبحث گرامر موجود در فایل معتبر نبود.")
                }

                val successStr = toDariDigits(successCount)
                val failureStr = toDariDigits(failureCount)
                val summary = if (failureCount > 0) {
                    "$successStr مبحث گرامر با موفقیت وارد/جایگزین شد ($failureStr مبحث به دلیل خطای قالب رد شد)"
                } else {
                    "$successStr مبحث گرامر با موفقیت وارد/جایگزین شد"
                }

                GrammarBatchImportResult(
                    totalProcessed = jsonArray.length(),
                    successCount = successCount,
                    failureCount = failureCount,
                    singleTopic = if (importedList.size == 1) importedList.first() else null,
                    summaryMessage = summary
                )
            } else {
                // Single object import
                val parseResult = GrammarTopic.parseAndValidate(cleaned)
                val topic = parseResult.getOrThrow()

                val currentOverrides = loadOverrides().toMutableMap()
                val finalTopic = topic.copy(
                    id = if (topic.id.isBlank()) "grammar_${topic.number}" else topic.id
                )
                currentOverrides[finalTopic.number] = finalTopic
                saveOverrides(currentOverrides)
                reloadAllTopics()

                GrammarBatchImportResult(
                    totalProcessed = 1,
                    successCount = 1,
                    failureCount = 0,
                    singleTopic = finalTopic,
                    summaryMessage = "مبحث گرامر «${finalTopic.titleDari}» با موفقیت وارد/جایگزین شد"
                )
            }
        }
    }

    fun importTopic(jsonString: String): Result<GrammarTopic> {
        val cleaned = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        if (cleaned.startsWith("[")) {
            val batchResult = importJson(cleaned)
            if (batchResult.isFailure) return Result.failure(batchResult.exceptionOrNull() ?: Exception("خطا در واردسازی مباحث گرامر"))
            val batch = batchResult.getOrThrow()
            val topic = batch.singleTopic ?: _grammarTopicsFlow.value.firstOrNull()
            return if (topic != null) Result.success(topic) else Result.failure(Exception("هیچ مبحث معتبری یافت نشد"))
        }

        val parseResult = GrammarTopic.parseAndValidate(cleaned)
        if (parseResult.isFailure) {
            return Result.failure(parseResult.exceptionOrNull() ?: Exception("خطا در اعتبارسنجی قالب مبحث گرامر"))
        }

        val topic = parseResult.getOrThrow()
        val currentOverrides = loadOverrides().toMutableMap()
        val finalTopic = topic.copy(
            id = if (topic.id.isBlank()) "grammar_${topic.number}" else topic.id
        )
        currentOverrides[finalTopic.number] = finalTopic
        saveOverrides(currentOverrides)
        reloadAllTopics()
        return Result.success(finalTopic)
    }

    fun deleteCustomTopic(identifier: String): Boolean {
        val currentOverrides = loadOverrides().toMutableMap()
        var removedNumber: Int? = null

        val directNumber = identifier.toIntOrNull()
        if (directNumber != null && currentOverrides.containsKey(directNumber)) {
            currentOverrides.remove(directNumber)
            removedNumber = directNumber
        } else {
            val entry = currentOverrides.entries.firstOrNull { it.value.id == identifier }
            if (entry != null) {
                currentOverrides.remove(entry.key)
                removedNumber = entry.key
            } else {
                val suffixNum = identifier.removePrefix("grammar_").toIntOrNull()
                if (suffixNum != null && currentOverrides.containsKey(suffixNum)) {
                    currentOverrides.remove(suffixNum)
                    removedNumber = suffixNum
                }
            }
        }

        if (removedNumber != null) {
            saveOverrides(currentOverrides)
            reloadAllTopics()
            return true
        }
        return false
    }

    fun isCustomTopic(topicNumber: Int): Boolean {
        return _overrideNumbersFlow.value.contains(topicNumber)
    }

    private fun toDariDigits(num: Int): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return num.toString().map { if (it in '0'..'9') persianDigits[it - '0'] else it }.joinToString("")
    }

    companion object {
        @Volatile
        private var instance: GrammarRepository? = null

        fun getInstance(context: Context): GrammarRepository {
            return instance ?: synchronized(this) {
                instance ?: GrammarRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

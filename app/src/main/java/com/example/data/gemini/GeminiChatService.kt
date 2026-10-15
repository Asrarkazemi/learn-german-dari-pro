package com.example.data.gemini

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedLessonJson: String? = null,
    val isError: Boolean = false,
    val canRetry: Boolean = false,
    val isKeyError: Boolean = false
)

enum class ChatMode(
    val label: String,
    val primaryModel: String,
    val temperature: Double,
    val thinkingLevel: String,
    val systemInstruction: String
) {
    LIGHT(
        label = "⚡ سبک",
        primaryModel = "gemini-3.8-flash-lite",
        temperature = 0.3,
        thinkingLevel = "low",
        systemInstruction = "تو یک دستیار سریع آموزش زبان آلمانی هستی. پاسخها را بسیار کوتاه، دقیق و بدون حاشیه به زبان فارسی بده. اگر کاربر کلمه یا اصطلاحی فرستاد، فقط معنی، آرتیکل (der/die/das)، حالت جمع و یک مثال کوتاه آلمانی با ترجمه فارسی بنویس."
    ),
    NORMAL(
        label = "⚖️ معمولی",
        primaryModel = "gemini-3.8-flash",
        temperature = 0.6,
        thinkingLevel = "low",
        systemInstruction = "تو یک معلم صمیمی زبان آلمانی هستی. به سوالات کاربر با بیانی ساده و کاربردی پاسخ بده، مثالهای روزمره آلمانی همراه با ترجمه فارسی بیاور و اشتباهات واضح کاربر را به آرامی اصلاح کن."
    ),
    DEEP(
        label = "🧠 عمیق",
        primaryModel = "gemini-3.8-flash",
        temperature = 0.7,
        thinkingLevel = "high",
        systemInstruction = "تو یک استاد برجسته گرامر و ممتحن آزمونهای زبان آلمانی (Goethe / DTZ / TestDaF) هستی. متن یا سوال کاربر را به صورت عمیق کالبدشکافی کن. تمام نکات گرامری (Kasus: Nominativ, Akkusativ, Dativ, Genitiv، جایگاه فعل، حروف اضافه و ساختار جمله) را با دلیل توضیح بده. در صورت وجود خطا، ابتدا علت خطا را بگو، سپس شکل صحیح و در نهایت معادل طبیعیتر و سطح بالاتر (B1/B2) آن را بنویس."
    );

    companion object {
        fun fromNameOrDefault(name: String?): ChatMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: LIGHT
        }
    }
}

data class ChatExecutionStep(
    val model: String,
    val temperature: Double,
    val thinkingLevel: String,
    val systemInstruction: String
)

class GeminiLightCache(context: Context? = null) {
    private val prefs: SharedPreferences? = try {
        context?.applicationContext?.getSharedPreferences("gemini_light_chat_cache", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }
    private val memoryMap = mutableMapOf<String, String>()

    fun get(prompt: String): String? {
        val key = normalize(prompt)
        if (key.isBlank()) return null
        return prefs?.getString(key, null) ?: memoryMap[key]
    }

    fun put(prompt: String, response: String) {
        val key = normalize(prompt)
        if (key.isBlank() || response.isBlank()) return
        prefs?.edit()?.putString(key, response)?.apply()
        memoryMap[key] = response
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
        memoryMap.clear()
    }

    fun size(): Int {
        val prefSize = prefs?.all?.size ?: 0
        return if (prefSize > 0) prefSize else memoryMap.size
    }

    private fun normalize(prompt: String): String = prompt.trim().lowercase()
}

sealed class GeminiChatException(message: String) : Exception(message) {
    class ServerBusyException(
        message: String = "سرور جیمنای فعلاً شلوغ است؛ چند دقیقۀ دیگر دوباره کوشش کنید."
    ) : GeminiChatException(message)

    class KeyErrorException(
        message: String = "کلید API جیمنای نامعتبر است یا هنوز تنظیم نشده است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید."
    ) : GeminiChatException(message)

    class NetworkErrorException(
        message: String = "خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید."
    ) : GeminiChatException(message)

    class GeneralException(
        message: String = "متأسفانه در دریافت پاسخ خطایی رخ داد؛ لطفاً دوباره کوشش کنید."
    ) : GeminiChatException(message)
}

class GeminiChatService(context: Context? = null) {

    val lightCache = GeminiLightCache(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a chat message to Gemini with mode routing, 3.8 models, thinking config,
     * persistent cache for LIGHT mode, and silent fallback across execution steps.
     */
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        apiKey: String,
        mode: ChatMode = ChatMode.LIGHT,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                GeminiChatException.KeyErrorException("کلید API جیمنای تنظیم نشده است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید.")
            )
        }

        // Caching: return cached answer immediately for LIGHT mode only
        if (mode == ChatMode.LIGHT) {
            val cached = lightCache.get(userMessage)
            if (!cached.isNullOrBlank()) {
                return@withContext Result.success(cached)
            }
        }

        val steps = getExecutionPlan(mode)
        var lastBusy = false

        for ((index, step) in steps.withIndex()) {
            if (index > 0) {
                onStatusUpdate?.invoke("سرور شلوغ است، دوباره تلاش میشود…")
                delay(1000L)
            }

            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/${step.model}:generateContent?key=$cleanKey"
                val requestBodyJson = buildRequestBody(
                    userMessage = userMessage,
                    conversationHistory = conversationHistory,
                    systemInstruction = step.systemInstruction,
                    temperature = step.temperature,
                    thinkingLevel = step.thinkingLevel
                )
                val requestBody = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val parsedText = parseCandidateText(responseBody)
                    if (parsedText.isNotBlank()) {
                        if (mode == ChatMode.LIGHT) {
                            lightCache.put(userMessage, parsedText)
                        }
                        return@withContext Result.success(parsedText)
                    } else {
                        return@withContext Result.failure(
                            GeminiChatException.GeneralException("پاسخی از جیمنای دریافت نشد.")
                        )
                    }
                }

                // Check for invalid or missing API key error
                if (isKeyError(responseCode, responseBody)) {
                    return@withContext Result.failure(
                        GeminiChatException.KeyErrorException("کلید API جیمنای نامعتبر است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید.")
                    )
                }

                // Check for 5xx / 429 / overloaded
                if (isServerBusy(responseCode, responseBody)) {
                    lastBusy = true
                    continue
                }

                // Other HTTP error code
                lastBusy = true
                continue

            } catch (e: IOException) {
                lastBusy = true
                if (index == steps.lastIndex) {
                    return@withContext Result.failure(
                        GeminiChatException.NetworkErrorException("خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید.")
                    )
                }
                onStatusUpdate?.invoke("سرور شلوغ است، دوباره تلاش میشود…")
                delay(1000L)
            } catch (e: Exception) {
                lastBusy = true
                if (index == steps.lastIndex) {
                    return@withContext Result.failure(
                        GeminiChatException.GeneralException("خطای غیرمنتظره در ارتباط با جیمنای")
                    )
                }
            }
        }

        if (lastBusy) {
            Result.failure(
                GeminiChatException.ServerBusyException("سرور جیمنای فعلاً شلوغ است؛ چند دقیقۀ دیگر دوباره کوشش کنید.")
            )
        } else {
            Result.failure(
                GeminiChatException.GeneralException("خطا در ارتباط با سرور جیمنای؛ لطفاً دوباره کوشش کنید.")
            )
        }
    }

    fun buildRequestBody(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        systemInstruction: String = ChatMode.LIGHT.systemInstruction,
        temperature: Double = 0.3,
        thinkingLevel: String = "low"
    ): String {
        val contentsArray = JSONArray()

        // Take last 6 recent turns to keep context lightweight
        val recentTurns = conversationHistory.takeLast(6).filter { !it.isError }
        for (msg in recentTurns) {
            val role = if (msg.isUser) "user" else "model"
            val partObj = JSONObject().put("text", msg.text)
            val turnObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(turnObj)
        }

        // Add current message
        contentsArray.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        )

        val thinkingConfig = JSONObject().apply {
            put("thinking_level", thinkingLevel)
        }

        val generationConfig = JSONObject().apply {
            put("temperature", temperature)
            put("thinkingConfig", thinkingConfig)
        }

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("generationConfig", generationConfig)
        }
        return rootJson.toString()
    }

    private fun parseCandidateText(responseBody: String): String {
        return try {
            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val firstCand = candidates.getJSONObject(0)
            val content = firstCand.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            parts.optJSONObject(0)?.optString("text", "").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        const val MODEL_FLASH = "gemini-3.8-flash"
        const val MODEL_FLASH_LITE = "gemini-3.8-flash-lite"
        const val PRIMARY_MODEL = MODEL_FLASH

        // Fallback models mapping exclusively to served 3.8 models
        val FALLBACK_MODELS = listOf(
            MODEL_FLASH,
            MODEL_FLASH_LITE
        )

        fun getExecutionPlan(mode: ChatMode): List<ChatExecutionStep> {
            return when (mode) {
                ChatMode.DEEP -> listOf(
                    ChatExecutionStep(
                        model = MODEL_FLASH,
                        temperature = 0.7,
                        thinkingLevel = "high",
                        systemInstruction = ChatMode.DEEP.systemInstruction
                    ),
                    ChatExecutionStep(
                        model = MODEL_FLASH,
                        temperature = 0.7,
                        thinkingLevel = "high",
                        systemInstruction = ChatMode.DEEP.systemInstruction
                    ),
                    ChatExecutionStep(
                        model = MODEL_FLASH_LITE,
                        temperature = 0.7,
                        thinkingLevel = "low",
                        systemInstruction = ChatMode.DEEP.systemInstruction
                    )
                )
                ChatMode.NORMAL -> listOf(
                    ChatExecutionStep(
                        model = MODEL_FLASH,
                        temperature = 0.6,
                        thinkingLevel = "low",
                        systemInstruction = ChatMode.NORMAL.systemInstruction
                    ),
                    ChatExecutionStep(
                        model = MODEL_FLASH_LITE,
                        temperature = 0.6,
                        thinkingLevel = "low",
                        systemInstruction = ChatMode.NORMAL.systemInstruction
                    )
                )
                ChatMode.LIGHT -> listOf(
                    ChatExecutionStep(
                        model = MODEL_FLASH_LITE,
                        temperature = 0.3,
                        thinkingLevel = "low",
                        systemInstruction = ChatMode.LIGHT.systemInstruction
                    ),
                    ChatExecutionStep(
                        model = MODEL_FLASH_LITE,
                        temperature = 0.3,
                        thinkingLevel = "low",
                        systemInstruction = ChatMode.LIGHT.systemInstruction
                    )
                )
            }
        }

        fun isServerBusy(code: Int, responseBody: String): Boolean {
            if (code == 503 || code == 429 || code in 500..599) return true
            val lower = responseBody.lowercase()
            return lower.contains("503") ||
                    lower.contains("429") ||
                    lower.contains("high demand") ||
                    lower.contains("high-demand") ||
                    lower.contains("overloaded") ||
                    lower.contains("resource has been exhausted") ||
                    lower.contains("resource_exhausted") ||
                    lower.contains("quota") ||
                    lower.contains("rate limit") ||
                    lower.contains("rate_limit") ||
                    lower.contains("unavailable") ||
                    lower.contains("temporarily unavailable")
        }

        fun isKeyError(code: Int, responseBody: String): Boolean {
            if (code == 401 || code == 403) return true
            val lower = responseBody.lowercase()
            return lower.contains("api_key_invalid") ||
                    lower.contains("api key not valid") ||
                    lower.contains("invalid api key") ||
                    lower.contains("permission_denied") ||
                    lower.contains("api key expired") ||
                    lower.contains("unauthenticated")
        }

        fun extractLessonJson(text: String): String? {
            val jsonStart = text.indexOf("```json")
            if (jsonStart != -1) {
                val afterStart = text.substring(jsonStart + 7)
                val jsonEnd = afterStart.indexOf("```")
                if (jsonEnd != -1) {
                    val potentialJson = afterStart.substring(0, jsonEnd).trim()
                    if (potentialJson.contains("\"vocabulary\"") ||
                        potentialJson.contains("\"exercises\"") ||
                        potentialJson.contains("\"sections\"")) {
                        return potentialJson
                    }
                }
            }
            return null
        }

        fun extractDialogueJson(text: String): String? {
            val jsonStart = text.indexOf("```json")
            if (jsonStart != -1) {
                val afterStart = text.substring(jsonStart + 7)
                val jsonEnd = afterStart.indexOf("```")
                if (jsonEnd != -1) {
                    val potentialJson = afterStart.substring(0, jsonEnd).trim()
                    if (potentialJson.contains("\"lines\"") || potentialJson.contains("\"german\"")) {
                        return potentialJson
                    }
                }
            }
            // Also check for raw JSON without markdown
            val trimmed = text.trim()
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                if (trimmed.contains("\"lines\"") || trimmed.contains("\"german\"")) {
                    return trimmed
                }
            }
            return null
        }
    }

    /**
     * Generates a 2-person A/B dialogue (~10-16 lines) on the specified topic.
     * Uses existing Gemini integration, models fallback, and Dari error handling.
     */
    suspend fun generateDialogue(
        topic: String,
        apiKey: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<List<com.example.data.model.DialogueMakerLine>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                GeminiChatException.KeyErrorException("کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید خود را از بخش تنظیمات وارد نمایید.")
            )
        }

        val prompt = """
            یک مکالمه طبیعی دونفره (شخص A و شخص B) به زبان آلمانی دربارهٔ موضوع زیر بسازید:
            موضوع: $topic

            الزامات مهم:
            ۱. تعداد خطوط: بین ۱۰ تا ۱۶ خط متناوب (یک بار شخص A و بار بعد شخص B).
            ۲. سطح: مناسب زبان‌آموز سطح A1 تا A2، جملات کاربردی، روان و طبیعی روزمره.
            ۳. برای هر خط:
               - speaker: "A" برای شخص اول (مرد) و "B" برای شخص دوم (زن).
               - german: متن جمله به زبان آلمانی استاندارد.
               - pronunciation: تلفظ کامل جمله به خط فارسی دری.
               - translationDari: معنی دقیق و طبیعی به زبان فارسی دری.
            ۴. خروجی را دقیقاً و فقط در یک بلوک کد ```json ارائه دهید با این ساختار:
            ```json
            {
              "topic": "$topic",
              "lines": [
                {
                  "speaker": "A",
                  "isSpeakerA": true,
                  "german": "Hallo! Wie geht es dir?",
                  "pronunciation": "هالو! وی گیت اِس دیر؟",
                  "translationDari": "سلام! چطور هستی؟"
                },
                {
                  "speaker": "B",
                  "isSpeakerA": false,
                  "german": "Mir geht es gut, danke. Und dir?",
                  "pronunciation": "میر گیت اِس گوت، دانکه. اونت دیر؟",
                  "translationDari": "من خوب هستم، تشکر. و تو؟"
                }
              ]
            }
            ```
            فقط بلوک کد JSON را ارسال کنید بدون توضیحات اضافی.
        """.trimIndent()

        val rawResult = sendMessage(
            userMessage = prompt,
            apiKey = cleanKey,
            conversationHistory = emptyList(),
            onStatusUpdate = onStatusUpdate
        )

        rawResult.mapCatching { responseText ->
            val jsonStr = extractDialogueJson(responseText) ?: responseText
            val jsonClean = jsonStr.replace("```json", "").replace("```", "").trim()
            val linesList = mutableListOf<com.example.data.model.DialogueMakerLine>()

            val rootObj = if (jsonClean.startsWith("{")) JSONObject(jsonClean) else JSONObject().put("lines", JSONArray(jsonClean))
            val linesArr = rootObj.getJSONArray("lines")

            for (i in 0 until linesArr.length()) {
                val lineObj = linesArr.getJSONObject(i)
                linesList.add(com.example.data.model.DialogueMakerLine.fromJsonObject(lineObj))
            }

            if (linesList.isEmpty()) {
                throw GeminiChatException.GeneralException("مکالمه‌ای با خطوط معتبر از سرور دریافت نشد.")
            }

            linesList
        }
    }
}

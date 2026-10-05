package com.example.data.gemini

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

class GeminiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val systemInstructionText = """
        شما یک استاد مهربان و باحوصله زبان آلمانی برای یک زبان‌آموز سطح A1 به زبان فارسی دری هستید و همه چیز را به زبان دری توضیح می‌دهید.
        قوانین بسیار مهم:
        ۱. برای هر کلمه یا جمله آلمانی، الزامی است که تلفظ به خط فارسی و ترجمه دری آن را بنویسید (مثال: Guten Tag (گوتِن تاگ) - روز بخیر).
        ۲. هرگاه کاربر از شما سوال تمرینی یا درس خواست (مانند «۱۰ سوال تمرینی بساز» یا «یک درس جدید بساز»)، سوالات را با دقیقاً ۴ گزینه و پاسخ صحیح مشخص شده و توضیح کوتاه دری بسازید و آن‌ها را علاوه بر توضیحات متنی، در قالب یک بلوک کد JSON معتبر با اسکیمای زیر ارائه دهید:
        ```json
        {
          "id": "gemini_lesson_1",
          "number": 9,
          "titleGerman": "Neues Thema",
          "titleDari": "موضوع جدید",
          "vocabulary": [
            {
              "article": "der",
              "word": "Tisch",
              "pronunciationPersianScript": "تیش",
              "meaningDari": "میز"
            }
          ],
          "exampleSentences": [
            {
              "german": "Das ist ein Tisch.",
              "pronunciation": "داس ایست آین تیش.",
              "meaningDari": "این یک میز است."
            }
          ],
          "exercises": [
            {
              "type": "multiple-choice",
              "question": "___ Tisch ist groß.",
              "pronunciation": "دِر تیش ایست گروس.",
              "translationDari": "میز بزرگ است.",
              "options": ["Der", "Die", "Das", "Den"],
              "correctAnswer": "Der",
              "explanationDari": "کلمه Tisch مذکر است."
            }
          ],
          "dialogues": [
            {
              "lines": [
                {
                  "speaker": "سارا",
                  "german": "Hallo!",
                  "pronunciation": "هالو!",
                  "meaningDari": "سلام!"
                }
              ]
            }
          ]
        }
        ```
        ۳. هرگاه کاربر از شما مبحث گرامر یا تدریس قواعد گرامری خواست (مانند «گرامر آکوزاتیو را یاد بده» یا «یک مبحث گرامر بساز»)، قواعد را با توضیحات کامل دری، جملات نمونه (همراه با تلفظ به خط فارسی و ترجمه دری) و تمرین‌های ۴ گزینه‌ای آموزش دهید و علاوه بر توضیحات متنی، کد JSON مبحث گرامر را در قالب یک بلوک کد JSON معتبر با اسکیمای زیر ارائه دهید تا کاربر بتواند آن را از بخش «افزودن مبحث گرامر» وارد نماید:
        ```json
        {
          "id": "grammar_topic_1",
          "number": 2,
          "titleGerman": "Akkusativ",
          "titleDari": "حالت مفعولی مستقیم (Akkusativ)",
          "sections": [
            {
              "title": "تعریف و مفهوم حالت آکوزاتیو",
              "bodyDari": "حالت آکوزاتیو برای مفعول مستقیم جمله استفاده می‌شود. وقتی کاری روی چیزی یا کسی انجام می‌شود، آن اسم در حالت آکوزاتیو قرار می‌گیرد."
            },
            {
              "title": "تغییرات حروف تعریف در آکوزاتیو",
              "bodyDari": "در حالت آکوزاتیو فقط حرف تعریف مذکر (der / ein) تغییر می‌کند:\nder  ->  den\nein  ->  einen\nhier: die و das و صورت جمع بدون تغییر باقی می‌مانند."
            }
          ],
          "exampleSentences": [
            {
              "german": "Ich habe einen Hund.",
              "pronunciation": "ایش هابِه آینِن هوند.",
              "meaningDari": "من یک سگ دارم."
            },
            {
              "german": "Siehst du den Mann?",
              "pronunciation": "زیست دو دِن مان؟",
              "meaningDari": "آیا آن مرد را می‌بینی؟"
            }
          ],
          "exercises": [
            {
              "type": "multiple-choice",
              "question": "Ich kaufe ___ Tisch.",
              "pronunciation": "ایش کاوفِه ... تیش.",
              "translationDari": "من میز را می‌خرم.",
              "options": ["den", "der", "das", "die"],
              "correctAnswer": "den",
              "explanationDari": "کلمه Tisch مذکر است و در جایگاه مفعول مستقیم der به den تبدیل می‌شود."
            }
          ]
        }
        ```
        ۴. هرگاه کاربر از شما ساخت جمله خواست (مانند «۱۰ جمله بساز»)، جملات را به صورت لیست شماره‌دار با فرمت:
        جمله آلمانی + تلفظ به خط فارسی + ترجمه به زبان دری ارائه دهید.
    """.trimIndent()

    /**
     * Sends a chat message to Gemini with automated retry (up to 2 retries per model)
     * and fallback to alternate models when experiencing 503, 429, or overloaded server responses.
     */
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        apiKey: String,
        onStatusUpdate: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                GeminiChatException.KeyErrorException("کلید API جیمنای تنظیم نشده است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید.")
            )
        }

        val requestBodyJson = buildRequestBody(userMessage, conversationHistory)
        var lastBusy = false

        for (model in FALLBACK_MODELS) {
            // Up to 2 retries per model (total 3 attempts per model)
            val maxRetries = 2
            for (attempt in 0..maxRetries) {
                if (attempt > 0) {
                    onStatusUpdate?.invoke("سرور شلوغ است، دوباره تلاش میشود…")
                    val retryDelayMs = if (attempt == 1) 1000L else 1500L
                    delay(retryDelayMs)
                }

                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
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

                    // Check for 503 / 429 / overloaded
                    if (isServerBusy(responseCode, responseBody)) {
                        lastBusy = true
                        // Continue to next retry or next model
                        continue
                    }

                    // Other HTTP error code (e.g. 400 Bad Request not related to key)
                    return@withContext Result.failure(
                        GeminiChatException.GeneralException("خطا در ارتباط با سرور جیمنای (کد $responseCode)")
                    )

                } catch (e: IOException) {
                    // Network disconnect or timeout
                    if (attempt == maxRetries && model == FALLBACK_MODELS.last()) {
                        return@withContext Result.failure(
                            GeminiChatException.NetworkErrorException("خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید.")
                        )
                    }
                    onStatusUpdate?.invoke("سرور شلوغ است، دوباره تلاش میشود…")
                    delay(1000L)
                } catch (e: Exception) {
                    if (attempt == maxRetries && model == FALLBACK_MODELS.last()) {
                        return@withContext Result.failure(
                            GeminiChatException.GeneralException("خطای غیرمنتظره در ارتباط با جیمنای")
                        )
                    }
                }
            }

            // Before switching to the next fallback model
            if (model != FALLBACK_MODELS.last()) {
                onStatusUpdate?.invoke("سرور شلوغ است، دوباره تلاش میشود…")
                delay(1000L)
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

    private fun buildRequestBody(userMessage: String, conversationHistory: List<ChatMessage>): String {
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

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
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
        const val PRIMARY_MODEL = "gemini-3.5-flash"

        // Fallback chain in strict order: primary first, then 2.5-flash -> 2.5-flash-lite -> 2.0-flash (no duplicates)
        val FALLBACK_MODELS = listOf(
            PRIMARY_MODEL,
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
        ).distinct()

        fun isServerBusy(code: Int, responseBody: String): Boolean {
            if (code == 503 || code == 429 || code == 500 || code == 504 || code == 502) return true
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
    }
}

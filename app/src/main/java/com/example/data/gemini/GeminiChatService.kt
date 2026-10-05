package com.example.data.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val extractedLessonJson: String? = null
)

class GeminiChatService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                Exception("کلید API جیمنای تنظیم نشده است. لطفاً کلید رایگان خود را از Google AI Studio وارد کنید.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Add previous recent messages
            val recentTurns = conversationHistory.takeLast(6)
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

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody).optJSONObject("error")
                    errJson?.optString("message", "خطای دریافت پاسخ: کد ${response.code}") ?: "خطا ${response.code}"
                } catch (e: Exception) {
                    "خطای سرور جیمنای: کد ${response.code}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("پاسخی از جیمنای دریافت نشد."))
            }

            val firstCand = candidates.getJSONObject(0)
            val content = firstCand.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(Exception("خطا در اتصال به اینترنت یا سرور: ${e.localizedMessage ?: "نامشخص"}"))
        }
    }

    companion object {
        fun extractLessonJson(text: String): String? {
            val jsonStart = text.indexOf("```json")
            if (jsonStart != -1) {
                val afterStart = text.substring(jsonStart + 7)
                val jsonEnd = afterStart.indexOf("```")
                if (jsonEnd != -1) {
                    val potentialJson = afterStart.substring(0, jsonEnd).trim()
                    if (potentialJson.contains("\"vocabulary\"") || potentialJson.contains("\"exercises\"") || potentialJson.contains("\"sections\"")) {
                        return potentialJson
                    }
                }
            }
            return null
        }
    }
}

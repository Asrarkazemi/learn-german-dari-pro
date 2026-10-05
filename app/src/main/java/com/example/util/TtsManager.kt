package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.data.storage.UserProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class TtsManager(
    private val context: Context,
    private val apiKeyProvider: (() -> String)? = null,
    private val speedProvider: (() -> Float)? = null
) : TextToSpeech.OnInitListener {

    private val applicationContext: Context = context.applicationContext

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set
    var isGermanSupported: Boolean = false
        private set

    // CHANGE 8: Gemini TTS HTTP client & cache
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioCacheDir = File(applicationContext.cacheDir, "gemini_tts_cache").apply { mkdirs() }
    private var mediaPlayer: MediaPlayer? = null

    init {
        tts = createTts()
    }

    // CHANGE 7: Initialize TextToSpeech preferring Google TTS engine package
    private fun createTts(): TextToSpeech {
        val googleEngine = "com.google.android.tts"
        val isGoogleInstalled = try {
            applicationContext.packageManager.getPackageInfo(googleEngine, 0)
            true
        } catch (e: Exception) {
            false
        }

        return if (isGoogleInstalled) {
            try {
                TextToSpeech(applicationContext, this, googleEngine)
            } catch (e: Exception) {
                TextToSpeech(applicationContext, this)
            }
        } else {
            TextToSpeech(applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.GERMAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "German language is missing or not supported on this device.")
                isGermanSupported = false
                isInitialized = true
            } else {
                isGermanSupported = true
                isInitialized = true
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)

                // CHANGE 7: Select best installed de-DE voice (offline and high quality preferred)
                try {
                    val voices = tts?.voices
                    if (!voices.isNullOrEmpty()) {
                        val germanVoices = voices.filter { voice ->
                            voice.locale.language.equals("de", ignoreCase = true)
                        }
                        val bestVoice = germanVoices
                            .filter { it.locale.country.equals("DE", ignoreCase = true) }
                            .sortedWith(
                                compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                                    .thenByDescending { it.quality }
                            )
                            .firstOrNull() ?: germanVoices.firstOrNull()

                        if (bestVoice != null) {
                            tts?.voice = bestVoice
                            Log.d("TtsManager", "Selected German voice: ${bestVoice.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TtsManager", "Could not set custom voice: ${e.message}")
                }
            }
        } else {
            Log.e("TtsManager", "TTS initialization failed with code $status")
            isInitialized = false
            isGermanSupported = false
        }
    }

    fun getEffectiveApiKey(): String {
        return apiKeyProvider?.invoke()?.trim()
            ?: UserProgressManager.getInstance(applicationContext).getEffectiveGeminiApiKey().trim()
    }

    fun getEffectiveSpeed(): Float {
        return speedProvider?.invoke()
            ?: UserProgressManager.getInstance(applicationContext).getPlaybackSpeed()
    }

    fun isGeminiVoiceConfigured(): Boolean {
        return getEffectiveApiKey().isNotEmpty()
    }

    fun speak(text: String, speed: Float) {
        if (text.isBlank()) return

        // Clean German text (remove extra punctuation or Persian script if mixed)
        val cleanGerman = text.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
        val speechText = if (cleanGerman.isNotEmpty()) cleanGerman else text

        val apiKey = getEffectiveApiKey()

        if (apiKey.isNotEmpty()) {
            coroutineScope.launch {
                val success = playGeminiTts(speechText, speed, apiKey)
                if (!success) {
                    speakWithDeviceTts(speechText, speed)
                }
            }
        } else {
            speakWithDeviceTts(speechText, speed)
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        val speed = if (slow && getEffectiveSpeed() > 0.75f) 0.5f else getEffectiveSpeed()
        speak(text, speed)
    }

    fun speak(text: String) {
        speak(text, getEffectiveSpeed())
    }

    // FIX F (4 & 5): Test voice method supporting chosen speed with Dari error reporting
    suspend fun testGeminiVoice(
        sampleText: String = "Guten Tag! Ich lerne Deutsch.",
        speed: Float = getEffectiveSpeed(),
        keyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = keyOverride?.trim()?.ifEmpty { null } ?: getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید رایگان خود را از Google AI Studio وارد نمایید."))
        }

        try {
            val promptText = when {
                speed <= 0.75f -> {
                    "Speak this German phrase very slowly, clearly, and deliberately, syllable by syllable, for a beginner A1 German language learner: \"$sampleText\""
                }
                speed >= 1.25f -> {
                    "Speak this German text briskly, fluently, and faster at an advanced conversational pace: \"$sampleText\""
                }
                else -> {
                    "Pronounce this German text clearly and naturally with standard German (Hochdeutsch) pronunciation at normal native speed: \"$sampleText\""
                }
            }
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val apiErrMsg = try {
                    JSONObject(responseBodyStr).optJSONObject("error")?.optString("message")
                } catch (e: Exception) {
                    null
                } ?: "کد خطا: ${response.code}"
                return@withContext Result.failure(Exception("خطای API جیمنای ($apiErrMsg)"))
            }

            if (responseBodyStr.isBlank()) {
                return@withContext Result.failure(Exception("پاسخ سرور جیمنای خالی بود."))
            }

            val respObj = JSONObject(responseBodyStr)
            val candidates = respObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("هیچ کاندیدای صوتی توسط جیمنای تولید نشد."))
            }

            val firstCand = candidates.getJSONObject(0)
            val content = firstCand.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext Result.failure(Exception("محتوای صوتی در پاسخ جیمنای یافت نشد."))
            }

            var audioBase64: String? = null
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                val inlineData = p.optJSONObject("inlineData")
                if (inlineData != null) {
                    val data = inlineData.optString("data", "")
                    if (data.isNotBlank()) {
                        audioBase64 = data
                        break
                    }
                }
            }

            if (audioBase64.isNullOrBlank()) {
                return@withContext Result.failure(Exception("داده صوتی در خروجی جیمنای یافت نشد."))
            }

            val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
            if (rawAudioBytes.isEmpty()) {
                return@withContext Result.failure(Exception("بایت‌های فایل صوتی خالی بود."))
            }

            val wavBytes = ensureWavBytes(rawAudioBytes)
            val speedTag = String.format(Locale.US, "%.2f", speed)
            val testFileName = "test_voice_sample_${speedTag}.wav"
            val testFile = File(audioCacheDir, testFileName)
            testFile.outputStream().use { it.write(wavBytes) }

            val played = playAudioFile(testFile, speed)
            if (played) {
                val speedDesc = UserProgressManager.formatSpeedToPersian(speed)
                Result.success("صدای جیمنای با سرعت $speedDesc با موفقیت پخش شد! ✨")
            } else {
                Result.failure(Exception("فایل صوتی دریافت شد ولی پخش‌کننده گوشی نتوانست آن را پخش کند."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در برقراری ارتباط با جیمنای: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun testGeminiVoice(
        sampleText: String = "Guten Tag! Ich lerne Deutsch.",
        isSlow: Boolean,
        keyOverride: String? = null
    ): Result<String> {
        val speed = if (isSlow) 0.5f else getEffectiveSpeed()
        return testGeminiVoice(sampleText, speed, keyOverride)
    }

    // FIX F (4): Gemini TTS request with on-device file caching per speed and MediaPlayer playback speed
    private suspend fun playGeminiTts(text: String, speed: Float, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = getCacheKey(text, speed)
                val cachedFile = File(audioCacheDir, "$cacheKey.wav")

                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return@withContext playAudioFile(cachedFile, speed)
                }

                val promptText = when {
                    speed <= 0.75f -> {
                        "Speak this German phrase very slowly, clearly, and deliberately, syllable by syllable, for a beginner A1 German language learner: \"$text\""
                    }
                    speed >= 1.25f -> {
                        "Speak this German text briskly, fluently, and faster at an advanced conversational pace: \"$text\""
                    }
                    else -> {
                        "Pronounce this German text clearly and naturally with standard German (Hochdeutsch) pronunciation at normal native speed: \"$text\""
                    }
                }

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", promptText)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Kore")
                                })
                            })
                        })
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
                val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w("TtsManager", "Gemini TTS HTTP error: ${response.code}")
                    return@withContext false
                }

                val responseBodyStr = response.body?.string().orEmpty()
                if (responseBodyStr.isBlank()) return@withContext false

                val respObj = JSONObject(responseBodyStr)
                val candidates = respObj.optJSONArray("candidates") ?: return@withContext false
                if (candidates.length() == 0) return@withContext false

                val firstCand = candidates.getJSONObject(0)
                val content = firstCand.optJSONObject("content") ?: return@withContext false
                val parts = content.optJSONArray("parts") ?: return@withContext false
                if (parts.length() == 0) return@withContext false

                var audioBase64: String? = null
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val inlineData = p.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val data = inlineData.optString("data", "")
                        if (data.isNotBlank()) {
                            audioBase64 = data
                            break
                        }
                    }
                }

                if (audioBase64.isNullOrBlank()) return@withContext false

                val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                if (rawAudioBytes.isEmpty()) return@withContext false

                val wavBytes = ensureWavBytes(rawAudioBytes)
                cachedFile.outputStream().use { it.write(wavBytes) }

                return@withContext playAudioFile(cachedFile)
            } catch (e: Exception) {
                Log.w("TtsManager", "Gemini TTS failed silently: ${e.message}")
                false
            }
        }
    }

    private fun ensureWavBytes(audioBytes: ByteArray, sampleRate: Int = 24000): ByteArray {
        if (audioBytes.size >= 4 &&
            audioBytes[0] == 'R'.code.toByte() &&
            audioBytes[1] == 'I'.code.toByte() &&
            audioBytes[2] == 'F'.code.toByte() &&
            audioBytes[3] == 'F'.code.toByte()
        ) {
            return audioBytes
        }
        val totalAudioLen = audioBytes.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header + audioBytes
    }

    private suspend fun playAudioFile(file: File, speed: Float = 1.0f): Boolean = withContext(Dispatchers.Main) {
        try {
            stopAudio()
            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.prepare()

            // FIX F (4): Play the audio with playback-speed control set to the chosen speed
            try {
                val params = player.playbackParams
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.w("TtsManager", "Could not set MediaPlayer playback speed: ${e.message}")
            }

            player.start()
            player.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (mediaPlayer === mp) {
                    mediaPlayer = null
                }
                true
            }
            mediaPlayer = player
            true
        } catch (e: Exception) {
            Log.w("TtsManager", "MediaPlayer playback failed: ${e.message}")
            false
        }
    }

    // FIX F (3): Apply setSpeechRate(chosenSpeed) immediately before every speak() call
    private fun speakWithDeviceTts(speechText: String, speed: Float) {
        stopAudio()
        if (!isInitialized) {
            showToast("موتور صوتی گوشی هنوز آماده نشده است.")
            return
        }

        if (!isGermanSupported) {
            showToast("بسته صدای آلمانی روی گوشی شما نصب نیست. لطفاً در تنظیمات زبان گوشی (Text-to-Speech) صدای آلمانی را فعال کنید.")
            return
        }

        // Stop current utterance before setting rate
        tts?.stop()

        // Apply setSpeechRate(chosenSpeed) immediately before every speak() call
        tts?.setSpeechRate(speed)
        tts?.setPitch(1.0f)

        // Pass utterance Bundle with KEY_PARAM_VOLUME and explicit rate params for engines that under-react (e.g. Samsung)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat("rate", speed)
            putFloat("speechRate", speed)
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "german_tts_${System.currentTimeMillis()}")
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "german_tts_${System.currentTimeMillis()}")
    }

    private fun speakWithDeviceTts(speechText: String, slow: Boolean) {
        val speed = if (slow && getEffectiveSpeed() > 0.75f) 0.5f else getEffectiveSpeed()
        speakWithDeviceTts(speechText, speed)
    }

    // FIX F (4): Speed-specific cache files to avoid collisions
    private fun getCacheKey(text: String, speed: Float): String {
        val speedTag = String.format(Locale.US, "speed_%.2f", speed)
        val raw = "${text.trim()}_$speedTag"
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    private fun getCacheKey(text: String, isSlow: Boolean): String {
        val speed = if (isSlow) 0.5f else 1.0f
        return getCacheKey(text, speed)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun stop() {
        stopAudio()
        tts?.stop()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        coroutineScope.cancel()
    }
}

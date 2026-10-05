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

/**
 * FIX K + ADDITION: Quota-aware Gemini TTS with:
 * 1) PERMANENT, PRIVATE STORAGE: Internal Context.filesDir/voice_library/ (inherently private,
 *    never indexed by MediaScanner or visible in external music/gallery apps).
 * 2) ONE FILE PER SENTENCE: Keyed by text + voice only in neutral style; user playback speed
 *    is applied locally on playback.
 * 3) LIBRARY-FIRST: Stored sentences replay forever with ZERO API calls, even during quota cooldown.
 * 4) QUOTA COOLDOWN + FALLBACK: gemini-2.5-flash-tts -> gemini-2.5-flash-preview-tts -> device TTS.
 * 5) HONEST STATUS & SETTINGS: Live sentence count & MB stats + clear library dialog.
 */
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 1) Internal private permanent voice library: survives restarts/updates, invisible to MediaScanner
    private val voiceLibraryDir: File = getVoiceLibraryDir(applicationContext)

    // Legacy temporary cache dir (read-only fallback for older cached files)
    private val legacyCacheDir: File = File(applicationContext.cacheDir, "gemini_tts_cache").apply { mkdirs() }

    private var mediaPlayer: MediaPlayer? = null

    init {
        tts = createTts()
    }

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

    fun areAllTtsModelsInCooldown(): Boolean {
        return UserProgressManager.getInstance(applicationContext).areAllTtsModelsInCooldown()
    }

    fun isGeminiVoiceActive(): Boolean {
        return isGeminiVoiceConfigured() && !areAllTtsModelsInCooldown()
    }

    fun isModelInCooldown(model: String): Boolean {
        return UserProgressManager.getInstance(applicationContext).isTtsModelInCooldown(model)
    }

    fun setCooldown(model: String, cooldownUntilEpochMs: Long) {
        UserProgressManager.getInstance(applicationContext).setTtsCooldown(model, cooldownUntilEpochMs)
    }

    fun clearCooldowns() {
        UserProgressManager.getInstance(applicationContext).clearTtsCooldowns()
    }

    fun getVoiceLibraryStats(): VoiceLibraryStats {
        return getVoiceLibraryStats(applicationContext)
    }

    fun clearVoiceLibrary(): Boolean {
        return clearVoiceLibrary(applicationContext)
    }

    /**
     * Checks the permanent voice library and legacy cache for this sentence.
     * 1) Canonical text+voice file in private internal filesDir/voice_library/
     * 2) Legacy speed-keyed file in filesDir/voice_library/
     * 3) Legacy speed-keyed file in cacheDir/gemini_tts_cache/
     * 4) Legacy neutral file in cacheDir/gemini_tts_cache/
     */
    fun findStoredAudioFile(text: String, speed: Float = 1.0f): File? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        // 1) Canonical permanent voice library file (keyed by text + voice only)
        val canonicalKey = getVoiceLibraryKey(clean)
        val libraryFile = File(voiceLibraryDir, "$canonicalKey.wav")
        if (libraryFile.exists() && libraryFile.length() > 0) {
            return libraryFile
        }

        // 2) Legacy check in voiceLibraryDir (in case any old speed-keyed files exist)
        val legacySpeedKey = getLegacyCacheKey(clean, speed)
        val legacyInLibrary = File(voiceLibraryDir, "$legacySpeedKey.wav")
        if (legacyInLibrary.exists() && legacyInLibrary.length() > 0) {
            return legacyInLibrary
        }

        // 3) Legacy check in cacheDir/gemini_tts_cache
        val legacyInCache = File(legacyCacheDir, "$legacySpeedKey.wav")
        if (legacyInCache.exists() && legacyInCache.length() > 0) {
            return legacyInCache
        }

        val legacyNeutralInCache = File(legacyCacheDir, "$canonicalKey.wav")
        if (legacyNeutralInCache.exists() && legacyNeutralInCache.length() > 0) {
            return legacyNeutralInCache
        }

        return null
    }

    fun getVoiceLibraryFile(text: String): File {
        val key = getVoiceLibraryKey(text)
        return File(voiceLibraryDir, "$key.wav")
    }

    /**
     * Main speech entrypoint called by flashcards, sentences, dialogues, Q&A, exercises,
     * grammar topics, and numbers.
     * Library is checked FIRST: if stored, plays with zero API usage even during quota cooldown.
     */
    fun speak(text: String, speed: Float) {
        if (text.isBlank()) return

        val cleanGerman = text.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
        val speechText = if (cleanGerman.isNotEmpty()) cleanGerman else text

        val apiKey = getEffectiveApiKey()

        coroutineScope.launch {
            // First check library/cache: if stored, plays without API key or cooldown check!
            val storedAudio = findStoredAudioFile(speechText, speed)
            if (storedAudio != null) {
                Log.d("TtsManager", "Permanent voice library hit for: \"$speechText\" -> ${storedAudio.name}. Playing instantly with zero API.")
                val played = playAudioFile(storedAudio, speed)
                if (played) return@launch
            }

            if (apiKey.isNotEmpty()) {
                val success = playGeminiTts(speechText, speed, apiKey)
                if (!success) {
                    speakWithDeviceTts(speechText, speed)
                }
            } else {
                speakWithDeviceTts(speechText, speed)
            }
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        val speed = if (slow && getEffectiveSpeed() > 0.75f) 0.5f else getEffectiveSpeed()
        speak(text, speed)
    }

    fun speak(text: String) {
        speak(text, getEffectiveSpeed())
    }

    /**
     * Tests Gemini voice with current speed, supporting library check, model fallback,
     * quota detection, and honest Dari reporting.
     */
    suspend fun testGeminiVoice(
        sampleText: String = "Guten Tag! Ich lerne Deutsch.",
        speed: Float = getEffectiveSpeed(),
        keyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanSample = sampleText.replace(Regex("[\\u0600-\\u06FF]"), "").trim().ifEmpty { sampleText }

        // 1) First check if sample is already in library (plays even during cooldown)
        val storedFile = findStoredAudioFile(cleanSample, speed)
        if (storedFile != null) {
            val played = playAudioFile(storedFile, speed)
            if (played) {
                val speedDesc = UserProgressManager.formatSpeedToPersian(speed)
                return@withContext Result.success("صدای جیمنای (از حافظهٔ دائمی برنامه) با سرعت $speedDesc با موفقیت پخش شد! ✨")
            }
        }

        val apiKey = keyOverride?.trim()?.ifEmpty { null } ?: getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید رایگان خود را از Google AI Studio وارد نمایید."))
        }

        // 2) Honest status: if all models are in cooldown, immediately return the Dari quota message
        if (areAllTtsModelsInCooldown()) {
            return@withContext Result.failure(Exception(QUOTA_EXCEEDED_DARI_MSG))
        }

        try {
            // Neutral style prompt — generated once per sentence; playback speed is applied locally
            val promptText = buildNeutralPrompt(cleanSample)
            val requestBodyStr = buildTtsRequestBody(promptText).toString()
            var sawQuotaError = false
            var sawKeyError = false

            for (model in TTS_MODELS) {
                if (isModelInCooldown(model)) {
                    sawQuotaError = true
                    continue
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseCode = response.code
                val responseBodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val audioBase64 = extractAudioBase64(responseBodyStr)
                    if (!audioBase64.isNullOrBlank()) {
                        val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        if (rawAudioBytes.isNotEmpty()) {
                            val wavBytes = ensureWavBytes(rawAudioBytes)

                            // Save into permanent private voice library
                            val canonicalKey = getVoiceLibraryKey(cleanSample)
                            val libraryFile = File(voiceLibraryDir, "$canonicalKey.wav")
                            libraryFile.outputStream().use { it.write(wavBytes) }
                            Log.d("TtsManager", "Saved test audio to voice library: ${libraryFile.name}")

                            val played = playAudioFile(libraryFile, speed)
                            if (played) {
                                val speedDesc = UserProgressManager.formatSpeedToPersian(speed)
                                return@withContext Result.success("صدای جیمنای با سرعت $speedDesc با موفقیت پخش و ذخیره شد! ✨")
                            } else {
                                return@withContext Result.failure(Exception("فایل صوتی دریافت شد ولی پخش‌کننده گوشی نتوانست آن را پخش کند."))
                            }
                        }
                    }
                }

                if (isQuotaError(responseCode, responseBodyStr)) {
                    sawQuotaError = true
                    val cooldownMs = parseCooldownDurationMs(responseBodyStr, response.header("Retry-After"))
                    setCooldown(model, System.currentTimeMillis() + cooldownMs)
                    continue
                }

                if (isKeyError(responseCode, responseBodyStr)) {
                    sawKeyError = true
                    return@withContext Result.failure(Exception("کلید API جیمنای نامعتبر است. لطفاً کلید صحیح خود را از Google AI Studio وارد نمایید."))
                }
            }

            if (sawQuotaError) {
                return@withContext Result.failure(Exception(QUOTA_EXCEEDED_DARI_MSG))
            }

            if (sawKeyError) {
                return@withContext Result.failure(Exception("کلید API جیمنای نامعتبر است. لطفاً کلید صحیح خود را از Google AI Studio وارد نمایید."))
            }

            Result.failure(Exception("خطا در برقراری ارتباط با سرویس صوتی؛ لطفاً بعداً دوباره امتحان کنید."))
        } catch (e: Exception) {
            Result.failure(Exception("خطا در اتصال به اینترنت یا سرور جیمنای."))
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

    /**
     * Plays Gemini TTS audio.
     * CRITICAL: Voice library / cache is ALWAYS checked first. Cached audio replays forever
     * without any network call or quota consumption.
     * Any new synthesis is saved to Context.filesDir/voice_library/ permanently.
     */
    private suspend fun playGeminiTts(text: String, speed: Float, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1) Library and cache check first
                val storedAudio = findStoredAudioFile(text, speed)
                if (storedAudio != null) {
                    return@withContext playAudioFile(storedAudio, speed)
                }

                // 2) Cooldown check before network call
                if (areAllTtsModelsInCooldown()) {
                    Log.d("TtsManager", "All TTS models are in quota cooldown and phrase not in library; falling back to device TTS.")
                    return@withContext false
                }

                // 3) Synthesize once in natural/neutral style; playback speed applied locally on player
                val promptText = buildNeutralPrompt(text)
                val requestJson = buildTtsRequestBody(promptText)
                val requestBodyStr = requestJson.toString()

                for (model in TTS_MODELS) {
                    if (isModelInCooldown(model)) {
                        Log.d("TtsManager", "Model $model is in quota cooldown, skipping.")
                        continue
                    }

                    try {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                        val requestBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        val response = httpClient.newCall(request).execute()
                        val responseCode = response.code
                        val responseBodyStr = response.body?.string().orEmpty()

                        if (response.isSuccessful) {
                            val audioBase64 = extractAudioBase64(responseBodyStr)
                            if (!audioBase64.isNullOrBlank()) {
                                val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                                if (rawAudioBytes.isNotEmpty()) {
                                    val wavBytes = ensureWavBytes(rawAudioBytes)

                                    // Save to permanent internal voice library
                                    val canonicalKey = getVoiceLibraryKey(text)
                                    val libraryFile = File(voiceLibraryDir, "$canonicalKey.wav")
                                    libraryFile.outputStream().use { it.write(wavBytes) }
                                    Log.d("TtsManager", "Saved audio to permanent internal voice library: ${libraryFile.name} (${libraryFile.length()} bytes)")

                                    return@withContext playAudioFile(libraryFile, speed)
                                }
                            }
                        }

                        if (isQuotaError(responseCode, responseBodyStr)) {
                            val cooldownMs = parseCooldownDurationMs(responseBodyStr, response.header("Retry-After"))
                            setCooldown(model, System.currentTimeMillis() + cooldownMs)
                            Log.w("TtsManager", "Quota exceeded for $model. Cooldown set for ${cooldownMs / 1000}s.")
                            continue
                        }

                        if (isKeyError(responseCode, responseBodyStr)) {
                            Log.w("TtsManager", "API key error for $model: $responseCode")
                            return@withContext false
                        }

                    } catch (e: Exception) {
                        Log.w("TtsManager", "Error attempting Gemini TTS with $model: ${e.message}")
                    }
                }

                // If both models are in cooldown or failed -> return false to trigger device fallback
                false
            } catch (e: Exception) {
                Log.w("TtsManager", "Gemini TTS failed silently: ${e.message}")
                false
            }
        }
    }

    private fun buildTtsRequestBody(promptText: String): JSONObject {
        return JSONObject().apply {
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
                            put("voiceName", DEFAULT_VOICE_NAME)
                        })
                    })
                })
            })
        }
    }

    private fun extractAudioBase64(responseBodyStr: String): String? {
        if (responseBodyStr.isBlank()) return null
        return try {
            val respObj = JSONObject(responseBodyStr)
            val candidates = respObj.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCand = candidates.getJSONObject(0)
            val content = firstCand.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                val inlineData = p.optJSONObject("inlineData")
                if (inlineData != null) {
                    val data = inlineData.optString("data", "")
                    if (data.isNotBlank()) {
                        return data
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
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

        tts?.stop()
        tts?.setSpeechRate(speed)
        tts?.setPitch(1.0f)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat("rate", speed)
            putFloat("speechRate", speed)
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "german_tts_${System.currentTimeMillis()}")
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "german_tts_${System.currentTimeMillis()}")
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

    data class VoiceLibraryStats(
        val count: Int,
        val totalBytes: Long,
        val sizeMb: Double
    ) {
        val formattedSize: String
            get() = String.format(Locale.US, "%.1f", sizeMb)
    }

    companion object {
        const val PRIMARY_TTS_MODEL = "gemini-2.5-flash-tts"
        const val SECONDARY_TTS_MODEL = "gemini-2.5-flash-preview-tts"

        val TTS_MODELS = listOf(PRIMARY_TTS_MODEL, SECONDARY_TTS_MODEL)

        const val DEFAULT_VOICE_NAME = "Kore"

        const val QUOTA_EXCEEDED_DARI_MSG = "سهمیۀ رایگان روزانۀ صدای جیمنای تمام شده است (۱۰ جمله در روز). صدا موقتاً از گوشی پخش میشود؛ جملههایی که قبلاً با صدای جیمنای پخش شدهاند از حافظه پخش میشوند."

        fun buildNeutralPrompt(text: String): String {
            return "Pronounce this German text clearly and naturally with standard German (Hochdeutsch) pronunciation at normal native speed: \"$text\""
        }

        fun buildSpeedPrompt(text: String, speed: Float): String {
            return when {
                speed <= 0.75f -> {
                    "Speak this German phrase very slowly, clearly, and deliberately, syllable by syllable, for a beginner A1 German language learner: \"$text\""
                }
                speed >= 1.25f -> {
                    "Speak this German text briskly, fluently, and faster at an advanced conversational pace: \"$text\""
                }
                else -> {
                    buildNeutralPrompt(text)
                }
            }
        }

        /**
         * Returns internal private storage directory: Context.filesDir/voice_library/.
         * Guaranteed to be inside internal private storage (no media scanner indexing).
         */
        fun getVoiceLibraryDir(context: Context): File {
            val dir = File(context.applicationContext.filesDir, "voice_library")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            try {
                val noMedia = File(dir, ".nomedia")
                if (!noMedia.exists()) {
                    noMedia.createNewFile()
                }
            } catch (e: Exception) {
                // ignore
            }
            return dir
        }

        /**
         * Key the library by the TEXT (plus voice) ONLY.
         */
        fun getVoiceLibraryKey(text: String, voiceName: String = DEFAULT_VOICE_NAME): String {
            val raw = "${voiceName}_${text.trim()}"
            return try {
                val digest = MessageDigest.getInstance("MD5")
                val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
                hash.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                raw.hashCode().toString()
            }
        }

        /**
         * Legacy cache key (speed-keyed) for backward compatibility with previous cache entries.
         */
        fun getLegacyCacheKey(text: String, speed: Float): String {
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

        /**
         * Live count of stored sentences and total size in MB.
         */
        fun getVoiceLibraryStats(context: Context): VoiceLibraryStats {
            val dir = getVoiceLibraryDir(context)
            val files = dir.listFiles { file ->
                file.isFile && file.extension.equals("wav", ignoreCase = true)
            } ?: emptyArray()
            val totalBytes = files.sumOf { it.length() }
            val sizeMb = totalBytes / (1024.0 * 1024.0)
            return VoiceLibraryStats(
                count = files.size,
                totalBytes = totalBytes,
                sizeMb = sizeMb
            )
        }

        /**
         * Empties the private voice library directory.
         */
        fun clearVoiceLibrary(context: Context): Boolean {
            val dir = getVoiceLibraryDir(context)
            var success = true
            dir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.equals(".nomedia")) {
                    if (!file.delete()) {
                        success = false
                    }
                }
            }
            return success
        }

        fun isQuotaError(responseCode: Int, responseBody: String): Boolean {
            if (responseCode == 429) return true
            val lower = responseBody.lowercase()
            return lower.contains("exceeded your current quota") ||
                    lower.contains("quota exceeded") ||
                    lower.contains("resource_exhausted") ||
                    lower.contains("generate_content_free_tier_requests") ||
                    lower.contains("quota")
        }

        fun isKeyError(responseCode: Int, responseBody: String): Boolean {
            if (responseCode == 401 || responseCode == 403) return true
            val lower = responseBody.lowercase()
            return lower.contains("api_key_invalid") ||
                    lower.contains("api key not valid") ||
                    lower.contains("invalid api key") ||
                    lower.contains("permission_denied")
        }

        fun parseCooldownDurationMs(responseBody: String, retryAfterHeader: String?): Long {
            retryAfterHeader?.trim()?.toLongOrNull()?.let { seconds ->
                if (seconds > 0) return (seconds * 1000L) + 15_000L
            }

            val hMRegex = Regex("""retry in\s+(\d+)h\s*(\d+)m""", RegexOption.IGNORE_CASE)
            hMRegex.find(responseBody)?.let { match ->
                val hours = match.groupValues[1].toLongOrNull() ?: 0L
                val minutes = match.groupValues[2].toLongOrNull() ?: 0L
                val totalMs = (hours * 3600_000L) + (minutes * 60_000L) + 30_000L
                if (totalMs > 0) return totalMs
            }

            val hRegex = Regex("""retry in\s+(\d+)h""", RegexOption.IGNORE_CASE)
            hRegex.find(responseBody)?.let { match ->
                val hours = match.groupValues[1].toLongOrNull() ?: 0L
                val totalMs = (hours * 3600_000L) + 30_000L
                if (totalMs > 0) return totalMs
            }

            val mRegex = Regex("""retry in\s+(\d+)m""", RegexOption.IGNORE_CASE)
            mRegex.find(responseBody)?.let { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val totalMs = (minutes * 60_000L) + 30_000L
                if (totalMs > 0) return totalMs
            }

            val sRegex = Regex("""retry (?:in|after)\s+(\d+)s""", RegexOption.IGNORE_CASE)
            sRegex.find(responseBody)?.let { match ->
                val seconds = match.groupValues[1].toLongOrNull() ?: 0L
                val totalMs = (seconds * 1000L) + 10_000L
                if (totalMs > 0) return totalMs
            }

            return 12L * 3600_000L
        }
    }
}

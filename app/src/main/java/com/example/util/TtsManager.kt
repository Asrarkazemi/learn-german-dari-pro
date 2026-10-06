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
import com.example.data.model.GrammarTopic
import com.example.data.model.LessonData
import com.example.data.storage.UserProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
 * FIX K, FIX L, & FIX M: Quota-aware Gemini TTS with:
 * 1) PERMANENT, PRIVATE STORAGE: Internal Context.filesDir/voice_library/ (inherently private,
 *    never indexed by MediaScanner or visible in external music/gallery apps).
 * 2) ONE FILE PER SENTENCE: Keyed by text + voice only in neutral style; user playback speed
 *    is applied locally on playback.
 * 3) LIBRARY-FIRST: Stored sentences replay forever with ZERO API calls, even during quota cooldown.
 * 4) QUOTA COOLDOWN + FALLBACK: gemini-2.5-flash-tts -> gemini-2.5-flash-preview-tts -> device TTS.
 * 5) HONEST STATUS & SETTINGS: Live sentence count & MB stats + clear library dialog.
 * 6) TRANSFER: Export to ZIP (voice-library.zip) + Import with merge-by-filename & zip-slip defense.
 * 7) BATCH DOWNLOAD (FIX M): One-tap grouped batch download for lessons and grammar topics.
 *    Pack into groups (<=20 items, <=900 chars), native Kotlin PCM RMS silence-splitting (>=0.8s),
 *    per-sentence fallback on segment mismatch, daily real API call counter.
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

    // Internal private permanent voice library: survives restarts/updates, invisible to MediaScanner
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

    fun getDailyRequestCount(): Int {
        return UserProgressManager.getInstance(applicationContext).getDailyGeminiRequestsCount()
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

                UserProgressManager.getInstance(applicationContext).incrementDailyGeminiRequestsCount()
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

                        UserProgressManager.getInstance(applicationContext).incrementDailyGeminiRequestsCount()
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

    /**
     * FIX M: Batch voice download for whole lesson or topic.
     * Gathers texts, skips existing, packs into groups (<=20 items, <=900 chars),
     * makes single grouped Gemini TTS calls, performs native PCM RMS silence splitting (>=0.8s),
     * and saves to voice library under canonical filenames.
     * On segment count mismatch, falls back for THAT GROUP ONLY to per-sentence generation.
     */
    /**
     * FIX M: Batch voice download for whole lesson or topic.
     * ADAPTIVE BATCHING:
     * First group attempt is largest possible: up to 6000 joined characters.
     * On failure or split-segment mismatch, halves group (6000 -> 3000 -> 1500 -> 900 chars / max 20 items)
     * and continues the rest at the size that works.
     * POSITION-GUIDED SLICING:
     * Decodes returned audio to PCM, detects >=0.6s silence gaps, maps cuts to cumulative text char ratios,
     * trims segments. If mismatch/failure, falls back for that group only to per-sentence synthesis.
     * Keeps track of total API requests used and updates daily requests counter.
     */
    suspend fun batchDownloadTexts(
        texts: List<String>,
        onProgress: (currentGroup: Int, totalGroups: Int) -> Unit
    ): BatchDownloadResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext BatchDownloadResult(
                isSuccess = false,
                savedCount = 0,
                skippedCount = 0,
                requestsUsed = 0,
                errorMessage = "کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید خود را در تنظیمات صدا وارد کنید."
            )
        }

        if (areAllTtsModelsInCooldown()) {
            return@withContext BatchDownloadResult(
                isSuccess = false,
                savedCount = 0,
                skippedCount = 0,
                requestsUsed = 0,
                errorMessage = QUOTA_EXCEEDED_DARI_MSG
            )
        }

        val pendingTexts = mutableListOf<String>()
        var skippedCount = 0

        // Filter out texts that already exist in permanent library
        for (rawText in texts) {
            val clean = cleanGermanText(rawText)
            if (clean.isBlank()) continue
            val canonicalKey = getVoiceLibraryKey(clean)
            val existingFile = File(voiceLibraryDir, "$canonicalKey.wav")
            if (existingFile.exists() && existingFile.length() > 0) {
                skippedCount++
            } else {
                pendingTexts.add(clean)
            }
        }

        if (pendingTexts.isEmpty()) {
            return@withContext BatchDownloadResult(
                isSuccess = true,
                savedCount = 0,
                skippedCount = skippedCount,
                requestsUsed = 0
            )
        }

        val tierLimits = listOf(6000, 3000, 1500, 900)
        var currentTierIndex = 0
        var textPointer = 0
        var savedCount = 0
        var totalRequestsUsed = 0
        var groupStepNumber = 0

        while (textPointer < pendingTexts.size) {
            currentCoroutineContext().ensureActive()
            groupStepNumber++

            if (areAllTtsModelsInCooldown()) {
                return@withContext BatchDownloadResult(
                    isSuccess = savedCount > 0,
                    savedCount = savedCount,
                    skippedCount = skippedCount,
                    requestsUsed = totalRequestsUsed,
                    errorMessage = QUOTA_EXCEEDED_DARI_MSG
                )
            }

            val maxChars = tierLimits[currentTierIndex]
            val maxItems = if (currentTierIndex >= 3) 20 else 100

            // Take next group from pendingTexts starting at textPointer
            val group = mutableListOf<String>()
            var groupChars = 0
            for (i in textPointer until pendingTexts.size) {
                val candidate = pendingTexts[i]
                val added = if (group.isEmpty()) candidate.length else candidate.length + 1
                if (group.size >= maxItems || (groupChars + added > maxChars && group.isNotEmpty())) {
                    break
                }
                group.add(candidate)
                groupChars += added
            }

            if (group.isEmpty()) break

            val remainingAfterGroup = pendingTexts.size - textPointer - group.size
            val estimatedAdditional = if (group.isEmpty()) 0 else (remainingAfterGroup + group.size - 1) / group.size
            val estimatedTotalSteps = maxOf(groupStepNumber, groupStepNumber + estimatedAdditional)

            withContext(Dispatchers.Main) {
                onProgress(groupStepNumber, estimatedTotalSteps)
            }

            val joined = group.joinToString("\n")
            val promptText = "$BATCH_STYLE_INSTRUCTION\n\n$joined"
            val requestBodyStr = buildTtsRequestBody(promptText).toString()

            var groupSuccess = false
            var wavBytes: ByteArray? = null

            for (model in TTS_MODELS) {
                if (isModelInCooldown(model)) continue

                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val requestBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    totalRequestsUsed++
                    UserProgressManager.getInstance(applicationContext).incrementDailyGeminiRequestsCount()
                    val response = httpClient.newCall(request).execute()
                    val responseCode = response.code
                    val responseBodyStr = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val audioBase64 = extractAudioBase64(responseBodyStr)
                        if (!audioBase64.isNullOrBlank()) {
                            val rawBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                            if (rawBytes.isNotEmpty()) {
                                wavBytes = ensureWavBytes(rawBytes)
                                groupSuccess = true
                                break
                            }
                        }
                    }

                    if (isQuotaError(responseCode, responseBodyStr)) {
                        val cooldownMs = parseCooldownDurationMs(responseBodyStr, response.header("Retry-After"))
                        setCooldown(model, System.currentTimeMillis() + cooldownMs)
                        continue
                    }

                    if (isKeyError(responseCode, responseBodyStr)) {
                        return@withContext BatchDownloadResult(
                            isSuccess = savedCount > 0,
                            savedCount = savedCount,
                            skippedCount = skippedCount,
                            requestsUsed = totalRequestsUsed,
                            errorMessage = "کلید API جیمنای نامعتبر است."
                        )
                    }
                } catch (e: Exception) {
                    Log.w("TtsManager", "Batch call failed for model $model: ${e.message}")
                }
            }

            var slicedSegments: List<ByteArray>? = null
            if (groupSuccess && wavBytes != null) {
                slicedSegments = splitAudioPositionGuided(wavBytes, group)
            }

            if (slicedSegments != null && slicedSegments.size == group.size) {
                // Success with position-guided slicing!
                for (i in group.indices) {
                    val key = getVoiceLibraryKey(group[i])
                    val targetFile = File(voiceLibraryDir, "$key.wav")
                    targetFile.outputStream().use { it.write(slicedSegments[i]) }
                    savedCount++
                }
                textPointer += group.size
                // Continue rest at the size that works (keep currentTierIndex)
            } else {
                // Mismatch or API failure! Halve or fallback
                if (currentTierIndex < tierLimits.size - 1) {
                    currentTierIndex++
                    // Do not advance textPointer; retry next attempt at smaller size
                } else {
                    // Already at smallest tier (900 chars / 20 items): fallback for this group to per-sentence
                    Log.w("TtsManager", "Group slicing failed at smallest tier; falling back to per-sentence synthesis for ${group.size} items.")
                    for (singleText in group) {
                        currentCoroutineContext().ensureActive()
                        val (singleOk, singleRequests) = synthesizeAndSaveSingleSentenceWithCount(singleText, apiKey)
                        totalRequestsUsed += singleRequests
                        if (singleOk) {
                            savedCount++
                        } else {
                            if (areAllTtsModelsInCooldown()) {
                                return@withContext BatchDownloadResult(
                                    isSuccess = savedCount > 0,
                                    savedCount = savedCount,
                                    skippedCount = skippedCount,
                                    requestsUsed = totalRequestsUsed,
                                    errorMessage = QUOTA_EXCEEDED_DARI_MSG
                                )
                            }
                        }
                    }
                    textPointer += group.size
                }
            }
        }

        BatchDownloadResult(
            isSuccess = true,
            savedCount = savedCount,
            skippedCount = skippedCount,
            requestsUsed = totalRequestsUsed
        )
    }

    private suspend fun synthesizeAndSaveSingleSentenceWithCount(text: String, apiKey: String): Pair<Boolean, Int> {
        if (areAllTtsModelsInCooldown()) return Pair(false, 0)
        val canonicalKey = getVoiceLibraryKey(text)
        val targetFile = File(voiceLibraryDir, "$canonicalKey.wav")
        if (targetFile.exists() && targetFile.length() > 0) return Pair(true, 0)

        val promptText = buildNeutralPrompt(text)
        val requestJson = buildTtsRequestBody(promptText)
        val requestBodyStr = requestJson.toString()
        var requestsCount = 0

        for (model in TTS_MODELS) {
            if (isModelInCooldown(model)) continue
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestBody = requestBodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(requestBody).build()

                requestsCount++
                UserProgressManager.getInstance(applicationContext).incrementDailyGeminiRequestsCount()
                val response = httpClient.newCall(request).execute()
                val responseCode = response.code
                val responseBodyStr = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val audioBase64 = extractAudioBase64(responseBodyStr)
                    if (!audioBase64.isNullOrBlank()) {
                        val rawAudioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        if (rawAudioBytes.isNotEmpty()) {
                            val wavBytes = ensureWavBytes(rawAudioBytes)
                            targetFile.outputStream().use { it.write(wavBytes) }
                            return Pair(true, requestsCount)
                        }
                    }
                }

                if (isQuotaError(responseCode, responseBodyStr)) {
                    val cooldownMs = parseCooldownDurationMs(responseBodyStr, response.header("Retry-After"))
                    setCooldown(model, System.currentTimeMillis() + cooldownMs)
                    continue
                }

                if (isKeyError(responseCode, responseBodyStr)) {
                    return Pair(false, requestsCount)
                }
            } catch (e: Exception) {
                Log.w("TtsManager", "Single synthesis failed for $model: ${e.message}")
            }
        }
        return Pair(false, requestsCount)
    }

    private suspend fun synthesizeAndSaveSingleSentence(text: String, apiKey: String): Boolean {
        return synthesizeAndSaveSingleSentenceWithCount(text, apiKey).first
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

    data class VoiceLibraryImportResult(
        val isSuccess: Boolean,
        val addedCount: Int = 0,
        val skippedCount: Int = 0,
        val errorMessage: String? = null
    ) {
        fun formatDariReport(): String {
            val addedPersian = toPersianDigits("$addedCount")
            val skippedPersian = toPersianDigits("$skippedCount")
            return "$addedPersian جملهٔ تازه اضافه شد — $skippedPersian جمله از قبل بود"
        }
    }

    data class BatchDownloadResult(
        val isSuccess: Boolean,
        val savedCount: Int,
        val skippedCount: Int,
        val requestsUsed: Int = 0,
        val errorMessage: String? = null
    ) {
        fun formatSummary(): String {
            val req = toPersianDigits("$requestsUsed")
            val n = toPersianDigits("$savedCount")
            val m = toPersianDigits("$skippedCount")
            return when {
                savedCount > 0 && skippedCount > 0 -> "✅ $n جمله ذخیره شد (کل درس با $req درخواست ذخیره شد) — $m جمله از قبل بود"
                savedCount > 0 -> "✅ $n جمله ذخیره شد (کل درس با $req درخواست ذخیره شد)"
                skippedCount > 0 -> "✅ همهٔ جملات ($m جمله) از قبل در حافظه موجود بودند"
                else -> "✅ هیچ جمله‌ای برای ذخیره‌سازی یافت نشد."
            }
        }
    }

    companion object {
        const val PRIMARY_TTS_MODEL = "gemini-2.5-flash-tts"
        const val SECONDARY_TTS_MODEL = "gemini-2.5-flash-preview-tts"

        val TTS_MODELS = listOf(PRIMARY_TTS_MODEL, SECONDARY_TTS_MODEL)

        const val DEFAULT_VOICE_NAME = "Kore"

        const val BATCH_MAX_ITEMS_PER_GROUP = 20
        const val BATCH_MAX_CHARS_PER_GROUP = 900
        const val BATCH_STYLE_INSTRUCTION = "Read each line exactly as written, in order, add nothing, leave about 2 seconds of complete silence between lines."

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

        fun ensureWavBytes(audioBytes: ByteArray, sampleRate: Int = 24000): ByteArray {
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

        fun extractPcmData(wavBytes: ByteArray): Pair<ShortArray, Int> {
            var sampleRate = 24000
            var pcmOffset = 0
            if (wavBytes.size >= 44 &&
                wavBytes[0] == 'R'.code.toByte() &&
                wavBytes[1] == 'I'.code.toByte() &&
                wavBytes[2] == 'F'.code.toByte() &&
                wavBytes[3] == 'F'.code.toByte()
            ) {
                val sr = (wavBytes[24].toInt() and 0xFF) or
                        ((wavBytes[25].toInt() and 0xFF) shl 8) or
                        ((wavBytes[26].toInt() and 0xFF) shl 16) or
                        ((wavBytes[27].toInt() and 0xFF) shl 24)
                if (sr in 8000..48000) {
                    sampleRate = sr
                }
                pcmOffset = 44
            }

            val numSamples = maxOf(0, (wavBytes.size - pcmOffset) / 2)
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val idx = pcmOffset + i * 2
                val low = wavBytes[idx].toInt() and 0xFF
                val high = wavBytes[idx + 1].toInt()
                samples[i] = ((high shl 8) or low).toShort()
            }
            return Pair(samples, sampleRate)
        }

        fun pcmShortsToBytes(shorts: ShortArray): ByteArray {
            val bytes = ByteArray(shorts.size * 2)
            for (i in shorts.indices) {
                val s = shorts[i].toInt()
                bytes[2 * i] = (s and 0xFF).toByte()
                bytes[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
            }
            return bytes
        }

        data class SilenceSpan(
            val startSample: Int,
            val endSample: Int,
            val midSample: Int
        )

        /**
         * FIX M (POSITION-GUIDED SLICING):
         * 1) Decodes to PCM
         * 2) Finds silence spans (RMS near zero, minimum gap ~0.6s)
         * 3) Computes expected boundary positions from cumulative character proportions of the texts
         * 4) Chooses exactly N-1 cuts, each at the midpoint of the silence span nearest its expected boundary
         * 5) Trims segment edges
         * 6) Sanity checks (each segment >= ~0.3s and non-silent; at least N-1 usable silences)
         * Returns List<ByteArray> of exactly texts.size segments, or null on any failure/mismatch.
         */
        fun splitAudioPositionGuided(
            wavBytes: ByteArray,
            texts: List<String>
        ): List<ByteArray>? {
            if (texts.isEmpty()) return null
            val (samples, sampleRate) = extractPcmData(wavBytes)
            if (samples.isEmpty()) return null

            val n = texts.size
            val frameDurationMs = 20
            val frameSize = (sampleRate * frameDurationMs) / 1000
            if (frameSize <= 0 || samples.size < frameSize) {
                return null
            }

            val numFrames = samples.size / frameSize
            val rmsList = DoubleArray(numFrames)
            var maxRms = 0.0

            for (f in 0 until numFrames) {
                val start = f * frameSize
                var sumSq = 0.0
                for (i in 0 until frameSize) {
                    val s = samples[start + i].toDouble()
                    sumSq += s * s
                }
                val rms = Math.sqrt(sumSq / frameSize)
                rmsList[f] = rms
                if (rms > maxRms) maxRms = rms
            }

            if (maxRms < 100.0) {
                return null
            }

            // Silence threshold: RMS near zero
            val silenceThreshold = maxOf(150.0, minOf(700.0, maxRms * 0.035))
            val isSilent = BooleanArray(numFrames) { rmsList[it] < silenceThreshold }

            val firstSpeechFrame = isSilent.indexOfFirst { !it }
            val lastSpeechFrame = isSilent.indexOfLast { !it }
            if (firstSpeechFrame == -1 || lastSpeechFrame <= firstSpeechFrame) {
                return null
            }

            if (n == 1) {
                val marginSamples = (sampleRate * 0.05).toInt()
                val speechStart = maxOf(0, firstSpeechFrame * frameSize - marginSamples)
                val speechEnd = minOf(samples.size, (lastSpeechFrame + 1) * frameSize + marginSamples)
                val len = speechEnd - speechStart
                if (len < sampleRate * 0.25) return null
                val seg = ShortArray(len)
                System.arraycopy(samples, speechStart, seg, 0, len)
                return listOf(ensureWavBytes(pcmShortsToBytes(seg), sampleRate))
            }

            // Minimum gap: ~0.6s (>= 30 frames of 20ms)
            val minSilenceFrames = maxOf(2, (0.6 * 1000 / frameDurationMs).toInt())

            val silenceSpans = mutableListOf<SilenceSpan>()
            var inSilence = false
            var silenceStart = 0

            for (f in 0 until numFrames) {
                if (isSilent[f]) {
                    if (!inSilence) {
                        inSilence = true
                        silenceStart = f
                    }
                } else {
                    if (inSilence) {
                        inSilence = false
                        val length = f - silenceStart
                        if (length >= minSilenceFrames) {
                            val startSample = silenceStart * frameSize
                            val endSample = f * frameSize
                            silenceSpans.add(
                                SilenceSpan(
                                    startSample = startSample,
                                    endSample = endSample,
                                    midSample = (startSample + endSample) / 2
                                )
                            )
                        }
                    }
                }
            }
            if (inSilence) {
                val length = numFrames - silenceStart
                if (length >= minSilenceFrames) {
                    val startSample = silenceStart * frameSize
                    val endSample = numFrames * frameSize
                    silenceSpans.add(
                        SilenceSpan(
                            startSample = startSample,
                            endSample = endSample,
                            midSample = (startSample + endSample) / 2
                        )
                    )
                }
            }

            // Filter usable silence spans strictly between first speech and last speech
            val usableSpans = silenceSpans.filter { span ->
                span.startSample > (firstSpeechFrame * frameSize) &&
                span.endSample < ((lastSpeechFrame + 1) * frameSize)
            }

            val requiredCuts = n - 1
            if (usableSpans.size < requiredCuts) {
                return null
            }

            // Expected boundary positions from cumulative character proportions of the texts
            val textLengths = texts.map { maxOf(1, it.length) }
            val totalChars = textLengths.sum().toDouble()
            var cum = 0
            val expectedSamples = LongArray(requiredCuts)
            val activeStartSample = (firstSpeechFrame * frameSize).toLong()
            val activeEndSample = ((lastSpeechFrame + 1) * frameSize).toLong()
            val activeDuration = activeEndSample - activeStartSample

            for (k in 0 until requiredCuts) {
                cum += textLengths[k]
                val prop = cum / totalChars
                expectedSamples[k] = activeStartSample + (prop * activeDuration).toLong()
            }

            // Monotonic DP selection of exactly N-1 cuts nearest expected boundaries
            val m = usableSpans.size
            val dp = Array(requiredCuts) { LongArray(m) { Long.MAX_VALUE } }
            val parent = Array(requiredCuts) { IntArray(m) { -1 } }

            for (j in 0 until m) {
                dp[0][j] = Math.abs(usableSpans[j].midSample - expectedSamples[0])
            }

            for (k in 1 until requiredCuts) {
                var minPrev = Long.MAX_VALUE
                var bestPrevIdx = -1
                for (j in k until m) {
                    val prevVal = dp[k - 1][j - 1]
                    if (prevVal < minPrev) {
                        minPrev = prevVal
                        bestPrevIdx = j - 1
                    }
                    if (minPrev != Long.MAX_VALUE) {
                        dp[k][j] = minPrev + Math.abs(usableSpans[j].midSample - expectedSamples[k])
                        parent[k][j] = bestPrevIdx
                    }
                }
            }

            var bestLast = -1
            var bestCost = Long.MAX_VALUE
            for (j in (requiredCuts - 1) until m) {
                if (dp[requiredCuts - 1][j] < bestCost) {
                    bestCost = dp[requiredCuts - 1][j]
                    bestLast = j
                }
            }
            if (bestLast == -1) return null

            val chosenIndices = IntArray(requiredCuts)
            var curr = bestLast
            for (k in (requiredCuts - 1) downTo 0) {
                chosenIndices[k] = curr
                curr = parent[k][curr]
            }

            val cutSamples = IntArray(n + 1)
            cutSamples[0] = 0
            for (k in 0 until requiredCuts) {
                cutSamples[k + 1] = usableSpans[chosenIndices[k]].midSample
            }
            cutSamples[n] = samples.size

            val segments = mutableListOf<ByteArray>()
            val marginSamples = (sampleRate * 0.05).toInt() // 50ms margin

            for (i in 0 until n) {
                val segStart = cutSamples[i].coerceIn(0, samples.size)
                val segEnd = cutSamples[i + 1].coerceIn(segStart, samples.size)

                val startF = segStart / frameSize
                val endF = minOf(numFrames - 1, (segEnd + frameSize - 1) / frameSize)

                var firstActive = -1
                var lastActive = -1
                for (f in startF..endF) {
                    if (!isSilent[f]) {
                        if (firstActive == -1) firstActive = f
                        lastActive = f
                    }
                }

                if (firstActive == -1 || lastActive < firstActive) {
                    return null // Segment is silent
                }

                val trimStart = maxOf(segStart, firstActive * frameSize - marginSamples)
                val trimEnd = minOf(segEnd, (lastActive + 1) * frameSize + marginSamples)
                val len = trimEnd - trimStart

                // Sanity check: each segment >= ~0.3s (250ms threshold)
                if (len < sampleRate * 0.25) {
                    return null
                }

                val segSamples = ShortArray(len)
                System.arraycopy(samples, trimStart, segSamples, 0, len)
                segments.add(ensureWavBytes(pcmShortsToBytes(segSamples), sampleRate))
            }

            return if (segments.size == n) segments else null
        }

        /**
         * Splits a single WAV audio containing multiple phrases separated by pauses into
         * individual WAV segments using native PCM RMS silence detection.
         * Silence gap threshold: at least 0.8 seconds (>= 40 frames of 20ms).
         * Leading/trailing silence is trimmed per segment (leaving 50ms margin).
         */
        fun splitAudioBySilence(
            wavBytes: ByteArray,
            minSilenceDurationSec: Double = 0.8
        ): List<ByteArray> {
            val (samples, sampleRate) = extractPcmData(wavBytes)
            if (samples.isEmpty()) return emptyList()

            val frameDurationMs = 20
            val frameSize = (sampleRate * frameDurationMs) / 1000 // 480 samples at 24kHz
            if (frameSize <= 0 || samples.size < frameSize) {
                return listOf(ensureWavBytes(pcmShortsToBytes(samples), sampleRate))
            }

            val numFrames = samples.size / frameSize
            val rmsList = DoubleArray(numFrames)
            var maxRms = 0.0

            for (f in 0 until numFrames) {
                val start = f * frameSize
                var sumSq = 0.0
                for (i in 0 until frameSize) {
                    val s = samples[start + i].toDouble()
                    sumSq += s * s
                }
                val rms = Math.sqrt(sumSq / frameSize)
                rmsList[f] = rms
                if (rms > maxRms) maxRms = rms
            }

            // Adaptive silence threshold: between 200.0 and 800.0, or 4% of max RMS
            val silenceThreshold = maxOf(200.0, minOf(800.0, maxRms * 0.04))
            val isSilent = BooleanArray(numFrames) { rmsList[it] < silenceThreshold }

            // Silence duration threshold in frames (0.8s * 1000 / 20 = 40 frames)
            val minSilenceFrames = maxOf(2, (minSilenceDurationSec * 1000 / frameDurationMs).toInt())

            data class FrameRange(val start: Int, val end: Int)
            val silenceGaps = mutableListOf<FrameRange>()
            var inSilence = false
            var silenceStart = 0

            for (f in 0 until numFrames) {
                if (isSilent[f]) {
                    if (!inSilence) {
                        inSilence = true
                        silenceStart = f
                    }
                } else {
                    if (inSilence) {
                        inSilence = false
                        val length = f - silenceStart
                        if (length >= minSilenceFrames) {
                            silenceGaps.add(FrameRange(silenceStart, f - 1))
                        }
                    }
                }
            }
            if (inSilence) {
                val length = numFrames - silenceStart
                if (length >= minSilenceFrames) {
                    silenceGaps.add(FrameRange(silenceStart, numFrames - 1))
                }
            }

            val segments = mutableListOf<ByteArray>()
            var currentSpeechStartFrame = 0

            for (gap in silenceGaps) {
                val speechEndFrame = gap.start - 1
                if (speechEndFrame >= currentSpeechStartFrame) {
                    val segWav = extractAndTrimSegment(
                        samples,
                        sampleRate,
                        frameSize,
                        isSilent,
                        currentSpeechStartFrame,
                        speechEndFrame
                    )
                    if (segWav != null) {
                        segments.add(segWav)
                    }
                }
                currentSpeechStartFrame = gap.end + 1
            }

            if (currentSpeechStartFrame < numFrames) {
                val segWav = extractAndTrimSegment(
                    samples,
                    sampleRate,
                    frameSize,
                    isSilent,
                    currentSpeechStartFrame,
                    numFrames - 1
                )
                if (segWav != null) {
                    segments.add(segWav)
                }
            }

            return segments
        }

        private fun extractAndTrimSegment(
            samples: ShortArray,
            sampleRate: Int,
            frameSize: Int,
            isSilent: BooleanArray,
            startFrame: Int,
            endFrame: Int
        ): ByteArray? {
            var actualStart = startFrame
            while (actualStart <= endFrame && isSilent[actualStart]) {
                actualStart++
            }
            var actualEnd = endFrame
            while (actualEnd >= actualStart && isSilent[actualEnd]) {
                actualEnd--
            }

            if (actualStart > actualEnd) return null

            // Margin of 50ms (2-3 frames) so we don't clip words
            val marginFrames = 2
            val paddedStartFrame = maxOf(0, actualStart - marginFrames)
            val paddedEndFrame = minOf(isSilent.size - 1, actualEnd + marginFrames)

            val sampleStart = (paddedStartFrame * frameSize).coerceIn(0, samples.size)
            val sampleEnd = minOf(samples.size, (paddedEndFrame + 1) * frameSize)

            val lengthSamples = sampleEnd - sampleStart
            if (lengthSamples < sampleRate * 0.10) {
                return null
            }

            val segmentSamples = ShortArray(lengthSamples)
            System.arraycopy(samples, sampleStart, segmentSamples, 0, lengthSamples)
            val pcmBytes = pcmShortsToBytes(segmentSamples)
            return ensureWavBytes(pcmBytes, sampleRate)
        }

        fun groupTexts(
            texts: List<String>,
            maxItems: Int = BATCH_MAX_ITEMS_PER_GROUP,
            maxChars: Int = BATCH_MAX_CHARS_PER_GROUP
        ): List<List<String>> {
            val groups = mutableListOf<List<String>>()
            var currentGroup = mutableListOf<String>()
            var currentChars = 0

            for (text in texts) {
                val addedChars = if (currentGroup.isEmpty()) text.length else text.length + 1
                if (currentGroup.size >= maxItems || (currentChars + addedChars > maxChars && currentGroup.isNotEmpty())) {
                    groups.add(currentGroup)
                    currentGroup = mutableListOf(text)
                    currentChars = text.length
                } else {
                    currentGroup.add(text)
                    currentChars += addedChars
                }
            }
            if (currentGroup.isNotEmpty()) {
                groups.add(currentGroup)
            }
            return groups
        }

        fun cleanGermanText(text: String): String {
            val clean = text.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
            return if (clean.isNotEmpty()) clean else text.trim()
        }

        fun collectGermanTextsFromLesson(lesson: LessonData): List<String> {
            val result = mutableListOf<String>()
            // 1) Vocabulary words
            for (vocab in lesson.vocabulary) {
                val wordWithArticle = if (vocab.article.isNotBlank()) "${vocab.article} ${vocab.word}".trim() else vocab.word.trim()
                val cleanWord = cleanGermanText(wordWithArticle)
                if (cleanWord.isNotBlank()) result.add(cleanWord)
            }
            // 2) Example sentences
            for (ex in lesson.exampleSentences) {
                val cleanEx = cleanGermanText(ex.german)
                if (cleanEx.isNotBlank()) result.add(cleanEx)
            }
            // 3) Dialogue lines
            for (d in lesson.dialogues) {
                for (line in d.lines) {
                    val cleanLine = cleanGermanText(line.german)
                    if (cleanLine.isNotBlank()) result.add(cleanLine)
                }
            }
            // 4) Q&A pairs (question + answer)
            for (qa in lesson.qaPairs) {
                val cleanQ = cleanGermanText(qa.questionGerman)
                if (cleanQ.isNotBlank()) result.add(cleanQ)
                val cleanA = cleanGermanText(qa.answerGerman)
                if (cleanA.isNotBlank()) result.add(cleanA)
            }
            return result.distinct()
        }

        fun collectGermanTextsFromGrammarTopic(topic: GrammarTopic): List<String> {
            val result = mutableListOf<String>()
            // Example sentences
            for (ex in topic.exampleSentences) {
                val cleanEx = cleanGermanText(ex.german)
                if (cleanEx.isNotBlank()) result.add(cleanEx)
            }
            return result.distinct()
        }

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

        fun toPersianDigits(input: String): String {
            return input
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹')
        }

        fun exportVoiceLibraryToZip(context: Context): File? {
            val libraryDir = getVoiceLibraryDir(context)
            val files = libraryDir.listFiles { file ->
                file.isFile && file.extension.equals("wav", ignoreCase = true) && !file.name.equals(".nomedia")
            } ?: emptyArray()

            if (files.isEmpty()) {
                return null
            }

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "voice-library.zip")
            if (zipFile.exists()) {
                zipFile.delete()
            }

            return try {
                java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(zipFile))).use { zipOut ->
                    val buffer = ByteArray(8192)
                    for (file in files) {
                        val entry = java.util.zip.ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        file.inputStream().use { input ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                zipOut.write(buffer, 0, bytesRead)
                            }
                        }
                        zipOut.closeEntry()
                    }
                }
                if (zipFile.exists() && zipFile.length() > 0) zipFile else null
            } catch (e: Exception) {
                Log.e("TtsManager", "Failed to export voice library to zip: ${e.message}", e)
                null
            }
        }

        fun importVoiceLibraryFromZip(context: Context, inputStream: java.io.InputStream): VoiceLibraryImportResult {
            val libraryDir = getVoiceLibraryDir(context)
            val tempDir = File(context.cacheDir, "temp_voice_import_${System.currentTimeMillis()}").apply { mkdirs() }

            try {
                var hasValidWavFiles = false
                java.util.zip.ZipInputStream(java.io.BufferedInputStream(inputStream)).use { zipIn ->
                    val buffer = ByteArray(8192)
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (entry.isDirectory ||
                            name.contains("..") ||
                            name.startsWith("/") ||
                            name.startsWith("\\") ||
                            name.contains(":") ||
                            name.startsWith("__MACOSX") ||
                            name.endsWith(".DS_Store")
                        ) {
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                            continue
                        }

                        val safeFileName = File(name).name
                        if (!safeFileName.endsWith(".wav", ignoreCase = true)) {
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                            continue
                        }

                        val destFile = File(tempDir, safeFileName)
                        if (!destFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                            continue
                        }

                        destFile.outputStream().use { fileOut ->
                            var bytesRead: Int
                            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                fileOut.write(buffer, 0, bytesRead)
                            }
                        }
                        zipIn.closeEntry()
                        if (destFile.length() > 0) {
                            hasValidWavFiles = true
                        } else {
                            destFile.delete()
                        }
                        entry = zipIn.nextEntry
                    }
                }

                if (!hasValidWavFiles) {
                    tempDir.deleteRecursively()
                    return VoiceLibraryImportResult(
                        isSuccess = false,
                        errorMessage = "هیچ فایل صوتی معتبری (.wav) در این فایل زیپ یافت نشد."
                    )
                }

                val extractedFiles = tempDir.listFiles { f -> f.isFile && f.extension.equals("wav", ignoreCase = true) } ?: emptyArray()
                var addedCount = 0
                var skippedCount = 0

                for (file in extractedFiles) {
                    val targetFile = File(libraryDir, file.name)
                    if (targetFile.exists() && targetFile.length() > 0) {
                        skippedCount++
                    } else {
                        file.copyTo(targetFile, overwrite = false)
                        addedCount++
                    }
                }

                tempDir.deleteRecursively()
                return VoiceLibraryImportResult(
                    isSuccess = true,
                    addedCount = addedCount,
                    skippedCount = skippedCount
                )
            } catch (e: Exception) {
                Log.e("TtsManager", "Error importing voice library: ${e.message}", e)
                tempDir.deleteRecursively()
                return VoiceLibraryImportResult(
                    isSuccess = false,
                    errorMessage = "فایل زیپ نامعتبر یا آسیب‌دیده است. حافظهٔ صدا بدون تغییر باقی ماند."
                )
            }
        }

        fun shareVoiceLibraryZip(context: Context, zipFile: File) {
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "خروجی حافظهٔ صدای جیمنای - voice-library.zip")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = android.content.Intent.createChooser(shareIntent, "ارسال یا ذخیرهٔ فایل حافظهٔ صدا").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.e("TtsManager", "Error sharing voice library zip: ${e.message}", e)
                Toast.makeText(context, "خطا در اشتراک‌گذاری فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
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

package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.data.model.GrammarTopic
import com.example.data.model.LessonData
import com.example.data.storage.UserProgressManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume

/**
 * Phase 2 — amended Fix P: Dual-Provider Bilingual Voice System:
 * 1) BILINGUAL PLAYBACK: German text first, then Dari translation, chained under one control.
 * 2) DUAL PROVIDERS: Microsoft Azure Speech REST API + Google Gemini TTS with automatic fallback
 *    (chosen provider tried first -> other provider on failure -> device TTS).
 * 3) GENDER-MATCHED VOICES:
 *    - Male: Azure de-DE-ConradNeural + fa-IR-FaridNeural; Gemini Charon
 *    - Female: Azure de-DE-KlaraNeural + fa-IR-DilaraNeural; Gemini Kore
 * 4) PER-SEGMENT SCRIPT RESOLUTION: Persian segments use Azure fa-IR voices matched to selected German voice gender.
 * 5) PERMANENT VOICE LIBRARY: filesDir/voice_library/ checked FIRST before any cloud call,
 *    keyed per text+voice+provider; existing cached files stay valid.
 * 6) TRANSFER: ZIP export/import supporting .wav & .mp3 with zip-slip defense.
 * 7) PERSISTED PER-PROVIDER COUNTERS: Tracks real daily Azure and Gemini requests.
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

    val activeLoadingSentenceFlow: StateFlow<String?> = Companion.activeLoadingSentenceFlow

    private var sequentialJob: Job? = null
    private val _isSequentialPlayingFlow = MutableStateFlow(false)
    val isSequentialPlayingFlow: StateFlow<Boolean> = _isSequentialPlayingFlow.asStateFlow()

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

    fun getEffectiveAzureKey(): String {
        return UserProgressManager.getInstance(applicationContext).getAzureSpeechKey().trim()
    }

    fun getEffectiveAzureRegion(): String {
        return UserProgressManager.getInstance(applicationContext).getAzureSpeechRegion().trim()
    }

    fun getChosenVoiceProvider(): String {
        return UserProgressManager.getInstance(applicationContext).getVoiceProvider()
    }

    fun getSelectedAzureVoice(): String {
        return UserProgressManager.getInstance(applicationContext).getAzureSpeechVoice()
    }

    fun isSelectedVoiceMale(): Boolean {
        return !getSelectedAzureVoice().contains("Klara", ignoreCase = true)
    }

    fun getGeminiVoiceName(): String {
        return if (isSelectedVoiceMale()) GEMINI_VOICE_CHARON else GEMINI_VOICE_KORE
    }

    fun getAzureVoiceForSegment(isPersian: Boolean): String {
        return if (isPersian) {
            if (isSelectedVoiceMale()) AZURE_VOICE_FA_FARID else AZURE_VOICE_FA_DILARA
        } else {
            getSelectedAzureVoice()
        }
    }

    fun getLangCodeForSegment(isPersian: Boolean): String {
        return if (isPersian) "fa-IR" else "de-DE"
    }

    fun getEffectiveSpeed(): Float {
        return speedProvider?.invoke()
            ?: UserProgressManager.getInstance(applicationContext).getPlaybackSpeed()
    }

    fun isGeminiVoiceConfigured(): Boolean {
        return getEffectiveApiKey().isNotEmpty()
    }

    fun isAzureVoiceConfigured(): Boolean {
        return getEffectiveAzureKey().isNotEmpty()
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

    fun getDailyAzureRequestCount(): Int {
        return UserProgressManager.getInstance(applicationContext).getDailyAzureRequestsCount()
    }

    /**
     * Checks the permanent voice library and legacy cache for this sentence.
     * 1) Provider + voice + text key (.wav and .mp3)
     * 2) Voice + text key (.wav and .mp3)
     * 3) Default voice canonical key in filesDir/voice_library/
     * 4) Legacy speed-keyed file in filesDir/voice_library/
     * 5) Legacy cache in cacheDir/gemini_tts_cache/
     */
    fun findStoredAudioFile(
        text: String,
        speed: Float = 1.0f,
        voiceName: String? = null,
        provider: String? = null
    ): File? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        // 1) Provider + voice specific key
        if (provider != null && voiceName != null) {
            val key = getVoiceLibraryKey(clean, voiceName, provider)
            val fMp3 = File(voiceLibraryDir, "$key.mp3")
            if (fMp3.exists() && fMp3.length() > 0) return fMp3
            val fWav = File(voiceLibraryDir, "$key.wav")
            if (fWav.exists() && fWav.length() > 0) return fWav
        }

        // 2) Voice-specific key (both .mp3 and .wav)
        val vName = voiceName ?: DEFAULT_VOICE_NAME
        val vKey = getVoiceLibraryKey(clean, vName)
        val vMp3 = File(voiceLibraryDir, "$vKey.mp3")
        if (vMp3.exists() && vMp3.length() > 0) return vMp3
        val vWav = File(voiceLibraryDir, "$vKey.wav")
        if (vWav.exists() && vWav.length() > 0) return vWav

        // 3) Default voice canonical key in library
        val canonicalKey = getVoiceLibraryKey(clean, DEFAULT_VOICE_NAME)
        val libraryFileWav = File(voiceLibraryDir, "$canonicalKey.wav")
        if (libraryFileWav.exists() && libraryFileWav.length() > 0) return libraryFileWav
        val libraryFileMp3 = File(voiceLibraryDir, "$canonicalKey.mp3")
        if (libraryFileMp3.exists() && libraryFileMp3.length() > 0) return libraryFileMp3

        // 4) Legacy speed-keyed file in library
        val legacySpeedKey = getLegacyCacheKey(clean, speed)
        val legacyInLibrary = File(voiceLibraryDir, "$legacySpeedKey.wav")
        if (legacyInLibrary.exists() && legacyInLibrary.length() > 0) return legacyInLibrary

        // 5) Legacy checks in cacheDir/gemini_tts_cache
        val legacyInCache = File(legacyCacheDir, "$legacySpeedKey.wav")
        if (legacyInCache.exists() && legacyInCache.length() > 0) return legacyInCache

        val legacyNeutralInCache = File(legacyCacheDir, "$canonicalKey.wav")
        if (legacyNeutralInCache.exists() && legacyNeutralInCache.length() > 0) return legacyNeutralInCache

        return null
    }

    fun getVoiceLibraryFile(text: String): File {
        val key = getVoiceLibraryKey(text)
        return File(voiceLibraryDir, "$key.wav")
    }

    /**
     * Synthesizes audio using Azure Speech REST API.
     * Saves audio to filesDir/voice_library/ as MP3.
     */
    suspend fun synthesizeAzureAudioToLibrary(
        text: String,
        isPersian: Boolean,
        speed: Float,
        azureKey: String,
        region: String = getEffectiveAzureRegion(),
        voiceOverride: String? = null
    ): File? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isBlank()) return@withContext null

        val voiceName = voiceOverride ?: getAzureVoiceForSegment(isPersian)
        val langCode = getLangCodeForSegment(isPersian)

        // Rate calculation: (1.0x=0%, 0.5x=−50%, 1.5x=+50%)
        val ratePercent = ((speed - 1.0f) * 100).toInt()
        val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
        val escapedText = escapeXml(clean)

        val ssml = """
            <speak version='1.0' xml:lang='$langCode'>
              <voice xml:lang='$langCode' name='$voiceName'>
                <prosody rate='$rateStr'>
                  $escapedText
                </prosody>
              </voice>
            </speak>
        """.trimIndent()

        val url = "https://${region.trim()}.tts.speech.microsoft.com/cognitiveservices/v1"
        val requestBody = ssml.toRequestBody("application/ssml+xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Ocp-Apim-Subscription-Key", azureKey.trim())
            .addHeader("Content-Type", "application/ssml+xml")
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
            .addHeader("User-Agent", "GermanLearningApp")
            .post(requestBody)
            .build()

        try {
            UserProgressManager.getInstance(applicationContext).incrementDailyAzureRequestsCount()
            val response = httpClient.newCall(request).execute()
            val code = response.code
            val bodyBytes = response.body?.bytes()

            if (response.isSuccessful && bodyBytes != null && bodyBytes.isNotEmpty()) {
                val key = getVoiceLibraryKey(clean, voiceName, PROVIDER_AZURE)
                val libraryFile = File(voiceLibraryDir, "$key.mp3")
                libraryFile.outputStream().use { it.write(bodyBytes) }
                Log.d("TtsManager", "Saved Azure audio to voice library: ${libraryFile.name} (${libraryFile.length()} bytes)")
                return@withContext libraryFile
            } else {
                Log.w("TtsManager", "Azure TTS synthesis failed with code $code: ${response.message}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("TtsManager", "Azure TTS synthesis error: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Synthesizes audio using Google Gemini TTS API.
     * Fallback: gemini-2.5-flash-tts -> gemini-2.5-flash-preview-tts.
     * Saves audio to filesDir/voice_library/ as WAV.
     */
    suspend fun synthesizeGeminiAudioToLibrary(
        text: String,
        isPersian: Boolean,
        apiKey: String,
        voiceOverride: String? = null
    ): File? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isBlank()) return@withContext null

        val geminiVoice = voiceOverride ?: getGeminiVoiceName()
        val promptText = if (isPersian) {
            "Pronounce this Persian / Dari text clearly and naturally: \"$clean\""
        } else {
            buildNeutralPrompt(clean)
        }

        val requestJson = buildTtsRequestBody(promptText, geminiVoice)
        val requestBodyStr = requestJson.toString()

        for (model in TTS_MODELS) {
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
                            val key = getVoiceLibraryKey(clean, geminiVoice, PROVIDER_GEMINI)
                            val libraryFile = File(voiceLibraryDir, "$key.wav")
                            libraryFile.outputStream().use { it.write(wavBytes) }
                            Log.d("TtsManager", "Saved Gemini audio to voice library: ${libraryFile.name}")
                            return@withContext libraryFile
                        }
                    }
                } else {
                    Log.w("TtsManager", "Gemini TTS $model returned HTTP $responseCode")
                }
            } catch (e: Exception) {
                Log.w("TtsManager", "Error attempting Gemini TTS with $model: ${e.message}")
            }
        }
        return@withContext null
    }

    /**
     * Synthesizes a single segment with dual-provider fallback logic.
     * 1) Voice library check FIRST.
     * 2) Selected provider tried first.
     * 3) Other provider tried automatically on failure.
     * 4) If Persian and Gemini cannot synthesize fa-IR, falls back to Azure.
     */
    suspend fun synthesizeSegment(
        text: String,
        isPersian: Boolean,
        speed: Float
    ): File? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val chosenProvider = getChosenVoiceProvider()
        val azureVoice = getAzureVoiceForSegment(isPersian)
        val geminiVoice = getGeminiVoiceName()

        // 1) First check stored audio in voice library
        val storedFile = findStoredAudioFile(clean, speed, if (chosenProvider == PROVIDER_AZURE) azureVoice else geminiVoice, chosenProvider)
            ?: findStoredAudioFile(clean, speed, azureVoice, PROVIDER_AZURE)
            ?: findStoredAudioFile(clean, speed, geminiVoice, PROVIDER_GEMINI)
        if (storedFile != null) {
            return storedFile
        }

        val azureKey = getEffectiveAzureKey()
        val geminiKey = getEffectiveApiKey()

        // 2) Dual-provider execution order
        if (chosenProvider == PROVIDER_AZURE) {
            // Try Azure first
            if (azureKey.isNotEmpty()) {
                val file = synthesizeAzureAudioToLibrary(clean, isPersian, speed, azureKey)
                if (file != null) return file
            }
            // Fallback to Gemini
            if (geminiKey.isNotEmpty()) {
                val file = synthesizeGeminiAudioToLibrary(clean, isPersian, geminiKey)
                if (file != null) return file
            }
        } else {
            // Try Gemini first
            if (geminiKey.isNotEmpty()) {
                val file = synthesizeGeminiAudioToLibrary(clean, isPersian, geminiKey)
                if (file != null) return file
            }
            // Fallback to Azure (especially critical for fa-IR)
            if (azureKey.isNotEmpty()) {
                val file = synthesizeAzureAudioToLibrary(clean, isPersian, speed, azureKey)
                if (file != null) return file
            }
        }

        return null
    }

    /**
     * Synthesizes a segment with a specific fixed voice (e.g. speaker A male Conrad/Charon,
     * speaker B female Klara/Kore) checking cache first, trying chosen provider then other,
     * saving to permanent voice library. Returns the audio File if synthesized or cached.
     */
    suspend fun synthesizeSegmentWithVoice(
        text: String,
        isPersian: Boolean,
        speed: Float,
        isMaleVoice: Boolean
    ): File? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val chosenProvider = getChosenVoiceProvider()
        val azureVoice = if (isPersian) {
            if (isMaleVoice) AZURE_VOICE_FA_FARID else AZURE_VOICE_FA_DILARA
        } else {
            if (isMaleVoice) AZURE_VOICE_CONRAD else AZURE_VOICE_KLARA
        }
        val geminiVoice = if (isMaleVoice) GEMINI_VOICE_CHARON else GEMINI_VOICE_KORE

        // 1) First check stored audio in voice library
        val storedFile = findStoredAudioFile(clean, speed, if (chosenProvider == PROVIDER_AZURE) azureVoice else geminiVoice, chosenProvider)
            ?: findStoredAudioFile(clean, speed, azureVoice, PROVIDER_AZURE)
            ?: findStoredAudioFile(clean, speed, geminiVoice, PROVIDER_GEMINI)
        if (storedFile != null) {
            return storedFile
        }

        val azureKey = getEffectiveAzureKey()
        val geminiKey = getEffectiveApiKey()

        // 2) Dual-provider execution order
        if (chosenProvider == PROVIDER_AZURE) {
            if (azureKey.isNotEmpty()) {
                val file = synthesizeAzureAudioToLibrary(clean, isPersian, speed, azureKey, voiceOverride = azureVoice)
                if (file != null) return file
            }
            if (geminiKey.isNotEmpty()) {
                val file = synthesizeGeminiAudioToLibrary(clean, isPersian, geminiKey, voiceOverride = geminiVoice)
                if (file != null) return file
            }
        } else {
            if (geminiKey.isNotEmpty()) {
                val file = synthesizeGeminiAudioToLibrary(clean, isPersian, geminiKey, voiceOverride = geminiVoice)
                if (file != null) return file
            }
            if (azureKey.isNotEmpty()) {
                val file = synthesizeAzureAudioToLibrary(clean, isPersian, speed, azureKey, voiceOverride = azureVoice)
                if (file != null) return file
            }
        }

        return null
    }

    /**
     * Plays a dialogue line using fixed voice (Speaker A Conrad/male, Speaker B Klara/female)
     * with German first then Dari translation, falling back to device TTS with Dari note if unconfigured.
     */
    suspend fun playDialogueLineAndWait(
        germanText: String,
        dariText: String?,
        isSpeakerA: Boolean,
        speed: Float = getEffectiveSpeed(),
        existingAudioFile: File? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val (german, dari) = parseGermanAndDari(germanText, dariText)
        if (german.isBlank() && (dari == null || dari.isBlank())) return@withContext false

        _activeLoadingSentenceFlow.value = german
        try {
            var playedOk = false

            // 1. If explicit offline file exists, play it directly!
            if (existingAudioFile != null && existingAudioFile.exists() && existingAudioFile.length() > 0) {
                playedOk = playAudioFileAndWait(existingAudioFile, speed)
            } else {
                // Synthesize or retrieve from cache
                val germanFile = synthesizeSegmentWithVoice(german, isPersian = false, speed = speed, isMaleVoice = isSpeakerA)
                if (germanFile != null) {
                    playedOk = playAudioFileAndWait(germanFile, speed)
                } else {
                    // Fallback to device TTS with Dari toast if no cloud voice is configured
                    val hasAnyCloudKey = getEffectiveAzureKey().isNotEmpty() || getEffectiveApiKey().isNotEmpty()
                    if (!hasAnyCloudKey) {
                        showToast("کلید صوتی تنظیم نشده است؛ صدا از موتور گفتار گوشی پخش می‌شود.")
                    }
                    playedOk = speakWithDeviceTtsAndWait(german, isPersian = false, speed = speed)
                }
            }

            // Play Dari translation
            if (!dari.isNullOrBlank() && currentCoroutineContext().isActive) {
                delay(300)
                val dariFile = synthesizeSegmentWithVoice(dari, isPersian = true, speed = speed, isMaleVoice = isSpeakerA)
                if (dariFile != null) {
                    playAudioFileAndWait(dariFile, speed)
                } else {
                    speakWithDeviceTtsAndWait(dari, isPersian = true, speed = speed)
                }
            }

            playedOk
        } finally {
            _activeLoadingSentenceFlow.value = null
        }
    }

    /**
     * Plays a single segment and waits for playback to finish.
     * Cloud synthesis -> Device TTS fallback.
     */
    suspend fun playSegmentAndWait(
        text: String,
        isPersian: Boolean,
        speed: Float
    ): Boolean {
        val clean = text.trim()
        if (clean.isBlank()) return false

        val file = synthesizeSegment(clean, isPersian, speed)
        if (file != null) {
            return playAudioFileAndWait(file, speed)
        }

        // Fallback to device TTS
        return speakWithDeviceTtsAndWait(clean, isPersian, speed)
    }

    /**
     * Parses input text into German segment and optional Dari translation.
     */
    fun parseGermanAndDari(text: String, explicitDari: String?): Pair<String, String?> {
        if (!explicitDari.isNullOrBlank()) {
            return Pair(cleanGermanText(text), explicitDari.trim())
        }

        // Check if text has ⟦Dari translation⟧
        val bracketMatch = Regex("""⟦(.*?)⟧""").find(text)
        if (bracketMatch != null) {
            val dari = bracketMatch.groupValues[1].trim()
            val german = cleanGermanText(text.replace(Regex("""⟦(.*?)⟧"""), "").trim())
            if (german.isNotEmpty() && dari.isNotEmpty()) {
                return Pair(german, dari)
            }
        }

        // Check if text has (Dari translation)
        val parenMatch = Regex("""\(([\u0600-\u06FF\s.,!?]+)\)""").find(text)
        if (parenMatch != null) {
            val dari = parenMatch.groupValues[1].trim()
            val german = cleanGermanText(text.substring(0, parenMatch.range.first).trim())
            if (german.isNotEmpty() && dari.isNotEmpty()) {
                return Pair(german, dari)
            }
        }

        // Check if text has "German | Dari"
        if (text.contains("|")) {
            val parts = text.split("|").map { it.trim() }
            val germanPart = parts.firstOrNull { it.any { c -> c in 'a'..'z' || c in 'A'..'Z' } }
            val dariPart = parts.firstOrNull { it.any { c -> c in '\u0600'..'\u06FF' } }
            if (germanPart != null && dariPart != null) {
                return Pair(cleanGermanText(germanPart), dariPart)
            }
        }

        return Pair(cleanGermanText(text), null)
    }

    /**
     * BILINGUAL playback: German text first, then Dari translation, chained under one control.
     */
    fun speak(
        germanText: String,
        dariText: String?,
        speed: Float
    ) {
        if (germanText.isBlank() && dariText.isNullOrBlank()) return

        val (german, dari) = parseGermanAndDari(germanText, dariText)
        if (german.isBlank() && (dari == null || dari.isBlank())) return

        coroutineScope.launch {
            _activeLoadingSentenceFlow.value = german
            try {
                if (german.isNotBlank()) {
                    playSegmentAndWait(german, isPersian = false, speed = speed)
                }
                if (!dari.isNullOrBlank() && isActive) {
                    delay(250)
                    playSegmentAndWait(dari, isPersian = true, speed = speed)
                }
            } finally {
                _activeLoadingSentenceFlow.value = null
            }
        }
    }

    fun speak(
        germanText: String,
        dariText: String?,
        isSlow: Boolean
    ) {
        val speed = if (isSlow && getEffectiveSpeed() > 0.75f) 0.5f else getEffectiveSpeed()
        speak(germanText, dariText, speed)
    }

    fun speak(text: String, speed: Float) {
        speak(text, null, speed)
    }

    fun speak(text: String, isSlow: Boolean = false) {
        speak(text, null, isSlow)
    }

    fun speak(text: String) {
        speak(text, null, getEffectiveSpeed())
    }

    suspend fun speakAndWait(
        germanText: String,
        dariText: String? = null,
        speed: Float = getEffectiveSpeed()
    ): Boolean = withContext(Dispatchers.IO) {
        val (german, dari) = parseGermanAndDari(germanText, dariText)
        if (german.isBlank() && (dari == null || dari.isBlank())) return@withContext false

        _activeLoadingSentenceFlow.value = german
        try {
            var ok = true
            if (german.isNotBlank()) {
                ok = playSegmentAndWait(german, isPersian = false, speed = speed)
            }
            if (!dari.isNullOrBlank() && currentCoroutineContext().isActive) {
                delay(250)
                playSegmentAndWait(dari, isPersian = true, speed = speed)
            }
            ok
        } finally {
            _activeLoadingSentenceFlow.value = null
        }
    }

    /**
     * Tests voice with current speed, supporting Azure and Gemini providers.
     */
    suspend fun testVoice(
        sampleGerman: String = "Guten Tag! Ich lerne Deutsch.",
        sampleDari: String = "روز بخیر! من آلمانی یاد می‌گیرم.",
        speed: Float = getEffectiveSpeed(),
        providerOverride: String? = null,
        keyOverride: String? = null,
        regionOverride: String? = null,
        voiceOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val provider = providerOverride ?: getChosenVoiceProvider()
        val speedDesc = UserProgressManager.formatSpeedToPersian(speed)

        if (provider == PROVIDER_AZURE) {
            val key = keyOverride?.trim()?.ifEmpty { null } ?: getEffectiveAzureKey()
            val region = regionOverride?.trim()?.ifEmpty { null } ?: getEffectiveAzureRegion()
            if (key.isBlank()) {
                return@withContext Result.failure(Exception("کلید API سرویس صوتی آژور (Azure) وارد نشده است. لطفاً کلید اشتراک Speech خود را وارد نمایید."))
            }

            try {
                // Test German segment
                val germanFile = synthesizeAzureAudioToLibrary(
                    text = sampleGerman,
                    isPersian = false,
                    speed = speed,
                    azureKey = key,
                    region = region,
                    voiceOverride = voiceOverride
                )
                if (germanFile == null) {
                    return@withContext Result.failure(Exception("خطا در ارتباط با سرویس صوتی آژور. لطفاً کلید و منطقه (Region) را بررسی نمایید."))
                }

                // Test Persian segment
                val dariFile = synthesizeAzureAudioToLibrary(
                    text = sampleDari,
                    isPersian = true,
                    speed = speed,
                    azureKey = key,
                    region = region,
                    voiceOverride = null
                )

                playAudioFileAndWait(germanFile, speed)
                if (dariFile != null) {
                    delay(250)
                    playAudioFileAndWait(dariFile, speed)
                }

                return@withContext Result.success("صدای دو زبانهٔ آژور با سرعت $speedDesc با موفقیت پخش شد! ✨")
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("خطا در برقراری ارتباط با سرویس صوتی آژور."))
            }
        } else {
            // Test Gemini
            val key = keyOverride?.trim()?.ifEmpty { null } ?: getEffectiveApiKey()
            if (key.isBlank()) {
                return@withContext Result.failure(Exception("کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید خود را از Google AI Studio وارد نمایید."))
            }

            try {
                val germanFile = synthesizeGeminiAudioToLibrary(
                    text = sampleGerman,
                    isPersian = false,
                    apiKey = key,
                    voiceOverride = voiceOverride
                )
                if (germanFile == null) {
                    return@withContext Result.failure(Exception("خطا در برقراری ارتباط با سرور جیمنای؛ لطفاً کلید API را بررسی نمایید."))
                }

                val dariFile = synthesizeGeminiAudioToLibrary(
                    text = sampleDari,
                    isPersian = true,
                    apiKey = key,
                    voiceOverride = voiceOverride
                )

                playAudioFileAndWait(germanFile, speed)
                if (dariFile != null) {
                    delay(250)
                    playAudioFileAndWait(dariFile, speed)
                }

                return@withContext Result.success("صدای جیمنای با سرعت $speedDesc با موفقیت پخش شد! ✨")
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("خطا در اتصال به اینترنت یا سرور جیمنای."))
            }
        }
    }

    suspend fun testGeminiVoice(
        sampleText: String = "Guten Tag! Ich lerne Deutsch.",
        speed: Float = getEffectiveSpeed(),
        keyOverride: String? = null
    ): Result<String> {
        return testVoice(
            sampleGerman = sampleText,
            speed = speed,
            providerOverride = PROVIDER_GEMINI,
            keyOverride = keyOverride
        )
    }

    suspend fun testGeminiVoice(
        sampleText: String = "Guten Tag! Ich lerne Deutsch.",
        isSlow: Boolean,
        keyOverride: String? = null
    ): Result<String> {
        val speed = if (isSlow) 0.5f else getEffectiveSpeed()
        return testGeminiVoice(sampleText, speed, keyOverride)
    }

    private fun buildTtsRequestBody(promptText: String, voiceName: String = DEFAULT_VOICE_NAME): JSONObject {
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
                            put("voiceName", voiceName)
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

    private fun speakWithDeviceTts(speechText: String, isPersian: Boolean = false, speed: Float) {
        stopAudio()
        if (!isInitialized) {
            showToast("موتور صوتی گوشی هنوز آماده نشده است.")
            return
        }

        tts?.stop()
        tts?.setSpeechRate(speed)
        tts?.setPitch(1.0f)

        if (isPersian) {
            try {
                tts?.setLanguage(Locale("fa"))
            } catch (e: Exception) {
                // fallback
            }
        } else {
            if (!isGermanSupported) {
                showToast("بسته صدای آلمانی روی گوشی شما نصب نیست. لطفاً در تنظیمات زبان گوشی (Text-to-Speech) صدای آلمانی را فعال کنید.")
                return
            }
            try {
                tts?.setLanguage(Locale.GERMAN)
            } catch (e: Exception) {
                // fallback
            }
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat("rate", speed)
            putFloat("speechRate", speed)
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "device_tts_${System.currentTimeMillis()}")
        }
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "device_tts_${System.currentTimeMillis()}")
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

    suspend fun playAudioFileAndWait(file: File, speed: Float = 1.0f): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
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

                player.setOnCompletionListener { mp ->
                    try { mp.release() } catch (e: Exception) {}
                    if (mediaPlayer === mp) mediaPlayer = null
                    if (cont.isActive) cont.resume(true)
                }
                player.setOnErrorListener { mp, _, _ ->
                    try { mp.release() } catch (e: Exception) {}
                    if (mediaPlayer === mp) mediaPlayer = null
                    if (cont.isActive) cont.resume(false)
                    true
                }
                cont.invokeOnCancellation {
                    try {
                        player.stop()
                        player.release()
                    } catch (e: Exception) {}
                    if (mediaPlayer === player) mediaPlayer = null
                }
                mediaPlayer = player
                player.start()
            } catch (e: Exception) {
                Log.w("TtsManager", "MediaPlayer playback failed: ${e.message}")
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    suspend fun speakWithDeviceTtsAndWait(speechText: String, isPersian: Boolean = false, speed: Float): Boolean = withContext(Dispatchers.Main) {
        val ttsInstance = tts ?: return@withContext false
        if (!isInitialized) return@withContext false
        if (!isPersian && !isGermanSupported) return@withContext false

        suspendCancellableCoroutine { cont ->
            val utteranceId = "device_seq_${System.currentTimeMillis()}_${(0..999).random()}"
            ttsInstance.setSpeechRate(speed)
            ttsInstance.setPitch(1.0f)
            if (isPersian) {
                try { ttsInstance.setLanguage(Locale("fa")) } catch (e: Exception) {}
            } else {
                try { ttsInstance.setLanguage(Locale.GERMAN) } catch (e: Exception) {}
            }

            val listener = object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && cont.isActive) cont.resume(true)
                }
                override fun onError(id: String?) {
                    if (id == utteranceId && cont.isActive) cont.resume(false)
                }
            }
            ttsInstance.setOnUtteranceProgressListener(listener)
            cont.invokeOnCancellation {
                try { ttsInstance.stop() } catch (e: Exception) {}
            }
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putFloat("rate", speed)
                putFloat("speechRate", speed)
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val result = ttsInstance.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    fun startSequentialPlayback(lines: List<String>, speed: Float = getEffectiveSpeed()) {
        stopSequentialPlayback()
        if (lines.isEmpty()) return
        _isSequentialPlayingFlow.value = true
        sequentialJob = coroutineScope.launch {
            try {
                for (line in lines) {
                    if (!isActive) break
                    speakAndWait(line, null, speed)
                    delay(300)
                }
            } catch (e: CancellationException) {
                // Cancelled
            } finally {
                _isSequentialPlayingFlow.value = false
            }
        }
    }

    fun stopSequentialPlayback() {
        sequentialJob?.cancel()
        sequentialJob = null
        _isSequentialPlayingFlow.value = false
        stopAudio()
        tts?.stop()
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

    companion object {
        private val _activeLoadingSentenceFlow = MutableStateFlow<String?>(null)
        val activeLoadingSentenceFlow: StateFlow<String?> = _activeLoadingSentenceFlow.asStateFlow()

        const val PRIMARY_TTS_MODEL = "gemini-2.5-flash-tts"
        const val SECONDARY_TTS_MODEL = "gemini-2.5-flash-preview-tts"
        val TTS_MODELS = listOf(PRIMARY_TTS_MODEL, SECONDARY_TTS_MODEL)

        const val PROVIDER_AZURE = "AZURE"
        const val PROVIDER_GEMINI = "GEMINI"

        const val AZURE_VOICE_CONRAD = "de-DE-ConradNeural"
        const val AZURE_VOICE_KLARA = "de-DE-KlaraNeural"
        const val AZURE_VOICE_FA_FARID = "fa-IR-FaridNeural"
        const val AZURE_VOICE_FA_DILARA = "fa-IR-DilaraNeural"

        const val GEMINI_VOICE_CHARON = "Charon"
        const val GEMINI_VOICE_KORE = "Kore"

        const val DEFAULT_VOICE_NAME = "Kore"
        const val DEFAULT_AZURE_VOICE = "de-DE-ConradNeural"
        const val DEFAULT_AZURE_REGION = "eastus"

        const val QUOTA_EXCEEDED_DARI_MSG = "سهمیۀ رایگان روزانۀ صدای جیمنای تمام شده است (۱۰ جمله در روز). صدا موقتاً از گوشی پخش میشود؛ جملههایی که قبلاً با صدای جیمنای پخش شدهاند از حافظه پخش میشوند."

        fun escapeXml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }

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

        fun cleanGermanText(text: String): String {
            val resolved = com.example.ui.components.resolveCompletedGermanText(text)
            val clean = resolved.replace(Regex("[\\u0600-\\u06FF]"), "").trim()
            return if (clean.isNotEmpty()) clean else resolved.trim()
        }

        fun collectGermanTextsFromLesson(lesson: LessonData): List<String> {
            val result = mutableListOf<String>()
            for (vocab in lesson.vocabulary) {
                val wordWithArticle = if (vocab.article.isNotBlank()) "${vocab.article} ${vocab.word}".trim() else vocab.word.trim()
                val cleanWord = cleanGermanText(wordWithArticle)
                if (cleanWord.isNotBlank()) result.add(cleanWord)
            }
            for (ex in lesson.exampleSentences) {
                val cleanEx = cleanGermanText(ex.german)
                if (cleanEx.isNotBlank()) result.add(cleanEx)
            }
            for (d in lesson.dialogues) {
                for (line in d.lines) {
                    val cleanLine = cleanGermanText(line.german)
                    if (cleanLine.isNotBlank()) result.add(cleanLine)
                }
            }
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
            for (ex in topic.exampleSentences) {
                val cleanEx = cleanGermanText(ex.german)
                if (cleanEx.isNotBlank()) result.add(cleanEx)
            }
            return result.distinct()
        }

        fun extractGermanLinesFromSectionBody(bodyDari: String): List<String> {
            val results = mutableListOf<String>()
            val rawLines = bodyDari.split("\n")
            for (raw in rawLines) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (line.contains("|")) {
                    val segments = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    for (seg in segments) {
                        if (com.example.ui.components.isMainlyLatin(seg)) {
                            val clean = cleanGermanText(seg)
                            if (clean.isNotBlank()) results.add(clean)
                        }
                    }
                } else if (com.example.ui.components.isGermanItemWithDariParenthesis(line)) {
                    val openParen = line.indexOf('(')
                    val beforeParen = line.substring(0, openParen).removePrefix("•").removePrefix("-").removePrefix("–").trim()
                    val clean = cleanGermanText(beforeParen)
                    if (clean.isNotBlank()) results.add(clean)
                } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–")) {
                    val content = line.removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("–").trim()
                    if (com.example.ui.components.isMainlyLatin(content)) {
                        val clean = cleanGermanText(content)
                        if (clean.isNotBlank()) results.add(clean)
                    }
                } else if (com.example.ui.components.isMainlyLatin(line)) {
                    val clean = cleanGermanText(line)
                    if (clean.isNotBlank()) results.add(clean)
                }
            }
            return results.distinct()
        }

        fun extractAllGermanLinesForReading(
            lesson: LessonData,
            stepByStepIndex: Int? = null
        ): List<String> {
            val results = mutableListOf<String>()
            if (stepByStepIndex != null && stepByStepIndex in lesson.grammarSections.indices) {
                val sec = lesson.grammarSections[stepByStepIndex]
                results.addAll(extractGermanLinesFromSectionBody(sec.bodyDari))
            } else {
                for (sec in lesson.grammarSections) {
                    results.addAll(extractGermanLinesFromSectionBody(sec.bodyDari))
                }
                for (ex in lesson.exampleSentences) {
                    val clean = cleanGermanText(ex.german)
                    if (clean.isNotBlank()) results.add(clean)
                }
                for (d in lesson.dialogues) {
                    for (line in d.lines) {
                        val clean = cleanGermanText(line.german)
                        if (clean.isNotBlank()) results.add(clean)
                    }
                }
            }
            return results.distinct()
        }

        fun extractBilingualLinesFromSectionBody(bodyDari: String): List<String> {
            val results = mutableListOf<String>()
            val rawLines = bodyDari.split("\n")
            for (raw in rawLines) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (line.contains("|")) {
                    val segments = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    val germanSeg = segments.firstOrNull { com.example.ui.components.isMainlyLatin(it) }
                    val dariSeg = segments.firstOrNull { !com.example.ui.components.isMainlyLatin(it) }
                    if (germanSeg != null) {
                        val clean = cleanGermanText(germanSeg)
                        if (clean.isNotBlank()) {
                            results.add(if (dariSeg != null) "$clean ⟦$dariSeg⟧" else clean)
                        }
                    }
                } else if (com.example.ui.components.isGermanItemWithDariParenthesis(line)) {
                    val openParen = line.indexOf('(')
                    val closeParen = line.lastIndexOf(')')
                    val beforeParen = line.substring(0, openParen).removePrefix("•").removePrefix("-").removePrefix("–").trim()
                    val insideParen = if (closeParen > openParen) line.substring(openParen + 1, closeParen).trim() else ""
                    val clean = cleanGermanText(beforeParen)
                    if (clean.isNotBlank()) {
                        results.add(if (insideParen.isNotBlank()) "$clean ⟦$insideParen⟧" else clean)
                    }
                } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–")) {
                    val content = line.removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("–").trim()
                    if (com.example.ui.components.isMainlyLatin(content)) {
                        val clean = cleanGermanText(content)
                        if (clean.isNotBlank()) results.add(clean)
                    }
                } else if (com.example.ui.components.isMainlyLatin(line)) {
                    val clean = cleanGermanText(line)
                    if (clean.isNotBlank()) results.add(clean)
                }
            }
            return results.distinct()
        }

        fun extractAllBilingualLinesForReading(
            lesson: LessonData,
            stepByStepIndex: Int? = null
        ): List<String> {
            val results = mutableListOf<String>()
            if (stepByStepIndex != null && stepByStepIndex in lesson.grammarSections.indices) {
                val sec = lesson.grammarSections[stepByStepIndex]
                results.addAll(extractBilingualLinesFromSectionBody(sec.bodyDari))
            } else {
                for (sec in lesson.grammarSections) {
                    results.addAll(extractBilingualLinesFromSectionBody(sec.bodyDari))
                }
                for (ex in lesson.exampleSentences) {
                    val clean = cleanGermanText(ex.german)
                    if (clean.isNotBlank()) {
                        val combined = if (ex.meaningDari.isNotBlank()) "$clean ⟦${ex.meaningDari}⟧" else clean
                        results.add(combined)
                    }
                }
                for (d in lesson.dialogues) {
                    for (line in d.lines) {
                        val clean = cleanGermanText(line.german)
                        if (clean.isNotBlank()) {
                            val combined = if (line.meaningDari.isNotBlank()) "$clean ⟦${line.meaningDari}⟧" else clean
                            results.add(combined)
                        }
                    }
                }
            }
            return results.distinct()
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

        fun getVoiceLibraryKey(
            text: String,
            voiceName: String = DEFAULT_VOICE_NAME,
            provider: String? = null
        ): String {
            val raw = if (provider != null) {
                "${provider}_${voiceName}_${text.trim()}"
            } else {
                "${voiceName}_${text.trim()}"
            }
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
                file.isFile && (file.extension.equals("wav", ignoreCase = true) || file.extension.equals("mp3", ignoreCase = true)) && !file.name.equals(".nomedia")
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
                file.isFile && (file.extension.equals("wav", ignoreCase = true) || file.extension.equals("mp3", ignoreCase = true)) && !file.name.equals(".nomedia")
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
                var hasValidAudioFiles = false
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
                        if (!safeFileName.endsWith(".wav", ignoreCase = true) && !safeFileName.endsWith(".mp3", ignoreCase = true)) {
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
                            hasValidAudioFiles = true
                        } else {
                            destFile.delete()
                        }
                        entry = zipIn.nextEntry
                    }
                }

                if (!hasValidAudioFiles) {
                    tempDir.deleteRecursively()
                    return VoiceLibraryImportResult(
                        isSuccess = false,
                        errorMessage = "هیچ فایل صوتی معتبری (.wav یا .mp3) در این فایل زیپ یافت نشد."
                    )
                }

                val extractedFiles = tempDir.listFiles { f ->
                    f.isFile && (f.extension.equals("wav", ignoreCase = true) || f.extension.equals("mp3", ignoreCase = true))
                } ?: emptyArray()
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
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "خروجی حافظهٔ صدا - voice-library.zip")
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

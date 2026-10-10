package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProgressManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("german_learning_prefs", Context.MODE_PRIVATE)

    private val _learnedWordsFlow = MutableStateFlow<Set<String>>(emptySet())
    val learnedWordsFlow: StateFlow<Set<String>> = _learnedWordsFlow.asStateFlow()

    private val _quizHighScoreFlow = MutableStateFlow(0)
    val quizHighScoreFlow: StateFlow<Int> = _quizHighScoreFlow.asStateFlow()

    private val _quizzesTakenCountFlow = MutableStateFlow(0)
    val quizzesTakenCountFlow: StateFlow<Int> = _quizzesTakenCountFlow.asStateFlow()

    private val _geminiApiKeyFlow = MutableStateFlow("")
    val geminiApiKeyFlow: StateFlow<String> = _geminiApiKeyFlow.asStateFlow()

    private val _azureSpeechKeyFlow = MutableStateFlow("")
    val azureSpeechKeyFlow: StateFlow<String> = _azureSpeechKeyFlow.asStateFlow()

    private val _azureSpeechRegionFlow = MutableStateFlow("eastus")
    val azureSpeechRegionFlow: StateFlow<String> = _azureSpeechRegionFlow.asStateFlow()

    private val _azureSpeechVoiceFlow = MutableStateFlow("de-DE-ConradNeural")
    val azureSpeechVoiceFlow: StateFlow<String> = _azureSpeechVoiceFlow.asStateFlow()

    private val _voiceProviderFlow = MutableStateFlow("AZURE")
    val voiceProviderFlow: StateFlow<String> = _voiceProviderFlow.asStateFlow()

    private val _playbackSpeedFlow = MutableStateFlow(1.0f)
    val playbackSpeedFlow: StateFlow<Float> = _playbackSpeedFlow.asStateFlow()

    private val _ttsCooldownActiveFlow = MutableStateFlow(false)
    val ttsCooldownActiveFlow: StateFlow<Boolean> = _ttsCooldownActiveFlow.asStateFlow()

    private val _dailyGeminiRequestsFlow = MutableStateFlow(0)
    val dailyGeminiRequestsFlow: StateFlow<Int> = _dailyGeminiRequestsFlow.asStateFlow()

    private val _dailyAzureRequestsFlow = MutableStateFlow(0)
    val dailyAzureRequestsFlow: StateFlow<Int> = _dailyAzureRequestsFlow.asStateFlow()

    private val _studyDaysFlow = MutableStateFlow<Set<String>>(emptySet())
    val studyDaysFlow: StateFlow<Set<String>> = _studyDaysFlow.asStateFlow()

    private val _studyStreakFlow = MutableStateFlow(0)
    val studyStreakFlow: StateFlow<Int> = _studyStreakFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val learnedSet = prefs.getStringSet(KEY_LEARNED_WORDS, emptySet()) ?: emptySet()
        _learnedWordsFlow.value = learnedSet
        _quizHighScoreFlow.value = prefs.getInt(KEY_QUIZ_HIGH_SCORE, 0)
        _quizzesTakenCountFlow.value = prefs.getInt(KEY_QUIZZES_TAKEN_COUNT, 0)
        _geminiApiKeyFlow.value = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        _azureSpeechKeyFlow.value = prefs.getString(KEY_AZURE_SPEECH_KEY, "") ?: ""
        _azureSpeechRegionFlow.value = prefs.getString(KEY_AZURE_SPEECH_REGION, DEFAULT_AZURE_REGION) ?: DEFAULT_AZURE_REGION
        _azureSpeechVoiceFlow.value = prefs.getString(KEY_AZURE_SPEECH_VOICE, DEFAULT_AZURE_VOICE) ?: DEFAULT_AZURE_VOICE
        _voiceProviderFlow.value = prefs.getString(KEY_VOICE_PROVIDER, VOICE_PROVIDER_AZURE) ?: VOICE_PROVIDER_AZURE
        _playbackSpeedFlow.value = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        _ttsCooldownActiveFlow.value = false
        _dailyGeminiRequestsFlow.value = getDailyGeminiRequestsCount()
        _dailyAzureRequestsFlow.value = getDailyAzureRequestsCount()

        val studyDays = prefs.getStringSet(KEY_STUDY_DAYS, emptySet()) ?: emptySet()
        _studyDaysFlow.value = studyDays
        _studyStreakFlow.value = calculateStreak(studyDays)
    }

    fun getVoiceProvider(): String = _voiceProviderFlow.value

    fun setVoiceProvider(provider: String) {
        val valid = if (provider.equals(VOICE_PROVIDER_GEMINI, ignoreCase = true)) VOICE_PROVIDER_GEMINI else VOICE_PROVIDER_AZURE
        prefs.edit().putString(KEY_VOICE_PROVIDER, valid).apply()
        _voiceProviderFlow.value = valid
    }

    fun getAzureSpeechKey(): String = _azureSpeechKeyFlow.value.trim()

    fun saveAzureSpeechKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_AZURE_SPEECH_KEY, trimmed).apply()
        _azureSpeechKeyFlow.value = trimmed
    }

    fun getAzureSpeechRegion(): String = _azureSpeechRegionFlow.value.trim().ifEmpty { DEFAULT_AZURE_REGION }

    fun setAzureSpeechRegion(region: String) {
        val trimmed = region.trim().ifEmpty { DEFAULT_AZURE_REGION }
        prefs.edit().putString(KEY_AZURE_SPEECH_REGION, trimmed).apply()
        _azureSpeechRegionFlow.value = trimmed
    }

    fun getAzureSpeechVoice(): String = _azureSpeechVoiceFlow.value.trim().ifEmpty { DEFAULT_AZURE_VOICE }

    fun setAzureSpeechVoice(voice: String) {
        val trimmed = voice.trim().ifEmpty { DEFAULT_AZURE_VOICE }
        prefs.edit().putString(KEY_AZURE_SPEECH_VOICE, trimmed).apply()
        _azureSpeechVoiceFlow.value = trimmed
    }

    fun getActiveVoiceProvider(): String {
        val chosen = getVoiceProvider()
        val hasAzure = getAzureSpeechKey().isNotEmpty()
        val hasGemini = getEffectiveGeminiApiKey().isNotEmpty()
        return when {
            chosen == VOICE_PROVIDER_AZURE && hasAzure -> VOICE_PROVIDER_AZURE
            chosen == VOICE_PROVIDER_GEMINI && hasGemini -> VOICE_PROVIDER_GEMINI
            hasAzure -> VOICE_PROVIDER_AZURE
            hasGemini -> VOICE_PROVIDER_GEMINI
            else -> "DEVICE"
        }
    }

    fun getActiveVoiceChipText(): String {
        return when (getActiveVoiceProvider()) {
            VOICE_PROVIDER_AZURE -> "صدا: آژور ✨"
            VOICE_PROVIDER_GEMINI -> "صدا: جیمنای ✨"
            else -> "صدا: گوشی"
        }
    }

    fun getTodayDateKey(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }

    fun getDailyGeminiRequestsCount(): Int {
        val today = getTodayDateKey()
        val savedDate = prefs.getString("daily_tts_requests_date", "")
        val count = if (savedDate == today) {
            prefs.getInt("daily_tts_requests_count", 0)
        } else {
            0
        }
        _dailyGeminiRequestsFlow.value = count
        return count
    }

    @Synchronized
    fun incrementDailyGeminiRequestsCount(): Int {
        val today = getTodayDateKey()
        val savedDate = prefs.getString("daily_tts_requests_date", "")
        val current = if (savedDate == today) prefs.getInt("daily_tts_requests_count", 0) else 0
        val newCount = current + 1
        prefs.edit()
            .putString("daily_tts_requests_date", today)
            .putInt("daily_tts_requests_count", newCount)
            .commit()
        _dailyGeminiRequestsFlow.value = newCount
        return newCount
    }

    fun setDailyGeminiRequestsForTesting(date: String, count: Int) {
        prefs.edit()
            .putString("daily_tts_requests_date", date)
            .putInt("daily_tts_requests_count", count)
            .commit()
        _dailyGeminiRequestsFlow.value = if (date == getTodayDateKey()) count else 0
    }

    fun getDailyAzureRequestsCount(): Int {
        val today = getTodayDateKey()
        val savedDate = prefs.getString("daily_azure_requests_date", "")
        val count = if (savedDate == today) {
            prefs.getInt("daily_azure_requests_count", 0)
        } else {
            0
        }
        _dailyAzureRequestsFlow.value = count
        return count
    }

    @Synchronized
    fun incrementDailyAzureRequestsCount(): Int {
        val today = getTodayDateKey()
        val savedDate = prefs.getString("daily_azure_requests_date", "")
        val current = if (savedDate == today) prefs.getInt("daily_azure_requests_count", 0) else 0
        val newCount = current + 1
        prefs.edit()
            .putString("daily_azure_requests_date", today)
            .putInt("daily_azure_requests_count", newCount)
            .commit()
        _dailyAzureRequestsFlow.value = newCount
        return newCount
    }

    fun setDailyAzureRequestsForTesting(date: String, count: Int) {
        prefs.edit()
            .putString("daily_azure_requests_date", date)
            .putInt("daily_azure_requests_count", count)
            .commit()
        _dailyAzureRequestsFlow.value = if (date == getTodayDateKey()) count else 0
    }

    fun areAllTtsModelsInCooldown(): Boolean {
        val now = System.currentTimeMillis()
        val tts1 = prefs.getLong("tts_cooldown_gemini-2.5-flash-tts", 0L)
        val tts2 = prefs.getLong("tts_cooldown_gemini-2.5-flash-preview-tts", 0L)
        return now < tts1 && now < tts2
    }

    fun isTtsModelInCooldown(model: String): Boolean {
        val now = System.currentTimeMillis()
        val until = prefs.getLong("tts_cooldown_$model", 0L)
        return now < until
    }

    fun getTtsCooldownUntil(model: String): Long {
        return prefs.getLong("tts_cooldown_$model", 0L)
    }

    fun setTtsCooldown(model: String, cooldownUntilEpochMs: Long) {
        prefs.edit().putLong("tts_cooldown_$model", cooldownUntilEpochMs).apply()
        _ttsCooldownActiveFlow.value = areAllTtsModelsInCooldown()
    }

    fun clearTtsCooldowns() {
        prefs.edit()
            .remove("tts_cooldown_gemini-2.5-flash-tts")
            .remove("tts_cooldown_gemini-2.5-flash-preview-tts")
            .apply()
        _ttsCooldownActiveFlow.value = false
    }

    fun notifyTtsCooldownUpdated() {
        _ttsCooldownActiveFlow.value = areAllTtsModelsInCooldown()
    }

    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = when {
            speed <= 0.6f -> 0.5f
            speed <= 0.85f -> 0.75f
            speed <= 1.1f -> 1.0f
            speed <= 1.35f -> 1.25f
            else -> 1.5f
        }
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, validSpeed).apply()
        _playbackSpeedFlow.value = validSpeed
    }

    fun getPlaybackSpeed(): Float = _playbackSpeedFlow.value

    fun saveGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        _geminiApiKeyFlow.value = trimmed
    }

    fun getEffectiveGeminiApiKey(): String {
        val userKey = _geminiApiKeyFlow.value.trim()
        if (userKey.isNotEmpty()) return userKey

        // Fallback to BuildConfig if configured
        val buildKey = com.example.BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun isWordLearned(wordId: String): Boolean {
        return _learnedWordsFlow.value.contains(wordId)
    }

    fun toggleWordLearned(wordId: String): Boolean {
        val current = _learnedWordsFlow.value.toMutableSet()
        val isNowLearned: Boolean
        if (current.contains(wordId)) {
            current.remove(wordId)
            isNowLearned = false
        } else {
            current.add(wordId)
            isNowLearned = true
            recordStudyDay()
        }
        prefs.edit().putStringSet(KEY_LEARNED_WORDS, current).apply()
        _learnedWordsFlow.value = current
        return isNowLearned
    }

    fun setWordLearned(wordId: String, learned: Boolean) {
        val current = _learnedWordsFlow.value.toMutableSet()
        if (learned) {
            current.add(wordId)
            recordStudyDay()
        } else {
            current.remove(wordId)
        }
        prefs.edit().putStringSet(KEY_LEARNED_WORDS, current).apply()
        _learnedWordsFlow.value = current
    }

    fun recordStudyDay(date: String = getTodayDateKey()): Int {
        val current = _studyDaysFlow.value.toMutableSet()
        val added = current.add(date)
        val newStreak = calculateStreak(current)
        if (added) {
            prefs.edit().putStringSet(KEY_STUDY_DAYS, current).apply()
            _studyDaysFlow.value = current
            _studyStreakFlow.value = newStreak
        } else {
            _studyStreakFlow.value = newStreak
        }
        return newStreak
    }

    fun getStudyStreak(): Int = _studyStreakFlow.value

    fun isTodayStudied(todayDateStr: String = getTodayDateKey()): Boolean {
        return _studyDaysFlow.value.contains(todayDateStr)
    }

    fun setStudyDaysForTesting(days: Set<String>, todayDateStr: String = getTodayDateKey()) {
        prefs.edit().putStringSet(KEY_STUDY_DAYS, days).apply()
        _studyDaysFlow.value = days
        _studyStreakFlow.value = calculateStreak(days, todayDateStr)
    }

    fun calculateStreak(studyDays: Set<String>, todayDateStr: String = getTodayDateKey()): Int {
        if (studyDays.isEmpty()) return 0

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(todayDateStr)
            if (parsed != null) {
                cal.time = parsed
            }
        } catch (e: Exception) {
            // fallback to current system time
        }

        val todayFormatted = dateFormat.format(cal.time)
        val studiedToday = studyDays.contains(todayFormatted)

        if (!studiedToday) {
            // Check yesterday
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayFormatted = dateFormat.format(cal.time)
            if (!studyDays.contains(yesterdayFormatted)) {
                return 0
            }
            // Yesterday is the start of the consecutive streak ending yesterday
        }

        var streak = 0
        while (true) {
            val checkDate = dateFormat.format(cal.time)
            if (studyDays.contains(checkDate)) {
                streak++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun saveQuizResult(score: Int, total: Int) {
        recordStudyDay()
        val currentHigh = _quizHighScoreFlow.value
        val newHigh = maxOf(currentHigh, score)
        val newCount = _quizzesTakenCountFlow.value + 1

        prefs.edit()
            .putInt(KEY_QUIZ_HIGH_SCORE, newHigh)
            .putInt(KEY_QUIZZES_TAKEN_COUNT, newCount)
            .apply()

        _quizHighScoreFlow.value = newHigh
        _quizzesTakenCountFlow.value = newCount
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
        _learnedWordsFlow.value = emptySet()
        _quizHighScoreFlow.value = 0
        _quizzesTakenCountFlow.value = 0
        _studyDaysFlow.value = emptySet()
        _studyStreakFlow.value = 0
    }

    companion object {
        private const val KEY_LEARNED_WORDS = "learned_words"
        private const val KEY_QUIZ_HIGH_SCORE = "quiz_high_score"
        private const val KEY_QUIZZES_TAKEN_COUNT = "quizzes_taken_count"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_STUDY_DAYS = "study_days"

        const val KEY_VOICE_PROVIDER = "voice_provider"
        const val KEY_AZURE_SPEECH_KEY = "azure_speech_key"
        const val KEY_AZURE_SPEECH_REGION = "azure_speech_region"
        const val KEY_AZURE_SPEECH_VOICE = "azure_speech_voice"

        const val VOICE_PROVIDER_AZURE = "AZURE"
        const val VOICE_PROVIDER_GEMINI = "GEMINI"

        const val DEFAULT_AZURE_REGION = "eastus"
        const val DEFAULT_AZURE_VOICE = "de-DE-ConradNeural"
        const val AZURE_VOICE_CONRAD = "de-DE-ConradNeural"
        const val AZURE_VOICE_KLARA = "de-DE-KlaraNeural"

        val SUPPORTED_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

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

        fun formatSpeedToPersian(speed: Float): String {
            return when (speed) {
                0.5f -> "۰.۵x"
                0.75f -> "۰.۷۵x"
                1.0f -> "۱.۰x"
                1.25f -> "۱.۲۵x"
                1.5f -> "۱.۵x"
                else -> toPersianDigits("${speed}x")
            }
        }

        @Volatile
        private var instance: UserProgressManager? = null

        fun getInstance(context: Context): UserProgressManager {
            return instance ?: synchronized(this) {
                instance ?: UserProgressManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

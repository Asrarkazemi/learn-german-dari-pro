package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LessonData
import com.example.data.repository.BuiltInCourseData
import com.example.ui.consumeTaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("آلمانی بیاموز", appName)
    }

    @Test
    fun `verify unified schema has 8 built-in lessons with mandatory pronunciation and translation`() {
        assertEquals(8, BuiltInCourseData.lessons.size)
        for (lesson in BuiltInCourseData.lessons) {
            assertTrue("Lesson ${lesson.number} must have >= 15 vocabulary items", lesson.vocabulary.size >= 15)
            for (vocab in lesson.vocabulary) {
                assertTrue(vocab.word.isNotBlank())
                assertTrue("Pronunciation in Persian script is required for ${vocab.word}", vocab.pronunciationPersianScript.isNotBlank())
                assertTrue("Meaning in Dari is required for ${vocab.word}", vocab.meaningDari.isNotBlank())
            }

            assertTrue("Lesson ${lesson.number} must have exercises", lesson.exercises.isNotEmpty())
            for (ex in lesson.exercises) {
                assertEquals("Each exercise must have exactly 4 options", 4, ex.options.size)
                assertTrue("Exercise correctAnswer must be in options", ex.options.contains(ex.correctAnswer))
                assertTrue("Exercise must have pronunciation", ex.pronunciation.isNotBlank())
                assertTrue("Exercise must have Dari translation", ex.translationDari.isNotBlank())
                assertTrue("Exercise must have Dari explanation", ex.explanationDari.isNotBlank())
            }
        }
    }

    @Test
    fun `verify lesson parser rejects missing pronunciation or translation`() {
        // Missing pronunciation in vocabulary
        val invalidJson = """
            {
              "id": "test_invalid",
              "number": 9,
              "titleGerman": "Test",
              "titleDari": "تست",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Test",
                  "pronunciationPersianScript": "",
                  "meaningDari": "تست"
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(invalidJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("تلفظ") == true)
    }

    @Test
    fun `verify lesson parser succeeds for valid schema`() {
        val validJson = """
            {
              "id": "test_valid_9",
              "number": 9,
              "titleGerman": "Reisen",
              "titleDari": "سفر کردن",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Zug",
                  "pronunciationPersianScript": "تسوگ",
                  "meaningDari": "قطار"
                }
              ],
              "exampleSentences": [
                {
                  "german": "Der Zug kommt pünktlich.",
                  "pronunciation": "دِر تسوگ کومت پونکتلیش.",
                  "meaningDari": "قطار به موقع می‌رسد."
                }
              ],
              "exercises": [
                {
                  "type": "multiple-choice",
                  "question": "___ Zug fährt nach Berlin.",
                  "pronunciation": "دِر تسوگ فِرت ناخ برلین.",
                  "translationDari": "قطار به سمت برلین حرکت می‌کند.",
                  "options": ["Der", "Die", "Das", "Den"],
                  "correctAnswer": "Der",
                  "explanationDari": "واژه Zug مذکر است و با حرف تعریف Der می‌آید."
                }
              ],
              "dialogues": [
                {
                  "lines": [
                    {
                      "speaker": "احمد",
                      "german": "Wann fährt der Zug?",
                      "pronunciation": "وان فِرت دِر تسوگ؟",
                      "meaningDari": "قطار چه ساعتی حرکت می‌کند؟"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(validJson)
        assertTrue(result.isSuccess)
        val lesson = result.getOrNull()
        assertNotNull(lesson)
        assertEquals("test_valid_9", lesson?.id)
        assertEquals("Reisen", lesson?.titleGerman)
        assertEquals("سفر کردن", lesson?.titleDari)
        assertEquals(1, lesson?.vocabulary?.size)
        assertEquals("Zug", lesson?.vocabulary?.first()?.word)
        assertEquals("تسوگ", lesson?.vocabulary?.first()?.pronunciationPersianScript)
        assertEquals(4, lesson?.exercises?.first()?.options?.size)
    }

    @Test
    fun `verify source field parsing for muse lessons`() {
        val museJson = """
            {
              "id": "muse_lesson_1",
              "number": 9,
              "source": "muse",
              "titleGerman": "Im Restaurant",
              "titleDari": "در رستورانت",
              "vocabulary": [
                {
                  "article": "die",
                  "word": "Suppe",
                  "pronunciationPersianScript": "زوپه",
                  "meaningDari": "شوربا / سوپ"
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(museJson)
        assertTrue(result.isSuccess)
        val lesson = result.getOrNull()
        assertNotNull(lesson)
        assertEquals("muse", lesson?.source)
    }

    @Test
    fun `verify grammarSections and qaPairs parsing and serialization`() {
        val jsonWithGrammarAndQa = """
            {
              "id": "test_grammar_qa",
              "number": 10,
              "titleGerman": "Grammatik & Dialog",
              "titleDari": "گرامر و گفتگو",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Kugelschreiber",
                  "pronunciationPersianScript": "کوگِل‌شرایبِر",
                  "meaningDari": "خودکار"
                }
              ],
              "grammarSections": [
                {
                  "title": "حروف اضافه",
                  "bodyDari": "توضیح کامل در مورد حروف اضافه زمان و مکان."
                }
              ],
              "qaPairs": [
                {
                  "questionGerman": "Hast du einen Stift?",
                  "questionPronunciation": "هاست دو آینن شتیفت؟",
                  "questionDari": "آیا قلم داری؟",
                  "answerGerman": "Ja, hier ist ein Stift.",
                  "answerPronunciation": "یا، هیر ایست آین شتیفت.",
                  "answerDari": "بله، اینجا یک قلم است."
                }
              ]
            }
        """.trimIndent()

        val parseResult = LessonData.parseAndValidate(jsonWithGrammarAndQa)
        assertTrue(parseResult.isSuccess)
        val lesson = parseResult.getOrThrow()
        assertEquals(1, lesson.grammarSections.size)
        assertEquals("حروف اضافه", lesson.grammarSections.first().title)
        assertEquals(1, lesson.qaPairs.size)
        assertEquals("Hast du einen Stift?", lesson.qaPairs.first().questionGerman)
        assertEquals("Ja, hier ist ein Stift.", lesson.qaPairs.first().answerGerman)

        // Verify toJson preserves them
        val exportedJson = lesson.toJson()
        val reimported = LessonData.parseAndValidate(exportedJson)
        assertTrue(reimported.isSuccess)
        assertEquals(1, reimported.getOrThrow().grammarSections.size)
        assertEquals(1, reimported.getOrThrow().qaPairs.size)
    }

    @Test
    fun `verify qaPairs validation fails if any of the six fields is empty`() {
        val invalidQaJson = """
            {
              "id": "test_invalid_qa",
              "number": 10,
              "titleGerman": "Invalid QA",
              "titleDari": "تست سوال و جواب ناقص",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Stift",
                  "pronunciationPersianScript": "شتیفت",
                  "meaningDari": "قلم"
                }
              ],
              "qaPairs": [
                {
                  "questionGerman": "Wo ist das?",
                  "questionPronunciation": "وو ایست داس؟",
                  "questionDari": "",
                  "answerGerman": "Hier.",
                  "answerPronunciation": "هیر.",
                  "answerDari": "اینجا."
                }
              ]
            }
        """.trimIndent()

        val parseResult = LessonData.parseAndValidate(invalidQaJson)
        assertTrue(parseResult.isFailure)
    }

    @Test
    fun `verify same-number import replaces existing lesson and delete restores built-in`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.UnifiedCourseRepository.getInstance(context)

        val replacementLessonJson = """
            {
              "id": "replacement_lesson_1",
              "number": 1,
              "titleGerman": "Ersetzte Lektion 1",
              "titleDari": "درس ۱ جایگزین شده",
              "vocabulary": [
                {
                  "article": "das",
                  "word": "Haus",
                  "pronunciationPersianScript": "هاوس",
                  "meaningDari": "خانه"
                }
              ]
            }
        """.trimIndent()

        val importResult = repo.importLesson(replacementLessonJson)
        assertTrue(importResult.isSuccess)

        val replaced = repo.lessonsFlow.value.find { it.number == 1 }
        assertNotNull(replaced)
        assertEquals("Ersetzte Lektion 1", replaced?.titleGerman)
        assertEquals("درس ۱ جایگزین شده", replaced?.titleDari)

        // Deleting custom/override lesson should restore the built-in one
        val deleted = repo.deleteCustomLesson("1")
        assertTrue(deleted)

        val restored = repo.lessonsFlow.value.find { it.number == 1 }
        assertNotNull(restored)
        assertEquals("Begrüßung & Vorstellen", restored?.titleGerman)
    }

    @Test
    fun `verify batch import supports JSON array and reports summary`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.UnifiedCourseRepository.getInstance(context)

        val jsonArrayStr = """
            [
              {
                "id": "batch_lesson_1",
                "number": 1,
                "titleGerman": "Batch 1",
                "titleDari": "درس ۱ دسته‌ای",
                "vocabulary": [
                  {
                    "article": "der",
                    "word": "Morgen",
                    "pronunciationPersianScript": "مورگن",
                    "meaningDari": "صبح"
                  }
                ]
              },
              {
                "id": "batch_lesson_2",
                "number": 2,
                "titleGerman": "Batch 2",
                "titleDari": "درس ۲ دسته‌ای",
                "vocabulary": [
                  {
                    "article": "die",
                    "word": "Nacht",
                    "pronunciationPersianScript": "ناخت",
                    "meaningDari": "شب"
                  }
                ]
              }
            ]
        """.trimIndent()

        val batchResult = repo.importJson(jsonArrayStr)
        assertTrue(batchResult.isSuccess)
        val result = batchResult.getOrThrow()
        assertEquals(2, result.totalProcessed)
        assertEquals(2, result.successCount)
        assertEquals(0, result.failureCount)
        assertTrue(result.summaryMessage.contains("۲ درس با موفقیت وارد/جایگزین شد"))

        // Cleanup
        repo.deleteCustomLesson("1")
        repo.deleteCustomLesson("2")
    }

    @Test
    fun `verify playback speed persistence and Persian formatting in UserProgressManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)

        // Default speed must be 1.0f
        progressManager.setPlaybackSpeed(1.0f)
        assertEquals(1.0f, progressManager.getPlaybackSpeed(), 0.01f)

        // Format check
        assertEquals("۰.۵x", com.example.data.storage.UserProgressManager.formatSpeedToPersian(0.5f))
        assertEquals("۰.۷۵x", com.example.data.storage.UserProgressManager.formatSpeedToPersian(0.75f))
        assertEquals("۱.۰x", com.example.data.storage.UserProgressManager.formatSpeedToPersian(1.0f))
        assertEquals("۱.۲۵x", com.example.data.storage.UserProgressManager.formatSpeedToPersian(1.25f))
        assertEquals("۱.۵x", com.example.data.storage.UserProgressManager.formatSpeedToPersian(1.5f))

        // Set speed to 1.25f and verify persistence
        progressManager.setPlaybackSpeed(1.25f)
        assertEquals(1.25f, progressManager.getPlaybackSpeed(), 0.01f)
        assertEquals(1.25f, progressManager.playbackSpeedFlow.value, 0.01f)

        // Test with TtsManager
        val ttsManager = com.example.util.TtsManager(
            context = context,
            speedProvider = { progressManager.getPlaybackSpeed() }
        )
        assertEquals(1.25f, ttsManager.getEffectiveSpeed(), 0.01f)

        // Reset back to 1.0f
        progressManager.setPlaybackSpeed(1.0f)
        assertEquals(1.0f, ttsManager.getEffectiveSpeed(), 0.01f)
    }

    @Test
    fun `verify grammar rendering direction detection and table splitting`() {
        // Latin text detection
        assertTrue(com.example.ui.components.isMainlyLatin("Ich lerne Deutsch."))
        assertTrue(com.example.ui.components.isMainlyLatin("Guten Tag!"))
        assertTrue(com.example.ui.components.isMainlyLatin("der Lehrer"))

        // Dari text detection
        org.junit.Assert.assertFalse(com.example.ui.components.isMainlyLatin("سلام و روز بخیر!"))
        org.junit.Assert.assertFalse(com.example.ui.components.isMainlyLatin("من آلمانی یاد می‌گیرم."))
        org.junit.Assert.assertFalse(com.example.ui.components.isMainlyLatin("افعال مهم برای سلام و معرفی"))

        // Table row with pipe "|" split check
        val tableLine = "ich bin | من هستم"
        val segments = tableLine.split("|").map { it.trim() }
        assertEquals(2, segments.size)
        assertTrue(com.example.ui.components.isMainlyLatin(segments[0]))
        org.junit.Assert.assertFalse(com.example.ui.components.isMainlyLatin(segments[1]))
    }

    @Test
    fun `verify GrammarBookRepository isolation, persistence, number 101 support, and delete`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val grammarBookRepo = com.example.data.repository.GrammarBookRepository.getInstance(context)
        val courseRepo = com.example.data.repository.UnifiedCourseRepository.getInstance(context)

        val grammarLesson101Json = """
            {
              "id": "grammar_book_101",
              "number": 101,
              "titleGerman": "Personalpronomen",
              "titleDari": "ضمایر شخصی در زبان آلمانی",
              "vocabulary": [
                {
                  "article": "das",
                  "word": "Pronomen",
                  "pronunciationPersianScript": "پرونومِن",
                  "meaningDari": "ضمیر"
                }
              ],
              "exampleSentences": [
                {
                  "german": "Ich lerne Grammatik.",
                  "pronunciation": "ایش لِرنه گراماتیک.",
                  "meaningDari": "من گرامر یاد می‌گیرم."
                }
              ],
              "exercises": [
                {
                  "type": "multiple-choice",
                  "question": "___ heiße Ali.",
                  "pronunciation": "ایش هایسه علی.",
                  "translationDari": "من علی نام دارم.",
                  "options": ["Ich", "Du", "Er", "Sie"],
                  "correctAnswer": "Ich",
                  "explanationDari": "برای فعل heiße ضمیر Ich استفاده می‌شود."
                }
              ],
              "dialogues": [],
              "grammarSections": [
                {
                  "title": "تعریف ضمایر فاعلی",
                  "bodyDari": "در زبان آلمانی ضمایر فاعلی شامل ich, du, er, sie, es, wir, ihr, sie, Sie هستند."
                }
              ]
            }
        """.trimIndent()

        // 1. Import lesson 101 into Grammar Book
        val result = grammarBookRepo.importJson(grammarLesson101Json)
        assertTrue(result.isSuccess)
        val batchRes = result.getOrThrow()
        assertEquals(1, batchRes.successCount)
        assertTrue(batchRes.summaryMessage.contains("۱۰۱"))

        // 2. Verify lesson 101 is in grammarBookRepo with number 101 preserved
        val foundInGrammarBook = grammarBookRepo.lessonsFlow.value.find { it.number == 101 }
        assertNotNull(foundInGrammarBook)
        assertEquals("Personalpronomen", foundInGrammarBook?.titleGerman)
        assertEquals(101, foundInGrammarBook?.number)

        // 3. Verify total isolation: lesson 101 MUST NOT appear in courseRepo («درس‌ها»)
        val foundInCourse = courseRepo.lessonsFlow.value.find { it.number == 101 }
        assertTrue("Lesson 101 must not appear in courseRepo", foundInCourse == null)

        // 4. Delete lesson 101 from grammarBookRepo
        val deleted = grammarBookRepo.deleteLesson("101")
        assertTrue(deleted)
        val afterDelete = grammarBookRepo.lessonsFlow.value.find { it.number == 101 }
        assertTrue(afterDelete == null)
    }

    @Test
    fun `verify Gemini Chat message extraction and chat model behavior`() {
        val userMsg = com.example.data.gemini.ChatMessage(
            isUser = true,
            text = "سلام"
        )
        assertTrue(userMsg.isUser)
        assertEquals("سلام", userMsg.text)

        val assistantResponse = """
            بله، سلام و درود! در اینجا یک درس کامل برای شما آماده کرده‌ام:
            ```json
            {
              "id": "chat_lesson_1",
              "number": 99,
              "titleGerman": "Im Restaurant",
              "titleDari": "در رستورانت",
              "vocabulary": [
                {
                  "article": "das",
                  "word": "Essen",
                  "pronunciationPersianScript": "اِسِن",
                  "meaningDari": "غذا"
                }
              ],
              "exampleSentences": [],
              "exercises": [],
              "dialogues": [],
              "grammarSections": []
            }
            ```
        """.trimIndent()

        val extracted = com.example.data.gemini.GeminiChatService.extractLessonJson(assistantResponse)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("Im Restaurant"))
        assertTrue(extracted.contains("chat_lesson_1"))

        val assistantMsg = com.example.data.gemini.ChatMessage(
            isUser = false,
            text = assistantResponse,
            extractedLessonJson = extracted
        )
        org.junit.Assert.assertFalse(assistantMsg.isUser)
        assertNotNull(assistantMsg.extractedLessonJson)
    }

    @Test
    fun `verify Gemini Chat error classification and fallback order`() {
        // Primary model check
        assertEquals("gemini-3.5-flash", com.example.data.gemini.GeminiChatService.PRIMARY_MODEL)

        // Exact fallback order check
        val expectedFallbacks = listOf(
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash"
        )
        assertEquals(expectedFallbacks, com.example.data.gemini.GeminiChatService.FALLBACK_MODELS)

        // Server busy classification checks (503 / 429 / overloaded / high-demand)
        assertTrue(com.example.data.gemini.GeminiChatService.isServerBusy(503, "The model is overloaded. Please try again later."))
        assertTrue(com.example.data.gemini.GeminiChatService.isServerBusy(429, "Resource has been exhausted (e.g. check quota)."))
        assertTrue(com.example.data.gemini.GeminiChatService.isServerBusy(200, "high demand on server"))
        assertTrue(com.example.data.gemini.GeminiChatService.isServerBusy(500, "Internal Server Error"))

        // Busy errors must NOT be classified as key errors
        org.junit.Assert.assertFalse(com.example.data.gemini.GeminiChatService.isKeyError(503, "The model is overloaded. Please try again later."))
        org.junit.Assert.assertFalse(com.example.data.gemini.GeminiChatService.isKeyError(429, "Resource has been exhausted"))

        // Key error classification checks (401 / 403 / API_KEY_INVALID)
        assertTrue(com.example.data.gemini.GeminiChatService.isKeyError(400, "API_KEY_INVALID: API key not valid. Please pass a valid API key."))
        assertTrue(com.example.data.gemini.GeminiChatService.isKeyError(401, "Unauthorized"))
        assertTrue(com.example.data.gemini.GeminiChatService.isKeyError(403, "PERMISSION_DENIED"))

        // Key errors must NOT be classified as server busy
        org.junit.Assert.assertFalse(com.example.data.gemini.GeminiChatService.isServerBusy(401, "Unauthorized"))
    }

    @Test
    fun `verify Gemini TTS quota cooldown, model fallback, and honest Dari status`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)
        val ttsManager = com.example.util.TtsManager(context)

        // 1. Model order
        assertEquals("gemini-2.5-flash-tts", com.example.util.TtsManager.PRIMARY_TTS_MODEL)
        assertEquals("gemini-2.5-flash-preview-tts", com.example.util.TtsManager.SECONDARY_TTS_MODEL)
        val expectedModels = listOf("gemini-2.5-flash-tts", "gemini-2.5-flash-preview-tts")
        assertEquals(expectedModels, com.example.util.TtsManager.TTS_MODELS)

        // 2. Exact Dari quota message
        assertEquals(
            "سهمیۀ رایگان روزانۀ صدای جیمنای تمام شده است (۱۰ جمله در روز). صدا موقتاً از گوشی پخش میشود؛ جملههایی که قبلاً با صدای جیمنای پخش شدهاند از حافظه پخش میشوند.",
            com.example.util.TtsManager.QUOTA_EXCEEDED_DARI_MSG
        )

        // 3. Quota error detection
        val userErrorSnippet = "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 10, model: gemini-2.5-flash-tts. Please retry in 11h47m"
        assertTrue(com.example.util.TtsManager.isQuotaError(429, userErrorSnippet))
        assertTrue(com.example.util.TtsManager.isQuotaError(400, "RESOURCE_EXHAUSTED: You have exceeded your current quota."))
        org.junit.Assert.assertFalse(com.example.util.TtsManager.isKeyError(429, userErrorSnippet))

        // 4. Retry-after duration parsing
        val durationMs = com.example.util.TtsManager.parseCooldownDurationMs(userErrorSnippet, null)
        assertTrue("Duration must be at least 11 hours in ms", durationMs >= 11 * 3600_000L)

        // 5. Cooldown persistence & honest status
        progressManager.clearTtsCooldowns()
        org.junit.Assert.assertFalse(progressManager.areAllTtsModelsInCooldown())
        org.junit.Assert.assertFalse(ttsManager.areAllTtsModelsInCooldown())

        // Set cooldown on both models
        val futureTime = System.currentTimeMillis() + 3600_000L
        progressManager.setTtsCooldown("gemini-2.5-flash-tts", futureTime)
        progressManager.setTtsCooldown("gemini-2.5-flash-preview-tts", futureTime)

        assertTrue(progressManager.areAllTtsModelsInCooldown())
        assertTrue(ttsManager.areAllTtsModelsInCooldown())
        org.junit.Assert.assertFalse("When in cooldown, isGeminiVoiceActive must be false", ttsManager.isGeminiVoiceActive())

        // Cleanup
        progressManager.clearTtsCooldowns()
        org.junit.Assert.assertFalse(progressManager.areAllTtsModelsInCooldown())
    }

    @Test
    fun `verify permanent in-app voice library storage, per-text keying, legacy fallback, and stats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)
        val ttsManager = com.example.util.TtsManager(context)

        // 1. Storage Location: Must be in internal private filesDir/voice_library/, NOT external storage
        val libraryDir = com.example.util.TtsManager.getVoiceLibraryDir(context)
        assertTrue(libraryDir.exists())
        assertTrue(libraryDir.isDirectory)
        assertEquals(java.io.File(context.filesDir, "voice_library").absolutePath, libraryDir.absolutePath)
        // Ensure .nomedia exists
        assertTrue(java.io.File(libraryDir, ".nomedia").exists())

        // Clear library initially
        com.example.util.TtsManager.clearVoiceLibrary(context)
        var stats = com.example.util.TtsManager.getVoiceLibraryStats(context)
        assertEquals(0, stats.count)
        assertEquals(0L, stats.totalBytes)

        // 2. Per-text keying: Key depends on text + voice ONLY, invariant to playback speed
        val testSentence = "Guten Morgen, wie geht es dir?"
        val keySpeed1 = com.example.util.TtsManager.getVoiceLibraryKey(testSentence)
        val keySpeed2 = com.example.util.TtsManager.getVoiceLibraryKey(testSentence)
        assertEquals(keySpeed1, keySpeed2)
        org.junit.Assert.assertFalse(keySpeed1.contains("speed"))

        // Neutral prompt generator
        val neutralPrompt = com.example.util.TtsManager.buildNeutralPrompt(testSentence)
        assertTrue(neutralPrompt.contains("Hochdeutsch") || neutralPrompt.contains("normal native speed"))
        assertTrue(neutralPrompt.contains(testSentence))

        // 3. Storing a sentence in permanent library
        val sentenceFile = java.io.File(libraryDir, "$keySpeed1.wav")
        sentenceFile.writeBytes(ByteArray(1024 * 10)) // 10 KB dummy audio file

        stats = com.example.util.TtsManager.getVoiceLibraryStats(context)
        assertEquals(1, stats.count)
        assertEquals(10240L, stats.totalBytes)

        // 4. Checking that findStoredAudioFile finds it immediately for any speed
        val foundFile05 = ttsManager.findStoredAudioFile(testSentence, 0.5f)
        org.junit.Assert.assertNotNull(foundFile05)
        assertEquals(sentenceFile.absolutePath, foundFile05?.absolutePath)

        val foundFile15 = ttsManager.findStoredAudioFile(testSentence, 1.5f)
        org.junit.Assert.assertNotNull(foundFile15)
        assertEquals(sentenceFile.absolutePath, foundFile15?.absolutePath)

        // 5. Library hit works even during complete quota cooldown
        val futureTime = System.currentTimeMillis() + 3600_000L
        progressManager.setTtsCooldown("gemini-2.5-flash-tts", futureTime)
        progressManager.setTtsCooldown("gemini-2.5-flash-preview-tts", futureTime)
        assertTrue(ttsManager.areAllTtsModelsInCooldown())

        // Even with cooldown, findStoredAudioFile still finds it for instant replay without API call!
        val hitDuringCooldown = ttsManager.findStoredAudioFile(testSentence, 1.0f)
        org.junit.Assert.assertNotNull("Stored sentence must hit even during cooldown", hitDuringCooldown)
        assertEquals(sentenceFile.absolutePath, hitDuringCooldown?.absolutePath)

        // 6. Legacy cache fallback: speed-keyed file in cacheDir
        val legacySentence = "Auf Wiedersehen!"
        val legacyCacheDir = java.io.File(context.cacheDir, "gemini_tts_cache").apply { mkdirs() }
        val legacyKey = com.example.util.TtsManager.getLegacyCacheKey(legacySentence, 1.25f)
        val legacyFile = java.io.File(legacyCacheDir, "$legacyKey.wav")
        legacyFile.writeBytes(ByteArray(512))

        val foundLegacy = ttsManager.findStoredAudioFile(legacySentence, 1.25f)
        org.junit.Assert.assertNotNull("Legacy speed-keyed cache file must be found as fallback", foundLegacy)
        assertEquals(legacyFile.absolutePath, foundLegacy?.absolutePath)

        // 7. Clear library
        val cleared = com.example.util.TtsManager.clearVoiceLibrary(context)
        assertTrue(cleared)
        stats = com.example.util.TtsManager.getVoiceLibraryStats(context)
        assertEquals(0, stats.count)
        assertEquals(0L, stats.totalBytes)
        org.junit.Assert.assertNull(ttsManager.findStoredAudioFile(testSentence, 1.0f))

        // Cleanup
        progressManager.clearTtsCooldowns()
        legacyFile.delete()
    }

    @Test
    fun `verify FIX L voice library export, import merge-by-filename, zip-slip defense, and Dari report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val libraryDir = com.example.util.TtsManager.getVoiceLibraryDir(context)

        // Clear library
        com.example.util.TtsManager.clearVoiceLibrary(context)
        assertEquals(0, com.example.util.TtsManager.getVoiceLibraryStats(context).count)

        // 1. Export when library is empty returns null
        val emptyExport = com.example.util.TtsManager.exportVoiceLibraryToZip(context)
        org.junit.Assert.assertNull(emptyExport)

        // 2. Add an existing sentence to library
        val existingSentence = "Hallo, wie geht es dir?"
        val existingKey = com.example.util.TtsManager.getVoiceLibraryKey(existingSentence)
        val existingFile = java.io.File(libraryDir, "$existingKey.wav")
        existingFile.writeBytes(ByteArray(1024) { 1 }) // 1 KB dummy audio
        assertEquals(1, com.example.util.TtsManager.getVoiceLibraryStats(context).count)

        // 3. Export with files creates valid voice-library.zip
        val exportedZip = com.example.util.TtsManager.exportVoiceLibraryToZip(context)
        org.junit.Assert.assertNotNull(exportedZip)
        assertTrue(exportedZip!!.exists())
        assertEquals("voice-library.zip", exportedZip.name)
        assertTrue(exportedZip.length() > 0)

        // Verify FileProvider can generate URI for exported zip
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportedZip
        )
        org.junit.Assert.assertNotNull(uri)
        assertTrue(uri.toString().contains("voice-library.zip"))

        // 4. Create an incoming ZIP file with:
        //    - existingKey.wav (should be SKIPPED)
        //    - newSentence1.wav (should be ADDED)
        //    - newSentence2.wav (should be ADDED)
        //    - ../../malicious/hack.wav (zip-slip attempt, should be REJECTED/SKIPPED)
        val newKey1 = com.example.util.TtsManager.getVoiceLibraryKey("Ich lerne Deutsch.")
        val newKey2 = com.example.util.TtsManager.getVoiceLibraryKey("Danke schön!")
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            // Entry 1: existing
            zos.putNextEntry(java.util.zip.ZipEntry("$existingKey.wav"))
            zos.write(ByteArray(2048) { 99 }) // different content to prove it won't overwrite
            zos.closeEntry()

            // Entry 2: new file 1
            zos.putNextEntry(java.util.zip.ZipEntry("$newKey1.wav"))
            zos.write(ByteArray(512) { 2 })
            zos.closeEntry()

            // Entry 3: new file 2
            zos.putNextEntry(java.util.zip.ZipEntry("$newKey2.wav"))
            zos.write(ByteArray(512) { 3 })
            zos.closeEntry()

            // Entry 4: zip-slip attack
            zos.putNextEntry(java.util.zip.ZipEntry("../../bad.wav"))
            zos.write(ByteArray(128))
            zos.closeEntry()
        }

        val incomingBytes = baos.toByteArray()

        // 5. Test import with merge-by-filename
        val importResult = com.example.util.TtsManager.importVoiceLibraryFromZip(
            context,
            java.io.ByteArrayInputStream(incomingBytes)
        )

        assertTrue(importResult.isSuccess)
        assertEquals(2, importResult.addedCount)
        assertEquals(1, importResult.skippedCount)

        // Exact Dari report format: «N جملهٔ تازه اضافه شد — M جمله از قبل بود» in Persian digits
        val expectedReport = "۲ جملهٔ تازه اضافه شد — ۱ جمله از قبل بود"
        assertEquals(expectedReport, importResult.formatDariReport())

        // Verify library stats updated
        val updatedStats = com.example.util.TtsManager.getVoiceLibraryStats(context)
        assertEquals(3, updatedStats.count)

        // Verify existing file was NOT overwritten (size should still be 1024, not 2048)
        assertEquals(1024L, existingFile.length())

        // Verify new files exist
        assertTrue(java.io.File(libraryDir, "$newKey1.wav").exists())
        assertTrue(java.io.File(libraryDir, "$newKey2.wav").exists())

        // Verify zip-slip file was NOT created anywhere outside
        org.junit.Assert.assertFalse(java.io.File(context.filesDir, "bad.wav").exists())
        org.junit.Assert.assertFalse(java.io.File(libraryDir, "bad.wav").exists())

        // 6. Test corrupt / invalid ZIP
        val corruptBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val corruptResult = com.example.util.TtsManager.importVoiceLibraryFromZip(
            context,
            java.io.ByteArrayInputStream(corruptBytes)
        )
        org.junit.Assert.assertFalse(corruptResult.isSuccess)
        org.junit.Assert.assertNotNull(corruptResult.errorMessage)
        // Library remains completely untouched
        assertEquals(3, com.example.util.TtsManager.getVoiceLibraryStats(context).count)

        // Cleanup
        com.example.util.TtsManager.clearVoiceLibrary(context)
        exportedZip.delete()
    }

    @Test
    fun `verify FIX N daily Gemini requests counter persistence and automatic date reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)

        // 1. Daily Gemini requests counter persistence and automatic date reset
        val today = progressManager.getTodayDateKey()
        val initialCount = progressManager.getDailyGeminiRequestsCount()
        val incremented = progressManager.incrementDailyGeminiRequestsCount()
        assertEquals(initialCount + 1, incremented)
        assertEquals(initialCount + 1, progressManager.getDailyGeminiRequestsCount())

        // Check formatting in Persian digits
        val countStr = com.example.data.storage.UserProgressManager.toPersianDigits("$incremented")
        assertTrue(countStr.all { it in "۰۱۲۳۴۵۶۷۸۹" })

        // Check simulated date change resets counter
        progressManager.setDailyGeminiRequestsForTesting("2020-01-01", 99)
        val countOnDifferentDay = progressManager.getDailyGeminiRequestsCount()
        assertEquals("Count should be 0 when date differs from today", 0, countOnDifferentDay)
        // Increment after new date sets count to 1 for today
        val countAfterNewDay = progressManager.incrementDailyGeminiRequestsCount()
        assertEquals(1, countAfterNewDay)
        assertEquals(1, progressManager.getDailyGeminiRequestsCount())

        // Cleanup
        progressManager.setDailyGeminiRequestsForTesting(today, 0)
    }

    @Test
    fun `verify FIX O study day persistence and streak calculation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)

        // 1. Empty study days -> streak 0
        assertEquals(0, progressManager.calculateStreak(emptySet(), "2026-10-07"))

        // 2. Studied today -> streak 1
        val today = "2026-10-07"
        assertEquals(1, progressManager.calculateStreak(setOf(today), today))

        // 3. Studied yesterday only -> streak 1
        assertEquals(1, progressManager.calculateStreak(setOf("2026-10-06"), today))

        // 4. Studied 2 days ago but not yesterday or today -> streak 0
        assertEquals(0, progressManager.calculateStreak(setOf("2026-10-05"), today))

        // 5. 3 consecutive days ending today -> streak 3
        val streak3Today = setOf("2026-10-07", "2026-10-06", "2026-10-05")
        assertEquals(3, progressManager.calculateStreak(streak3Today, today))

        // 6. 3 consecutive days ending yesterday -> streak 3
        val streak3Yesterday = setOf("2026-10-06", "2026-10-05", "2026-10-04")
        assertEquals(3, progressManager.calculateStreak(streak3Yesterday, today))

        // 7. Broken streak: today, yesterday, missing, 4 days ago -> streak 2
        val broken = setOf("2026-10-07", "2026-10-06", "2026-10-03")
        assertEquals(2, progressManager.calculateStreak(broken, today))

        // 8. Test live recordStudyDay
        progressManager.recordStudyDay()
        assertTrue(progressManager.isTodayStudied())
        assertTrue(progressManager.getStudyStreak() >= 1)
    }

    @Test
    fun `verify FIX O German line extraction from grammar sections`() {
        val sampleBody = """
            Das Verb sein ist unregelmäßig.
            • Ich bin Student (من دانشجو هستم)
            • Du bist mein Freund
            ich bin | من هستم
            du bist | تو هستی
            نکته مهم در زبان آلمانی:
            فعل در جمله جایگاه دوم را دارد.
        """.trimIndent()

        val extracted = com.example.util.TtsManager.extractGermanLinesFromSectionBody(sampleBody)
        assertTrue(extracted.contains("Das Verb sein ist unregelmäßig."))
        assertTrue(extracted.contains("Ich bin Student"))
        assertTrue(extracted.contains("Du bist mein Freund"))
        assertTrue(extracted.contains("ich bin"))
        assertTrue(extracted.contains("du bist"))
        // Dari-only lines should NOT be extracted
        org.junit.Assert.assertFalse(extracted.contains("نکته مهم در زبان آلمانی:"))
        org.junit.Assert.assertFalse(extracted.contains("فعل در جمله جایگاه دوم را دارد."))
    }

    @Test
    fun `verify FIX R root immersive focus mode state and tap consumer`() {
        var visible = true
        val state = com.example.ui.ImmersiveUiState(
            isUiVisible = visible,
            toggleUiVisibility = { visible = !visible },
            setUiVisible = { visible = it }
        )

        // 1. Initially UI chrome is visible
        assertTrue(state.isUiVisible)

        // 2. Toggling hides UI chrome (root TopAppBar, bottom navigation, tabs)
        state.toggleUiVisibility()
        org.junit.Assert.assertFalse(visible)

        // 3. Toggling again restores UI chrome
        state.toggleUiVisibility()
        assertTrue(visible)

        // 4. setUiVisible works explicitly
        state.setUiVisible(false)
        org.junit.Assert.assertFalse(visible)
        state.setUiVisible(true)
        assertTrue(visible)

        // 5. Test consumeTaps modifier does not crash and chains on Modifier
        val testModifier = androidx.compose.ui.Modifier.consumeTaps()
        org.junit.Assert.assertNotNull(testModifier)
    }

    @Test
    fun `verify Fix P dual-provider bilingual voice settings, request counters, and provider fallback order`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val progressManager = com.example.data.storage.UserProgressManager.getInstance(context)
        val ttsManager = com.example.util.TtsManager(context)

        // 1. Defaults
        assertEquals("AZURE", com.example.data.storage.UserProgressManager.VOICE_PROVIDER_AZURE)
        assertEquals("GEMINI", com.example.data.storage.UserProgressManager.VOICE_PROVIDER_GEMINI)
        assertEquals("de-DE-ConradNeural", com.example.data.storage.UserProgressManager.DEFAULT_AZURE_VOICE)
        assertEquals("eastus", com.example.data.storage.UserProgressManager.DEFAULT_AZURE_REGION)

        // Set and verify Azure preferences
        progressManager.setVoiceProvider("AZURE")
        assertEquals("AZURE", progressManager.getVoiceProvider())
        progressManager.saveAzureSpeechKey("test_azure_key_123")
        assertEquals("test_azure_key_123", progressManager.getAzureSpeechKey())
        progressManager.setAzureSpeechRegion("westeurope")
        assertEquals("westeurope", progressManager.getAzureSpeechRegion())
        progressManager.setAzureSpeechVoice("de-DE-ConradNeural")
        assertEquals("de-DE-ConradNeural", progressManager.getAzureSpeechVoice())

        // Voice gender and matching
        assertTrue(ttsManager.isSelectedVoiceMale())
        assertEquals("Charon", ttsManager.getGeminiVoiceName())
        assertEquals("de-DE-ConradNeural", ttsManager.getAzureVoiceForSegment(isPersian = false))
        assertEquals("fa-IR-FaridNeural", ttsManager.getAzureVoiceForSegment(isPersian = true))

        // Switch to Klara (female)
        progressManager.setAzureSpeechVoice("de-DE-KlaraNeural")
        org.junit.Assert.assertFalse(ttsManager.isSelectedVoiceMale())
        assertEquals("Kore", ttsManager.getGeminiVoiceName())
        assertEquals("de-DE-KlaraNeural", ttsManager.getAzureVoiceForSegment(isPersian = false))
        assertEquals("fa-IR-DilaraNeural", ttsManager.getAzureVoiceForSegment(isPersian = true))

        // Reset to Conrad
        progressManager.setAzureSpeechVoice("de-DE-ConradNeural")

        // 2. Provider chip text
        assertEquals("صدا: آژور ✨", progressManager.getActiveVoiceChipText())

        progressManager.setVoiceProvider("GEMINI")
        progressManager.saveGeminiApiKey("test_gemini_key_456")
        assertEquals("صدا: جیمنای ✨", progressManager.getActiveVoiceChipText())

        // When neither has key
        progressManager.saveAzureSpeechKey("")
        progressManager.saveGeminiApiKey("")
        assertEquals("صدا: گوشی", progressManager.getActiveVoiceChipText())

        // 3. Persisted request counters
        val today = progressManager.getTodayDateKey()
        progressManager.setDailyAzureRequestsForTesting(today, 5)
        assertEquals(5, progressManager.getDailyAzureRequestsCount())
        val incrementedAzure = progressManager.incrementDailyAzureRequestsCount()
        assertEquals(6, incrementedAzure)
        assertEquals(6, progressManager.getDailyAzureRequestsCount())

        progressManager.setDailyGeminiRequestsForTesting(today, 2)
        assertEquals(2, progressManager.getDailyGeminiRequestsCount())
        val incrementedGemini = progressManager.incrementDailyGeminiRequestsCount()
        assertEquals(3, incrementedGemini)
        assertEquals(3, progressManager.getDailyGeminiRequestsCount())
    }

    @Test
    fun `verify Fix P bilingual text parsing, XML escaping, and voice library provider keying`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ttsManager = com.example.util.TtsManager(context)

        // 1. Bilingual parsing: German first, Dari second
        val parsed1 = ttsManager.parseGermanAndDari("Guten Morgen", "صبح بخیر")
        assertEquals("Guten Morgen", parsed1.first)
        assertEquals("صبح بخیر", parsed1.second)

        val parsed2 = ttsManager.parseGermanAndDari("Hallo ⟦سلام⟧", null)
        assertEquals("Hallo", parsed2.first)
        assertEquals("سلام", parsed2.second)

        val parsed3 = ttsManager.parseGermanAndDari("Danke (تشکر)", null)
        assertEquals("Danke", parsed3.first)
        assertEquals("تشکر", parsed3.second)

        val parsed4 = ttsManager.parseGermanAndDari("Auf Wiedersehen | خداحافظ", null)
        assertEquals("Auf Wiedersehen", parsed4.first)
        assertEquals("خداحافظ", parsed4.second)

        val parsed5 = ttsManager.parseGermanAndDari("Tschüss", null)
        assertEquals("Tschüss", parsed5.first)
        org.junit.Assert.assertNull(parsed5.second)

        // 2. XML escaping for Azure SSML
        val rawXml = "<speak test=\"1\" & 'hello'>"
        val escaped = com.example.util.TtsManager.escapeXml(rawXml)
        assertEquals("&lt;speak test=&quot;1&quot; &amp; &apos;hello&apos;&gt;", escaped)

        // 3. Provider-aware voice library keying
        val keyAzure = com.example.util.TtsManager.getVoiceLibraryKey("Guten Tag", "de-DE-ConradNeural", "AZURE")
        val keyGemini = com.example.util.TtsManager.getVoiceLibraryKey("Guten Tag", "Charon", "GEMINI")
        val keyDefault = com.example.util.TtsManager.getVoiceLibraryKey("Guten Tag")

        assertTrue(keyAzure.isNotBlank())
        assertTrue(keyGemini.isNotBlank())
        assertTrue(keyDefault.isNotBlank())
        // Keys should be distinct hashes
        org.junit.Assert.assertNotEquals(keyAzure, keyGemini)

        // 4. Storing both WAV and MP3 files in voice library
        val libraryDir = com.example.util.TtsManager.getVoiceLibraryDir(context)
        val dummyMp3 = java.io.File(libraryDir, "$keyAzure.mp3")
        dummyMp3.writeBytes(ByteArray(256))

        val foundMp3 = ttsManager.findStoredAudioFile("Guten Tag", 1.0f, "de-DE-ConradNeural", "AZURE")
        org.junit.Assert.assertNotNull(foundMp3)
        assertEquals(dummyMp3.absolutePath, foundMp3?.absolutePath)

        dummyMp3.delete()
    }

    @Test
    fun `verify Phase Q dialogue maker, saved dialogues repository, offline audio caching, and muse removal`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.SavedDialoguesRepository.getInstance(context)
        val ttsManager = com.example.util.TtsManager(context)

        // 1. Verify Muse tab is absent and new tabs exist in NavDestination
        val navTags = com.example.ui.NavDestination.values().map { it.testTag }
        org.junit.Assert.assertFalse("MUSE must be removed", navTags.contains("nav_item_muse"))
        assertTrue("DIALOGUE_MAKER must be present", navTags.contains("nav_item_dialogue_maker"))
        assertTrue("SAVED_DIALOGUES must be present", navTags.contains("nav_item_saved_dialogues"))

        // 2. Data model: DialogueMakerLine serialization & deserialization
        val sampleLine1 = com.example.data.model.DialogueMakerLine(
            speaker = "A",
            isSpeakerA = true,
            german = "Hallo! Wie geht es dir?",
            pronunciation = "هالو! وی گیت اِس دیر؟",
            translationDari = "سلام! چطور هستی؟"
        )
        val sampleLine2 = com.example.data.model.DialogueMakerLine(
            speaker = "B",
            isSpeakerA = false,
            german = "Mir geht es gut, danke!",
            pronunciation = "میر گیت اِس گوت، دانکه!",
            translationDari = "من خوب هستم، تشکر!"
        )

        val jsonLine1 = sampleLine1.toJsonObject()
        val parsedLine1 = com.example.data.model.DialogueMakerLine.fromJsonObject(jsonLine1)
        assertEquals("Hallo! Wie geht es dir?", parsedLine1.german)
        assertTrue(parsedLine1.isSpeakerA)
        assertEquals("سلام! چطور هستی؟", parsedLine1.translationDari)

        // 3. SavedDialogue model & repository save/load/delete
        val dialogueId = "test_dialogue_q_1"
        val savedDialogue = com.example.data.model.SavedDialogue(
            id = dialogueId,
            topic = "در رستوران",
            createdAtFormatted = "2026/10/10 - 10:00",
            lines = listOf(sampleLine1, sampleLine2)
        )

        // Dummy audio files simulating generated audio for the dialogue
        val tempAudio0 = java.io.File.createTempFile("line_0_test", ".mp3", context.cacheDir).apply {
            writeBytes(ByteArray(512) { 1 })
        }
        val tempAudio1 = java.io.File.createTempFile("line_1_test", ".wav", context.cacheDir).apply {
            writeBytes(ByteArray(1024) { 2 })
        }

        kotlinx.coroutines.runBlocking {
            // Save dialogue with line audio files
            val saveResult = repo.saveDialogue(savedDialogue, mapOf(0 to tempAudio0, 1 to tempAudio1))
            assertTrue(saveResult.isSuccess)

            // Verify dialogue exists in repo
            val allSaved = repo.dialoguesFlow.value
            val found = allSaved.firstOrNull { it.id == dialogueId }
            assertNotNull(found)
            assertEquals("در رستوران", found?.topic)
            assertEquals(2, found?.lines?.size)

            // Verify audio files are stored in files/saved_dialogues/<id>/
            val audio0 = repo.findAudioForLine(dialogueId, 0)
            assertNotNull("Offline audio for line 0 must exist", audio0)
            assertEquals(512L, audio0?.length())

            val audio1 = repo.findAudioForLine(dialogueId, 1)
            assertNotNull("Offline audio for line 1 must exist", audio1)
            assertEquals(1024L, audio1?.length())

            // Line 2 has no audio
            org.junit.Assert.assertNull(repo.findAudioForLine(dialogueId, 2))

            // Delete dialogue
            val deleted = repo.deleteDialogue(dialogueId)
            assertTrue(deleted)
            org.junit.Assert.assertNull(repo.dialoguesFlow.value.firstOrNull { it.id == dialogueId })
            org.junit.Assert.assertNull(repo.findAudioForLine(dialogueId, 0))
        }

        tempAudio0.delete()
        tempAudio1.delete()
    }
}


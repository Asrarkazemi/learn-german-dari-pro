package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LessonData
import com.example.data.repository.BuiltInCourseData
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
}

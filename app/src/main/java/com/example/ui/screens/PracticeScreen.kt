package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseItem
import com.example.data.model.LessonData
import com.example.data.repository.GrammarAndQuestionsData
import com.example.ui.components.ExactExerciseQuestionView
import com.example.ui.theme.SuccessGreen

@Composable
fun PracticeScreen(
    allLessons: List<LessonData>,
    initialLessonId: String,
    onPlayAudio: (String, Boolean) -> Unit, // text, isSlow
    modifier: Modifier = Modifier
) {
    if (allLessons.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("هیچ درسی موجود نیست.")
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: تمرین دروس, 1: صرف افعال
    val context = androidx.compose.ui.platform.LocalContext.current
    val progressManager = remember { com.example.data.storage.UserProgressManager.getInstance(context) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        progressManager.recordStudyDay()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("practice_screen")
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("تمرین دروس (${allLessons.size} درس)", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("صرف افعال A1", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            LessonExercisesSection(
                allLessons = allLessons,
                initialLessonId = initialLessonId,
                onPlayAudio = onPlayAudio
            )
        } else {
            VerbConjugationSection(onPlayAudio = onPlayAudio)
        }
    }
}

@Composable
private fun LessonExercisesSection(
    allLessons: List<LessonData>,
    initialLessonId: String,
    onPlayAudio: (String, Boolean) -> Unit
) {
    var selectedLessonId by remember(initialLessonId, allLessons) {
        val found = allLessons.find { it.id == initialLessonId }
        mutableStateOf(found?.id ?: allLessons.first().id)
    }

    val currentLesson = allLessons.find { it.id == selectedLessonId } ?: allLessons.first()
    val exercises = currentLesson.exercises

    var currentExIndex by remember(selectedLessonId) { mutableIntStateOf(0) }
    var score by remember(selectedLessonId) { mutableIntStateOf(0) }
    var isLessonPracticeCompleted by remember(selectedLessonId) { mutableStateOf(false) }

    val safeIndex = if (exercises.isNotEmpty()) currentExIndex.coerceIn(0, exercises.size - 1) else 0
    val currentExercise = exercises.getOrNull(safeIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lesson Filter Carousel
        item {
            Text(
                text = "انتخاب درس برای تمرین:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allLessons) { lesson ->
                    FilterChip(
                        selected = selectedLessonId == lesson.id,
                        onClick = {
                            selectedLessonId = lesson.id
                            currentExIndex = 0
                            score = 0
                            isLessonPracticeCompleted = false
                        },
                        label = { Text("درس ${lesson.number}") }
                    )
                }
            }
        }

        if (isLessonPracticeCompleted) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(com.example.ui.theme.SuccessGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = com.example.ui.theme.SuccessGreen,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "تمرین درس ${currentLesson.number} به پایان رسید!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "شما به $score از ${exercises.size} سوال پاسخ درست دادید.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        androidx.compose.material3.Button(
                            onClick = {
                                currentExIndex = 0
                                score = 0
                                isLessonPracticeCompleted = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "شروع مجدد تمرین این درس",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        } else if (currentExercise != null) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                ExactExerciseQuestionView(
                    exercise = currentExercise,
                    questionNumber = safeIndex + 1,
                    totalQuestions = exercises.size,
                    onAnswerSelected = { isCorrect ->
                        if (isCorrect) score++
                    },
                    onNext = {
                        if (safeIndex < exercises.size - 1) {
                            currentExIndex = safeIndex + 1
                        } else {
                            isLessonPracticeCompleted = true
                        }
                    },
                    onPlayAudio = onPlayAudio,
                    nextButtonText = if (safeIndex < exercises.size - 1) "سوال بعدی" else "پایان و مشاهدهٔ نتیجه",
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("سوالی برای این درس تعریف نشده است.")
                }
            }
        }
    }
}

@Composable
private fun VerbConjugationSection(
    onPlayAudio: (String, Boolean) -> Unit
) {
    val verbs = GrammarAndQuestionsData.verbConjugations
    var selectedVerbIndex by remember { mutableIntStateOf(0) }
    val currentVerb = verbs[selectedVerbIndex]

    var drillPronounIndex by remember(selectedVerbIndex) { mutableIntStateOf(0) }
    val currentConjugation = currentVerb.conjugations[drillPronounIndex]

    // Construct 4 options (1 correct + 3 plausible)
    val options = remember(currentVerb, drillPronounIndex) {
        val correct = currentConjugation.conjugatedForm
        val otherForms = currentVerb.conjugations
            .map { it.conjugatedForm }
            .filter { it != correct }
            .distinct()
            .shuffled()
            .take(3)
        (otherForms + correct).shuffled()
    }

    val simulatedExercise = remember(currentVerb, drillPronounIndex, options) {
        ExerciseItem(
            type = "fill-blank",
            question = "${currentConjugation.pronounGerman} ___",
            pronunciation = "${currentConjugation.pronounGerman} ${currentConjugation.conjugatedForm}",
            translationDari = "${currentConjugation.pronounDari} (${currentVerb.meaningDari})",
            options = if (options.size == 4) options else listOf("bin", "bist", "ist", "sind"),
            correctAnswer = currentConjugation.conjugatedForm,
            explanationDari = "صرف فعل ${currentVerb.verbInfinitive} برای ضمیر ${currentConjugation.pronounGerman}: ${currentConjugation.conjugatedForm} (${currentVerb.tipDari})"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "انتخاب فعل برای تمرین صرف:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(verbs) { index, verb ->
                    FilterChip(
                        selected = selectedVerbIndex == index,
                        onClick = {
                            selectedVerbIndex = index
                            drillPronounIndex = 0
                        },
                        label = { Text(verb.verbInfinitive) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            ExactExerciseQuestionView(
                exercise = simulatedExercise,
                questionNumber = drillPronounIndex + 1,
                totalQuestions = currentVerb.conjugations.size,
                onAnswerSelected = { },
                onNext = {
                    drillPronounIndex = (drillPronounIndex + 1) % currentVerb.conjugations.size
                },
                onPlayAudio = onPlayAudio,
                nextButtonText = if (drillPronounIndex < currentVerb.conjugations.size - 1) "ضمیر بعدی" else "شروع مجدد صرف این فعل",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

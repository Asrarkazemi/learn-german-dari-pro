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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseItem
import com.example.data.model.LessonData
import com.example.ui.components.AudioSpeechButtons
import com.example.ui.components.ExactExerciseQuestionView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

data class QuizMistake(
    val exercise: ExerciseItem,
    val givenAnswer: String,
    val correctAnswer: String
)

@Composable
fun QuizScreen(
    allLessons: List<LessonData>,
    onSaveQuizScore: (Int, Int) -> Unit,
    onPlayAudio: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Gather all exercises from all lessons
    val allExercises = remember(allLessons) {
        val pool = allLessons.flatMap { it.exercises }
        if (pool.isNotEmpty()) pool else emptyList()
    }

    // 10 randomized questions
    var quizQuestions by remember(allExercises) {
        mutableStateOf(allExercises.shuffled().take(10))
    }

    var currentQIndex by remember { mutableIntStateOf(0) }
    var runningScore by remember { mutableIntStateOf(0) }
    val mistakesList = remember { mutableStateListOf<QuizMistake>() }
    var isQuizCompleted by remember { mutableStateOf(false) }

    fun restartQuiz() {
        quizQuestions = allExercises.shuffled().take(10)
        currentQIndex = 0
        runningScore = 0
        mistakesList.clear()
        isQuizCompleted = false
    }

    if (quizQuestions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("تمرینی برای آزمون یافت نشد. لطفاً درسی اضافه کنید.")
        }
        return
    }

    if (isQuizCompleted) {
        // Final Score Screen with Review of Mistakes
        QuizCompletedScreen(
            total = quizQuestions.size,
            score = runningScore,
            mistakes = mistakesList,
            onRestart = { restartQuiz() },
            onPlayAudio = onPlayAudio,
            modifier = modifier
        )
    } else {
        val currentExercise = quizQuestions.getOrNull(currentQIndex)
        val progress = (currentQIndex + 1).toFloat() / quizQuestions.size.toFloat()

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("quiz_screen_active"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Running Score and Progress Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آزمون جامع سطح A1",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "امتیاز زنده: $runningScore از ${currentQIndex}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Current Question Card with EXACT 4-option locked interaction
            if (currentExercise != null) {
                item {
                    var answeredThisCorrectly by remember(currentExercise) { mutableStateOf<Boolean?>(null) }

                    Spacer(modifier = Modifier.height(6.dp))
                    ExactExerciseQuestionView(
                        exercise = currentExercise,
                        questionNumber = currentQIndex + 1,
                        totalQuestions = quizQuestions.size,
                        onAnswerSelected = { isCorrect ->
                            answeredThisCorrectly = isCorrect
                            if (isCorrect) {
                                runningScore++
                            } else {
                                mistakesList.add(
                                    QuizMistake(
                                        exercise = currentExercise,
                                        givenAnswer = "پاسخ نادرست",
                                        correctAnswer = currentExercise.correctAnswer
                                    )
                                )
                            }
                        },
                        onNext = {
                            if (currentQIndex < quizQuestions.size - 1) {
                                currentQIndex++
                            } else {
                                isQuizCompleted = true
                                onSaveQuizScore(runningScore, quizQuestions.size)
                            }
                        },
                        onPlayAudio = onPlayAudio,
                        nextButtonText = if (currentQIndex < quizQuestions.size - 1) "سوال بعدی" else "مشاهده کارنامه نهایی",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizCompletedScreen(
    total: Int,
    score: Int,
    mistakes: List<QuizMistake>,
    onRestart: () -> Unit,
    onPlayAudio: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (score.toFloat() / total.toFloat() * 100).toInt() else 0

    val (titleEvaluation, feedbackDari) = when {
        percentage >= 90 -> Pair("فوق‌العاده عالی! 🌟", "تسلط شما بر واژگان و گرامر A1 بی‌نظیر است!")
        percentage >= 70 -> Pair("بسیار خوب! 👏", "پاسخ‌های خوبی دادید و بخش زیادی از درس‌ها را به خاطر دارید.")
        percentage >= 50 -> Pair("خوب، نیاز به تمرین بیشتر 👍", "با چند بار مرور دیگر فلش‌کارت‌ها به نمره کامل می‌رسید.")
        else -> Pair("تلاش مجدد لازم است 🌱", "پیشنهاد می‌کنیم درس‌ها و تمرین صرف افعال را دوباره مرور کنید.")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("quiz_completed_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Result Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(AccentAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = titleEvaluation,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = feedbackDari,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "نمره شما: $score از $total ($percentage٪)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("شروع آزمون جدید", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Mistakes Review Header
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (mistakes.isNotEmpty()) "بررسی اشتباهات (${mistakes.size})" else "بدون هیچ اشتباهی! آفرین!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Mistakes details with explanation and audio
        items(mistakes) { mistake ->
            val ex = mistake.exercise
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = ex.question,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AudioSpeechButtons(
                            textToSpeak = ex.question,
                            onPlayAudio = onPlayAudio,
                            size = 34
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "تلفظ: ${ex.pronunciation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "معنی: ${ex.translationDari}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "پاسخ صحیح: ${ex.correctAnswer}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "نکته: ${ex.explanationDari}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

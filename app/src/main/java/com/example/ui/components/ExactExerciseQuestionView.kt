package com.example.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseItem
import com.example.data.storage.UserProgressManager
import com.example.ui.consumeTaps
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ExactExerciseQuestionView(
    exercise: ExerciseItem,
    questionNumber: Int,
    totalQuestions: Int,
    onAnswerSelected: (Boolean) -> Unit, // isCorrect
    onNext: () -> Unit,
    onPlayAudio: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    nextButtonText: String = "بعدی"
) {
    // Exact interaction state: starts BLANK for each question
    var selectedOption by remember(exercise, questionNumber) { mutableStateOf<String?>(null) }
    val isAnswered = selectedOption != null

    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffsetX = remember { Animatable(0f) }
    val checkPopScale = remember { Animatable(0f) }
    val flashAlphaAnim = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }

    // Options (exactly 4)
    val options = remember(exercise) { exercise.options }

    // FIX B: On the last question it becomes «پایان و مشاهدهٔ نتیجه», otherwise «سؤال بعدی»
    val isLastQuestion = questionNumber >= totalQuestions
    val resolvedNextText = if (isLastQuestion) {
        "پایان و مشاهدهٔ نتیجه"
    } else if (nextButtonText != "بعدی" && nextButtonText.isNotBlank()) {
        nextButtonText
    } else {
        "سؤال بعدی"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exact_exercise_container")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .consumeTaps()
                .offset { IntOffset(shakeOffsetX.value.roundToInt(), 0) }
                .testTag("exact_exercise_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (flashAlphaAnim.value > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(flashColor.copy(alpha = flashAlphaAnim.value))
                    )
                }

                Column(modifier = Modifier.padding(14.dp)) {
                // Header: question counter chip & audio buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "سوال $questionNumber از $totalQuestions",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Audio buttons in their own non-overlapping area
                    AudioSpeechButtons(
                        textToSpeak = exercise.question,
                        translationDari = exercise.translationDari,
                        onPlayAudio = onPlayAudio,
                        size = 36
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Question Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Question text (German in LTR)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = exercise.question,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        if (exercise.pronunciation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "تلفظ: ${exercise.pronunciation}",
                                fontSize = 12.5.sp,
                                color = AccentAmber,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (exercise.translationDari.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "معنی: ${exercise.translationDari}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Exactly FOUR option buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEachIndexed { index, optionText ->
                        val isThisCorrect = optionText == exercise.correctAnswer
                        val isThisSelected = selectedOption == optionText

                        val backgroundColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.surface
                            isThisCorrect -> SuccessGreen.copy(alpha = 0.16f)
                            isThisSelected -> ErrorRed.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        }

                        val borderColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.outlineVariant
                            isThisCorrect -> SuccessGreen
                            isThisSelected -> ErrorRed
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }

                        val textColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.onSurface
                            isThisCorrect -> SuccessGreen
                            isThisSelected -> ErrorRed
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("exercise_option_$index")
                                .clickable(enabled = !isAnswered) {
                                    selectedOption = optionText
                                    val isCorrect = optionText == exercise.correctAnswer
                                    progressManager.recordStudyDay()
                                    onAnswerSelected(isCorrect)

                                    if (isCorrect) {
                                        coroutineScope.launch {
                                            flashColor = Color(0xFF10B981)
                                            flashAlphaAnim.snapTo(0.25f)
                                            launch { flashAlphaAnim.animateTo(0f, tween(550)) }
                                            checkPopScale.snapTo(0f)
                                            checkPopScale.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            flashColor = Color(0xFFEF4444)
                                            flashAlphaAnim.snapTo(0.22f)
                                            launch { flashAlphaAnim.animateTo(0f, tween(550)) }
                                            shakeOffsetX.snapTo(0f)
                                            for (dx in listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)) {
                                                shakeOffsetX.animateTo(dx, tween(35))
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                            border = BorderStroke(1.6.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = optionText,
                                    fontSize = 15.sp,
                                    fontWeight = if (isAnswered && (isThisCorrect || isThisSelected)) FontWeight.Bold else FontWeight.SemiBold,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isAnswered) {
                                    if (isThisCorrect) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "درست است! ✅",
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "درست",
                                                tint = SuccessGreen,
                                                modifier = Modifier
                                                    .size(19.dp)
                                                    .scale(if (selectedOption == exercise.correctAnswer) checkPopScale.value.coerceAtLeast(0.7f) else 1f)
                                            )
                                        }
                                    } else if (isThisSelected) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "اشتباه است ❌",
                                                color = ErrorRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "نادرست",
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Dari Explanation Box
                AnimatedVisibility(visible = isAnswered) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "توضیح: ${exercise.explanationDari}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

        // FIX B: Sticky/Prominent Action Bar ALWAYS VISIBLE when answered without scrolling
        AnimatedVisibility(visible = isAnswered) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Next Question / Finish Button
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("exercise_next_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isLastQuestion) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = resolvedNextText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    // FIX B: Small «تلاش دوباره» button that resets JUST the current question
                    OutlinedButton(
                        onClick = {
                            selectedOption = null
                            coroutineScope.launch {
                                shakeOffsetX.snapTo(0f)
                                checkPopScale.snapTo(0f)
                                flashAlphaAnim.snapTo(0f)
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("exercise_retry_question_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تلاش دوباره",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تلاش دوباره",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExerciseItem
import com.example.data.model.GermanNumber
import com.example.data.repository.GermanCourseData
import com.example.ui.components.AudioSpeechButtons
import com.example.ui.components.ExactExerciseQuestionView
import com.example.ui.theme.AccentAmber

@Composable
fun NumbersScreen(
    onPlayAudio: (String, Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: آموزش اعداد, 1: آزمون اعداد
    val allNumbers = remember { GermanCourseData.getNumbersList() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("numbers_screen")
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت"
                    )
                }
                Text(
                    text = "آموزش و تمرین اعداد (۰ تا ۱۰۰)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("فهرست و تلفظ اعداد", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("آزمون ۴ گزینه‌ای اعداد", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            NumbersLearningSection(allNumbers = allNumbers, onPlayAudio = onPlayAudio)
        } else {
            NumbersExactQuizSection(onPlayAudio = onPlayAudio)
        }
    }
}

@Composable
private fun NumbersLearningSection(
    allNumbers: List<GermanNumber>,
    onPlayAudio: (String, Boolean) -> Unit
) {
    var categoryFilter by remember { mutableIntStateOf(0) } // 0: همه, 1: ۰-۱۰, 2: ۱۱-۲۰, 3: ۲۱-۱۰۰

    val filteredList = remember(categoryFilter, allNumbers) {
        when (categoryFilter) {
            1 -> allNumbers.filter { it.value in 0..10 }
            2 -> allNumbers.filter { it.value in 11..20 }
            3 -> allNumbers.filter { it.value > 20 }
            else -> allNumbers
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Golden Rule Explanation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نکته طلایی برای یادگیری اعداد آلمانی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "در زبان آلمانی برای اعداد ۲۱ تا ۹۹، دقیقاً مانند زبان فارسی/دری اول عدد یکان و سپس دهگان خوانده می‌شود:\n" +
                                "• عدد ۲۱: ein + und + zwanzig = einundzwanzig (یک و بیست)\n" +
                                "• عدد ۲۵: fünf + und + zwanzig = fünfundzwanzig (پنج و بیست)\n" +
                                "• عدد ۳۵: fünf + und + dreißig = fünfunddreißig (پنج و سی)",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = categoryFilter == 0, onClick = { categoryFilter = 0 }, label = { Text("همه") })
                FilterChip(selected = categoryFilter == 1, onClick = { categoryFilter = 1 }, label = { Text("۰ تا ۱۰") })
                FilterChip(selected = categoryFilter == 2, onClick = { categoryFilter = 2 }, label = { Text("۱۱ تا ۲۰") })
                FilterChip(selected = categoryFilter == 3, onClick = { categoryFilter = 3 }, label = { Text("۲۱ تا ۱۰۰") })
            }
        }

        items(filteredList, key = { it.value }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.value} (${item.dariDigits})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = item.germanWord,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "تلفظ: ${item.persianPronunciation}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.size(86.dp, 36.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AudioSpeechButtons(
                            textToSpeak = item.germanWord,
                            onPlayAudio = onPlayAudio,
                            size = 36
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumbersExactQuizSection(
    onPlayAudio: (String, Boolean) -> Unit
) {
    var questionCounter by remember { mutableIntStateOf(1) }
    var targetNumber by remember { mutableIntStateOf((0..100).random()) }

    val currentGerman = GermanCourseData.numberToGerman(targetNumber)

    // Generate exactly 4 options
    val options = remember(targetNumber) {
        val wrongChoices = mutableListOf<Int>()
        while (wrongChoices.size < 3) {
            val rand = (0..100).random()
            if (rand != targetNumber && !wrongChoices.contains(rand)) {
                wrongChoices.add(rand)
            }
        }
        (wrongChoices + targetNumber).shuffled().map { "$it" }
    }

    val simulatedExercise = remember(targetNumber, options) {
        ExerciseItem(
            type = "multiple-choice",
            question = currentGerman,
            pronunciation = currentGerman,
            translationDari = "عدد $targetNumber در زبان آلمانی",
            options = options,
            correctAnswer = "$targetNumber",
            explanationDari = "واژه $currentGerman در زبان آلمانی نشان‌دهنده عدد $targetNumber است."
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ExactExerciseQuestionView(
                exercise = simulatedExercise,
                questionNumber = questionCounter,
                totalQuestions = 10,
                onAnswerSelected = { },
                onNext = {
                    questionCounter++
                    targetNumber = (0..100).random()
                },
                onPlayAudio = onPlayAudio,
                nextButtonText = "عدد بعدی"
            )
        }
    }
}

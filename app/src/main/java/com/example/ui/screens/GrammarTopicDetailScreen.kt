package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.GrammarTopic
import com.example.ui.LocalImmersiveUiState
import com.example.ui.consumeTaps
import com.example.ui.immersiveTapToggle
import com.example.ui.components.AttachedSoundsSection
import com.example.ui.components.AudioSpeechButtons
import com.example.ui.components.ExactExerciseQuestionView
import com.example.ui.components.GrammarSectionCard
import com.example.ui.components.RuntimeExercisesSection
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.SuccessGreen

@Composable
fun GrammarTopicDetailScreen(
    topic: GrammarTopic,
    onBack: () -> Unit,
    onPlayAudio: (String, Boolean) -> Unit,
    ttsManager: com.example.util.TtsManager? = null,
    initialSectionIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var currentExerciseIndex by remember(topic.id) { mutableIntStateOf(0) }
    var exerciseScore by remember(topic.id) { mutableIntStateOf(0) }
    var isExerciseCompleted by remember(topic.id) { mutableStateOf(false) }

    var isStepByStepMode by remember(topic.id, initialSectionIndex) {
        mutableStateOf(initialSectionIndex > 0 && topic.sections.size > 1)
    }
    var currentStepIndex by remember(topic.id, initialSectionIndex) {
        mutableIntStateOf(if (topic.sections.isNotEmpty()) initialSectionIndex.coerceIn(0, topic.sections.size - 1) else 0)
    }

    var selectedSection by remember(topic.id, initialSectionIndex) {
        mutableIntStateOf(if (initialSectionIndex == 4) 1 else 0)
    }

    val immersiveState = LocalImmersiveUiState.current

    val context = LocalContext.current
    val progressManager = remember { com.example.data.storage.UserProgressManager.getInstance(context) }
    val effectiveTts = ttsManager ?: remember { com.example.util.TtsManager(context) }
    val isSequentialPlaying by effectiveTts.isSequentialPlayingFlow.collectAsState()

    LaunchedEffect(topic.id, isStepByStepMode, currentStepIndex, selectedSection) {
        progressManager.recordStudyDay()
        val currentTabTitle = if (selectedSection == 1) "ساوندها" else (if (isStepByStepMode) "بخش ${currentStepIndex + 1}" else "توضیحات و قواعد")
        val sectionIdx = if (selectedSection == 1) 4 else (if (isStepByStepMode) currentStepIndex else 0)
        progressManager.saveReadingPosition(
            libraryId = com.example.data.storage.UserProgressManager.LIBRARY_GRAMMAR,
            itemNumber = topic.number,
            itemId = topic.id,
            currentTab = currentTabTitle,
            currentSectionIndex = sectionIdx,
            displayTitle = "گرامر ${topic.number}: ${topic.titleDari}"
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Topic Overview Header Banner
            AnimatedVisibility(visible = immersiveState.isUiVisible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .consumeTaps(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${topic.number}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topic.titleDari,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = topic.titleGerman,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_grammar_topic")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Tabs: 0: «توضیحات و تمرین‌ها», 1: «🎧 ساوندها»
            TabRow(
                selectedTabIndex = selectedSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = {
                        Text(
                            text = "توضیحات و تمرین‌ها",
                            fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.5.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_grammar_content")
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = {
                        Text(
                            text = "🎧 ساوندها",
                            fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.5.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_grammar_sounds")
                )
            }

            if (selectedSection == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .immersiveTapToggle(immersiveState)
                        .testTag("grammar_detail_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // FIX O (4a): Sequential Audio playback for Grammar Topic
                    item {
                        FilledTonalButton(
                            onClick = {
                            if (isSequentialPlaying) {
                                effectiveTts.stopSequentialPlayback()
                            } else {
                                val lines = mutableListOf<String>()
                                if (isStepByStepMode && topic.sections.isNotEmpty()) {
                                    val safeStep = currentStepIndex.coerceIn(0, topic.sections.size - 1)
                                    lines.addAll(com.example.util.TtsManager.extractGermanLinesFromSectionBody(topic.sections[safeStep].bodyDari))
                                } else {
                                    for (sec in topic.sections) {
                                        lines.addAll(com.example.util.TtsManager.extractGermanLinesFromSectionBody(sec.bodyDari))
                                    }
                                    for (ex in topic.exampleSentences) {
                                        val clean = com.example.util.TtsManager.cleanGermanText(ex.german)
                                        if (clean.isNotBlank()) lines.add(clean)
                                    }
                                }
                                val speed = progressManager.getPlaybackSpeed()
                                effectiveTts.startSequentialPlayback(lines, speed)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_grammar_full_text_audio"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSequentialPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isSequentialPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isSequentialPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSequentialPlaying) "⏹ توقف پخش" else "▶ پخش صدای متن گرامر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Section 1: Explanation Sections with Step-by-Step Toggle
                item(key = "header_grammar_sections_${topic.id}") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(5f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "توضیحات و قواعد گرامر",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (topic.sections.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "مرور قدم‌به‌قدم",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isStepByStepMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = isStepByStepMode,
                                            onCheckedChange = {
                                                isStepByStepMode = it
                                                currentStepIndex = 0
                                            },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isStepByStepMode && topic.sections.isNotEmpty()) {
                    val safeStep = currentStepIndex.coerceIn(0, topic.sections.size - 1)
                    val section = topic.sections[safeStep]

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = { if (currentStepIndex > 0) currentStepIndex-- },
                                enabled = currentStepIndex > 0,
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "قبلی")
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "بخش ${safeStep + 1} از ${topic.sections.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = { if (currentStepIndex < topic.sections.size - 1) currentStepIndex++ },
                                enabled = currentStepIndex < topic.sections.size - 1,
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "بعدی")
                            }
                        }
                    }

                    item {
                        GrammarSectionCard(
                            sectionNumber = safeStep + 1,
                            title = section.title,
                            bodyDari = section.bodyDari,
                            onPlayAudio = onPlayAudio,
                            extraDistractorWords = emptyList(),
                            modifier = Modifier.testTag("grammar_section_card_$safeStep")
                        )
                    }
                } else {
                    // Render all explanation sections
                    items(topic.sections.size, key = { "sec_${topic.id}_$it" }) { index ->
                        val section = topic.sections[index]
                        GrammarSectionCard(
                            sectionNumber = index + 1,
                            title = section.title,
                            bodyDari = section.bodyDari,
                            onPlayAudio = onPlayAudio,
                            extraDistractorWords = emptyList(),
                            modifier = Modifier.testTag("grammar_section_card_$index")
                        )
                    }
                }

                // Section 2: Example Sentences (Numbered, Line-by-Line with Audio)
                if (topic.exampleSentences.isNotEmpty()) {
                    item(key = "header_grammar_examples_${topic.id}") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(5f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جملات نمونه و کاربردی (${topic.exampleSentences.size} جمله)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(topic.exampleSentences.size, key = { "ex_sent_${topic.id}_$it" }) { index ->
                        val sentence = topic.exampleSentences[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .consumeTaps()
                                .testTag("grammar_sentence_card_$index"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header row: Number chip + German sentence with weight(1f) + Audio buttons in dedicated column
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "جمله ${index + 1}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                Text(
                                                    text = sentence.german,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.5.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Box(
                                        modifier = Modifier.size(86.dp, 36.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        AudioSpeechButtons(
                                            textToSpeak = sentence.german,
                                            translationDari = sentence.meaningDari,
                                            onPlayAudio = onPlayAudio,
                                            size = 36
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Pronunciation in Persian script
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تلفظ: ",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = sentence.pronunciation,
                                        fontSize = 13.5.sp,
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Dari Meaning
                                Text(
                                    text = sentence.meaningDari,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                // Section 3: Exercises in 4-option ✓/✗ format (ExactExerciseQuestionView)
                if (topic.exercises.isNotEmpty()) {
                    item(key = "header_grammar_exercises_${topic.id}") {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(5f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تمرین‌های تثبیت گرامر (${topic.exercises.size} سوال)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        if (!isExerciseCompleted && currentExerciseIndex < topic.exercises.size) {
                            val exercise = topic.exercises[currentExerciseIndex]
                            ExactExerciseQuestionView(
                                exercise = exercise,
                                questionNumber = currentExerciseIndex + 1,
                                totalQuestions = topic.exercises.size,
                                onAnswerSelected = { isCorrect ->
                                    if (isCorrect) exerciseScore++
                                },
                                onNext = {
                                    if (currentExerciseIndex + 1 < topic.exercises.size) {
                                        currentExerciseIndex++
                                    } else {
                                        isExerciseCompleted = true
                                    }
                                },
                                onPlayAudio = onPlayAudio,
                                nextButtonText = if (currentExerciseIndex + 1 < topic.exercises.size) "سوال بعدی" else "مشاهده نتیجه تمرین"
                            )
                        } else {
                            // Completed card with restart
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .consumeTaps()
                                    .testTag("grammar_exercise_completed_card"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "آفرین! تمرین این مبحث را به پایان رساندید",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "نمره شما: $exerciseScore از ${topic.exercises.size}",
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = {
                                            currentExerciseIndex = 0
                                            exerciseScore = 0
                                            isExerciseCompleted = false
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تمرین مجدد این مبحث", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Runtime generated exercises «تمرین‌های تازه» (Cloze, Word order, Matching)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RuntimeExercisesSection(
                        exampleSentences = topic.exampleSentences,
                        vocabulary = emptyList(),
                        onPlayAudio = onPlayAudio
                    )
                }

                // Sound Section Link at end of topic
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .consumeTaps(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        onClick = { selectedSection = 1 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "🎧 ساوندها (فایل‌های صوتی این مبحث)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AttachedSoundsSection(
                    library = com.example.data.storage.UserProgressManager.LIBRARY_GRAMMAR,
                    itemNumber = topic.number,
                    itemTitle = topic.titleDari
                )
            }
        }
    }
}
}


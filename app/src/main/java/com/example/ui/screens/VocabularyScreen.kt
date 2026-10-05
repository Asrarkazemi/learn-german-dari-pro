package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ArticleType
import com.example.data.model.LessonData
import com.example.data.model.VocabularyItem
import com.example.ui.components.ArticleBadge
import com.example.ui.components.AudioSpeechButtons
import com.example.ui.components.ExactExerciseQuestionView
import com.example.ui.components.FlipFlashcard
import com.example.ui.components.GrammarSectionCard
import com.example.ui.theme.SuccessGreen

enum class VocabFilter {
    ALL,
    NEED_REVIEW,
    LEARNED
}

@Composable
fun VocabularyScreen(
    allLessons: List<LessonData>,
    initialLessonId: String,
    learnedWords: Set<String>,
    onToggleLearned: (String, Boolean) -> Unit,
    onPlayAudio: (String, Boolean) -> Unit, // text, isSlow
    initialSection: Int = 0,
    modifier: Modifier = Modifier
) {
    if (allLessons.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("هیچ درسی موجود نیست.")
        }
        return
    }

    var selectedLessonId by remember(initialLessonId, allLessons) {
        val found = allLessons.find { it.id == initialLessonId }
        mutableStateOf(found?.id ?: allLessons.first().id)
    }

    // 4 sections: 0: «فلشکارتها», 1: «متن کامل درس», 2: «پرسش و پاسخ», 3: «تمرینها»
    var selectedSection by remember(selectedLessonId, initialSection) {
        mutableIntStateOf(initialSection.coerceIn(0, 3))
    }

    val currentLesson: LessonData = allLessons.find { it.id == selectedLessonId } ?: allLessons.first()
    val lessonIndex = allLessons.indexOf(currentLesson).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("vocabulary_screen")
    ) {
        // Dynamic Lesson Selector Tabs
        ScrollableTabRow(
            selectedTabIndex = lessonIndex,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            allLessons.forEach { lesson ->
                Tab(
                    selected = selectedLessonId == lesson.id,
                    onClick = {
                        selectedLessonId = lesson.id
                    },
                    text = {
                        Text(
                            text = "درس ${lesson.number}",
                            fontWeight = if (selectedLessonId == lesson.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Subheader: Lesson Title
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = currentLesson.titleDari,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = currentLesson.titleGerman,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // CHANGE 1: Restructure each lesson screen into 4 clear sections
        // (a) «فلشکارتها»
        // (b) «متن کامل درس»
        // (c) «پرسش و پاسخ»
        // (d) «تمرینها»
        TabRow(
            selectedTabIndex = selectedSection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = {
                    Text(
                        text = "فلشکارتها",
                        fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_lesson_flashcards")
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = {
                    Text(
                        text = "متن کامل درس",
                        fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_lesson_full_reading")
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                text = {
                    Text(
                        text = "پرسش و پاسخ",
                        fontWeight = if (selectedSection == 2) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_lesson_qa")
            )
            Tab(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3 },
                text = {
                    Text(
                        text = "تمرینها",
                        fontWeight = if (selectedSection == 3) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_lesson_exercises")
            )
        }

        // Content of the active section
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedSection) {
                0 -> FlashcardsSection(
                    currentLesson = currentLesson,
                    learnedWords = learnedWords,
                    onToggleLearned = onToggleLearned,
                    onPlayAudio = onPlayAudio
                )
                1 -> FullReadingSection(
                    currentLesson = currentLesson,
                    onPlayAudio = onPlayAudio
                )
                2 -> QaPairsSection(
                    currentLesson = currentLesson,
                    onPlayAudio = onPlayAudio
                )
                3 -> LessonExercisesTab(
                    currentLesson = currentLesson,
                    onPlayAudio = onPlayAudio
                )
            }
        }
    }
}

// ================= SECTION (a) «فلشکارتها» =================
@Composable
private fun FlashcardsSection(
    currentLesson: LessonData,
    learnedWords: Set<String>,
    onToggleLearned: (String, Boolean) -> Unit,
    onPlayAudio: (String, Boolean) -> Unit
) {
    val lessonWords: List<VocabularyItem> = currentLesson.vocabulary
    var activeFilter by remember { mutableStateOf(VocabFilter.ALL) }
    var currentIndex by remember(currentLesson.id, activeFilter) { mutableIntStateOf(0) }
    var isFlipped by remember(currentLesson.id, currentIndex) { mutableStateOf(false) }
    var isListViewMode by remember { mutableStateOf(false) }

    val filteredWords: List<VocabularyItem> = remember(lessonWords, activeFilter, learnedWords, currentLesson.id) {
        when (activeFilter) {
            VocabFilter.ALL -> lessonWords
            VocabFilter.NEED_REVIEW -> lessonWords.filter { !learnedWords.contains("${currentLesson.id}_${it.word}") }
            VocabFilter.LEARNED -> lessonWords.filter { learnedWords.contains("${currentLesson.id}_${it.word}") }
        }
    }

    val safeIndex = if (filteredWords.isNotEmpty()) currentIndex.coerceIn(0, filteredWords.size - 1) else 0
    val currentWord: VocabularyItem? = filteredWords.getOrNull(safeIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips and Toggle Mode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                FilterChip(
                    selected = activeFilter == VocabFilter.ALL,
                    onClick = {
                        activeFilter = VocabFilter.ALL
                        currentIndex = 0
                    },
                    label = { Text("همه (${lessonWords.size})", fontSize = 12.sp) }
                )

                val unlearnedCount = lessonWords.count { !learnedWords.contains("${currentLesson.id}_${it.word}") }
                FilterChip(
                    selected = activeFilter == VocabFilter.NEED_REVIEW,
                    onClick = {
                        activeFilter = VocabFilter.NEED_REVIEW
                        currentIndex = 0
                    },
                    label = { Text("مرور ($unlearnedCount)", fontSize = 12.sp) }
                )

                val learnedCount = lessonWords.count { learnedWords.contains("${currentLesson.id}_${it.word}") }
                FilterChip(
                    selected = activeFilter == VocabFilter.LEARNED,
                    onClick = {
                        activeFilter = VocabFilter.LEARNED
                        currentIndex = 0
                    },
                    label = { Text("یاد گرفته ($learnedCount)", fontSize = 12.sp) }
                )
            }

            IconButton(
                onClick = { isListViewMode = !isListViewMode },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isListViewMode) Icons.Default.ViewAgenda else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "تغییر حالت نمایش",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isListViewMode) {
            // List View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWords, key = { it.word }) { wordItem ->
                    val wordKey = "${currentLesson.id}_${wordItem.word}"
                    val isLearned = learnedWords.contains(wordKey)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (wordItem.article.isNotBlank()) {
                                        val artType = when (wordItem.article.lowercase()) {
                                            "der" -> ArticleType.DER
                                            "die" -> ArticleType.DIE
                                            "das" -> ArticleType.DAS
                                            else -> ArticleType.NONE
                                        }
                                        ArticleBadge(article = artType, showGenderLabel = false)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = wordItem.word,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier.size(80.dp, 34.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        AudioSpeechButtons(
                                            textToSpeak = if (wordItem.article.isNotBlank()) "${wordItem.article} ${wordItem.word}" else wordItem.word,
                                            onPlayAudio = onPlayAudio,
                                            size = 34
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تلفظ: ${wordItem.pronunciationPersianScript}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = wordItem.meaningDari,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { onToggleLearned(wordKey, !isLearned) },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLearned) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isLearned) SuccessGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Flashcard Mode
            if (currentWord != null) {
                val wordKey = "${currentLesson.id}_${currentWord.word}"
                val isLearned = learnedWords.contains(wordKey)
                val example = currentLesson.exampleSentences.getOrNull(safeIndex % currentLesson.exampleSentences.size.coerceAtLeast(1))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "واژه ${safeIndex + 1} از ${filteredWords.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                if (filteredWords.size > 1) {
                                    var newIdx: Int
                                    do {
                                        newIdx = (0 until filteredWords.size).random()
                                    } while (newIdx == safeIndex)
                                    currentIndex = newIdx
                                    isFlipped = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "تصادفی",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FlipFlashcard(
                        item = currentWord,
                        example = example,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        isLearned = isLearned,
                        onMarkLearned = { learned -> onToggleLearned(wordKey, learned) },
                        onPlayAudio = onPlayAudio,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Previous / Next Nav
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (safeIndex > 0) {
                                    currentIndex = safeIndex - 1
                                    isFlipped = false
                                }
                            },
                            enabled = safeIndex > 0,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "قبلی",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "${safeIndex + 1} / ${filteredWords.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FilledIconButton(
                            onClick = {
                                if (safeIndex < filteredWords.size - 1) {
                                    currentIndex = safeIndex + 1
                                    isFlipped = false
                                }
                            },
                            enabled = safeIndex < filteredWords.size - 1,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "بعدی",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ واژه‌ای در این دسته یافت نشد.")
                }
            }
        }
    }
}

// ================= SECTION (b) «متن کامل درس» =================
@Composable
private fun FullReadingSection(
    currentLesson: LessonData,
    onPlayAudio: (String, Boolean) -> Unit
) {
    val hasGrammar = currentLesson.grammarSections.isNotEmpty()
    val hasExamples = currentLesson.exampleSentences.isNotEmpty()
    val hasDialogues = currentLesson.dialogues.isNotEmpty()

    if (!hasGrammar && !hasExamples && !hasDialogues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "متن کامل، گرامر یا مکالمه‌ای برای این درس ثبت نشده است.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_reading_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIRST: Grammar Sections
        if (hasGrammar) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دستور زبان و نکات گرامری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            itemsIndexed(currentLesson.grammarSections) { index, grammarSection ->
                GrammarSectionCard(
                    sectionNumber = index + 1,
                    title = grammarSection.title,
                    bodyDari = grammarSection.bodyDari
                )
            }
        }

        // THEN: Example Sentences (numbered, one per row with audio buttons normal + slow)
        if (hasExamples) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جملات نمونه (${currentLesson.exampleSentences.size} جمله)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            itemsIndexed(currentLesson.exampleSentences) { index, sentence ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 13.sp,
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
                                            fontSize = 16.sp,
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
                                    onPlayAudio = onPlayAudio,
                                    size = 36
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "تلفظ: ${sentence.pronunciation}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "معنی: ${sentence.meaningDari}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // THEN: Dialogues (line by line with speaker, German, pronunciation, meaning, and audio buttons)
        if (hasDialogues) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مکالمه و گفتگوی درس",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            currentLesson.dialogues.forEach { dialogue ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            dialogue.lines.forEachIndexed { lineIdx, line ->
                                Column(modifier = Modifier.fillMaxWidth()) {
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
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = line.speaker,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        text = line.german,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
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
                                                textToSpeak = line.german,
                                                onPlayAudio = onPlayAudio,
                                                size = 34
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تلفظ: ${line.pronunciation}",
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "معنی: ${line.meaningDari}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (lineIdx < dialogue.lines.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= SECTION (c) «پرسش و پاسخ» =================
@Composable
private fun QaPairsSection(
    currentLesson: LessonData,
    onPlayAudio: (String, Boolean) -> Unit
) {
    if (currentLesson.qaPairs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "پرسش و پاسخی برای این درس ثبت نشده است.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qa_pairs_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پرسش و پاسخ‌های کلیدی (${currentLesson.qaPairs.size} جفت)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        itemsIndexed(currentLesson.qaPairs) { idx, qa ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // QUESTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "سوال ${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = qa.questionGerman,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
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
                                textToSpeak = qa.questionGerman,
                                onPlayAudio = onPlayAudio,
                                size = 34
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تلفظ: ${qa.questionPronunciation}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "معنی: ${qa.questionDari}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ANSWER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "پاسخ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = qa.answerGerman,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary,
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
                                textToSpeak = qa.answerGerman,
                                onPlayAudio = onPlayAudio,
                                size = 34
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تلفظ: ${qa.answerPronunciation}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "معنی: ${qa.answerDari}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ================= SECTION (d) «تمرینها» =================
@Composable
private fun LessonExercisesTab(
    currentLesson: LessonData,
    onPlayAudio: (String, Boolean) -> Unit
) {
    val exercises = currentLesson.exercises

    if (exercises.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "تمرینی برای این درس ثبت نشده است.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var currentExIndex by remember(currentLesson.id) { mutableIntStateOf(0) }
    var score by remember(currentLesson.id) { mutableIntStateOf(0) }
    var isFinished by remember(currentLesson.id) { mutableStateOf(false) }

    val safeIndex = currentExIndex.coerceIn(0, exercises.size - 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isFinished) {
            Spacer(modifier = Modifier.height(6.dp))
            ExactExerciseQuestionView(
                exercise = exercises[safeIndex],
                questionNumber = safeIndex + 1,
                totalQuestions = exercises.size,
                onAnswerSelected = { isCorrect ->
                    if (isCorrect) score++
                },
                onNext = {
                    if (safeIndex < exercises.size - 1) {
                        currentExIndex = safeIndex + 1
                    } else {
                        isFinished = true
                    }
                },
                onPlayAudio = onPlayAudio,
                nextButtonText = if (safeIndex < exercises.size - 1) "سوال بعدی" else "مشاهده نتیجه",
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
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
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "تمرین این درس پایان یافت!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "شما به $score از ${exercises.size} سوال پاسخ درست دادید.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            currentExIndex = 0
                            score = 0
                            isFinished = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "شروع مجدد تمرین",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GrammarBookRepository
import com.example.data.repository.GrammarRepository
import com.example.data.repository.UnifiedCourseRepository
import com.example.data.storage.UserProgressManager
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.screens.DialoguesScreen
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.GrammarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MuseScreen
import com.example.ui.screens.NumbersScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.IndigoPrimary
import com.example.util.TtsManager

enum class NavDestination(
    val titleDari: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("خانه", Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
    GRAMMAR("گرامر", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_item_grammar"),
    VOCABULARY("لغات", Icons.Filled.Book, Icons.Outlined.Book, "nav_item_vocab"),
    PRACTICE("تمرین", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_item_practice"),
    QUIZ("آزمون", Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_item_quiz"),
    DIALOGUES("گفتگو", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "nav_item_dialogues"),
    GEMINI_CHAT("چت جیمنای", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "nav_item_gemini_chat"),
    MUSE("موس", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_item_muse")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val courseRepository = remember { UnifiedCourseRepository.getInstance(context) }
    val grammarRepository = remember { GrammarRepository.getInstance(context) }
    val grammarBookRepository = remember { GrammarBookRepository.getInstance(context) }

    val allLessons by courseRepository.lessonsFlow.collectAsState()
    val overrideNumbers by courseRepository.overrideNumbersFlow.collectAsState()
    val allGrammarTopics by grammarRepository.grammarTopicsFlow.collectAsState()
    val overrideGrammarNumbers by grammarRepository.overrideNumbersFlow.collectAsState()
    val grammarBookLessons by grammarBookRepository.lessonsFlow.collectAsState()
    val learnedWords by progressManager.learnedWordsFlow.collectAsState()
    val quizHighScore by progressManager.quizHighScoreFlow.collectAsState()
    val savedGeminiKey by progressManager.geminiApiKeyFlow.collectAsState()

    var currentTab by remember { mutableStateOf(NavDestination.HOME) }
    var activeLessonIsGrammarBook by remember { mutableStateOf(false) }
    var selectedLessonIdForVocab by remember { mutableStateOf("lesson_1") }
    var selectedLessonSection by remember { mutableIntStateOf(0) }
    var selectedLessonIdForPractice by remember { mutableStateOf("lesson_1") }
    var isNumbersScreenOpen by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }

    var isUiVisible by remember { mutableStateOf(true) }
    val immersiveUiState = remember(isUiVisible) {
        ImmersiveUiState(
            isUiVisible = isUiVisible,
            toggleUiVisibility = { isUiVisible = !isUiVisible },
            setUiVisible = { isUiVisible = it }
        )
    }

    val effectiveKey = progressManager.getEffectiveGeminiApiKey()
    val isTtsCooldownActive by progressManager.ttsCooldownActiveFlow.collectAsState()
    val isGeminiVoiceActive = effectiveKey.isNotEmpty() && !isTtsCooldownActive

    // Always RTL layout for Dari UI
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalImmersiveUiState provides immersiveUiState
    ) {
        if (isNumbersScreenOpen) {
            BackHandler {
                isNumbersScreenOpen = false
                isUiVisible = true
            }
        } else if (currentTab != NavDestination.HOME) {
            BackHandler {
                currentTab = NavDestination.HOME
                isUiVisible = true
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = isUiVisible,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = if (isNumbersScreenOpen) "تمرین اعداد آلمانی" else "آلمانی بیاموز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        navigationIcon = {
                            // FIX A (2): Visible voice status in app header - clickable to open voice settings dialog
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isGeminiVoiceActive) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showVoiceSettingsDialog = true }
                                    .testTag("top_bar_voice_status_chip")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = if (isGeminiVoiceActive) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isGeminiVoiceActive) "صدا: جیمنای ✨" else "صدا: گوشی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGeminiVoiceActive) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isNumbersScreenOpen) {
                                IconButton(
                                    onClick = { isNumbersScreenOpen = true },
                                    modifier = Modifier.testTag("top_bar_numbers_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "اعداد ۰ تا ۱۰۰",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isUiVisible && !isNumbersScreenOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavDestination.values().forEach { destination ->
                            val isSelected = currentTab == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    currentTab = destination
                                    isUiVisible = true
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.titleDari,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.titleDari,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            val effectivePadding = if (isUiVisible) paddingValues else PaddingValues(0.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(effectivePadding)
            ) {
                if (isNumbersScreenOpen) {
                    NumbersScreen(
                        onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                        onBack = { isNumbersScreenOpen = false }
                    )
                } else {
                    when (currentTab) {
                        NavDestination.HOME -> {
                            HomeScreen(
                                allLessons = allLessons,
                                grammarBookLessons = grammarBookLessons,
                                learnedWords = learnedWords,
                                quizHighScore = quizHighScore,
                                overrideLessonNumbers = overrideNumbers,
                                onNavigateToLessonVocab = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 0
                                    activeLessonIsGrammarBook = false
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToLessonPractice = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 3
                                    activeLessonIsGrammarBook = false
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToGrammarBookLessonVocab = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 0
                                    activeLessonIsGrammarBook = true
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToGrammarBookLessonPractice = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 3
                                    activeLessonIsGrammarBook = true
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToNumbers = { isNumbersScreenOpen = true },
                                onNavigateToQuiz = { currentTab = NavDestination.QUIZ },
                                onNavigateToMuse = { currentTab = NavDestination.MUSE },
                                onImportJson = { json -> courseRepository.importJson(json) },
                                onImportGrammarBookJson = { json -> grammarBookRepository.importJson(json) },
                                onDeleteCustomLesson = { lessonId -> courseRepository.deleteCustomLesson(lessonId) },
                                onDeleteGrammarBookLesson = { lessonId -> grammarBookRepository.deleteLesson(lessonId) },
                                onNavigateToGrammar = { currentTab = NavDestination.GRAMMAR }
                            )
                        }

                        NavDestination.GRAMMAR -> {
                            GrammarScreen(
                                allTopics = allGrammarTopics,
                                overrideTopicNumbers = overrideGrammarNumbers,
                                onImportJson = { json -> grammarRepository.importJson(json) },
                                onDeleteCustomTopic = { topicId -> grammarRepository.deleteCustomTopic(topicId) },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                onNavigateToLessons = { currentTab = NavDestination.HOME },
                                ttsManager = ttsManager
                            )
                        }

                        NavDestination.VOCABULARY -> {
                            val activeLessons = if (activeLessonIsGrammarBook && grammarBookLessons.isNotEmpty()) {
                                grammarBookLessons
                            } else {
                                allLessons
                            }
                            VocabularyScreen(
                                allLessons = activeLessons,
                                initialLessonId = selectedLessonIdForVocab,
                                learnedWords = learnedWords,
                                onToggleLearned = { wordKey, learned ->
                                    progressManager.setWordLearned(wordKey, learned)
                                },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                initialSection = selectedLessonSection,
                                ttsManager = ttsManager
                            )
                        }

                        NavDestination.PRACTICE -> {
                            val activeLessons = if (activeLessonIsGrammarBook && grammarBookLessons.isNotEmpty()) {
                                grammarBookLessons
                            } else {
                                allLessons
                            }
                            PracticeScreen(
                                allLessons = activeLessons,
                                initialLessonId = selectedLessonIdForPractice,
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.QUIZ -> {
                            QuizScreen(
                                allLessons = allLessons,
                                onSaveQuizScore = { score, total ->
                                    progressManager.saveQuizResult(score, total)
                                },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.DIALOGUES -> {
                            DialoguesScreen(
                                allLessons = allLessons,
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.GEMINI_CHAT -> {
                            GeminiChatScreen(
                                currentApiKey = effectiveKey,
                                onSaveApiKey = { key -> progressManager.saveGeminiApiKey(key) },
                                onImportLessonJson = { json -> courseRepository.importLesson(json) },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                onImportGrammarJson = { json -> grammarRepository.importTopic(json) },
                                onTestVoice = { key, speed -> ttsManager.testGeminiVoice(speed = speed, keyOverride = key) }
                            )
                        }

                        NavDestination.MUSE -> {
                            MuseScreen(
                                onImportJson = { json -> courseRepository.importJson(json) }
                            )
                        }
                    }
                }
            }
        }

        // Voice Settings Dialog accessible anywhere from top bar
        if (showVoiceSettingsDialog) {
            VoiceSettingsDialog(
                currentKey = effectiveKey,
                onDismiss = { showVoiceSettingsDialog = false },
                onSave = { newKey ->
                    progressManager.saveGeminiApiKey(newKey)
                    showVoiceSettingsDialog = false
                },
                onTestVoice = { key, speed ->
                    ttsManager.testGeminiVoice(speed = speed, keyOverride = key)
                }
            )
        }
    }
}

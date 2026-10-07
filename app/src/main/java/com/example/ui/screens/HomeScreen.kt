package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LessonData
import com.example.data.repository.BatchImportResult
import com.example.data.storage.UserProgressManager
import com.example.ui.components.ImportLessonDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGold
import com.example.ui.theme.HeaderNavyGradient
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessGreen

enum class HomeLibraryTab {
    LESSONS,       // درس‌ها (Main course)
    GRAMMAR,       // گرامر (Grammar topics shortcut)
    GRAMMAR_BOOK   // کتاب گرامر (Independent grammar book)
}

@Composable
fun HomeScreen(
    allLessons: List<LessonData>,
    grammarBookLessons: List<LessonData> = emptyList(),
    learnedWords: Set<String>,
    quizHighScore: Int,
    overrideLessonNumbers: Set<Int> = emptySet(),
    onNavigateToLessonVocab: (String) -> Unit,
    onNavigateToLessonPractice: (String) -> Unit,
    onNavigateToGrammarBookLessonVocab: (String) -> Unit = {},
    onNavigateToGrammarBookLessonPractice: (String) -> Unit = {},
    onNavigateToNumbers: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToMuse: () -> Unit,
    onImportJson: (String) -> Result<BatchImportResult>,
    onImportGrammarBookJson: (String) -> Result<BatchImportResult> = { Result.failure(Exception("Not implemented")) },
    onDeleteCustomLesson: (String) -> Unit,
    onDeleteGrammarBookLesson: (String) -> Unit = {},
    onNavigateToGrammar: () -> Unit = {},
    initialLibraryTab: HomeLibraryTab = HomeLibraryTab.LESSONS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val studyStreak by progressManager.studyStreakFlow.collectAsState()
    val isStudiedToday = progressManager.isTodayStudied()

    var selectedLibraryTab by remember(initialLibraryTab) { mutableStateOf(initialLibraryTab) }
    var showImportCourseDialog by remember { mutableStateOf(false) }
    var showImportGrammarBookDialog by remember { mutableStateOf(false) }

    var lessonToDeleteFromCourse by remember { mutableStateOf<LessonData?>(null) }
    var lessonToDeleteFromGrammarBook by remember { mutableStateOf<LessonData?>(null) }

    val totalWords = allLessons.sumOf { it.vocabulary.size }
    val totalLearned = allLessons.sumOf { lesson ->
        lesson.vocabulary.count { vocab -> learnedWords.contains("${lesson.id}_${vocab.word}") }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIX O (1b): Learning-Streak Counter with 🔥 icon at the TOP of the Home Screen
        item {
            StreakBanner(
                streak = studyStreak,
                isStudiedToday = isStudiedToday
            )
        }

        // Welcome Banner (Main Course) OR Grammar Book Banner
        item {
            if (selectedLibraryTab == HomeLibraryTab.GRAMMAR_BOOK) {
                GrammarBookBanner(totalLessons = grammarBookLessons.size)
            } else {
                WelcomeBanner(
                    totalLearned = totalLearned,
                    totalWords = totalWords,
                    quizHighScore = quizHighScore
                )
            }
        }

        // FIX H (1): Three Libraries Switcher: «درس‌ها» | «گرامر» | «کتاب گرامر»
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Segment 1: درس‌ها (Main course)
                    if (selectedLibraryTab == HomeLibraryTab.LESSONS) {
                        Button(
                            onClick = { /* already active */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_tab_lessons_active"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("درس‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedLibraryTab = HomeLibraryTab.LESSONS },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_tab_lessons_inactive"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("درس‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Segment 2: گرامر (Shortcut to Grammar topics library)
                    OutlinedButton(
                        onClick = onNavigateToGrammar,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_tab_grammar_shortcut"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("گرامر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Segment 3: کتاب گرامر (Independent grammar book)
                    if (selectedLibraryTab == HomeLibraryTab.GRAMMAR_BOOK) {
                        Button(
                            onClick = { /* already active */ },
                            modifier = Modifier
                                .weight(1.15f)
                                .height(44.dp)
                                .testTag("btn_tab_grammar_book_active"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کتاب گرامر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedLibraryTab = HomeLibraryTab.GRAMMAR_BOOK },
                            modifier = Modifier
                                .weight(1.15f)
                                .height(44.dp)
                                .testTag("btn_tab_grammar_book_inactive"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کتاب گرامر", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ================= CONTENT SWITCHER =================
        if (selectedLibraryTab == HomeLibraryTab.GRAMMAR_BOOK) {
            // FIX H (4): Inside «کتاب گرامر» section: action «افزودن درس گرامر (JSON)»
            item {
                Button(
                    onClick = { showImportGrammarBookDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_add_grammar_book_lesson"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "افزودن درس گرامر (JSON)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // FIX H (3): Empty state in Dari if no lessons
            if (grammarBookLessons.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("grammar_book_empty_state"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "هنوز درس گرامری وارد نشده است — فایل JSON درس گرامر را وارد کنید",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showImportGrammarBookDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("افزودن اولین درس گرامر", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Header for Grammar Book Lessons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فهرست درس‌های کتاب گرامر (${grammarBookLessons.size} درس)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Render Grammar Book Lessons (Full Lessons in LessonSchema)
                items(grammarBookLessons, key = { it.id }) { lesson ->
                    val totalWordsInLesson = lesson.vocabulary.size
                    val learnedInLesson = lesson.vocabulary.count {
                        learnedWords.contains("${lesson.id}_${it.word}")
                    }
                    val progress = if (totalWordsInLesson > 0) learnedInLesson.toFloat() / totalWordsInLesson.toFloat() else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grammar_book_lesson_card_${lesson.id}")
                            .clickable {
                                progressManager.recordStudyDay()
                                onNavigateToGrammarBookLessonVocab(lesson.id)
                            },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${lesson.number}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = lesson.titleDari,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = lesson.titleGerman,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Circular Progress Ring showing percentage of learned vocabulary
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("grammar_book_progress_ring_${lesson.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 3.5.dp,
                                            color = if (progress >= 1f) SuccessGreen else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}٪",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (progress >= 1f) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Delete button for Grammar Book Lesson (FIX H (5))
                                    IconButton(
                                        onClick = { lessonToDeleteFromGrammarBook = lesson },
                                        modifier = Modifier.testTag("btn_delete_grammar_book_lesson_${lesson.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف درس از کتاب گرامر",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${lesson.vocabulary.size} واژه",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (lesson.grammarSections.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "${lesson.grammarSections.size} نکته گرامری",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (lesson.exercises.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "${lesson.exercises.size} تمرین",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Reuses the exact same lesson detail experiences
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToGrammarBookLessonVocab(lesson.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_learn_grammar_book_lesson_${lesson.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مطالعه و فلش‌کارت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = { onNavigateToGrammarBookLessonPractice(lesson.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_practice_grammar_book_lesson_${lesson.id}"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تمرین‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ================= LESSONS TAB (Main Course) =================
            // Prominent Button: «افزودن درس جدید»
            item {
                Button(
                    onClick = { showImportCourseDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_add_new_lesson"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "افزودن درس جدید (JSON)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Training Shortcuts (Numbers & Mixed Quiz)
            item {
                Text(
                    text = "بخش‌های تمرین ویژه",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Numbers Shortcut
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_numbers_trainer")
                            .clickable { onNavigateToNumbers() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("تمرین اعداد (۰-۱۰۰)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("همراه با تلفظ و قوانین", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f))
                        }
                    }

                    // Mixed Quiz Shortcut
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shortcut_mixed_quiz")
                            .clickable { onNavigateToQuiz() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IndigoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("آزمون جامع A1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("۱۰ سوال تصادفی با تحلیل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Muse Assistant Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shortcut_muse_assistant")
                        .clickable { onNavigateToMuse() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEEF2FF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4338CA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "دستیار هوشمند درس‌ساز (موس)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF312E81)
                                )
                                Text(
                                    text = "طراحی درس‌های پیشرفته بر اساس سطوح CEFR",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF4338CA).copy(alpha = 0.85f)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Lessons List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فهرست درس‌ها (${allLessons.size} درس)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = onNavigateToGrammar,
                        modifier = Modifier.testTag("btn_header_grammar")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کتابخانه گرامر", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Dynamic Lesson Cards (built-in + imported course lessons)
            items(allLessons, key = { it.id }) { lesson ->
                val totalWordsInLesson = lesson.vocabulary.size
                val learnedInLesson = lesson.vocabulary.count {
                    learnedWords.contains("${lesson.id}_${it.word}")
                }
                val progress = if (totalWordsInLesson > 0) learnedInLesson.toFloat() / totalWordsInLesson.toFloat() else 0f
                val isCustom = overrideLessonNumbers.contains(lesson.number) || lesson.number > 8 || !lesson.id.startsWith("lesson_")
                val isFromMuse = lesson.source?.trim()?.lowercase() == "muse"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_card_${lesson.id}")
                        .clickable {
                            progressManager.recordStudyDay()
                            onNavigateToLessonVocab(lesson.id)
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Circular Progress Ring with Percentage/Badge
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(54.dp),
                                        strokeWidth = 3.5.dp,
                                        color = if (progress >= 1.0f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isFromMuse) Color(0xFF6366F1).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${lesson.number}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (isFromMuse) Color(0xFF4338CA) else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lesson.titleDari,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (isFromMuse) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "ساختهٔ موس",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF4338CA),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = lesson.titleGerman,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (isCustom) {
                                IconButton(
                                    onClick = { lessonToDeleteFromCourse = lesson },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف درس سفارشی",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "واژگان آموخته: $learnedInLesson از $totalWordsInLesson",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}٪",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fast Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    progressManager.recordStudyDay()
                                    onNavigateToLessonVocab(lesson.id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("فلش‌کارت‌ها (${lesson.vocabulary.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    progressManager.recordStudyDay()
                                    onNavigateToLessonPractice(lesson.id)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تمرین (${lesson.exercises.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Course Lesson Import
    if (showImportCourseDialog) {
        ImportLessonDialog(
            title = "افزودن درس جدید (JSON)",
            onDismiss = { showImportCourseDialog = false },
            onImportJson = onImportJson,
            onImportSuccess = { result ->
                Toast.makeText(context, result.summaryMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Dialog for Grammar Book Lesson Import
    if (showImportGrammarBookDialog) {
        ImportLessonDialog(
            title = "افزودن درس کتاب گرامر (JSON)",
            onDismiss = { showImportGrammarBookDialog = false },
            onImportJson = onImportGrammarBookJson,
            onImportSuccess = { result ->
                Toast.makeText(context, result.summaryMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Delete Confirmation Dialog for Course Lesson
    if (lessonToDeleteFromCourse != null) {
        AlertDialog(
            onDismissRequest = { lessonToDeleteFromCourse = null },
            title = { Text("حذف درس سفارشی", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مطمئن هستید که می‌خواهید درس ${lessonToDeleteFromCourse?.number} را حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        lessonToDeleteFromCourse?.let { l ->
                            onDeleteCustomLesson(l.number.toString())
                            Toast.makeText(context, "درس ${l.number} حذف شد.", Toast.LENGTH_SHORT).show()
                        }
                        lessonToDeleteFromCourse = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDeleteFromCourse = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Delete Confirmation Dialog for Grammar Book Lesson (FIX H (5))
    if (lessonToDeleteFromGrammarBook != null) {
        AlertDialog(
            onDismissRequest = { lessonToDeleteFromGrammarBook = null },
            title = { Text("حذف درس از کتاب گرامر", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مطمئن هستید که می‌خواهید درس ${lessonToDeleteFromGrammarBook?.number} («${lessonToDeleteFromGrammarBook?.titleGerman}») را از کتاب گرامر حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        lessonToDeleteFromGrammarBook?.let { l ->
                            onDeleteGrammarBookLesson(l.id)
                            Toast.makeText(context, "درس ${l.number} از کتاب گرامر حذف شد.", Toast.LENGTH_SHORT).show()
                        }
                        lessonToDeleteFromGrammarBook = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDeleteFromGrammarBook = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun WelcomeBanner(
    totalLearned: Int,
    totalWords: Int,
    quizHighScore: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(HeaderNavyGradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "آلمانی بیاموز 🇩🇪",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "آموزش گام‌به‌گام سطح A1 به زبان فارسی دری",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("$totalLearned / $totalWords", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text("لغات آموخته", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("$quizHighScore از ۱۰", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Text("رکورد آزمون", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarBookBanner(
    totalLessons: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(HeaderNavyGradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کتاب گرامر آلمانی 📖",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "درس‌های مستقل گرامری همراه با واژگان، قواعد، مثال‌ها و تمرین‌ها",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "$totalLessons درس در کتابخانه",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * FIX O (1b): Learning-streak counter banner at the top of the home screen.
 * Persists study days, calculating consecutive days ending today (or yesterday).
 * Label in Dari: «زنجیرهٔ یادگیری: N روز 🔥»
 */
@Composable
private fun StreakBanner(
    streak: Int,
    isStudiedToday: Boolean
) {
    val streakText = UserProgressManager.toPersianDigits(streak.toString())
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("learning_streak_banner"),
        shape = RoundedCornerShape(18.dp),
        color = if (streak > 0) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (streak > 0) Color(0xFFFDBA74) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (streak > 0) Color(0xFFFFEDD5) else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔥", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "زنجیرهٔ یادگیری: $streakText روز 🔥",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (streak > 0) Color(0xFFC2410C) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isStudiedToday) "امروز با موفقیت تمرین کرده‌اید! عالی هستید 👏" else "امروز هنوز درسی باز نکرده‌اید؛ تمرین کنید تا زنجیره حفظ شود!",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (streak > 0) Color(0xFF9A3412) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

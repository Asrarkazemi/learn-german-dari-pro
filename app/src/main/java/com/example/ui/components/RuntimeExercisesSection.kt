package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.consumeTaps
import com.example.data.model.ExampleSentence
import com.example.data.model.VocabularyItem
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * FIX O (Feature 1): Runtime generated exercise types from existing lesson data:
 * (a) Cloze «جای خالی»
 * (b) Word order «ترتیب کلمات»
 * (c) Matching «وصل‌کردنی»
 *
 * Section title: «تمرین‌های تازه»
 */
@Composable
fun RuntimeExercisesSection(
    exampleSentences: List<ExampleSentence>,
    vocabulary: List<VocabularyItem>,
    onPlayAudio: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mode availability checks
    val hasCloze = remember(exampleSentences) {
        exampleSentences.any { ClozeQuestion.canCreateFrom(it) }
    }
    val hasWordOrder = remember(exampleSentences) {
        exampleSentences.any { WordOrderQuestion.canCreateFrom(it) }
    }
    val hasMatching = remember(vocabulary) {
        vocabulary.size >= 5
    }

    if (!hasCloze && !hasWordOrder && !hasMatching) {
        return // Gracefully hide if lesson lacks enough data
    }

    val availableModes = remember(hasCloze, hasWordOrder, hasMatching) {
        buildList {
            if (hasCloze) add(0 to "جای خالی")
            if (hasWordOrder) add(1 to "ترتیب کلمات")
            if (hasMatching) add(2 to "وصل‌کردنی")
        }
    }

    var selectedModeIndex by remember(availableModes) {
        mutableIntStateOf(0)
    }

    val activeModeId = availableModes.getOrNull(selectedModeIndex)?.first ?: availableModes.first().first

    Card(
        modifier = modifier
            .fillMaxWidth()
            .consumeTaps()
            .testTag("runtime_exercises_container"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = AccentGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تمرین‌های تازه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تمرین‌های تعاملی تولید شده از محتوای این درس",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector Tabs
            if (availableModes.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedModeIndex.coerceIn(0, availableModes.size - 1),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableModes.forEachIndexed { idx, (_, label) ->
                        Tab(
                            selected = selectedModeIndex == idx,
                            onClick = { selectedModeIndex = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedModeIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_runtime_mode_$idx")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            when (activeModeId) {
                0 -> ClozeExerciseView(
                    exampleSentences = exampleSentences,
                    vocabulary = vocabulary,
                    onPlayAudio = onPlayAudio
                )
                1 -> WordOrderExerciseView(
                    exampleSentences = exampleSentences,
                    onPlayAudio = onPlayAudio
                )
                2 -> MatchingExerciseView(
                    vocabulary = vocabulary,
                    onPlayAudio = onPlayAudio
                )
            }
        }
    }
}

// =========================================================================
// (a) CLOZE: «جای خالی»
// =========================================================================
private data class ClozeQuestion(
    val fullSentence: ExampleSentence,
    val blankedSentence: String,
    val correctWord: String,
    val options: List<String>
) {
    companion object {
        fun canCreateFrom(s: ExampleSentence): Boolean {
            val words = s.german.split(Regex("\\s+"))
            return words.any { cleanWord(it).length >= 4 }
        }

        fun cleanWord(w: String): String =
            w.trim().trim('"', '\'', '.', ',', '!', '?', ':', ';', '(', ')')

        fun generate(
            sentences: List<ExampleSentence>,
            vocabulary: List<VocabularyItem>,
            seed: Int
        ): ClozeQuestion? {
            val candidates = sentences.filter { canCreateFrom(it) }
            if (candidates.isEmpty()) return null
            val rnd = Random(seed)
            val pickedSentence = candidates[rnd.nextInt(candidates.size)]

            val words = pickedSentence.german.split(Regex("\\s+"))
            val eligibleIndices = words.indices.filter { cleanWord(words[it]).length >= 4 }
            val chosenIdx = if (eligibleIndices.isNotEmpty()) {
                eligibleIndices[rnd.nextInt(eligibleIndices.size)]
            } else {
                words.indices.maxByOrNull { cleanWord(words[it]).length } ?: 0
            }

            val targetRaw = words[chosenIdx]
            val correctWord = cleanWord(targetRaw)
            val blanked = words.mapIndexed { idx, w ->
                if (idx == chosenIdx) {
                    val punc = targetRaw.filter { it in ".,!?:;\"'()" }
                    "___$punc"
                } else w
            }.joinToString(" ")

            // Collect distractors from same lesson's sentences & vocabulary
            val otherWords = mutableSetOf<String>()
            for (s in sentences) {
                for (w in s.german.split(Regex("\\s+"))) {
                    val c = cleanWord(w)
                    if (c.length >= 3 && !c.equals(correctWord, ignoreCase = true)) {
                        otherWords.add(c)
                    }
                }
            }
            for (v in vocabulary) {
                val c = cleanWord(v.word)
                if (c.length >= 3 && !c.equals(correctWord, ignoreCase = true)) {
                    otherWords.add(c)
                }
            }

            val distractors = otherWords.shuffled(rnd).take(3).toMutableList()
            while (distractors.size < 3) {
                val fallback = listOf("nicht", "sehr", "gut", "hier", "dort", "auch", "immer", "heute")
                    .filter { !it.equals(correctWord, ignoreCase = true) && !distractors.contains(it) }
                if (fallback.isNotEmpty()) {
                    distractors.add(fallback.first())
                } else break
            }

            val options = (distractors + correctWord).shuffled(rnd)
            return ClozeQuestion(
                fullSentence = pickedSentence,
                blankedSentence = blanked,
                correctWord = correctWord,
                options = options
            )
        }
    }
}

@Composable
private fun ClozeExerciseView(
    exampleSentences: List<ExampleSentence>,
    vocabulary: List<VocabularyItem>,
    onPlayAudio: (String, Boolean) -> Unit
) {
    var roundSeed by remember { mutableIntStateOf(Random.nextInt()) }
    var selectedOption by remember(roundSeed) { mutableStateOf<String?>(null) }
    var isSubmitted by remember(roundSeed) { mutableStateOf(false) }

    val question = remember(roundSeed, exampleSentences, vocabulary) {
        ClozeQuestion.generate(exampleSentences, vocabulary, roundSeed)
    }

    if (question == null) {
        Text(
            text = "داده کافی برای تمرین جای خالی موجود نیست.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        return
    }

    val isCorrect = selectedOption?.equals(question.correctWord, ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloze_exercise_view")
    ) {
        // German sentence with blank
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = question.blankedSentence,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Options
        question.options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowOptions.forEach { opt ->
                    val isThisSelected = selectedOption == opt
                    val isThisCorrect = opt.equals(question.correctWord, ignoreCase = true)

                    val bgColor by animateColorAsState(
                        targetValue = when {
                            !isSubmitted && isThisSelected -> MaterialTheme.colorScheme.primaryContainer
                            isSubmitted && isThisCorrect -> SuccessGreen.copy(alpha = 0.25f)
                            isSubmitted && isThisSelected && !isThisCorrect -> ErrorRed.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        label = "cloze_opt_bg"
                    )

                    val borderColor = when {
                        isSubmitted && isThisCorrect -> SuccessGreen
                        isSubmitted && isThisSelected && !isThisCorrect -> ErrorRed
                        !isSubmitted && isThisSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isSubmitted) {
                                selectedOption = opt
                                isSubmitted = true
                            }
                            .testTag("cloze_option_$opt")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = opt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = when {
                                        isSubmitted && isThisCorrect -> SuccessGreen
                                        isSubmitted && isThisSelected && !isThisCorrect -> ErrorRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Feedback & Explanation & Next button
        AnimatedVisibility(
            visible = isSubmitted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCorrect) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isCorrect) SuccessGreen else ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isCorrect) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCorrect) "درست است ✓" else "نادرست ✗ (جواب درست: ${question.correctWord})",
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) SuccessGreen else ErrorRed,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ترجمه: ${question.fullSentence.meaningDari}",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        roundSeed = Random.nextInt()
                        selectedOption = null
                        isSubmitted = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_cloze_next"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "سؤال بعدی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// (b) WORD ORDER: «ترتیب کلمات»
// =========================================================================
private data class WordOrderQuestion(
    val fullSentence: ExampleSentence,
    val originalWords: List<String>,
    val shuffledChips: List<WordChip>
) {
    data class WordChip(val id: Int, val word: String)

    companion object {
        fun canCreateFrom(s: ExampleSentence): Boolean {
            val words = s.german.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return words.size in 5..9
        }

        fun generate(sentences: List<ExampleSentence>, seed: Int): WordOrderQuestion? {
            val eligible = sentences.filter { canCreateFrom(it) }
            val pool = if (eligible.isNotEmpty()) eligible else sentences.filter {
                val words = it.german.trim().split(Regex("\\s+")).filter { w -> w.isNotBlank() }
                words.size in 3..12
            }
            if (pool.isEmpty()) return null
            val rnd = Random(seed)
            val picked = pool[rnd.nextInt(pool.size)]
            val words = picked.german.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val chips = words.mapIndexed { idx, w -> WordChip(idx, w) }.shuffled(rnd)
            return WordOrderQuestion(
                fullSentence = picked,
                originalWords = words,
                shuffledChips = chips
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordOrderExerciseView(
    exampleSentences: List<ExampleSentence>,
    onPlayAudio: (String, Boolean) -> Unit
) {
    var roundSeed by remember { mutableIntStateOf(Random.nextInt()) }
    val question = remember(roundSeed, exampleSentences) {
        WordOrderQuestion.generate(exampleSentences, roundSeed)
    }

    if (question == null) {
        Text(
            text = "جمله مناسب برای تمرین ترتیب کلمات موجود نیست.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        return
    }

    // Placed chips in user answer area
    var placedChips by remember(roundSeed) { mutableStateOf<List<WordOrderQuestion.WordChip>>(emptyList()) }
    var isChecked by remember(roundSeed) { mutableStateOf(false) }
    var isCorrect by remember(roundSeed) { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val placedIds = remember(placedChips) { placedChips.map { it.id }.toSet() }
    val availableChips = remember(question.shuffledChips, placedIds) {
        question.shuffledChips.filter { !placedIds.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_order_exercise_view")
    ) {
        Text(
            text = "کلمات زیر را برای ساخت جمله درست مرتب کنید:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Target / Answer area (box)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = when {
                isChecked && isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                isChecked && !isCorrect -> ErrorRed.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            border = BorderStroke(
                1.5.dp,
                when {
                    isChecked && isCorrect -> SuccessGreen
                    isChecked && !isCorrect -> ErrorRed
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = if (placedChips.isEmpty()) "برای چیدن جمله روی کلمات زیر ضربه بزنید..." else "پاسخ شما (برای حذف، روی کلمه بزنید):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        placedChips.forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isCorrect) {
                                        placedChips = placedChips.filter { it.id != chip.id }
                                        isChecked = false
                                    }
                                    .testTag("placed_chip_${chip.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = chip.word,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Available Chips Area
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableChips.forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isCorrect) {
                                placedChips = placedChips + chip
                                isChecked = false
                            }
                            .testTag("avail_chip_${chip.id}")
                    ) {
                        Text(
                            text = chip.word,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Check button or Success / Next card
        if (!isCorrect) {
            Button(
                onClick = {
                    val userWords = placedChips.map { it.word }
                    val targetWords = question.originalWords
                    val ok = userWords == targetWords
                    isChecked = true
                    isCorrect = ok
                    if (!ok) {
                        // Shake animation
                        coroutineScope.launch {
                            for (i in 0 until 3) {
                                shakeOffset.animateTo(18f, tween(50, easing = FastOutSlowInEasing))
                                shakeOffset.animateTo(-18f, tween(50, easing = FastOutSlowInEasing))
                            }
                            shakeOffset.animateTo(0f, tween(50))
                        }
                    }
                },
                enabled = placedChips.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_word_order_check"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "بررسی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            // Correct result card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "آفرین! ترتیب کلمات کاملاً درست است ✓",
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ترجمه: ${question.fullSentence.meaningDari}",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    roundSeed = Random.nextInt()
                    placedChips = emptyList()
                    isChecked = false
                    isCorrect = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_word_order_next"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "سؤال بعدی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// =========================================================================
// (c) MATCHING: «وصل‌کردنی»
// =========================================================================
private data class MatchingPair(
    val id: String,
    val german: String,
    val dari: String
)

@Composable
private fun MatchingExerciseView(
    vocabulary: List<VocabularyItem>,
    onPlayAudio: (String, Boolean) -> Unit
) {
    var roundSeed by remember { mutableIntStateOf(Random.nextInt()) }

    val pairs = remember(roundSeed, vocabulary) {
        val sample = vocabulary.shuffled(Random(roundSeed)).take(5)
        sample.mapIndexed { index, it ->
            val fullGerman = if (it.article.isNotBlank()) "${it.article} ${it.word}" else it.word
            MatchingPair(id = "${it.word}_$index", german = fullGerman, dari = it.meaningDari)
        }
    }

    if (pairs.size < 5) {
        Text(
            text = "برای تمرین وصل‌کردنی حداقل ۵ لغت نیاز است.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        return
    }

    val shuffledGerman = remember(pairs, roundSeed) {
        pairs.map { it.id to it.german }.shuffled(Random(roundSeed + 1))
    }
    val shuffledDari = remember(pairs, roundSeed) {
        pairs.map { it.id to it.dari }.shuffled(Random(roundSeed + 2))
    }

    var selectedGermanId by remember(roundSeed) { mutableStateOf<String?>(null) }
    var selectedDariId by remember(roundSeed) { mutableStateOf<String?>(null) }
    val matchedIds = remember(roundSeed) { mutableStateMapOf<String, Boolean>() }
    var wrongPairIds by remember(roundSeed) { mutableStateOf<Pair<String, String>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isAllMatched = matchedIds.size == pairs.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("matching_exercise_view")
    ) {
        Text(
            text = "هر واژه آلمانی را به معنای دری آن وصل کنید (یک واژه از راست و یکی از چپ انتخاب کنید):",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Two columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // German Column (Left / Right depending on layout, explicitly LTR content)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "آلمانی",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                shuffledGerman.forEach { (id, text) ->
                    val isLocked = matchedIds[id] == true
                    val isSelected = selectedGermanId == id
                    val isWrong = wrongPairIds?.first == id

                    val bg = when {
                        isLocked -> SuccessGreen.copy(alpha = 0.2f)
                        isWrong -> ErrorRed.copy(alpha = 0.25f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }

                    val border = when {
                        isLocked -> SuccessGreen
                        isWrong -> ErrorRed
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = BorderStroke(1.5.dp, border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isLocked && wrongPairIds == null) {
                                selectedGermanId = id
                                if (selectedDariId != null) {
                                    val currentDari = selectedDariId!!
                                    if (id == currentDari) {
                                        matchedIds[id] = true
                                        selectedGermanId = null
                                        selectedDariId = null
                                    } else {
                                        wrongPairIds = id to currentDari
                                        coroutineScope.launch {
                                            delay(700)
                                            wrongPairIds = null
                                            selectedGermanId = null
                                            selectedDariId = null
                                        }
                                    }
                                }
                            }
                            .testTag("matching_german_$id")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = text,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Dari Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "دری",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                shuffledDari.forEach { (id, text) ->
                    val isLocked = matchedIds[id] == true
                    val isSelected = selectedDariId == id
                    val isWrong = wrongPairIds?.second == id

                    val bg = when {
                        isLocked -> SuccessGreen.copy(alpha = 0.2f)
                        isWrong -> ErrorRed.copy(alpha = 0.25f)
                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }

                    val border = when {
                        isLocked -> SuccessGreen
                        isWrong -> ErrorRed
                        isSelected -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = BorderStroke(1.5.dp, border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isLocked && wrongPairIds == null) {
                                selectedDariId = id
                                if (selectedGermanId != null) {
                                    val currentGerman = selectedGermanId!!
                                    if (id == currentGerman) {
                                        matchedIds[id] = true
                                        selectedGermanId = null
                                        selectedDariId = null
                                    } else {
                                        wrongPairIds = currentGerman to id
                                        coroutineScope.launch {
                                            delay(700)
                                            wrongPairIds = null
                                            selectedGermanId = null
                                            selectedDariId = null
                                        }
                                    }
                                }
                            }
                            .testTag("matching_dari_$id")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = text,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocked) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isAllMatched) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "آفرین! همه جفت‌ها وصل شدند",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            roundSeed = Random.nextInt()
                            selectedGermanId = null
                            selectedDariId = null
                            matchedIds.clear()
                            wrongPairIds = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_matching_next_round"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "دور بعدی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

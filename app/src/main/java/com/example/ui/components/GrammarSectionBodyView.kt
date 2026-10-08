package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.UserProgressManager
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.util.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FIX G, FIX O & Feature 3: Beautiful, readable rendering of grammar sections.
 * Splits section body (bodyDari) into lines and renders with correct direction,
 * stacked table rows for "|" separators, subheadings, comfortable typography,
 * and FEATURE 3 interactive blanks: ⟦answer⟧.
 */
@Composable
fun GrammarSectionCard(
    title: String,
    bodyDari: String,
    modifier: Modifier = Modifier,
    sectionNumber: Int? = null,
    onPlayAudio: ((String, Boolean) -> Unit)? = null,
    extraDistractorWords: List<String> = emptyList()
) {
    // Collect all ⟦answer⟧ occurrences in this section for distractors
    val allSectionAnswers = remember(bodyDari) {
        val matches = Regex("""⟦(.*?)⟧""").findAll(bodyDari)
        matches.map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.distinct().toList()
    }
    val hasBlanks = allSectionAnswers.isNotEmpty()

    // State per section: map of blankId (e.g. "lineIdx_blankIdx") -> current displayed text
    val filledBlanks = remember(bodyDari) { mutableStateMapOf<String, String>() }
    // Global reveal toggle for all blanks of this section
    var isRevealedAll by remember(bodyDari) { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Section Number / Badge & Title + «نمایش جواب‌ها» button if has blanks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sectionNumber != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$sectionNumber",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (hasBlanks) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            isRevealedAll = !isRevealedAll
                            if (!isRevealedAll) {
                                filledBlanks.clear()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_toggle_section_answers")
                    ) {
                        Icon(
                            imageVector = if (isRevealedAll) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRevealedAll) "پنهان‌کردن جواب‌ها" else "نمایش جواب‌ها",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Body rendering
            GrammarSectionBodyView(
                bodyDari = bodyDari,
                onPlayAudio = onPlayAudio,
                allSectionAnswers = allSectionAnswers,
                extraDistractorWords = extraDistractorWords,
                filledBlanks = filledBlanks,
                isRevealedAll = isRevealedAll
            )
        }
    }
}

@Composable
fun GrammarSectionBodyView(
    bodyDari: String,
    modifier: Modifier = Modifier,
    onPlayAudio: ((String, Boolean) -> Unit)? = null,
    allSectionAnswers: List<String> = emptyList(),
    extraDistractorWords: List<String> = emptyList(),
    filledBlanks: MutableMap<String, String> = remember(bodyDari) { mutableStateMapOf() },
    isRevealedAll: Boolean = false
) {
    val rawLines = bodyDari.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var consecutiveEmptyCount = 0

        for ((lineIdx, rawLine) in rawLines.withIndex()) {
            val line = rawLine.trim()

            if (line.isEmpty()) {
                consecutiveEmptyCount++
                if (consecutiveEmptyCount == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                continue
            }
            consecutiveEmptyCount = 0

            // RULE 2: Table Row containing "|" separator
            if (line.contains("|")) {
                renderTableRowWithPipe(
                    line = line,
                    lineIdx = lineIdx,
                    onPlayAudio = onPlayAudio,
                    allSectionAnswers = allSectionAnswers,
                    extraDistractorWords = extraDistractorWords,
                    filledBlanks = filledBlanks,
                    isRevealedAll = isRevealedAll
                )
                continue
            }

            // RULE 3: Subheadings inside a section
            if (isSubheadingLine(line)) {
                renderSubheading(line)
                continue
            }

            // Check if line is a German item with Dari translation in parentheses
            if (isGermanItemWithDariParenthesis(line)) {
                renderGermanWithDariParenthesis(
                    line = line,
                    lineIdx = lineIdx,
                    onPlayAudio = onPlayAudio,
                    allSectionAnswers = allSectionAnswers,
                    extraDistractorWords = extraDistractorWords,
                    filledBlanks = filledBlanks,
                    isRevealedAll = isRevealedAll
                )
                continue
            }

            // Bullet or list items
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–")) {
                renderListItem(
                    line = line,
                    lineIdx = lineIdx,
                    onPlayAudio = onPlayAudio,
                    allSectionAnswers = allSectionAnswers,
                    extraDistractorWords = extraDistractorWords,
                    filledBlanks = filledBlanks,
                    isRevealedAll = isRevealedAll
                )
                continue
            }

            // Standard paragraph: check dominant script direction or presence of blanks
            val cleanForCheck = line.replace("⟦", "").replace("⟧", "")
            if (isMainlyLatin(cleanForCheck) || line.contains("⟦")) {
                // German paragraph / line with blanks: LTR, left-aligned in a row with its fixed speaker column
                val completedLine = resolveCompletedGermanText(line)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (line.contains("⟦")) {
                                    InteractiveBlankLine(
                                        lineText = line,
                                        lineIdx = lineIdx,
                                        allSectionAnswers = allSectionAnswers,
                                        extraDistractorWords = extraDistractorWords,
                                        filledBlanks = filledBlanks,
                                        isRevealedAll = isRevealedAll
                                    )
                                } else {
                                    Text(
                                        text = line,
                                        fontSize = 14.5.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        if (onPlayAudio != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GrammarLineSpeakerButton(text = completedLine, onPlayAudio = onPlayAudio)
                            }
                        }
                    }
                }
            } else {
                // Dari paragraph: RTL, right-aligned, comfortable line-height ~1.6
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = line,
                        fontSize = 14.5.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Strips ⟦answer⟧ and replaces with answer for speech and fallback text.
 */
fun resolveCompletedGermanText(rawText: String): String {
    return rawText.replace(Regex("""⟦(.*?)⟧""")) { it.groupValues[1] }
}

/**
 * Tokenizes a line into literal string segments and Blank tokens.
 */
private sealed class LineSegment {
    data class Literal(val text: String) : LineSegment()
    data class Blank(val blankIndex: Int, val answer: String) : LineSegment()
}

private fun parseLineSegments(line: String): List<LineSegment> {
    val results = mutableListOf<LineSegment>()
    val regex = Regex("""⟦(.*?)⟧""")
    var lastIndex = 0
    var blankCounter = 0
    for (match in regex.findAll(line)) {
        if (match.range.first > lastIndex) {
            results.add(LineSegment.Literal(line.substring(lastIndex, match.range.first)))
        }
        val answer = match.groupValues[1].trim()
        results.add(LineSegment.Blank(blankCounter++, answer))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < line.length) {
        results.add(LineSegment.Literal(line.substring(lastIndex)))
    }
    return results
}

/**
 * Interactive blank line: renders inline literal text + tappable blank chips.
 * When a blank chip is clicked, a row/popover of options is displayed below the line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveBlankLine(
    lineText: String,
    lineIdx: Int,
    allSectionAnswers: List<String>,
    extraDistractorWords: List<String>,
    filledBlanks: MutableMap<String, String>,
    isRevealedAll: Boolean,
    modifier: Modifier = Modifier
) {
    val segments = remember(lineText) { parseLineSegments(lineText) }
    var activeBlankForOptions by remember(lineText) { mutableStateOf<LineSegment.Blank?>(null) }
    var wrongOptionFlash by remember(lineText) { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEach { seg ->
                when (seg) {
                    is LineSegment.Literal -> {
                        Text(
                            text = seg.text,
                            fontSize = 15.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    is LineSegment.Blank -> {
                        val blankKey = "${lineIdx}_${seg.blankIndex}"
                        val currentFilled = if (isRevealedAll) seg.answer else filledBlanks[blankKey]
                        val isFilledCorrectly = currentFilled != null && currentFilled.equals(seg.answer, ignoreCase = true)

                        val chipBgColor by animateColorAsState(
                            targetValue = when {
                                isFilledCorrectly -> SuccessGreen.copy(alpha = 0.25f)
                                activeBlankForOptions?.blankIndex == seg.blankIndex -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            label = "blank_chip_bg"
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chipBgColor,
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    isFilledCorrectly -> SuccessGreen
                                    activeBlankForOptions?.blankIndex == seg.blankIndex -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activeBlankForOptions = if (activeBlankForOptions?.blankIndex == seg.blankIndex) null else seg
                                }
                                .testTag("blank_chip_${lineIdx}_${seg.blankIndex}")
                        ) {
                            Text(
                                text = if (currentFilled != null) currentFilled else " ___ ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = if (isFilledCorrectly) SuccessGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Popover/row of options when a blank is active
        activeBlankForOptions?.let { activeBlank ->
            val blankKey = "${lineIdx}_${activeBlank.blankIndex}"
            val options = remember(activeBlank, allSectionAnswers, extraDistractorWords) {
                val correctAnswer = activeBlank.answer
                val candidates = mutableSetOf<String>()
                for (ans in allSectionAnswers) {
                    if (!ans.equals(correctAnswer, ignoreCase = true) && ans.length >= 2) {
                        candidates.add(ans)
                    }
                }
                for (extra in extraDistractorWords) {
                    val clean = extra.trim().trim('"', '\'', '.', ',', '!', '?')
                    if (!clean.equals(correctAnswer, ignoreCase = true) && clean.length >= 2) {
                        candidates.add(clean)
                    }
                }
                val distractors = candidates.shuffled().take(3).toMutableList()
                val fallbackWords = listOf("ist", "sind", "haben", "wir", "Sie", "ich", "du", "er", "es", "gut")
                for (fb in fallbackWords) {
                    if (distractors.size >= 3) break
                    if (!fb.equals(correctAnswer, ignoreCase = true) && !distractors.contains(fb)) {
                        distractors.add(fb)
                    }
                }
                (distractors + correctAnswer).shuffled()
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "یک گزینه را برای جای خالی انتخاب کنید:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.forEach { opt ->
                                val isWrongFlash = wrongOptionFlash == opt
                                val optBg by animateColorAsState(
                                    targetValue = when {
                                        isWrongFlash -> ErrorRed.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    label = "opt_flash_bg"
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = optBg,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isWrongFlash) ErrorRed else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (opt.equals(activeBlank.answer, ignoreCase = true)) {
                                                filledBlanks[blankKey] = activeBlank.answer
                                                activeBlankForOptions = null
                                            } else {
                                                wrongOptionFlash = opt
                                                coroutineScope.launch {
                                                    delay(600)
                                                    wrongOptionFlash = null
                                                }
                                            }
                                        }
                                        .testTag("blank_opt_${opt}")
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isWrongFlash) ErrorRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

/**
 * Fixed non-overlapping speaker button for a German grammar line.
 * Automatically shows a loading spinner while Gemini audio is being fetched.
 */
@Composable
fun GrammarLineSpeakerButton(
    text: String,
    onPlayAudio: ((String, Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (onPlayAudio == null) return
    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val currentSpeed by progressManager.playbackSpeedFlow.collectAsState()
    val loadingSentence by TtsManager.activeLoadingSentenceFlow.collectAsState()
    val cleanText = remember(text) { TtsManager.cleanGermanText(resolveCompletedGermanText(text)) }
    val isLoading = loadingSentence != null && loadingSentence == cleanText

    IconButton(
        onClick = { onPlayAudio(cleanText, currentSpeed <= 0.75f) },
        enabled = !isLoading,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "شنیدن تلفظ",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * Renders a line with "|" separator as a neat STACKED group:
 * German segment first as LTR line with dedicated speaker column, then Dari segment as RTL line.
 */
@Composable
private fun renderTableRowWithPipe(
    line: String,
    lineIdx: Int,
    onPlayAudio: ((String, Boolean) -> Unit)? = null,
    allSectionAnswers: List<String> = emptyList(),
    extraDistractorWords: List<String> = emptyList(),
    filledBlanks: MutableMap<String, String> = remember { mutableStateMapOf() },
    isRevealedAll: Boolean = false
) {
    val segments = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.isEmpty()) return

    val latinSegments = segments.filter { isMainlyLatin(it.replace("⟦", "").replace("⟧", "")) || it.contains("⟦") }
    val dariSegments = segments.filter { !isMainlyLatin(it.replace("⟦", "").replace("⟧", "")) && !it.contains("⟦") }

    val germanText = if (latinSegments.isNotEmpty()) {
        latinSegments.joinToString(" • ")
    } else if (segments.size >= 2) {
        segments[0]
    } else {
        null
    }

    val dariText = if (dariSegments.isNotEmpty()) {
        dariSegments.joinToString(" | ")
    } else if (segments.size >= 2) {
        segments.drop(1).joinToString(" | ")
    } else {
        segments.firstOrNull()
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!germanText.isNullOrBlank()) {
                val completedGerman = resolveCompletedGermanText(germanText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (germanText.contains("⟦")) {
                                InteractiveBlankLine(
                                    lineText = germanText,
                                    lineIdx = lineIdx,
                                    allSectionAnswers = allSectionAnswers,
                                    extraDistractorWords = extraDistractorWords,
                                    filledBlanks = filledBlanks,
                                    isRevealedAll = isRevealedAll
                                )
                            } else {
                                Text(
                                    text = germanText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (onPlayAudio != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GrammarLineSpeakerButton(text = completedGerman, onPlayAudio = onPlayAudio)
                        }
                    }
                }
            }
            if (!dariText.isNullOrBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = dariText,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Checks if a line is a subheading inside a grammar section.
 */
private fun isSubheadingLine(line: String): Boolean {
    val trimmed = line.trim()
    if (trimmed.startsWith("###") || trimmed.startsWith("##") || trimmed.startsWith("**")) return true
    if (trimmed.startsWith("• فعل ") || trimmed.startsWith("• صرف ") ||
        trimmed.startsWith("• قاعده ") || trimmed.startsWith("• نکته ") ||
        trimmed.startsWith("• ساختار ")
    ) return true

    // Short heading ending with colon
    if (trimmed.endsWith(":") || trimmed.endsWith("：")) {
        val clean = trimmed.removeSuffix(":").removeSuffix("：").trim()
        if (clean.length in 3..65 && !clean.contains("(") && !clean.startsWith("-")) {
            return true
        }
    }

    // Numbered topic like "۱. تعریف و کاربرد"
    if ((trimmed.startsWith("۱.") || trimmed.startsWith("1.") ||
        trimmed.startsWith("۲.") || trimmed.startsWith("2.") ||
        trimmed.startsWith("۳.") || trimmed.startsWith("3.") ||
        trimmed.startsWith("۴.") || trimmed.startsWith("4.")) && trimmed.length <= 60
    ) {
        return !trimmed.endsWith(".") || trimmed.count { it == '.' } == 1
    }

    return false
}

@Composable
private fun renderSubheading(line: String) {
    val cleanTitle = line
        .removePrefix("###")
        .removePrefix("##")
        .removePrefix("**")
        .removeSuffix("**")
        .trim()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = cleanTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 22.sp
        )
    }
}

/**
 * Detects patterns like "- Ich heiße... (من نامیده می‌شوم)" where German is outside
 * and Dari translation is in parentheses.
 */
fun isGermanItemWithDariParenthesis(line: String): Boolean {
    val openParen = line.indexOf('(')
    val closeParen = line.lastIndexOf(')')
    if (openParen > 2 && closeParen > openParen) {
        val beforeParen = line.substring(0, openParen).removePrefix("•").removePrefix("-").trim()
        val insideParen = line.substring(openParen + 1, closeParen).trim()
        val cleanBefore = beforeParen.replace("⟦", "").replace("⟧", "")
        return (isMainlyLatin(cleanBefore) || beforeParen.contains("⟦")) && !isMainlyLatin(insideParen)
    }
    return false
}

@Composable
private fun renderGermanWithDariParenthesis(
    line: String,
    lineIdx: Int,
    onPlayAudio: ((String, Boolean) -> Unit)? = null,
    allSectionAnswers: List<String> = emptyList(),
    extraDistractorWords: List<String> = emptyList(),
    filledBlanks: MutableMap<String, String> = remember { mutableStateMapOf() },
    isRevealedAll: Boolean = false
) {
    val openParen = line.indexOf('(')
    val closeParen = line.lastIndexOf(')')
    val beforeParen = line.substring(0, openParen).removePrefix("•").removePrefix("-").removePrefix("–").trim()
    val insideParen = line.substring(openParen + 1, closeParen).trim()
    val completedBefore = resolveCompletedGermanText(beforeParen)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (beforeParen.contains("⟦")) {
                            InteractiveBlankLine(
                                lineText = beforeParen,
                                lineIdx = lineIdx,
                                allSectionAnswers = allSectionAnswers,
                                extraDistractorWords = extraDistractorWords,
                                filledBlanks = filledBlanks,
                                isRevealedAll = isRevealedAll
                            )
                        } else {
                            Text(
                                text = beforeParen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (onPlayAudio != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GrammarLineSpeakerButton(text = completedBefore, onPlayAudio = onPlayAudio)
                    }
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = insideParen,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun renderListItem(
    line: String,
    lineIdx: Int,
    onPlayAudio: ((String, Boolean) -> Unit)? = null,
    allSectionAnswers: List<String> = emptyList(),
    extraDistractorWords: List<String> = emptyList(),
    filledBlanks: MutableMap<String, String> = remember { mutableStateMapOf() },
    isRevealedAll: Boolean = false
) {
    val content = line.removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("–").trim()
    val cleanForCheck = content.replace("⟦", "").replace("⟧", "")
    val isLatin = isMainlyLatin(cleanForCheck) || content.contains("⟦")
    val completedContent = resolveCompletedGermanText(content)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isLatin) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "•",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (content.contains("⟦")) {
                                InteractiveBlankLine(
                                    lineText = content,
                                    lineIdx = lineIdx,
                                    allSectionAnswers = allSectionAnswers,
                                    extraDistractorWords = extraDistractorWords,
                                    filledBlanks = filledBlanks,
                                    isRevealedAll = isRevealedAll
                                )
                            } else {
                                Text(
                                    text = content,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 23.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                if (onPlayAudio != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GrammarLineSpeakerButton(text = completedContent, onPlayAudio = onPlayAudio)
                    }
                }
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "•",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    Text(
                        text = content,
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

/**
 * Returns true if Latin letters outnumber Persian/Arabic letters in the string.
 */
fun isMainlyLatin(text: String): Boolean {
    var latinCount = 0
    var persianCount = 0
    for (ch in text) {
        if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in "ÄÖÜäöüß") {
            latinCount++
        } else if (ch in '\u0600'..'\u06FF') {
            persianCount++
        }
    }
    return latinCount > persianCount
}

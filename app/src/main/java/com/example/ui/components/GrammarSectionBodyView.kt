package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.UserProgressManager
import com.example.util.TtsManager

/**
 * FIX G & FIX O: Beautiful, readable rendering of grammar sections.
 * Splits section body (bodyDari) into lines and renders with correct direction,
 * stacked table rows for "|" separators, subheadings, and comfortable typography.
 * FIX O: Every German line gets its own speaker button in a fixed non-overlapping side column.
 */
@Composable
fun GrammarSectionCard(
    title: String,
    bodyDari: String,
    modifier: Modifier = Modifier,
    sectionNumber: Int? = null,
    onPlayAudio: ((String, Boolean) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Section Number / Badge & Title
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
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Body rendering
            GrammarSectionBodyView(bodyDari = bodyDari, onPlayAudio = onPlayAudio)
        }
    }
}

@Composable
fun GrammarSectionBodyView(
    bodyDari: String,
    modifier: Modifier = Modifier,
    onPlayAudio: ((String, Boolean) -> Unit)? = null
) {
    val rawLines = bodyDari.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var consecutiveEmptyCount = 0

        for (rawLine in rawLines) {
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
                renderTableRowWithPipe(line, onPlayAudio)
                continue
            }

            // RULE 3: Subheadings inside a section
            if (isSubheadingLine(line)) {
                renderSubheading(line)
                continue
            }

            // Check if line is a German item with Dari translation in parentheses
            // e.g. "- Ich heiße... (من نامیده می‌شوم / نام من ... است)"
            if (isGermanItemWithDariParenthesis(line)) {
                renderGermanWithDariParenthesis(line, onPlayAudio)
                continue
            }

            // Bullet or list items
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || line.startsWith("–")) {
                renderListItem(line, onPlayAudio)
                continue
            }

            // Standard paragraph: check dominant script direction
            if (isMainlyLatin(line)) {
                // German paragraph: LTR, left-aligned in a row with its fixed speaker column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = line,
                            fontSize = 14.5.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (onPlayAudio != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GrammarLineSpeakerButton(text = line, onPlayAudio = onPlayAudio)
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
    val cleanText = remember(text) { TtsManager.cleanGermanText(text) }
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
    onPlayAudio: ((String, Boolean) -> Unit)? = null
) {
    val segments = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.isEmpty()) return

    val latinSegments = segments.filter { isMainlyLatin(it) }
    val dariSegments = segments.filter { !isMainlyLatin(it) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = germanText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (onPlayAudio != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GrammarLineSpeakerButton(text = germanText, onPlayAudio = onPlayAudio)
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
        return isMainlyLatin(beforeParen) && !isMainlyLatin(insideParen)
    }
    return false
}

@Composable
private fun renderGermanWithDariParenthesis(
    line: String,
    onPlayAudio: ((String, Boolean) -> Unit)? = null
) {
    val openParen = line.indexOf('(')
    val closeParen = line.lastIndexOf(')')
    val beforeParen = line.substring(0, openParen).removePrefix("•").removePrefix("-").removePrefix("–").trim()
    val insideParen = line.substring(openParen + 1, closeParen).trim()

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
                    Text(
                        text = beforeParen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onPlayAudio != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GrammarLineSpeakerButton(text = beforeParen, onPlayAudio = onPlayAudio)
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
    onPlayAudio: ((String, Boolean) -> Unit)? = null
) {
    val content = line.removePrefix("•").removePrefix("-").removePrefix("*").removePrefix("–").trim()
    val isLatin = isMainlyLatin(content)

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
                        Text(
                            text = content,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 23.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                if (onPlayAudio != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GrammarLineSpeakerButton(text = content, onPlayAudio = onPlayAudio)
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

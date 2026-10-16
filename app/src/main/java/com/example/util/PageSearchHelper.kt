package com.example.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrammarTopic
import com.example.data.model.LessonData

/**
 * Fix X: Page-number search helper for all three libraries:
 * 1. درس‌ها (Lessons)
 * 2. گرامر (Grammar topics)
 * 3. کتاب گرامر (Grammar-book chapters)
 */
object PageSearchHelper {

    /**
     * Normalizes Persian (۰-۹) and Arabic-Indic (٠-٩) digits to standard Latin digits (0-9).
     */
    fun normalizeDigits(input: String): String {
        return input.map { ch ->
            when (ch) {
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
        }.joinToString("")
    }

    /**
     * Checks if the search query is a pure number (Latin or Persian/Arabic digits).
     */
    fun isPureNumberQuery(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return false
        return trimmed.all { it in '0'..'9' || it in '۰'..'۹' || it in '٠'..'٩' }
    }

    /**
     * Extracts the leading textbook page number from section titles.
     * Section titles carry the page, e.g.:
     * «صفحهٔ ۱۵۸ · ...», «صفحه 158 · ...», «صفحهٔ ۱۵۸: ...», «ص ۱۵۸ · ...», «Seite 158 · ...», «۱۵۸ · ...»
     */
    fun extractPageFromSectionTitle(title: String): String? {
        val norm = normalizeDigits(title).trim()
        val regex = Regex("""^(?:صفحه[\u200C\s]*[ءٔ]?|ص\.?|Seite)?\s*([0-9]+)(?:[\s.·:،,-]|$)""")
        val match = regex.find(norm)
        return match?.groupValues?.get(1)?.trimStart('0')?.ifEmpty { "0" }
    }

    /**
     * Checks if a section title begins with the target page number.
     */
    fun sectionMatchesPage(title: String, targetPage: String): Boolean {
        val extracted = extractPageFromSectionTitle(title) ?: return false
        val cleanTarget = normalizeDigits(targetPage).trim().trimStart('0').ifEmpty { "0" }
        return extracted == cleanTarget
    }

    enum class LibraryType(val titleDari: String) {
        LESSONS("درس‌ها"),
        GRAMMAR("گرامر"),
        GRAMMAR_BOOK("کتاب گرامر")
    }

    data class PageSectionResult(
        val libraryType: LibraryType,
        val parentId: String,
        val parentNumber: Int,
        val parentTitle: String,
        val sectionIndex: Int,
        val sectionTitle: String,
        val pageNumber: String
    )

    /**
     * Finds section(s) whose title begins with targetPage across all three libraries.
     */
    fun searchSectionsByPage(
        lessons: List<LessonData>,
        grammarBookLessons: List<LessonData>,
        grammarTopics: List<GrammarTopic>,
        pageQuery: String
    ): List<PageSectionResult> {
        val cleanTarget = normalizeDigits(pageQuery).trim().trimStart('0').ifEmpty { "0" }
        val results = mutableListOf<PageSectionResult>()

        // 1. Lessons library («درس‌ها»)
        for (lesson in lessons) {
            lesson.grammarSections.forEachIndexed { idx, section ->
                if (sectionMatchesPage(section.title, cleanTarget)) {
                    results.add(
                        PageSectionResult(
                            libraryType = LibraryType.LESSONS,
                            parentId = lesson.id,
                            parentNumber = lesson.number,
                            parentTitle = "درس ${lesson.number}: ${lesson.titleDari}",
                            sectionIndex = idx,
                            sectionTitle = section.title,
                            pageNumber = cleanTarget
                        )
                    )
                }
            }
        }

        // 2. Grammar topics library («گرامر»)
        for (topic in grammarTopics) {
            topic.sections.forEachIndexed { idx, section ->
                if (sectionMatchesPage(section.title, cleanTarget)) {
                    results.add(
                        PageSectionResult(
                            libraryType = LibraryType.GRAMMAR,
                            parentId = topic.id,
                            parentNumber = topic.number,
                            parentTitle = "گرامر ${topic.number}: ${topic.titleDari}",
                            sectionIndex = idx,
                            sectionTitle = section.title,
                            pageNumber = cleanTarget
                        )
                    )
                }
            }
        }

        // 3. Grammar-book chapters library («کتاب گرامر»)
        for (lesson in grammarBookLessons) {
            lesson.grammarSections.forEachIndexed { idx, section ->
                if (sectionMatchesPage(section.title, cleanTarget)) {
                    results.add(
                        PageSectionResult(
                            libraryType = LibraryType.GRAMMAR_BOOK,
                            parentId = lesson.id,
                            parentNumber = lesson.number,
                            parentTitle = "کتاب گرامر: درس ${lesson.number} - ${lesson.titleDari}",
                            sectionIndex = idx,
                            sectionTitle = section.title,
                            pageNumber = cleanTarget
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Standard text filtering for lessons (non-numeric query).
     */
    fun filterLessons(lessons: List<LessonData>, query: String): List<LessonData> {
        val q = query.trim()
        if (q.isEmpty()) return lessons
        return lessons.filter { lesson ->
            lesson.titleGerman.contains(q, ignoreCase = true) ||
            lesson.titleDari.contains(q, ignoreCase = true) ||
            lesson.number.toString() == q ||
            lesson.vocabulary.any { it.word.contains(q, ignoreCase = true) || it.meaningDari.contains(q, ignoreCase = true) } ||
            lesson.grammarSections.any { it.title.contains(q, ignoreCase = true) || it.bodyDari.contains(q, ignoreCase = true) }
        }
    }

    /**
     * Standard text filtering for grammar topics (non-numeric query).
     */
    fun filterGrammarTopics(topics: List<GrammarTopic>, query: String): List<GrammarTopic> {
        val q = query.trim()
        if (q.isEmpty()) return topics
        return topics.filter { topic ->
            topic.titleGerman.contains(q, ignoreCase = true) ||
            topic.titleDari.contains(q, ignoreCase = true) ||
            topic.number.toString() == q ||
            topic.sections.any { it.title.contains(q, ignoreCase = true) || it.bodyDari.contains(q, ignoreCase = true) }
        }
    }
}

@Composable
fun PageSearchResultCard(
    result: PageSearchHelper.PageSectionResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when (result.libraryType) {
        PageSearchHelper.LibraryType.LESSONS -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PageSearchHelper.LibraryType.GRAMMAR -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PageSearchHelper.LibraryType.GRAMMAR_BOOK -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("page_search_result_${result.libraryType.name.lowercase()}_${result.parentId}_${result.sectionIndex}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = result.libraryType.titleDari,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "صفحهٔ ${result.pageNumber}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = result.sectionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.parentTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "رفتن به این بخش ➔",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

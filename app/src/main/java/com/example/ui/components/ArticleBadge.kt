package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArticleType
import com.example.ui.theme.ArticleDasGreen
import com.example.ui.theme.ArticleDerBlue
import com.example.ui.theme.ArticleDieRed
import com.example.ui.theme.ArticlePluralOrange

@Composable
fun ArticleBadge(
    article: ArticleType,
    modifier: Modifier = Modifier,
    showGenderLabel: Boolean = true
) {
    if (article == ArticleType.NONE) return

    val (bgColor, textColor, displayLabel) = when (article) {
        ArticleType.DER -> Triple(
            ArticleDerBlue,
            Color.White,
            if (showGenderLabel) "der • مذکر" else "der"
        )
        ArticleType.DIE -> Triple(
            ArticleDieRed,
            Color.White,
            if (showGenderLabel) "die • مؤنث" else "die"
        )
        ArticleType.DAS -> Triple(
            ArticleDasGreen,
            Color.White,
            if (showGenderLabel) "das • خنثی" else "das"
        )
        ArticleType.PLURAL_DIE -> Triple(
            ArticlePluralOrange,
            Color.White,
            if (showGenderLabel) "die • جمع" else "die (جمع)"
        )
        ArticleType.NONE -> Triple(Color.Transparent, Color.Transparent, "")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayLabel,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.UserProgressManager
import com.example.util.TtsManager

/**
 * FIX F (1): Replace the fixed normal/۰.۶x button pair with:
 * - ONE speaker button (plays at the currently chosen speed)
 * - ONE compact speed control that shows the current speed (e.g. «۱.۰x» in Persian digits)
 *   and opens a small selector with speeds ۰.۵x, ۰.۷۵x, ۱.۰x, ۱.۲۵x, ۱.۵x.
 * - Occupies its own column (~88dp), never overlapping the German text (Fix D layout).
 */
@Composable
fun AudioSpeechButtons(
    textToSpeak: String,
    onPlayAudio: (String, Boolean) -> Unit, // text, isSlow (preserved for full backward compatibility)
    modifier: Modifier = Modifier,
    size: Int = 38,
    speedOverride: Float? = null,
    onSpeedChange: ((Float) -> Unit)? = null,
    translationDari: String? = null,
    onPlayBilingual: ((String, String?, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val appSpeed by progressManager.playbackSpeedFlow.collectAsState()
    val effectiveSpeed = speedOverride ?: appSpeed

    val loadingSentence by TtsManager.activeLoadingSentenceFlow.collectAsState()
    val cleanSentence = remember(textToSpeak) { TtsManager.cleanGermanText(textToSpeak) }
    val isLoading = loadingSentence != null && loadingSentence == cleanSentence

    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.testTag("audio_speech_controls"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // ONE Speaker button: plays at the currently chosen speed (shows spinner and disabled while loading)
        IconButton(
            onClick = {
                val isSlow = effectiveSpeed <= 0.75f
                if (onPlayBilingual != null) {
                    onPlayBilingual(textToSpeak, translationDari, isSlow)
                } else {
                    val fullPayload = if (!translationDari.isNullOrBlank()) {
                        "$textToSpeak ⟦$translationDari⟧"
                    } else {
                        textToSpeak
                    }
                    onPlayAudio(fullPayload, isSlow)
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("audio_btn_speaker")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size((size * 0.52).dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "شنیدن تلفظ با سرعت ${UserProgressManager.formatSpeedToPersian(effectiveSpeed)}",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size((size * 0.54).dp)
                )
            }
        }

        // ONE Compact speed control showing current speed in Persian digits & opening dropdown
        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expandedMenu = true }
                    .testTag("audio_btn_speed_selector")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = UserProgressManager.formatSpeedToPersian(effectiveSpeed),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false },
                modifier = Modifier
                    .widthIn(min = 115.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                UserProgressManager.SUPPORTED_SPEEDS.forEach { speedVal ->
                    val isSelected = speedVal == effectiveSpeed
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = UserProgressManager.formatSpeedToPersian(speedVal),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "انتخاب شده",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            if (onSpeedChange != null) {
                                onSpeedChange(speedVal)
                            } else {
                                progressManager.setPlaybackSpeed(speedVal)
                            }
                            expandedMenu = false
                        },
                        modifier = Modifier.testTag("speed_option_${speedVal}")
                    )
                }
            }
        }
    }
}

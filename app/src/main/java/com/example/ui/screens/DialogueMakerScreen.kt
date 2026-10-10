package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiChatService
import com.example.data.model.DialogueMakerLine
import com.example.data.model.SavedDialogue
import com.example.data.repository.SavedDialoguesRepository
import com.example.data.storage.UserProgressManager
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessGreen
import com.example.util.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Phase Q (3, 4, 5, 7): Dialogue Maker Screen («مکالمه‌ساز»)
 * - Topic input + «ساخت مکالمه» button using existing Gemini integration (keys/fallbacks/Dari errors).
 * - Produces natural A/B dialogue (~10-16 lines) with German, Persian-script pronunciation, and Dari translation.
 * - Fixed voices: Speaker A = Conrad (male), Speaker B = Klara (female).
 * - Per-line play button.
 * - «پخش کل مکالمه» sequentially with ~1s pauses and Stop control.
 * - «ذخیرهٔ این مکالمه» saves to local repository and caches audio files in files/saved_dialogues/<id>/
 */
@Composable
fun DialogueMakerScreen(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val savedRepo = remember { SavedDialoguesRepository.getInstance(context) }
    val geminiService = remember { GeminiChatService() }

    var topicInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<DialogueMakerLine>>(emptyList()) }
    var isSaved by remember { mutableStateOf(false) }

    // Playback state
    var currentlyPlayingIndex by remember { mutableIntStateOf(-1) }
    var isPlayingAll by remember { mutableStateOf(false) }
    var playAllJob by remember { mutableStateOf<Job?>(null) }

    // Track cached audio files per line index
    val lineAudioMap = remember { mutableStateMapOf<Int, File>() }

    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            playAllJob?.cancel()
            ttsManager.stop()
        }
    }

    fun stopPlayback() {
        playAllJob?.cancel()
        playAllJob = null
        isPlayingAll = false
        currentlyPlayingIndex = -1
        ttsManager.stop()
    }

    fun playSingleLine(index: Int, line: DialogueMakerLine) {
        if (currentlyPlayingIndex == index) {
            stopPlayback()
            return
        }
        stopPlayback()
        currentlyPlayingIndex = index
        coroutineScope.launch {
            try {
                // Synthesize & cache audio if not yet cached
                val existingFile = lineAudioMap[index]
                val speed = ttsManager.getEffectiveSpeed()
                if (existingFile == null) {
                    val synthFile = ttsManager.synthesizeSegmentWithVoice(
                        text = line.german,
                        isPersian = false,
                        speed = speed,
                        isMaleVoice = line.isSpeakerA
                    )
                    if (synthFile != null) {
                        lineAudioMap[index] = synthFile
                    }
                }
                ttsManager.playDialogueLineAndWait(
                    germanText = line.german,
                    dariText = line.translationDari,
                    isSpeakerA = line.isSpeakerA,
                    speed = speed,
                    existingAudioFile = lineAudioMap[index]
                )
            } finally {
                if (currentlyPlayingIndex == index) {
                    currentlyPlayingIndex = -1
                }
            }
        }
    }

    fun playAllLines() {
        if (lines.isEmpty()) return
        stopPlayback()
        isPlayingAll = true
        playAllJob = coroutineScope.launch {
            try {
                val speed = ttsManager.getEffectiveSpeed()
                for (i in lines.indices) {
                    if (!isActive) break
                    val line = lines[i]
                    currentlyPlayingIndex = i

                    // Synthesize/retrieve file
                    if (lineAudioMap[i] == null) {
                        val synth = ttsManager.synthesizeSegmentWithVoice(
                            text = line.german,
                            isPersian = false,
                            speed = speed,
                            isMaleVoice = line.isSpeakerA
                        )
                        if (synth != null) {
                            lineAudioMap[i] = synth
                        }
                    }

                    ttsManager.playDialogueLineAndWait(
                        germanText = line.german,
                        dariText = line.translationDari,
                        isSpeakerA = line.isSpeakerA,
                        speed = speed,
                        existingAudioFile = lineAudioMap[i]
                    )

                    if (!isActive) break
                    // Pause ~1 second between dialogue lines
                    delay(1000)
                }
            } catch (e: CancellationException) {
                // Cancelled
            } finally {
                isPlayingAll = false
                currentlyPlayingIndex = -1
            }
        }
    }

    fun startGenerating() {
        val topic = topicInput.trim()
        if (topic.isBlank()) {
            Toast.makeText(context, "لطفاً موضوع مکالمه را وارد کنید.", Toast.LENGTH_SHORT).show()
            return
        }

        val key = progressManager.getEffectiveGeminiApiKey().trim()
        if (key.isBlank()) {
            errorMessage = "کلید API جیمنای تنظیم نشده است. لطفاً ابتدا کلید خود را از بخش تنظیمات وارد نمایید."
            return
        }

        stopPlayback()
        isGenerating = true
        errorMessage = null
        statusMessage = "در حال نگارش مکالمهٔ طبیعی دونفره با جیمنای…"
        isSaved = false
        lineAudioMap.clear()

        coroutineScope.launch {
            val result = geminiService.generateDialogue(
                topic = topic,
                apiKey = key,
                onStatusUpdate = { status -> statusMessage = status }
            )

            isGenerating = false
            statusMessage = null

            result.onSuccess { generatedLines ->
                lines = generatedLines
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.message ?: "خطا در ساخت مکالمه."
            }
        }
    }

    fun saveCurrentDialogue() {
        if (lines.isEmpty() || isSaved) return
        coroutineScope.launch {
            val topic = topicInput.trim().ifBlank { "مکالمه آلمانی" }
            val nowStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date())
            val dialogue = SavedDialogue(
                id = UUID.randomUUID().toString(),
                topic = topic,
                createdAtFormatted = nowStr,
                timestamp = System.currentTimeMillis(),
                lines = lines
            )
            val result = savedRepo.saveDialogue(dialogue, lineAudioMap.toMap())
            if (result.isSuccess) {
                isSaved = true
                Toast.makeText(context, "مکالمه با موفقیت در کتابخانه ذخیره شد! ✨", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "خطا در ذخیره مکالمه.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dialogue_maker_screen")
    ) {
        // Topic Input Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مکالمه‌ساز هوشمند (A/B)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تولید گفتگوی دونفره با صدای کنراد (مرد) و کلارا (زن)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("موضوع مکالمه (مثلاً: در رستوران، اجاره خانه، خرید)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { startGenerating() },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_generate_dialogue"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(statusMessage ?: "در حال ساخت…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ساخت مکالمه", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Error message card
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = err,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Toolbar when lines exist
        if (lines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play all / Stop button
                FilledTonalButton(
                    onClick = {
                        if (isPlayingAll) stopPlayback() else playAllLines()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_play_all_dialogue"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPlayingAll) ErrorRed.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.2f),
                        contentColor = if (isPlayingAll) ErrorRed else Color(0xFFB45309)
                    )
                ) {
                    Icon(
                        imageVector = if (isPlayingAll) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlayingAll) "توقف پخش" else "پخش کل مکالمه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Save button
                OutlinedButton(
                    onClick = { saveCurrentDialogue() },
                    enabled = !isSaved,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_save_dialogue"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = if (isSaved) SuccessGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSaved) "ذخیره شد" else "ذخیرهٔ این مکالمه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSaved) SuccessGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dialogue lines list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val isPlayingThis = (currentlyPlayingIndex == index)
                DialogueLineCard(
                    line = line,
                    index = index,
                    isPlaying = isPlayingThis,
                    onPlay = { playSingleLine(index, line) }
                )
            }
        }
    }
}

@Composable
fun DialogueLineCard(
    line: DialogueMakerLine,
    index: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpeakerA = line.isSpeakerA
    val speakerColor = if (isSpeakerA) Color(0xFF2563EB) else Color(0xFFD946EF)
    val speakerBg = if (isSpeakerA) Color(0xFFEFF6FF) else Color(0xFFFDF4FF)
    val speakerName = if (isSpeakerA) "شخص A (کنراد)" else "شخص B (کلارا)"

    var showTranslation by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dialogue_line_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) speakerBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Speaker tag row + Audio play button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = speakerBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeakerA) Icons.Default.Male else Icons.Default.Female,
                                contentDescription = null,
                                tint = speakerColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = speakerName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = speakerColor
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showTranslation = !showTranslation },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showTranslation) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "نمایش یا پنهان‌سازی ترجمه",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) speakerColor else speakerBg)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "پخش صدا",
                            tint = if (isPlaying) Color.White else speakerColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // German text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = line.german,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Persian-script pronunciation
            if (line.pronunciation.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "تلفظ: ${line.pronunciation}",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dari translation
            if (showTranslation && line.translationDari.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = line.translationDari,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

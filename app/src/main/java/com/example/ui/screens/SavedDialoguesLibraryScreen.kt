package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedDialogue
import com.example.data.repository.SavedDialoguesRepository
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoPrimary
import com.example.util.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Phase Q (6, 7): Saved Dialogues Library Screen («کتابخانه مکالمات»)
 * - List of saved dialogues (topic + date).
 * - Opening one shows full dialogue with per-line play + play-all.
 * - Offline audio cached in files/saved_dialogues/<id>/ is checked and played without cloud calls!
 * - Delete dialog / entry deletion supported.
 */
@Composable
fun SavedDialoguesLibraryScreen(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { SavedDialoguesRepository.getInstance(context) }
    val savedDialogues by repo.dialoguesFlow.collectAsState()

    var selectedDialogueId by remember { mutableStateOf<String?>(null) }
    var dialogueToDelete by remember { mutableStateOf<SavedDialogue?>(null) }

    val activeDialogue = savedDialogues.firstOrNull { it.id == selectedDialogueId }

    if (activeDialogue != null) {
        BackHandler {
            selectedDialogueId = null
        }
        SavedDialogueDetailView(
            dialogue = activeDialogue,
            ttsManager = ttsManager,
            repo = repo,
            onBack = { selectedDialogueId = null },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("saved_dialogues_library_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IndigoPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "کتابخانه مکالمات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${savedDialogues.size} مکالمهٔ ذخیره‌شده با صدای آفلاین",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (savedDialogues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "هنوز هیچ مکالمه‌ای ذخیره نشده است.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "از برگهٔ «مکالمه‌ساز» می‌توانید هر مکالمه را با صدای کنراد و کلارا ذخیره کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(savedDialogues, key = { it.id }) { dialogue ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_dialogue_card_${dialogue.id}")
                            .clickable { selectedDialogueId = dialogue.id },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dialogue.topic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${dialogue.lines.size} خط گفتگو • ${dialogue.createdAtFormatted}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { dialogueToDelete = dialogue },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف مکالمه",
                                    tint = ErrorRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (dialogueToDelete != null) {
            AlertDialog(
                onDismissRequest = { dialogueToDelete = null },
                title = { Text("حذف مکالمه", fontWeight = FontWeight.Bold) },
                text = { Text("آیا از حذف مکالمه «${dialogueToDelete?.topic}» و فایل‌های صوتی آن اطمینان دارید؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogueToDelete?.let { d ->
                                coroutineScope.launch {
                                    repo.deleteDialogue(d.id)
                                    Toast.makeText(context, "مکالمه حذف شد.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            dialogueToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogueToDelete = null }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedDialogueDetailView(
    dialogue: SavedDialogue,
    ttsManager: TtsManager,
    repo: SavedDialoguesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentlyPlayingIndex by remember { mutableIntStateOf(-1) }
    var isPlayingAll by remember { mutableStateOf(false) }
    var playAllJob by remember { mutableStateOf<Job?>(null) }

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

    fun playLine(index: Int) {
        if (currentlyPlayingIndex == index) {
            stopPlayback()
            return
        }
        stopPlayback()
        currentlyPlayingIndex = index
        coroutineScope.launch {
            try {
                val line = dialogue.lines[index]
                val offlineFile = repo.findAudioForLine(dialogue.id, index)
                val speed = ttsManager.getEffectiveSpeed()
                ttsManager.playDialogueLineAndWait(
                    germanText = line.german,
                    dariText = line.translationDari,
                    isSpeakerA = line.isSpeakerA,
                    speed = speed,
                    existingAudioFile = offlineFile
                )
            } finally {
                if (currentlyPlayingIndex == index) {
                    currentlyPlayingIndex = -1
                }
            }
        }
    }

    fun playAll() {
        if (dialogue.lines.isEmpty()) return
        stopPlayback()
        isPlayingAll = true
        playAllJob = coroutineScope.launch {
            try {
                val speed = ttsManager.getEffectiveSpeed()
                for (i in dialogue.lines.indices) {
                    if (!isActive) break
                    val line = dialogue.lines[i]
                    currentlyPlayingIndex = i

                    val offlineFile = repo.findAudioForLine(dialogue.id, i)
                    ttsManager.playDialogueLineAndWait(
                        germanText = line.german,
                        dariText = line.translationDari,
                        isSpeakerA = line.isSpeakerA,
                        speed = speed,
                        existingAudioFile = offlineFile
                    )

                    if (!isActive) break
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("saved_dialogue_detail_view")
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dialogue.topic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${dialogue.lines.size} خط گفتگو • ${dialogue.createdAtFormatted}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play all / Stop button
            FilledTonalButton(
                onClick = { if (isPlayingAll) stopPlayback() else playAll() },
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isPlayingAll) ErrorRed.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.2f),
                    contentColor = if (isPlayingAll) ErrorRed else Color(0xFFB45309)
                )
            ) {
                Icon(
                    imageVector = if (isPlayingAll) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlayingAll) "توقف" else "پخش کل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(dialogue.lines) { index, line ->
                val isPlayingThis = (currentlyPlayingIndex == index)
                DialogueLineCard(
                    line = line,
                    index = index,
                    isPlaying = isPlayingThis,
                    onPlay = { playLine(index) }
                )
            }
        }
    }
}

package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.ChatMessage
import com.example.data.gemini.GeminiChatService
import com.example.data.model.GrammarTopic
import com.example.data.model.LessonData
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun GeminiChatScreen(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onImportLessonJson: (String) -> Result<LessonData>,
    onPlayAudio: (String, Boolean) -> Unit,
    onImportGrammarJson: ((String) -> Result<GrammarTopic>)? = null,
    onTestVoice: (suspend (String, Float) -> Result<String>)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val chatService = remember { GeminiChatService() }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "سلام! من دستیار هوشمند و معلم آلمانی شما در سطح A1 هستم 🇩🇪\nهر سوالی درباره واژگان، گرامر، ساختن جمله یا تمرین‌های جدید دارید بپرسید. تمام توضیحات را به زبان دری همراه با تلفظ به خط فارسی ارائه می‌دهم."
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "یک مبحث گرامر کاربردی با تمرین و مثال بساز",
        "۱۰ سوال تمرینی سطح A1 برایم بساز",
        "۱۰ جمله درباره سفارش غذا با تلفظ بساز",
        "تفاوت der و die و das را به دری توضیح بده",
        "صرف فعل haben و sein را با مثال یاد بده"
    )

    fun sendUserPrompt(prompt: String) {
        if (prompt.isBlank() || isLoading) return
        val userMsg = ChatMessage(isUser = true, text = prompt)
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            val result = chatService.sendMessage(
                userMessage = prompt,
                conversationHistory = messages,
                apiKey = currentApiKey
            )

            isLoading = false
            if (result.isSuccess) {
                val responseText = result.getOrThrow()
                val extractedJson = GeminiChatService.extractLessonJson(responseText)
                messages.add(
                    ChatMessage(
                        isUser = false,
                        text = responseText,
                        extractedLessonJson = extractedJson
                    )
                )
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "خطای ناشناخته در ارتباط با جیمنای"
                messages.add(
                    ChatMessage(
                        isUser = false,
                        text = "متأسفانه خطایی رخ داد: $error\nاگر کلید API تنظیم نشده است، لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید."
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("gemini_chat_screen")
    ) {
        // Chat Top Bar with API Key Button
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "استاد هوشمند آلمانی (جیمنای)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentApiKey.isNotEmpty()) "کلید API فعال است ✓ (تلفظ طبیعی جیمنای)" else "نیاز به کلید API",
                            fontSize = 11.sp,
                            color = if (currentApiKey.isNotEmpty()) SuccessGreen else AccentAmber
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Visible voice status (FIX A (2))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentApiKey.isNotEmpty()) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (currentApiKey.isNotEmpty()) "صدا: جیمنای ✨" else "صدا: گوشی",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentApiKey.isNotEmpty()) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // API Key Settings Button
                    OutlinedButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("btn_gemini_api_key_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "کلید API جیمنای",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کلید API جیمنای", fontSize = 12.sp)
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onImportLessonJson = { json ->
                        if (json.contains("\"sections\"") && !json.contains("\"vocabulary\"") && onImportGrammarJson != null) {
                            val res = onImportGrammarJson(json)
                            if (res.isSuccess) {
                                Toast.makeText(context, "مبحث گرامر جدید با موفقیت به بخش گرامر اضافه شد! ✓", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "خطا در واردسازی گرامر: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val res = onImportLessonJson(json)
                            if (res.isSuccess) {
                                Toast.makeText(context, "درس جدید با موفقیت به بخش دروس اضافه شد! ✓", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "خطا: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "متن در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "جیمنای در حال تدریس و نوشتن پاسخ است...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                FilterChip(
                    selected = false,
                    onClick = { sendUserPrompt(prompt) },
                    label = { Text(prompt, fontSize = 12.sp) }
                )
            }
        }

        // Input Field and Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("سوال یا درخواست خود را بنویسید...", fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gemini_chat_input"),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = { sendUserPrompt(inputText) },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(50.dp)
                    .testTag("gemini_chat_send_button"),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "ارسال پیام",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // API Key Settings Dialog
    if (showApiKeyDialog) {
        VoiceSettingsDialog(
            currentKey = currentApiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { newKey ->
                onSaveApiKey(newKey)
                showApiKeyDialog = false
            },
            onTestVoice = { key, speed ->
                onTestVoice?.invoke(key, speed) ?: Result.failure(Exception("سرویس صوتی در دسترس نیست"))
            }
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onImportLessonJson: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 22.sp
                )

                // Copy and Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCopyText(message.text) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی متن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // If Lesson or Grammar JSON was detected, offer 1-tap import!
                if (!isUser && message.extractedLessonJson != null) {
                    val isGrammar = message.extractedLessonJson.contains("\"sections\"") && !message.extractedLessonJson.contains("\"vocabulary\"")
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onImportLessonJson(message.extractedLessonJson) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_import_from_chat"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGrammar) "افزودن به مباحث گرامر" else "افزودن به درس‌های من",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}


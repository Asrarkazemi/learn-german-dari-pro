package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.ChatMessage
import com.example.data.gemini.GeminiChatException
import com.example.data.gemini.GeminiChatService
import com.example.data.model.GrammarTopic
import com.example.data.model.LessonData
import com.example.data.storage.UserProgressManager
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

/**
 * FIX J: Resilient Gemini chat with clean Dari errors and model fallback.
 * 1) Pinned unclipped header row
 * 2) Automated model fallback & retries with live Dari status
 * 3) Clean Dari error messages (no raw English text) with «تلاش دوباره» button
 * 4) Key hint displayed ONLY for genuine API key errors
 */
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
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val azureKey by progressManager.azureSpeechKeyFlow.collectAsState()
    val voiceProvider by progressManager.voiceProviderFlow.collectAsState()
    val activeVoiceChipText = remember(voiceProvider, currentApiKey, azureKey) {
        progressManager.getActiveVoiceChipText()
    }
    val isVoiceActive = activeVoiceChipText != "صدا: گوشی"

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
    var loadingStatusText by remember { mutableStateOf("جیمنای در حال تدریس و نوشتن پاسخ است...") }
    var lastUserPrompt by remember { mutableStateOf("") }
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
        lastUserPrompt = prompt
        inputText = ""
        isLoading = true
        loadingStatusText = "جیمنای در حال تدریس و نوشتن پاسخ است..."

        coroutineScope.launch {
            val result = chatService.sendMessage(
                userMessage = prompt,
                conversationHistory = messages,
                apiKey = currentApiKey,
                onStatusUpdate = { status ->
                    loadingStatusText = status
                }
            )

            isLoading = false
            if (result.isSuccess) {
                val responseText = result.getOrThrow()
                val extractedJson = GeminiChatService.extractLessonJson(responseText)
                messages.add(
                    ChatMessage(
                        isUser = false,
                        text = responseText,
                        extractedLessonJson = extractedJson,
                        isError = false,
                        canRetry = false,
                        isKeyError = false
                    )
                )
            } else {
                val exception = result.exceptionOrNull()
                val isKeyErr = exception is GeminiChatException.KeyErrorException ||
                        GeminiChatService.isKeyError(0, exception?.message.orEmpty())

                val errorText = when (exception) {
                    is GeminiChatException.ServerBusyException -> {
                        "سرور جیمنای فعلاً شلوغ است؛ چند دقیقۀ دیگر دوباره کوشش کنید."
                    }
                    is GeminiChatException.KeyErrorException -> {
                        "کلید API جیمنای نامعتبر است یا هنوز تنظیم نشده است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید."
                    }
                    is GeminiChatException.NetworkErrorException -> {
                        "خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید."
                    }
                    else -> {
                        if (isKeyErr) {
                            "کلید API جیمنای نامعتبر است یا هنوز تنظیم نشده است. لطفاً از دکمه تنظیم کلید در بالای صفحه کلید رایگان خود را وارد کنید."
                        } else {
                            "سرور جیمنای فعلاً شلوغ است؛ چند دقیقۀ دیگر دوباره کوشش کنید."
                        }
                    }
                }

                messages.add(
                    ChatMessage(
                        isUser = false,
                        text = errorText,
                        isError = true,
                        canRetry = !isKeyErr,
                        isKeyError = isKeyErr
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("gemini_chat_screen")
    ) {
        // 1) HEADER PINNED AT TOP: Proper padding, single compact unclipped row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teacher Identity & Status
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "استاد هوشمند آلمانی (جیمنای)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (currentApiKey.isNotEmpty()) "کلید API فعال است ✓" else "نیاز به کلید API",
                            fontSize = 11.sp,
                            color = if (currentApiKey.isNotEmpty()) SuccessGreen else AccentAmber,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Voice status chip & Settings button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Voice chip (clickable to open voice settings)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVoiceActive) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showApiKeyDialog = true }
                    ) {
                        Text(
                            text = activeVoiceChipText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVoiceActive) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // API Key Settings Button
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .testTag("btn_gemini_api_key_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "کلید API جیمنای",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            thickness = 1.dp
        )

        // 2) MESSAGE LIST: Fills middle with weight(1f), messages anchored to top, no stretching
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    },
                    onRetryPrompt = {
                        if (lastUserPrompt.isNotBlank()) {
                            sendUserPrompt(lastUserPrompt)
                        }
                    },
                    onOpenKeySettings = {
                        showApiKeyDialog = true
                    }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = loadingStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3) SUGGESTION CHIPS: Sits directly above input row
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
                    label = { Text(prompt, fontSize = 11.5.sp) }
                )
            }
        }

        // 4) BOTTOM AREA: Compact input row pinned at the bottom, directly above bottom navigation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("سوال یا درخواست خود را بنویسید...", fontSize = 13.5.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_chat_input"),
                        shape = RoundedCornerShape(22.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = { sendUserPrompt(inputText) },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("gemini_chat_send_button"),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال پیام",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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

/**
 * Clean message bubble with strict content-hugging dimensions.
 * User bubbles: tight wrap without redundant copy bar.
 * Assistant bubbles: bordered surface with compact action bar; clean Dari error messages with retry/settings button.
 */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    onImportLessonJson: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onRetryPrompt: () -> Unit = {},
    onOpenKeySettings: () -> Unit = {}
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            // User message bubble: strictly hugs the text
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                ),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .wrapContentHeight()
                    .widthIn(min = 36.dp, max = 290.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            // Assistant message bubble
            val borderColor = if (message.isError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                ),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .wrapContentHeight()
                    .widthIn(min = 50.dp, max = 340.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .wrapContentHeight()
                ) {
                    Text(
                        text = message.text,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    // Error Actions: Retry button or Open Key Settings button
                    if (message.isError) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (message.canRetry) {
                            OutlinedButton(
                                onClick = onRetryPrompt,
                                modifier = Modifier.testTag("btn_retry_chat_message"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تلاش دوباره", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (message.isKeyError) {
                            Button(
                                onClick = onOpenKeySettings,
                                modifier = Modifier.testTag("btn_open_key_from_chat"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنظیم کلید API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Standard Assistant Actions: Compact Copy button
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
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
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // 1-Tap Import Lesson / Grammar JSON button if extracted
                        if (message.extractedLessonJson != null) {
                            val isGrammar = message.extractedLessonJson.contains("\"sections\"") &&
                                    !message.extractedLessonJson.contains("\"vocabulary\"")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onImportLessonJson(message.extractedLessonJson) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("btn_import_from_chat"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
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
    }
}

package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.UserProgressManager
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.util.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phase 2 — amended Fix P: Dual-Provider Voice Settings Dialog:
 * 1) Voice-provider selector «Microsoft Azure» / «Google Gemini».
 * 2) Azure settings: masked key field (azure_speech_key), region (azure_speech_region, default eastus),
 *    voice selector: de-DE-ConradNeural «مرد — کنراد» DEFAULT / de-DE-KlaraNeural «زن — کلارا».
 * 3) Gemini settings: masked key field (gemini_api_key), voices by gender (Charon / Kore).
 * 4) Status chip: «صدا: آژور ✨» / «صدا: جیمنای ✨» / «صدا: گوشی».
 * 5) Per-provider persisted request counters.
 * 6) Bilingual test voice button with honest short Dari error messages.
 * 7) Voice Library: live sentence count & MB stats + clear + ZIP export/import (.wav & .mp3).
 */
@Composable
fun VoiceSettingsDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTestVoice: suspend (key: String, speed: Float) -> Result<String>,
    ttsManager: TtsManager? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val effectiveTts = ttsManager ?: remember { TtsManager(context) }

    val currentSpeed by progressManager.playbackSpeedFlow.collectAsState()
    val dailyGeminiRequestsCount by progressManager.dailyGeminiRequestsFlow.collectAsState()
    val dailyAzureRequestsCount by progressManager.dailyAzureRequestsFlow.collectAsState()

    var selectedProvider by remember { mutableStateOf(progressManager.getVoiceProvider()) }

    var geminiKeyText by remember { mutableStateOf(currentKey.ifEmpty { progressManager.getEffectiveGeminiApiKey() }) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }

    var azureKeyText by remember { mutableStateOf(progressManager.getAzureSpeechKey()) }
    var isAzureKeyVisible by remember { mutableStateOf(false) }
    var azureRegionText by remember { mutableStateOf(progressManager.getAzureSpeechRegion()) }
    var selectedVoice by remember { mutableStateOf(progressManager.getAzureSpeechVoice()) }

    var isTesting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<String?>(null) }
    var testResultError by remember { mutableStateOf<String?>(null) }

    // Live Voice Library statistics
    var libraryStats by remember { mutableStateOf(TtsManager.getVoiceLibraryStats(context)) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Import result report
    var importReportMessage by remember { mutableStateOf<String?>(null) }
    var isImportError by remember { mutableStateOf(false) }

    // Active voice status chip calculation
    val activeChipText = remember(selectedProvider, azureKeyText, geminiKeyText) {
        val hasAzure = azureKeyText.trim().isNotBlank()
        val hasGemini = geminiKeyText.trim().isNotBlank()
        when {
            selectedProvider == UserProgressManager.VOICE_PROVIDER_AZURE && hasAzure -> "صدا: آژور ✨"
            selectedProvider == UserProgressManager.VOICE_PROVIDER_GEMINI && hasGemini -> "صدا: جیمنای ✨"
            hasAzure -> "صدا: آژور ✨"
            hasGemini -> "صدا: جیمنای ✨"
            else -> "صدا: گوشی"
        }
    }

    // File picker launcher for ZIP import
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            importReportMessage = null
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val result = TtsManager.importVoiceLibraryFromZip(context, inputStream)
                        withContext(Dispatchers.Main) {
                            isImporting = false
                            libraryStats = TtsManager.getVoiceLibraryStats(context)
                            if (result.isSuccess) {
                                val report = result.formatDariReport()
                                importReportMessage = report
                                isImportError = false
                                Toast.makeText(context, report, Toast.LENGTH_LONG).show()
                            } else {
                                val err = result.errorMessage ?: "خطا در وارد کردن فایل زیپ."
                                importReportMessage = err
                                isImportError = true
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        }
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            isImporting = false
                            importReportMessage = "خطا در باز کردن فایل زیپ انتخاب‌شده."
                            isImportError = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isImporting = false
                        val errMsg = "فایل زیپ نامعتبر یا آسیب‌دیده است. حافظهٔ صدا بدون تغییر باقی ماند."
                        importReportMessage = errMsg
                        isImportError = true
                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun mapErrorToDari(rawError: String?, provider: String): String {
        if (rawError.isNullOrBlank()) {
            return "خطا در ارتباط با سرویس صوتی؛ لطفاً بعداً دوباره امتحان کنید."
        }
        val lower = rawError.lowercase()
        return when {
            lower.contains("401") || lower.contains("403") ||
                    lower.contains("api_key_invalid") || lower.contains("invalid api key") ||
                    lower.contains("authentication") || rawError.contains("کلید") -> {
                if (provider == UserProgressManager.VOICE_PROVIDER_AZURE) {
                    "کلید API سرویس صوتی آژور (Azure) نامعتبر است. لطفاً کلید صحیح را در تنظیمات وارد نمایید."
                } else {
                    "کلید API جیمنای نامعتبر است. لطفاً کلید صحیح خود را از Google AI Studio وارد نمایید."
                }
            }
            lower.contains("quota") || lower.contains("429") || lower.contains("resource_exhausted") -> {
                "سهمیهٔ این سرویس صوتی موقتاً به پایان رسیده است؛ سیستم به صورت خودکار از سرویس جایگزین استفاده می‌کند."
            }
            lower.contains("timeout") || lower.contains("connect") || lower.contains("network") || lower.contains("internet") -> {
                "خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید."
            }
            else -> {
                rawError
            }
        }
    }

    // Confirmation dialog for clearing permanent voice library
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پاک کردن حافظهٔ صدا",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید تمام فایل‌های صوتی ذخیره‌شده (${libraryStats.count} جمله) را پاک کنید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        TtsManager.clearVoiceLibrary(context)
                        libraryStats = TtsManager.getVoiceLibraryStats(context)
                        importReportMessage = null
                        Toast.makeText(context, "حافظهٔ صدا با موفقیت پاک شد.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("btn_confirm_clear_voice_library")
                ) {
                    Text("پاک کردن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_clear_voice_library")
                ) {
                    Text("انصراف")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تنظیمات پیشرفتهٔ صدا و گوینده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "سیستم صوتی دوگانه و دوزبانه (آلمانی + ترجمهٔ دری). در صورت بروز خطا در سرویس انتخاب‌شده، سرویس دیگر و سپس صدای گوشی به صورت خودکار جایگزین می‌گردد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PROVIDER SELECTOR: Microsoft Azure / Google Gemini
                Text(
                    text = "موتور اصلی تولید صدا:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isAzure = selectedProvider == UserProgressManager.VOICE_PROVIDER_AZURE
                    FilterChip(
                        selected = isAzure,
                        onClick = {
                            selectedProvider = UserProgressManager.VOICE_PROVIDER_AZURE
                            testResultSuccess = null
                            testResultError = null
                        },
                        label = {
                            Text(
                                text = "Microsoft Azure",
                                fontWeight = if (isAzure) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("provider_chip_azure"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    val isGemini = selectedProvider == UserProgressManager.VOICE_PROVIDER_GEMINI
                    FilterChip(
                        selected = isGemini,
                        onClick = {
                            selectedProvider = UserProgressManager.VOICE_PROVIDER_GEMINI
                            testResultSuccess = null
                            testResultError = null
                        },
                        label = {
                            Text(
                                text = "Google Gemini",
                                fontWeight = if (isGemini) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("provider_chip_gemini"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PROVIDER-SPECIFIC SETTINGS
                if (selectedProvider == UserProgressManager.VOICE_PROVIDER_AZURE) {
                    // AZURE SPEECH SETTINGS
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "تنظیمات سرویس Microsoft Azure Speech:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Azure Speech Key with show/hide
                            OutlinedTextField(
                                value = azureKeyText,
                                onValueChange = {
                                    azureKeyText = it
                                    testResultSuccess = null
                                    testResultError = null
                                },
                                label = { Text("کلید اشتراک Azure Speech Key") },
                                placeholder = { Text("مثال: 32 کاراکتر هگزادسیمال") },
                                visualTransformation = if (isAzureKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isAzureKeyVisible = !isAzureKeyVisible },
                                        modifier = Modifier.testTag("btn_toggle_azure_key_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isAzureKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isAzureKeyVisible) "مخفی‌سازی کلید" else "نمایش کلید",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("azure_speech_key_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Azure Region
                            OutlinedTextField(
                                value = azureRegionText,
                                onValueChange = {
                                    azureRegionText = it
                                    testResultSuccess = null
                                    testResultError = null
                                },
                                label = { Text("منطقه (Region)") },
                                placeholder = { Text("eastus") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("azure_speech_region_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Voice Selector: Conrad / Klara
                            Text(
                                text = "گوینده آلمانی و فارسی:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isConrad = selectedVoice == UserProgressManager.AZURE_VOICE_CONRAD
                                FilterChip(
                                    selected = isConrad,
                                    onClick = {
                                        selectedVoice = UserProgressManager.AZURE_VOICE_CONRAD
                                        testResultSuccess = null
                                        testResultError = null
                                    },
                                    label = {
                                        Text(
                                            text = "مرد — کنراد",
                                            fontSize = 12.sp,
                                            fontWeight = if (isConrad) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("azure_voice_conrad_chip")
                                )

                                val isKlara = selectedVoice == UserProgressManager.AZURE_VOICE_KLARA
                                FilterChip(
                                    selected = isKlara,
                                    onClick = {
                                        selectedVoice = UserProgressManager.AZURE_VOICE_KLARA
                                        testResultSuccess = null
                                        testResultError = null
                                    },
                                    label = {
                                        Text(
                                            text = "زن — کلارا",
                                            fontSize = 12.sp,
                                            fontWeight = if (isKlara) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("azure_voice_klara_chip")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val voiceHint = if (selectedVoice == UserProgressManager.AZURE_VOICE_CONRAD) {
                                "گوینده آلمانی: ConradNeural | گوینده فارسی/دری: FaridNeural"
                            } else {
                                "گوینده آلمانی: KlaraNeural | گوینده فارسی/دری: DilaraNeural"
                            }
                            Text(
                                text = voiceHint,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Daily Azure requests counter
                            val azureDailyPersian = UserProgressManager.toPersianDigits("$dailyAzureRequestsCount")
                            Text(
                                text = "درخواست‌های امروز به آژور: $azureDailyPersian",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("daily_azure_requests_text")
                            )
                        }
                    }
                } else {
                    // GEMINI SETTINGS
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "تنظیمات سرویس Google Gemini:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Gemini API key field with show/hide
                            OutlinedTextField(
                                value = geminiKeyText,
                                onValueChange = {
                                    geminiKeyText = it
                                    testResultSuccess = null
                                    testResultError = null
                                },
                                label = { Text("کلید API جیمنای") },
                                placeholder = { Text("AIzaSy...") },
                                visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isGeminiKeyVisible = !isGeminiKeyVisible },
                                        modifier = Modifier.testTag("btn_toggle_gemini_key_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isGeminiKeyVisible) "مخفی‌سازی کلید" else "نمایش کلید",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_api_key_dialog_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "گوینده جیمنای: به صورت خودکار بر اساس جنسیت گوینده تنظیم می‌شود (مرد: Charon / زن: Kore).",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Daily Gemini API requests counter
                            val geminiDailyPersian = UserProgressManager.toPersianDigits("$dailyGeminiRequestsCount")
                            Text(
                                text = "درخواست‌های امروز به جیمنای: $geminiDailyPersian",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("daily_gemini_requests_text")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ACTIVE STATUS CHIP: «صدا: آژور ✨» / «صدا: جیمنای ✨» / «صدا: گوشی»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت گوینده فعال:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeChipText != "صدا: گوشی") Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = activeChipText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeChipText != "صدا: گوشی") Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PLAYBACK SPEED SELECTOR (0.5x - 1.5x)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تنظیم سرعت پخش سراسری صدا:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Speed chips: ۰.۵x, ۰.۷۵x, ۱.۰x, ۱.۲۵x, ۱.۵x
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UserProgressManager.SUPPORTED_SPEEDS.forEach { speedVal ->
                        val isSelected = speedVal == currentSpeed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                progressManager.setPlaybackSpeed(speedVal)
                                testResultSuccess = null
                                testResultError = null
                            },
                            label = {
                                Text(
                                    text = UserProgressManager.formatSpeedToPersian(speedVal),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_speed_chip_${speedVal}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TEST VOICE BUTTON (Bilingual Test)
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResultSuccess = null
                        testResultError = null
                        coroutineScope.launch {
                            val result = effectiveTts.testVoice(
                                sampleGerman = "Guten Tag! Ich lerne Deutsch.",
                                sampleDari = "روز بخیر! من آلمانی یاد می‌گیرم.",
                                speed = currentSpeed,
                                providerOverride = selectedProvider,
                                keyOverride = if (selectedProvider == UserProgressManager.VOICE_PROVIDER_AZURE) azureKeyText else geminiKeyText,
                                regionOverride = azureRegionText,
                                voiceOverride = selectedVoice
                            )
                            isTesting = false
                            if (result.isSuccess) {
                                testResultSuccess = result.getOrNull()
                                testResultError = null
                                libraryStats = TtsManager.getVoiceLibraryStats(context)
                            } else {
                                val rawErr = result.exceptionOrNull()?.message
                                testResultError = mapErrorToDari(rawErr, selectedProvider)
                                testResultSuccess = null
                            }
                        }
                    },
                    enabled = !isTesting && !isExporting && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_gemini_voice"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("در حال آزمایش صدا...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "آزمایش صدای دو زبانه (آلمانی + دری)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Success Card
                AnimatedVisibility(visible = testResultSuccess != null) {
                    testResultSuccess?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = SuccessGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Error Card
                AnimatedVisibility(visible = testResultError != null) {
                    testResultError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PERMANENT IN-APP VOICE LIBRARY SECTION
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_library_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حافظهٔ صدای برنامه (ذخیرهٔ دائمی)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Exact required text: «حافظهٔ صدا: N جمله ذخیره شده (X مگابایت)»
                        Text(
                            text = "حافظهٔ صدا: ${libraryStats.count} جمله ذخیره شده (${libraryStats.formattedSize} مگابایت)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("voice_library_stats_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "جملاتی که یک‌بار با صدای آژور یا جیمنای پخش شوند در حافظه داخلی ذخیره شده و دفعات بعد با سرعت بالا و بدون نیاز به اینترنت پخش می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // EXPORT BUTTON: «خروجی گرفتن از حافظهٔ صدا»
                        OutlinedButton(
                            onClick = {
                                if (libraryStats.count == 0) {
                                    Toast.makeText(
                                        context,
                                        "حافظهٔ صدا خالی است؛ هنوز جمله‌ای برای خروجی گرفتن ذخیره نشده است.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@OutlinedButton
                                }
                                isExporting = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val zipFile = TtsManager.exportVoiceLibraryToZip(context)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        if (zipFile != null && zipFile.exists() && zipFile.length() > 0) {
                                            TtsManager.shareVoiceLibraryZip(context, zipFile)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "خطا در ایجاد فایل زیپ خروجی.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isTesting && !isExporting && !isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_voice_library"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("در حال آماده‌سازی فایل زیپ...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "خروجی گرفتن از حافظهٔ صدا",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // IMPORT BUTTON: «وارد کردن حافظهٔ صدا»
                        OutlinedButton(
                            onClick = {
                                importReportMessage = null
                                openDocumentLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            enabled = !isTesting && !isExporting && !isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_import_voice_library"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("در حال استخراج و افزودن جملات...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "وارد کردن حافظهٔ صدا",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // DARI REPORT CARD: «N جملهٔ تازه اضافه شد — M جمله از قبل بود»
                        AnimatedVisibility(visible = importReportMessage != null) {
                            importReportMessage?.let { reportText ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isImportError) SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .testTag("import_report_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (!isImportError) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (!isImportError) SuccessGreen else ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = reportText,
                                            color = if (!isImportError) SuccessGreen else ErrorRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("import_report_text")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // CLEAR VOICE LIBRARY BUTTON
                        TextButton(
                            onClick = { showClearConfirmDialog = true },
                            enabled = !isTesting && !isExporting && !isImporting && libraryStats.count > 0,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("btn_clear_voice_library")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (libraryStats.count > 0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "پاک کردن حافظهٔ صدا",
                                fontSize = 12.sp,
                                color = if (libraryStats.count > 0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    progressManager.setVoiceProvider(selectedProvider)
                    progressManager.saveAzureSpeechKey(azureKeyText.trim())
                    progressManager.setAzureSpeechRegion(azureRegionText.trim())
                    progressManager.setAzureSpeechVoice(selectedVoice)
                    onSave(geminiKeyText.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_voice_settings")
            ) {
                Text("ذخیره تنظیمات")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_dismiss_voice_settings")
            ) {
                Text("انصراف")
            }
        }
    )
}

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
 * FIX K & FIX L: Quota-aware VoiceSettingsDialog with:
 * 1) Password-masked API key field with show/hide eye toggle
 * 2) Honest status chip: shows «صدا: گوشی» when in quota cooldown
 * 3) Prominent Dari note about quota limits and device voice fallback
 * 4) Test button with clean Dari error mapping (raw English errors excluded)
 * 5) PERMANENT VOICE LIBRARY: Live count & size «حافظهٔ صدا: N جمله ذخیره شده (X مگابایت)»
 * 6) EXPORT VOICE LIBRARY: Button «خروجی گرفتن از حافظهٔ صدا» -> voice-library.zip share sheet
 * 7) IMPORT VOICE LIBRARY: Button «وارد کردن حافظهٔ صدا» -> file picker + merge-by-filename + zip-slip defense
 *    + exact Dari report: «N جملهٔ تازه اضافه شد — M جمله از قبل بود»
 * 8) Clear library button with confirmation dialog
 */
@Composable
fun VoiceSettingsDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTestVoice: suspend (key: String, speed: Float) -> Result<String>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val currentSpeed by progressManager.playbackSpeedFlow.collectAsState()
    val isCooldownActive by progressManager.ttsCooldownActiveFlow.collectAsState()
    val dailyRequestsCount by progressManager.dailyGeminiRequestsFlow.collectAsState()

    var keyText by remember { mutableStateOf(currentKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
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

    val hasKey = keyText.trim().isNotBlank()
    // Honest voice status: if in cooldown, voice is temporarily phone TTS
    val isGeminiVoiceActive = hasKey && !isCooldownActive

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

    fun mapErrorToDari(rawError: String?): String {
        if (rawError.isNullOrBlank()) {
            return "خطا در ارتباط با سرویس صوتی؛ لطفاً بعداً دوباره امتحان کنید."
        }
        val lower = rawError.lowercase()
        return when {
            rawError.contains(TtsManager.QUOTA_EXCEEDED_DARI_MSG) ||
                    lower.contains("quota") ||
                    lower.contains("429") ||
                    lower.contains("resource_exhausted") ||
                    lower.contains("exceeded") -> {
                TtsManager.QUOTA_EXCEEDED_DARI_MSG
            }
            lower.contains("api_key_invalid") ||
                    lower.contains("api key not valid") ||
                    lower.contains("invalid api key") ||
                    lower.contains("401") ||
                    lower.contains("403") ||
                    rawError.contains("کلید") -> {
                "کلید API جیمنای نامعتبر است. لطفاً کلید صحیح خود را از Google AI Studio وارد نمایید."
            }
            lower.contains("timeout") ||
                    lower.contains("connect") ||
                    lower.contains("unknownhost") ||
                    lower.contains("internet") -> {
                "خطا در اتصال به اینترنت؛ لطفاً اتصال شبکه خود را بررسی کرده و دوباره تلاش کنید."
            }
            else -> {
                "خطا در برقراری ارتباط با سرویس صوتی؛ لطفاً بعداً دوباره امتحان کنید."
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
                    text = "آیا مطمئن هستید که می‌خواهید تمام فایل‌های صوتی ذخیره‌شده (${libraryStats.count} جمله) را پاک کنید؟ در دفعات بعدی برای پخش این جمله‌ها با صدای جیمنای نیاز به سهمیه یا اینترنت خواهد بود.",
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
                        Toast.makeText(context, "حافظهٔ صدای جیمنای با موفقیت پاک شد.", Toast.LENGTH_SHORT).show()
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
                    text = "تنظیمات صدای جیمنای و سرعت",
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
                    text = "کلید API در حافظه امن تلفن شما ذخیره شده و برای تولید صدای طبیعی آلمانی استفاده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // KEY FIELD PRIVACY: Password-style visual transformation + eye toggle
                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        testResultSuccess = null
                        testResultError = null
                    },
                    label = { Text("کلید API جیمنای") },
                    placeholder = { Text("AIzaSy...") },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { isKeyVisible = !isKeyVisible },
                            modifier = Modifier.testTag("btn_toggle_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isKeyVisible) "مخفی‌سازی کلید" else "نمایش کلید",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_dialog_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // HONEST STATUS: Status chip shows «صدا: گوشی» during cooldown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت موتور صدا:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGeminiVoiceActive) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isGeminiVoiceActive) "صدا: جیمنای ✨" else "صدا: گوشی",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGeminiVoiceActive) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // FIX M: Daily Gemini API requests counter
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dailyPersian = UserProgressManager.toPersianDigits("$dailyRequestsCount")
                    Text(
                        text = "درخواست\u200Cهای امروز به جیمنای: $dailyPersian",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("daily_gemini_requests_text")
                    )
                }

                // QUOTA COOLDOWN NOTE: Clear Dari explanation when quota is exhausted
                if (isCooldownActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = TtsManager.QUOTA_EXCEEDED_DARI_MSG,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adjustable speed setting (app-wide)
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
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Gemini voice button with current speed & quota handling
                OutlinedButton(
                    onClick = {
                        testResultSuccess = null
                        testResultError = null
                        isTesting = true
                        coroutineScope.launch {
                            val res = onTestVoice(keyText.trim(), currentSpeed)
                            isTesting = false
                            if (res.isSuccess) {
                                val msg = res.getOrThrow()
                                testResultSuccess = msg
                                // Refresh library stats after successful voice test
                                libraryStats = TtsManager.getVoiceLibraryStats(context)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                val rawErrMsg = res.exceptionOrNull()?.localizedMessage
                                testResultError = mapErrorToDari(rawErrMsg)
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
                        Text("در حال پخش با سرعت ${UserProgressManager.formatSpeedToPersian(currentSpeed)}...", fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "امتحان صدا با سرعت ${UserProgressManager.formatSpeedToPersian(currentSpeed)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Success Message Card
                AnimatedVisibility(visible = testResultSuccess != null) {
                    testResultSuccess?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
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

                // Error Message Card (Clean Dari failure reason - raw English excluded)
                AnimatedVisibility(visible = testResultError != null) {
                    testResultError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (err == TtsManager.QUOTA_EXCEEDED_DARI_MSG) AccentAmber.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (err == TtsManager.QUOTA_EXCEEDED_DARI_MSG) Icons.Default.Info else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (err == TtsManager.QUOTA_EXCEEDED_DARI_MSG) AccentAmber else ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (err == TtsManager.QUOTA_EXCEEDED_DARI_MSG) "وضعیت سهمیه صدای جیمنای:" else "خطا در آزمایش صدا:",
                                        color = if (err == TtsManager.QUOTA_EXCEEDED_DARI_MSG) AccentAmber else ErrorRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PERMANENT IN-APP VOICE LIBRARY SECTION (FIX K & FIX L)
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
                                text = "حافظهٔ صدای جیمنای (ذخیرهٔ دائمی)",
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
                            text = "جملاتی که با صدای جیمنای پخش می‌شوند به صورت خودکار در حافظه داخلی ذخیره شده و در دفعات بعد بدون نیاز به اینترنت یا سهمیه پخش می‌گردند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1) EXPORT BUTTON: «خروجی گرفتن از حافظهٔ صدا»
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

                        // 2) IMPORT BUTTON: «وارد کردن حافظهٔ صدا»
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

                        // 3) DARI REPORT CARD: «N جملهٔ تازه اضافه شد — M جمله از قبل بود»
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

                        // 4) CLEAR BUTTON: «پاک کردن حافظهٔ صدا»
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            enabled = libraryStats.count > 0 && !isTesting && !isExporting && !isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_voice_library"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ErrorRed
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (libraryStats.count > 0) ErrorRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (libraryStats.count > 0) ErrorRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پاک کردن حافظهٔ صدا",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (libraryStats.count > 0) ErrorRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyText.trim()) },
                modifier = Modifier.testTag("btn_save_gemini_api_key")
            ) {
                Text("ذخیره کلید", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

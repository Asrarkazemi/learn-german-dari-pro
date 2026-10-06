package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SuccessGreen
import com.example.util.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * FIX M: Prominent one-tap batch voice download button for lessons & grammar topics.
 * 1) Button label: «📥 دانلود یکجای صدای کل این درس»
 * 2) Live progress: «⏳ در حال دریافت و برش صدا… (گروه X از Y)»
 * 3) Result line: «✅ N جمله ذخیره شد — M جمله از قبل بود»
 * 4) Disabled while running; lifecycle scoped to screen (cancelled if leaving screen).
 */
@Composable
fun BatchVoiceDownloadButton(
    onStartDownload: suspend (onProgress: (current: Int, total: Int) -> Unit) -> TtsManager.BatchDownloadResult,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("batch_download_voice_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    isRunning = true
                    progressText = "⏳ در حال آماده‌سازی و بررسی جملات..."
                    resultText = null
                    isError = false
                    coroutineScope.launch {
                        try {
                            val result = onStartDownload { cur, tot ->
                                val pCur = TtsManager.toPersianDigits("$cur")
                                val pTot = TtsManager.toPersianDigits("$tot")
                                progressText = "⏳ در حال دریافت و برش صدا… (گروه $pCur از $pTot)"
                            }
                            isRunning = false
                            if (result.isSuccess) {
                                resultText = result.formatSummary()
                                isError = false
                            } else {
                                resultText = result.errorMessage ?: "خطا در دانلود دسته‌جمعی صدا."
                                isError = true
                            }
                        } catch (e: CancellationException) {
                            isRunning = false
                        } catch (e: Exception) {
                            isRunning = false
                            resultText = "خطا در دانلود صدا: ${e.localizedMessage}"
                            isError = true
                        }
                    }
                },
                enabled = !isRunning,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_batch_voice_download")
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = progressText ?: "⏳ در حال دریافت و برش صدا…",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "📥 دانلود یک\u200Cجای صدای کل این درس",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = resultText != null) {
                resultText?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isError) SuccessGreen else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("batch_download_result_text")
                    )
                }
            }
        }
    }
}

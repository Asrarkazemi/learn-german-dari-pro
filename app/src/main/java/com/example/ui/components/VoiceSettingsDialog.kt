package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.storage.UserProgressManager
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

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

    var keyText by remember { mutableStateOf(currentKey) }
    var isTesting by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<String?>(null) }
    var testResultError by remember { mutableStateOf<String?>(null) }

    val hasKey = keyText.trim().isNotBlank()

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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "کلید API فقط در حافظه امن تلفن شما ذخیره شده و برای تولید صدای طبیعی آلمانی با مدل Gemini TTS استفاده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = {
                        keyText = it
                        testResultSuccess = null
                        testResultError = null
                    },
                    label = { Text("کلید API جیمنای") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_dialog_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Voice status indicator in Dari
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
                        color = if (hasKey) Color(0xFFE0E7FF) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (hasKey) "صدا: جیمنای ✨" else "صدا: گوشی",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasKey) Color(0xFF3730A3) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // FIX F (1 & 2): Adjustable speed setting (app-wide)
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

                // FIX F (5): Test Gemini voice button with current speed
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
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                testResultError = res.exceptionOrNull()?.localizedMessage
                                    ?: "خطای ناشناخته در اتصال به جیمنای"
                            }
                        }
                    },
                    enabled = !isTesting,
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

                // Error Message Card (Detailed Dari failure reason)
                AnimatedVisibility(visible = testResultError != null) {
                    testResultError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
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
                                Column {
                                    Text(
                                        text = "خطا در آزمایش صدا:",
                                        color = ErrorRed,
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

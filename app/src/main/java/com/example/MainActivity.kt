package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.storage.UserProgressManager
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.TtsManager

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val progressManager = UserProgressManager.getInstance(applicationContext)
        ttsManager = TtsManager(
            context = this,
            apiKeyProvider = { progressManager.getEffectiveGeminiApiKey() },
            speedProvider = { progressManager.getPlaybackSpeed() }
        )

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(ttsManager = ttsManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

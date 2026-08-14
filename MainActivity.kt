package com.viralcaption.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.viralcaption.ai.ui.ViralCaptionScreen
import com.viralcaption.ai.ui.theme.ViralCaptionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViralCaptionTheme {
                ViralCaptionScreen()
            }
        }
    }
}

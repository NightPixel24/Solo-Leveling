package com.nightpixel.sololeveling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nightpixel.sololeveling.ui.navigation.SoloLevelingApp
import com.nightpixel.sololeveling.ui.theme.SoloLevelingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoloLevelingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoloLevelingApp()
                }
            }
        }
    }
}

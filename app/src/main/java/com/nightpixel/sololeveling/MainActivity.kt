package com.nightpixel.sololeveling

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nightpixel.sololeveling.ui.navigation.SoloLevelingApp
import com.nightpixel.sololeveling.ui.theme.SoloLevelingTheme

class MainActivity : ComponentActivity() {
    // Spec Section 7's local reminders are all scheduled unconditionally at app startup
    // (SoloLevelingApplication.onCreate); this just asks for the API 33+ runtime permission they
    // need to actually display - no branching needed on the result, Notifier re-checks per show.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SoloLevelingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SoloLevelingApp()
                }
            }
        }
    }
}

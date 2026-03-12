package io.duckemu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.duckemu.emulator.presentation.MainScreen
import io.duckemu.emulator.repository.config.appContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        setContent {
            MaterialTheme {
                MainScreen(showSettings1 = showSettings)
            }
        }
    }
}
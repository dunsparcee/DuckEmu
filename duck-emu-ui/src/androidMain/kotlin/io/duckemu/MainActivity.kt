package io.duckemu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.duckemu.emulator.presentation.MainMobileScreen
import io.duckemu.emulator.repository.config.appContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        setContent {
            MaterialTheme {
                MainMobileScreen()
            }
        }
    }
}
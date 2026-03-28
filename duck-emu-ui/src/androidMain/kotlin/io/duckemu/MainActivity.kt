package io.duckemu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.duckemu.emulator.presentation.MainViewModel
import io.duckemu.emulator.repository.config.appContext
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        setContent {
            MaterialTheme {
                MainViewModel.MainScreen(mobileDevice = true)
            }
        }
    }

    override fun onStop() {
        runBlocking {
            MainViewModel.consoleRunning?.emulator?.stop()
        }
        super.onStop()
    }
}
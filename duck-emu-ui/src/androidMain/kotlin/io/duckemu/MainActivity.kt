package io.duckemu

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import io.duckemu.emulator.presentation.MainViewModel
import io.duckemu.emulator.repository.config.appContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.kreenshot.KreenshotCapture
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        setContent {
            MaterialTheme {
                KreenshotCapture.init(this)
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
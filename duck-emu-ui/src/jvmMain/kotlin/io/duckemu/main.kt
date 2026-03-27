package io.duckemu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.emulator.presentation.MainViewModel
import kotlinx.coroutines.runBlocking

fun main() = application {
    Window(onCloseRequest = {
        runBlocking {
            MainViewModel.consoleRunning?.emulator?.stop()
        }
        exitApplication()
    } , title = "DuckEmu") {
        MaterialTheme {
            MainViewModel.MainScreen(mobileDevice = true)
        }
    }
}

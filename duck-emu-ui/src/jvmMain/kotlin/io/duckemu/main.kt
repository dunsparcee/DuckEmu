package io.duckemu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.emulator.data.ControllerSource
import io.duckemu.emulator.presentation.MainViewModel
import io.github.vinceglb.filekit.FileKit
import io.kreenshot.KreenshotCapture
import kotlinx.coroutines.runBlocking

fun main() = application {
    val titleName = "DuckEmu"

    Window(onCloseRequest = {
        runBlocking {
            MainViewModel.consoleRunning?.emulator?.stop()
        }
        exitApplication()
    } , title = titleName) {
        MaterialTheme {
            ControllerSource.init(listOf(ControllerSource.KEYBOARD, ControllerSource.GAMEPAD))
            KreenshotCapture.init(window)
            FileKit.init(titleName)
            MainViewModel.MainScreen(false)
        }
    }
}

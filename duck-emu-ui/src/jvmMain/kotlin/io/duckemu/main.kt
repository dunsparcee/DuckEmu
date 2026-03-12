package io.duckemu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.emulator.presentation.MainScreen

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "DuckEmu") {
        MaterialTheme {
            MainScreen(mobileDevice = true)
        }
    }
}

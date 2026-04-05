package io.duckemu.emulator.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import io.duckemu.emulator.presentation.ControllerSourceContent

enum class SettingsAction {
    SAVE, LOAD, AUDIO, SCREENSHOT, LIST_LOAD, FORWARD, GAMEPAD_SKIN, EXIT_GAME, NONE, CONTROLLER_CONNECTION
}

enum class ControllerSource(val label: String, val icon: ImageVector) {
    TOUCH("Touch Screen", Icons.Default.TouchApp),
    GAMEPAD("Controller", Icons.Default.SportsEsports),
    KEYBOARD("Keyboard", Icons.Default.Keyboard);

    companion object {
        fun init(available: List<ControllerSource>) {
            availableControllerSources = available
        }

        var availableControllerSources: List<ControllerSource> = listOf()
            private set
    }
}

class EmuController(
    val source: ControllerSource = ControllerSource.availableControllerSources.first(),
    val port: Int = 9999
)

val defaultEmuController = EmuController()




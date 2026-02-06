package io.duckemu

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.gbc.EmulatorScreen
import io.duckemu.gbc.GameBoyViewModel
import io.duckemu.gbc.setupKeyHandler
import io.github.compose_keyhandler.KeyHandlerHost
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch

fun main() = application {
    val gbController = remember { GameBoyViewModel() }
    val keyHandler = remember { setupKeyHandler(gbController) }
    val scope = rememberCoroutineScope()

    Window(onCloseRequest = ::exitApplication, title = "DuckEmu") {
        MenuBar {
            Menu("File") {
                Item(
                    "Open ROM",
                    onClick = {
                        scope.launch {
                            val file = FileKit.openFilePicker()
                            file?.let {
                                gbController.startGBC(it)
                            }
                        }
                    }
                )

                Item("Close", onClick = {
                    scope.launch {
                        gbController.stopGBC()
                    }
                })
            }
        }

        KeyHandlerHost(keyHandler) {
            Box {
                EmulatorScreen(gbController)
            }
        }
    }
}

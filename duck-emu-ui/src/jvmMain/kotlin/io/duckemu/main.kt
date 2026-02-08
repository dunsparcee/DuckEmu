package io.duckemu

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.gbc.data.game.GameRepository
import io.duckemu.gbc.presentation.emulator.EmulatorScreen
import io.duckemu.gbc.presentation.emulator.GameBoyViewModel
import io.duckemu.gbc.presentation.emulator.GameLibraryScreen
import io.duckemu.gbc.presentation.emulator.GameLibraryViewModel
import io.duckemu.gbc.presentation.emulator.setupKeyHandler
import io.github.compose_keyhandler.KeyHandlerHost
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch


fun main() = application {
    val gameBoy = remember { GameBoyViewModel() }
    val gameRepository = remember { GameRepository() }
    val gameLibrary = remember { GameLibraryViewModel(gameRepository) }
    val keyHandler = remember { setupKeyHandler(gameBoy) }
    val scope = rememberCoroutineScope()

    Window(onCloseRequest = ::exitApplication, title = "DuckEmu") {
        MaterialTheme {
            MenuBar {
                Menu("File") {
                    Item(
                        "Load File",
                        onClick = {
                            scope.launch {
                                val file = FileKit.openFilePicker()
                                file?.let {
                                    gameBoy.startGBC(it)
                                }
                            }
                        }
                    )

                    Item("Exit", onClick = {
                        scope.launch {
                            gameBoy.stopGBC()
                        }
                    })
                }
                Menu("Emulation") {
                    Item(
                        "Pause",
                        onClick = {

                        }
                    )

                    Item(
                        "Stop",
                        onClick = {

                        }
                    )

                    Item(
                        "Restart",
                        onClick = {

                        }
                    )

                    Menu("Save States") {
                        Item(
                            "Save",
                            onClick = {

                            }
                        )

                        Item(
                            "Load",
                            onClick = {

                            }
                        )
                    }

                    Item(
                        "Configure",
                        onClick = {

                        }
                    )
                }
            }

            if (gameBoy.gameBoyImage == null) {
                Box {
                    GameLibraryScreen(gameLibrary)
                }
            }

            KeyHandlerHost(keyHandler) {
                Box {
                    EmulatorScreen(gameBoy)
                }
            }
        }
    }
}

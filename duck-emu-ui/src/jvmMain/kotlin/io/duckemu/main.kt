package io.duckemu

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.duckemu.emulator.presentation.EmulatorScreen
import io.duckemu.emulator.presentation.GameLibraryScreen
import io.duckemu.emulator.presentation.GameLibraryViewModel
import io.duckemu.emulator.repository.config.ConfigRepository
import io.duckemu.emulator.repository.game.GameRepository
import io.duckemu.gbc.presentation.emulator.GameBoyViewModel
import io.duckemu.nes.core.NesViewModel
import io.github.compose_keyhandler.KeyHandlerHost
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch

fun main() = application {
    val consoles: Map<String, EmulatorViewModel> = remember {
        mapOf(
            "gb" to GameBoyViewModel,
            "gbc" to GameBoyViewModel,
            "nes" to NesViewModel
        )
    }

    val gameRepository = remember { GameRepository() }
    val gameLibrary = remember { GameLibraryViewModel(gameRepository, ConfigRepository()) }
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
                                    consoles["gbc"]?.start(file)
                                }
                            }
                        }
                    )

                    Item("Exit", onClick = {
                        scope.launch {
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

            consoles.values.find { it.isEmuRunning() }?.let {
                KeyHandlerHost(it.controllerSetup()) {
                    Box {
                        EmulatorScreen(it)
                    }
                }
            } ?: run {
                Box {
                    GameLibraryScreen(gameLibrary, consoles)
                }
            }
        }
    }
}

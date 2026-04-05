package io.duckemu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.duckemu.controller.GamepadController
import io.duckemu.emulator.data.ControllerSource
import io.duckemu.emulator.data.EmuController
import io.duckemu.emulator.data.SettingsAction
import io.duckemu.emulator.data.defaultEmuController
import io.duckemu.emulator.repository.config.ControllerThemeStore
import io.duckemu.gbc.presentation.emulator.ControllerTheme
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile
import io.kreenshot.KreenshotCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

abstract class EmulatorViewModel(consoleId: String) : ViewModel() {

    var soundEnable by mutableStateOf(true)
    var controllerTheme by mutableStateOf<ControllerTheme?>(null)
    var showSheet by mutableStateOf(false)
    var isRunning by mutableStateOf(false)
    var graphics by mutableStateOf<ImageBitmap?>(null)
    var openSettings by mutableStateOf(false)
    var settingsAction by mutableStateOf(SettingsAction.NONE)
    var playerSources by mutableStateOf(mapOf(1 to defaultEmuController))

    init {
        viewModelScope.launch {
            controllerTheme = ControllerThemeStore.get(consoleId)
        }
    }

    abstract suspend fun start(path: PlatformFile)
    abstract fun stop()
    abstract fun isEmuRunning(): Boolean
    abstract fun controllerSetup(): KeyHandler
    abstract fun listSaves(): List<String>
    abstract fun loadState(path: String)
    abstract fun loadState()
    abstract fun saveState()
    abstract fun toggleAudio()
    abstract fun connectController(player: Int, controller: Int)

    fun captureAndSave() {
        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)
            KreenshotCapture.capture { bytes ->
                bytes?.let {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    KreenshotCapture.save(
                        bytes = it,
                        fileName = "Screenshot_$timestamp"
                    )
                }
            }
        }
    }

    fun onSettingsAction(action: SettingsAction) {
        when (action) {
            SettingsAction.SAVE -> saveState()
            SettingsAction.LOAD -> loadState()
            SettingsAction.AUDIO -> toggleAudio()
            SettingsAction.FORWARD -> TODO()
            SettingsAction.EXIT_GAME -> stop()
            SettingsAction.SCREENSHOT -> {
                closeSettingsSheet()
                captureAndSave()
            }
            SettingsAction.CONTROLLER_CONNECTION, SettingsAction.LIST_LOAD,
            SettingsAction.GAMEPAD_SKIN -> settingsAction = action
            else -> {}
        }
    }

    fun closeSettingsSheet() {
        showSheet = false
        settingsAction = SettingsAction.NONE
    }

    fun openSettingsSheet() {
        showSheet = true
    }

    fun setPlayerSource(player: Int, source: EmuController) {
        val previous = playerSources[player]

        if (previous?.source == ControllerSource.GAMEPAD) {
            GamepadController.stopListening(previous.port)
        }

        playerSources = playerSources + (player to source)

        if (source.source == ControllerSource.GAMEPAD) {
            connectController(player, source.port)
        }
    }
}
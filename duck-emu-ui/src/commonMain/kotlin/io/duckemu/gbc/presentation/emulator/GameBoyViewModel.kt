package io.duckemu.gbc.presentation.emulator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import io.duckemu.EmulatorViewModel
import io.duckemu.emulator.repository.config.ControllerThemeStore
import io.duckemu.gbc.data.emulator.Controller
import io.duckemu.gbc.data.emulator.DuckEmuConfig
import io.duckemu.gbc.data.gpu.Colors
import io.duckemu.gbc.domain.emulator.GameBoy
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

object GameBoyViewModel : EmulatorViewModel() {
    var isRunning by mutableStateOf(false)
    private var gameBoy: GameBoy? = null
    val inputHandler = Controller()
    private var gameLoaded = ""
    var controller: KeyHandler = setupKeyHandler(this)
    var controllerTheme by mutableStateOf<ControllerTheme>(ControllerTheme.GBC())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        scope.launch {
            controllerTheme = ControllerThemeStore.get("gbc")
        }
    }

    override suspend fun start(path: PlatformFile) {
        stop()
        val cartridgeBin = path.readBytes()
        val isGbc = path.extension.contains("gbc")
        val palette = if (DuckEmuConfig.color_style == "GBP") Colors.GBP else Colors.GB

        gameBoy = GameBoy(isGbc, palette, cartridgeBin, inputHandler) { image, _ ->
            graphics = image
        }.apply {
            setSoundEnable(DuckEmuConfig.enableSound)
            setSpeed(DuckEmuConfig.speed)
            gameLoaded = path.path
            val sRamFile = "${gameLoaded}.sram.sav"

            val file = PlatformFile(sRamFile)
            if (file.exists()) {
                setSram(file.readBytes())
            }

            startup()
        }
        isRunning = true
    }

    override fun controllerSetup(): KeyHandler {
        return controller
    }

    override fun isEmuRunning() : Boolean {
        return isRunning
    }

    override suspend fun stop() {
        gameBoy?.let { gb ->
            if (gb.running()) {
                gb.shutdown()
                gb.sarm()?.let {
                    val path = "${gameLoaded}.sram.sav".toPath()
                    FileSystem.SYSTEM.write(path) {
                        write(it)
                    }
                }
            }
        }
        gameBoy = null
        graphics = null
        isRunning = false
    }
}

fun setupKeyHandler(gameBoyViewModel: GameBoyViewModel): KeyHandler {
    return KeyHandler {
        onPress {
            keys(gameBoyViewModel.inputHandler, true)
        }
        onRelease {
            keys(gameBoyViewModel.inputHandler, false)
        }
    }
}

private fun KeyActionBuilder.keys(inputHandler: Controller, isPressed: Boolean) {
    key(Key.A) {
        upDown(inputHandler, isPressed, 1)
    }
    key(Key.W) {
        upDown(inputHandler, isPressed, 2)
    }
    key(Key.S) {
        upDown(inputHandler, isPressed, 3)
    }
    key(Key.D) {
        upDown(inputHandler, isPressed, 0)
    }
    key(Key.K) {
        upDown(inputHandler, isPressed, 4)
    }
    key(Key.J) {
        upDown(inputHandler, isPressed, 5)
    }
    key(Key.F) {
        upDown(inputHandler, isPressed, 6)
    }
    key(Key.H) {
        upDown(inputHandler, isPressed, 7)
    }
}

fun upDown(inputHandler: Controller, isPressed: Boolean, index: Int) {
    if (isPressed) inputHandler.buttonPressed(index)
    else inputHandler.buttonRelease(index)
}
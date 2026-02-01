package io.duckemu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import io.duckemu.emulator.config.DuckEmuConfig
import io.duckemu.gbc.gpu.Colors
import io.duckemu.gbc.Controller
import io.duckemu.gbc.GameBoy
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class GameBoyViewModel {
    var gameBoyImage by mutableStateOf<BufferedImage?>(null)
    var isRunning by mutableStateOf(false)
    private var gameBoy: GameBoy? = null
    val inputHandler = Controller()
    private var gameLoaded = ""
    fun startGBC(path: String) {
        stopGBC()
        val cartridgeBin = Files.readAllBytes(Paths.get(path)) ?: return
        val isGbc = DuckEmuConfig.color_style != "GB" && DuckEmuConfig.color_style != "GBP"
        val palette = if (DuckEmuConfig.color_style == "GBP") Colors.GBP else Colors.GB

        gameBoy = GameBoy(isGbc, palette, cartridgeBin, inputHandler) { image, _ ->
            gameBoyImage = image
        }.apply {
            setSoundEnable(DuckEmuConfig.enableSound)
            setSpeed(DuckEmuConfig.speed)
            gameLoaded = path.substringBeforeLast(".")
            val sRamFile = "${gameLoaded}.sram.sav"
            if (File(sRamFile).exists()) {
                setSarm(Files.readAllBytes(Paths.get(sRamFile)))
            }

            startup()
        }
        isRunning = true
    }

    fun stopGBC() {
        gameBoy?.let { gb ->
            if (gb.running()) {
                gb.shutdown()
                gb.sarm().let {
                    Files.write(Paths.get("${gameLoaded}.sram.sav"), it)
                }
            }
        }
        gameBoy = null
        gameBoyImage = null
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
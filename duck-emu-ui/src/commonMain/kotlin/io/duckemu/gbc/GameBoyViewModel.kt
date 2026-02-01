package io.duckemu.gbc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import io.duckemu.gbc.gpu.Colors
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class GameBoyViewModel {
    var gameBoyImage by mutableStateOf<ImageBitmap?>(null)
    var isRunning by mutableStateOf(false)
    private var gameBoy: GameBoy? = null
    val inputHandler = Controller()
    private var gameLoaded = ""
    suspend fun startGBC(path: PlatformFile) {
        stopGBC()
        val cartridgeBin = path.readBytes()
        val isGbc = DuckEmuConfig.color_style != "GB" && DuckEmuConfig.color_style != "GBP"
        val palette = if (DuckEmuConfig.color_style == "GBP") Colors.GBP else Colors.GB

        gameBoy = GameBoy(isGbc, palette, cartridgeBin, inputHandler) { image, _ ->
            gameBoyImage = image
        }.apply {
            setSoundEnable(DuckEmuConfig.enableSound)
            setSpeed(DuckEmuConfig.speed)
            gameLoaded = path.path.substringBeforeLast("")
            val sRamFile = "${gameLoaded}.sram.sav"

            val file = PlatformFile(sRamFile)
            if (file.exists()) {
                setSarm(file.readBytes())
            }

            startup()
        }
        isRunning = true
    }

    suspend fun stopGBC() {
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
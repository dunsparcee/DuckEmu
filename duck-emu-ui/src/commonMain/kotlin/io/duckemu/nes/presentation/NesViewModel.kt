package io.duckemu.nes.presentation

import androidx.compose.ui.input.key.Key
import io.duckemu.EmulatorViewModel
import io.duckemu.controller.GamepadController
import io.duckemu.gbc.presentation.emulator.GameBoyViewModel.inputHandler
import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.ui.Renderer
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.*
import kotlin.time.Clock

object NesViewModel : EmulatorViewModel(consoleId = "nes") {
    var nes: Nes? = null
    var gameLoaded = ""
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    var frameDurationMs = 16 //60 FPS

    override suspend fun start(path: PlatformFile) {
        stop()
        gameLoaded = path.name
        val saveFile = FileKit.filesDir.path.plus("/${gameLoaded}.sram.sav")
        val r = Renderer()
        nes = Nes(r)
        nes!!.load(path.path, saveFile)
        startup()
        isRunning = true
    }

    fun run() {
        while (true) {
            val start = Clock.System.now().toEpochMilliseconds()
            graphics = nes?.execFrame()

            while (true) {
                val bufStat = nes?.renderer?.soundBufferState
                if (bufStat != null && bufStat < 0) break

                if (bufStat == 0) {
                    val elapsed = Clock.System.now().toEpochMilliseconds() - start
                    val wait = (frameDurationMs - elapsed).toLong()
                    if (wait > 0) {
                        runBlocking { delay(wait) }
                    }
                    break
                }
                runBlocking { delay(1) }
            }
        }
    }

    fun running(): Boolean {
        return job?.isActive == true
    }

    fun startup() {
        if (!running()) {
            job = scope.launch {
                run()
            }
        }
    }

    override fun toggleAudio() {
        nes?.let {
            it.soundEnable = !it.soundEnable
        }
    }

    override fun controllerSetup(): KeyHandler {
        return setupKeyHandler()
    }

    override fun isEmuRunning(): Boolean {
        return isRunning
    }

    override fun stop() {
        val saveFile = FileKit.filesDir.path.plus("/${gameLoaded}.sram.sav")
        nes?.saveSram(saveFile)
        nes = null
        graphics = null
        isRunning = false
    }

    override fun connectController(player: Int, controller: Int) {
        GamepadController.startListening(
            playerSources[player]!!.port,
            onPressed = {
                when (it) {
                    "DPAD_RIGHT" -> upDown(true, Key.DirectionRight)
                    "DPAD_LEFT" -> upDown(true, Key.DirectionLeft)
                    "DPAD_UP" -> upDown(true, Key.DirectionUp)
                    "DPAD_DOWN" -> upDown(true, Key.DirectionDown)
                    "BUTTON_B" -> upDown(true, Key.Z)
                    "BUTTON_A" -> upDown(true, Key.X)
                    "BUTTON_SELECT" -> upDown(true, Key.ShiftLeft)
                    "BUTTON_START" -> upDown(true, Key.Enter)
                }
            }, onReleased = {
                when (it) {
                    "DPAD_RIGHT" -> upDown(false, Key.DirectionRight)
                    "DPAD_LEFT" -> upDown(false, Key.DirectionLeft)
                    "DPAD_UP" -> upDown(false, Key.DirectionUp)
                    "DPAD_DOWN" -> upDown(false, Key.DirectionDown)
                    "BUTTON_B" -> upDown(false, Key.Z)
                    "BUTTON_A" -> upDown(false, Key.X)
                    "BUTTON_SELECT" -> upDown(false, Key.ShiftLeft)
                    "BUTTON_START" -> upDown(false, Key.Enter)
                }
            })
    }

    override fun setSpeed() {
        // no-op
    }

    fun setupKeyHandler(): KeyHandler {
        return KeyHandler {
            onPress {
                key(Key.Escape) {
                    showSheet = !showSheet
                }
                keys(true)
            }
            onRelease {
                keys(false)
            }
        }
    }

    val keysToMap = arrayOf(
        Key.Z, Key.X, Key.ShiftLeft,
        Key.Enter, Key.DirectionUp, Key.DirectionDown,
        Key.DirectionLeft, Key.DirectionRight,
        Key.V, Key.B, Key.N, Key.M,
        Key.O, Key.Comma, Key.K, Key.L
    )

    private fun KeyActionBuilder.keys(isPressed: Boolean) {
        keysToMap.forEach { targetKey ->
            key(targetKey) {
                upDown(isPressed, targetKey)
            }
        }
    }

    fun upDown(isPressed: Boolean, index: Key) {
        nes?.renderer?.onKey(index, isPressed)
    }

    override fun listSaves(): List<String> {
        TODO("Not yet implemented")
    }

    override fun loadState(path: String) {
    }

    override fun loadState() {
    }

    override fun saveState() {
    }
}
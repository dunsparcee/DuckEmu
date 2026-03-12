package io.duckemu.nes.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import io.duckemu.EmulatorViewModel
import io.duckemu.gbc.data.emulator.Controller
import io.duckemu.gbc.presentation.emulator.GameBoyViewModel
import io.duckemu.nes.core.ui.Renderer
import io.github.compose_keyhandler.KeyActionBuilder
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.*
import kotlin.time.Clock

object NesViewModel : EmulatorViewModel() {
    private var nes: Nes? = null
    var isRunning by mutableStateOf(false)

    override suspend fun start(path: PlatformFile) {
        stop()
        val r = Renderer()
        nes = Nes(r)
        nes!!.load(path.path)
        startup()
        nes

        isRunning = true
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun run() {
        val fps = 60
        val frameDurationMs = 500.0 / fps

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

    override fun controllerSetup(): KeyHandler {
        return setupKeyHandler()
    }

    override fun isEmuRunning(): Boolean {
        return isRunning
    }

    override suspend fun stop() {
        nes = null
        graphics = null
        isRunning = false
    }

    fun setupKeyHandler(): KeyHandler {
        return KeyHandler {
            onPress {
                key(Key.Escape) {
                    openSettings = !openSettings
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
}


package io.duckemu.nes.presentation

import androidx.compose.ui.input.key.Key
import io.duckemu.EmulatorViewModel
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

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun run() {
        val fps = 60
        val frameDurationMs = 1000.0 / fps

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

    }

    fun setupKeyHandler(): KeyHandler {
        return KeyHandler {
            onPress {
                keys(true)
            }
            onRelease {
                keys(false)
            }
        }
    }


    val keysToMap = arrayOf(
        Key.Companion.Z, Key.Companion.X, Key.Companion.ShiftLeft,
        Key.Companion.Enter, Key.Companion.DirectionUp, Key.Companion.DirectionDown,
        Key.Companion.DirectionLeft, Key.Companion.DirectionRight,
        Key.Companion.V, Key.Companion.B, Key.Companion.N, Key.Companion.M,
        Key.Companion.O, Key.Companion.Comma, Key.Companion.K, Key.Companion.L
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
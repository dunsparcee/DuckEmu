package io.duckemu.nes.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.duckemu.EmulatorViewModel
import io.duckemu.nes.core.ui.Renderer
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.*
import kotlin.time.Clock

class NesViewModel : EmulatorViewModel() {
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
    private val nesLock = Any()

    fun run() {
        val fps = 60

        while (true) {

            val start = Clock.System.now().nanosecondsOfSecond // available in Kotlin/JVM & Android
            graphics = nes?.execFrame()

            while (true) {
                val bufStat = nes?.renderer?.soundBufferState
                if (bufStat != null && bufStat < 0) {
                    break
                }
                if (bufStat == 0) {
                    val elapsed = Clock.System.now().nanosecondsOfSecond - start
                    val wait = ((1.0 / fps) * 1e9 - elapsed).toLong() // fixed unit bug*
                    if (wait > 0) {
                        runBlocking { delay(wait / 1_000_000L) }
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
        return KeyHandler()
    }

    override fun isEmuRunning(): Boolean {
        return isRunning
    }

    override suspend fun stop() {
        nes = null
        graphics = null
        isRunning = false
    }
}
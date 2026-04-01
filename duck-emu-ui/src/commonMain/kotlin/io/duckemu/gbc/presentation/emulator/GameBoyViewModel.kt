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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

object GameBoyViewModel : EmulatorViewModel() {
    var isRunning by mutableStateOf(false)
    var gameBoy: GameBoy? = null
    var gameLoaded = PlatformFile("")
    var controllerTheme by mutableStateOf<ControllerTheme>(ControllerTheme.GBC())
    val inputHandler = Controller()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controller: KeyHandler = setupKeyHandler(this)

    init {
        scope.launch {
            controllerTheme = ControllerThemeStore.get("gbc")
        }
    }

    override fun saveState() {
        gameBoy?.let {
            val flatten = it.flatten()
            val path = FileKit.filesDir.path.plus("/${gameLoaded.name}.${Clock.System.now().toEpochMilliseconds()}.ss")
                .toPath()
            FileSystem.SYSTEM.write(path) {
                write(flatten)
            }
        }
    }

    override fun listSaves(): List<String> {
        val savesDir = FileKit.filesDir.path.toPath()
        val prefix = "${gameLoaded.name}."
        return FileSystem.SYSTEM
            .list(savesDir)
            .filter { it.name.startsWith(prefix) && it.name.endsWith(".ss") }
            .sortedByDescending { it.name.removePrefix(prefix).removeSuffix(".ss").toLongOrNull() ?: 0L }
            .map { it.toString() }
    }

    override fun loadState() {
        gameBoy?.let {

            val savesDir = FileKit.filesDir.path.toPath()
            val prefix = "${gameLoaded.name}."

            val latestFile = FileSystem.SYSTEM
                .list(savesDir)
                .filter { it.name.startsWith(prefix) && it.name.endsWith(".ss") }
                .maxByOrNull { it.name.removePrefix(prefix).removeSuffix(".ss").toLongOrNull() ?: 0L }

            latestFile?.let { path ->
                val bytes = FileSystem.SYSTEM.read(path) { readByteArray() }
                it.unflatten(bytes)
            }
        }
    }

    override fun loadState(path: String) {
        gameBoy?.let {
            val bytes = FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }
            it.unflatten(bytes)
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
            gameLoaded = path
            val sRamFile = FileKit.filesDir.path.plus("/${gameLoaded.name}.sram.sav")

            val file = PlatformFile(sRamFile)
            if (file.exists()) {
                setSram(file.readBytes())
            }

            startup()
            scope.launch {
                while (running()) {
                    delay(3000)
                    save()
                }
            }
        }
        isRunning = true
    }

    override fun controllerSetup(): KeyHandler {
        return controller
    }

    override fun isEmuRunning(): Boolean {
        return isRunning
    }

    override suspend fun stop() {
        gameBoy?.let { gb ->
            if (gb.running()) {
                gb.shutdown()
                save()
            }
        }
        gameBoy = null
        graphics = null
        isRunning = false
    }

    private fun save() {
        gameBoy?.sram()?.let {
            val path = FileKit.filesDir.path.plus("/${gameLoaded.name}.sram.sav").toPath()
            FileSystem.SYSTEM.write(path) {
                write(it)
            }
        }
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
package io.duckemu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.PlatformFile

interface Emulator {

    suspend fun start(path: PlatformFile)

    suspend fun stop()

    fun isEmuRunning(): Boolean

    fun controllerSetup(): KeyHandler
}

abstract class EmulatorViewModel : Emulator {
    fun openSettingsSheet() {
        showSheet = true
    }

    fun closeSettingsSheet() {
        showSheet = false
    }

    abstract fun listSaves(): List<String>
    abstract fun loadState(path: String)
    abstract fun loadState()
    abstract fun saveState()

    var showSheet by mutableStateOf(false)
    var graphics by mutableStateOf<ImageBitmap?>(null)
    var openSettings by mutableStateOf(false)
}
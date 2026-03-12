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
    var graphics by mutableStateOf<ImageBitmap?>(null)
    var openSettings by mutableStateOf(false)
}
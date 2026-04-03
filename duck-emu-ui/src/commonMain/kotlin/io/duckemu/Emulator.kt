package io.duckemu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.compose_keyhandler.KeyHandler
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import io.kreenshot.KreenshotCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Clock

abstract class EmulatorViewModel : ViewModel() {
    fun openSettingsSheet() {
        showSheet = true
    }

    fun closeSettingsSheet() {
        showSheet = false
    }

    abstract suspend fun start(path: PlatformFile)
    abstract fun stop()
    abstract fun isEmuRunning(): Boolean
    abstract fun controllerSetup(): KeyHandler
    abstract fun listSaves(): List<String>
    abstract fun loadState(path: String)
    abstract fun loadState()
    abstract fun saveState()
    abstract fun toggleAudio()

    fun captureAndSave() {
        viewModelScope.launch(Dispatchers.Default) {
            delay(1000)
            KreenshotCapture.capture { bytes ->
                bytes?.let {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    KreenshotCapture.save(
                        bytes = it,
                        fileName = "Screenshot_$timestamp"
                    )
                }
            }
        }
    }

    var soundEnable by mutableStateOf(true)
    var showSheet by mutableStateOf(false)
    var isRunning by mutableStateOf(false)
    var graphics by mutableStateOf<ImageBitmap?>(null)
    var openSettings by mutableStateOf(false)
}
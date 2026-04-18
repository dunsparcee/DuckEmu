package io.duckemu.sms.presentation.emulator

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import io.duckemu.EmulatorViewModel
import io.duckemu.sms.domain.Sms
import io.duckemu.sms.domain.Vdp
import io.duckemu.sms.data.EmuState
import io.duckemu.gbc.domain.gpu.createImage
import io.github.compose_keyhandler.KeyHandler
import androidx.compose.ui.input.key.Key
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SMSViewModel : EmulatorViewModel(consoleId = "sms") {
    private var sms: Sms? = null
    private var isPaused = false

    override suspend fun start(path: PlatformFile) {
        val bytes = path.readBytes()
        val extension = path.extension.lowercase()
        
        sms = Sms()
        if (extension == "gg") sms?.setGG() else sms?.setSMS()
        
        sms?.loadRom(bytes)
        isRunning = true
        
        viewModelScope.launch(Dispatchers.Default) {
            while (isRunning) {
                if (!isPaused) {
                    val frame = sms?.emulateFrame()
                    if (frame != null) {
                        val width = EmuState.emuWidth
                        val height = EmuState.emuHeight
                        
                        val visiblePixels = if (EmuState.is_gg) {
                            val cropped = IntArray(width * height)
                            for (y in 0 until height) {
                                val srcY = y + Vdp.GG_Y_OFFSET
                                val srcPos = srcY * 256 + Vdp.GG_X_OFFSET
                                frame.copyInto(cropped, y * width, srcPos, srcPos + width)
                            }
                            cropped
                        } else {
                            frame
                        }

                        val bitmap = createImage(width, height, visiblePixels)
                        withContext(Dispatchers.Main) {
                            graphics = bitmap
                        }
                    }
                }
                delay(16) // roughly 60fps
            }
        }
    }

    override fun stop() {
        isRunning = false
        sms = null
    }

    override fun isEmuRunning(): Boolean = isRunning

    override fun controllerSetup(): KeyHandler {
        return KeyHandler {
            onPress {
                key(Key.Escape) { showSheet = !showSheet }
                key(Key.W) { updateController(0x01, true) }
                key(Key.S) { updateController(0x02, true) }
                key(Key.A) { updateController(0x04, true) }
                key(Key.D) { updateController(0x08, true) }
                key(Key.K) { updateController(0x10, true) }
                key(Key.L) { updateController(0x20, true) }
                
                key(Key.Enter) {
                    if (EmuState.is_gg) updateStart(true) else sms?.pause()
                }
                key(Key.P) {
                    if (EmuState.is_gg) updateStart(true) else sms?.pause()
                }
            }
            onRelease {
                key(Key.W) { updateController(0x01, false) }
                key(Key.S) { updateController(0x02, false) }
                key(Key.A) { updateController(0x04, false) }
                key(Key.D) { updateController(0x08, false) }
                key(Key.K) { updateController(0x10, false) }
                key(Key.L) { updateController(0x20, false) }
                
                key(Key.Enter) {
                    if (EmuState.is_gg) updateStart(false)
                }
                key(Key.P) {
                    if (EmuState.is_gg) updateStart(false)
                }
            }
        }
    }

    private fun updateController(bit: Int, pressed: Boolean) {
        if (pressed) {
            EmuState.controller1 = EmuState.controller1 and bit.inv()
        } else {
            EmuState.controller1 = EmuState.controller1 or bit
        }
    }

    private fun updateStart(pressed: Boolean) {
        if (pressed) {
            EmuState.ggstart = EmuState.ggstart and 0x7F
        } else {
            EmuState.ggstart = EmuState.ggstart or 0x80
        }
    }

    override fun listSaves(): List<String> = emptyList()

    override fun loadState(path: String) {}

    override fun loadState() {}

    override fun saveState() {}

    override fun toggleAudio() {
        soundEnable = !soundEnable
    }

    override fun setSpeed() {
        // TODO: Implement speed control
    }

    override fun connectController(player: Int, controller: Int) {
        // TODO: Implement gamepad support
    }
}

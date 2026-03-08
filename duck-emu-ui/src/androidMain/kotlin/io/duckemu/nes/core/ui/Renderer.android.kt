package io.duckemu.nes.core.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key

actual class Renderer actual constructor() {
    actual val soundBufferState: Int
        get() = TODO("Not yet implemented")

    actual fun requestScreen(
        width: Int,
        height: Int
    ): ScreenInfo {
        TODO("Not yet implemented")
    }

    actual fun requestSound(): SoundInfo? {
        TODO("Not yet implemented")
    }

    actual fun requestInput(
        padCount: Int,
        buttonCount: Int
    ): InputInfo {
        TODO("Not yet implemented")
    }

    actual fun outputScreen(info: ScreenInfo): ImageBitmap {
        TODO("Not yet implemented")
    }

    actual fun outputSound(info: SoundInfo) {
    }

    actual fun onKey(keyCode: Key, press: Boolean) {
    }

    actual class ScreenInfo {
        actual var buf: ByteArray
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var width: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var height: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var pitch: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var bpp: Int
            get() = TODO("Not yet implemented")
            set(value) {}
    }

    actual class SoundInfo {
        actual var buf: ByteArray
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var freq: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var bps: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var ch: Int
            get() = TODO("Not yet implemented")
            set(value) {}
        actual var sample: Int
            get() = TODO("Not yet implemented")
            set(value) {}
    }

    actual class InputInfo {
        actual var buf: IntArray
            get() = TODO("Not yet implemented")
            set(value) {}
    }
}
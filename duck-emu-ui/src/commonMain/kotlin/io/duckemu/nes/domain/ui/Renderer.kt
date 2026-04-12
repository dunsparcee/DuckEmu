@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.duckemu.nes.domain.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key

expect class Renderer() {
    val soundBufferState: Int

    class ScreenInfo {
        var buf: ByteArray
        var width: Int
        var height: Int
        var pitch: Int
        var bpp: Int
    }

    class SoundInfo {
        var buf: ByteArray
        var freq: Int
        var bps: Int
        var ch: Int
        var sample: Int
    }

    class InputInfo {
        var buf: IntArray
    }

    fun requestScreen(width: Int, height: Int): ScreenInfo
    fun requestSound(): SoundInfo?
    fun requestInput(padCount: Int, buttonCount: Int): InputInfo
    fun outputScreen(info: ScreenInfo): ImageBitmap
    fun outputSound(info: SoundInfo)
    fun onKey(keyCode: Key, press: Boolean)
    fun fillSilence()
}
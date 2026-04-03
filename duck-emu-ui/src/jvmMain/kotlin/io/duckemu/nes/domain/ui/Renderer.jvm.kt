@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.duckemu.nes.domain.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

actual class Renderer {
    private val scri = ScreenInfo()
    private val sndi = SoundInfo()
    private val inpi = InputInfo()

    private val line: SourceDataLine?
    private var lineBufferSize: Int = 32

    actual fun requestScreen(width: Int, height: Int): ScreenInfo {
        if (!(scri.width == width && scri.height == height)) {
            scri.width = width
            scri.height = height
            scri.buf = ByteArray(3 * width * height)
            scri.pitch = 3 * width
            scri.bpp = 24
        }
        return scri
    }

    actual fun outputScreen(info: ScreenInfo): ImageBitmap {
        val bytes = ByteArray(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
        for (i in 0..<SCREEN_WIDTH * SCREEN_HEIGHT) {
            bytes[i * 4 + 0] = info.buf[i * 3 + 2]
            bytes[i * 4 + 1] = info.buf[i * 3 + 1]
            bytes[i * 4 + 2] = info.buf[i * 3 + 0]
            bytes[i * 4 + 3] = 0xFF.toByte()
        }

        return Image.makeRaster(
            ImageInfo.makeN32(SCREEN_WIDTH, SCREEN_HEIGHT, ColorAlphaType.OPAQUE),
            bytes,
            SCREEN_WIDTH * 4
        ).toComposeImageBitmap()
    }

    actual fun requestSound(): SoundInfo? {
        return if (this.soundBufferState <= 0) sndi
        else null
    }

    actual fun outputSound(info: SoundInfo) {
        line?.write(info.buf, 0, info.sample * (info.bps / 8) * info.ch)
    }

    actual val soundBufferState: Int
        get() {
            if (line == null) {
                return 0
            }

            val rest = ((lineBufferSize - line.available()) / (sndi.bps / 8) / sndi.ch)

            if (rest < SAMPLES_PER_FRAME * BUFFER_FRAMES) return -1
            if (rest == SAMPLES_PER_FRAME * BUFFER_FRAMES) return 0
            return 1
        }

    init {
        line = getSound()
        line?.run {
            this.open()
            this.start()
            lineBufferSize = this.available()
        }

        val bufSamples: Int = SAMPLES_PER_FRAME

        sndi.bps = 16
        sndi.buf = ByteArray(bufSamples * (BPS / 8) * CHANNELS)
        sndi.ch = 2
        sndi.freq = SAMPLE_RATE
        sndi.sample = bufSamples

        inpi.buf = IntArray(16)
    }

    private fun getSound(): SourceDataLine? {
        try {
            val format = AudioFormat(
                SAMPLE_RATE.toFloat(), BPS, CHANNELS, true,
                false
            )
            val info = DataLine.Info(SourceDataLine::class.java, format)
            return AudioSystem.getLine(info) as SourceDataLine
        } catch (_: Exception) {
            return null
        }
    }

    actual fun onKey(keyCode: Key, press: Boolean) {
        for (i in 0..1) for (j in 0..7) if (keyCode == keysPlayers[i][j]) inpi.buf[i * 8 + j] = (if (press) 1 else 0)
    }

    actual fun requestInput(padCount: Int, buttonCount: Int): InputInfo {
        return inpi
    }

    companion object {
        private const val SCREEN_WIDTH = 256
        private const val SCREEN_HEIGHT = 240

        private const val SAMPLE_RATE = 48000
        private const val BPS = 16
        private const val CHANNELS = 2
        private const val BUFFER_FRAMES = 2

        private const val FPS = 60
        private const val SAMPLES_PER_FRAME: Int = SAMPLE_RATE / FPS

        val keysPlayers: Array<Array<Key>> = arrayOf(
            arrayOf(
                Key.Z, Key.X, Key.ShiftLeft,
                Key.Enter, Key.DirectionUp, Key.DirectionDown,
                Key.DirectionLeft, Key.DirectionRight
            ),
            arrayOf(
                Key.V, Key.B, Key.N, Key.M,
                Key.O, Key.Comma, Key.K,
                Key.L
            )
        )
    }

    actual class ScreenInfo {
        actual var buf: ByteArray = ByteArray(16)
        actual var width: Int = 0
        actual var height: Int = 0
        actual var pitch: Int = 0
        actual var bpp: Int = 0
    }

    actual class SoundInfo {
        actual var buf: ByteArray = ByteArray(16)
        actual var freq: Int = 0
        actual var bps: Int = 0
        actual var ch: Int = 0
        actual var sample: Int = 0
    }

    actual class InputInfo {
        actual var buf: IntArray = IntArray(16)
    }
}

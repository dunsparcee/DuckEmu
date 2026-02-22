@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package io.duckemu.nes.core.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.Frame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

actual class Renderer {
    lateinit var frame: Frame
    private val scri = ScreenInfo()
    private val sndi = SoundInfo()
    private val inpi = InputInfo()

    private val image = BufferedImage(
        SCREEN_WIDTH,
        SCREEN_HEIGHT, BufferedImage.TYPE_3BYTE_BGR
    )

    private val line: SourceDataLine
    private val lineBufferSize: Int

    actual fun outputMessage(msg: String?) {
        println(msg)
    }

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
            bytes[i * 4 + 0] = info.buf[i * 3 + 2] // R ← was B
            bytes[i * 4 + 1] = info.buf[i * 3 + 1] // G
            bytes[i * 4 + 2] = info.buf[i * 3 + 0] // B ← was R
            bytes[i * 4 + 3] = 0xFF.toByte()        // A
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
        line.write(info.buf, 0, info.sample * (info.bps / 8) * info.ch)
    }

    actual val soundBufferState: Int
        get() {
            val rest = ((lineBufferSize - line.available()) / (sndi.bps / 8)
                    / sndi.ch)
            if (rest < SAMPLES_PER_FRAME * BUFFER_FRAMES) return -1
            if (rest == SAMPLES_PER_FRAME * BUFFER_FRAMES) return 0
            return 1
        }

    init {
        val format = AudioFormat(
            SAMPLE_RATE.toFloat(), BPS, CHANNELS, true,
            false
        )
        val info = DataLine.Info(SourceDataLine::class.java, format)
        line = AudioSystem.getLine(info) as SourceDataLine
        line.open()
        line.start()
        lineBufferSize = line.available()

        val bufSamples: Int = SAMPLES_PER_FRAME

        sndi.bps = 16
        sndi.buf = ByteArray(bufSamples * (BPS / 8) * CHANNELS)
        sndi.ch = 2
        sndi.freq = SAMPLE_RATE
        sndi.sample = bufSamples

        inpi.buf = IntArray(16)
    }

    fun loadKey() {
        frame.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                onKey(e.getKeyCode(), true)
            }

            override fun keyReleased(e: KeyEvent) {
                onKey(e.getKeyCode(), false)
            }
        })
    }

    private fun onKey(keyCode: Int, press: Boolean) {
        for (i in 0..1) for (j in 0..7) if (keyCode == keyDef[i]!![j]) inpi.buf[i * 8 + j] = (if (press) 1 else 0)
    }

    actual fun requestInput(padCount: Int, buttonCount: Int): Renderer.InputInfo {
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
        private val SAMPLES_PER_FRAME: Int = SAMPLE_RATE / FPS

        val keyDef: Array<IntArray?> = arrayOf<IntArray?>(
            intArrayOf(
                KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_SHIFT,
                KeyEvent.VK_ENTER, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
                KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            ),
            intArrayOf(
                KeyEvent.VK_V, KeyEvent.VK_B, KeyEvent.VK_N, KeyEvent.VK_M,
                KeyEvent.VK_O, KeyEvent.VK_COMMA, KeyEvent.VK_K,
                KeyEvent.VK_L,
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

    actual fun run() {

    }


}

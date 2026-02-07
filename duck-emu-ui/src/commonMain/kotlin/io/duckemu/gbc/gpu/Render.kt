package io.duckemu.gbc.gpu

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.duckemu.gbc.BytesOperation
import io.duckemu.gbc.addons.Speed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal abstract class ScreenAbstract(
    protected var registers: ByteArray,
    protected var memory: Array<ByteArray>,
    protected var oam: ByteArray,
    protected var gbcFeatures: Boolean,
    private val screenListener: ScreenListener,
    private val maxFrameSkip: Int
) {
    private val speed: Speed = Speed()
    protected val MS_PER_FRAME: Int = 17
    protected val TILE_FLIPX: Int = 1
    protected val TILE_FLIPY: Int = 2
    protected var videoRam: ByteArray
    protected var videoRamBanks: Array<ByteArray>
    protected lateinit var colors: IntArray
    protected var gbPalette: IntArray = IntArray(12)
    protected var gbcRawPalette: IntArray = IntArray(128)
    protected var gbcPalette: IntArray = IntArray(64)
    protected var gbcMask: Int = 0
    protected var transparentCutoff: Int = 0

    protected var bgEnabled: Boolean = true
    protected var winEnabled: Boolean = true
    protected var spritesEnabled: Boolean = true
    protected var lcdEnabled: Boolean = true
    protected var spritePriorityEnabled: Boolean = true

    protected var timer: Int = 0
    protected var skipping: Boolean = true
    protected var frameCount: Int = 0
    protected var skipCount: Int = 0
    protected var lastSkipCount: Int = 0
    protected var bgWindowDataSelect: Boolean = true
    protected var doubledSprites: Boolean = false
    protected var hiBgTileMapAddress: Boolean = false
    protected var hiWinTileMapAddress: Boolean = false
    protected var tileOffset: Int = 0
    protected var tileCount: Int = 0
    protected var colorCount: Int = 0
    protected var width: Int = 160
    protected var height: Int = 144
    protected var frameBufferImage: ImageBitmap? = null

    init {
        if (this.gbcFeatures) {
            videoRamBanks = Array<ByteArray>(2) { ByteArray(0x2000) }
            tileCount = 384 * 2
            colorCount = 64
        } else {
            videoRamBanks = Array<ByteArray>(1) { ByteArray(0x2000) }
            tileCount = 384
            colorCount = 12
        }

        videoRam = videoRamBanks[0]
        this.memory[4] = videoRam

        for (i in gbcRawPalette.indices) gbcRawPalette[i] = -1000

        for (i in 0..<(gbcPalette.size shr 1)) gbcPalette[i] = -1

        for (i in (gbcPalette.size shr 1)..<gbcPalette.size) gbcPalette[i] = 0
    }

    fun unflatten(flatState: ByteArray, offset: Int): Int {
        var offset = offset
        for (i in videoRamBanks.indices) {
            flatState.copyInto(
                destination = videoRamBanks[i],
                destinationOffset = 0,
                startIndex = offset,
                endIndex = offset + 0x2000
            )
            offset += 0x2000
        }

        for (i in 0..11) {
            if ((i and 3) == 0) gbPalette[i] = 0x00ffffff and BytesOperation.getInt(flatState, offset)
            else gbPalette[i] = -0x1000000 or BytesOperation.getInt(flatState, offset)
            offset += 4
        }

        UpdateLCDCFlags(this.registers[0x40].toInt())

        if (this.gbcFeatures) {
            setVRamBank(flatState[offset++].toInt() and 0xff)
            for (i in 0..127) {
                setGBCPalette(i, flatState[offset++].toInt() and 0xff)
            }
        } else {
            invalidateAll(0)
            invalidateAll(1)
            invalidateAll(2)
        }

        return offset
    }

    fun flatten(flatState: ByteArray, offset: Int): Int {
        var offset = offset
        for (i in videoRamBanks.indices) {
            videoRamBanks[i].copyInto(
                destination = flatState,
                destinationOffset = offset,
                startIndex = 0,
                endIndex = 0x2000
            )
            offset += 0x2000
        }

        for (j in 0..11) {
            BytesOperation.setInt(flatState, offset, gbPalette[j])
            offset += 4
        }

        if (this.gbcFeatures) {
            flatState[offset++] = (if (tileOffset != 0) 1 else 0).toByte()
            for (i in 0..127) {
                flatState[offset++] = getGBCPalette(i).toByte()
            }
        }

        return offset
    }

    fun UpdateLCDCFlags(data: Int) {
        bgEnabled = true
        lcdEnabled = ((data and 0x80) != 0)
        hiWinTileMapAddress = ((data and 0x40) != 0)
        winEnabled = ((data and 0x20) != 0)
        bgWindowDataSelect = ((data and 0x10) != 0)
        hiBgTileMapAddress = ((data and 0x08) != 0)
        doubledSprites = ((data and 0x04) != 0)
        spritesEnabled = ((data and 0x02) != 0)

        if (this.gbcFeatures) {
            spritePriorityEnabled = ((data and 0x01) != 0)
        } else {
            if ((data and 0x01) == 0) {
                bgEnabled = false
                winEnabled = false
            }
        }
    }

    fun vBlank() {
        if (!speed.output()) return
        timer += MS_PER_FRAME
        frameCount++
        if (skipping) {
            skipCount++
            if (skipCount >= maxFrameSkip) {
                skipping = false
                val lag = Clock.System.now().toEpochMilliseconds().toInt() - timer

                if (lag > MS_PER_FRAME) timer += lag - MS_PER_FRAME
            } else skipping = (timer - (Clock.System.now().toEpochMilliseconds().toInt()) < 0)
            return
        }
        lastSkipCount = skipCount
        screenListener.onFrameReady(frameBufferImage, lastSkipCount)
        var now = Clock.System.now().toEpochMilliseconds().toInt()
        if (maxFrameSkip == 0) skipping = false
        else skipping = timer - now < 0
        while (timer > now + MS_PER_FRAME) {
            now = Clock.System.now().toEpochMilliseconds().toInt()
        }
        skipCount = 0
    }

    fun decodePalette(startIndex: Int, data: Int) {
        for (i in 0..3) gbPalette[startIndex + i] = colors[((data shr (2 * i)) and 0x03)]
        gbPalette[startIndex] = gbPalette[startIndex] and 0x00ffffff
    }

    open fun setGBCPalette(index: Int, data: Int) {
        if (gbcRawPalette[index] == data) return

        gbcRawPalette[index] = data
        if (index >= 0x40 && (index and 0x6) == 0) {
            return
        }

        val value = (gbcRawPalette[index or 1] shl 8) + gbcRawPalette[index and -2]

        gbcPalette[index shr 1] =
            gbcMask + ((value and 0x001F) shl 19) + ((value and 0x03E0) shl 6) + ((value and 0x7C00) shr 7)

        invalidateAll(index shr 3)
    }

    fun getGBCPalette(index: Int): Int {
        return gbcRawPalette[index]
    }

    fun setVRamBank(value: Int) {
        tileOffset = value * 384
        videoRam = videoRamBanks[value]
        this.memory[4] = videoRam
    }

    fun stopWindowFromLine() {
    }

    fun fixTimer() {
        timer = Clock.System.now().toEpochMilliseconds().toInt()
    }

    abstract fun addressWrite(addr: Int, data: Byte)

    abstract fun invalidateAll(pal: Int)

    abstract fun notifyScanline(line: Int)

    fun setSpeed(i: Int) {
        speed.setSpeed(i)
    }

    companion object {
        var weaveLookup: IntArray = IntArray(256)

        init {
            for (i in 1..255) {
                for (d in 0..7) weaveLookup[i] += ((i shr d) and 1) shl (d * 2)
            }
        }
    }
}
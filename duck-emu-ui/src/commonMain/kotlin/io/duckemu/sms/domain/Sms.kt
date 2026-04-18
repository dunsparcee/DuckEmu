package io.duckemu.sms.domain

import io.duckemu.sms.data.EmuState
import io.duckemu.sms.data.Setup
import kotlin.math.min

class Sms(
    private val screenListener: ScreenListener? = null
) {

    private val display = IntArray(Vdp.SMS_WIDTH * Vdp.SMS_HEIGHT)
    private var fps = 60
    private var noOfScanlines = Vdp.SMS_Y_PIXELS_NTSC
    private var cyclesPerLine = 0
    private var samplesPerFrame = 0
    private var samplesPerLine = IntArray(0)
    
    private var frameSkipCounter = 0
    var frameSkip = 0

    var vdp: Vdp = Vdp(display)
    var psg: SN76489 = SN76489()
    var ports: Ports = Ports(vdp, psg)
    var z80: Z80 = Z80(ports)

    init {
        setSMS()
    }

    fun setSMS() {
        EmuState.is_sms = true
        EmuState.is_gg = false
        EmuState.h_start = 0
        EmuState.h_end = 32
        EmuState.emuWidth = 256
        EmuState.emuHeight = 192
        setVideoTiming(Vdp.videoMode)
    }

    fun setGG() {
        EmuState.is_gg = true
        EmuState.is_sms = false
        EmuState.h_start = 5
        EmuState.h_end = 27
        EmuState.emuWidth = 160
        EmuState.emuHeight = 144
        setVideoTiming(Vdp.videoMode)
    }

    fun reset() {
        if (EmuState.is_gg) setGG() else setSMS()
        frameSkipCounter = frameSkip
        z80.reset()
        z80.resetMemory(null)
        ports.reset()
        vdp.reset()
        display.fill(0)
    }

    fun loadRom(romData: ByteArray) {
        val pages = splitRom(romData)
        z80.resetMemory(pages)
        reset()
    }

    private fun splitRom(romData: ByteArray): Array<ByteArray?> {
        var data = romData
        var size = data.size
        
        // Strip 512 Byte File Headers
        if ((size % 1024) != 0) {
            val newData = ByteArray(size - 512)
            data.copyInto(newData, 0, 512)
            data = newData
            size -= 512
        }

        val numberOfPages = size / Setup.PAGE_SIZE
        val pages = arrayOfNulls<ByteArray>(numberOfPages)

        for (i in 0 until numberOfPages) {
            val page = ByteArray(Setup.PAGE_SIZE)
            data.copyInto(page, 0, i * Setup.PAGE_SIZE, (i + 1) * Setup.PAGE_SIZE)
            pages[i] = page
        }
        
        return pages
    }

    fun setVideoTiming(mode: Int) {
        var clockSpeedHz = 0

        if (mode == Vdp.NTSC || EmuState.is_gg) {
            fps = 60
            noOfScanlines = Vdp.SMS_Y_PIXELS_NTSC
            clockSpeedHz = Setup.CLOCK_NTSC
        } else {
            fps = 50
            noOfScanlines = Vdp.SMS_Y_PIXELS_PAL
            clockSpeedHz = Setup.CLOCK_PAL
        }

        cyclesPerLine = (clockSpeedHz / fps / noOfScanlines) + 1
        Vdp.videoMode = mode

        psg.init(clockSpeedHz, Setup.SAMPLE_RATE)
        samplesPerFrame = Setup.SAMPLE_RATE / fps
        
        samplesPerLine = IntArray(noOfScanlines)
        var fractional = 0
        for (i in 0 until noOfScanlines) {
            val v: Int = ((samplesPerFrame shl 16) / noOfScanlines) + fractional
            fractional = v - ((v shr 16) shl 16)
            samplesPerLine[i] = v shr 16
        }
    }

    fun emulateFrame(): IntArray? {
        for (lineno in 0 until noOfScanlines) {
            if (Setup.ACCURATE_INTERRUPT_EMULATION && lineno == 193) {
                z80.run(cyclesPerLine, 8)
                vdp.setVBlankFlag()
                z80.run(0, 0)
            } else {
                z80.run(cyclesPerLine, 0)
            }

            updateSound(lineno)

            vdp.line = lineno

            if (frameSkipCounter == 0 && lineno < 192) {
                vdp.drawLine(lineno)
            }

            vdp.interrupts(lineno)
        }

        if (frameSkipCounter > 0) {
            frameSkipCounter--
            return null
        } else {
            frameSkipCounter = frameSkip
            return display
        }
    }

    private fun updateSound(lineno: Int) {
        // TODO: Implement audio buffer handling
        //psg.run(samplesPerLine[lineno])
    }

    fun pause() {
        z80.nmi()
    }
}

fun interface ScreenListener {
    fun onFrameReady(display: IntArray)
}

package io.duckemu.nes.domain

import io.duckemu.nes.domain.ui.Renderer
import kotlin.math.max
import kotlin.math.min


class Apu(private val nes: Nes) {
    fun reset() {
        ch = Array(4) {
            ChState()
        }

        sch = Array(4) {
            ChState()
        }

        dmc = DmcState()

        ch?.get(3)?.shiftRegister = 1
        befSync = 0
        befClock = 0
    }

    fun read(adr: Short): Byte {
        if (adr.toInt() == 0x4015) {
            sync()
            return (((if (sch!![0].length == 0) 0 else 1) or ((if (sch!![1].length == 0) 0 else 1) shl 1) or ((if (sch!![2].length == 0) 0 else 1) shl 2)
                    or ((if (sch!![3].length == 0) 0 else 1) shl 3) or ((if (sdmc.enable) 1 else 0) shl 4) or ((if (sdmc.irq) 1 else 0) shl 7))).toByte()
        }
        return 0xA0.toByte()
    }

    fun write(adr: Short, dat: Byte) {
        // delay writing data for generating sound
        writeQueue.add(WriteDat(nes.cpu.masterClock, adr, dat))
        while (writeQueue.size > 1000) {
            val wd = writeQueue.removeFirst()
            doWrite(ch!!, dmc, wd.adr, wd.dat)
        }
        // process for status register
        sync()
        doWrite(sch!!, sdmc, adr, dat)
    }

    fun genAudio(info: Renderer.SoundInfo) {
        val cpuClock = nes.cpu.frequency

        val curClock = nes.cpu.masterClock
        val sample = info.sample

        val buf = info.buf
        val span = info.ch * (info.bps / 8)

        val incClk = ((curClock - befClock).toDouble()) / sample // executed
        val sampleClk = cpuClock / info.freq // CPU clocks per sample

        buf.fill(0x00.toByte())

        if (nes.mapper != null)  // external APU
            nes.mapper!!.audio(info)

        for (i in 0..<sample) {
            //long pos = (curClock - befClock) * i / sample + befClock;
            val pos = (befClock + sampleClk * i).toLong()
            while (!writeQueue.isEmpty() && writeQueue.first().clk <= pos) {
                val wd = writeQueue.removeFirst()
                doWrite(ch!!, dmc, wd.adr, wd.dat)
            }

            var v = 0.0

            for (j in 0..3) {
                val cc = ch!![j]

                var pause = false
                if (!cc.enable) continue
                if (cc.length == 0) pause = true

                // length counter
                if (cc.lengthEnable) {
                    val length_clk = cpuClock / 60.0
                    cc.lengthClk += incClk
                    while (cc.lengthClk > length_clk) {
                        cc.lengthClk -= length_clk
                        if (cc.length > 0) cc.length--
                    }
                }
                // linear counter
                if (j == Companion.TRI) {
                    if (cc.counterStart != 0) cc.linearCounter = cc.linearLatch
                    else {
                        val linear_clk = cpuClock / 240.0
                        cc.linearClk += incClk
                        while (cc.linearClk > linear_clk) {
                            cc.linearClk -= linear_clk
                            if (cc.linearCounter > 0) cc.linearCounter--
                        }
                    }
                    if (!cc.holdnote && cc.linearCounter != 0) cc.counterStart = 0

                    if (cc.linearCounter == 0) pause = true
                }

                // envelope
                var vol = 16
                if (j != Companion.TRI) {
                    if (cc.envelopeEnable) {
                        val decay_clk = cpuClock / (240.0 / (cc.envelopeRate + 1))
                        cc.envelopeClk += incClk
                        while (cc.envelopeClk > decay_clk) {
                            cc.envelopeClk -= decay_clk
                            if (cc.volume > 0) cc.volume--
                            else {
                                if (!cc.lengthEnable)  // loop
                                    cc.volume = 0xf
                                else cc.volume = 0
                            }
                        }
                    }
                    vol = cc.volume
                }

                // sweep
                if ((j == Companion.SQ1 || j == Companion.SQ2) && cc.sweepEnable && !cc.sweepPausing) {
                    val sweep_clk = cpuClock / (120.0 / (cc.sweepRate + 1))
                    cc.sweepClk += incClk
                    while (cc.sweepClk > sweep_clk) {
                        cc.sweepClk -= sweep_clk
                        if (cc.sweepShift != 0 && cc.length != 0) {
                            if (!cc.sweepMode)  // increase
                                cc.waveLength += cc.waveLength shr cc.sweepShift
                            else  // decrease
                                cc.waveLength += (cc.waveLength shr cc.sweepShift).inv()
                            if (cc.waveLength < 0x008) cc.sweepPausing = true
                            if ((cc.waveLength and 0x7FF.inv()) != 0) cc.sweepPausing = true
                            cc.waveLength = cc.waveLength and 0x7FF
                        }
                    }
                }

                pause = pause or cc.sweepPausing
                pause = pause or (cc.waveLength == 0)
                if (pause) continue

                // generate wave
                val t = (if (j == Companion.SQ1 || j == Companion.SQ2) sqProduce(
                    cc,
                    sampleClk
                ) else if (j == Companion.TRI) triProduce(
                    cc,
                    sampleClk
                ) else if (j == Companion.NOI)
                    noiProduce(cc, sampleClk)
                else
                    0.0)

                v += t * vol / 16
            }

            v += dmcProduce(sampleClk)

            if (info.bps == 8) {
                buf[i * span + 0] = (buf[i * span + 0] + (v * 30).toInt().toByte()).toByte()
                if (info.ch == 2) buf[i * span + 1] = (buf[i * span + 1] + (v * 30).toInt().toByte()).toByte()
            } else {
                run {
                    val b = ((buf[i * span + 0].toInt() and 0xff) or (buf[i * span + 1].toInt() shl 8)).toShort()
                    val w = min(32767.0, max(-32767.0, b + v * 8000)).toInt().toShort()
                    buf[i * span + 0] = (w.toInt() and 0xff).toByte()
                    buf[i * span + 1] = (w.toInt() shr 8).toByte()
                }
                if (info.ch == 2) {
                    val b = ((buf[i * span + 2].toInt() and 0xff) or (buf[i * span + 3].toInt() shl 8)).toShort()
                    val w = min(32767.0, max(-32767.0, b + v * 8000)).toInt().toShort()
                    buf[i * span + 2] = (w.toInt() and 0xff).toByte()
                    buf[i * span + 3] = (w.toInt() shr 8).toByte()
                }
            }
        }
        befClock = curClock
    }

    fun sync() {
        val cpuClock = nes.cpu.frequency
        val cur = nes.cpu.masterClock
        val adv_clock = (cur - befSync).toInt()

        // update 4 channels
        for (j in 0..3) {
            val cc = sch!![j]
            // length counter
            if (cc.enable && cc.lengthEnable) {
                val length_clk = cpuClock / 60.0
                cc.lengthClk += adv_clock.toDouble()
                val dec = (cc.lengthClk / length_clk).toInt()
                cc.lengthClk -= length_clk * dec
                cc.length = max(0, cc.length - dec)
            }
        }
        // update DMC
        if (sdmc.enable) {
            sdmc.clk += adv_clock.toDouble()
            val dec = (sdmc.clk / sdmc.waveLength).toInt()
            sdmc.clk -= (dec * sdmc.waveLength).toDouble()

            val rest = sdmc.shiftCount + sdmc.length * 8 - dec
            if (rest <= 0) { // end playback
                if ((sdmc.playbackMode and 1) != 0) { // loop
                    sdmc.length = rest / 8
                    while (sdmc.length < 0) sdmc.length += sdmc.lengthLatch
                    sdmc.shiftCount = 0
                } else {
                    sdmc.enable = false
                    if (sdmc.playbackMode == 2) { // IRQ occur
                        sdmc.irq = true
                        nes.cpu.setIrq(true)
                    }
                }
            } else {
                sdmc.length = rest / 8
                sdmc.shiftCount = rest % 8
            }
        }

        befSync = cur
    }

    inner class ChState {
        var enable: Boolean = false

        var waveLength: Int = 0

        var lengthEnable: Boolean = false
        var length: Int = 0
        var lengthClk: Double = 0.0

        var volume: Int = 0
        var envelopeRate: Int = 0
        var envelopeEnable: Boolean = false
        var envelopeClk: Double = 0.0

        var sweepEnable: Boolean = false
        var sweepRate: Int = 0
        var sweepMode: Boolean = false
        var sweepShift: Int = 0
        var sweepClk: Double = 0.0
        var sweepPausing: Boolean = false

        var duty: Int = 0

        var linearLatch: Int = 0
        var linearCounter: Int = 0
        var holdnote: Boolean = false
        var counterStart: Int = 0
        var linearClk: Double = 0.0

        var randomType: Boolean = false

        var step: Int = 0
        var stepClk: Double = 0.0
        var shiftRegister: Int = 0
    }

    private var ch: Array<ChState>? = null
    private var sch: Array<ChState>? = null

    inner class DmcState {
        var enable: Boolean = false
        var irq: Boolean = false

        var playbackMode: Int = 0
        var waveLength: Int = 0
        var clk: Double = 0.0

        var counter: Int = 0
        var length: Int = 0
        var lengthLatch: Int = 0
        var adr: Short = 0
        var adrLatch: Short = 0
        var shiftReg: Int = 0
        var shiftCount: Int = 0
        var dacLsb: Int = 0
    }

    private var dmc: DmcState = DmcState()
    private val sdmc: DmcState = DmcState()

    fun doWrite(ch: Array<ChState>, dmc: DmcState, adr: Short, bdat: Byte) {
        val cn = (adr.toInt() and 0x1f) / 4
        var cc: ChState? = null
        if (cn < 4) cc = ch[cn]

        val dat = bdat.toInt() and 0xff

        when (adr.toInt()) {
            0x4000, 0x4004, 0x400C -> {
                cc?.envelopeEnable = (dat and 0x10) == 0
                if (cc?.envelopeEnable == true) {
                    cc.volume = 0xf
                    cc.envelopeRate = dat and 0xf
                } else cc?.volume = dat and 0xf
                cc?.lengthEnable = (dat and 0x20) == 0
                cc?.duty = dat shr 6
                cc?.envelopeClk = 0.0
            }

            0x4008 -> {
                cc?.linearLatch = dat and 0x7f
                cc?.holdnote = (dat and 0x80) != 0
            }

            0x4001, 0x4005 -> {
                cc?.sweepShift = dat and 7
                cc?.sweepMode = (dat and 0x8) != 0
                cc?.sweepRate = (dat shr 4) and 7
                cc?.sweepEnable = (dat and 0x80) != 0
                cc?.sweepClk = 0.0
                cc?.sweepPausing = false
            }

            0x4009, 0x400D -> {}
            0x4002, 0x4006, 0x400A -> cc?.waveLength = (cc.waveLength and 0xff.inv()) or dat
            0x400E -> {
                cc?.waveLength = convTable[dat and 0xf] - 1
                cc?.randomType = (dat and 0x80) != 0
            }

            0x4003, 0x4007, 0x400B, 0x400F -> {
                if (cn != 3) cc?.waveLength = (cc.waveLength and 0xff) or ((dat and 0x7) shl 8)
                if ((dat and 0x8) == 0) cc?.length = lengthTbl[dat shr 4]
                else cc?.length = if ((dat shr 4) == 0) 0x7f else (dat shr 4)
                if (cn == 2) cc?.counterStart = 1

                if (cc?.envelopeEnable == true) {
                    cc.volume = 0xf
                    cc.envelopeClk = 0.0
                }
            }

            0x4010 -> {
                dmc.playbackMode = dat shr 6
                dmc.waveLength = dacTable[dat and 0xf] / 8
                if ((dat shr 7) == 0) dmc.irq = false
            }

            0x4011 -> {
                dmc.dacLsb = dat and 1
                dmc.counter = (dat shr 1) and 0x3f
            }

            0x4012 -> dmc.adrLatch = ((dat shl 6) or 0xC000).toShort()
            0x4013 -> dmc.lengthLatch = (dat shl 4) + 1
            0x4015 -> {
                ch[0].enable = (dat and 1) != 0
                if (!ch[0].enable) ch[0].length = 0
                ch[1].enable = (dat and 2) != 0
                if (!ch[1].enable) ch[1].length = 0
                ch[2].enable = (dat and 4) != 0
                if (!ch[2].enable) ch[2].length = 0
                ch[3].enable = (dat and 8) != 0
                if (!ch[3].enable) ch[3].length = 0

                if ((dat and 0x10) != 0) {
                    if (!dmc.enable) {
                        dmc.adr = dmc.adrLatch
                        dmc.length = dmc.lengthLatch
                        dmc.shiftCount = 0
                    }
                    dmc.enable = true
                } else dmc.enable = false
                dmc.irq = false
            }
        }
    }

    fun sqProduce(cc: ChState, clk: Double): Double {
        cc.stepClk += clk
        val ret: Double = (0.5 - sqWav.get(cc.duty)[cc.step])
        val term = (cc.waveLength + 1).toDouble()
        if (cc.stepClk >= term) {
            val t = (cc.stepClk / term).toInt()
            cc.stepClk -= term * t
            cc.step = (cc.step + t) % 16
        }
        return ret
    }

    fun triProduce(cc: ChState, clk: Double): Double {
        cc.stepClk += clk
        val ret: Double = (Companion.triWav[cc.step] / 16.0 - 0.5)
        val term = (cc.waveLength + 1).toDouble()
        if (cc.stepClk >= term) {
            val t = (cc.stepClk / term).toInt()
            cc.stepClk -= term * t
            cc.step = (cc.step + t) % 32
        }
        return ret
    }

    fun noiProduce(cc: ChState, clk: Double): Double {
        cc.stepClk += clk
        val ret = 0.5 - (cc.shiftRegister shr 14)
        val term = (cc.waveLength + 1).toDouble()

        while (cc.stepClk >= term) {
            cc.stepClk -= term
            val t = cc.shiftRegister
            if (cc.randomType) cc.shiftRegister = ((t shl 1) or (((t shr 14) xor (t shr 8)) and 1)) and 0x7fff
            else cc.shiftRegister = ((t shl 1) or (((t shr 14) xor (t shr 13)) and 1)) and 0x7fff
        }
        return ret
    }

    fun dmcProduce(clk: Double): Double {
        if (!dmc.enable) return ((((dmc.counter shl 1) or dmc.dacLsb) - 64) / 32.0)

        dmc.clk += clk
        while (dmc.clk > dmc.waveLength) {
            dmc.clk -= dmc.waveLength.toDouble()
            if (dmc.shiftCount == 0) {
                if (dmc.length == 0) { // is end?
                    if ((dmc.playbackMode and 1) != 0) { // loop mode
                        dmc.adr = dmc.adrLatch
                        dmc.length = dmc.lengthLatch
                    } else {
                        dmc.enable = false
                        if (dmc.playbackMode == 2) { // occur IRQ
                            dmc.irq = true
                            // nes.getCpu().setIrq(true); //
                            // actually, IRQ occurs at sync()
                        }
                        return ((((dmc.counter shl 1) or dmc.dacLsb) - 64) / 32.0)
                    }
                }
                dmc.shiftCount = 8
                dmc.shiftReg = nes.mbc.read(dmc.adr).toInt()
                if (dmc.adr.toInt() == 0xFFFF) dmc.adr = 0x8000.toShort()
                else dmc.adr++
                dmc.length--
            }

            val b = dmc.shiftReg and 1
            if (b == 0 && dmc.counter != 0)  // decrement
                dmc.counter--
            if (b == 1 && dmc.counter != 0x3F) dmc.counter++
            dmc.counter = dmc.counter and 0x3f
            dmc.shiftCount--
            dmc.shiftReg = dmc.shiftReg shr 1
        }
        return ((((dmc.counter shl 1) or dmc.dacLsb) - 64) / 32.0)
    }

    private inner class WriteDat(var clk: Long, var adr: Short, var dat: Byte)

    private val writeQueue: ArrayDeque<WriteDat> = ArrayDeque<WriteDat>()
    private var befClock: Long = 0
    private var befSync: Long = 0

    init {
        reset()
    }

    companion object {
        const val SQ1: Int = 0
        const val SQ2: Int = 1
        const val TRI: Int = 2
        const val NOI: Int = 3
        const val DMC: Int = 4

        val convTable: IntArray = intArrayOf(
            0x002,
            0x004,
            0x008,
            0x010,
            0x020,
            0x030,
            0x040,
            0x050,
            0x065,
            0x07F,
            0x0BE,
            0x0FE,
            0x17D,
            0x1FC,
            0x3F9,
            0x7F2,
        )

        val lengthTbl: IntArray =
            intArrayOf(0x05, 0x06, 0x0A, 0x0C, 0x14, 0x18, 0x28, 0x30, 0x50, 0x60, 0x1E, 0x24, 0x07, 0x08, 0x0E, 0x10)

        val dacTable: IntArray = intArrayOf(
            0xD60,
            0xBE0,
            0xAA0,
            0xA00,
            0x8F0,
            0x7F0,
            0x710,
            0x6B0,
            0x5F0,
            0x500,
            0x470,
            0x400,
            0x350,
            0x2A0,
            0x240,
            0x1B0,
        )

        val sqWav: Array<IntArray> = arrayOf<IntArray>(
            intArrayOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1),
        )

        val triWav: IntArray = intArrayOf(
            0,
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            15,
            14,
            13,
            12,
            11,
            10,
            9,
            8,
            7,
            6,
            5,
            4,
            3,
            2,
            1,
            0,
        )
    }
}
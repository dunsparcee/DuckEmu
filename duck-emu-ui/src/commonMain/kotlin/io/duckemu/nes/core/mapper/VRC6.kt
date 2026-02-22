package io.duckemu.nes.core.mapper

import io.duckemu.nes.core.Nes
import io.duckemu.gbc.domain.nes.core.Ppu
import io.duckemu.nes.core.ui.Renderer


class VRC6 : MapperAdapter() {
    override fun setNes(nes: Nes) {
        this.nesCore = nes
    }

    override fun mapperNo(): Int {
        return 24
    }

    override fun reset() {
        val romSize = nesCore.rom.romSize()

        nesCore.mbc.mapRom(0, 0)
        nesCore.mbc.mapRom(1, 1)
        nesCore.mbc.mapRom(2, (romSize - 1) * 2)
        nesCore.mbc.mapRom(3, (romSize - 1) * 2 + 1)

        irqCount = 0
        irqEnable = 0
        irqLatch = 0
        befClk = 0

        sq[0] = SqState()
        sq[1] = SqState()
        saw = SawState()

        writeQueue = ArrayDeque<WriteDat>()
    }

    override fun write(sadr: Short, sdat: Byte) {
        val adr = sadr.toInt() and 0xffff
        val dat = sdat.toInt() and 0xff

        when (adr and 0xF003) {
            0x8000 -> {
                nesCore.mbc.mapRom(0, dat * 2)
                nesCore.mbc.mapRom(1, dat * 2 + 1)
            }

            0xC000 -> nesCore.mbc.mapRom(2, dat)
            0xD000 -> nesCore.mbc.mapVrom(0, dat)
            0xD001 -> nesCore.mbc.mapVrom(1, dat)
            0xD002 -> nesCore.mbc.mapVrom(2, dat)
            0xD003 -> nesCore.mbc.mapVrom(3, dat)
            0xE000 -> nesCore.mbc.mapVrom(4, dat)
            0xE001 -> nesCore.mbc.mapVrom(5, dat)
            0xE002 -> nesCore.mbc.mapVrom(6, dat)
            0xE003 -> nesCore.mbc.mapVrom(7, dat)
            0xB003 -> when ((dat shr 2) and 3) {
                0 -> nesCore.ppu.setMirroring(Ppu.MirrorType.VERTICAL)
                1 -> nesCore.ppu.setMirroring(Ppu.MirrorType.HOLIZONTAL)
                2 -> nesCore.ppu.setMirroring(0, 0, 0, 0)
                3 -> nesCore.ppu.setMirroring(1, 1, 1, 1)
            }

            0xF000 -> irqLatch = dat
            0xF001 -> {
                irqEnable = dat and 3
                if ((irqEnable and 2) != 0) irqCount = irqLatch
            }

            0xF002 -> if ((irqEnable and 1) != 0) irqEnable = irqEnable or 2
            else irqEnable = irqEnable and 1

            0x9000, 0x9001, 0x9002, 0xA000, 0xA001, 0xA002, 0xB000, 0xB001, 0xB002 -> {
                writeQueue!!.add(
                    WriteDat(
                        nesCore.cpu.masterClock, sadr,
                        sdat
                    )
                )
                while (writeQueue!!.size > 1000) {
                    val wd = writeQueue!!.removeFirst()
                    sndWrite(wd.adr, wd.dat)
                }
            }
        }
    }

    private fun sndWrite(sadr: Short, sdat: Byte) {
        val adr = sadr.toInt() and 0xffff
        val dat = sdat.toInt() and 0xff
        when (adr and 0xF003) {
            0x9000, 0xA000 -> {
                sq[(adr shr 12) - 9]!!.duty = ((dat shr 4) and 7)
                sq[(adr shr 12) - 9]!!.volume = dat and 0xF
                sq[(adr shr 12) - 9]!!.gate = (dat shr 7) != 0
            }

            0x9001, 0xA001 -> sq[(adr shr 12) - 9]!!.freq = (sq[(adr shr 12) - 9]!!.freq and 0xFF.inv()) or dat
            0x9002, 0xA002 -> {
                sq[(adr shr 12) - 9]!!.freq = ((sq[(adr shr 12) - 9]!!.freq and 0xFF)
                        or ((dat and 0xF) shl 8))
                sq[(adr shr 12) - 9]!!.enable = (dat shr 7) != 0
            }

            0xB000 -> saw!!.phase = dat and 0x3F
            0xB001 -> saw!!.freq = (saw!!.freq and 0xFF.inv()) or dat
            0xB002 -> {
                saw!!.freq = (saw!!.freq and 0xFF) or ((dat and 0xF) shl 8)
                saw!!.enable = (dat shr 7) != 0
            }
        }
    }

    override fun hblank(line: Int) {
        if ((irqEnable and 2) != 0) {
            if (irqCount >= 0xFF) {
                nesCore.cpu.setIrq(true)
                irqCount = irqLatch
            } else irqCount++
        }
    }

    override fun audio(info: Renderer.SoundInfo) {
        val cpuClk = nesCore.cpu.frequency
        val sampleClk = cpuClk / info.freq
        val curClk = nesCore.cpu.masterClock
        val span = info.bps / 8 * info.ch

        for (i in 0..<info.sample) {
            // long cur = (curClk - befClk) * i / info.sample + befClk;
            val cur = (befClk + sampleClk * i).toLong()
            while (!writeQueue!!.isEmpty() && cur >= writeQueue!!.first().clk) {
                val wd = writeQueue!!.removeFirst()
                sndWrite(wd.adr, wd.dat)
            }

            val d = ((sqProduce(sq[0]!!, sampleClk)
                    + sqProduce(sq[1]!!, sampleClk) + sawProduce(sampleClk))) / 32.0

            if (info.bps == 16) {
                val v = (d * 8000).toInt().toShort()
                info.buf[i * span + 0] = (v.toInt() and 0xff).toByte()
                info.buf[i * span + 1] = (v.toInt() shr 8).toByte()
                if (info.ch == 2) {
                    info.buf[i * span + 2] = (v.toInt() and 0xff).toByte()
                    info.buf[i * span + 3] = (v.toInt() shr 8).toByte()
                }
            } else if (info.bps == 8) {
                info.buf[i * span + 0] = (d * 30).toInt().toByte()
                if (info.ch == 2) {
                    info.buf[i * span + 1] = (d * 30).toInt().toByte()
                }
            }
        }

        befClk = curClk
    }

    private var irqLatch = 0
    private var irqCount = 0
    private var irqEnable = 0

    private inner class SqState {
        var duty: Int = 0
        var volume: Int = 0
        var gate: Boolean = false
        var freq: Int = 0
        var enable: Boolean = false

        var step: Int = 0
        var clk: Double = 0.0
    }

    private val sq = arrayOfNulls<SqState>(2)

    internal inner class SawState {
        var phase: Int = 0
        var freq: Int = 0
        var enable: Boolean = false

        var step: Int = 0
        var clk: Double = 0.0
    }

    private var saw: SawState? = null

    private fun sqProduce(sq: SqState, clk: Double): Int {
        if (!sq.enable) return 0
        if (sq.gate) return sq.volume
        sq.clk += clk
        val adv = (sq.clk / (sq.freq + 1)).toInt()
        sq.clk -= ((sq.freq + 1) * adv).toDouble()
        sq.step = ((sq.step + adv) % 16)
        return (if (sq.step > sq.duty) 1 else 0) * sq.volume
    }

    private fun sawProduce(clk: Double): Int {
        if (!saw!!.enable) return 0
        saw!!.clk += clk
        val adv = (saw!!.clk / (saw!!.freq + 1)).toInt()
        saw!!.clk -= ((saw!!.freq + 1) * adv).toDouble()
        saw!!.step = ((saw!!.step + adv) % 7)
        return ((saw!!.step * saw!!.phase) shr 3) and 0x1f
    }

    private class WriteDat(var clk: Long, var adr: Short, var dat: Byte)

    private var writeQueue: ArrayDeque<WriteDat>? = null

    private var befClk: Long = 0
}

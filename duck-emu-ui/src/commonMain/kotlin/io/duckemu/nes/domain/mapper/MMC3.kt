package io.duckemu.nes.domain.mapper

import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.Ppu
import io.duckemu.nes.util.bit

class MMC3 : MapperAdapter() {

    override fun setNes(nes: Nes) {
        this.nesCore = nes
    }

    override fun mapperNo(): Int {
        return 4
    }

    override fun reset() {
        romSize = nesCore.rom.romSize()
        prgPage[0] = 0
        prgPage[1] = 1
        chrPage[0] = 0
        chrPage[1] = 1
        chrPage[2] = 2
        chrPage[3] = 3
        chrPage[4] = 4
        chrPage[5] = 5
        chrPage[6] = 6
        chrPage[7] = 7

        prgSwap = false
        chrSwap = false

        setRom()
        setVrom()
    }

    override fun write(adr: Short, bdat: Byte) {
        val dat = bdat.toInt() and 0xff
        when (adr.toInt() and 0xE001) {
            0x8000 -> {
                cmd = dat and 7
                prgSwap = bit(dat, 6)
                chrSwap = bit(dat, 7)
            }

            0x8001 -> when (cmd) {
                0 -> {
                    chrPage[0] = dat and 0xfe
                    chrPage[1] = (dat and 0xfe) + 1
                    setVrom()
                }

                1 -> {
                    chrPage[2] = dat and 0xfe
                    chrPage[3] = (dat and 0xfe) + 1
                    setVrom()
                }

                2 -> {
                    chrPage[4] = dat
                    setVrom()
                }

                3 -> {
                    chrPage[5] = dat
                    setVrom()
                }

                4 -> {
                    chrPage[6] = dat
                    setVrom()
                }

                5 -> {
                    chrPage[7] = dat
                    setVrom()
                }

                6 -> {
                    prgPage[0] = dat
                    setRom()
                }

                7 -> {
                    prgPage[1] = dat
                    setRom()
                }
            }

            0xA000 -> if (!nesCore.rom.isFourScreen) nesCore.ppu.setMirroring(
                if ((dat and 1) != 0)
                    Ppu.MirrorType.HOLIZONTAL
                else
                    Ppu.MirrorType.VERTICAL
            )

            0xA001 -> if ((dat and 0x80) != 0);
            0xC000 -> irqCounter = dat
            0xC001 -> irqLatch = dat
            0xE000 -> {
                irqEnable = false
                irqCounter = irqLatch
            }

            0xE001 -> irqEnable = true
        }
    }

    override fun hblank(line: Int) {
        if (irqEnable && line >= 0 && line < 239 && nesCore.regs.drawEnabled()) {
            if ((irqCounter--) == 0) {
                irqCounter = irqLatch
                nesCore.cpu.setIrq(true)
            }
        }
    }

    private fun setRom() {
        if (prgSwap) {
            nesCore.mbc.mapRom(0, (romSize - 1) * 2)
            nesCore.mbc.mapRom(1, prgPage[1])
            nesCore.mbc.mapRom(2, prgPage[0])
            nesCore.mbc.mapRom(3, (romSize - 1) * 2 + 1)
        } else {
            nesCore.mbc.mapRom(0, prgPage[0])
            nesCore.mbc.mapRom(1, prgPage[1])
            nesCore.mbc.mapRom(2, (romSize - 1) * 2)
            nesCore.mbc.mapRom(3, (romSize - 1) * 2 + 1)
        }
    }

    private fun setVrom() {
        for (i in 0..7) nesCore.mbc.mapVrom((i + (if (chrSwap) 4 else 0)) % 8, chrPage[i])
    }

    private var romSize = 0

    private var cmd = 0
    private var prgSwap = false
    private var chrSwap = false

    private var irqCounter = 0
    private var irqLatch = 0
    private var irqEnable = false

    private val prgPage = IntArray(2)
    private val chrPage = IntArray(8)
}
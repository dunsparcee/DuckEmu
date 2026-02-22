package io.duckemu.nes.core.mapper

import io.duckemu.gbc.domain.nes.core.Ppu
import io.duckemu.nes.core.util.bit

class MMC1 : MapperAdapter() {
    override fun mapperNo(): Int {
        return 1
    }

    override fun reset() {
        romSize = nesCore.rom.romSize()
        sizeInKB = romSize * 16

        buf = 0
        cnt = buf
        mirror = false
        oneScreen = false
        switchArea = true
        switchSize = true
        vromSwitchSize = false
        swapBase = 0
        vromPage[1] = 1
        vromPage[0] = vromPage[1]
        romPage[0] = 0
        romPage[1] = romSize - 1

        setBank()
    }

    override fun write(sadr: Short, sdat: Byte) {
        val adr = sadr.toInt() and 0xffff
        var dat = sdat.toInt() and 0xff

        if ((dat and 0x80) != 0) {
            cnt = 0
            buf = 0
            return
        }
        buf = buf or ((dat and 1) shl cnt)
        cnt++
        if (cnt < 5) return
        dat = buf and 0xff
        cnt = 0
        buf = cnt

        if (adr >= 0x8000 && adr <= 0x9FFF) { // reg0
            mirror = bit(dat, 0)
            oneScreen = bit(dat, 1)
            switchArea = bit(dat, 2)
            switchSize = bit(dat, 3)
            vromSwitchSize = bit(dat, 4)

            setMirroring()
        } else if (adr >= 0xA000 && adr <= 0xBFFF) { // reg1
            if (sizeInKB == 512) {
                swapBase = if (bit(dat, 4)) 16 else 0
                setBank()
            } else if (sizeInKB == 1024) {
            }

            vromPage[0] = dat and 0xf
            if (vromSwitchSize) {
                nesCore.mbc.mapVrom(0, vromPage[0] * 4)
                nesCore.mbc.mapVrom(1, vromPage[0] * 4 + 1)
                nesCore.mbc.mapVrom(2, vromPage[0] * 4 + 2)
                nesCore.mbc.mapVrom(3, vromPage[0] * 4 + 3)
            } else {
                nesCore.mbc.mapVrom(0, vromPage[0] * 4)
                nesCore.mbc.mapVrom(1, vromPage[0] * 4 + 1)
                nesCore.mbc.mapVrom(2, vromPage[0] * 4 + 2)
                nesCore.mbc.mapVrom(3, vromPage[0] * 4 + 3)
                nesCore.mbc.mapVrom(4, vromPage[0] * 4 + 4)
                nesCore.mbc.mapVrom(5, vromPage[0] * 4 + 5)
                nesCore.mbc.mapVrom(6, vromPage[0] * 4 + 6)
                nesCore.mbc.mapVrom(7, vromPage[0] * 4 + 7)
            }
        } else if (adr >= 0xC000 && adr <= 0xDFFF) { // reg2
            vromPage[1] = dat and 0xf
            if (vromSwitchSize) {
                nesCore.mbc.mapVrom(4, vromPage[1] * 4)
                nesCore.mbc.mapVrom(5, vromPage[1] * 4 + 1)
                nesCore.mbc.mapVrom(6, vromPage[1] * 4 + 2)
                nesCore.mbc.mapVrom(7, vromPage[1] * 4 + 3)
            }

            if (sizeInKB == 1024) {
            }
        } else if (adr >= 0xE000) { // reg3
            if (switchSize) { // 16K
                if (switchArea) {
                    romPage[0] = dat and 0xf
                    romPage[1] = (romSize - 1) and 0xf
                } else {
                    romPage[0] = 0
                    romPage[1] = dat and 0xf
                }
            } else { // 32K
                romPage[0] = dat and 0xe
                romPage[1] = (dat and 0xe) or 1
            }
            setBank()
        }
    }

    private fun setBank() {
        nesCore.mbc.mapRom(0, (swapBase or romPage[0]) * 2)
        nesCore.mbc.mapRom(1, (swapBase or romPage[0]) * 2 + 1)
        nesCore.mbc.mapRom(2, (swapBase or romPage[1]) * 2)
        nesCore.mbc.mapRom(3, (swapBase or romPage[1]) * 2 + 1)
    }

    private fun setMirroring() {
        if (oneScreen) nesCore?.ppu?.setMirroring(
            if (mirror)
                Ppu.MirrorType.HOLIZONTAL
            else
                Ppu.MirrorType.VERTICAL
        )
        else if (mirror) nesCore.ppu.setMirroring(1, 1, 1, 1)
        else nesCore.ppu.setMirroring(0, 0, 0, 0)
    }

    var romSize: Int = 0
    var sizeInKB: Int = 0
    var cnt: Int = 0
    var buf: Int = 0
    var mirror: Boolean = false
    var oneScreen: Boolean = false
    var switchArea: Boolean = false
    var switchSize: Boolean = false
    var vromSwitchSize: Boolean = false
    var swapBase: Int = 0
    var vromPage: IntArray = IntArray(2)
    var romPage: IntArray = IntArray(2)
}
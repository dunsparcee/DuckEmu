package io.duckemu.gbc.domain.nes.core

import io.duckemu.nes.core.Nes


class Mbc(private val nes: Nes) {
    fun reset() {
        ram.fill(0x00.toByte())

        rom = nes.rom.rom
        vrom = nes.rom.chr
        sram = nes.rom.sram
        vram = nes.rom.vram

        for (i in 0..3) mapRom(i, i)
        if (vrom != null) for (i in 0..7) mapVrom(i, i)
        else for (i in 0..7) mapVram(i, i)

        sramEnabled = true
    }

    fun mapRom(page: Int, `val`: Int) {
        romPage[page] = (`val` % (nes.rom.romSize() * 2)) * 0x2000
    }

    fun mapVrom(page: Int, `val`: Int) {
        if (vrom != null) {
            chrPage[page] = (`val` % (nes.rom.chrSize() * 8)) * 0x0400
            isVram = false
        }
    }

    fun mapVram(page: Int, `val`: Int) {
        chrPage[page] = (`val` % 8) * 0x400
        isVram = true
    }

    fun read(adr: Short): Byte {
        when ((adr.toInt() and 0xffff) shr 11) {
            0x00, 0x01, 0x02, 0x03 -> return ram[adr.toInt() and 0x7ff]
            0x04, 0x05, 0x06, 0x07 -> return nes.regs.read(adr)
            0x08, 0x09, 0x0A, 0x0B -> {
                if (adr < 0x4020) return nes.regs.read(adr)
                return 0 // TODO : Expanision ROM
            }

            0x0C, 0x0D, 0x0E, 0x0F -> return if (sramEnabled)
                sram!![adr.toInt() and 0x1fff]
            else 0x00

            0x10, 0x11, 0x12, 0x13 -> return rom!![romPage[0] + (adr.toInt() and 0x1fff)]
            0x14, 0x15, 0x16, 0x17 -> return rom!![romPage[1] + (adr.toInt() and 0x1fff)]
            0x18, 0x19, 0x1A, 0x1B -> return rom!![romPage[2] + (adr.toInt() and 0x1fff)]
            0x1C, 0x1D, 0x1E, 0x1F -> return rom!![romPage[3] + (adr.toInt() and 0x1fff)]
        }
        return 0x00
    }

    fun write(adr: Short, dat: Byte) {
        when ((adr.toInt() and 0xffff) shr 11) {
            0x00, 0x01, 0x02, 0x03 -> ram[adr.toInt() and 0x7ff] = dat
            0x04, 0x05, 0x06, 0x07 -> nes.regs.write(adr, dat)
            0x08, 0x09, 0x0A, 0x0B -> if (adr < 0x4020) nes.regs.write(adr, dat)
            else if (nes.mapper != null) nes.mapper!!.write(adr, dat)

            0x0C, 0x0D, 0x0E, 0x0F -> if (sramEnabled)
                sram!![adr.toInt() and 0x1fff] = dat
            else if (nes.mapper != null) nes.mapper!!.write(adr, dat)

            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F -> if (nes.mapper != null) nes.mapper?.write(
                adr,
                dat
            )
        }
    }

    fun readChrRom(adr: Short): Byte {
        return if (isVram) vram!![chrPage[(adr.toInt() shr 10) and 7] + (adr.toInt() and 0x03ff)]
        else vrom!![chrPage[(adr.toInt() shr 10) and 7] + (adr.toInt() and 0x03ff)]
    }

    fun writeChrRom(adr: Short, dat: Byte) {
        if (isVram) vram!![chrPage[(adr.toInt() shr 10) and 7] + (adr.toInt() and 0x03ff)] = dat
        else vrom!![chrPage[(adr.toInt() shr 10) and 7] + (adr.toInt() and 0x03ff)] = dat
    }

    private var rom: ByteArray?
    private var vrom: ByteArray? = null
    var sram: ByteArray? = ByteArray(16)
    private var vram: ByteArray? = ByteArray(16)
    private val ram = ByteArray(0x800)

    private val romPage = IntArray(4)
    private val chrPage = IntArray(8)

    private var isVram: Boolean
    private var sramEnabled = true

    init {
        rom = vrom
        isVram = false
    }
}
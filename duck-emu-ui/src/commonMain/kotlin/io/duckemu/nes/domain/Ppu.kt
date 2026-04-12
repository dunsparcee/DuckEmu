package io.duckemu.nes.domain

import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.ui.Renderer

class Ppu(var nes: Nes) {
    fun reset() {
        spriteRam.fill(0x00.toByte())
        nameTable[0]?.fill(0x00.toByte())
        nameTable[1]?.fill(0x00.toByte())
        nameTable[2]?.fill(0x00.toByte())
        nameTable[3]?.fill(0x00.toByte())
        palette.fill(0x00.toByte())

        if (nes.rom.isFourScreen) setMirroring(MirrorType.FOUR_SCREEN)
        else if (nes.rom.mirror() == Rom.MirrorType.HORIZONTAL) setMirroring(MirrorType.HOLIZONTAL)
        else if (nes.rom.mirror() == Rom.MirrorType.VERTICAL) setMirroring(MirrorType.VERTICAL)
        else setMirroring(MirrorType.FOUR_SCREEN)
    }

    fun setMirroring(mt: MirrorType) {
        when (mt) {
            MirrorType.HOLIZONTAL -> setMirroring(0, 0, 1, 1)
            MirrorType.VERTICAL -> setMirroring(0, 1, 0, 1)
            MirrorType.FOUR_SCREEN -> setMirroring(0, 1, 2, 3)
            MirrorType.SINGLE_SCREEN -> setMirroring(0, 0, 0, 0)
        }
    }

    fun setMirroring(m0: Int, m1: Int, m2: Int, m3: Int) {
        namePage[0] = nameTable[m0]
        namePage[1] = nameTable[m1]
        namePage[2] = nameTable[m2]
        namePage[3] = nameTable[m3]
    }

    fun render(line: Int, scri: Renderer.ScreenInfo) {
        buf.fill(palette[0])

        if (nes.regs.bgVisible) renderBG(line, buf)
        if (nes.regs.spriteVisible) renderSPR(line, buf)

        if (scri.bpp == 24 || scri.bpp == 32) { // full color
            val inc = scri.bpp / 8
            var dest = scri.pitch * line
            for (i in 0..255) {
                val c = nesPalette24[buf[i + 8].toInt() and 0x3f]
                scri.buf[dest + 0] = (c and 0xff).toByte()
                scri.buf[dest + 1] = ((c shr 8) and 0xff).toByte()
                scri.buf[dest + 2] = ((c shr 16) and 0xff).toByte()
                dest += inc
            }
        } else if (scri.bpp == 16) { // high color
            var dest = scri.pitch * line
            for (i in 0..255) {
                val c = nesPalette16[buf[i + 8].toInt() and 0x3f]
                scri.buf[dest + 0] = (c.toInt() and 0xff).toByte()
                scri.buf[dest + 1] = ((c.toInt() shr 8) and 0xff).toByte()
                dest += 2
            }
        } else {
            // not supported
        }
    }

    enum class MirrorType {
        HOLIZONTAL, VERTICAL, FOUR_SCREEN, SINGLE_SCREEN,
    }

    fun serialize() {
        // TODO
    }

    // -----
    private fun initPalette() {
        val palDat = arrayOf<IntArray?>(
            intArrayOf(0x75, 0x75, 0x75),
            intArrayOf(0x27, 0x1B, 0x8F), intArrayOf(0x00, 0x00, 0xAB),
            intArrayOf(0x47, 0x00, 0x9F), intArrayOf(0x8F, 0x00, 0x77),
            intArrayOf(0xAB, 0x00, 0x13), intArrayOf(0xA7, 0x00, 0x00),
            intArrayOf(0x7F, 0x0B, 0x00), intArrayOf(0x43, 0x2F, 0x00),
            intArrayOf(0x00, 0x47, 0x00), intArrayOf(0x00, 0x51, 0x00),
            intArrayOf(0x00, 0x3F, 0x17), intArrayOf(0x1B, 0x3F, 0x5F),
            intArrayOf(0x00, 0x00, 0x00), intArrayOf(0x05, 0x05, 0x05),
            intArrayOf(0x05, 0x05, 0x05),

            intArrayOf(0xBC, 0xBC, 0xBC), intArrayOf(0x00, 0x73, 0xEF),
            intArrayOf(0x23, 0x3B, 0xEF), intArrayOf(0x83, 0x00, 0xF3),
            intArrayOf(0xBF, 0x00, 0xBF), intArrayOf(0xE7, 0x00, 0x5B),
            intArrayOf(0xDB, 0x2B, 0x00), intArrayOf(0xCB, 0x4F, 0x0F),
            intArrayOf(0x8B, 0x73, 0x00), intArrayOf(0x00, 0x97, 0x00),
            intArrayOf(0x00, 0xAB, 0x00), intArrayOf(0x00, 0x93, 0x3B),
            intArrayOf(0x00, 0x83, 0x8B), intArrayOf(0x11, 0x11, 0x11),
            intArrayOf(0x09, 0x09, 0x09), intArrayOf(0x09, 0x09, 0x09),

            intArrayOf(0xFF, 0xFF, 0xFF), intArrayOf(0x3F, 0xBF, 0xFF),
            intArrayOf(0x5F, 0x97, 0xFF), intArrayOf(0xA7, 0x8B, 0xFD),
            intArrayOf(0xF7, 0x7B, 0xFF), intArrayOf(0xFF, 0x77, 0xB7),
            intArrayOf(0xFF, 0x77, 0x63), intArrayOf(0xFF, 0x9B, 0x3B),
            intArrayOf(0xF3, 0xBF, 0x3F), intArrayOf(0x83, 0xD3, 0x13),
            intArrayOf(0x4F, 0xDF, 0x4B), intArrayOf(0x58, 0xF8, 0x98),
            intArrayOf(0x00, 0xEB, 0xDB), intArrayOf(0x66, 0x66, 0x66),
            intArrayOf(0x0D, 0x0D, 0x0D), intArrayOf(0x0D, 0x0D, 0x0D),

            intArrayOf(0xFF, 0xFF, 0xFF), intArrayOf(0xAB, 0xE7, 0xFF),
            intArrayOf(0xC7, 0xD7, 0xFF), intArrayOf(0xD7, 0xCB, 0xFF),
            intArrayOf(0xFF, 0xC7, 0xFF), intArrayOf(0xFF, 0xC7, 0xDB),
            intArrayOf(0xFF, 0xBF, 0xB3), intArrayOf(0xFF, 0xDB, 0xAB),
            intArrayOf(0xFF, 0xE7, 0xA3), intArrayOf(0xE3, 0xFF, 0xA3),
            intArrayOf(0xAB, 0xF3, 0xBF), intArrayOf(0xB3, 0xFF, 0xCF),
            intArrayOf(0x9F, 0xFF, 0xF3), intArrayOf(0xDD, 0xDD, 0xDD),
            intArrayOf(0x11, 0x11, 0x11), intArrayOf(0x11, 0x11, 0x11)
        )

        for (i in 0..0x3f) nesPalette24[i] = (palDat[i]!![0] or (palDat[i]!![1] shl 8)
                or (palDat[i]!![2] shl 16))
    }

    private fun readNameTable(adr: Short): Byte {
        return namePage[((adr).toInt() shr 10) and 3]!![(adr).toInt() and 0x3ff]
    }

    private fun readPatTable(adr: Short): Byte {
        return nes.mbc.readChrRom(adr)
    }

    private fun renderBG(line: Int, buf: ByteArray) {
        val r = nes.regs
        val x_ofs = r.ppuAdrX and 7
        val y_ofs = (r.ppuAdrV shr 12) and 7
        var name_adr = (r.ppuAdrV and 0xfff).toShort()
        val pat_adr = (if (r.bgPatAdr) 0x1000 else 0x0000).toShort()

        var ix = -x_ofs
        var i = 0
        while (i < 33) {
            val tile = readNameTable((0x2000 + name_adr).toShort())

            var l = readPatTable((pat_adr + (tile.toInt() and 0xff) * 16 + y_ofs).toShort())
            var u = readPatTable((pat_adr + (tile.toInt() and 0xff) * 16 + y_ofs + 8).toShort())

            val tx = name_adr.toInt() and 0x1f
            val ty = (name_adr.toInt() shr 5) and 0x1f
            val attr_adr = (((name_adr.toInt() and 0xC00) + 0x3C0
                    + ((ty and 3.inv()) shl 1) + (tx shr 2))).toShort()
            val aofs = (if ((ty and 2) == 0) 0 else 4) + (if ((tx and 2) == 0) 0 else 2)
            val attr = ((readNameTable((0x2000 + attr_adr).toShort()).toInt() shr aofs) and 3) shl 2

            for (j in 7 downTo 0) {
                val t = ((l.toInt() and 1) or (u.toInt() shl 1)) and 3
                if (t != 0) buf[8 + ix + j] = (0x40 or palette[t or attr].toInt()).toByte()
                l = (l.toInt() shr 1).toByte()
                u = (u.toInt() shr 1).toByte()
            }

            if ((name_adr.toInt() and 0x1f) == 0x1f) name_adr = ((name_adr.toInt() and 0x1f.inv()) xor 0x400).toShort()
            else name_adr++
            i++
            ix += 8
        }
    }

    private fun renderSPR(line: Int, buf: ByteArray) {
        val spr_height = if (nes.regs.spriteSize) 16 else 8
        val pat_adr = if (nes.regs.spritePatAdr) 0x1000 else 0x0000

        for (i in 0..63) {
            val spr_y = (this.spriteRam[i * 4 + 0].toInt() and 0xff) + 1
            val attr = this.spriteRam[i * 4 + 2].toInt() and 0xff

            if (!(line >= spr_y && line < spr_y + spr_height)) continue

            val is_bg = ((attr shr 5) and 1) != 0
            var y_ofs = line - spr_y
            val tile_index = this.spriteRam[i * 4 + 1].toInt() and 0xff
            val spr_x = this.spriteRam[i * 4 + 3].toInt() and 0xff
            val upper = (attr and 3) shl 2

            val h_flip = (attr and 0x40) == 0
            val sx = if (h_flip) 7 else 0
            val ex = if (h_flip) -1 else 8
            val ix = if (h_flip) -1 else 1

            if ((attr and 0x80) != 0) y_ofs = spr_height - 1 - y_ofs

            val tile_adr: Short
            if (spr_height == 16) tile_adr =
                ((tile_index and 1.inv()) * 16 + ((tile_index and 1) * 0x1000) + (if (y_ofs >= 8) 16 else 0) + (y_ofs and 7)).toShort()
            else tile_adr = (pat_adr + tile_index * 16 + y_ofs).toShort()

            var l = readPatTable(tile_adr)
            var u = readPatTable((tile_adr + 8).toShort())

            var x = sx
            while (x != ex) {
                val lower = (l.toInt() and 1) or ((u.toInt() and 1) shl 1)
                if (lower != 0 && (buf[8 + spr_x + x].toInt() and 0x80) == 0) {
                    if (!is_bg || (buf[8 + spr_x + x].toInt() and 0x40) == 0) buf[8 + spr_x + x] =
                        palette[0x10 or upper or lower]
                    buf[8 + spr_x + x] = (buf[8 + spr_x + x].toInt() or 0x80).toByte()
                }
                l = (l.toInt() shr 1).toByte()
                u = (u.toInt() shr 1).toByte()
                x += ix
            }
        }
    }

    fun spriteCheck(line: Int) {
        if (nes.regs.spriteVisible) {
            val spr_y = (this.spriteRam[0].toInt() and 0xff) + 1
            val spr_height = if (nes.regs.spriteSize) 16 else 8
            val pat_adr = if (nes.regs.spritePatAdr) 0x1000 else 0x0000

            if (line >= spr_y && line < spr_y + spr_height) {
                var y_ofs = line - spr_y
                val tile_index = this.spriteRam[1].toInt() and 0xff
                if ((this.spriteRam[2].toInt() and 0x80) != 0) y_ofs = spr_height - 1 - y_ofs
                val tile_adr: Short
                if (spr_height == 16) tile_adr = (((tile_index and 1.inv()) * 16 + ((tile_index and 1) * 0x1000)
                        + (if (y_ofs >= 8) 16 else 0) + (y_ofs and 7))).toShort()
                else tile_adr = (pat_adr + tile_index * 16 + y_ofs).toShort()
                val l = readPatTable(tile_adr)
                val u = readPatTable((tile_adr + 8).toShort())
                if (l.toInt() != 0 || u.toInt() != 0) nes.regs.setSprite0Occur(true)
            }
        }
    }

    val spriteRam: ByteArray = ByteArray(0x100)
    val nameTable: Array<ByteArray?> = Array<ByteArray?>(4) { ByteArray(0x400) }
    val namePage: Array<ByteArray?> = arrayOfNulls<ByteArray>(4)
    val palette: ByteArray = ByteArray(0x20)

    private val nesPalette24 = IntArray(0x40)
    private val nesPalette16 = ShortArray(0x40)

    private val buf = ByteArray(256 + 16)

    init {
        initPalette()
    }
}
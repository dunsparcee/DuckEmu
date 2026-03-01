package io.duckemu.gbc.domain.nes.core

import io.duckemu.nes.core.Nes

class Regs(private val nes: Nes) {
    fun reset() {
        nmiEnable = false
        ppuMaster = false // unused
        spriteSize = false
        spritePatAdr = false
        bgPatAdr = spritePatAdr
        ppuAdrIncr = false
        nameTblAdr = 0

        bgColor = 0
        bgVisible = false
        spriteVisible = bgVisible
        bgClip = false
        spriteClip = bgClip
        colorDisplay = false

        isVBlank = false
        spriteOver = false
        sprite0Occur = spriteOver
        vramWriteFlag = false

        sprramAdr = 0
        ppuAdrX = 0
        ppuAdrV = ppuAdrX
        ppuAdrT = ppuAdrV
        ppuAdrToggle = false
        ppuReadBuf = 0

        joypadStrobe = false
        joypadReadPos[1] = 0
        joypadReadPos[0] = joypadReadPos[1]

        joypadSign[0] = 0x1
        joypadSign[1] = 0x2

        frameIrq = 0xFF
    }

    fun setVBlank(b: Boolean, nmi: Boolean) {
        isVBlank = b
        if (nmi && (!isVBlank || nmiEnable)) nes.cpu.setNmi(isVBlank)
        if (b) sprite0Occur = false
    }

    fun startFrame() {
        if (bgVisible || spriteVisible) ppuAdrV = ppuAdrT
    }

    fun startScanline() {
        if (bgVisible || spriteVisible) ppuAdrV =
            ((ppuAdrV and 0xfbe0) or (ppuAdrT and 0x041f))
    }

    fun endScanline() {
        if (bgVisible || spriteVisible) {
            if (((ppuAdrV shr 12) and 7) == 7) {
                ppuAdrV = ppuAdrV and 0x7000.inv()
                if (((ppuAdrV shr 5) and 0x1f) == 29) ppuAdrV =
                    ((ppuAdrV and 0x03e0.inv()) xor 0x800)
                else if (((ppuAdrV shr 5) and 0x1f) == 31) ppuAdrV = ppuAdrV and 0x03e0.inv()
                else ppuAdrV = (ppuAdrV + 0x20)
            } else ppuAdrV = (ppuAdrV + 0x1000)
        }
    }

    fun drawEnabled(): Boolean {
        return spriteVisible || bgVisible
    }

    fun setInput(dat: IntArray) {
        for (i in 0..15) padDat[i / 8]!![i % 8] = if (dat[i] != 0) true else false
    }

    fun read(adr: Short): Byte {
        // System.out.printf("[%04X] ->\n", adr & 0xffff);
        if ((adr.toInt() and 0xffff) >= 0x4000) {
            when (adr.toInt() and 0xffff) {
                0x4016, 0x4017 -> {
                    val padNum = adr - 0x4016
                    val readPos = joypadReadPos[padNum]
                    val ret: Byte
                    if (readPos < 8)  // �p�b�h�f�[�^
                        ret = (if (padDat[padNum]!![readPos]) 1 else 0).toByte()
                    else if (readPos < 16)  // Ignored
                        ret = 0
                    else if (readPos < 20)  // Signature
                        ret = ((joypadSign[padNum] shr (readPos - 16)) and 1).toByte()
                    else ret = 0
                    joypadReadPos[padNum]++
                    if (joypadReadPos[padNum] == 24) joypadReadPos[padNum] = 0
                    return ret
                }

                0x4015 -> return (nes.apu.read(0x4015.toShort()).toInt() or (if ((frameIrq and 0xC0) == 0)
                    0x40
                else
                    0)).toByte()

                else -> return nes.apu.read(adr)
            }
        }

        when (adr.toInt() and 7) {
            0, 1, 3, 4, 5, 6 -> {
                return 0
            }

            2 -> {
                // PPU Status Register (R)
                val ret = ((_set(isVBlank, 7) or _set(sprite0Occur, 6) or
                        _set(spriteOver, 5) or _set(vramWriteFlag, 4)
                        )).toByte()
                setVBlank(false, true)
                ppuAdrToggle = false
                return ret
            }

            7 -> {
                // VRAM I/O Register (RW)
                val ret = ppuReadBuf
                ppuReadBuf = read2007()
                return ret
            }
        }

        return 0
    }

    fun write(adr: Short, dat: Byte) {
        // System.out.printf("[%04X] <- %02X\n", adr & 0xffff, dat & 0xff);
        if ((adr.toInt() and 0xffff) >= 0x4000) {
            when (adr.toInt() and 0xffff) {
                0x4014 -> {
                    val sprram = nes.ppu.spriteRam
                    var i = 0
                    while (i < 0x100) {
                        sprram[i] = nes.mbc.read(((dat.toInt() shl 8) or i).toShort())
                        i++
                    }
                }

                0x4016 -> {
                    val newval = (dat.toInt() and 1) != 0
                    if (joypadStrobe && !newval)  // ��������G�b�W�Ń��Z�b�g
                    {
                        joypadReadPos[1] = 0
                        joypadReadPos[0] = joypadReadPos[1]
                    }
                    joypadStrobe = newval
                }

                0x4017 -> frameIrq = dat.toInt()
                else -> nes.apu.write(adr, dat)
            }
            return
        }

        when (adr.toInt() and 7) {
            0 -> {
                nmiEnable = _bit(dat.toInt(), 7)
                ppuMaster = _bit(dat.toInt(), 6)
                spriteSize = _bit(dat.toInt(), 5)
                bgPatAdr = _bit(dat.toInt(), 4)
                spritePatAdr = _bit(dat.toInt(), 3)
                ppuAdrIncr = _bit(dat.toInt(), 2)
                // name_tbl_adr =dat&3;
                ppuAdrT = ((ppuAdrT and 0xf3ff) or ((dat.toInt() and 3) shl 10))
            }

            1 -> {
                bgColor = dat.toInt() shr 5
                spriteVisible = _bit(dat.toInt(), 4)
                bgVisible = _bit(dat.toInt(), 3)
                spriteClip = _bit(dat.toInt(), 2)
                bgClip = _bit(dat.toInt(), 1)
                colorDisplay = _bit(dat.toInt(), 0)
            }

            2 ->            // what should I do...?
                println("*** write to $2002")

            3 -> sprramAdr = dat
            4 -> nes.ppu.spriteRam[(sprramAdr++).toInt() and 0xff] = dat
            5 -> {
                ppuAdrToggle = !ppuAdrToggle
                if (ppuAdrToggle) {
                    ppuAdrT = ((ppuAdrT and 0xffe0) or ((dat.toInt() and 0xff) shr 3))
                    ppuAdrX = (dat.toInt() and 7)
                } else {
                    ppuAdrT = ((ppuAdrT and 0xfC1f) or (((dat.toInt() and 0xff) shr 3) shl 5))
                    ppuAdrT = ((ppuAdrT and 0x8fff) or ((dat.toInt() and 7) shl 12))
                }
            }

            6 -> {
                ppuAdrToggle = !ppuAdrToggle
                if (ppuAdrToggle) ppuAdrT = ((ppuAdrT and 0x00ff) or ((dat.toInt() and 0x3f) shl 8))
                else {
                    ppuAdrT = ((ppuAdrT and 0xff00) or (dat.toInt() and 0xff))
                    ppuAdrV = ppuAdrT
                }
            }

            7 -> write2007(dat)
        }
    }

    private fun read2007(): Byte {
        var adr = (ppuAdrV and 0x3fff).toShort()
        ppuAdrV = (ppuAdrV + if (ppuAdrIncr) 32 else 1)
        val adr0X3F00 = nes.ppu.namePage[(adr.toInt() shr 10) and 3]?.get(adr.toInt() and 0x3ff)

        if (adr < 0x2000) return nes.mbc.readChrRom(adr)
        else if (adr < 0x3f00 && adr0X3F00 != null) {
            return adr0X3F00
        }
        else {
            if ((adr.toInt() and 3) == 0) adr = (adr.toInt() and 0x10.inv()).toShort()
            return nes.ppu.palette[adr.toInt() and 0x1f]
        }
    }

    private fun write2007(dat: Byte) {
        var adr = (ppuAdrV and 0x3fff).toShort()
        ppuAdrV = (ppuAdrV + if (ppuAdrIncr) 32 else 1)
        if (adr < 0x2000)  // CHR-ROM
            nes.mbc.writeChrRom(adr, dat)
        else if (adr < 0x3f00)  // name table
            nes.ppu.namePage[(adr.toInt() shr 10) and 3]?.set(adr.toInt() and 0x3ff, dat)
        else { // palette
            if ((adr.toInt() and 3) == 0) adr = (adr.toInt() and 0x10.inv()).toShort() // mirroring

            nes.ppu.palette[adr.toInt() and 0x1f] = (dat.toInt() and 0x3f).toByte()
        }
    }

    fun setSprite0Occur(b: Boolean) {
        this.sprite0Occur = b
    }

    private var nmiEnable = false
    private var ppuMaster = false
    var spriteSize: Boolean = false
        private set
    var bgPatAdr: Boolean = false
        private set
    var spritePatAdr: Boolean = false
        private set
    private var ppuAdrIncr = false
    private var nameTblAdr = 0

    private var bgColor = 0
    var spriteVisible: Boolean = false
        private set
    var bgVisible: Boolean = false
        private set
    private var spriteClip = false
    private var bgClip = false
    private var colorDisplay = false

    var isVBlank: Boolean = false
        private set
    private var sprite0Occur = false
    private var spriteOver = false
    private var vramWriteFlag = false

    private var sprramAdr: Byte = 0
    private var ppuAdrT: Int = 0
    var ppuAdrV: Int = 0
        private set
    var ppuAdrX: Int = 0
        private set
    private var ppuAdrToggle = false
    private var ppuReadBuf: Byte = 0

    private var joypadStrobe = false
    private val joypadReadPos = IntArray(2)
    private val joypadSign = IntArray(2)
    var frameIrq: Int = 0
        private set

    private val padDat = Array<BooleanArray?>(2) { BooleanArray(8) }

    companion object {
        private fun _bit(x: Int, n: Int): Boolean {
            return ((x shr n) and 1) != 0
        }

        private fun _set(b: Boolean, n: Int): Int {
            return ((if (b) 1 else 0) shl n)
        }
    }
}
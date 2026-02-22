package io.duckemu.gbc.domain.nes.core

import io.duckemu.nes.core.Nes

class Cpu(private val nes: Nes) {
    fun reset() {
        mbc = nes.mbc

        regPC = read16(0xFFFC.toShort())
        regA = 0x00
        regX = 0x00
        regY = 0x00
        regS = 0xFF.toByte()

        oprPC = 0

        cFlag = 0
        zFlag = cFlag
        vFlag = zFlag
        nFlag = vFlag // reset for Reproducibility
        bFlag = 1
        dFlag = 0
        iFlag = 1

        rest = 0
        mclock = 0
        resetLine = false
        irqLine = resetLine
        nmiLine = irqLine
    }

    private fun imm(): Short {
        return regPC++
    }

    // TODO: penalty of carry
    private fun abs(): Short {
        regPC = (regPC + 2).toShort()
        return read16(oprPC)
    }

    // private short abxi(){ regPC+=2; return
    // read16((short)(read16(oprPC)+(regX&0xff))); }
    private fun abx(): Short {
        regPC = (regPC + 2).toShort()
        return (read16(oprPC) + (regX.toInt() and 0xff)).toShort()
    }

    private fun aby(): Short {
        regPC = (regPC + 2).toShort()
        return (read16(oprPC) + (regY.toInt() and 0xff)).toShort()
    }

    private fun absi(): Short {
        regPC = (regPC + 2).toShort()
        return read16(read16(oprPC))
    }

    private fun zp(): Short {
        return (read8(regPC++).toInt() and 0xff).toShort()
    }

    private fun zpxi(): Short {
        return read16(((read8(regPC++) + (regX.toInt() and 0xff)) and 0xff).toShort())
    }

    private fun zpx(): Short {
        return ((read8(regPC++) + (regX.toInt() and 0xff)) and 0xff).toShort()
    }

    private fun zpy(): Short {
        return ((read8(regPC++) + (regY.toInt() and 0xff)) and 0xff).toShort()
    }

    // private short zpi(){ return read16((short)(read8(regPC++)&0xff)); }
    private fun zpiy(): Short {
        return (read16((read8(regPC++).toInt() and 0xff).toShort()) + (regY.toInt() and 0xff)).toShort()
    }

    private fun push8(dat: Byte) {
        write8((0x100 or ((regS--).toInt() and 0xff)).toShort(), dat)
    }

    private fun pop8(): Byte {
        return read8((0x100 or ((++regS).toInt() and 0xff)).toShort())
    }

    private fun push16(dat: Short) {
        write16((0x100 or ((regS - 1) and 0xff)).toShort(), dat)
        regS = (regS - 2).toByte()
    }

    private fun pop16(): Short {
        regS = (regS + 2).toByte()
        return read16((0x100 or ((regS - 1) and 0xff)).toShort())
    }

    private fun bindFlags(): Byte {
        return (((nFlag.toInt() shl 7) or (vFlag.toInt() shl 6) or 0x20 or (bFlag.toInt() shl 4)
                or (dFlag.toInt() shl 3) or (iFlag.toInt() shl 2) or (zFlag.toInt() shl 1) or cFlag.toInt())).toByte()
    }

    private fun unbindFlags(dat: Byte) {
        nFlag = ((dat.toInt() shr 7) and 1).toByte()
        vFlag = ((dat.toInt() shr 6) and 1).toByte()
        bFlag = ((dat.toInt() shr 4) and 1).toByte()
        dFlag = ((dat.toInt() shr 3) and 1).toByte()
        iFlag = ((dat.toInt() shr 2) and 1).toByte()
        zFlag = ((dat.toInt() shr 1) and 1).toByte()
        cFlag = (dat.toInt() and 1).toByte()
    }

    // ops
    // TODO : decimal support
    private fun adc(cycle: Int, adr: Short) {
        val s = read8(adr)
        val t = (regA.toInt() and 0xff) + (s.toInt() and 0xff) + (cFlag.toInt() and 0xff)
        cFlag = (t shr 8).toByte()
        zFlag = (if ((t and 0xff) == 0) 1 else 0).toByte()
        nFlag = ((t shr 7) and 1).toByte()
        vFlag = (if (((regA.toInt() xor s.toInt()) and 0x80) == 0 && ((regA.toInt() xor t) and 0x80) != 0)
            1
        else
            0).toByte()
        regA = (t and 0xff).toByte()
        rest -= cycle
    }

    // TODO : decimal support
    private fun sbc(cycle: Int, adr: Short) {
        val s = read8(adr)
        val t = (regA.toInt() and 0xff) - (s.toInt() and 0xff) - (if (cFlag.toInt() != 0) 0 else 1)
        cFlag = (if ((t and 0xff00) == 0) 1 else 0).toByte()
        zFlag = (if ((t and 0xff) == 0) 1 else 0).toByte()
        nFlag = ((t shr 7) and 1).toByte()
        vFlag = (if ((((regA.toInt() xor s.toInt()) and 0x80) != 0)
            && (((regA.toInt() xor t) and 0x80) != 0)
        ) 1 else 0).toByte()
        regA = t.toByte()
        rest -= cycle
    }

    private fun cmp(cycle: Int, reg: Byte, adr: Short) {
        val t = ((reg.toInt() and 0xff) - (read8(adr).toInt() and 0xff)).toShort()
        cFlag = (if ((t.toInt() and 0xff00) == 0) 1 else 0).toByte()
        zFlag = (if ((t.toInt() and 0xff) == 0) 1 else 0).toByte()
        nFlag = ((t.toInt() shr 7) and 1).toByte()
        rest -= cycle
    }

    private fun and(cycle: Int, adr: Short) {
        regA = (regA.toInt() and read8(adr).toInt()).toByte()
        nFlag = ((regA.toInt() shr 7) and 1).toByte()
        zFlag = (if (regA.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun ora(cycle: Int, adr: Short) {
        regA = (regA.toInt() or read8(adr).toInt()).toByte()
        nFlag = ((regA.toInt() shr 7) and 1).toByte()
        zFlag = (if (regA.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun eor(cycle: Int, adr: Short) {
        regA = (regA.toInt() xor read8(adr).toInt()).toByte()
        nFlag = ((regA.toInt() shr 7) and 1).toByte()
        zFlag = (if (regA.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun bit(cycle: Int, adr: Short) {
        val t = read8(adr)
        nFlag = ((t.toInt() shr 7) and 1).toByte()
        vFlag = ((t.toInt() shr 6) and 1).toByte()
        zFlag = (if ((regA.toInt() and t.toInt()) == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun loadA(cycle: Int, adr: Short) {
        regA = read8(adr)
        nFlag = ((regA.toInt() shr 7) and 1).toByte()
        zFlag = (if (regA.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun loadX(cycle: Int, adr: Short) {
        regX = read8(adr)
        nFlag = ((regX.toInt() shr 7) and 1).toByte()
        zFlag = (if (regX.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun loadY(cycle: Int, adr: Short) {
        regY = read8(adr)
        nFlag = ((regY.toInt() shr 7) and 1).toByte()
        zFlag = (if (regY.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun store(cycle: Int, reg: Byte, adr: Short) {
        write8(adr, reg)
        rest -= cycle
    }

    private fun movA(cycle: Int, src: Byte) {
        regA = src
        nFlag = ((src.toInt() shr 7) and 1).toByte()
        zFlag = (if (src.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun movX(cycle: Int, src: Byte) {
        regX = src
        nFlag = ((src.toInt() shr 7) and 1).toByte()
        zFlag = (if (src.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun movY(cycle: Int, src: Byte) {
        regY = src
        nFlag = ((src.toInt() shr 7) and 1).toByte()
        zFlag = (if (src.toInt() == 0) 1 else 0).toByte()
        rest -= cycle
    }

    private fun asli(arg: Byte): Byte {
        var arg = arg
        cFlag = ((arg.toInt() shr 7) and 1).toByte()
        arg = (arg.toInt() shl 1).toByte()
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun lsri(arg: Byte): Byte {
        var arg = arg
        cFlag = (arg.toInt() and 1).toByte()
        arg = ((arg.toInt() and 0xff) shr 1).toByte()
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun roli(arg: Byte): Byte {
        var arg = arg
        val u = arg
        arg = ((arg.toInt() shl 1) or cFlag.toInt()).toByte()
        cFlag = ((u.toInt() shr 7) and 1).toByte()
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun rori(arg: Byte): Byte {
        var arg = arg
        val u = arg
        arg = (((arg.toInt() and 0xff) shr 1) or (cFlag.toInt() shl 7)).toByte()
        cFlag = (u.toInt() and 1).toByte()
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun inci(arg: Byte): Byte {
        var arg = arg
        arg++
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun deci(arg: Byte): Byte {
        var arg = arg
        arg--
        nFlag = ((arg.toInt() shr 7) and 1).toByte()
        zFlag = (if (arg.toInt() == 0) 1 else 0).toByte()
        return arg
    }

    private fun asla(cycle: Int) {
        regA = asli(regA)
        rest -= cycle
    }

    private fun asl(cycle: Int, adr: Short) {
        write8(adr, asli(read8(adr)))
        rest -= cycle
    }

    private fun lsra(cycle: Int) {
        regA = lsri(regA)
        rest -= cycle
    }

    private fun lsr(cycle: Int, adr: Short) {
        write8(adr, lsri(read8(adr)))
        rest -= cycle
    }

    private fun rola(cycle: Int) {
        regA = roli(regA)
        rest -= cycle
    }

    private fun rol(cycle: Int, adr: Short) {
        write8(adr, roli(read8(adr)))
        rest -= cycle
    }

    private fun rora(cycle: Int) {
        regA = rori(regA)
        rest -= cycle
    }

    private fun ror(cycle: Int, adr: Short) {
        write8(adr, rori(read8(adr)))
        rest -= cycle
    }

    private fun incX(cycle: Int) {
        regX = inci(regX)
        rest -= cycle
    }

    private fun incY(cycle: Int) {
        regY = inci(regY)
        rest -= cycle
    }

    private fun inc(cycle: Int, adr: Short) {
        write8(adr, inci(read8(adr)))
        rest -= cycle
    }

    private fun decX(cycle: Int) {
        regX = deci(regX)
        rest -= cycle
    }

    private fun decY(cycle: Int) {
        regY = deci(regY)
        rest -= cycle
    }

    private fun dec(cycle: Int, adr: Short) {
        write8(adr, deci(read8(adr)))
        rest -= cycle
    }

    private fun bra(cycle: Int, cond: Boolean) {
        val rel = read8(imm())
        rest -= cycle
        if (cond) {
            rest -= if ((regPC.toInt() and 0xff00) == ((regPC + rel) and 0xff00)) 1 else 2
            regPC = (regPC + rel).toShort()
        }
    }

    fun exec(clk: Int) {
        rest += clk
        mclock += clk.toLong()

        do {
            if (iFlag.toInt() == 0) { // check IRQs
                if (resetLine) {
                    execIrq(IrqType.RESET)
                    resetLine = false
                } else if (irqLine) {
                    execIrq(IrqType.IRQ)
                    irqLine = false
                }
            }

            if (logging) log()

            val opc = read8(regPC++)
            oprPC = regPC

            when (opc.toInt() and 0xff) {
                0x69 -> adc(2, imm())
                0x65 -> adc(3, zp())
                0x75 -> adc(4, zpx())
                0x6D -> adc(4, abs())
                0x7D -> adc(4, abx())
                0x79 -> adc(4, aby())
                0x61 -> adc(6, zpxi())
                0x71 -> adc(5, zpiy())
                0xE9 -> sbc(2, imm())
                0xE5 -> sbc(3, zp())
                0xF5 -> sbc(4, zpx())
                0xED -> sbc(4, abs())
                0xFD -> sbc(4, abx())
                0xF9 -> sbc(4, aby())
                0xE1 -> sbc(6, zpxi())
                0xF1 -> sbc(5, zpiy())
                0xC9 -> cmp(2, regA, imm())
                0xC5 -> cmp(3, regA, zp())
                0xD5 -> cmp(4, regA, zpx())
                0xCD -> cmp(4, regA, abs())
                0xDD -> cmp(4, regA, abx())
                0xD9 -> cmp(4, regA, aby())
                0xC1 -> cmp(6, regA, zpxi())
                0xD1 -> cmp(5, regA, zpiy())
                0xE0 -> cmp(2, regX, imm())
                0xE4 -> cmp(2, regX, zp())
                0xEC -> cmp(3, regX, abs())
                0xC0 -> cmp(2, regY, imm())
                0xC4 -> cmp(2, regY, zp())
                0xCC -> cmp(3, regY, abs())
                0x29 -> and(2, imm())
                0x25 -> and(3, zp())
                0x35 -> and(4, zpx())
                0x2D -> and(4, abs())
                0x3D -> and(4, abx())
                0x39 -> and(4, aby())
                0x21 -> and(6, zpxi())
                0x31 -> and(5, zpiy())
                0x09 -> ora(2, imm())
                0x05 -> ora(3, zp())
                0x15 -> ora(4, zpx())
                0x0D -> ora(4, abs())
                0x1D -> ora(4, abx())
                0x19 -> ora(4, aby())
                0x01 -> ora(6, zpxi())
                0x11 -> ora(5, zpiy())
                0x49 -> eor(2, imm())
                0x45 -> eor(3, zp())
                0x55 -> eor(4, zpx())
                0x4D -> eor(4, abs())
                0x5D -> eor(4, abx())
                0x59 -> eor(4, aby())
                0x41 -> eor(6, zpxi())
                0x51 -> eor(5, zpiy())
                0x24 -> bit(3, zp())
                0x2C -> bit(4, abs())
                0xA9 -> loadA(2, imm())
                0xA5 -> loadA(3, zp())
                0xB5 -> loadA(4, zpx())
                0xAD -> loadA(4, abs())
                0xBD -> loadA(4, abx())
                0xB9 -> loadA(4, aby())
                0xA1 -> loadA(6, zpxi())
                0xB1 -> loadA(5, zpiy())
                0xA2 -> loadX(2, imm())
                0xA6 -> loadX(3, zp())
                0xB6 -> loadX(4, zpy())
                0xAE -> loadX(4, abs())
                0xBE -> loadX(4, aby())
                0xA0 -> loadY(2, imm())
                0xA4 -> loadY(3, zp())
                0xB4 -> loadY(4, zpx())
                0xAC -> loadY(4, abs())
                0xBC -> loadY(4, abx())
                0x85 -> store(3, regA, zp())
                0x95 -> store(4, regA, zpx())
                0x8D -> store(4, regA, abs())
                0x9D -> store(5, regA, abx())
                0x99 -> store(5, regA, aby())
                0x81 -> store(6, regA, zpxi())
                0x91 -> store(6, regA, zpiy())
                0x86 -> store(3, regX, zp())
                0x96 -> store(4, regX, zpy())
                0x8E -> store(4, regX, abs())
                0x84 -> store(3, regY, zp())
                0x94 -> store(4, regY, zpx())
                0x8C -> store(4, regY, abs())
                0xAA -> movX(2, regA)
                0xA8 -> movY(2, regA)
                0x8A -> movA(2, regX)
                0x98 -> movA(2, regY)
                0xBA -> movX(2, regS)
                0x9A -> {
                    regS = regX
                    rest -= 2
                }

                0x0A -> asla(2)
                0x06 -> asl(5, zp())
                0x16 -> asl(6, zpx())
                0x0E -> asl(6, abs())
                0x1E -> asl(7, abx())
                0x4A -> lsra(2)
                0x46 -> lsr(5, zp())
                0x56 -> lsr(6, zpx())
                0x4E -> lsr(6, abs())
                0x5E -> lsr(7, abx())
                0x2A -> rola(2)
                0x26 -> rol(5, zp())
                0x36 -> rol(6, zpx())
                0x2E -> rol(6, abs())
                0x3E -> rol(7, abx())
                0x6A -> rora(2)
                0x66 -> ror(5, zp())
                0x76 -> ror(6, zpx())
                0x6E -> ror(6, abs())
                0x7E -> ror(7, abx())
                0xE6 -> inc(5, zp())
                0xF6 -> inc(6, zpx())
                0xEE -> inc(6, abs())
                0xFE -> inc(7, abx())
                0xE8 -> incX(2)
                0xC8 -> incY(2)
                0xC6 -> dec(5, zp())
                0xD6 -> dec(6, zpx())
                0xCE -> dec(6, abs())
                0xDE -> dec(7, abx())
                0xCA -> decX(2)
                0x88 -> decY(2)
                0x90 -> bra(2, cFlag.toInt() == 0)
                0xB0 -> bra(2, cFlag.toInt() != 0)
                0xD0 -> bra(2, zFlag.toInt() == 0)
                0xF0 -> bra(2, zFlag.toInt() != 0)
                0x10 -> bra(2, nFlag.toInt() == 0)
                0x30 -> bra(2, nFlag.toInt() != 0)
                0x50 -> bra(2, vFlag.toInt() == 0)
                0x70 -> bra(2, vFlag.toInt() != 0)
                0x4C -> {
                    regPC = abs()
                    rest -= 3
                }

                0x6C -> {
                    regPC = absi()
                    rest -= 5
                }

                0x20 -> {
                    push16((regPC + 1).toShort())
                    regPC = abs()
                    rest -= 6
                }

                0x60 -> {
                    regPC = (pop16() + 1).toShort()
                    rest -= 6
                }

                0x40 -> {
                    unbindFlags(pop8())
                    regPC = pop16()
                    rest -= 6
                }

                0x38 -> {
                    cFlag = 1
                    rest -= 2
                }

                0xF8 -> {
                    dFlag = 1
                    rest -= 2
                }

                0x78 -> {
                    iFlag = 1
                    rest -= 2
                }

                0x18 -> {
                    cFlag = 0
                    rest -= 2
                }

                0xD8 -> {
                    dFlag = 0
                    rest -= 2
                }

                0x58 -> {
                    iFlag = 0
                    rest -= 2
                }

                0xB8 -> {
                    vFlag = 0
                    rest -= 2
                }

                0x48 -> {
                    push8(regA)
                    rest -= 3
                }

                0x08 -> {
                    push8(bindFlags())
                    rest -= 3
                }

                0x68 -> {
                    regA = pop8()
                    nFlag = ((regA.toInt() shr 7) and 1).toByte()
                    zFlag = (if (regA.toInt() == 0) 1 else 0).toByte()
                    rest -= 4
                }

                0x28 -> {
                    unbindFlags(pop8())
                    rest -= 4
                }

                0x00 -> {
                    bFlag = 1
                    regPC++
                    execIrq(IrqType.IRQ)
                }

                0xEA -> rest -= 2
                else -> println("No Op Found")
            }
        } while (rest > 0)
    }

    fun setLogging(b: Boolean) {
        logging = b
    }

    fun setNmi(b: Boolean) {
        // edge sensitive
        if (!nmiLine && b) execIrq(IrqType.NMI)
        nmiLine = b
    }

    fun setIrq(b: Boolean) {
        irqLine = b
    }

    fun setReset(b: Boolean) {
        resetLine = b
    }

    val masterClock: Long
        get() = mclock - rest

    val frequency: Double
        get() = 3579545.0 / 2

    private fun read8(adr: Short): Byte {
        return mbc!!.read(adr)
    }

    private fun read16(adr: Short): Short {
        return ((mbc!!.read(adr).toInt() and 0xff) or ((mbc!!.read((adr + 1).toShort())
            .toInt() and 0xff) shl 8)).toShort()
    }

    private fun write8(adr: Short, dat: Byte) {
        mbc!!.write(adr, dat)
    }

    private fun write16(adr: Short, dat: Short) {
        mbc!!.write(adr, dat.toByte())
        mbc!!.write((adr + 1).toShort(), (dat.toInt() shr 8).toByte())
    }

    private enum class IrqType {
        NMI, IRQ, RESET,
    }

    private fun execIrq(it: IrqType?) {
        val vect = (if (it == IrqType.RESET)
            0xFFFC
        else
            if (it == IrqType.NMI) 0xFFFA else if (it == IrqType.IRQ)
                0xFFFE
            else
                0xFFFE).toShort()

        push16(regPC)
        push8(bindFlags())
        iFlag = 1
        regPC = read16(vect)
        rest -= 7
    }

    private var cnt = 0

    private fun log() {
        val opc = read8(regPC)
        val opr = read16((regPC + 1).toShort())
        val s = disasm(regPC, opc, opr)
    }

    private fun disasm(pc: Short, opc: Byte, opr: Short): String? {
        val op: String? = mne[opc.toInt() and 0xff]
        when (adr[opc.toInt() and 0xff]) {
            0 -> return op
            1 -> return "$op #\$${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')}"
            2 -> return "$op A"
            3 -> return "$op $${(opr.toInt() and 0xffff).toString(16).uppercase().padStart(4, '0')}"
            4 -> return "$op $${(opr.toInt() and 0xffff).toString(16).uppercase().padStart(4, '0')},X"
            5 -> return "$op $${(opr.toInt() and 0xffff).toString(16).uppercase().padStart(4, '0')},Y"
            6 -> return "$op ($${(opr.toInt() and 0xffff).toString(16).uppercase().padStart(4, '0')},X)"
            7 -> return "$op ($${(opr.toInt() and 0xffff).toString(16).uppercase().padStart(4, '0')})"
            8 -> return "$op $${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')}"
            9 -> return "$op $${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')},X"
            10 -> return "$op $${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')},Y"
            11 -> return "$op ($${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')})"
            12 -> return "$op ($${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')},X)"
            13 -> return "$op ($${(opr.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')}),Y"
            14 -> return "$op $${
                ((pc + (opr.toInt() and 0xff).toByte() + 2) and 0xffff).toString(16).uppercase().padStart(4, '0')
            }"

            else -> return ""
        }
    }

    private var regA: Byte = 0
    private var regX: Byte = 0
    private var regY: Byte = 0
    private var regS: Byte = 0
    private var regPC: Short = 0
    private var cFlag: Byte = 0
    private var zFlag: Byte = 0
    private var iFlag: Byte = 0
    private var dFlag: Byte = 0
    private var bFlag: Byte = 0
    private var vFlag: Byte = 0
    private var nFlag: Byte = 0

    private var oprPC: Short = 0

    private var rest = 0
    private var mclock: Long = 0
    private var nmiLine = false
    private var irqLine = false
    private var resetLine = false

    private var logging = false

    private var mbc: Mbc? = null

    companion object {
        val mne: Array<String> = arrayOf<String>(
            "BRK", "ORA", "UNK", "UNK", "UNK", "ORA",
            "ASL", "UNK", "PHP", "ORA", "ASL", "UNK", "UNK", "ORA", "ASL",
            "UNK", "BPL", "ORA", "UNK", "UNK", "UNK", "ORA", "ASL", "UNK",
            "CLC", "ORA", "UNK", "UNK", "UNK", "ORA", "ASL", "UNK", "JSR",
            "AND", "UNK", "UNK", "BIT", "AND", "ROL", "UNK", "PLP", "AND",
            "ROL", "UNK", "BIT", "AND", "ROL", "UNK", "BMI", "AND", "UNK",
            "UNK", "UNK", "AND", "ROL", "UNK", "SEC", "AND", "UNK", "UNK",
            "UNK", "AND", "ROL", "UNK", "RTI", "EOR", "UNK", "UNK", "UNK",
            "EOR", "LSR", "UNK", "PHA", "EOR", "LSR", "UNK", "JMP", "EOR",
            "LSR", "UNK", "BVC", "EOR", "UNK", "UNK", "UNK", "EOR", "LSR",
            "UNK", "CLI", "EOR", "UNK", "UNK", "UNK", "EOR", "LSR", "UNK",
            "RTS", "ADC", "UNK", "UNK", "UNK", "ADC", "ROR", "UNK", "PLA",
            "ADC", "ROR", "UNK", "JMP", "ADC", "ROR", "UNK", "BVS", "ADC",
            "UNK", "UNK", "UNK", "ADC", "ROR", "UNK", "SEI", "ADC", "UNK",
            "UNK", "UNK", "ADC", "ROR", "UNK", "UNK", "STA", "UNK", "UNK",
            "STY", "STA", "STX", "UNK", "DEY", "UNK", "TXA", "UNK", "STY",
            "STA", "STX", "UNK", "BCC", "STA", "UNK", "UNK", "STY", "STA",
            "STX", "UNK", "TYA", "STA", "TXS", "UNK", "UNK", "STA", "UNK",
            "UNK", "LDY", "LDA", "LDX", "UNK", "LDY", "LDA", "LDX", "UNK",
            "TAY", "LDA", "TAX", "UNK", "LDY", "LDA", "LDX", "UNK", "BCS",
            "LDA", "UNK", "UNK", "LDY", "LDA", "LDX", "UNK", "CLV", "LDA",
            "TSX", "UNK", "LDY", "LDA", "LDX", "UNK", "CPY", "CMP", "UNK",
            "UNK", "CPY", "CMP", "DEC", "UNK", "INY", "CMP", "DEX", "UNK",
            "CPY", "CMP", "DEC", "UNK", "BNE", "CMP", "UNK", "UNK", "UNK",
            "CMP", "DEC", "UNK", "CLD", "CMP", "UNK", "UNK", "UNK", "CMP",
            "DEC", "UNK", "CPX", "SBC", "UNK", "UNK", "CPX", "SBC", "INC",
            "UNK", "INX", "SBC", "NOP", "UNK", "CPX", "SBC", "INC", "UNK",
            "BEQ", "SBC", "UNK", "UNK", "UNK", "SBC", "INC", "UNK", "SED",
            "SBC", "UNK", "UNK", "UNK", "SBC", "INC", "UNK",
        )

        val adr: IntArray = intArrayOf(
            0, 12, 0, 0, 0, 8, 8, 0, 0, 1, 2, 0, 0, 3, 3, 0,
            14, 13, 0, 0, 0, 9, 9, 0, 0, 5, 0, 0, 0, 4, 4, 0, 3, 12, 0, 0, 8,
            8, 8, 0, 0, 1, 2, 0, 3, 3, 3, 0, 14, 13, 0, 0, 0, 9, 9, 0, 0, 5, 0,
            0, 0, 4, 4, 0, 0, 12, 0, 0, 0, 8, 8, 0, 0, 1, 2, 0, 3, 3, 3, 0, 14,
            13, 0, 0, 0, 9, 9, 0, 0, 5, 0, 0, 0, 4, 4, 0, 0, 12, 0, 0, 0, 8, 8,
            0, 0, 1, 2, 0, 7, 3, 3, 0, 14, 13, 0, 0, 0, 9, 9, 0, 0, 5, 0, 0, 0,
            4, 4, 0, 0, 12, 0, 0, 8, 8, 8, 0, 0, 0, 0, 0, 3, 3, 3, 0, 14, 13,
            0, 0, 9, 9, 10, 0, 0, 5, 0, 0, 0, 4, 0, 0, 1, 12, 1, 0, 8, 8, 8, 0,
            0, 1, 0, 0, 3, 3, 3, 0, 14, 13, 0, 0, 9, 9, 10, 0, 0, 5, 0, 0, 4,
            4, 5, 0, 1, 12, 0, 0, 8, 8, 8, 0, 0, 1, 0, 0, 3, 3, 3, 0, 14, 13,
            0, 0, 0, 9, 9, 0, 0, 5, 0, 0, 0, 4, 4, 0, 1, 12, 0, 0, 8, 8, 8, 0,
            0, 1, 0, 0, 3, 3, 3, 0, 14, 13, 0, 0, 0, 9, 9, 0, 0, 5, 0, 0, 0, 4,
            4, 0,
        )
    }
}
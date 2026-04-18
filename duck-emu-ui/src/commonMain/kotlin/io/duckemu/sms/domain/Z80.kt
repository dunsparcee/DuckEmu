package io.duckemu.sms.domain

import io.duckemu.sms.data.Setup
import kotlin.math.min
import kotlin.random.Random


class Z80
    (
    /** Reference to ports attached to Z80  */
    private val port: Ports
) {
    // --------------------------------------------------------------------------------------------
    // Z80 Internal Stuff
    // --------------------------------------------------------------------------------------------
    /** Program Counter  */
    private var pc = 0

    /** Stack Pointer  */
    private var sp = 0

    /** Interrupt Mode (0,1,2)  */
    private var im = 0

    /** Interrupt Flip Flop 1  */
    private var iff1 = false

    /** Interrupt Flip Flop 2  */
    private var iff2 = false

    /** Halt Instruction Called  */
    private var halt = false

    /** EI Instruction Called  */
    private var EI_inst = false

    // --------------------------------------------------------------------------------------------
    // Registers
    // --------------------------------------------------------------------------------------------
    /** Accumulator Register  */
    private var a = 0
    private var a2 = 0

    /** BC Register  */
    private var b = 0
    private var c = 0
    private var b2 = 0
    private var c2 = 0

    /** DE Register  */
    private var d = 0
    private var e = 0
    private var d2 = 0
    private var e2 = 0

    /** HL Register  */
    private var h = 0
    private var l = 0
    private var h2 = 0
    private var l2 = 0

    /** IX Register  */
    private var ixL = 0
    private var ixH = 0

    /** IY Register  */
    private var iyL = 0
    private var iyH = 0

    /** Memory Refresh Register  */
    private var r = 0

    /** Interrupt page address register  */
    private var i = 0

    /**
     * Reset
     *
     * Note that some of these values aren't what a real Z80 would reset to.
     * They are the values that the SMS BIOS (to the best of my knowledge)
     * sets the registers to.
     *
     * For example, the Index Registers should reset to 0xFFFF
     * but doing so breaks 'Prince of Persia', so they are set to 0x0000.
     *
     * The stack pointer is also reset to 0xDFF0 as opposed to 0x0000.
     */
    fun reset() {
        a2 = 0
        a = a2

        c2 = 0
        b2 = c2
        c = b2
        b = c
        e2 = 0
        d2 = e2
        e = d2
        d = e
        l2 = 0
        h2 = l2
        l = h2
        h = l
        ixH = 0
        ixL = ixH
        iyH = 0
        iyL = iyH

        r = 0
        i = 0
        f = 0
        f2 = 0

        pc = 0x0000
        sp = 0xDFF0
        tstates = 0
        totalCycles = 0

        im = 0
        iff1 = false
        iff2 = false
        EI_inst = false
        interruptVector = 0
        halt = false
    }


    /**
     * Return Next Opcode for Debugging Purposes
     *
     * @return             String containing opcode bytes
     */
    /*public final String getOp()
    {
        int opcode = readMem(pc);
        String oplist = Integer.toHexString(opcode&0xff);

        switch(opcode)
        {
            case 0xCB:
            case 0xED: opcode = readMem(pc+1); oplist += " "+Integer.toHexString(opcode&0xff); break;
            case 0xDD:
            case 0xFD:
                opcode = readMem(pc+1);
                oplist += " "+Integer.toHexString(opcode&0xff);
                if (opcode == 0xCB) // DDCB etc
                {
                    opcode = readMem(pc+3);
                    oplist += " "+Integer.toHexString(opcode&0xff);
                }
                break;
            default:
                break;
        }

        return oplist.toUpperCase();
    }*/
    /**
     * Return Mnemonic of next opcode for Debugging Purposes
     *
     * @return             String containing opcode bytes
     */
    /*public final String getMnu()
    {
        int opcode = readMem(pc);
        Mnemonic mnu = new Mnemonic();
        switch(opcode)
        {
            // special cases
            case 0xDD:
            case 0xFD:
                opcode = readMem(pc+1); return mnu.getIndex(opcode);
            case 0xCB:
                opcode = readMem(pc+1); return mnu.getCB(opcode);
            case 0xED:
                opcode = readMem(pc+1); return mnu.getED(opcode);
            default:
                return mnu.getOP(opcode);
        }
        return null;
    }*/
    /**
     * Output Contents of Z80 Registers to Console for Debugging Purposes
     */
    /*private final void consoledebug()
    {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(Integer.toHexString(pc)+" 0x"+getOp()+" "+getMnu());
        System.out.println("A : "+Integer.toHexString(a)+" BC : "+Integer.toHexString(getBC())+" DE : "+Integer.toHexString(getDE())+" HL : "+Integer.toHexString(getHL())+
                           " IX: "+Integer.toHexString(getIX())+  " IY: "+Integer.toHexString(getIY()));

        exAF(); exBC(); exDE(); exHL();

        System.out.println("A': "+Integer.toHexString(a)+" BC': "+Integer.toHexString(getBC())+" DE': "+Integer.toHexString(getDE())+ " HL': "+Integer.toHexString(getHL())+ " SP: "+Integer.toHexString(sp));

        exAF(); exBC(); exDE(); exHL();

        //System.out.println("FS: "+getSign()+" FZ: "+getZero()+" FHC: "+getHc()+" FP: "+getParity()+" FN: "+getNegative()+ " FC: "+getCarry());
    }*/
    /**
     * Run Z80
     *
     * @param cycles       Machine cycles to run for in total
     * @param cycles
     */
    fun run(cycles: Int, cyclesTo: Int) {
        tstates += cycles

        if (cycles != 0) totalCycles = cycles

        if (!Setup.ACCURATE_INTERRUPT_EMULATION) {
            if (interruptLine) interrupt() // Check for interrupt
        }

        while (tstates > cyclesTo) {
            if (Setup.ACCURATE_INTERRUPT_EMULATION) {
                if (interruptLine) interrupt() // Check for interrupt
            }


            // ------------------------------------------------------------------------------------
            // Fetch & Interpret Opcodes
            // Main Opcode Switch Rolled In For Speed
            // ------------------------------------------------------------------------------------
            val opcode = readMem(pc++) // Fetch & Interpret Opcode

            if (Setup.ACCURATE_INTERRUPT_EMULATION) EI_inst = false

            tstates -= OP_STATES!![opcode].toInt() // Decrement TStates

            if (Setup.REFRESH_EMULATION) incR()

            when (opcode) {
                0x00 -> {}
                0x01 -> {
                    c = readMem(pc++)
                    b = readMem(pc++)
                }

                0x02 -> writeMem(this.bC, a)
                0x03 -> incBC()
                0x04 -> b = inc8(b)
                0x05 -> b = dec8(b)
                0x06 -> b = readMem(pc++)
                0x07 -> rlca_a()
                0x08 -> exAF()
                0x09 -> this.hL = add16(this.hL, this.bC)
                0x0A -> a = readMem(this.bC)
                0x0B -> decBC()
                0x0C -> c = inc8(c)
                0x0D -> c = dec8(c)
                0x0E -> c = readMem(pc++)
                0x0F -> rrca_a()
                0x10 -> {
                    b = (b - 1) and 0xff // DJNZ (PC+e)
                    jr(b != 0)
                }

                0x11 -> {
                    e = readMem(pc++)
                    d = readMem(pc++)
                }

                0x12 -> writeMem(this.dE, a)
                0x13 -> incDE()
                0x14 -> d = inc8(d)
                0x15 -> d = dec8(d)
                0x16 -> d = (readMem(pc++))
                0x17 -> rla_a()
                0x18 -> pc += d() + 1
                0x19 -> this.hL = add16(this.hL, this.dE)
                0x1A -> a = readMem(this.dE)
                0x1B -> decDE()
                0x1C -> e = inc8(e)
                0x1D -> e = dec8(e)
                0x1E -> e = readMem(pc++)
                0x1F -> rra_a()
                0x20 -> jr((f and F_ZERO) == 0)
                0x21 -> {
                    l = readMem(pc++)
                    h = readMem(pc++)
                }

                0x22 -> {
                    // LD (nn),HL
                    var location = readMemWord(pc)
                    writeMem(location, l)
                    writeMem(++location, h)
                    pc += 2
                }

                0x23 -> incHL()
                0x24 -> h = inc8(h)
                0x25 -> h = dec8(h)
                0x26 -> h = readMem(pc++)
                0x27 -> daa()
                0x28 -> jr(((f and F_ZERO) != 0))
                0x29 -> this.hL = add16(this.hL, this.hL)
                0x2A -> {
                    val location = readMemWord(pc)
                    l = readMem(location)
                    h = readMem(location + 1)
                    pc += 2
                }

                0x2B -> decHL()
                0x2C -> l = inc8(l)
                0x2D -> l = dec8(l)
                0x2E -> l = readMem(pc++)
                0x2F -> cpl_a()
                0x30 -> jr((f and F_CARRY) == 0)
                0x31 -> {
                    sp = readMemWord(pc)
                    pc += 2
                }

                0x32 -> {
                    writeMem(readMemWord(pc), a)
                    pc += 2
                }

                0x33 -> sp++
                0x34 -> incMem(this.hL)
                0x35 -> decMem(this.hL)
                0x36 -> writeMem(this.hL, readMem(pc++))
                0x37 -> {
                    f = f or F_CARRY
                    f = f and F_NEGATIVE.inv()
                    f = f and F_HALFCARRY.inv() // SCF
                }

                0x38 -> jr((f and F_CARRY) != 0)
                0x39 -> this.hL = add16(this.hL, sp)
                0x3A -> {
                    a = readMem(readMemWord(pc))
                    pc += 2
                }

                0x3B -> sp--
                0x3C -> a = inc8(a)
                0x3D -> a = dec8(a)
                0x3E -> a = readMem(pc++)
                0x3F -> ccf()
                0x40 -> {}
                0x41 -> b = c
                0x42 -> b = d
                0x43 -> b = e
                0x44 -> b = h
                0x45 -> b = l
                0x46 -> b = readMem(this.hL)
                0x47 -> b = a
                0x48 -> c = b
                0x49 -> {}
                0x4A -> c = d
                0x4B -> c = e
                0x4C -> c = h
                0x4D -> c = l
                0x4E -> c = readMem(this.hL)
                0x4F -> c = a
                0x50 -> d = b
                0x51 -> d = c
                0x52 -> {}
                0x53 -> d = e
                0x54 -> d = h
                0x55 -> d = l
                0x56 -> d = readMem(this.hL)
                0x57 -> d = a
                0x58 -> e = b
                0x59 -> e = c
                0x5A -> e = d
                0x5B -> {}
                0x5C -> e = h
                0x5D -> e = l
                0x5E -> e = readMem(this.hL)
                0x5F -> e = a
                0x60 -> h = b
                0x61 -> h = c
                0x62 -> h = d
                0x63 -> h = e
                0x64 -> {}
                0x65 -> h = l
                0x66 -> h = readMem(this.hL)
                0x67 -> h = a
                0x68 -> l = b
                0x69 -> l = c
                0x6A -> l = d
                0x6B -> l = e
                0x6C -> l = h
                0x6D -> {}
                0x6E -> l = readMem(this.hL)
                0x6F -> l = a
                0x70 -> writeMem(this.hL, b)
                0x71 -> writeMem(this.hL, c)
                0x72 -> writeMem(this.hL, d)
                0x73 -> writeMem(this.hL, e)
                0x74 -> writeMem(this.hL, h)
                0x75 -> writeMem(this.hL, l)
                0x76 -> {
                    if (HALT_SPEEDUP) tstates = 0
                    halt = true
                    pc--
                    return  // HALT
                }

                0x77 -> writeMem(this.hL, a)
                0x78 -> a = b
                0x79 -> a = c
                0x7A -> a = d
                0x7B -> a = e
                0x7C -> a = h
                0x7D -> a = l
                0x7E -> a = readMem(this.hL)
                0x7F -> {}
                0x80 -> add_a(b)
                0x81 -> add_a(c)
                0x82 -> add_a(d)
                0x83 -> add_a(e)
                0x84 -> add_a(h)
                0x85 -> add_a(l)
                0x86 -> add_a(readMem(this.hL))
                0x87 -> add_a(a)
                0x88 -> adc_a(b)
                0x89 -> adc_a(c)
                0x8A -> adc_a(d)
                0x8B -> adc_a(e)
                0x8C -> adc_a(h)
                0x8D -> adc_a(l)
                0x8E -> adc_a(readMem(this.hL))
                0x8F -> adc_a(a)
                0x90 -> sub_a(b)
                0x91 -> sub_a(c)
                0x92 -> sub_a(d)
                0x93 -> sub_a(e)
                0x94 -> sub_a(h)
                0x95 -> sub_a(l)
                0x96 -> sub_a(readMem(this.hL))
                0x97 -> sub_a(a)
                0x98 -> sbc_a(b)
                0x99 -> sbc_a(c)
                0x9A -> sbc_a(d)
                0x9B -> sbc_a(e)
                0x9C -> sbc_a(h)
                0x9D -> sbc_a(l)
                0x9E -> sbc_a(readMem(this.hL))
                0x9F -> sbc_a(a)
                0xA0 -> f = SZP_TABLE[b.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA1 -> f = SZP_TABLE[c.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA2 -> f = SZP_TABLE[d.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA3 -> f = SZP_TABLE[e.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA4 -> f = SZP_TABLE[h.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA5 -> f = SZP_TABLE[l.let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA6 -> f = SZP_TABLE[readMem(this.hL).let { a = a and it; a }].toInt() or F_HALFCARRY
                0xA7 -> f = SZP_TABLE[a].toInt() or F_HALFCARRY
                0xA8 -> f = SZP_TABLE[b.let { a = a xor it; a }].toInt()
                0xA9 -> f = SZP_TABLE[c.let { a = a xor it; a }].toInt()
                0xAA -> f = SZP_TABLE[d.let { a = a xor it; a }].toInt()
                0xAB -> f = SZP_TABLE[e.let { a = a xor it; a }].toInt()
                0xAC -> f = SZP_TABLE[h.let { a = a xor it; a }].toInt()
                0xAD -> f = SZP_TABLE[l.let { a = a xor it; a }].toInt()
                0xAE -> f = SZP_TABLE[readMem(this.hL).let { a = a xor it; a }].toInt()
                0xAF -> f = SZP_TABLE[0.also { a = it }].toInt()
                0xB0 -> f = SZP_TABLE[b.let { a = a or it; a }].toInt()
                0xB1 -> f = SZP_TABLE[c.let { a = a or it; a }].toInt()
                0xB2 -> f = SZP_TABLE[d.let { a = a or it; a }].toInt()
                0xB3 -> f = SZP_TABLE[e.let { a = a or it; a }].toInt()
                0xB4 -> f = SZP_TABLE[h.let { a = a or it; a }].toInt()
                0xB5 -> f = SZP_TABLE[l.let { a = a or it; a }].toInt()
                0xB6 -> f = SZP_TABLE[readMem(this.hL).let { a = a or it; a }].toInt()
                0xB7 -> f = SZP_TABLE[a].toInt()
                0xB8 -> cp_a(b)
                0xB9 -> cp_a(c)
                0xBA -> cp_a(d)
                0xBB -> cp_a(e)
                0xBC -> cp_a(h)
                0xBD -> cp_a(l)
                0xBE -> cp_a(readMem(this.hL))
                0xBF -> cp_a(a)
                0xC0 -> ret((f and F_ZERO) == 0)
                0xC1 -> {
                    this.bC = readMemWord(sp)
                    sp += 2
                }

                0xC2 -> jp((f and F_ZERO) == 0)
                0xC3 -> pc = readMemWord(pc)
                0xC4 -> call((f and F_ZERO) == 0)
                0xC5 -> push(b, c)
                0xC6 -> add_a(readMem(pc++))
                0xC7 -> {
                    push(pc)
                    pc = 0x00
                }

                0xC8 -> ret((f and F_ZERO) != 0)
                0xC9 -> {
                    pc = readMemWord(sp)
                    sp += 2
                }

                0xCA -> jp((f and F_ZERO) != 0)
                0xCB -> doCB(readMem(pc++))
                0xCC -> call((f and F_ZERO) != 0)
                0xCD -> {
                    push(pc + 2)
                    pc = readMemWord(pc)
                }

                0xCE -> adc_a(readMem(pc++))
                0xCF -> {
                    push(pc)
                    pc = 0x08
                }

                0xD0 -> ret((f and F_CARRY) == 0)
                0xD1 -> {
                    this.dE = readMemWord(sp)
                    sp += 2
                }

                0xD2 -> jp((f and F_CARRY) == 0)
                0xD3 -> port.out(readMem(pc++), a)
                0xD4 -> call((f and F_CARRY) == 0)
                0xD5 -> push(d, e)
                0xD6 -> sub_a(readMem(pc++))
                0xD7 -> {
                    push(pc)
                    pc = 0x10
                }

                0xD8 -> ret(((f and F_CARRY) != 0))
                0xD9 -> {
                    exBC()
                    exDE()
                    exHL()
                }

                0xDA -> jp((f and F_CARRY) != 0)
                0xDB -> a = port.`in`(readMem(pc++))
                0xDC -> call((f and F_CARRY) != 0)
                0xDD -> doIndexOpIX(readMem(pc++))
                0xDE -> sbc_a(readMem(pc++))
                0xDF -> {
                    push(pc)
                    pc = 0x18
                }

                0xE0 -> ret((f and F_PARITY) == 0)
                0xE1 -> {
                    this.hL = readMemWord(sp)
                    sp += 2
                }

                0xE2 -> jp((f and F_PARITY) == 0)
                0xE3 -> {
                    // EX (SP),HL
                    var temp = h
                    h = readMem(sp + 1)
                    writeMem(sp + 1, temp)

                    temp = l
                    l = readMem(sp)
                    writeMem(sp, temp)
                }

                0xE4 -> call((f and F_PARITY) == 0)
                0xE5 -> push(h, l)
                0xE6 -> f = SZP_TABLE[readMem(pc++).let { a = a and it; a }].toInt() or F_HALFCARRY
                0xE7 -> {
                    push(pc)
                    pc = 0x20
                }

                0xE8 -> ret((f and F_PARITY) != 0)
                0xE9 -> pc = this.hL
                0xEA -> jp((f and F_PARITY) != 0)
                0xEB -> {
                    // EX DE,HL
                    var temp = d
                    d = h
                    h = temp
                    temp = e
                    e = l
                    l = temp
                }

                0xEC -> call((f and F_PARITY) != 0)
                0xED -> doED(readMem(pc))
                0xEE -> f = SZP_TABLE[readMem(pc++).let { a = a xor it; a }].toInt()
                0xEF -> {
                    push(pc)
                    pc = 0x28
                }

                0xF0 -> ret((f and F_SIGN) == 0)
                0xF1 -> {
                    f = readMem(sp++)
                    a = readMem(sp++)
                }

                0xF2 -> jp((f and F_SIGN) == 0)
                0xF3 -> {
                    run {
                        iff2 = false
                        iff1 = iff2
                    }
                    EI_inst = true
                }

                0xF4 -> call((f and F_SIGN) == 0)
                0xF5 -> push(a, f)
                0xF6 -> f = SZP_TABLE[readMem(pc++).let { a = a or it; a }].toInt()
                0xF7 -> {
                    push(pc)
                    pc = 0x30
                }

                0xF8 -> ret((f and F_SIGN) != 0)
                0xF9 -> sp = this.hL
                0xFA -> jp((f and F_SIGN) != 0)
                0xFB -> {
                    EI_inst = true
                    iff2 = EI_inst
                    iff1 = iff2
                }

                0xFC -> call((f and F_SIGN) != 0)
                0xFD -> doIndexOpIY(readMem(pc++))
                0xFE -> cp_a(readMem(pc++))
                0xFF -> {
                    push(pc)
                    pc = 0x38
                }
            } // end switch
        }
    }

    /**
     * Generate Non Maskable Interrupt (NMI)
     */
    fun nmi() {
        iff2 = iff1
        iff1 = false

        if (Setup.REFRESH_EMULATION) incR()


        // If we're in a halt instruction, increment the PC and get out of it
        if (halt) {
            pc++
            halt = false
        }

        push(pc) // Preserve PC on stack
        pc = 0x66
        tstates -= 11
    }


    /**
     * Normal Interrupt Routine
     */
    private fun interrupt() {
        // Interrupts not allowed OR
        // Intterupts not allowed after EI instruction
        if (!iff1 || (Setup.ACCURATE_INTERRUPT_EMULATION && EI_inst)) return


        // If we're in a halt instruction, increment the PC and get out of it
        if (halt) {
            pc++
            halt = false
        }

        if (Setup.REFRESH_EMULATION) incR()

        iff2 = false
        iff1 = iff2
        interruptLine = false

        push(pc) // Preserve PC on stack


        // IM 0: Execute Instruction on Bus
        if (im == 0) {
            pc = if (interruptVector == 0 || interruptVector == 0xFF) 0x38 else interruptVector
            tstates -= 13
        } else if (im == 1) {
            pc = 0x38
            tstates -= 13
        } else {
            pc = readMemWord((i shl 8) + interruptVector)
            tstates -= 19
        }
    }

    /**
     * Jump
     *
     * @param condition        If true jump will be taken
     */
    private fun jp(condition: Boolean) {
        if (condition) pc = readMemWord(pc)
        else pc += 2
    }


    /**
     * Jump Relative
     *
     * @param condition        If true jump will be taken
     */
    private fun jr(condition: Boolean) {
        if (condition) {
            pc += d() + 1
            tstates -= 5
        } else pc++
    }


    /**
     * Call
     *
     * @param condition        If true call will be taken
     */
    private fun call(condition: Boolean) {
        if (condition) {
            push(pc + 2) // write value of PC to stack
            pc = readMemWord(pc)
            tstates -= 7
        } else pc += 2
    }


    /**
     * Return
     *
     * @param condition        If true return will be taken
     */
    private fun ret(condition: Boolean) {
        if (condition) {
            pc = readMemWord(sp)
            sp += 2
            tstates -= 6
        }
    }


    /**
     * Push Value Onto Stack
     *
     * @param value        Value to push
     */
    private fun push(value: Int) {
        writeMem(--sp, value shr 8) // (SP - 1) <- high
        writeMem(--sp, value and 0xff) // (SP - 2) <- low
    }

    private fun push(h: Int, l: Int) {
        writeMem(--sp, h) // (SP - 1) <- high
        writeMem(--sp, l) // (SP - 2) <- low
    }


    /**
     * INC - Increment Memory Location
     *
     * @param offset       Memory Offset to Increment
     */
    private fun incMem(offset: Int) {
        writeMem(offset, inc8(readMem(offset)))
    }


    /**
     * DEC - Decrement Memory Location
     *
     * @param offset       Memory Offset to Increment
     */
    private fun decMem(offset: Int) {
        writeMem(offset, dec8(readMem(offset)))
    }

    /**
     * CCF - Complement Carry Flag
     */
    private fun ccf() {
        if ((f and F_CARRY) != 0) {
            f = f and F_CARRY.inv()
            f = f or F_HALFCARRY
        } else {
            f = f or F_CARRY
            f = f and F_HALFCARRY.inv()
        }
        f = f and F_NEGATIVE.inv()
    }


    /**
     * DAA - Decimal Adjust Accumulator
     * adds 6 to left and/or right nibble
     *
     * Pre-Calculated Result For Speed
     *
     * Checked with ZEXALL
     */
    private fun daa() {
        // Get result for calculated table (carry flag = bit 8, negative = bit 9, halfcarry = bit 10)
        val temp =
            DAA_TABLE[a or ((f and F_CARRY) shl 8) or ((f and F_NEGATIVE) shl 8) or ((f and F_HALFCARRY) shl 6)].toInt()
        a = temp and 0xFF
        f = (f and F_NEGATIVE) or (temp shr 8)
    }


    /**
     * Execute CB Prefixed Opcode
     *
     * @param opcode       Opcode hex value
     */
    private fun doCB(opcode: Int) {
        if (Setup.REFRESH_EMULATION) incR()

        tstates -= OP_CB_STATES!![opcode].toInt()

        when (opcode) {
            0x00 -> b = (rlc(b))
            0x01 -> c = (rlc(c))
            0x02 -> d = (rlc(d))
            0x03 -> e = (rlc(e))
            0x04 -> h = (rlc(h))
            0x05 -> l = (rlc(l))
            0x06 -> writeMem(this.hL, rlc(readMem(this.hL)))
            0x07 -> a = rlc(a)
            0x08 -> b = (rrc(b))
            0x09 -> c = (rrc(c))
            0x0A -> d = (rrc(d))
            0x0B -> e = (rrc(e))
            0x0C -> h = (rrc(h))
            0x0D -> l = (rrc(l))
            0x0E -> writeMem(this.hL, rrc(readMem(this.hL)))
            0x0F -> a = rrc(a)
            0x10 -> b = (rl(b))
            0x11 -> c = (rl(c))
            0x12 -> d = (rl(d))
            0x13 -> e = (rl(e))
            0x14 -> h = (rl(h))
            0x15 -> l = (rl(l))
            0x16 -> writeMem(this.hL, rl(readMem(this.hL)))
            0x17 -> a = rl(a)
            0x18 -> b = (rr(b))
            0x19 -> c = (rr(c))
            0x1A -> d = (rr(d))
            0x1B -> e = (rr(e))
            0x1C -> h = (rr(h))
            0x1D -> l = (rr(l))
            0x1E -> writeMem(this.hL, rr(readMem(this.hL)))
            0x1F -> a = rr(a)
            0x20 -> b = (sla(b))
            0x21 -> c = (sla(c))
            0x22 -> d = (sla(d))
            0x23 -> e = (sla(e))
            0x24 -> h = (sla(h))
            0x25 -> l = (sla(l))
            0x26 -> writeMem(this.hL, sla(readMem(this.hL)))
            0x27 -> a = sla(a)
            0x28 -> b = (sra(b))
            0x29 -> c = (sra(c))
            0x2A -> d = (sra(d))
            0x2B -> e = (sra(e))
            0x2C -> h = (sra(h))
            0x2D -> l = (sra(l))
            0x2E -> writeMem(this.hL, sra(readMem(this.hL)))
            0x2F -> a = sra(a)
            0x30 -> b = (sll(b))
            0x31 -> c = (sll(c))
            0x32 -> d = (sll(d))
            0x33 -> e = (sll(e))
            0x34 -> h = (sll(h))
            0x35 -> l = (sll(l))
            0x36 -> writeMem(this.hL, sll(readMem(this.hL)))
            0x37 -> a = (sll(a))
            0x38 -> b = (srl(b))
            0x39 -> c = (srl(c))
            0x3A -> d = (srl(d))
            0x3B -> e = (srl(e))
            0x3C -> h = (srl(h))
            0x3D -> l = (srl(l))
            0x3E -> writeMem(this.hL, srl(readMem(this.hL)))
            0x3F -> a = srl(a)
            0x40 -> bit(b and BIT_0)
            0x41 -> bit(c and BIT_0)
            0x42 -> bit(d and BIT_0)
            0x43 -> bit(e and BIT_0)
            0x44 -> bit(h and BIT_0)
            0x45 -> bit(l and BIT_0)
            0x46 -> bit(readMem(this.hL) and BIT_0)
            0x47 -> bit(a and BIT_0)
            0x48 -> bit(b and BIT_1)
            0x49 -> bit(c and BIT_1)
            0x4A -> bit(d and BIT_1)
            0x4B -> bit(e and BIT_1)
            0x4C -> bit(h and BIT_1)
            0x4D -> bit(l and BIT_1)
            0x4E -> bit(readMem(this.hL) and BIT_1)
            0x4F -> bit(a and BIT_1)
            0x50 -> bit(b and BIT_2)
            0x51 -> bit(c and BIT_2)
            0x52 -> bit(d and BIT_2)
            0x53 -> bit(e and BIT_2)
            0x54 -> bit(h and BIT_2)
            0x55 -> bit(l and BIT_2)
            0x56 -> bit(readMem(this.hL) and BIT_2)
            0x57 -> bit(a and BIT_2)
            0x58 -> bit(b and BIT_3)
            0x59 -> bit(c and BIT_3)
            0x5A -> bit(d and BIT_3)
            0x5B -> bit(e and BIT_3)
            0x5C -> bit(h and BIT_3)
            0x5D -> bit(l and BIT_3)
            0x5E -> bit(readMem(this.hL) and BIT_3)
            0x5F -> bit(a and BIT_3)
            0x60 -> bit(b and BIT_4)
            0x61 -> bit(c and BIT_4)
            0x62 -> bit(d and BIT_4)
            0x63 -> bit(e and BIT_4)
            0x64 -> bit(h and BIT_4)
            0x65 -> bit(l and BIT_4)
            0x66 -> bit(readMem(this.hL) and BIT_4)
            0x67 -> bit(a and BIT_4)
            0x68 -> bit(b and BIT_5)
            0x69 -> bit(c and BIT_5)
            0x6A -> bit(d and BIT_5)
            0x6B -> bit(e and BIT_5)
            0x6C -> bit(h and BIT_5)
            0x6D -> bit(l and BIT_5)
            0x6E -> bit(readMem(this.hL) and BIT_5)
            0x6F -> bit(a and BIT_5)
            0x70 -> bit(b and BIT_6)
            0x71 -> bit(c and BIT_6)
            0x72 -> bit(d and BIT_6)
            0x73 -> bit(e and BIT_6)
            0x74 -> bit(h and BIT_6)
            0x75 -> bit(l and BIT_6)
            0x76 -> bit(readMem(this.hL) and BIT_6)
            0x77 -> bit(a and BIT_6)
            0x78 -> bit(b and BIT_7)
            0x79 -> bit(c and BIT_7)
            0x7A -> bit(d and BIT_7)
            0x7B -> bit(e and BIT_7)
            0x7C -> bit(h and BIT_7)
            0x7D -> bit(l and BIT_7)
            0x7E -> bit(readMem(this.hL) and BIT_7)
            0x7F -> bit(a and BIT_7)
            0x80 -> b = b and BIT_0.inv()
            0x81 -> c = c and BIT_0.inv()
            0x82 -> d = d and BIT_0.inv()
            0x83 -> e = e and BIT_0.inv()
            0x84 -> h = h and BIT_0.inv()
            0x85 -> l = l and BIT_0.inv()
            0x86 -> writeMem(this.hL, readMem(this.hL) and BIT_0.inv())
            0x87 -> a = a and BIT_0.inv()
            0x88 -> b = b and BIT_1.inv()
            0x89 -> c = c and BIT_1.inv()
            0x8A -> d = d and BIT_1.inv()
            0x8B -> e = e and BIT_1.inv()
            0x8C -> h = h and BIT_1.inv()
            0x8D -> l = l and BIT_1.inv()
            0x8E -> writeMem(this.hL, readMem(this.hL) and BIT_1.inv())
            0x8F -> a = a and BIT_1.inv()
            0x90 -> b = b and BIT_2.inv()
            0x91 -> c = c and BIT_2.inv()
            0x92 -> d = d and BIT_2.inv()
            0x93 -> e = e and BIT_2.inv()
            0x94 -> h = h and BIT_2.inv()
            0x95 -> l = l and BIT_2.inv()
            0x96 -> writeMem(this.hL, readMem(this.hL) and BIT_2.inv())
            0x97 -> a = a and BIT_2.inv()
            0x98 -> b = b and BIT_3.inv()
            0x99 -> c = c and BIT_3.inv()
            0x9A -> d = d and BIT_3.inv()
            0x9B -> e = e and BIT_3.inv()
            0x9C -> h = h and BIT_3.inv()
            0x9D -> l = l and BIT_3.inv()
            0x9E -> writeMem(this.hL, readMem(this.hL) and BIT_3.inv())
            0x9F -> a = a and BIT_3.inv()
            0xA0 -> b = b and BIT_4.inv()
            0xA1 -> c = c and BIT_4.inv()
            0xA2 -> d = d and BIT_4.inv()
            0xA3 -> e = e and BIT_4.inv()
            0xA4 -> h = h and BIT_4.inv()
            0xA5 -> l = l and BIT_4.inv()
            0xA6 -> writeMem(this.hL, readMem(this.hL) and BIT_4.inv())
            0xA7 -> a = a and BIT_4.inv()
            0xA8 -> b = b and BIT_5.inv()
            0xA9 -> c = c and BIT_5.inv()
            0xAA -> d = d and BIT_5.inv()
            0xAB -> e = e and BIT_5.inv()
            0xAC -> h = h and BIT_5.inv()
            0xAD -> l = l and BIT_5.inv()
            0xAE -> writeMem(this.hL, readMem(this.hL) and BIT_5.inv())
            0xAF -> a = a and BIT_5.inv()
            0xB0 -> b = b and BIT_6.inv()
            0xB1 -> c = c and BIT_6.inv()
            0xB2 -> d = d and BIT_6.inv()
            0xB3 -> e = e and BIT_6.inv()
            0xB4 -> h = h and BIT_6.inv()
            0xB5 -> l = l and BIT_6.inv()
            0xB6 -> writeMem(this.hL, readMem(this.hL) and BIT_6.inv())
            0xB7 -> a = a and BIT_6.inv()
            0xB8 -> b = b and BIT_7.inv()
            0xB9 -> c = c and BIT_7.inv()
            0xBA -> d = d and BIT_7.inv()
            0xBB -> e = e and BIT_7.inv()
            0xBC -> h = h and BIT_7.inv()
            0xBD -> l = l and BIT_7.inv()
            0xBE -> writeMem(this.hL, readMem(this.hL) and BIT_7.inv())
            0xBF -> a = a and BIT_7.inv()
            0xC0 -> b = b or BIT_0
            0xC1 -> c = c or BIT_0
            0xC2 -> d = d or BIT_0
            0xC3 -> e = e or BIT_0
            0xC4 -> h = h or BIT_0
            0xC5 -> l = l or BIT_0
            0xC6 -> writeMem(this.hL, readMem(this.hL) or BIT_0)
            0xC7 -> a = a or BIT_0
            0xC8 -> b = b or BIT_1
            0xC9 -> c = c or BIT_1
            0xCA -> d = d or BIT_1
            0xCB -> e = e or BIT_1
            0xCC -> h = h or BIT_1
            0xCD -> l = l or BIT_1
            0xCE -> writeMem(this.hL, readMem(this.hL) or BIT_1)
            0xCF -> a = a or BIT_1
            0xD0 -> b = b or BIT_2
            0xD1 -> c = c or BIT_2
            0xD2 -> d = d or BIT_2
            0xD3 -> e = e or BIT_2
            0xD4 -> h = h or BIT_2
            0xD5 -> l = l or BIT_2
            0xD6 -> writeMem(this.hL, readMem(this.hL) or BIT_2)
            0xD7 -> a = a or BIT_2
            0xD8 -> b = b or BIT_3
            0xD9 -> c = c or BIT_3
            0xDA -> d = d or BIT_3
            0xDB -> e = e or BIT_3
            0xDC -> h = h or BIT_3
            0xDD -> l = l or BIT_3
            0xDE -> writeMem(this.hL, readMem(this.hL) or BIT_3)
            0xDF -> a = a or BIT_3
            0xE0 -> b = b or BIT_4
            0xE1 -> c = c or BIT_4
            0xE2 -> d = d or BIT_4
            0xE3 -> e = e or BIT_4
            0xE4 -> h = h or BIT_4
            0xE5 -> l = l or BIT_4
            0xE6 -> writeMem(this.hL, readMem(this.hL) or BIT_4)
            0xE7 -> a = a or BIT_4
            0xE8 -> b = b or BIT_5
            0xE9 -> c = c or BIT_5
            0xEA -> d = d or BIT_5
            0xEB -> e = e or BIT_5
            0xEC -> h = h or BIT_5
            0xED -> l = l or BIT_5
            0xEE -> writeMem(this.hL, readMem(this.hL) or BIT_5)
            0xEF -> a = a or BIT_5
            0xF0 -> b = b or BIT_6
            0xF1 -> c = c or BIT_6
            0xF2 -> d = d or BIT_6
            0xF3 -> e = e or BIT_6
            0xF4 -> h = h or BIT_6
            0xF5 -> l = l or BIT_6
            0xF6 -> writeMem(this.hL, readMem(this.hL) or BIT_6)
            0xF7 -> a = a or BIT_6
            0xF8 -> b = b or BIT_7
            0xF9 -> c = c or BIT_7
            0xFA -> d = d or BIT_7
            0xFB -> e = e or BIT_7
            0xFC -> h = h or BIT_7
            0xFD -> l = l or BIT_7
            0xFE -> writeMem(this.hL, readMem(this.hL) or BIT_7)
            0xFF -> a = a or BIT_7
        }
    }


    /**
     * CB RLC - Rotate Left Carry
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun rlc(value: Int): Int {
        var value = value
        val carry = (value and 0x80) shr 7
        value = ((value shl 1) or (value shr 7)) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    /**
     * CB RRC - Rotate Right Carry
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun rrc(value: Int): Int {
        var value = value
        val carry = (value and 0x01)
        value = ((value shr 1) or (value shl 7)) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    /**
     * CB RL - Rotate Left
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun rl(value: Int): Int {
        var value = value
        val carry = (value and 0x80) shr 7
        value = ((value shl 1) or (f and F_CARRY)) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    /**
     * CB RR - Rotate Right
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun rr(value: Int): Int {
        var value = value
        val carry = (value and 0x01)
        value = ((value shr 1) or (f shl 7)) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    /**
     * CB SLA - Shift Left Arithmetic
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun sla(value: Int): Int {
        var value = value
        val carry = (value and 0x80) shr 7
        value = (value shl 1) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    /**
     * CB SLL - Logical Left Shift
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun sll(value: Int): Int {
        var value = value
        val carry = (value and 0x80) shr 7
        value = ((value shl 1) or 1) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }

    /**
     * CB SRA - Shift Right Arithmetic
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun sra(value: Int): Int {
        var value = value
        val carry = value and 0x01
        value = (value shr 1) or (value and 0x80)
        f = carry or SZP_TABLE[value].toInt()
        return value
    }

    /**
     * CB SRL - Logical Shift Right
     *
     * @param value        Value to adjust
     *
     * @return             Adjusted value
     */
    private fun srl(value: Int): Int {
        var value = value
        val carry = value and 0x01
        value = (value shr 1) and 0xff
        f = carry or SZP_TABLE[value].toInt()
        return value
    }


    private fun bit(mask: Int) {
        f = (f and F_CARRY) or SZ_BIT_TABLE[mask].toInt()
    }


    private fun doIndexOpIX(opcode: Int) {
        tstates -= OP_DD_STATES!![opcode].toInt()

        if (Setup.REFRESH_EMULATION) incR()

        when (opcode) {
            0x09 -> this.iX = add16(this.iX, this.bC)
            0x19 -> this.iX = add16(this.iX, this.dE)
            0x21 -> {
                this.iX = readMemWord(pc)
                pc += 2
            }

            0x22 -> {
                // LD (nn),IX
                var location = readMemWord(pc)
                writeMem(location++, ixL)
                writeMem(location, ixH)
                pc += 2
            }

            0x23 -> incIX()
            0x24 -> ixH = inc8(ixH)
            0x25 -> ixH = dec8(ixH)
            0x26 -> ixH = readMem(pc++)
            0x29 -> this.iX = add16(this.iX, this.iX)
            0x2A -> {
                // LD IX,(nn)
                var location = readMemWord(pc)
                ixL = readMem(location)
                ixH = readMem(++location)
                pc += 2
            }

            0x2B -> decIX()
            0x2C -> ixL = inc8(ixL)
            0x2D -> ixL = dec8(ixL)
            0x2E -> ixL = readMem(pc++)
            0x34 -> {
                incMem(this.iX + d())
                pc++
            }

            0x35 -> {
                decMem(this.iX + d())
                pc++
            }

            0x36 -> {
                writeMem(this.iX + d(), readMem(++pc))
                pc++
            }

            0x39 -> this.iX = add16(this.iX, sp)
            0x44 -> b = ixH
            0x45 -> b = ixL
            0x46 -> {
                b = readMem(this.iX + d())
                pc++
            }

            0x4C -> c = ixH
            0x4D -> c = ixL
            0x4E -> {
                c = readMem(this.iX + d())
                pc++
            }

            0x54 -> d = ixH
            0x55 -> d = ixL
            0x56 -> {
                d = readMem(this.iX + d())
                pc++
            }

            0x5C -> e = ixH
            0x5D -> e = ixL
            0x5E -> {
                e = readMem(this.iX + d())
                pc++
            }

            0x60 -> ixH = b
            0x61 -> ixH = c
            0x62 -> ixH = d
            0x63 -> ixH = e
            0x64 -> {}
            0x65 -> ixH = ixL
            0x66 -> {
                h = readMem(this.iX + d())
                pc++
            }

            0x67 -> ixH = a
            0x68 -> ixL = b
            0x69 -> ixL = c
            0x6A -> ixL = d
            0x6B -> ixL = e
            0x6C -> ixL = ixH
            0x6D -> {}
            0x6E -> {
                l = readMem(this.iX + d())
                pc++
            }

            0x6F -> ixL = a
            0x70 -> {
                writeMem(this.iX + d(), b)
                pc++
            }

            0x71 -> {
                writeMem(this.iX + d(), c)
                pc++
            }

            0x72 -> {
                writeMem(this.iX + d(), d)
                pc++
            }

            0x73 -> {
                writeMem(this.iX + d(), e)
                pc++
            }

            0x74 -> {
                writeMem(this.iX + d(), h)
                pc++
            }

            0x75 -> {
                writeMem(this.iX + d(), l)
                pc++
            }

            0x77 -> {
                writeMem(this.iX + d(), a)
                pc++
            }

            0x7C -> a = ixH
            0x7D -> a = ixL
            0x7E -> {
                a = readMem(this.iX + d())
                pc++
            }

            0x84 -> add_a(ixH)
            0x85 -> add_a(ixL)
            0x86 -> {
                add_a(readMem(this.iX + d()))
                pc++
            }

            0x8C -> adc_a(ixH)
            0x8D -> adc_a(ixL)
            0x8E -> {
                adc_a(readMem(this.iX + d()))
                pc++
            }

            0x94 -> sub_a(ixH)
            0x95 -> sub_a(ixL)
            0x96 -> {
                sub_a(readMem(this.iX + d()))
                pc++
            }

            0x9C -> sbc_a(ixH)
            0x9D -> sbc_a(ixL)
            0x9E -> {
                sbc_a(readMem(this.iX + d()))
                pc++
            }

            0xA4 -> f = SZP_TABLE[ixH.let { a = a and it; a }].toInt() or F_HALFCARRY
            0xA5 -> f = SZP_TABLE[ixL.let { a = a and it; a }].toInt() or F_HALFCARRY
            0xA6 -> {
                f = SZP_TABLE[readMem(this.iX + d()).let { a = a and it; a }].toInt() or F_HALFCARRY
                pc++
            }

            0xAC -> f = SZP_TABLE[ixH.let { a = a xor it; a }].toInt()
            0xAD -> f = SZP_TABLE[ixL.let { a = a xor it; a }].toInt()
            0xAE -> {
                f = SZP_TABLE[readMem(this.iX + d()).let { a = a xor it; a }].toInt()
                pc++
            }

            0xB4 -> f = SZP_TABLE[ixH.let { a = a or it; a }].toInt()
            0xB5 -> f = SZP_TABLE[ixL.let { a = a or it; a }].toInt()
            0xB6 -> {
                f = SZP_TABLE[readMem(this.iX + d()).let { a = a or it; a }].toInt()
                pc++
            }

            0xBC -> cp_a(ixH)
            0xBD -> cp_a(ixL)
            0xBE -> {
                cp_a(readMem(this.iX + d()))
                pc++
            }

            0xCB -> doIndexCB(this.iX)
            0xE1 -> {
                this.iX = readMemWord(sp)
                sp += 2
            }

            0xE3 -> {
                val temp = this.iX
                this.iX = readMemWord(sp) // EX SP,(IX)
                writeMem(sp, temp and 0xff)
                writeMem(sp + 1, temp shr 8)
            }

            0xE5 -> push(ixH, ixL)
            0xE9 -> pc = this.iX
            0xF9 -> sp = this.iX
            else -> pc--
        } // end of switch
    }

    private fun doIndexOpIY(opcode: Int) {
        tstates -= OP_DD_STATES!![opcode].toInt()

        if (Setup.REFRESH_EMULATION) incR()

        when (opcode) {
            0x09 -> this.iY = add16(this.iY, this.bC)
            0x19 -> this.iY = add16(this.iY, this.dE)
            0x21 -> {
                this.iY = readMemWord(pc)
                pc += 2
            }

            0x22 -> {
                // LD (nn),IY
                var location = readMemWord(pc)
                writeMem(location++, iyL)
                writeMem(location, iyH)
                pc += 2
            }

            0x23 -> incIY()
            0x24 -> iyH = inc8(iyH)
            0x25 -> iyH = dec8(iyH)
            0x26 -> iyH = readMem(pc++)
            0x29 -> this.iY = add16(this.iY, this.iY)
            0x2A -> {
                // LD IY,(nn)
                var location = readMemWord(pc)
                iyL = readMem(location)
                iyH = readMem(++location)
                pc += 2
            }

            0x2B -> decIY()
            0x2C -> iyL = inc8(iyL)
            0x2D -> iyL = dec8(iyL)
            0x2E -> iyL = readMem(pc++)
            0x34 -> {
                incMem(this.iY + d())
                pc++
            }

            0x35 -> {
                decMem(this.iY + d())
                pc++
            }

            0x36 -> {
                writeMem(this.iY + d(), readMem(++pc))
                pc++
            }

            0x39 -> this.iY = add16(this.iY, sp)
            0x44 -> b = iyH
            0x45 -> b = iyL
            0x46 -> {
                b = readMem(this.iY + d())
                pc++
            }

            0x4C -> c = iyH
            0x4D -> c = iyL
            0x4E -> {
                c = readMem(this.iY + d())
                pc++
            }

            0x54 -> d = iyH
            0x55 -> d = iyL
            0x56 -> {
                d = readMem(this.iY + d())
                pc++
            }

            0x5C -> e = iyH
            0x5D -> e = iyL
            0x5E -> {
                e = readMem(this.iY + d())
                pc++
            }

            0x60 -> iyH = b
            0x61 -> iyH = c
            0x62 -> iyH = d
            0x63 -> iyH = e
            0x64 -> {}
            0x65 -> iyH = iyL
            0x66 -> {
                h = readMem(this.iY + d())
                pc++
            }

            0x67 -> iyH = a
            0x68 -> iyL = b
            0x69 -> iyL = c
            0x6A -> iyL = d
            0x6B -> iyL = e
            0x6C -> iyL = iyH
            0x6D -> {}
            0x6E -> {
                l = readMem(this.iY + d())
                pc++
            }

            0x6F -> iyL = a
            0x70 -> {
                writeMem(this.iY + d(), b)
                pc++
            }

            0x71 -> {
                writeMem(this.iY + d(), c)
                pc++
            }

            0x72 -> {
                writeMem(this.iY + d(), d)
                pc++
            }

            0x73 -> {
                writeMem(this.iY + d(), e)
                pc++
            }

            0x74 -> {
                writeMem(this.iY + d(), h)
                pc++
            }

            0x75 -> {
                writeMem(this.iY + d(), l)
                pc++
            }

            0x77 -> {
                writeMem(this.iY + d(), a)
                pc++
            }

            0x7C -> a = iyH
            0x7D -> a = iyL
            0x7E -> {
                a = readMem(this.iY + d())
                pc++
            }

            0x84 -> add_a(iyH)
            0x85 -> add_a(iyL)
            0x86 -> {
                add_a(readMem(this.iY + d()))
                pc++
            }

            0x8C -> adc_a(iyH)
            0x8D -> adc_a(iyL)
            0x8E -> {
                adc_a(readMem(this.iY + d()))
                pc++
            }

            0x94 -> sub_a(iyH)
            0x95 -> sub_a(iyL)
            0x96 -> {
                sub_a(readMem(this.iY + d()))
                pc++
            }

            0x9C -> sbc_a(iyH)
            0x9D -> sbc_a(iyL)
            0x9E -> {
                sbc_a(readMem(this.iY + d()))
                pc++
            }

            0xA4 -> f = SZP_TABLE[iyH.let { a = a and it; a }].toInt() or F_HALFCARRY
            0xA5 -> f = SZP_TABLE[iyL.let { a = a and it; a }].toInt() or F_HALFCARRY
            0xA6 -> {
                f = SZP_TABLE[readMem(this.iY + d()).let { a = a and it; a }].toInt() or F_HALFCARRY
                pc++
            }

            0xAC -> f = SZP_TABLE[iyH.let { a = a xor it; a }].toInt()
            0xAD -> f = SZP_TABLE[iyL.let { a = a xor it; a }].toInt()
            0xAE -> {
                f = SZP_TABLE[readMem(this.iY + d()).let { a = a xor it; a }].toInt()
                pc++
            }

            0xB4 -> f = SZP_TABLE[iyH.let { a = a or it; a }].toInt()
            0xB5 -> f = SZP_TABLE[iyL.let { a = a or it; a }].toInt()
            0xB6 -> {
                f = SZP_TABLE[readMem(this.iY + d()).let { a = a or it; a }].toInt()
                pc++
            }

            0xBC -> cp_a(iyH)
            0xBD -> cp_a(iyL)
            0xBE -> {
                cp_a(readMem(this.iY + d()))
                pc++
            }

            0xCB -> doIndexCB(this.iY)
            0xE1 -> {
                this.iY = readMemWord(sp)
                sp += 2
            }

            0xE3 -> {
                val temp = this.iY
                this.iY = readMemWord(sp) // EX SP,(IY)
                writeMem(sp, temp and 0xff)
                writeMem(sp + 1, temp shr 8)
            }

            0xE5 -> push(iyH, iyL)
            0xE9 -> pc = this.iY
            0xF9 -> sp = this.iY
            else -> pc--
        } // end of switch
    }

    /**
     * Execute DDCB/FDCB Prefixed Opcode
     *
     * @param index        Index Register To Use
     */
    private fun doIndexCB(index: Int) {
        val location = (index + d()) and 0xFFFF
        val opcode = readMem(++pc)
        tstates -= OP_INDEX_CB_STATES!![opcode].toInt()

        when (opcode) {
            0x06 -> writeMem(location, rlc(readMem(location)))
            0x0E -> writeMem(location, rrc(readMem(location)))
            0x16 -> writeMem(location, rl(readMem(location)))
            0x1E -> writeMem(location, rr(readMem(location)))
            0x26 -> writeMem(location, sla(readMem(location)))
            0x2E -> writeMem(location, sra(readMem(location)))
            0x36 -> writeMem(location, sll(readMem(location)))
            0x3E -> writeMem(location, srl(readMem(location)))
            0x46 -> bit(readMem(location) and BIT_0)
            0x4E -> bit(readMem(location) and BIT_1)
            0x56 -> bit(readMem(location) and BIT_2)
            0x5E -> bit(readMem(location) and BIT_3)
            0x66 -> bit(readMem(location) and BIT_4)
            0x6E -> bit(readMem(location) and BIT_5)
            0x76 -> bit(readMem(location) and BIT_6)
            0x7E -> bit(readMem(location) and BIT_7)
            0x86 -> writeMem(location, readMem(location) and BIT_0.inv())
            0x8E -> writeMem(location, readMem(location) and BIT_1.inv())
            0x96 -> writeMem(location, readMem(location) and BIT_2.inv())
            0x9E -> writeMem(location, readMem(location) and BIT_3.inv())
            0xA6 -> writeMem(location, readMem(location) and BIT_4.inv())
            0xAE -> writeMem(location, readMem(location) and BIT_5.inv())
            0xB6 -> writeMem(location, readMem(location) and BIT_6.inv())
            0xBE -> writeMem(location, readMem(location) and BIT_7.inv())
            0xC6 -> writeMem(location, readMem(location) or BIT_0)
            0xCE -> writeMem(location, readMem(location) or BIT_1)
            0xD6 -> writeMem(location, readMem(location) or BIT_2)
            0xDE -> writeMem(location, readMem(location) or BIT_3)
            0xE6 -> writeMem(location, readMem(location) or BIT_4)
            0xEE -> writeMem(location, readMem(location) or BIT_5)
            0xF6 -> writeMem(location, readMem(location) or BIT_6)
            0xFE -> writeMem(location, readMem(location) or BIT_7)
        } // end of switch
        pc++
    }


    /**
     * Execute ED Prefixed Opcode
     *
     * @param opcode       Opcode hex value
     */
    private fun doED(opcode: Int) {
        var temp: Int

        tstates -= OP_ED_STATES!![opcode].toInt()

        if (Setup.REFRESH_EMULATION) incR()

        when (opcode) {
            0x40 -> {
                b = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[b].toInt()
                pc++
            }

            0x41 -> {
                port.out(c, b)
                pc++
            }

            0x42 -> {
                sbc16(this.bC)
                pc++
            }

            0x43 -> {
                var location = readMemWord(pc + 1)
                writeMem(location++, c)
                writeMem(location, b)
                pc += 3
            }

            0x44, 0x4C, 0x54, 0x5C, 0x64, 0x6C, 0x74, 0x7C -> {
                // A <- 0-A
                val a_copy = a
                a = 0
                sub_a(a_copy)
                pc++
            }

            0x45, 0x4D, 0x55, 0x5D, 0x65, 0x6D, 0x75, 0x7D -> {
                pc = readMemWord(sp)
                sp += 2
                iff1 = iff2
            }

            0x46, 0x4E, 0x66, 0x6E -> {
                im = 0
                pc++
            }

            0x47 -> {
                i = a
                pc++
            }

            0x48 -> {
                c = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[c].toInt()
                pc++
            }

            0x49 -> {
                port.out(c, c)
                pc++
            }

            0x4A -> {
                adc16(this.bC)
                pc++
            }

            0x4B -> {
                var location = readMemWord(pc + 1)
                c = readMem(location++)
                b = readMem(location)
                pc += 3
            }

            0x4F -> {
                r = a
                pc++
            }

            0x50 -> {
                d = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[d].toInt()
                pc++
            }

            0x51 -> {
                port.out(c, d)
                pc++
            }

            0x52 -> {
                sbc16(this.dE)
                pc++
            }

            0x53 -> {
                var location = readMemWord(pc + 1)
                writeMem(location++, e) //SPl
                writeMem(location, d) //SPh
                pc += 3
            }

            0x56, 0x76 -> {
                im = 1
                pc++
            }

            0x57 -> {
                a = i
                f = (f and F_CARRY) or SZ_TABLE[a].toInt() or (if (iff2) F_PARITY else 0)
                pc++
            }

            0x58 -> {
                e = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[e].toInt()
                pc++
            }

            0x59 -> {
                port.out(c, e)
                pc++
            }

            0x5A -> {
                adc16(this.dE)
                pc++
            }

            0x5B -> {
                var location = readMemWord(pc + 1)
                e = readMem(location++)
                d = readMem(location)
                pc += 3
            }

            0x5F -> {
                // Note, to fake refresh emulation we use the random number generator
                a = if (Setup.REFRESH_EMULATION) r else Random.nextInt(255)
                f = (f and F_CARRY) or SZ_TABLE[a].toInt() or (if (iff2) F_PARITY else 0)
                pc++
            }

            0x60 -> {
                h = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[h].toInt()
                pc++
            }

            0x61 -> {
                port.out(c, h)
                pc++
            }

            0x62 -> {
                sbc16(this.hL)
                pc++
            }

            0x63 -> {
                var location = readMemWord(pc + 1)
                writeMem(location++, l) //SPl
                writeMem(location, h) //SPh
                pc += 3
            }

            0x67 -> {
                val location = this.hL
                val hlmem = readMem(location)


                // move high 4 of hl to low 4 of hl
                // move low 4 of a to high 4 of hl
                writeMem(location, (hlmem shr 4) or ((a and 0x0f) shl 4))
                // move 4 lowest bits of hl to low 4 of a
                a = (a and 0xF0) or (hlmem and 0x0F)

                f = (f and F_CARRY) or SZP_TABLE[a].toInt()
                pc++
            }

            0x68 -> {
                l = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[l].toInt()
                pc++
            }

            0x69 -> {
                port.out(c, l)
                pc++
            }

            0x6A -> {
                adc16(this.hL)
                pc++
            }

            0x6B -> {
                var location = readMemWord(pc + 1)
                l = readMem(location++)
                h = readMem(location)
                pc += 3
            }

            0x6F -> {
                val location = this.hL
                val hlmem = readMem(location)


                // move low 4 of hl to high 4 of hl
                // move low 4 of a to low 4 of hl
                writeMem(location, (hlmem and 0x0F) shl 4 or (a and 0x0F))


                // move high 4 of hl to low 4 of a
                a = (a and 0xF0) or (hlmem shr 4)

                f = (f and F_CARRY) or SZP_TABLE[a].toInt()
                pc++
            }

            0x71 -> {
                port.out(c, 0)
                pc++
            }

            0x72 -> {
                sbc16(sp)
                pc++
            }

            0x73 -> {
                var location = readMemWord(pc + 1)
                writeMem(location++, sp and 0xff) //SPl
                writeMem(location, sp shr 8) //SPh
                pc += 3
            }

            0x78 -> {
                a = port.`in`(c)
                f = (f and F_CARRY) or SZP_TABLE[a].toInt()
                pc++
            }

            0x79 -> {
                port.out(c, a)
                pc++
            }

            0x7A -> {
                adc16(sp)
                pc++
            }

            0x7B -> {
                sp = readMemWord(readMemWord(pc + 1))
                pc += 3
            }

            0xA0 -> {
                // (DE) <- (HL)
                writeMem(this.dE, readMem(this.hL))
                incDE()
                incHL()
                decBC()
                f = (f and 0xC1) or (if (this.bC != 0) F_PARITY else 0)
                pc++
            }

            0xA1 -> {
                temp = (f and F_CARRY) or F_NEGATIVE
                cp_a(readMem(this.hL)) // sets some flags
                incHL()
                decBC()

                temp = temp or (if (this.bC == 0) 0 else F_PARITY)

                f = (f and 0xF8) or temp
                pc++
            }

            0xA2 -> {
                temp = port.`in`(c)
                writeMem(this.hL, temp)
                b = dec8(b)
                incHL()
                if ((temp and 0x80) == 0x80) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
                pc++
            }

            0xA3 -> {
                temp = readMem(this.hL)
                // (C) <- (HL)
                port.out(c, temp)
                // HL <- HL + 1
                incHL()
                // B <- B -1
                b = dec8(b) // Flags in OUTI adjusted in same way as dec b anyway.
                if ((l + temp) > 255) {
                    f = f or F_CARRY
                    f = f or F_HALFCARRY
                } else {
                    f = f and F_CARRY.inv()
                    f = f and F_HALFCARRY.inv()
                }
                if ((temp and 0x80) == 0x80) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
                pc++
            }

            0xA8 -> {
                // (DE) <- (HL)
                writeMem(this.dE, readMem(this.hL))
                decDE()
                decHL()
                decBC()
                f = (f and 0xC1) or (if (this.bC != 0) F_PARITY else 0)
                pc++
            }

            0xA9 -> {
                temp = (f and F_CARRY) or F_NEGATIVE
                cp_a(readMem(this.hL)) // sets some flags
                decHL()
                decBC()

                temp = temp or (if (this.bC == 0) 0 else F_PARITY)

                f = (f and 0xF8) or temp
                pc++
            }

            0xAA -> {
                temp = port.`in`(c)
                writeMem(this.hL, temp)
                b = dec8(b)
                decHL()
                if ((temp and 0x80) != 0) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
                pc++
            }

            0xAB -> {
                temp = readMem(this.hL)
                // (C) <- (HL)
                port.out(c, temp)
                // HL <- HL - 1
                decHL()
                // B <- B -1
                b = dec8(b) // Flags in OUTI adjusted in same way as dec b anyway.

                if ((l + temp) > 255) {
                    f = f or F_CARRY
                    f = f or F_HALFCARRY
                } else {
                    f = f and F_CARRY.inv()
                    f = f and F_HALFCARRY.inv()
                }
                if ((temp and 0x80) == 0x80) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
                pc++
            }

            0xB0 -> {
                writeMem(this.dE, readMem(this.hL))
                incDE()
                incHL()
                decBC()

                if (this.bC != 0) {
                    f = f or F_PARITY
                    tstates -= 5
                    pc--
                } else {
                    f = f and F_PARITY.inv()
                    pc++
                }

                f = f and F_NEGATIVE.inv()
                f = f and F_HALFCARRY.inv()
            }

            0xB1 -> {
                temp = (f and F_CARRY) or F_NEGATIVE
                cp_a(readMem(this.hL)) // sets zero flag for us
                incHL()
                decBC()

                temp = temp or if (this.bC == 0) 0 else F_PARITY

                // Repeat instruction until a = (hl) or bc == 0
                if (((temp and F_PARITY) != 0) && ((f and F_ZERO) == 0)) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }

                f = (f and 0xF8) or temp // Sign set by the cp instruction
            }

            0xB2 -> {
                temp = port.`in`(c)
                writeMem(this.hL, temp)
                b = dec8(b)
                incHL()
                if (b != 0) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }

                if ((temp and 0x80) == 0x80) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
            }

            0xB3 -> {
                temp = readMem(this.hL)
                // (C) <- (HL)
                port.out(c, temp)
                // B <- B -1
                b = dec8(b)
                // HL <- HL + 1
                incHL()

                if (b != 0) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }
                if ((l + temp) > 255) {
                    f = f or F_CARRY
                    f = f or F_HALFCARRY
                } else {
                    f = f and F_CARRY.inv()
                    f = f and F_HALFCARRY.inv()
                }

                if ((temp and 0x80) != 0) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
            }

            0xB8 -> {
                writeMem(this.dE, readMem(this.hL))
                decDE()
                decHL()
                decBC()

                if (this.bC != 0) {
                    f = f or F_PARITY
                    tstates -= 5
                    pc--
                } else {
                    f = f and F_PARITY.inv()
                    pc++
                }

                f = f and F_NEGATIVE.inv()
                f = f and F_HALFCARRY.inv()
            }

            0xB9 -> {
                temp = (f and F_CARRY) or F_NEGATIVE
                cp_a(readMem(this.hL)) // sets zero flag for us
                decHL()
                decBC()

                temp = temp or (if (this.bC == 0) 0 else F_PARITY)

                // Repeat instruction until a = (hl) or bc == 0
                if (((temp and F_PARITY) != 0) && ((f and F_ZERO) == 0)) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }

                f = (f and 0xF8) or temp
            }

            0xBA -> {
                temp = port.`in`(c)
                writeMem(this.hL, temp)
                b = dec8(b)
                decHL()
                if (b != 0) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }

                if ((temp and 0x80) != 0) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
            }

            0xBB -> {
                temp = readMem(this.hL)
                // (C) <- (HL)
                port.out(c, temp)
                // B <- B -1
                b = dec8(b)
                // HL <- HL + 1
                decHL()

                if (b != 0) {
                    tstates -= 5
                    pc--
                } else {
                    pc++
                }
                if ((l + temp) > 255) {
                    f = f or F_CARRY
                    f = f or F_HALFCARRY
                } else {
                    f = f and F_CARRY.inv()
                    f = f and F_HALFCARRY.inv()
                }

                if ((temp and 0x80) != 0) f = f or F_NEGATIVE
                else f = f and F_NEGATIVE.inv()
            }
        } // end of switch
    } // end of ed ops


    /**
     * Pre-calculate DAA Table
     *
     * Address:
     *
     * Bottom 8 bytes = a value
     * Byte 9  = carry flag
     * Byte 10 = neative flag
     * Byte 11 = halfcarry flag
     *
     * Returned Value:
     *
     * a register stored in lower 8 bits
     * f register stored in higher 8 bits
     */
    private fun generateDAATable() {
        DAA_TABLE = ShortArray(0x800)


        // Iterate all possible values of a register (0 to 0xFF)
        run {
            var i = 256
            while (i-- != 0) {
                // Iterate carry / not-carry set
                for (c in 0..1) {
                    // Iterate halfcarry / not-halfcarry set
                    for (h in 0..1) {
                        // Iterate negative / not-negative set
                        for (n in 0..1) {
                            DAA_TABLE[(c shl 8) or (n shl 9) or (h shl 10) or i] =
                                getDAAResult(i, c or (n shl 1) or (h shl 4)).toShort()
                        }
                    }
                }
            }
        }


        // Reset these to be sure
        f = 0
        a = f
    }

    private fun getDAAResult(value: Int, flags: Int): Int {
        var flags = flags
        a = value
        f = flags

        val a_copy = a
        var correction = 0
        val carry = (flags and F_CARRY)
        var carry_copy = carry
        if (((flags and F_HALFCARRY) != 0) || ((a_copy and 0x0f) > 0x09)) {
            correction = correction or 0x06
        }
        if ((carry == 1) || (a_copy > 0x9f) || ((a_copy > 0x8f) && ((a_copy and 0x0f) > 0x09))) {
            correction = correction or 0x60
            carry_copy = 1
        }
        if (a_copy > 0x99) {
            carry_copy = 1
        }
        if ((flags and F_NEGATIVE) != 0) {
            // cycle -= 4;
            sub_a(correction)
        } else {
            // cycle -= 4;
            add_a(correction)
        }

        flags = (f and 0xfe) or carry_copy

        if (getParity(a)) {
            flags = (flags and 0xfb) or F_PARITY
        } else {
            flags = (flags and 0xfb)
        }

        return a or (flags shl 8)
    }

    // --------------------------------------------------------------------------------------------
    // ACCUMULATOR REGISTER
    // --------------------------------------------------------------------------------------------
    /**
     * ADD 8 BIT
     *
     * @param value        Value to add
     */
    private fun add_a(value: Int) {
        val temp = (a + value) and 0xff
        f = SZHVC_ADD_TABLE[(a shl 8) or temp].toInt()
        a = temp
    }

    /**
     * ADC 8 BIT - Add with carry
     *
     * @param value        Value to add
     */
    private fun adc_a(value: Int) {
        val carry = f and F_CARRY
        val temp = (a + value + carry) and 0xff
        f = SZHVC_ADD_TABLE[(carry shl 16) or (a shl 8) or temp].toInt()
        a = temp
    }

    /**
     * SUB 8 BIT
     *
     * @param value        Value to subtract
     */
    private fun sub_a(value: Int) {
        val temp = (a - value) and 0xff
        f = SZHVC_SUB_TABLE[(a shl 8) or temp].toInt()
        a = temp
    }

    /**
     * SBC 8 BIT
     *
     * @param value        Subtract with carry
     */
    private fun sbc_a(value: Int) {
        val carry = f and F_CARRY
        val temp = (a - value - carry) and 0xff
        f = SZHVC_SUB_TABLE[(carry shl 16) or (a shl 8) or temp].toInt()
        a = temp
    }


    /**
     * AND Operation
     *
     * @param value        Value to &
     */
    //private final void and_a(int value)
    //{
    //    f = SZP_TABLE[a &= value] | F_HALFCARRY;
    //}
    /**
     * OR Operation (bitwise inclusive OR to turn relevant bits on)
     *
     * @param value        Value to |
     */
    //private final void or_a(int value)
    //{
    //    f = SZP_TABLE[a |= value];
    //}
    /**
     * XOR Operation (Bitwise Exclusive OR)
     *
     * @param value        Value to ^
     */
    //private final void xor_a(int value)
    //{
    //    f = SZP_TABLE[a ^= value];
    //}
    /**
     * CP Operation - Compare with Accumulator
     *
     * @param value        Value to compare
     */
    private fun cp_a(value: Int) {
        // Subtract value from accumulator but discard result
        f = SZHVC_SUB_TABLE[(a shl 8) or ((a - value) and 0xff)].toInt()
    }

    /**
     * CPL Operation - Complement Accumulator
     *
     * Bit 3 and Bit incomplete
     */
    private fun cpl_a() {
        a = a xor 0xFF
        f = f or (F_NEGATIVE or F_HALFCARRY)
    }

    /**
     * RRA Operation - Rotate Right Accumulator
     */
    private fun rra_a() {
        val carry = a and 1 // bit 1 rotates to carry flag
        a = ((a shr 1) or ((f and F_CARRY) shl 7)) and 0xff // Shift Right One Bit Position
        f = (f and 0xec) or carry
    }

    /**
     * RLA Operation - Rotate Left Accumulator
     */
    private fun rla_a() {
        val carry = a shr 7 // bit 7 rotates to carry flag
        a = ((a shl 1) or (f and F_CARRY)) and 0xff
        f = (f and 0xec) or carry
    }

    /**
     * RLCA Operation - Rotate Left With Carry Accumulator
     */
    private fun rlca_a() {
        // Transfer Original Bit 7 to Bit 0 and Carry Flag
        val carry = a shr 7

        // Shift register left
        a = ((a shl 1) and 0xff) or carry


        // Retain Sign, Zero, Bit 5, Bit 3 and Parity
        f = (f and 0xec) or carry
    }


    /**
     * RRCA Operation - Rotate Right With Carry Accumulator
     */
    private fun rrca_a() {
        val carry = a and 1

        a = (a shr 1) or (carry shl 7)


        // Retain Sign, Zero, Bit 5, Bit 3 and Parity
        f = (f and 0xec) or carry
    }

    private var bC: Int
        // --------------------------------------------------------------------------------------------
        get() = (b shl 8) or c
        private set(value) {
            b = (value shr 8)
            c = value and 0xff
        }

    private var dE: Int
        get() = (d shl 8) or e
        private set(value) {
            d = (value shr 8)
            e = value and 0xff
        }

    private var hL: Int
        get() = (h shl 8) or l
        private set(value) {
            h = (value shr 8)
            l = value and 0xff
        }

    private var iX: Int
        get() = (ixH shl 8) or ixL
        private set(value) {
            ixH = (value shr 8)
            ixL = value and 0xff
        }

    private var iY: Int
        get() = (iyH shl 8) or iyL
        private set(value) {
            iyH = (value shr 8)
            iyL = value and 0xff
        }

    private fun incBC() {
        c = (c + 1) and 0xff
        if (c == 0) b = (b + 1) and 0xff
    }

    private fun incDE() {
        e = (e + 1) and 0xff
        if (e == 0) d = (d + 1) and 0xff
    }

    private fun incHL() {
        l = (l + 1) and 0xff
        if (l == 0) h = (h + 1) and 0xff
    }

    private fun incIX() {
        ixL = (ixL + 1) and 0xff
        if (ixL == 0) ixH = (ixH + 1) and 0xff
    }

    private fun incIY() {
        iyL = (iyL + 1) and 0xff
        if (iyL == 0) iyH = (iyH + 1) and 0xff
    }

    private fun decBC() {
        c = (c - 1) and 0xff
        if (c == 255) b = (b - 1) and 0xff
    }

    private fun decDE() {
        e = (e - 1) and 0xff
        if (e == 255) d = (d - 1) and 0xff
    }

    private fun decHL() {
        l = (l - 1) and 0xff
        if (l == 255) h = (h - 1) and 0xff
    }

    private fun decIX() {
        ixL = (ixL - 1) and 0xff
        if (ixL == 255) ixH = (ixH - 1) and 0xff
    }

    private fun decIY() {
        iyL = (iyL - 1) and 0xff
        if (iyL == 255) iyH = (iyH - 1) and 0xff
    }

    private fun inc8(value: Int): Int {
        var value = value
        value = (value + 1) and 0xff
        f = (f and F_CARRY) or SZHV_INC_TABLE[value].toInt()
        return value
    }

    private fun dec8(value: Int): Int {
        var value = value
        value = (value - 1) and 0xff
        f = (f and F_CARRY) or SZHV_DEC_TABLE[value].toInt()
        return value
    }

    // --------------------------------------------------------------------------------------------
    // EXCHANGE REGISTER BANKS
    // --------------------------------------------------------------------------------------------
    private fun exAF() {
        var temp = a
        a = a2
        a2 = temp
        temp = f
        f = f2
        f2 = temp
    }

    private fun exBC() {
        var temp = b
        b = b2
        b2 = temp
        temp = c
        c = c2
        c2 = temp
    }

    private fun exDE() {
        var temp = d
        d = d2
        d2 = temp
        temp = e
        e = e2
        e2 = temp
    }

    private fun exHL() {
        var temp = h
        h = h2
        h2 = temp
        temp = l
        l = l2
        l2 = temp
    }


    private fun add16(reg: Int, value: Int): Int {
        val result = reg + value
        f = (f and 0xc4) or (((reg xor result xor value) shr 8) and 0x10) or ((result shr 16) and 1)
        return (result and 0xffff)
    }

    /**
     * Add with carry (16-bit)
     *
     * Only ever affects HL register
     *
     * @param value
     */
    private fun adc16(value: Int) {
        val hl = (h shl 8) or l

        val result = hl + value + (f and F_CARRY)
        f =
            (((hl xor result xor value) shr 8) and 0x10) or ((result shr 16) and 1) or ((result shr 8) and 0x80) or (if ((result and 0xffff) != 0) 0 else 0x40) or (((value xor hl xor 0x8000) and (value xor result) and 0x8000) shr 13)
        h = (result shr 8) and 0xff
        l = result and 0xff
    }

    /**
     * Subtract with carry (16-bit)
     *
     * Only ever affects HL register
     *
     * @param value
     */
    private fun sbc16(value: Int) {
        val hl = (h shl 8) or l

        val result = hl - value - (f and F_CARRY)
        f =
            (((hl xor result xor value) shr 8) and 0x10) or 0x02 or ((result shr 16) and 1) or ((result shr 8) and 0x80) or (if ((result and 0xffff) != 0) 0 else 0x40) or (((value xor hl) and (hl xor result) and 0x8000) shr 13)
        h = (result shr 8) and 0xff
        l = result and 0xff
    }


    /**
     * Increment Refresh register
     */
    private fun incR() {
        r = (r and 0x80) or ((r + 1) and 0x7F)
    }

    // --------------------------------------------------------------------------------------------
    // FLAG REGISTER
    // --------------------------------------------------------------------------------------------
    /**
     * Generate flag tables
     *
     * Based on code from the Java Emulation Framework
     * Copyright (C) 2002 Erik Duijs (erikduijs@yahoo.com)
     */
    private fun generateFlagTables() {
        SZ_TABLE = IntArray(256)
        SZP_TABLE = IntArray(256)
        SZHV_INC_TABLE = IntArray(256)
        SZHV_DEC_TABLE = IntArray(256)
        SZ_BIT_TABLE = IntArray(256)

        // Generate tables
        for (i in 0..255) {
            // Sign bits (0x80)
            val sf = (if ((i and 0x80) != 0) F_SIGN else 0)


            // Zero bits (0x40)
            val zf = (if (i == 0) F_ZERO else 0)


            // Bit 5 (0x20)
            val yf = i and 0x20


            // Halfcarry (0x10)
            //int hf = 0;

            // Bit 3 (0x08)
            val xf = i and 0x08


            // Overflow (0x04)
            //int vf = 0;

            // Parity bits (0x04)
            val pf = (if (getParity(i)) F_PARITY else 0)


            // Generate Sign/Zero Table
            SZ_TABLE[i] = (sf or zf or yf or xf)


            // Generate Sign/Zero/Parity Table
            SZP_TABLE[i] = (sf or zf or yf or xf or pf)


            // Generate table for inc8 instruction
            SZHV_INC_TABLE[i] = (sf or zf or yf or xf)
            SZHV_INC_TABLE[i] = SZHV_INC_TABLE[i].toInt() or if (i == 0x80) F_OVERFLOW else 0
            SZHV_INC_TABLE[i] = SZHV_INC_TABLE[i].toInt() or if ((i and 0x0f) == 0x00) F_HALFCARRY else 0


            // Generate table for dec8 instruction
            SZHV_DEC_TABLE[i] = (sf or zf or yf or xf or F_NEGATIVE)
            SZHV_DEC_TABLE[i] = SZHV_DEC_TABLE[i].toInt() or if (i == 0x7F) F_OVERFLOW else 0
            SZHV_DEC_TABLE[i] = SZHV_DEC_TABLE[i].toInt() or if ((i and 0x0f) == 0x0F) F_HALFCARRY else 0


            // Generate table for bit instruction (set sign flag on here)
            SZ_BIT_TABLE[i] = (if (i != 0) i and 0x80 else F_ZERO or F_PARITY)
            SZ_BIT_TABLE[i] =
                SZ_BIT_TABLE[i].toInt() or (yf or xf or F_HALFCARRY) // halfcarry is always on with bit instruction :)
        }


        // ----------------------------------------------------------------------------------------
        // Generate fast lookups for ADD/SUB/ADC/SBC instructions
        // ----------------------------------------------------------------------------------------
        SZHVC_ADD_TABLE = IntArray(2 * 256 * 256)
        SZHVC_SUB_TABLE = IntArray(2 * 256 * 256)

        var padd = 0 * 256
        var padc = 256 * 256
        var psub = 0 * 256
        var psbc = 256 * 256

        for (oldval in 0..255) {
            for (newval in 0..255) {
                /* add or adc w/o carry set */
                var `val` = newval - oldval

                if (newval != 0) {
                    if ((newval and 0x80) != 0) {
                        SZHVC_ADD_TABLE[padd] = F_SIGN
                    } else {
                        SZHVC_ADD_TABLE[padd] = 0
                    }
                } else {
                    SZHVC_ADD_TABLE[padd] = F_ZERO
                }

                SZHVC_ADD_TABLE[padd] =
                    SZHVC_ADD_TABLE[padd].toInt() or (newval and (F_BIT5 or F_BIT3)) /* undocumented flag bits 5+3 */

                if ((newval and 0x0f) < (oldval and 0x0f)) {
                    SZHVC_ADD_TABLE[padd] = SZHVC_ADD_TABLE[padd].toInt() or F_HALFCARRY
                }
                if (newval < oldval) {
                    SZHVC_ADD_TABLE[padd] = SZHVC_ADD_TABLE[padd].toInt() or F_CARRY
                }
                if (((`val` xor oldval xor 0x80) and (`val` xor newval) and 0x80) != 0) {
                    SZHVC_ADD_TABLE[padd] = SZHVC_ADD_TABLE[padd].toInt() or F_OVERFLOW
                }
                padd++

                /* adc with carry set */
                `val` = newval - oldval - 1
                if (newval != 0) {
                    if ((newval and 0x80) != 0) {
                        SZHVC_ADD_TABLE[padc] = F_SIGN
                    } else {
                        SZHVC_ADD_TABLE[padc] = 0
                    }
                } else {
                    SZHVC_ADD_TABLE[padc] = F_ZERO
                }

                SZHVC_ADD_TABLE[padc] =
                    SZHVC_ADD_TABLE[padc].toInt() or (newval and (F_BIT5 or F_BIT3)) /* undocumented flag bits 5+3 */
                if ((newval and 0x0f) <= (oldval and 0x0f)) {
                    SZHVC_ADD_TABLE[padc] = SZHVC_ADD_TABLE[padc].toInt() or F_HALFCARRY
                }
                if (newval <= oldval) {
                    SZHVC_ADD_TABLE[padc] = SZHVC_ADD_TABLE[padc].toInt() or F_CARRY
                }
                if (((`val` xor oldval xor 0x80) and (`val` xor newval) and 0x80) != 0) {
                    SZHVC_ADD_TABLE[padc] = SZHVC_ADD_TABLE[padc].toInt() or F_OVERFLOW
                }
                padc++

                /* cp, sub or sbc w/o carry set */
                `val` = oldval - newval
                if (newval != 0) {
                    if ((newval and 0x80) != 0) {
                        SZHVC_SUB_TABLE[psub] = (F_NEGATIVE or F_SIGN)
                    } else {
                        SZHVC_SUB_TABLE[psub] = F_NEGATIVE
                    }
                } else {
                    SZHVC_SUB_TABLE[psub] = (F_NEGATIVE or F_ZERO)
                }

                SZHVC_SUB_TABLE[psub] =
                    SZHVC_SUB_TABLE[psub].toInt() or (newval and (F_BIT5 or F_BIT3)) /* undocumented flag bits 5+3 */
                if ((newval and 0x0f) > (oldval and 0x0f)) {
                    SZHVC_SUB_TABLE[psub] = SZHVC_SUB_TABLE[psub].toInt() or F_HALFCARRY
                }
                if (newval > oldval) {
                    SZHVC_SUB_TABLE[psub] = SZHVC_SUB_TABLE[psub].toInt() or F_CARRY
                }
                if (((`val` xor oldval) and (oldval xor newval) and 0x80) != 0) {
                    SZHVC_SUB_TABLE[psub] = SZHVC_SUB_TABLE[psub].toInt() or F_OVERFLOW
                }
                psub++

                /* sbc with carry set */
                `val` = oldval - newval - 1
                if (newval != 0) {
                    if ((newval and 0x80) != 0) {
                        SZHVC_SUB_TABLE[psbc] = (F_NEGATIVE or F_SIGN)
                    } else {
                        SZHVC_SUB_TABLE[psbc] = F_NEGATIVE
                    }
                } else {
                    SZHVC_SUB_TABLE[psbc] = (F_NEGATIVE or F_ZERO)
                }

                SZHVC_SUB_TABLE[psbc] =
                    SZHVC_SUB_TABLE[psbc].toInt() or (newval and (F_BIT5 or F_BIT3)) /* undocumented flag bits 5+3 */
                if ((newval and 0x0f) >= (oldval and 0x0f)) {
                    SZHVC_SUB_TABLE[psbc] = SZHVC_SUB_TABLE[psbc].toInt() or F_HALFCARRY
                }
                if (newval >= oldval) {
                    SZHVC_SUB_TABLE[psbc] = SZHVC_SUB_TABLE[psbc].toInt() or F_CARRY
                }
                if (((`val` xor oldval) and (oldval xor newval) and 0x80) != 0) {
                    SZHVC_SUB_TABLE[psbc] = SZHVC_SUB_TABLE[psbc].toInt() or F_OVERFLOW
                }
                psbc++
            }
        }
    }

    /**
     * Return the parity of a number.
     * Only used for pre-calculations.
     *
     * @param   value
     * @return  true if parity
     */
    private fun getParity(value: Int): Boolean {
        var parity = true
        for (j in 0..7) {
            if ((value and (1 shl j)) != 0) {
                parity = !parity
            }
        }
        return parity
    }

    // --------------------------------------------------------------------------------------------
    // MEMORY ACCESS
    //
    // Simply moved here for speed purposes
    // --------------------------------------------------------------------------------------------
    /** Cartridge ROM pages  */
    private var rom: Array<ByteArray?>? = Array(16, { ByteArray(16) })

    /** RAM  */
    var ram: Array<ByteArray>  = Array(16, { ByteArray(16) })

    /** SRAM  */
    var sram: Array<ByteArray?> = Array(16, { ByteArray(16) })

    /** Catridge uses SRAM  */
    private var useSRAM = false

    /** Memory frame registers  */
    var frameReg: IntArray = IntArray(4)

    /** Total number of 16K catridge pages  */
    private var number_of_pages = 0

    /** Memory map  */
    private var memWriteMap: Array<ByteArray?> = Array(16, { ByteArray(16) })
    private var memReadMap: Array<ByteArray?> = Array(16, { ByteArray(16) })

    /** Dummy memory writes (never read)  */
    private var dummyWrite: ByteArray? = ByteArray(16)


    /**
     * Memory Constructor.
     */
    private fun generateMemory() {
        // Create read/write memory map (64 positions each representing 1K)

        // Note we create one extra dummy position to get around a dodgy write in
        // Back to the Future 2.

        memReadMap = arrayOfNulls<ByteArray>(65)
        memWriteMap = arrayOfNulls<ByteArray>(65)


        // Create 8K System RAM
        ram = Array<ByteArray>(8) { ByteArray(Setup.PAGE_SIZE) }


        // Create 2 x 16K RAM Cartridge Pages
        if (sram == null) {
            sram = Array<ByteArray?>(32) { ByteArray(Setup.PAGE_SIZE) }
            useSRAM = false
        }


        // Create dummy memory (for invalid writes)
        dummyWrite = ByteArray(Setup.PAGE_SIZE)


        // Ignore bad writes in Back To The Future 2
        memReadMap[64] = dummyWrite
        memWriteMap[64] = dummyWrite

        number_of_pages = 2
    }

    /**
     * Reset Memory to Default Values.
     */
    fun resetMemory(p: Array<ByteArray?>?) {
        if (p != null) rom = p

        frameReg[0] = 0
        frameReg[1] = 0
        frameReg[2] = 1
        frameReg[3] = 0


        // Default Mapping
        if (rom != null) {
            // 16K Page Chunks :)
            number_of_pages = rom!!.size / 16
            setDefaultMemoryMapping()
        } else number_of_pages = 0
    }

    private fun setDefaultMemoryMapping() {
        // Map ROM
        i = 0
        while (i < 48) {
            memReadMap[i] = rom!![i and 31]
            memWriteMap[i] = dummyWrite
            i++
        }

        // Map RAM
        i = 48
        while (i < 64) {
            memReadMap[i] = ram[i and 7]
            memWriteMap[i] = ram[i and 7]
            i++
        }
    }

    /**
     * Write to a memory location.
     *
     * @param address      Memory address
     * @param value        Value to write
     */
    private fun writeMem(address: Int, value: Int) {
        memWriteMap[address shr 10]!![address and 0x3FF] = value.toByte()


        // Paging registers
        if (address >= 0xFFFC) page(address and 3, value)
    }


    private fun readMem(address: Int): Int {
        return memReadMap[address shr 10]!![address and 0x3FF].toInt() and 0xFF
    }

    private fun d(): Int {
        return memReadMap[pc shr 10]!![pc and 0x3FF].toInt()
    }


    private fun readMemWord(address: Int): Int {
        var address = address
        return (memReadMap[address shr 10]!![address and 0x3FF].toInt() and 0xFF) or
                ((memReadMap[++address shr 10]!![address and 0x3FF].toInt() and 0xFF) shl 8)
    }

    private fun page(address: Int, value: Int) {
        frameReg[address] = value

        when (address) {
            0 ->                 // 1= SRAM mapped to $8000-$BFFF
                if ((value and 0x08) != 0) {
                    // SRAM banking; BA14 state when $8000-$BFFF is accessed (1= high, 0= low)

                    // 16K offset into SRAM

                    var offset = (value and 0x04) shl 2


                    // Map 16K of SRAM
                    run {
                        var i = 32
                        while (i < 48) {
                            memWriteMap[i] = sram!![offset++]
                            memReadMap[i] = memWriteMap[i]
                            i++
                        }
                    }

                    useSRAM = true
                } else {
                    var p = (frameReg[3] % number_of_pages) shl 4


                    // Map 16K of ROM
                    run {
                        var i = 32
                        while (i < 48) {
                            memReadMap[i] = rom!![p++]
                            memWriteMap[i] = dummyWrite
                            i++
                        }
                    }
                }

            1 -> {
                // Note +1 here, because for loop starts at '1'
                var p = ((value % number_of_pages) shl 4) + 1

                run {
                    var i = 1
                    while (i < 16) {
                        memReadMap[i] = rom!![p++]
                        i++
                    }
                }
            }

            2 -> {
                var p = (value % number_of_pages) shl 4

                run {
                    var i = 16
                    while (i < 32) {
                        memReadMap[i] = rom!![p++]
                        i++
                    }
                }
            }

            3 ->                 // Map ROM
                if ((frameReg[0] and 0x08) == 0) {
                    var p = (value % number_of_pages) shl 4

                    run {
                        var i = 32
                        while (i < 48) {
                            memReadMap[i] = rom!![p++]
                            i++
                        }
                    }
                }
        }
    }

    fun hasUsedSRAM(): Boolean {
        return useSRAM
    }

    fun setSRAM(bytes: ByteArray) {
        val length = bytes.size / Setup.PAGE_SIZE

        for (i in 0 until length) {
            bytes.copyInto(
                destination = sram[i]!!,
                destinationOffset = 0,
                startIndex = i * Setup.PAGE_SIZE,
                endIndex = i * Setup.PAGE_SIZE + Setup.PAGE_SIZE
            )
        }
    }


    /**
     * Called when restoring from a saved state
     *
     * @param state     Contents of frame register
     */
    fun setStateMem(state: IntArray) {
        frameReg = state

        setDefaultMemoryMapping()

        page(3, frameReg[3])
        page(2, frameReg[2])
        page(1, frameReg[1])
        page(0, frameReg[0])
    }

    //   --------------------------------------------------------------------------------------------
    /**
     * Z80 Constructor.
     *
     * @param port    Pointer to Z80's Ports
     */
    init {
        // Generate flag lookups
        generateFlagTables()


        // Pre-calculate results for DAA instruction
        generateDAATable()


        // Generate memory arrays
        generateMemory()
    }


    var state: IntArray
        get() {
            val state: IntArray? = IntArray(STATE_LENGTH)

            state!![0] = pc or (sp shl 16)
            state[1] =
                (if (iff1) 0x01 else 0) or (if (iff2) 0x02 else 0) or (if (halt) 0x04 else 0) or (if (EI_inst) 0x08 else 0) or (if (interruptLine) 0x10 else 0)
            state[2] = a or (a2 shl 8) or (f shl 16) or (f2 shl 24) // AF AF'
            state[3] = this.bC or (this.dE shl 16) // BC DE
            state[4] = this.hL or (r shl 16) or (i shl 24) // HL, r, i
            state[5] = this.iX or (this.iY shl 16) // IX, IY

            exBC()
            exDE()
            exHL() // swap registers

            state[6] = this.bC or (this.dE shl 16) // BC' DE'
            state[7] = this.hL or (im shl 16) or (interruptVector shl 24) // HL' and interrupt mode

            exBC()
            exDE()
            exHL() // restore registers

            return state
        }
        set(state) {
            var temp = state[0]
            pc = temp and 0xFFFF
            sp = (temp shr 16) and 0xFFFF

            temp = state[1]
            iff1 = (temp and 0x01) != 0
            iff2 = (temp and 0x02) != 0
            halt = (temp and 0x04) != 0
            EI_inst = (temp and 0x08) != 0
            interruptLine = (temp and 0x10) != 0

            temp = state[2]
            a = temp and 0xFF
            a2 = (temp shr 8) and 0xFF
            f = (temp shr 16) and 0xFF
            f2 = (temp shr 24) and 0xFF

            temp = state[3]
            this.bC = temp and 0xFFFF
            this.dE = (temp shr 16) and 0xFFFF

            temp = state[4]
            this.hL = temp and 0xFFFF
            r = (temp shr 16) and 0xFF
            i = (temp shr 24) and 0xFF

            temp = state[5]
            this.iX = temp and 0xFFFF
            this.iY = (temp shr 16) and 0xFFFF

            exBC()
            exDE()
            exHL() // swap registers

            temp = state[6]
            this.bC = temp and 0xFFFF
            this.dE = (temp shr 16) and 0xFFFF

            temp = state[7]
            this.hL = temp and 0xFFFF
            im = (temp shr 16) and 0xFF
            interruptVector = (temp shr 24) and 0xFF

            exBC()
            exDE()
            exHL() // restore registers
        }

    companion object {
        /** Speedup hack to set tstates to '0' on halt instruction  */
        private const val HALT_SPEEDUP = true

        /** Interrupt Line Status  */
        var interruptLine: Boolean = false

        /** Interrupt Vector  */
        var interruptVector: Int = 0

        // --------------------------------------------------------------------------------------------
        // Flag Register
        // --------------------------------------------------------------------------------------------
        /** Flag Register  */
        private var f = 0
        private var f2 = 0

        /** carry (set when a standard carry occurred)  */
        private const val F_CARRY = 0x01

        /** negative (set when instruction is subtraction, clear when addition)  */
        private const val F_NEGATIVE = 0x02

        /** true indicates even parity in the result, false for 2s complement sign overflow  */
        private const val F_PARITY = 0x04

        /** true indicates even parity in the result, false for 2s complement sign overflow  */
        private const val F_OVERFLOW = 0x04

        /** bit3 (usually a copy of bit 3 of the result)  */
        private const val F_BIT3 = 0x08

        /** half carry (set when a carry occured between bit 3 / 4 of result - used for BCD  */
        private const val F_HALFCARRY = 0x10

        /** bit5 (usually a copy of bit 5 of the result)  */
        private const val F_BIT5 = 0x20

        /** zero (set when a result is zero)  */
        private const val F_ZERO = 0x40

        /** sign (set when a result is negative)  */
        private const val F_SIGN = 0x80


        // --------------------------------------------------------------------------------------------
        // Opcode timings
        // --------------------------------------------------------------------------------------------
        /** Total number of cycles we're executing for  */
        private var totalCycles = 0

        /** TStates remaining  */
        var tstates: Int = 0

        private val OP_STATES: ShortArray? =
            shortArrayOf( /*          0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */ /* 0x00 */4,
                10,
                7,
                6,
                4,
                4,
                7,
                4,
                4,
                11,
                7,
                6,
                4,
                4,
                7,
                4,  /* 0x10 */
                8,
                10,
                7,
                6,
                4,
                4,
                7,
                4,
                12,
                11,
                7,
                6,
                4,
                4,
                7,
                4,  /* 0x20 */
                7,
                10,
                16,
                6,
                4,
                4,
                7,
                4,
                7,
                11,
                16,
                6,
                4,
                4,
                7,
                4,  /* 0x30 */
                7,
                10,
                13,
                6,
                11,
                11,
                10,
                4,
                7,
                11,
                13,
                6,
                4,
                4,
                7,
                4,  /* 0x40 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0x50 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0x60 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0x70 */
                7,
                7,
                7,
                7,
                7,
                7,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0x80 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0x90 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0xA0 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0xB0 */
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                7,
                4,  /* 0xC0 */
                5,
                10,
                10,
                10,
                10,
                11,
                7,
                11,
                5,
                10,
                10,
                0,
                10,
                17,
                7,
                11,  /* 0xD0 */
                5,
                10,
                10,
                11,
                10,
                11,
                7,
                11,
                5,
                4,
                10,
                11,
                10,
                0,
                7,
                11,  /* 0xE0 */
                5,
                10,
                10,
                19,
                10,
                11,
                7,
                11,
                5,
                4,
                10,
                4,
                10,
                0,
                7,
                11,  /* 0xF0 */
                5,
                10,
                10,
                4,
                10,
                11,
                7,
                11,
                5,
                6,
                10,
                4,
                10,
                0,
                7,
                11
            )

        private val OP_CB_STATES: ShortArray? =
            shortArrayOf( /*          0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */ /* 0x00 */8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0x10 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0x20 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0x30 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0x40 */
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,  /* 0x50 */
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,  /* 0x60 */
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,  /* 0x70 */
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                12,
                8,  /* 0x80 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0x90 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xA0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xB0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xC0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xD0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xE0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,  /* 0xF0 */
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                15,
                8
            )

        private val OP_DD_STATES: ShortArray? =
            shortArrayOf( /*          0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */ /* 0x00 */4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                15,
                4,
                4,
                4,
                4,
                4,
                4,  /* 0x10 */
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                15,
                4,
                4,
                4,
                4,
                4,
                4,  /* 0x20 */
                4,
                14,
                20,
                10,
                8,
                8,
                11,
                4,
                4,
                15,
                20,
                10,
                8,
                8,
                11,
                4,  /* 0x30 */
                4,
                4,
                4,
                4,
                23,
                23,
                19,
                4,
                4,
                15,
                4,
                4,
                4,
                4,
                4,
                4,  /* 0x40 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0x50 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0x60 */
                8,
                8,
                8,
                8,
                8,
                8,
                19,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                19,
                8,  /* 0x70 */
                19,
                19,
                19,
                19,
                19,
                19,
                4,
                19,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0x80 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0x90 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0xA0 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0xB0 */
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,
                4,
                4,
                4,
                4,
                8,
                8,
                19,
                4,  /* 0xC0 */
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                0,
                4,
                4,
                4,
                4,  /* 0xD0 */
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,  /* 0xE0 */
                4,
                14,
                4,
                23,
                4,
                15,
                4,
                4,
                4,
                8,
                4,
                4,
                4,
                4,
                4,
                4,  /* 0xF0 */
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                4,
                10,
                4,
                4,
                4,
                4,
                4,
                4
            )

        private val OP_INDEX_CB_STATES: ShortArray? =
            shortArrayOf( /*          0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */ /* 0x00 */0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0x10 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0x20 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0x30 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0x40 */
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,  /* 0x50 */
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,  /* 0x60 */
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,  /* 0x70 */
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                20,
                0,  /* 0x80 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0x90 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xA0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xB0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xC0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xD0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xE0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,  /* 0xF0 */
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                23,
                0
            )

        private val OP_ED_STATES: ShortArray? =
            shortArrayOf( /*          0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */ /* 0x00 */8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0x10 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0x20 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0x30 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0x40 */
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                9,
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                9,  /* 0x50 */
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                9,
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                9,  /* 0x60 */
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                18,
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                18,  /* 0x70 */
                8,
                12,
                15,
                20,
                8,
                14,
                8,
                8,
                12,
                12,
                15,
                20,
                8,
                14,
                8,
                8,  /* 0x80 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0x90 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0xA0 */
                16,
                16,
                16,
                16,
                8,
                8,
                8,
                8,
                16,
                16,
                16,
                16,
                8,
                8,
                8,
                8,  /* 0xB0 */
                16,
                16,
                16,
                16,
                8,
                8,
                8,
                8,
                16,
                16,
                16,
                16,
                8,
                8,
                8,
                8,  /* 0xC0 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0xD0 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0xE0 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,  /* 0xF0 */
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8,
                8
            )

        // --------------------------------------------------------------------------------------------
        // Precalculated tables for speed purposes
        // --------------------------------------------------------------------------------------------
        /** Pre-calculated result for DAA instruction  */
        private var DAA_TABLE: ShortArray = ShortArray(16)

        /** Sign, Zero table  */
        private var SZ_TABLE: IntArray = IntArray(16)

        /** Sign, Zero, Parity table  */
        private var SZP_TABLE: IntArray = IntArray(16)

        /** Flag lookup table for inc8 instruction  */
        private var SZHV_INC_TABLE: IntArray = IntArray(16)

        /** Flag lookup table for dec8 instruction  */
        private var SZHV_DEC_TABLE: IntArray = IntArray(16)

        /** Flag lookup table for add/adc instruction  */
        private var SZHVC_ADD_TABLE: IntArray = IntArray(16)

        /** Flag lookup table for dec/sbc instruction  */
        private var SZHVC_SUB_TABLE: IntArray = IntArray(16)

        /** Flag lookup table for bit instruction  */
        private var SZ_BIT_TABLE: IntArray = IntArray(16)

        // --------------------------------------------------------------------------------------------
        // Misc Helper Stuff
        // --------------------------------------------------------------------------------------------
        /** Easy bit reference for CB operations  */
        private const val BIT_0 = 0x01
        private const val BIT_1 = 0x02
        private const val BIT_2 = 0x04
        private const val BIT_3 = 0x08
        private const val BIT_4 = 0x10
        private const val BIT_5 = 0x20
        private const val BIT_6 = 0x40
        private const val BIT_7 = 0x80


        val cycle: Int
            /**
             * Get current cycle number
             *
             * @return  Cycle number
             */
            get() = totalCycles - tstates

        // --------------------------------------------------------------------------------------------
        // Z80 State Saving
        // --------------------------------------------------------------------------------------------
        /** Length of state array  */
        const val STATE_LENGTH: Int = 8
    }
}
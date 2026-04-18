package io.duckemu.sms.domain

class SN76489 {
    /** SN76489 Internal Clock Speed (Hz) [SCALED]  */
    private var clock = 0

    /** Stores fractional part of clock for various precise updates [SCALED]  */
    private var clockFrac = 0

    // --------------------------------------------------------------------------------------------
    // The SN76489 has 8 "registers":
    // 4 x 4 bit volume registers,
    // 3 x 10 bit tone registers and
    // 1 x 3 bit noise register.
    // --------------------------------------------------------------------------------------------
    /** SN76489 Registers  */
    private val reg: IntArray

    /** Register Latch  */
    private var regLatch = 0

    /** Channel Counters (10-bits on original hardware)  */
    private val freqCounter: IntArray

    /** Polarity of Tone Channel Counters  */
    private val freqPolarity: IntArray

    /** Position of Tone Amplitude Changes  */
    private val freqPos: IntArray

    /** Noise Generator Frequency  */
    private var noiseFreq = 0

    /** The Linear Feedback Shift Register (16-bits on original hardware)  */
    private var noiseShiftReg = 0

    // --------------------------------------------------------------------------------------------
    // Output & Amplification
    // --------------------------------------------------------------------------------------------
    /** Output channels  */
    private val outputChannel: IntArray

    /**
     * SN76489 Constructor.
     */
    init {
        // Create various arrays
        outputChannel = IntArray(4)
        reg = IntArray(8)
        freqCounter = IntArray(4)
        freqPolarity = IntArray(4)
        freqPos = IntArray(3)
    }


    /**
     * Init SN76496 to Default Values.
     *
     * @param clockSpeed    Clock Speed (Hz)
     * @param sampleRate    Sample Rate (Hz)
     */
    fun init(clockSpeed: Int, sampleRate: Int) {
        // Master clock divided by 16 to get internal clock
        // e.g. 3579545 / 16 / 44100 = 5
        clock = (clockSpeed shl SCALE) / 16 / sampleRate

        regLatch = 0
        clockFrac = 0
        noiseShiftReg = SHIFT_RESET
        noiseFreq = 0x10

        for (i in 0..3) {
            // Set Tone Frequency (Don't want this to be zero)
            reg[i shl 1] = 1


            // Set Volume Off
            reg[(i shl 1) + 1] = 0x0F


            // Set Frequency Counters
            freqCounter[i] = 0


            // Set Amplitudes Positive
            freqPolarity[i] = 1


            // Do not use intermediate positions
            if (i != 3) freqPos[i] = NO_ANTIALIAS
        }
    }

    /**
     * Program the SN76489.
     *
     * @param  value   Value to write (0-0xFF)
     */
    fun write(value: Int) {
        // ----------------------------------------------------------------------------------------
        // If bit 7 is 1 then the byte is a LATCH/DATA byte.
        //    %1cctdddd
        //    |||````-- Data
        //    ||`------ Type
        //    ``------- Channel
        // ----------------------------------------------------------------------------------------

        if ((value and 0x80) != 0) {
            // Bits 6 and 5 ("cc") give the channel to be latched, ALWAYS.
            // Bit 4 ("t") determines whether to latch volume (1) or tone/noise (0) data -
            // this gives the column.

            regLatch = (value shr 4) and 7


            reg[regLatch] = (reg[regLatch] and 0x3F0) or (value and 0x0F)
        } else {
            // TONE REGISTERS
            // If the currently latched register is a tone register then the low 6
            // bits of the byte are placed into the high 6 bits of the latched register.
            if (regLatch == 0 || regLatch == 2 || regLatch == 4) {
                // ddddDDDDDD (10 bits total) - keep lower 4 bits and replace upper 6 bits.
                // ddddDDDDDD gives the 10-bit half-wave counter reset value.
                reg[regLatch] = (reg[regLatch] and 0x0F) or ((value and 0x3F) shl 4)
            } else {
                reg[regLatch] = value and 0x0F
            }
        }

        when (regLatch) {
            0, 2, 4 -> if (reg[regLatch] == 0) reg[regLatch] = 1
            6 -> {
                noiseFreq = 0x10 shl (reg[6] and 3)
                noiseShiftReg = SHIFT_RESET
            }
        }
    }

    fun update(buffer: ByteArray, offset: Int, samplesToGenerate: Int) {
        for (sample in 0..<samplesToGenerate) {
            for (i in 0..2) {
                if (freqPos[i] != NO_ANTIALIAS) outputChannel[i] =
                    (PSG_VOLUME[reg[(i shl 1) + 1]] * freqPos[i]) shr SCALE
                else outputChannel[i] = PSG_VOLUME[reg[(i shl 1) + 1]] * freqPolarity[i]
            }

            outputChannel[3] = PSG_VOLUME[reg[7]] * (noiseShiftReg and 1) shl 1 // Double output

            var output = outputChannel[0] + outputChannel[1] + outputChannel[2] + outputChannel[3]

            if (output > 0x7F) output = 0x7F
            else if (output < -0x80) output = -0x80

            buffer[offset + sample] = output.toByte()

            clockFrac += clock

            val clockCycles = clockFrac shr SCALE
            val clockCyclesScaled = clockCycles shl SCALE

            clockFrac -= clockCyclesScaled
            freqCounter[0] -= clockCycles
            freqCounter[1] -= clockCycles
            freqCounter[2] -= clockCycles

            if (noiseFreq == 0x80) freqCounter[3] = freqCounter[2]
            else freqCounter[3] -= clockCycles

            for (i in 0..2) {
                val counter = freqCounter[i]

                if (counter <= 0) {
                    val tone = reg[i shl 1]

                    if (tone > 6) {
                        freqPos[i] = ((clockCyclesScaled - clockFrac + (2 shl SCALE) * counter) shl SCALE) *
                                freqPolarity[i] / (clockCyclesScaled + clockFrac)


                        freqPolarity[i] = -freqPolarity[i]
                    } else {
                        freqPolarity[i] = 1
                        freqPos[i] = NO_ANTIALIAS
                    }

                    freqCounter[i] += tone * (clockCycles / tone + 1)
                } else {
                    freqPos[i] = NO_ANTIALIAS
                }
            }


            if (freqCounter[3] <= 0) {
                freqPolarity[3] = -freqPolarity[3]

                if (noiseFreq != 0x80) freqCounter[3] += noiseFreq * (clockCycles / noiseFreq + 1)

                if (freqPolarity[3] == 1) {
                    val feedback: Int


                    if ((reg[6] and 0x04) != 0) {
                        feedback = if ((noiseShiftReg and FEEDBACK_PATTERN) != 0 &&
                            ((noiseShiftReg and FEEDBACK_PATTERN) xor FEEDBACK_PATTERN) != 0
                        )
                            1
                        else
                            0
                    } else {
                        feedback = noiseShiftReg and 1
                    }

                    noiseShiftReg = (noiseShiftReg shr 1) or (feedback shl 15)
                }
            }
        }
    }

    companion object {
        private const val SCALE = 8
        private const val NO_ANTIALIAS = Int.Companion.MIN_VALUE
        private const val SHIFT_RESET = 0x8000
        private const val FEEDBACK_PATTERN = 0x9
        private val PSG_VOLUME = intArrayOf(
            25, 20, 16, 13, 10, 8, 6, 5, 4, 3, 3, 2, 2, 1, 1, 0
        )
    }
}
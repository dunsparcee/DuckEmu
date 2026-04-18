package io.duckemu.sms.domain

import io.duckemu.sms.data.EmuState
import io.duckemu.sms.data.EmuState.controller1
import io.duckemu.sms.data.EmuState.controller2
import io.duckemu.sms.data.EmuState.cyclesPerLine
import io.duckemu.sms.data.EmuState.ggstart
import io.duckemu.sms.data.EmuState.is_gg
import io.duckemu.sms.data.EmuState.lightgunClick
import io.duckemu.sms.data.EmuState.lightgunX
import io.duckemu.sms.data.EmuState.lightgunY
import io.duckemu.sms.data.EmuState.soundEnabled
import io.duckemu.sms.data.Setup.LIGHTGUN


class Ports
/**
 * Ports Constructor.
 *
 * @param vdp Vdp
 */(
    /**
     * Reference to VDP
     */
    private val vdp: Vdp,
    /**
     * Reference to PSG
     */
    private val psg: SN76489
) {
    /**
     * Horizontal Counter Latch
     */
    private var hCounter = 0

    /**
     * I/O Ports A and B * (5 ints each)
     */
    var ioPorts: IntArray = IntArray(10)

    fun reset() {
        if (LIGHTGUN) {
            ioPorts = IntArray(10)
            ioPorts[PORT_A + IO_TH_INPUT] = 1
            ioPorts[PORT_B + IO_TH_INPUT] = 1
        } else {
            ioPorts = IntArray(2)
        }
    }


    fun out(port: Int, value: Int) {
        // Game Gear Serial Ports (do nothing for now)
        if (is_gg && port < 0x07) {
            return
        }

        when (port and 0xC1) {
            0x01 -> {
                // Accurate emulation with HCounter
                if (LIGHTGUN) {
                    val oldTH = getTH(PORT_A) != 0 || getTH(PORT_B) != 0

                    writePort(PORT_A, value)
                    writePort(PORT_B, value shr 2)

                    // Toggling TH latches H Counter
                    if (!oldTH && (getTH(PORT_A) != 0 || getTH(PORT_B) != 0)) {
                        hCounter = this.hCount
                    }
                } else {
                    ioPorts[0] = (value and 0x20) shl 1
                    ioPorts[1] = (value and 0x80)

                    if (europe == 0)  // not european system
                    {
                        ioPorts[0] = ioPorts[0].inv()
                        ioPorts[1] = ioPorts[1].inv()
                    }
                }
            }

            0x80 -> vdp.dataWrite(value)
            0x81 -> vdp.controlWrite(value)
            0x40, 0x41 -> if (EmuState.supportsSound && soundEnabled) psg.write(value)
        }
    }

    fun `in`(port: Int): Int {
        // Game Gear Serial Ports (not fully emulated)
        if (is_gg && port < 0x07) {
            when (port) {
                0x00 -> return (ggstart and 0xBF) or europe

                0x01, 0x02, 0x03, 0x04, 0x05 -> return 0
                0x06 -> return 0xFF
            }
        }


        when (port and 0xC1) {
            0x40 -> return vdp.vCount

            0x41 -> return hCounter

            0x80 -> return vdp.dataRead()

            0x81 -> return vdp.controlRead()

            0xC0 -> return controller1

            0xC1 -> if (LIGHTGUN) {
                if (lightgunClick) lightPhaserSync()

                return (controller2 and 0x3F) or (if (getTH(PORT_A) != 0) 0x40 else 0) or (if (getTH(PORT_B) != 0) 0x80 else 0)
            } else {
                return (controller2 and 0x3F) or ioPorts[0] or ioPorts[1]
            }
        }

        // Default Value is 0xFF
        return 0xFF
    }

    // --------------------------------------------------------------------------------------------
    // Port A/B Emulation
    // --------------------------------------------------------------------------------------------
    private fun writePort(index: Int, value: Int) {
        ioPorts[index + IO_TR_DIRECTION] = value and 0x01
        ioPorts[index + IO_TH_DIRECTION] = value and 0x02
        ioPorts[index + IO_TR_OUTPUT] = value and 0x10
        ioPorts[index + IO_TH_OUTPUT] = if (europe == 0) (value.inv()) and 0x20 else value and 0x20
    }

    private fun getTH(index: Int): Int {
        return if (ioPorts[index + IO_TH_DIRECTION] == 0) ioPorts[index + IO_TH_OUTPUT] else ioPorts[index + IO_TH_INPUT]
    }

    private fun setTH(index: Int, on: Boolean) {
        ioPorts[index + IO_TH_DIRECTION] = 1
        ioPorts[index + IO_TH_INPUT] = if (on) 1 else 0
    }

    private val hCount: Int
        // --------------------------------------------------------------------------------------------
        get() {
            val pixels = (Z80.cycle * Vdp.SMS_X_PIXELS) / cyclesPerLine
            var v = ((pixels - 8) shr 1)
            if (v > 0x93) v += 0xE9 - 0x94

            return v and 0xFF
        }

    private fun lightPhaserSync() {
        val oldTH = getTH(PORT_A)
        val hc = this.hCount

        val dx = lightgunX - (hc shl 1)
        val dy = lightgunY - vdp.line

        // Within 8 pixels of click on Y value
        // Within 96 pixels of click on X value
        if ((dy > -Y_RANGE && dy < Y_RANGE) &&
            (dx > -X_RANGE && dx < X_RANGE)
        ) {
            setTH(PORT_A, false)

            // TH has been toggled, update with lightgun position
            if (oldTH != getTH(PORT_A)) hCounter = 20 + (lightgunX shr 1)
        } else {
            setTH(PORT_A, true)

            // TH has been toggled, update with usual HCounter value
            if (oldTH != getTH(PORT_A)) hCounter = hc
        }
    }

    companion object {
        /**
         * European / Domestic System
         */
        private var europe = 0x40

        private const val IO_TR_DIRECTION = 0
        private const val IO_TH_DIRECTION = 1
        private const val IO_TR_OUTPUT = 2
        private const val IO_TH_OUTPUT = 3
        private const val IO_TH_INPUT = 4

        const val PORT_A: Int = 0
        const val PORT_B: Int = 5

        // --------------------------------------------------------------------------------------------
        // Lightgun <-> Port Synchronisation
        // This is a hacky way to do things, but works reasonably well.
        // --------------------------------------------------------------------------------------------
        /**
         * X range of Lightgun
         */
        private const val X_RANGE = 48

        /**
         * Y range of Lightgun
         */
        private const val Y_RANGE = 4

        var isDomestic: Boolean
            get() = europe != 0
            /**
             * Set Console to European / Japanese Model
             *
             * @param value True is European, False is Japanese
             */
            set(value) {
                europe = if (value) 0x40 else 0
            }
    }
}
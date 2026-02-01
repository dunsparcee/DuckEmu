package io.duckemu.gbc

import kotlin.time.Clock

open class GameCard(bin: ByteArray) {
    val rom: Array<ByteArray>
    val type: Int
    val colored: Boolean
    val hasBattery: Boolean
    val ram: Array<ByteArray>
    val rtcReg: ByteArray
    var lastRtcUpdate: Int

    init {
        this.rom = loadRom(bin)
        this.type = loadType(bin)
        this.colored = loadColored(bin)
        this.hasBattery = loadHasBattery(bin)
        this.ram = loadRam(bin)
        this.rtcReg = ByteArray(5)
        this.lastRtcUpdate = Clock.System.now().toEpochMilliseconds().toInt()
    }

    private fun loadRom(bin: ByteArray): Array<ByteArray> {
        val cartRomBankNumber: Int
        val sizeByte = bin[0x0148].toInt()
        if (sizeByte < 8) cartRomBankNumber = 2 shl sizeByte
        else if (sizeByte == 0x52) cartRomBankNumber = 72
        else if (sizeByte == 0x53) cartRomBankNumber = 80
        else if (sizeByte == 0x54) cartRomBankNumber = 96
        else cartRomBankNumber = -1
        val rom = Array(cartRomBankNumber * 2) { ByteArray(0x2000) }
        for (i in 0..<cartRomBankNumber * 2) {
            if (0x2000 * i < bin.size) bin.copyInto(
                destination = rom[i],
                destinationOffset = 0,
                startIndex = 0x2000 * i,
                endIndex = 0x2000 * i + 0x2000
            )
        }
        return rom
    }

    private fun loadType(bin: ByteArray): Int {
        return bin[0x0147].toInt() and 0xff
    }

    private fun loadColored(bin: ByteArray): Boolean {
        return ((bin[0x143].toInt() and 0x80) == 0x80)
    }

    private fun loadHasBattery(bin: ByteArray): Boolean {
        val type = loadType(bin)
        return (type == 3) || (type == 9) || (type == 0x1B)
                || (type == 0x1E) || (type == 6) || (type == 0x10)
                || (type == 0x13)
    }

    private fun loadRam(bin: ByteArray): Array<ByteArray> {
        val ramBankNumber = when (bin[0x149].toInt()) {
            1, 2 -> 1
            3 -> 4
            4, 5, 6 -> 16
            else -> 0
        }
        return Array(if (ramBankNumber == 0) 1 else ramBankNumber) { ByteArray(0x2000) }
    }

    fun rtcSync() {
        if ((rtcReg[4].toInt() and 0x40) == 0) {
            val now = Clock.System.now().toEpochMilliseconds().toInt()
            while (now - lastRtcUpdate > 1000) {
                lastRtcUpdate += 1000

                if ((++rtcReg[0]).toInt() == 60) {
                    rtcReg[0] = 0

                    if ((++rtcReg[1]).toInt() == 60) {
                        rtcReg[1] = 0

                        if ((++rtcReg[2]).toInt() == 24) {
                            rtcReg[2] = 0

                            if ((++rtcReg[3]).toInt() == 0) {
                                rtcReg[4] = ((rtcReg[4].toInt() or (rtcReg[4].toInt() shl 7)) xor 1).toByte()
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun rtcSkip(s: Int) {
        var sum = s + rtcReg[0]
        rtcReg[0] = (sum % 60).toByte()
        sum /= 60
        if (sum == 0) return

        sum += rtcReg[1]
        rtcReg[1] = (sum % 60).toByte()
        sum /= 60
        if (sum == 0) return

        sum += rtcReg[2]
        rtcReg[2] = (sum % 24).toByte()
        sum /= 24
        if (sum == 0) return

        sum += (rtcReg[3].toInt() and 0xff) + ((rtcReg[4].toInt() and 1) shl 8)
        rtcReg[3] = (sum).toByte()

        if (sum > 511) rtcReg[4] = (rtcReg[4].toInt() or 0x80).toByte()
        rtcReg[4] = ((rtcReg[4].toInt() and 0xfe) + ((sum shr 8) and 1)).toByte()
    }

    fun dumpSram(): ByteArray {
        val bankCount = ram.size
        val bankSize = ram[0].size
        val size = bankCount * bankSize + 13
        val b = ByteArray(size)
        for (i in 0..<bankCount) ram[i].copyInto(
            destination = b,
            destinationOffset = i * bankSize,
            startIndex = 0,
            endIndex = bankSize
        )

        rtcReg.copyInto(
            destination = b,
            destinationOffset = bankCount * bankSize,
            startIndex = 0,
            endIndex = 5
        )

        val now = Clock.System.now().toEpochMilliseconds().toInt()
        BytesOperation.setInt(b, bankCount * bankSize + 5, (now shr 32))
        BytesOperation.setInt(b, bankCount * bankSize + 9, now)
        return b
    }

    fun setSram(b: ByteArray) {
        val bankCount = ram.size
        val bankSize = ram[0].size
        for (i in 0..<bankCount) b.copyInto(
            destination = ram[i],
            destinationOffset = 0,
            startIndex = i * bankSize,
            endIndex = i * bankSize + bankSize
        )
        if (b.size == bankCount * bankSize + 13) {
            b.copyInto(
                destination = rtcReg,
                destinationOffset = 0,
                startIndex = bankCount * bankSize,
                endIndex = bankCount * bankSize + 5
            )
            var time: Long = BytesOperation.getInt(b, bankCount * bankSize + 5).toLong()
            time = (time shl 32) + (BytesOperation.getInt(b, bankCount * bankSize + 9).toLong() and 0xffffffffL)
            time = Clock.System.now().toEpochMilliseconds().toInt() - time
            this.rtcSkip((time / 1000).toInt())
        }
    }
}
package io.duckemu.gbc.domain.nes.core

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class Rom() {

    fun reset() {
    }

    fun release() {
        rom = null
        chr = null
        sram = null
        vram = null
    }

    fun load(fname: String) {
        release()

        val `is` = PlatformFile(fname)

        val dat = runBlocking {
            `is`.readBytes()
        }

        if (!(dat[0] == 'N'.code.toByte() && dat[1] == 'E'.code.toByte() && dat[2] == 'S'.code.toByte() && dat[3] == '\u001A'.code.toByte()))
            throw RuntimeException("Can't load rom")

        prgPageCnt = dat[4].toInt() and 0xff
        chrPageCnt = dat[5].toInt() and 0xff

        mirroring = if ((dat[6].toInt() and 1) != 0)
            MirrorType.VERTICAL
        else
            MirrorType.HORIZONTAL
        sramEnable = (dat[6].toInt() and 2) != 0
        trainerEnable = (dat[6].toInt() and 4) != 0
        this.isFourScreen = (dat[6].toInt() and 8) != 0

        mapper = ((dat[6].toInt() and 0xff) shr 4) or (dat[7].toInt() and 0xf0)

        val romSize = 0x4000 * prgPageCnt
        val chrSize = 0x2000 * chrPageCnt

        this.rom = ByteArray(romSize)
        if (chrSize != 0) this.chr = ByteArray(chrSize)
        sram = ByteArray(0x2000)
        vram = ByteArray(0x2000)

        if (romSize > 0 && this.rom != null) dat.copyInto(this.rom!!, 0, 16, 16 + romSize)
        if (chrSize > 0 && this.chr != null) dat.copyInto(
            this.chr!!,
            0,
            16 + romSize,
            16 + romSize + chrSize
        )
    }

    fun saveSram(fname: String, sram: ByteArray?) {
        sram?.let {
            val path = fname.toPath()
            FileSystem.SYSTEM.write(path) {
                write(it)
            }
        }
    }

    suspend fun loadSram(fname: String) {
        val file = PlatformFile(fname)
        if (file.exists()) {
            sram = file.readBytes()
        }
    }

    fun romSize(): Int {
        return prgPageCnt
    }

    fun chrSize(): Int {
        return chrPageCnt
    }

    fun mapperNo(): Int {
        return mapper
    }

    fun hasSram(): Boolean {
        return sramEnable
    }

    fun hasTrainer(): Boolean {
        return trainerEnable
    }

    enum class MirrorType {
        HORIZONTAL, VERTICAL,
    }

    fun mirror(): MirrorType? {
        return mirroring
    }

    private var prgPageCnt = 0
    private var chrPageCnt = 0
    private var mirroring: Rom.MirrorType? = null
    private var sramEnable = true
    private var trainerEnable = false
    var isFourScreen: Boolean = false
        private set
    private var mapper = 0

    var rom: ByteArray? = ByteArray(16)
    var chr: ByteArray? = ByteArray(16)
    var sram: ByteArray? = ByteArray(16)
    var vram: ByteArray? = ByteArray(16)
}
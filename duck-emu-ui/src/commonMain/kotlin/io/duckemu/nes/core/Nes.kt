package io.duckemu.nes.core

import androidx.compose.ui.graphics.ImageBitmap
import io.duckemu.gbc.domain.nes.core.*
import io.duckemu.nes.core.mapper.Mapper
import io.duckemu.nes.core.mapper.makeMapper
import io.duckemu.nes.core.ui.Renderer

class Nes(
    val renderer: Renderer
) {

    fun load(fname: String) {
        rom.load(fname)
        mapper = makeMapper(rom.mapperNo(), this)
        reset()
    }

    fun checkMapper(): Boolean {
        return mapper != null
    }

    fun saveSram(fname: String?) {
        rom.saveSram(fname)
    }

    fun loadSram(fname: String?) {
        rom.loadSram(fname)
    }

    fun saveState(fname: String?) {
    }

    fun loadState(fname: String?) {
    }

    fun reset() {
        rom.reset()
        mbc.reset()

        mapper!!.reset()

        cpu.reset()
        apu.reset()
        ppu.reset()
        regs.reset()
    }

    fun execFrame(): ImageBitmap {
        val scri = renderer.requestScreen(256, 240)
        val sndi = renderer.requestSound()
        val inpi = renderer.requestInput(2, 8)

        if (sndi != null) {
            apu.genAudio(sndi)
            renderer.outputSound(sndi)
        }

        regs.setInput(inpi.buf)

        regs.setVBlank(false, true)
        regs.startFrame()
        for (i in 0..239) {
            if (mapper != null) mapper!!.hblank(i)
            regs.startScanline()
            ppu.render(i, scri)
            ppu.spriteCheck(i)
            apu.sync()
            cpu.exec(114)
            regs.endScanline()
        }

        if ((regs.frameIrq and 0xC0) == 0) cpu.setIrq(true)

        for (i in 240..261) {
            if (mapper != null) mapper!!.hblank(i)
            apu.sync()
            if (i == 241) {
                regs.setVBlank(true, false)
                cpu.exec(0)
                regs.setVBlank(regs.isVBlank, true)
                cpu.exec(114)
            } else cpu.exec(114)
        }

        return renderer.outputScreen(scri)
    }

    val rom: Rom = Rom()
    val cpu: Cpu = Cpu(this)
    val ppu: Ppu = Ppu(this)
    val apu: Apu = Apu(this)
    val mbc: Mbc = Mbc(this)
    val regs: Regs = Regs(this)
    var mapper: Mapper?
        private set

    init {
        mapper = null
    }

}
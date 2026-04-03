package io.duckemu.nes.domain.mapper

import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.ui.Renderer

abstract class MapperAdapter : Mapper {
    protected lateinit var nesCore: Nes

    override fun audio(info: Renderer.SoundInfo) {
    }

    override fun hblank(line: Int) {
    }

    override fun reset() {
    }

    override fun write(adr: Short, dat: Byte) {
    }

    override fun setNes(nes: Nes) {
        this.nesCore = nes
        reset()
    }
}
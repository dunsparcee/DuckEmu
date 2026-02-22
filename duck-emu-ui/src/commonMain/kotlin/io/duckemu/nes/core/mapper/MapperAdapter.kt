package io.duckemu.nes.core.mapper

import io.duckemu.nes.core.Nes
import io.duckemu.nes.core.ui.Renderer

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
package io.duckemu.nes.domain.mapper

import io.duckemu.nes.domain.Nes
import io.duckemu.nes.domain.ui.Renderer

interface Mapper {
    fun mapperNo(): Int

    fun reset()

    fun write(adr: Short, dat: Byte)

    fun hblank(line: Int)

    fun audio(info: Renderer.SoundInfo)

    fun setNes(nes: Nes)
}

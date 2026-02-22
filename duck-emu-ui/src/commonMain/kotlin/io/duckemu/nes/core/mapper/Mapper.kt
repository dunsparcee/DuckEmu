package io.duckemu.nes.core.mapper

import io.duckemu.nes.core.Nes
import io.duckemu.nes.core.ui.Renderer

interface Mapper {
    fun mapperNo(): Int

    fun reset()

    fun write(adr: Short, dat: Byte)

    fun hblank(line: Int)

    fun audio(info: Renderer.SoundInfo)

    fun setNes(nes: Nes)
}

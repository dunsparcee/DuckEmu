package io.duckemu.nes.core.mapper

import io.duckemu.nes.core.Nes

var mappers: MutableList<Mapper> = mutableListOf(
    CNROM(), MMC1(), MMC3(), NopMapper(), UnRom(), VRC6()
)

fun makeMapper(num: Int, n: Nes): Mapper {
    for (m in mappers) {
        if (m.mapperNo() == num) {
            m.setNes(n)
            return m
        }
    }

    throw RuntimeException("dont find a mapper to this rom")
}


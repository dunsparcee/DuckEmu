package io.duckemu.nes.domain.mapper

import io.duckemu.nes.domain.Nes

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


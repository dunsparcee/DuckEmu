package io.duckemu.nes.core.mapper


class NopMapper : MapperAdapter() {
    override fun mapperNo(): Int {
        return 0
    }
}
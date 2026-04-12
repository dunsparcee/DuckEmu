package io.duckemu.nes.domain.mapper


class NopMapper : MapperAdapter() {
    override fun mapperNo(): Int {
        return 0
    }
}
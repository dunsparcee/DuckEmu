package io.duckemu.nes.core.mapper


class UnRom : MapperAdapter() {
    override fun mapperNo(): Int {
        return 2
    }

    public override fun reset() {
        val romSize = nesCore.rom.romSize()
        nesCore.mbc.mapRom(0, 0)
        nesCore.mbc.mapRom(1, 1)
        nesCore.mbc.mapRom(2, (romSize - 1) * 2)
        nesCore.mbc.mapRom(3, (romSize - 1) * 2 + 1)
    }

    public override fun write(adr: Short, dat: Byte) {
        nesCore.mbc.mapRom(0, dat * 2)
        nesCore.mbc.mapRom(1, dat * 2 + 1)
    }
}
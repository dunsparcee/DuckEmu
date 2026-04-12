package io.duckemu.nes.domain.mapper


class CNROM : MapperAdapter() {
    override fun mapperNo(): Int {
        return 3
    }

    override fun reset() {
        for (i in 0..3) nesCore.mbc.mapRom(i, i)
        for (i in 0..7) nesCore.mbc.mapVrom(i, i)
    }

    override fun write(adr: Short, dat: Byte) {
        for (i in 0..7) nesCore.mbc.mapVrom(i, (dat.toInt() and 0xff) * 8 + i)
    }
}
package io.duckemu.sms.data


object Setup {
    const val PROGRAM_NAME: String = "JavaGear"
    const val AUTHOR: String = "Chris White"
    const val DEBUG_TIMING: Boolean = false
    const val REFRESH_EMULATION: Boolean = true
    const val ACCURATE_INTERRUPT_EMULATION: Boolean = true
    const val LIGHTGUN: Boolean = false
    const val VDP_SPRITE_COLLISIONS: Boolean = true
    const val PAGE_SIZE: Int = 0x400
    const val CLOCK_NTSC: Int = 3579545
    const val CLOCK_PAL: Int = 3546893
    const val SAMPLE_RATE: Int = 22050
}
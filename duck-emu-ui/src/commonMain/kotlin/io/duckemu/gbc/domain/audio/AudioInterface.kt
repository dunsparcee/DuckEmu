package io.duckemu.gbc.domain.audio

const val CHAN_LEFT: Int = 1
const val CHAN_RIGHT: Int = 2
const val CHAN_MONO: Int = 4

expect class AudioInterface() {
    constructor(registers: ByteArray)

    fun setSoundEnabled(soundEnabled: Boolean)
    fun setChannelEnable(channel: Int, enable: Boolean)
    fun ioWrite(num: Int, data: Int)
    fun outputSound()
    fun setSpeed(i: Int)
}
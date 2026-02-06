package io.duckemu.gbc.audio

import io.duckemu.gbc.BytesOperation
import io.duckemu.gbc.addons.Speed
import kotlin.random.Random

const val CHAN_LEFT: Int = 1
const val CHAN_RIGHT: Int = 2
const val CHAN_MONO: Int = 4

// Common expect/actual class
expect class AudioInterface() {
    constructor(registers: ByteArray)

    fun setSoundEnabled(soundEnabled: Boolean)
    fun setChannelEnable(channel: Int, enable: Boolean)
    fun ioWrite(num: Int, data: Int)
    fun outputSound()
    fun setSpeed(i: Int)
}
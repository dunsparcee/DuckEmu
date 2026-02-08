package io.duckemu.gbc.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.duckemu.gbc.BytesOperation
import io.duckemu.gbc.addons.Speed
import java.util.Random
import kotlin.math.max

// Remove or comment out this suppression if not using KMP
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class AudioInterface actual constructor() {

    public actual constructor(registers: ByteArray) : this() {
        this.registers = registers
    }

    private var registers = ByteArray(0)
    private val speed = Speed()

    // Replaced SourceDataLine with Android's AudioTrack
    var audioTrack: AudioTrack? = null

    var channel1: SquareWaveGenerator
    var channel2: SquareWaveGenerator
    var channel3: VoluntaryWaveGenerator
    var channel4: NoiseGenerator

    var soundEnabledAudio: Boolean = false

    var channel1EnableAudio: Boolean = true

    var channel2EnableAudio: Boolean = true

    var channel3EnableAudio: Boolean = true

    var channel4EnableAudio: Boolean = true

    var sampleRateAudio: Int = 44100
    var bufferLengthMsec: Int = 200

    // Helper constants for channels
    companion object {
        const val CHAN_LEFT = 1
        const val CHAN_RIGHT = 2
        const val CHAN_MONO = 4 // Assuming 4 based on usage, adjust if your constants differ
    }

    init {
        audioTrack = initSoundHardware()
        channel1 = SquareWaveGenerator(sampleRateAudio)
        channel2 = SquareWaveGenerator(sampleRateAudio)
        channel3 = VoluntaryWaveGenerator(sampleRateAudio)
        channel4 = NoiseGenerator(sampleRateAudio)
    }

    actual fun setSoundEnabled(soundEnabled: Boolean) {
        this.soundEnabledAudio = soundEnabled
        if (soundEnabled) {
            audioTrack?.play()
        } else {
            audioTrack?.pause()
            audioTrack?.flush()
        }
    }

    actual fun setChannelEnable(channel: Int, enable: Boolean) {
        when (channel) {
            1 -> this.channel1EnableAudio = enable
            2 -> this.channel2EnableAudio = enable
            3 -> this.channel3EnableAudio = enable
            4 -> this.channel4EnableAudio = enable
        }
    }

    actual fun ioWrite(num: Int, data: Int) {
        // Logic remains identical to JVM version
        when (num) {
            0x10 -> this.channel1.setSweep(
                (BytesOperation.unsign(data.toByte()) and 0x70) shr 4,
                (BytesOperation.unsign(data.toByte()) and 0x07),
                false
            )

            0x11 -> {
                this.channel1.setDutyCycle((BytesOperation.unsign(data.toByte()) and 0xC0) shr 6)
                this.channel1.setLength(BytesOperation.unsign(data.toByte()) and 0x3F)
            }

            0x12 -> this.channel1.setEnvelope(
                (BytesOperation.unsign(data.toByte()) and 0xF0) shr 4,
                (BytesOperation.unsign(data.toByte()) and 0x07),
                (BytesOperation.unsign(data.toByte()) and 0x08) == 8
            )

            0x13 -> this.channel1.setFrequency(
                ((BytesOperation.unsign(registers[0x14]) and 0x07) shl 8) + BytesOperation.unsign(
                    registers[0x13]
                )
            )

            0x14 -> {
                if ((registers[0x14].toInt() and 0x80) != 0) {
                    this.channel1.setLength(BytesOperation.unsign(registers[0x11]) and 0x3F)
                    this.channel1.setEnvelope(
                        (BytesOperation.unsign(registers[0x12]) and 0xF0) shr 4,
                        (BytesOperation.unsign(registers[0x12]) and 0x07),
                        (BytesOperation.unsign(registers[0x12]) and 0x08) == 8
                    )
                }

                if ((registers[0x14].toInt() and 0x40) == 0) {
                    this.channel1.setLength(-1)
                }

                this.channel1.setFrequency(
                    ((BytesOperation.unsign(registers[0x14]) and 0x07) shl 8) + BytesOperation.unsign(
                        registers[0x13]
                    )
                )
            }

            0x16 -> {
                this.channel2.setDutyCycle((BytesOperation.unsign(data.toByte()) and 0xC0) shr 6)
                this.channel2.setLength(BytesOperation.unsign(data.toByte()) and 0x3F)
            }

            0x17 -> this.channel2.setEnvelope(
                (BytesOperation.unsign(data.toByte()) and 0xF0) shr 4,
                (BytesOperation.unsign(data.toByte()) and 0x07),
                (BytesOperation.unsign(data.toByte()) and 0x08) == 8
            )

            0x18 -> this.channel2.setFrequency(
                ((BytesOperation.unsign(registers[0x19]) and 0x07) shl 8) + BytesOperation.unsign(
                    registers[0x18]
                )
            )

            0x19 -> {
                if ((registers[0x19].toInt() and 0x80) != 0) {
                    this.channel2.setLength(BytesOperation.unsign(registers[0x21]) and 0x3F)
                    this.channel2.setEnvelope(
                        (BytesOperation.unsign(registers[0x17]) and 0xF0) shr 4,
                        (BytesOperation.unsign(registers[0x17]) and 0x07),
                        (BytesOperation.unsign(registers[0x17]) and 0x08) == 8
                    )
                }
                if ((registers[0x19].toInt() and 0x40) == 0) {
                    this.channel2.setLength(-1)
                }
                this.channel2.setFrequency(
                    ((BytesOperation.unsign(registers[0x19]) and 0x07) shl 8) + BytesOperation.unsign(
                        registers[0x18]
                    )
                )
            }

            0x1A -> if ((BytesOperation.unsign(data.toByte()) and 0x80) != 0) {
                this.channel3.setVolume((BytesOperation.unsign(registers[0x1C]) and 0x60) shr 5)
            } else {
                this.channel3.setVolume(0)
            }

            0x1B -> this.channel3.setLength(BytesOperation.unsign(data.toByte()))
            0x1C -> this.channel3.setVolume((BytesOperation.unsign(registers[0x1C]) and 0x60) shr 5)
            0x1D -> this.channel3.setFrequency(
                ((BytesOperation.unsign(registers[0x1E]) and 0x07) shl 8) + BytesOperation.unsign(
                    registers[0x1D]
                )
            )

            0x1E -> {
                if ((registers[0x19].toInt() and 0x80) != 0) {
                    this.channel3.setLength(BytesOperation.unsign(registers[0x1B]))
                }
                this.channel3.setFrequency(
                    ((BytesOperation.unsign(registers[0x1E]) and 0x07) shl 8) + BytesOperation.unsign(
                        registers[0x1D]
                    )
                )
            }

            0x20 -> this.channel4.setLength(BytesOperation.unsign(data.toByte()) and 0x3F)
            0x21 -> this.channel4.setEnvelope(
                (BytesOperation.unsign(data.toByte()) and 0xF0) shr 4,
                (BytesOperation.unsign(data.toByte()) and 0x07),
                (BytesOperation.unsign(data.toByte()) and 0x08) == 8
            )

            0x22 -> this.channel4.setParameters(
                (BytesOperation.unsign(data.toByte()) and 0x07).toFloat(),
                (BytesOperation.unsign(data.toByte()) and 0x08) == 8,
                (BytesOperation.unsign(data.toByte()) and 0xF0) shr 4
            )

            0x23 -> {
                if ((registers[0x23].toInt() and 0x80) != 0) {
                    this.channel4.setLength(BytesOperation.unsign(registers[0x20]) and 0x3F)
                }
                if ((registers[0x23].toInt() and 0x40) == 0) {
                    this.channel4.setLength(-1)
                }
            }

            0x25 -> {
                var chanData: Int
                run {
                    chanData = 0
                    if ((BytesOperation.unsign(data.toByte()) and 0x01) != 0) {
                        chanData = chanData or CHAN_LEFT
                    }
                    if ((BytesOperation.unsign(data.toByte()) and 0x10) != 0) {
                        chanData = chanData or CHAN_RIGHT
                    }
                    this.channel1.setChannel(chanData)

                    chanData = 0
                    if ((BytesOperation.unsign(data.toByte()) and 0x02) != 0) {
                        chanData = chanData or CHAN_LEFT
                    }
                    if ((BytesOperation.unsign(data.toByte()) and 0x20) != 0) {
                        chanData = chanData or CHAN_RIGHT
                    }
                    this.channel2.setChannel(chanData)

                    chanData = 0
                    if ((BytesOperation.unsign(data.toByte()) and 0x04) != 0) {
                        chanData = 0 or CHAN_LEFT
                    }
                    if ((BytesOperation.unsign(data.toByte()) and 0x40) != 0) {
                        chanData = chanData or CHAN_RIGHT
                    }
                    this.channel3.setChannel(chanData)
                }
            }
        }
    }

    actual fun outputSound() {
        if (soundEnabledAudio && speed.output()) {
            audioTrack?.let { track ->
                // Calculate chunk size.
                // Original used available() * 2, capped at sampleRate/28.
                // In Android, we try to write a consistent small chunk.
                // 44100 / 28 is approx 1575. We align to 2 (stereo).
                val chunkSize = (sampleRateAudio / 28) and 0xFFFE

                // Create buffer for mixing (Signed 8-bit)
                val b = ByteArray(chunkSize)

                // Mix channels
                if (channel1EnableAudio) channel1.play(b, chunkSize / 2, 0)
                if (channel2EnableAudio) channel2.play(b, chunkSize / 2, 0)
                if (channel3EnableAudio) channel3.play(b, chunkSize / 2, 0)
                if (channel4EnableAudio) channel4.play(b, chunkSize / 2, 0)

                // CONVERSION: Signed 8-bit -> Unsigned 8-bit
                // Android ENCODING_PCM_8BIT is unsigned (0-255), Generators are signed (-128 to 127)
                for (i in b.indices) {
                    b[i] = (b[i] + 128).toByte()
                }

                // Write to hardware
                track.write(b, 0, chunkSize)
            }
        }
    }

    fun initSoundHardware(): AudioTrack? {
        try {
            val encoding = AudioFormat.ENCODING_PCM_8BIT
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO

            val minBufferSize = AudioTrack.getMinBufferSize(sampleRateAudio, channelConfig, encoding)
            val desiredBuffer = (sampleRateAudio / 1000) * bufferLengthMsec
            val bufferSize = max(minBufferSize, desiredBuffer)

            val track =
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(encoding)
                            .setSampleRate(sampleRateAudio)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
                soundEnabledAudio = true
                return track
            }

            soundEnabledAudio = false
        } catch (e: Exception) {
            println("Error: Audio system busy! ${e.message}")
            e.printStackTrace()
            soundEnabledAudio = false
        }

        return null
    }

    fun setSampleRate(sr: Int) {
        sampleRateAudio = sr
        restartHardware()
        channel1.setSampleRate(sr)
        channel2.setSampleRate(sr)
        channel3.setSampleRate(sr)
        channel4.setSampleRate(sr)
    }

    fun setBufferLength(time: Int) {
        bufferLengthMsec = time
        restartHardware()
    }

    private fun restartHardware() {
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = initSoundHardware()
    }

    inner class SquareWaveGenerator {
        var totalLength: Int = 0
        var cyclePos: Int
        var cycleLength: Int
        var amplitude: Int

        var dutyCycleAudio: Int

        var channelAudio: Int

        var sampleRateAudio: Int
        var initialEnvelope: Int = 0
        var numStepsEnvelope: Int = 0
        var increaseEnvelope: Boolean = false
        var counterEnvelope: Int = 0
        var gbFrequency: Int = 0
        var timeSweep: Int = 0
        var numSweep: Int = 0
        var decreaseSweep: Boolean = false
        var counterSweep: Int = 0

        internal constructor(waveLength: Int, ampl: Int, duty: Int, chan: Int, rate: Int) {
            cycleLength = waveLength
            amplitude = ampl
            cyclePos = 0
            dutyCycleAudio = duty
            channelAudio = chan
            sampleRateAudio = rate
        }

        internal constructor(rate: Int) {
            dutyCycleAudio = 4
            cyclePos = 0
            channelAudio = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRateAudio = rate
            amplitude = 32
            counterSweep = 0
        }

        fun setSampleRate(sr: Int) {
            sampleRateAudio = sr
        }

        fun setDutyCycle(duty: Int) {
            when (duty) {
                0 -> dutyCycleAudio = 1
                1 -> dutyCycleAudio = 2
                2 -> dutyCycleAudio = 4
                3 -> dutyCycleAudio = 6
            }
        }

        fun setFrequency(gbFrequency: Int) {
            try {
                var frequency = (131072 / 2048).toFloat()

                if (gbFrequency != 2048) {
                    frequency = (131072f / (2048 - gbFrequency).toFloat())
                }
                this.gbFrequency = gbFrequency
                cycleLength = if (frequency != 0f) {
                    (256 * sampleRateAudio) / frequency.toInt()
                } else {
                    65535
                }
                if (cycleLength == 0) cycleLength = 1
            } catch (_: ArithmeticException) {
            }
        }

        fun setChannel(chan: Int) {
            channelAudio = chan
        }

        fun setEnvelope(initialValue: Int, numSteps: Int, increase: Boolean) {
            initialEnvelope = initialValue
            numStepsEnvelope = numSteps
            increaseEnvelope = increase
            amplitude = initialValue * 2
        }

        fun setSweep(time: Int, num: Int, decrease: Boolean) {
            timeSweep = (time + 1) / 2
            numSweep = num
            decreaseSweep = decrease
            counterSweep = 0
        }

        fun setLength(gbLength: Int) {
            totalLength = if (gbLength == -1) {
                -1
            } else {
                (64 - gbLength) / 4
            }
        }

        fun play(b: ByteArray, length: Int, offset: Int) {
            var value = 0

            if (totalLength != 0) {
                totalLength--

                if (timeSweep != 0) {
                    counterSweep++
                    if (counterSweep > timeSweep) {
                        if (decreaseSweep) {
                            setFrequency(gbFrequency - (gbFrequency shr numSweep))
                        } else {
                            setFrequency(gbFrequency + (gbFrequency shr numSweep))
                        }
                        counterSweep = 0
                    }
                }

                counterEnvelope++
                if (numStepsEnvelope != 0) {
                    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
                        if (!increaseEnvelope) {
                            amplitude -= 2
                        } else {
                            if (amplitude < 16) amplitude += 2
                        }
                    }
                }
                for (r in offset..<offset + length) {
                    if (cycleLength != 0) {
                        value = if (((8 * cyclePos) / cycleLength) >= dutyCycleAudio) {
                            amplitude
                        } else {
                            -amplitude
                        }
                    }

                    if ((channelAudio and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + value).toByte()
                    if ((channelAudio and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + value).toByte()
                    if ((channelAudio and CHAN_MONO) != 0) b[r] = (b[r] + value).toByte()

                    cyclePos = (cyclePos + 256) % cycleLength
                }
            }
        }
    }

    class VoluntaryWaveGenerator {
        var totalLength: Int = 0
        var cyclePos: Int
        var cycleLength: Int
        var amplitude: Int

        var channelAudio: Int

        var sampleRateAudio: Int
        var volumeShift: Int = 0

        var waveform: ByteArray = ByteArray(32)

        internal constructor(waveLength: Int, ampl: Int, duty: Int, chan: Int, rate: Int) {
            cycleLength = waveLength
            amplitude = ampl
            cyclePos = 0
            channelAudio = chan
            sampleRateAudio = rate
        }

        internal constructor(rate: Int) {
            cyclePos = 0
            channelAudio = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRateAudio = rate
            amplitude = 32
        }

        fun setSampleRate(sr: Int) {
            sampleRateAudio = sr
        }

        fun setFrequency(gbFrequency: Int) {
            val frequency = (65536f / (2048 - gbFrequency).toFloat()).toInt().toFloat()
            cycleLength = ((256f * sampleRateAudio) / frequency).toInt()
            if (cycleLength == 0) cycleLength = 1
        }

        fun setChannel(chan: Int) {
            channelAudio = chan
        }

        fun setLength(gbLength: Int) {
            totalLength = if (gbLength == -1) {
                -1
            } else {
                (256 - gbLength) / 4
            }
        }

        fun setSamplePair(address: Int, value: Int) {
            waveform[address * 2] = ((value and 0xF0) shr 4).toByte()
            waveform[address * 2 + 1] = ((value and 0x0F)).toByte()
        }

        fun setVolume(volume: Int) {
            when (volume) {
                0 -> volumeShift = 5
                1 -> volumeShift = 0
                2 -> volumeShift = 1
                3 -> volumeShift = 2
            }
        }

        fun play(b: ByteArray, length: Int, offset: Int) {
            var value: Int

            if (totalLength != 0) {
                totalLength--

                for (r in offset..<offset + length) {
                    val samplePos = (31 * cyclePos) / cycleLength
                    value = BytesOperation.unsign(waveform[samplePos % 32])
                        .toInt() shr volumeShift shl 1

                    if ((channelAudio and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + value).toByte()
                    if ((channelAudio and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + value).toByte()
                    if ((channelAudio and CHAN_MONO) != 0) b[r] = (b[r] + value).toByte()

                    cyclePos = (cyclePos + 256) % cycleLength
                }
            }
        }
    }

    inner class NoiseGenerator {
        var totalLength: Int = 0
        var cyclePos: Int
        var cycleLength: Int
        var amplitude: Int

        var channelAudio: Int

        var sampleRateAudio: Int
        var initialEnvelope: Int = 0
        var numStepsEnvelope: Int = 0
        var increaseEnvelope: Boolean = false
        var counterEnvelope: Int = 0
        var randomValues: BooleanArray?
        var dividingRatio: Int = 0
        var polynomialSteps: Int = 0
        var shiftClockFreq: Int = 0
        var finalFreq: Int = 0
        var cycleOffset: Int

        internal constructor(waveLength: Int, ampl: Int, chan: Int, rate: Int) {
            cycleLength = waveLength
            amplitude = ampl
            cyclePos = 0
            channelAudio = chan
            sampleRateAudio = rate
            cycleOffset = 0
            randomValues = BooleanArray(32767)
            val rand = Random()
            for (r in 0..32766) {
                randomValues!![r] = rand.nextBoolean()
            }
            cycleOffset = 0
        }

        internal constructor(rate: Int) {
            cyclePos = 0
            channelAudio = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRateAudio = rate
            amplitude = 32
            randomValues = BooleanArray(32767)
            val rand = Random()
            for (r in 0..32766) {
                randomValues!![r] = rand.nextBoolean()
            }
            cycleOffset = 0
        }


        fun setSampleRate(sr: Int) {
            sampleRateAudio = sr
        }

        fun setChannel(chan: Int) {
            channelAudio = chan
        }

        fun setEnvelope(initialValue: Int, numSteps: Int, increase: Boolean) {
            initialEnvelope = initialValue
            numStepsEnvelope = numSteps
            increaseEnvelope = increase
            amplitude = initialValue * 2
        }

        fun setLength(gbLength: Int) {
            totalLength = if (gbLength == -1) {
                -1
            } else {
                (64 - gbLength) / 4
            }
        }

        fun setParameters(dividingRatio: Float, polynomialSteps: Boolean, shiftClockFreq: Int) {
            var ratio = dividingRatio
            this.dividingRatio = ratio.toInt()
            if (!polynomialSteps) {
                this.polynomialSteps = 32767
                cycleLength = 32767 shl 8
                cycleOffset = 0
            } else {
                this.polynomialSteps = 63
                cycleLength = 63 shl 8
                val rand = Random()
                cycleOffset = (rand.nextFloat() * 1000).toInt()
            }
            this.shiftClockFreq = shiftClockFreq

            if (ratio == 0f) ratio = 0.5f

            finalFreq = ((4194304 / 8 / ratio).toInt()) shr (shiftClockFreq + 1)
        }

        fun play(b: ByteArray, length: Int, offset: Int) {
            if (totalLength != 0) {
                totalLength--

                counterEnvelope++
                if (numStepsEnvelope != 0) {
                    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
                        if (!increaseEnvelope) {
                            amplitude -= 2
                        } else {
                            if (amplitude < 16) amplitude += 2
                        }
                    }
                }

                val step = ((finalFreq) / (sampleRateAudio shr 8))

                for (r in offset..<offset + length) {
                    val value = randomValues!![((cycleOffset) + (cyclePos shr 8)) and 0x7FFF]
                    val v = if (value) (amplitude / 2) else (-amplitude / 2)

                    if ((channelAudio and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + v).toByte()
                    if ((channelAudio and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + v).toByte()
                    if ((channelAudio and CHAN_MONO) != 0) b[r] = (b[r] + v).toByte()

                    cyclePos = (cyclePos + step) % cycleLength
                }
            }
        }
    }

    actual fun setSpeed(i: Int) {
        speed.setSpeed(i)
    }
}
package io.duckemu.gbc.audio

import io.duckemu.gbc.BytesOperation
import io.duckemu.gbc.addons.Speed
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class Speaker(private val registers: ByteArray) {
    private val speed = Speed()

    var soundLine: SourceDataLine?

    var channel1: SquareWaveGenerator
    var channel2: SquareWaveGenerator
    var channel3: VoluntaryWaveGenerator
    var channel4: NoiseGenerator

    @set:JvmName("updateSoundEnabled")
    var soundEnabled: Boolean = false

    @set:JvmName("updateChannel1")
    var channel1Enable: Boolean = true

    @set:JvmName("updateChannel2")
    var channel2Enable: Boolean = true

    @set:JvmName("updateChannel3")
    var channel3Enable: Boolean = true

    @set:JvmName("updateChannel4")
    var channel4Enable: Boolean = true

    @set:JvmName("updateSampleRate")
    var sampleRate: Int = 44100

    var bufferLengthMsec: Int = 200

    init {
        soundLine = initSoundHardware()
        channel1 = SquareWaveGenerator(sampleRate)
        channel2 = SquareWaveGenerator(sampleRate)
        channel3 = VoluntaryWaveGenerator(sampleRate)
        channel4 = NoiseGenerator(sampleRate)
    }

    fun setSoundEnabled(soundEnabled: Boolean) {
        this.soundEnabled = soundEnabled
    }

    fun setChannelEnable(channel: Int, enable: Boolean) {
        when (channel) {
            1 -> this.channel1Enable = enable
            2 -> this.channel2Enable = enable
            3 -> this.channel3Enable = enable
            4 -> this.channel4Enable = enable
        }
    }

    fun ioWrite(num: Int, data: Int) {
        when (num) {
            0x10 -> this.channel1.setSweep(
                (BytesOperation.unsign(data.toByte()).toInt() and 0x70) shr 4,
                (BytesOperation.unsign(data.toByte()).toInt() and 0x07),
                false
            )

            0x11 -> {
                this.channel1.setDutyCycle((BytesOperation.unsign(data.toByte()).toInt() and 0xC0) shr 6)
                this.channel1.setLength(BytesOperation.unsign(data.toByte()).toInt() and 0x3F)
            }

            0x12 -> this.channel1.setEnvelope(
                (BytesOperation.unsign(data.toByte()) and 0xF0) shr 4,
                (BytesOperation.unsign(data.toByte()) and 0x07),
                (BytesOperation.unsign(data.toByte()) and 0x08) == 8
            )

            0x13 -> this.channel1.setFrequency(
                ((BytesOperation.unsign(registers[0x14]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x13])
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
                    ((BytesOperation.unsign(registers[0x14]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x13])
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
                ((BytesOperation.unsign(registers[0x19]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x18])
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
                    ((BytesOperation.unsign(registers[0x19]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x18])
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
                ((BytesOperation.unsign(registers[0x1E]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x1D])
            )

            0x1E -> {
                if ((registers[0x19].toInt() and 0x80) != 0) {
                    this.channel3.setLength(BytesOperation.unsign(registers[0x1B]))
                }
                this.channel3.setFrequency(
                    ((BytesOperation.unsign(registers[0x1E]) and 0x07) shl 8) + BytesOperation.unsign(registers[0x1D])
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

    fun outputSound() {
        if (soundEnabled && speed.output()) {
            soundLine?.let {
                val numSamples: Int = if (sampleRate / 28 >= it.available().times(2)) {
                    it.available() * 2
                } else {
                    (sampleRate / 28) and 0xFFFE
                }

                val b = ByteArray(numSamples)
                if (channel1Enable) channel1.play(b, numSamples / 2, 0)
                if (channel2Enable) channel2.play(b, numSamples / 2, 0)
                if (channel3Enable) channel3.play(b, numSamples / 2, 0)
                if (channel4Enable) channel4.play(b, numSamples / 2, 0)
                it.write(b, 0, numSamples)
            }
        }
    }

    fun initSoundHardware(): SourceDataLine? {
        try {
            val format = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate.toFloat(), 8, 2, 2, sampleRate.toFloat(), true
            )
            val lineInfo = DataLine.Info(SourceDataLine::class.java, format)

            if (AudioSystem.isLineSupported(lineInfo)) {
                val line = AudioSystem.getLine(lineInfo) as SourceDataLine
                val bufferLength = (sampleRate / 1000) * bufferLengthMsec
                line.open(format, bufferLength)
                line.start()
                soundEnabled = true
                return line
            }

            soundEnabled = false
        } catch (_: Exception) {
            println("Error: Audio system busy!")
            soundEnabled = false
        }

        return null
    }

    fun setSampleRate(sr: Int) {
        sampleRate = sr

        soundLine?.flush()
        soundLine?.close()

        soundLine = initSoundHardware()

        channel1.setSampleRate(sr)
        channel2.setSampleRate(sr)
        channel3.setSampleRate(sr)
        channel4.setSampleRate(sr)
    }

    fun setBufferLength(time: Int) {
        bufferLengthMsec = time

        soundLine?.flush()
        soundLine?.close()

        soundLine = initSoundHardware()
    }

    inner class SquareWaveGenerator {
        var totalLength: Int = 0
        var cyclePos: Int
        var cycleLength: Int
        var amplitude: Int

        @set:JvmName("updateDuty")
        var dutyCycle: Int

        @set:JvmName("updateChannelRaw")
        var channel: Int

        @set:JvmName("updateSampleRateRaw")
        var sampleRate: Int
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
            dutyCycle = duty
            channel = chan
            sampleRate = rate
        }

        internal constructor(rate: Int) {
            dutyCycle = 4
            cyclePos = 0
            channel = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRate = rate
            amplitude = 32
            counterSweep = 0
        }

        fun setSampleRate(sr: Int) {
            sampleRate = sr
        }

        fun setDutyCycle(duty: Int) {
            when (duty) {
                0 -> dutyCycle = 1
                1 -> dutyCycle = 2
                2 -> dutyCycle = 4
                3 -> dutyCycle = 6
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
                    (256 * sampleRate) / frequency.toInt()
                } else {
                    65535
                }
                if (cycleLength == 0) cycleLength = 1
            } catch (_: ArithmeticException) {
            }
        }

        fun setChannel(chan: Int) {
            channel = chan
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

        fun setLength3(gbLength: Int) {
            totalLength = if (gbLength == -1) {
                -1
            } else {
                (256 - gbLength) / 4
            }
        }

        fun setVolume3(volume: Int) {
            when (volume) {
                0 -> amplitude = 0
                1 -> amplitude = 32
                2 -> amplitude = 16
                3 -> amplitude = 8
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
                        value = if (((8 * cyclePos) / cycleLength) >= dutyCycle) {
                            amplitude
                        } else {
                            -amplitude
                        }
                    }
                    
                    if ((channel and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + value).toByte()
                    if ((channel and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + value).toByte()
                    if ((channel and CHAN_MONO) != 0) b[r] = (b[r] + value).toByte()

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

        @set:JvmName("updateChannelWave")
        var channel: Int

        @set:JvmName("updateSampleRateWave")
        var sampleRate: Int
        var volumeShift: Int = 0

        var waveform: ByteArray = ByteArray(32)

        internal constructor(waveLength: Int, ampl: Int, duty: Int, chan: Int, rate: Int) {
            cycleLength = waveLength
            amplitude = ampl
            cyclePos = 0
            channel = chan
            sampleRate = rate
        }

        internal constructor(rate: Int) {
            cyclePos = 0
            channel = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRate = rate
            amplitude = 32
        }

        fun setSampleRate(sr: Int) {
            sampleRate = sr
        }

        fun setFrequency(gbFrequency: Int) {
            val frequency = (65536f / (2048 - gbFrequency).toFloat()).toInt().toFloat()
            cycleLength = ((256f * sampleRate) / frequency).toInt()
            if (cycleLength == 0) cycleLength = 1
        }

        fun setChannel(chan: Int) {
            channel = chan
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
                    value = BytesOperation.unsign(waveform[samplePos % 32]).toInt() shr volumeShift shl 1

                    if ((channel and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + value).toByte()
                    if ((channel and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + value).toByte()
                    if ((channel and CHAN_MONO) != 0) b[r] = (b[r] + value).toByte()

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
        @set:JvmName("updateChannelNoise")
        var channel: Int
        @set:JvmName("updateSampleNoise")
        var sampleRate: Int
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
            channel = chan
            sampleRate = rate
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
            channel = CHAN_LEFT or CHAN_RIGHT
            cycleLength = 2
            totalLength = 0
            sampleRate = rate
            amplitude = 32

            randomValues = BooleanArray(32767)

            val rand = Random()


            for (r in 0..32766) {
                randomValues!![r] = rand.nextBoolean()
            }

            cycleOffset = 0
        }


        fun setSampleRate(sr: Int) {
            sampleRate = sr
        }

        fun setChannel(chan: Int) {
            channel = chan
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
            var dividingRatio = dividingRatio
            this.dividingRatio = dividingRatio.toInt()
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

            if (dividingRatio == 0f) dividingRatio = 0.5f

            finalFreq = ((4194304 / 8 / dividingRatio).toInt()) shr (shiftClockFreq + 1)
        }

        fun play(b: ByteArray, length: Int, offset: Int) {
            var value: Int

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


                val step = ((finalFreq) / (sampleRate shr 8))

                for (r in offset..<offset + length) {
                    val value = randomValues!![((cycleOffset) + (cyclePos shr 8)) and 0x7FFF]
                    val v = if (value) (amplitude / 2) else (-amplitude / 2)

                    if ((channel and CHAN_LEFT) != 0) b[r * 2] = (b[r * 2] + v).toByte()
                    if ((channel and CHAN_RIGHT) != 0) b[r * 2 + 1] = (b[r * 2 + 1] + v).toByte()
                    if ((channel and CHAN_MONO) != 0) b[r] = (b[r] + v).toByte()

                    cyclePos = (cyclePos + step) % cycleLength
                }
            }
        }
    }

    fun setSpeed(i: Int) {
        speed.setSpeed(i)
    }
}

const val CHAN_LEFT: Int = 1
const val CHAN_RIGHT: Int = 2
const val CHAN_MONO: Int = 4

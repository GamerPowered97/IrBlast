package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object SonyEncoder {
    private const val FREQUENCY_HZ = 40000
    private const val LEADER_MARK = 2400
    private const val LEADER_SPACE = 600
    private const val ZERO_MARK = 600
    private const val ONE_MARK = 1200
    private const val BIT_SPACE = 600

    private const val FRAME_PERIOD_US = 45000 // 45ms SIRC standard message cycle time

    fun encode(bitLength: Int, device: Int, subDevice: Int = -1, function: Int, repeats: Int = 1): IrSignal {
        val singleFrame = encodeSingleFrame(bitLength, device, subDevice, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = singleFrame,
                protocol = "SONY$bitLength"
            )
        }

        val frameDuration = singleFrame.sum()
        val interFrameGap = maxOf(BIT_SPACE, FRAME_PERIOD_US - frameDuration)

        val fullPattern = ArrayList<Int>(singleFrame.size * repeats)
        for (r in 0 until repeats) {
            for (idx in 0 until singleFrame.size - 1) {
                fullPattern.add(singleFrame[idx])
            }
            if (r < repeats - 1) {
                fullPattern.add(interFrameGap)
            } else {
                fullPattern.add(singleFrame.last())
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = fullPattern.toIntArray(),
            protocol = "SONY$bitLength"
        )
    }

    private fun encodeSingleFrame(bitLength: Int, device: Int, subDevice: Int, function: Int): IntArray {
        val pattern = ArrayList<Int>()

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // 1. Function / command (7 bits, LSB first)
        val func = function and 0x7F
        for (i in 0 until 7) {
            val bit = (func ushr i) and 1
            pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
            pattern.add(BIT_SPACE)
        }

        when (bitLength) {
            12 -> {
                // 5-bit device
                val dev = device and 0x1F
                for (i in 0 until 5) {
                    val bit = (dev ushr i) and 1
                    pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
                    pattern.add(BIT_SPACE)
                }
            }
            15 -> {
                // 8-bit device
                val dev = device and 0xFF
                for (i in 0 until 8) {
                    val bit = (dev ushr i) and 1
                    pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
                    pattern.add(BIT_SPACE)
                }
            }
            20 -> {
                // 5-bit device + 8-bit extended/subdevice
                val dev = device and 0x1F
                for (i in 0 until 5) {
                    val bit = (dev ushr i) and 1
                    pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
                    pattern.add(BIT_SPACE)
                }
                val sub = (if (subDevice >= 0) subDevice else 0) and 0xFF
                for (i in 0 until 8) {
                    val bit = (sub ushr i) and 1
                    pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
                    pattern.add(BIT_SPACE)
                }
            }
            else -> {
                // Default to 12-bit
                val dev = device and 0x1F
                for (i in 0 until 5) {
                    val bit = (dev ushr i) and 1
                    pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
                    pattern.add(BIT_SPACE)
                }
            }
        }

        return pattern.toIntArray()
    }
}

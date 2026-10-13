package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object SonyEncoder {
    private const val FREQUENCY_HZ = 40000
    private const val LEADER_MARK = 2400
    private const val LEADER_SPACE = 600
    private const val ZERO_MARK = 600
    private const val ONE_MARK = 1200
    private const val BIT_SPACE = 600

    fun encode(bitLength: Int, device: Int, subDevice: Int = -1, function: Int): IrSignal {
        val pattern = ArrayList<Int>()

        // SIRC standard sends 3 repeated frames with ~25-45ms space between frames
        // A single frame:
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

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = pattern.toIntArray(),
            protocol = "SONY$bitLength"
        )
    }
}

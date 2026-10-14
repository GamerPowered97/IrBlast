package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Dish Network / Dishplayer infrared protocol encoder.
 * Carrier frequency: 56,000 Hz.
 * Timings: T = 400 µs.
 * Leader: 1*T mark (400 µs), 15*T space (6000 µs).
 * Bit 0: 1*T mark (400 µs), 4*T space (1600 µs).
 * Bit 1: 1*T mark (400 µs), 7*T space (2800 µs).
 * Stop mark: 400 µs.
 * Trailing space: 6000 µs.
 * Total frame period: ~40-45 ms.
 *
 * 16 data bits, transmitted LSB first:
 * - 6 bits: Function (F0..F5)
 * - 5 bits: Device / Address (D0..D4)
 * - 5 bits: Subdevice / Unit code (S0..S4)
 */
object DishNetworkEncoder {
    private const val FREQUENCY_HZ = 56000
    private const val T_US = 400
    private const val LEADER_MARK = T_US          // 400 µs
    private const val LEADER_SPACE = 15 * T_US    // 6000 µs
    private const val BIT_MARK = T_US             // 400 µs
    private const val ZERO_SPACE = 4 * T_US       // 1600 µs
    private const val ONE_SPACE = 7 * T_US        // 2800 µs
    private const val STOP_MARK = T_US            // 400 µs
    private const val TRAILING_SPACE = 15 * T_US  // 6000 µs

    fun encode(device: Int, subDevice: Int = -1, function: Int, repeats: Int = 3): IrSignal {
        val singleFrame = encodeSingleFrame(device, subDevice, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = singleFrame,
                protocol = "DISH_NETWORK"
            )
        }

        val fullPattern = ArrayList<Int>(singleFrame.size * repeats)
        for (r in 0 until repeats) {
            for (p in singleFrame) {
                fullPattern.add(p)
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = fullPattern.toIntArray(),
            protocol = "DISH_NETWORK"
        )
    }

    private fun encodeSingleFrame(device: Int, subDevice: Int, function: Int): IntArray {
        val func = function and 0x3F
        val dev = device and 0x1F
        val sub = (if (subDevice >= 0) subDevice else 0) and 0x1F

        // 2 (leader) + 16 * 2 (bits) + 1 (stop mark) + 1 (trailing space) = 36 elements
        val pattern = ArrayList<Int>(36)

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // 1. Function (6 bits, LSB first)
        appendBits(pattern, func, 6)

        // 2. Device (5 bits, LSB first)
        appendBits(pattern, dev, 5)

        // 3. Subdevice (5 bits, LSB first)
        appendBits(pattern, sub, 5)

        // Stop mark and trailing space
        pattern.add(STOP_MARK)
        pattern.add(TRAILING_SPACE)

        return pattern.toIntArray()
    }

    private fun appendBits(pattern: ArrayList<Int>, value: Int, bitCount: Int) {
        for (i in 0 until bitCount) {
            val bit = (value ushr i) and 1
            pattern.add(BIT_MARK)
            pattern.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
        }
    }
}

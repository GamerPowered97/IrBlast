package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * RCA and RCA-38 protocol encoder.
 * Standard RCA carrier: 56,000 Hz.
 * RCA-38 carrier: 38,000 Hz.
 *
 * Timings: T = 500 µs.
 * Leader: 8*T mark (4000 µs), 8*T space (4000 µs).
 * Bit 0: 1*T mark (500 µs), 2*T space (1000 µs).
 * Bit 1: 1*T mark (500 µs), 4*T space (2000 µs).
 * Stop mark: 1*T mark (500 µs).
 * Trailing space: 16*T space (8000 µs).
 * Total frame duration: ~64 ms.
 *
 * 24 data bits, transmitted LSB first:
 * - 4 bits: Device (D0..D3)
 * - 8 bits: Function (F0..F7)
 * - 4 bits: Inverted Device (~D0..~D3)
 * - 8 bits: Inverted Function (~F0..~F7)
 */
object RcaEncoder {
    private const val FREQUENCY_56K = 56000
    private const val FREQUENCY_38K = 38000

    private const val T_US = 500
    private const val LEADER_MARK = 8 * T_US     // 4000 µs
    private const val LEADER_SPACE = 8 * T_US    // 4000 µs
    private const val BIT_MARK = T_US            // 500 µs
    private const val ZERO_SPACE = 2 * T_US      // 1000 µs
    private const val ONE_SPACE = 4 * T_US       // 2000 µs
    private const val STOP_MARK = T_US           // 500 µs
    private const val TRAILING_SPACE = 8000      // 8000 µs
    private const val FRAME_PERIOD_US = 64000

    fun encode(device: Int, function: Int, is38k: Boolean = false, repeats: Int = 1): IrSignal {
        val freq = if (is38k) FREQUENCY_38K else FREQUENCY_56K
        val protoName = if (is38k) "RCA-38" else "RCA"

        val singleFrame = encodeSingleFrame(device, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = freq,
                patternMicros = singleFrame,
                protocol = protoName
            )
        }

        val frameDuration = singleFrame.sum()
        val interFrameGap = maxOf(TRAILING_SPACE, FRAME_PERIOD_US - frameDuration)

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
            frequencyHz = freq,
            patternMicros = fullPattern.toIntArray(),
            protocol = protoName
        )
    }

    private fun encodeSingleFrame(device: Int, function: Int): IntArray {
        val dev = device and 0x0F
        val func = function and 0xFF
        val devInv = dev.inv() and 0x0F
        val funcInv = func.inv() and 0xFF

        // 2 (leader) + 24 * 2 (bits) + 1 (stop mark) + 1 (trailing space) = 52 elements
        val pattern = ArrayList<Int>(52)

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // 1. Device (4 bits, LSB first)
        appendBits(pattern, dev, 4)

        // 2. Function (8 bits, LSB first)
        appendBits(pattern, func, 8)

        // 3. Inverted Device (4 bits, LSB first)
        appendBits(pattern, devInv, 4)

        // 4. Inverted Function (8 bits, LSB first)
        appendBits(pattern, funcInv, 8)

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

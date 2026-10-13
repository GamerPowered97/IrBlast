package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Sharp / Denon protocol encoder.
 * Carrier frequency: 38,000 Hz.
 * Timings: T = 264 µs.
 * Bit 0: 1*T mark (264 µs), 3*T space (792 µs).
 * Bit 1: 1*T mark (264 µs), 7*T space (1848 µs).
 * Stop mark: 264 µs.
 * Trailing space / inter-frame gap: 40,000 µs.
 *
 * Each transmission consists of 2 frames (normal + inverted):
 * Frame 1: 5-bit Device + 8-bit Function + 1 (Expansion) + 0 (Check) = 15 bits
 * Frame 2: 5-bit Device + 8-bit Inverted Function + 0 + 1 = 15 bits
 * Total 64 pulse elements (even length).
 */
object SharpEncoder {
    private const val FREQUENCY_HZ = 38000
    private const val T_US = 264
    private const val BIT_MARK = T_US          // 264 µs
    private const val ZERO_SPACE = 3 * T_US    // 792 µs
    private const val ONE_SPACE = 7 * T_US     // 1848 µs
    private const val STOP_MARK = T_US         // 264 µs
    private const val TRAILING_SPACE = 40000   // 40,000 µs

    fun encode(device: Int, function: Int, repeats: Int = 1): IrSignal {
        val dev = device and 0x1F
        val func = function and 0xFF
        val funcInv = func.inv() and 0xFF

        val frame1Bits = IntArray(15)
        val frame2Bits = IntArray(15)

        // 5-bit device (LSB first)
        for (i in 0 until 5) {
            val bit = (dev ushr i) and 1
            frame1Bits[i] = bit
            frame2Bits[i] = bit
        }

        // 8-bit function (LSB first)
        for (i in 0 until 8) {
            frame1Bits[5 + i] = (func ushr i) and 1
            frame2Bits[5 + i] = (funcInv ushr i) and 1
        }

        // Frame 1 expansion & check
        frame1Bits[13] = 1
        frame1Bits[14] = 0

        // Frame 2 inverted expansion & check
        frame2Bits[13] = 0
        frame2Bits[14] = 1

        val pairPattern = ArrayList<Int>(64)
        addFrame(pairPattern, frame1Bits)
        addFrame(pairPattern, frame2Bits)

        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = pairPattern.toIntArray(),
                protocol = "SHARP"
            )
        }

        val fullPattern = ArrayList<Int>(pairPattern.size * repeats)
        for (r in 0 until repeats) {
            for (p in pairPattern) {
                fullPattern.add(p)
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = fullPattern.toIntArray(),
            protocol = "SHARP"
        )
    }

    private fun addFrame(pattern: ArrayList<Int>, bits: IntArray) {
        for (b in bits) {
            pattern.add(BIT_MARK)
            pattern.add(if (b == 1) ONE_SPACE else ZERO_SPACE)
        }
        pattern.add(STOP_MARK)
        pattern.add(TRAILING_SPACE)
    }
}

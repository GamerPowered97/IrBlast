package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Aiwa Protocol Encoder.
 *
 * Carrier: 38,000 Hz (38 kHz)
 * Base unit: 550 µs
 *
 * Leader: 8800 µs mark, 4400 µs space
 * Bit '0': 550 µs mark, 550 µs space
 * Bit '1': 550 µs mark, 1650 µs space
 *
 * Frame (LSB first):
 * - Device: 8 bits
 * - SubDevice: 5 bits (default 0 if negative)
 * - ~Device: 8 bits inverted
 * - ~SubDevice: 5 bits inverted
 * - Function: 8 bits
 * - ~Function: 8 bits inverted
 * - Stop pulse: 550 µs mark, 42,000 µs gap
 */
object AiwaEncoder {
    private const val CARRIER_HZ = 38000
    private const val LEADER_MARK_US = 8800
    private const val LEADER_SPACE_US = 4400
    private const val BIT_MARK_US = 550
    private const val BIT_SPACE_0_US = 550
    private const val BIT_SPACE_1_US = 1650
    private const val STOP_MARK_US = 550
    private const val GAP_US = 42000

    fun encode(device: Int, subDevice: Int = 0, function: Int, repeats: Int = 2): IrSignal {
        val dev = device and 0xFF
        val sub = if (subDevice >= 0) subDevice and 0x1F else 0
        val func = function and 0xFF

        val devInv = dev.inv() and 0xFF
        val subInv = sub.inv() and 0x1F
        val funcInv = func.inv() and 0xFF

        val singleFrame = mutableListOf<Int>()

        // Leader
        singleFrame.add(LEADER_MARK_US)
        singleFrame.add(LEADER_SPACE_US)

        // Helper to append bits (LSB first)
        fun appendBits(value: Int, bitCount: Int) {
            for (i in 0 until bitCount) {
                val bit = (value shr i) and 1
                singleFrame.add(BIT_MARK_US)
                singleFrame.add(if (bit == 1) BIT_SPACE_1_US else BIT_SPACE_0_US)
            }
        }

        appendBits(dev, 8)
        appendBits(sub, 5)
        appendBits(devInv, 8)
        appendBits(subInv, 5)
        appendBits(func, 8)
        appendBits(funcInv, 8)

        // Stop mark + gap
        singleFrame.add(STOP_MARK_US)
        singleFrame.add(GAP_US)

        val fullPattern = mutableListOf<Int>()
        val count = maxOf(1, repeats)
        repeat(count) {
            fullPattern.addAll(singleFrame)
        }

        return IrSignal(
            frequencyHz = CARRIER_HZ,
            patternMicros = fullPattern.toIntArray()
        )
    }
}

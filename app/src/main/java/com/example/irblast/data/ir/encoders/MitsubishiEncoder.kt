package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Mitsubishi IR Protocol Encoder.
 *
 * Carrier: 38,000 Hz (Hardware-safe carrier matching 38kHz IR receivers)
 * Base unit: 300 µs
 *
 * Encoding:
 * - Bit '1': 300 µs mark + 900 µs space
 * - Bit '0': 300 µs mark + 2100 µs space
 *
 * Payload: 8-bit Device, 8-bit Function (LSB first)
 * Followed by stop mark (300 µs) and inter-frame gap (24,000 µs).
 */
object MitsubishiEncoder {
    private const val CARRIER_HZ = 38000
    private const val MARK_US = 300
    private const val SPACE_1_US = 900
    private const val SPACE_0_US = 2100
    private const val GAP_US = 24000

    fun encode(device: Int, function: Int, repeats: Int = 2): IrSignal {
        val dev = device and 0xFF
        val func = function and 0xFF

        val singleFrame = mutableListOf<Int>()

        // 8-bit Device (LSB first)
        for (i in 0..7) {
            val bit = (dev shr i) and 1
            singleFrame.add(MARK_US)
            singleFrame.add(if (bit == 1) SPACE_1_US else SPACE_0_US)
        }

        // 8-bit Function (LSB first)
        for (i in 0..7) {
            val bit = (func shr i) and 1
            singleFrame.add(MARK_US)
            singleFrame.add(if (bit == 1) SPACE_1_US else SPACE_0_US)
        }

        // Stop mark + gap
        singleFrame.add(MARK_US)
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

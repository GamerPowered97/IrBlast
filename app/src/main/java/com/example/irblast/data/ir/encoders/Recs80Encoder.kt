package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * RECS80 (Philips Remote Control Electronic System 80) Protocol Encoder.
 *
 * Carrier: 38,000 Hz (38 kHz)
 * Base time unit (t): 158 µs
 *
 * Encoding: Pulse Distance Modulation
 * - Logical '0': 1 unit mark (158 µs) + 31 units space (4,898 µs)
 * - Logical '1': 1 unit mark (158 µs) + 47 units space (7,426 µs)
 *
 * Frame structure (MSB first):
 * - Start bit (value 1): mark 158 µs, space 7,426 µs
 * - Toggle bit T (1 bit, default 0): mark 158 µs, space 4,898 µs
 * - Device address D (3 bits, MSB first, range 0..7)
 * - Function code F (6 bits, MSB first, range 0..63)
 * - Stop pulse + inter-frame gap: mark 158 µs, space 45,000 µs
 *
 * Total per frame: 11 data bits + 1 stop pulse = 24 elements (even, alternating mark/space).
 */
object Recs80Encoder {
    private const val CARRIER_HZ = 38000
    private const val UNIT_US = 158
    private const val SPACE_0_US = 31 * UNIT_US // 4898 µs
    private const val SPACE_1_US = 47 * UNIT_US // 7426 µs
    private const val GAP_US = 45000 // 45 ms inter-frame gap

    fun encode(device: Int, function: Int, repeats: Int = 2): IrSignal {
        val dev = device and 0x07 // 3 bits
        val func = function and 0x3F // 6 bits

        val singleFrame = mutableListOf<Int>()

        // 1. Start bit: always '1' (mark 158, space 7426)
        singleFrame.add(UNIT_US)
        singleFrame.add(SPACE_1_US)

        // 2. Toggle bit: default 0 (mark 158, space 4898)
        singleFrame.add(UNIT_US)
        singleFrame.add(SPACE_0_US)

        // 3. Device address: 3 bits (MSB first, bits 2..0)
        for (i in 2 downTo 0) {
            val bit = (dev shr i) and 1
            singleFrame.add(UNIT_US)
            singleFrame.add(if (bit == 1) SPACE_1_US else SPACE_0_US)
        }

        // 4. Function: 6 bits (MSB first, bits 5..0)
        for (i in 5 downTo 0) {
            val bit = (func shr i) and 1
            singleFrame.add(UNIT_US)
            singleFrame.add(if (bit == 1) SPACE_1_US else SPACE_0_US)
        }

        // 5. Stop pulse + inter-frame gap
        singleFrame.add(UNIT_US)
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

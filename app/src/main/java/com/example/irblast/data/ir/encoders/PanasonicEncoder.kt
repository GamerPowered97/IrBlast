package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Panasonic / Kaseikyo 48-bit protocol encoder.
 * Carrier frequency: 37,000 Hz.
 * Timings: T = 432 µs.
 * Leader: 8*T mark (3456 µs), 4*T space (1728 µs).
 * Bit 0: 1*T mark (432 µs), 1*T space (432 µs).
 * Bit 1: 1*T mark (432 µs), 3*T space (1296 µs).
 * Stop mark: 432 µs.
 * Trailing space: ~74,000 µs (total ~130ms frame period).
 *
 * 48 data bits (6 bytes, LSB first):
 * - Byte 0: Manufacturer ID 1 (0x02)
 * - Byte 1: Manufacturer ID 2 (0x20)
 * - Byte 2: Device (device and 0xFF)
 * - Byte 3: SubDevice (subDevice and 0xFF, or 0)
 * - Byte 4: Function (function and 0xFF)
 * - Byte 5: Checksum (Device ^ SubDevice ^ Function)
 */
object PanasonicEncoder {
    private const val FREQUENCY_HZ = 37000
    private const val T_US = 432
    private const val LEADER_MARK = 8 * T_US     // 3456 µs
    private const val LEADER_SPACE = 4 * T_US    // 1728 µs
    private const val BIT_MARK = T_US            // 432 µs
    private const val ZERO_SPACE = T_US          // 432 µs
    private const val ONE_SPACE = 3 * T_US       // 1296 µs
    private const val STOP_MARK = T_US           // 432 µs
    private const val TRAILING_SPACE = 74000     // 74,000 µs

    fun encode(device: Int, subDevice: Int = -1, function: Int, repeats: Int = 1): IrSignal {
        val singleFrame = encodeSingleFrame(device, subDevice, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = singleFrame,
                protocol = "PANASONIC"
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
            protocol = "PANASONIC"
        )
    }

    private fun encodeSingleFrame(device: Int, subDevice: Int, function: Int): IntArray {
        val dev = device and 0xFF
        val sub = (if (subDevice >= 0) subDevice else 0) and 0xFF
        val func = function and 0xFF
        val checksum = (dev xor sub xor func) and 0xFF

        val bytes = intArrayOf(0x02, 0x20, dev, sub, func, checksum)
        // 2 (leader) + 48 * 2 (bits) + 1 (stop mark) + 1 (trailing space) = 100 elements
        val pattern = ArrayList<Int>(100)

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        for (byteVal in bytes) {
            for (bitIdx in 0..7) {
                val bit = (byteVal ushr bitIdx) and 1
                pattern.add(BIT_MARK)
                pattern.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
            }
        }

        pattern.add(STOP_MARK)
        pattern.add(TRAILING_SPACE)

        return pattern.toIntArray()
    }
}

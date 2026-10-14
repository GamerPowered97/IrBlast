package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

/**
 * Fujitsu 48-bit protocol encoder (Kaseikyo family OEM 0x14, 0x63).
 * Carrier frequency: 37,000 Hz.
 * Timings: T = 432 µs.
 * Leader: 8*T mark (3456 µs), 4*T space (1728 µs).
 * Bit 0: 1*T mark (432 µs), 1*T space (432 µs).
 * Bit 1: 1*T mark (432 µs), 3*T space (1296 µs).
 * Stop mark: 432 µs.
 * Trailing space: 110*T space (47,520 µs).
 *
 * 48 data bits (6 bytes, LSB first):
 * - Byte 0: Manufacturer ID 1 (0x14)
 * - Byte 1: Manufacturer ID 2 (0x63)
 * - Byte 2: Device
 * - Byte 3: SubDevice (or inverted device if subdevice not specified)
 * - Byte 4: Function
 * - Byte 5: Checksum / Inverted function
 */
object FujitsuEncoder {
    private const val FREQUENCY_HZ = 37000
    private const val T_US = 432
    private const val LEADER_MARK = 8 * T_US     // 3456 µs
    private const val LEADER_SPACE = 4 * T_US    // 1728 µs
    private const val BIT_MARK = T_US            // 432 µs
    private const val ZERO_SPACE = T_US          // 432 µs
    private const val ONE_SPACE = 3 * T_US       // 1296 µs
    private const val STOP_MARK = T_US           // 432 µs
    private const val TRAILING_SPACE = 47520     // 47,520 µs

    fun encode(device: Int, subDevice: Int = -1, function: Int, repeats: Int = 1): IrSignal {
        val singleFrame = encodeSingleFrame(device, subDevice, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = singleFrame,
                protocol = "FUJITSU"
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
            protocol = "FUJITSU"
        )
    }

    private fun encodeSingleFrame(device: Int, subDevice: Int, function: Int): IntArray {
        val dev = device and 0xFF
        val sub = if (subDevice >= 0) subDevice and 0xFF else (dev.inv() and 0xFF)
        val func = function and 0xFF
        val checksum = if (subDevice >= 0) (dev xor sub xor func) and 0xFF else (func.inv() and 0xFF)

        val bytes = intArrayOf(0x14, 0x63, dev, sub, func, checksum)
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

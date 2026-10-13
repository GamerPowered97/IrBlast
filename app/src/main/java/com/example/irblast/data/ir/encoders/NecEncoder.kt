package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object NecEncoder {
    private const val FREQUENCY_HZ = 38000
    private const val LEADER_MARK = 9000
    private const val LEADER_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1690
    private const val STOP_MARK = 560
    private const val TRAILING_SPACE = 40500

    private const val REPEAT_LEADER_MARK = 9000
    private const val REPEAT_LEADER_SPACE = 2250
    private const val REPEAT_STOP_MARK = 560
    private const val REPEAT_TRAILING_SPACE = 96190

    fun encode(device: Int, subDevice: Int = -1, function: Int, repeats: Int = 1): IrSignal {
        val devByte = device and 0xFF
        val subByte = if (subDevice >= 0) {
            subDevice and 0xFF
        } else {
            devByte.inv() and 0xFF
        }
        val funcByte = function and 0xFF
        val funcInvByte = funcByte.inv() and 0xFF

        val bytes = intArrayOf(devByte, subByte, funcByte, funcInvByte)
        // 2 (leader) + 32 * 2 (bits) + 1 (stop) + 1 (trailing space) = 68 elements
        val pattern = ArrayList<Int>(68 + maxOf(0, repeats - 1) * 4)

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        for (byteVal in bytes) {
            for (bitIdx in 0..7) {
                val bit = (byteVal ushr bitIdx) and 1
                pattern.add(BIT_MARK)
                if (bit == 1) {
                    pattern.add(ONE_SPACE)
                } else {
                    pattern.add(ZERO_SPACE)
                }
            }
        }

        pattern.add(STOP_MARK)
        pattern.add(TRAILING_SPACE)

        if (repeats > 1) {
            for (r in 1 until repeats) {
                pattern.add(REPEAT_LEADER_MARK)
                pattern.add(REPEAT_LEADER_SPACE)
                pattern.add(REPEAT_STOP_MARK)
                pattern.add(REPEAT_TRAILING_SPACE)
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = pattern.toIntArray(),
            protocol = "NEC"
        )
    }
}

package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object JvcEncoder {
    private const val FREQUENCY_HZ = 38000
    private const val LEADER_MARK = 8400
    private const val LEADER_SPACE = 4200
    private const val BIT_MARK = 526
    private const val ZERO_SPACE = 526
    private const val ONE_SPACE = 1578
    private const val STOP_MARK = 526

    fun encode(device: Int, function: Int): IrSignal {
        val pattern = ArrayList<Int>(35)

        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // 8-bit device (LSB first)
        val dev = device and 0xFF
        for (i in 0 until 8) {
            val bit = (dev ushr i) and 1
            pattern.add(BIT_MARK)
            pattern.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
        }

        // 8-bit function (LSB first)
        val func = function and 0xFF
        for (i in 0 until 8) {
            val bit = (func ushr i) and 1
            pattern.add(BIT_MARK)
            pattern.add(if (bit == 1) ONE_SPACE else ZERO_SPACE)
        }

        pattern.add(STOP_MARK)

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = pattern.toIntArray(),
            protocol = "JVC"
        )
    }
}

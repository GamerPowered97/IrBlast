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
    private const val TRAILING_SPACE = 50000

    fun encode(device: Int, function: Int, repeats: Int = 1): IrSignal {
        val singleFrame = encodeSingleFrame(device, function)
        if (repeats <= 1) {
            return IrSignal(
                frequencyHz = FREQUENCY_HZ,
                patternMicros = singleFrame,
                protocol = "JVC"
            )
        }

        val fullPattern = ArrayList<Int>(singleFrame.size * repeats)
        for (r in 0 until repeats) {
            for (pulse in singleFrame) {
                fullPattern.add(pulse)
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = fullPattern.toIntArray(),
            protocol = "JVC"
        )
    }

    private fun encodeSingleFrame(device: Int, function: Int): IntArray {
        val pattern = ArrayList<Int>(36)

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
        pattern.add(TRAILING_SPACE)

        return pattern.toIntArray()
    }
}

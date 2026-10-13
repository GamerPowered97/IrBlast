package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object Rc6Encoder {
    private const val FREQUENCY_HZ = 36000
    private const val T_US = 444
    private const val LEADER_MARK = 2666
    private const val LEADER_SPACE = 889

    fun encode(device: Int, function: Int, toggle: Boolean = false): IrSignal {
        val pattern = ArrayList<Int>()
        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // Half-bits representation: true = MARK, false = SPACE
        // Start bit: 1 -> [true, false]
        val halfBits = ArrayList<Pair<Boolean, Int>>() // Pair<isMark, durationUs>

        // Start bit
        halfBits.add(true to T_US)
        halfBits.add(false to T_US)

        // Mode 0 (3 bits = 0, 0, 0) -> each 0 is [false, true]
        for (i in 0 until 3) {
            halfBits.add(false to T_US)
            halfBits.add(true to T_US)
        }

        // Trailer / Toggle bit (double time: 2 * T_US = 889 us)
        if (toggle) {
            halfBits.add(true to (2 * T_US))
            halfBits.add(false to (2 * T_US))
        } else {
            halfBits.add(false to (2 * T_US))
            halfBits.add(true to (2 * T_US))
        }

        // Address: 8 bits MSB first
        val addr = device and 0xFF
        for (i in 7 downTo 0) {
            val bit = (addr ushr i) and 1
            if (bit == 1) {
                halfBits.add(true to T_US)
                halfBits.add(false to T_US)
            } else {
                halfBits.add(false to T_US)
                halfBits.add(true to T_US)
            }
        }

        // Command: 8 bits MSB first
        val cmd = function and 0xFF
        for (i in 7 downTo 0) {
            val bit = (cmd ushr i) and 1
            if (bit == 1) {
                halfBits.add(true to T_US)
                halfBits.add(false to T_US)
            } else {
                halfBits.add(false to T_US)
                halfBits.add(true to T_US)
            }
        }

        // Convert half-bits to alternating mark-space array starting with MARK
        var currentLevel = halfBits[0].first
        var currentDuration = 0

        for (item in halfBits) {
            if (item.first == currentLevel) {
                currentDuration += item.second
            } else {
                pattern.add(currentDuration)
                currentLevel = item.first
                currentDuration = item.second
            }
        }
        if (currentDuration > 0) {
            pattern.add(currentDuration)
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = pattern.toIntArray(),
            protocol = "RC6"
        )
    }
}

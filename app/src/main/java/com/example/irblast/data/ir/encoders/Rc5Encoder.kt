package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object Rc5Encoder {
    private const val FREQUENCY_HZ = 36000
    private const val HALF_BIT_US = 889
    private const val INTER_FRAME_GAP_US = 88000

    fun encode(device: Int, function: Int, toggle: Boolean = false, repeats: Int = 1): IrSignal {
        if (repeats <= 1) {
            val singleFrame = encodeSingleFrame(device, function, toggle, isLastFrame = true)
            return IrSignal(FREQUENCY_HZ, singleFrame, "RC5")
        }

        val fullPattern = ArrayList<Int>()
        for (r in 0 until repeats) {
            val isLast = (r == repeats - 1)
            val frame = encodeSingleFrame(device, function, toggle, isLastFrame = isLast)
            for (p in frame) {
                fullPattern.add(p)
            }
        }

        return IrSignal(
            frequencyHz = FREQUENCY_HZ,
            patternMicros = fullPattern.toIntArray(),
            protocol = "RC5"
        )
    }

    private fun encodeSingleFrame(device: Int, function: Int, toggle: Boolean, isLastFrame: Boolean): IntArray {
        // RC5 bits:
        // S1: 1
        // S2 (Field bit): 1 if function < 64 else 0
        // Toggle: 1 if toggle else 0
        // Address: 5 bits MSB first
        // Command: 6 bits MSB first (function and 0x3F)

        val s1 = 1
        val s2 = if (function < 64) 1 else 0
        val t = if (toggle) 1 else 0
        val addr = device and 0x1F
        val cmd = function and 0x3F

        val bits = IntArray(14)
        bits[0] = s1
        bits[1] = s2
        bits[2] = t

        for (i in 0 until 5) {
            bits[3 + i] = (addr ushr (4 - i)) and 1
        }
        for (i in 0 until 6) {
            bits[8 + i] = (cmd ushr (5 - i)) and 1
        }

        // Each bit has 2 half-bits:
        // Bit 1: [Space, Mark] (false, true)
        // Bit 0: [Mark, Space] (true, false)
        val halfBits = ArrayList<Boolean>()
        for (b in bits) {
            if (b == 1) {
                halfBits.add(false)
                halfBits.add(true)
            } else {
                halfBits.add(true)
                halfBits.add(false)
            }
        }

        // The signal starts at the first MARK (true)
        val firstMarkIndex = halfBits.indexOfFirst { it }
        if (firstMarkIndex == -1) {
            return intArrayOf()
        }

        val pattern = ArrayList<Int>()
        var currentLevel = true
        var currentDuration = 0

        for (i in firstMarkIndex until halfBits.size) {
            val level = halfBits[i]
            if (level == currentLevel) {
                currentDuration += HALF_BIT_US
            } else {
                pattern.add(currentDuration)
                currentLevel = level
                currentDuration = HALF_BIT_US
            }
        }
        if (currentDuration > 0) {
            pattern.add(currentDuration)
        }

        // Ensure even array length by appending an inter-message space gap (88,000 µs) if ending on a mark
        if (pattern.size % 2 != 0) {
            pattern.add(INTER_FRAME_GAP_US)
        } else if (!isLastFrame) {
            pattern[pattern.size - 1] = INTER_FRAME_GAP_US
        }

        return pattern.toIntArray()
    }
}

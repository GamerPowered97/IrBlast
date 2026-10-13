package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object ProntoHexParser {
    fun parse(prontoStr: String): Result<IrSignal> = runCatching {
        val tokens = prontoStr.trim()
            .split(Regex("[\\s,]+"))
            .filter { it.isNotEmpty() }

        require(tokens.size >= 4) { "Pronto hex must contain at least 4 preamble words" }

        val words = tokens.map { it.toInt(16) }
        val format = words[0]
        require(format == 0) { "Only raw Pronto hex (format 0000) is supported" }

        val freqCode = words[1]
        require(freqCode > 0) { "Invalid frequency code in Pronto hex" }
        val frequencyHz = (1000000.0 / (freqCode * 0.241246)).toInt()

        val onceCount = words[2]
        val repeatCount = words[3]
        val totalExpectedPairs = onceCount + repeatCount
        val availablePairs = (words.size - 4) / 2

        require(availablePairs > 0) { "Pronto hex has no pulse pairs defined" }

        val pairsToRead = if (onceCount > 0) onceCount else repeatCount
        val actualPairs = minOf(pairsToRead, availablePairs)

        val pattern = ArrayList<Int>(actualPairs * 2)
        var idx = 4
        for (i in 0 until actualPairs) {
            val markCycles = words[idx++]
            val spaceCycles = words[idx++]

            val markMicros = ((markCycles * 1000000L) / frequencyHz).toInt()
            val spaceMicros = ((spaceCycles * 1000000L) / frequencyHz).toInt()

            pattern.add(markMicros)
            pattern.add(spaceMicros)
        }

        IrSignal(
            frequencyHz = frequencyHz,
            patternMicros = pattern.toIntArray(),
            protocol = "PRONTO_HEX"
        )
    }
}

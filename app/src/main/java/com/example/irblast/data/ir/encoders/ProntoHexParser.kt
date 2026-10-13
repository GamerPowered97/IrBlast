package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object ProntoHexParser {
    fun parse(prontoStr: String): Result<IrSignal> = runCatching {
        val tokens = prontoStr.trim()
            .split(Regex("[\\s,]+"))
            .filter { it.isNotEmpty() }

        require(tokens.size >= 4) { "Pronto hex must contain at least 4 preamble words" }

        val words = tokens.map { 
            val clean = it.removePrefix("0x").removePrefix("0X")
            clean.toInt(16) 
        }
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

        val pairsToRead = if (totalExpectedPairs > 0) {
            minOf(totalExpectedPairs, availablePairs)
        } else {
            availablePairs
        }

        val pattern = ArrayList<Int>(pairsToRead * 2)
        var idx = 4
        for (i in 0 until pairsToRead) {
            val markCycles = words[idx++]
            val spaceCycles = words[idx++]

            val markMicros = maxOf(1, ((markCycles * 1000000L) / frequencyHz).toInt())
            val spaceMicros = maxOf(1, ((spaceCycles * 1000000L) / frequencyHz).toInt())

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

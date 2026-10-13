package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object RawPatternParser {
    fun parse(rawString: String, frequencyHz: Int = 38000): Result<IrSignal> = runCatching {
        val tokens = rawString.trim()
            .split(Regex("[,\\s]+"))
            .filter { it.isNotEmpty() }

        require(tokens.isNotEmpty()) { "Raw pattern cannot be empty" }

        val timings = tokens.map { token ->
            val cleaned = token.trim().removeSuffix(",").removeSuffix(";")
            cleaned.toInt()
        }

        require(timings.size >= 2) { "Raw pattern must have at least 2 pulses (mark, space)" }
        for (t in timings) {
            require(t > 0) { "Pulse timing must be a positive integer, found: $t" }
        }

        val safeTimings = if (timings.size % 2 != 0) {
            timings + 20000
        } else {
            timings
        }

        IrSignal(
            frequencyHz = if (frequencyHz in 10000..100000) frequencyHz else 38000,
            patternMicros = safeTimings.toIntArray(),
            protocol = "RAW"
        )
    }
}

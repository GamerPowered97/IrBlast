package com.example.irblast.domain.model

data class IrSignal(
    val frequencyHz: Int,
    val patternMicros: IntArray,
    val protocol: String = "RAW"
) {
    val totalDurationMs: Long
        get() = patternMicros.sumOf { it.toLong() } / 1000L

    fun toTimingString(): String = patternMicros.joinToString(", ")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IrSignal
        return frequencyHz == other.frequencyHz &&
                patternMicros.contentEquals(other.patternMicros) &&
                protocol == other.protocol
    }

    override fun hashCode(): Int {
        var result = frequencyHz
        result = 31 * result + patternMicros.contentHashCode()
        result = 31 * result + protocol.hashCode()
        return result
    }
}

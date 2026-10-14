package com.example.irblast.domain.model

data class SavedRemote(
    val id: Long = 0,
    val name: String,
    val brand: String,
    val deviceType: String,
    val category: String,
    val icon: String = "📺",
    val createdAt: Long = System.currentTimeMillis()
)

data class SavedButton(
    val id: Long = 0,
    val remoteId: Long,
    val label: String,
    val icon: String = "",
    val frequencyHz: Int = 38000,
    val patternMicros: String,
    val protocol: String = "RAW",
    val device: Int = -1,
    val subdevice: Int = -1,
    val functionCode: Int = -1,
    val sortOrder: Int = 0,
    val colSpan: Int = 1
) {
    fun toPatternIntArray(): IntArray {
        if (patternMicros.isBlank()) return intArrayOf()
        return patternMicros.split(Regex("[,\\s;]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.trim().toIntOrNull() }
            .toIntArray()
    }
}

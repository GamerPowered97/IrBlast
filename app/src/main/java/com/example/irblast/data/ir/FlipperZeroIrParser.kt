package com.example.irblast.data.ir

import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.data.repository.mapFunctionToIcon
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote

data class FlipperSignal(
    val name: String,
    val frequencyHz: Int,
    val patternMicros: IntArray,
    val protocol: String = "RAW",
    val device: Int = -1,
    val subdevice: Int = -1,
    val functionCode: Int = -1
)

data class FlipperRemote(
    val name: String,
    val signals: List<FlipperSignal>
)

object FlipperZeroIrParser {

    /**
     * Parses standard Flipper Zero .ir file content into a FlipperRemote.
     * Supports both "parsed" and "raw" signal blocks.
     */
    fun parse(content: String, defaultName: String = "Flipper Remote"): FlipperRemote {
        val signals = mutableListOf<FlipperSignal>()
        var remoteName = defaultName

        val lines = content.lines()
        var currentBlock = mutableMapOf<String, String>()
        val currentDataList = mutableListOf<String>()

        fun flushBlock() {
            if (currentBlock.isNotEmpty() || currentDataList.isNotEmpty()) {
                val name = currentBlock["name"]?.trim() ?: "BTN_${signals.size + 1}"
                val type = currentBlock["type"]?.trim()?.lowercase() ?: "raw"

                if (type == "parsed") {
                    val protocol = currentBlock["protocol"]?.trim() ?: "NEC"
                    val addressStr = currentBlock["address"]?.trim() ?: "00 00 00 00"
                    val commandStr = currentBlock["command"]?.trim() ?: "00 00 00 00"

                    val addrBytes = addressStr.split(Regex("\\s+")).mapNotNull { it.toIntOrNull(16) }
                    val cmdBytes = commandStr.split(Regex("\\s+")).mapNotNull { it.toIntOrNull(16) }

                    val device = addrBytes.getOrElse(0) { 0 }
                    val subdevice = if (addrBytes.size > 1 && addrBytes[1] != 0) addrBytes[1] else -1
                    val function = cmdBytes.getOrElse(0) { 0 }

                    val encoded = ProtocolEncoder.encode(protocol, device, subdevice, function)
                    if (encoded != null) {
                        signals.add(
                            FlipperSignal(
                                name = name,
                                frequencyHz = encoded.frequencyHz,
                                patternMicros = encoded.patternMicros,
                                protocol = protocol,
                                device = device,
                                subdevice = subdevice,
                                functionCode = function
                            )
                        )
                    }
                } else {
                    // Raw type
                    val freq = currentBlock["frequency"]?.trim()?.toIntOrNull() ?: 38000
                    val rawDataStr = (currentBlock["data"] ?: "") + " " + currentDataList.joinToString(" ")
                    val pattern = rawDataStr.split(Regex("\\s+"))
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it > 0 }
                        .toIntArray()

                    if (pattern.isNotEmpty()) {
                        signals.add(
                            FlipperSignal(
                                name = name,
                                frequencyHz = freq,
                                patternMicros = pattern,
                                protocol = "RAW"
                            )
                        )
                    }
                }
            }
            currentBlock.clear()
            currentDataList.clear()
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                if (line.startsWith("# Remote:") || line.startsWith("# name:")) {
                    val candidate = line.substringAfter(":").trim()
                    if (candidate.isNotBlank()) remoteName = candidate
                }
                if (currentBlock.containsKey("name") && (currentBlock.containsKey("type") || currentDataList.isNotEmpty())) {
                    flushBlock()
                }
                continue
            }

            val colonIdx = line.indexOf(':')
            if (colonIdx > 0 && !line.startsWith("data")) {
                val key = line.substring(0, colonIdx).trim().lowercase()
                val value = line.substring(colonIdx + 1).trim()
                if (key == "name" && currentBlock.containsKey("name")) {
                    flushBlock()
                }
                currentBlock[key] = value
            } else if (line.startsWith("data:")) {
                val value = line.substringAfter(":").trim()
                currentBlock["data"] = value
            } else if (currentBlock.containsKey("data")) {
                // Continuation line of raw data
                currentDataList.add(line)
            }
        }
        flushBlock()

        return FlipperRemote(name = remoteName, signals = signals)
    }

    /**
     * Converts a parsed FlipperRemote into a SavedRemote and its SavedButtons.
     */
    fun toSavedEntities(flipperRemote: FlipperRemote, category: String = "Other", icon: String = "📻"): Pair<SavedRemote, List<SavedButton>> {
        val remote = SavedRemote(
            name = flipperRemote.name.replace(".ir", "").replace("_", " ").trim(),
            brand = "Flipper Zero",
            deviceType = "Imported IR",
            category = category,
            icon = icon
        )

        val buttons = flipperRemote.signals.mapIndexed { index, sig ->
            SavedButton(
                remoteId = 0,
                label = sig.name.replace("_", " "),
                icon = mapFunctionToIcon(sig.name),
                frequencyHz = sig.frequencyHz,
                patternMicros = sig.patternMicros.joinToString(","),
                protocol = sig.protocol,
                device = sig.device,
                subdevice = sig.subdevice,
                functionCode = sig.functionCode,
                sortOrder = index,
                colSpan = if (sig.name.contains("power", ignoreCase = true) || sig.name.contains("ok", ignoreCase = true)) 2 else 1
            )
        }

        return remote to buttons
    }

    /**
     * Exports a SavedRemote and its SavedButtons to standard Flipper Zero .ir format.
     */
    fun export(remote: SavedRemote, buttons: List<SavedButton>): String {
        val sb = StringBuilder()
        sb.appendLine("Filetype: IR signals file")
        sb.appendLine("Version: 1")
        sb.appendLine("#")
        sb.appendLine("# Exported by IrBlast")
        sb.appendLine("# Remote: ${remote.name} (${remote.brand})")
        sb.appendLine("#")

        for (btn in buttons) {
            val safeName = btn.label.trim().replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "BTN_${btn.id}" }
            val pattern = btn.toPatternIntArray()
            if (pattern.isEmpty()) continue

            sb.appendLine("name: $safeName")
            sb.appendLine("type: raw")
            sb.appendLine("frequency: ${btn.frequencyHz}")
            sb.appendLine("duty_cycle: 0.330000")
            sb.appendLine("data: ${pattern.joinToString(" ")}")
            sb.appendLine("#")
        }

        return sb.toString()
    }
}

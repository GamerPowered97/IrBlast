package com.example.irblast.data.settings

import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote

object JsonBackupManager {

    /**
     * Serializes all remotes and their corresponding buttons to a formatted JSON string.
     * Pure Kotlin implementation — zero stub dependencies and immune to ProGuard stripping.
     */
    fun exportToJson(remotesWithButtons: List<Pair<SavedRemote, List<SavedButton>>>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"exportedAt\": ${System.currentTimeMillis()},\n")
        sb.append("  \"remotes\": [\n")

        remotesWithButtons.forEachIndexed { rIdx, (remote, buttons) ->
            sb.append("    {\n")
            sb.append("      \"name\": \"${escapeJson(remote.name)}\",\n")
            sb.append("      \"brand\": \"${escapeJson(remote.brand)}\",\n")
            sb.append("      \"deviceType\": \"${escapeJson(remote.deviceType)}\",\n")
            sb.append("      \"category\": \"${escapeJson(remote.category)}\",\n")
            sb.append("      \"icon\": \"${escapeJson(remote.icon)}\",\n")
            sb.append("      \"isFavorite\": ${remote.isFavorite},\n")
            sb.append("      \"createdAt\": ${remote.createdAt},\n")
            sb.append("      \"buttons\": [\n")

            buttons.forEachIndexed { bIdx, btn ->
                sb.append("        {\n")
                sb.append("          \"label\": \"${escapeJson(btn.label)}\",\n")
                sb.append("          \"icon\": \"${escapeJson(btn.icon)}\",\n")
                sb.append("          \"frequencyHz\": ${btn.frequencyHz},\n")
                sb.append("          \"patternMicros\": \"${escapeJson(btn.patternMicros)}\",\n")
                sb.append("          \"protocol\": \"${escapeJson(btn.protocol)}\",\n")
                sb.append("          \"device\": ${btn.device},\n")
                sb.append("          \"subdevice\": ${btn.subdevice},\n")
                sb.append("          \"functionCode\": ${btn.functionCode},\n")
                sb.append("          \"sortOrder\": ${btn.sortOrder},\n")
                sb.append("          \"colSpan\": ${btn.colSpan}\n")
                sb.append("        }${if (bIdx < buttons.size - 1) "," else ""}\n")
            }

            sb.append("      ]\n")
            sb.append("    }${if (rIdx < remotesWithButtons.size - 1) "," else ""}\n")
        }

        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Parses a JSON backup string into a list of SavedRemote and list of SavedButton pairs.
     */
    fun importFromJson(jsonStr: String): List<Pair<SavedRemote, List<SavedButton>>> {
        val result = mutableListOf<Pair<SavedRemote, List<SavedButton>>>()
        val root = JsonParser(jsonStr).parse() as? JsonElement.JsonObject ?: return emptyList()
        val remotesArray = root.getArray("remotes") ?: return emptyList()

        for (elem in remotesArray.list) {
            val remoteObj = elem as? JsonElement.JsonObject ?: continue
            val name = remoteObj.getString("name", "Restored Remote")
            val brand = remoteObj.getString("brand", "Generic")
            val deviceType = remoteObj.getString("deviceType", "Device")
            val category = remoteObj.getString("category", "Other")
            val icon = remoteObj.getString("icon", "📺")
            val isFavorite = remoteObj.getBoolean("isFavorite", false)
            val createdAt = remoteObj.getLong("createdAt", System.currentTimeMillis())

            val savedRemote = SavedRemote(
                name = name,
                brand = brand,
                deviceType = deviceType,
                category = category,
                icon = icon,
                createdAt = createdAt,
                isFavorite = isFavorite
            )

            val buttonsList = mutableListOf<SavedButton>()
            val buttonsArray = remoteObj.getArray("buttons")
            if (buttonsArray != null) {
                buttonsArray.list.forEachIndexed { j, bElem ->
                    val btnObj = bElem as? JsonElement.JsonObject
                    if (btnObj != null) {
                        buttonsList.add(
                            SavedButton(
                                remoteId = 0,
                                label = btnObj.getString("label", "BTN"),
                                icon = btnObj.getString("icon", ""),
                                frequencyHz = btnObj.getInt("frequencyHz", 38000),
                                patternMicros = btnObj.getString("patternMicros", ""),
                                protocol = btnObj.getString("protocol", "RAW"),
                                device = btnObj.getInt("device", -1),
                                subdevice = btnObj.getInt("subdevice", -1),
                                functionCode = btnObj.getInt("functionCode", -1),
                                sortOrder = btnObj.getInt("sortOrder", j),
                                colSpan = btnObj.getInt("colSpan", 1)
                            )
                        )
                    }
                }
            }
            result.add(savedRemote to buttonsList)
        }

        return result
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private sealed class JsonElement {
        data class JsonObject(val map: Map<String, JsonElement>) : JsonElement() {
            fun getString(key: String, def: String = ""): String =
                (map[key] as? JsonPrimitive)?.value ?: def
            fun getInt(key: String, def: Int = 0): Int =
                (map[key] as? JsonPrimitive)?.value?.toIntOrNull() ?: def
            fun getLong(key: String, def: Long = 0L): Long =
                (map[key] as? JsonPrimitive)?.value?.toLongOrNull() ?: def
            fun getBoolean(key: String, def: Boolean = false): Boolean =
                (map[key] as? JsonPrimitive)?.value?.toBooleanStrictOrNull() ?: def
            fun getArray(key: String): JsonArray? = map[key] as? JsonArray
        }
        data class JsonArray(val list: List<JsonElement>) : JsonElement()
        data class JsonPrimitive(val value: String) : JsonElement()
        object JsonNull : JsonElement()
    }

    private class JsonParser(private val src: String) {
        private var idx = 0

        fun parse(): JsonElement {
            skipWhitespace()
            if (idx >= src.length) return JsonElement.JsonNull
            return parseValue()
        }

        private fun skipWhitespace() {
            while (idx < src.length && src[idx].isWhitespace()) idx++
        }

        private fun parseValue(): JsonElement {
            skipWhitespace()
            if (idx >= src.length) return JsonElement.JsonNull
            return when (val c = src[idx]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f', 'n' -> parseLiteral()
                else -> if (c == '-' || c.isDigit()) parseNumber() else {
                    idx++
                    JsonElement.JsonNull
                }
            }
        }

        private fun parseObject(): JsonElement.JsonObject {
            val map = mutableMapOf<String, JsonElement>()
            idx++ // consume '{'
            skipWhitespace()
            if (idx < src.length && src[idx] == '}') {
                idx++
                return JsonElement.JsonObject(map)
            }
            while (idx < src.length) {
                skipWhitespace()
                if (idx >= src.length || src[idx] != '"') break
                val keyElem = parseString()
                val key = keyElem.value
                skipWhitespace()
                if (idx < src.length && src[idx] == ':') idx++ // consume ':'
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (idx < src.length && src[idx] == ',') {
                    idx++
                } else if (idx < src.length && src[idx] == '}') {
                    idx++
                    break
                }
            }
            return JsonElement.JsonObject(map)
        }

        private fun parseArray(): JsonElement.JsonArray {
            val list = mutableListOf<JsonElement>()
            idx++ // consume '['
            skipWhitespace()
            if (idx < src.length && src[idx] == ']') {
                idx++
                return JsonElement.JsonArray(list)
            }
            while (idx < src.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (idx < src.length && src[idx] == ',') {
                    idx++
                } else if (idx < src.length && src[idx] == ']') {
                    idx++
                    break
                }
            }
            return JsonElement.JsonArray(list)
        }

        private fun parseString(): JsonElement.JsonPrimitive {
            idx++ // consume opening '"'
            val sb = StringBuilder()
            while (idx < src.length) {
                val c = src[idx++]
                if (c == '"') break
                if (c == '\\' && idx < src.length) {
                    val esc = src[idx++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (idx + 4 <= src.length) {
                                val hex = src.substring(idx, idx + 4)
                                idx += 4
                                hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return JsonElement.JsonPrimitive(sb.toString())
        }

        private fun parseLiteral(): JsonElement {
            val start = idx
            while (idx < src.length && src[idx].isLetter()) idx++
            val word = src.substring(start, idx)
            return when (word) {
                "true" -> JsonElement.JsonPrimitive("true")
                "false" -> JsonElement.JsonPrimitive("false")
                else -> JsonElement.JsonNull
            }
        }

        private fun parseNumber(): JsonElement.JsonPrimitive {
            val start = idx
            if (idx < src.length && (src[idx] == '-' || src[idx] == '+')) idx++
            while (idx < src.length && (src[idx].isDigit() || src[idx] == '.' || src[idx] == 'e' || src[idx] == 'E' || src[idx] == '+' || src[idx] == '-')) {
                idx++
            }
            return JsonElement.JsonPrimitive(src.substring(start, idx))
        }
    }
}

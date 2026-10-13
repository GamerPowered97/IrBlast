package com.example.irblast.data.repository

import com.example.irblast.data.db.IrdbDatabase
import com.example.irblast.data.db.UserRemoteDatabase
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.domain.model.IrdbCandidate
import com.example.irblast.domain.model.IrdbCode
import com.example.irblast.domain.model.IrdbRemote
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class IrdbRepository(private val irdbDatabase: IrdbDatabase) {

    suspend fun getCategories(): List<String> = withContext(Dispatchers.IO) {
        irdbDatabase.getCategories()
    }

    suspend fun getBrands(category: String? = null, query: String? = null): List<String> = withContext(Dispatchers.IO) {
        irdbDatabase.getBrands(category, query)
    }

    suspend fun getRemotes(brand: String, category: String? = null): List<IrdbRemote> = withContext(Dispatchers.IO) {
        irdbDatabase.getRemotes(brand, category)
    }

    suspend fun getCodesForRemote(remoteId: Long): List<IrdbCode> = withContext(Dispatchers.IO) {
        irdbDatabase.getCodesForRemote(remoteId)
    }

    suspend fun getCandidatesForSmartScan(
        category: String? = null,
        brand: String? = null,
        functionTarget: String = "POWER"
    ): List<IrdbCandidate> = withContext(Dispatchers.IO) {
        irdbDatabase.getCandidatesForSmartScan(category, brand, functionTarget)
    }
}

class RemoteRepository(
    private val userDb: UserRemoteDatabase,
    private val irdbDb: IrdbDatabase
) {
    private val _remotesFlow = MutableStateFlow<List<SavedRemote>>(emptyList())
    val remotesFlow: Flow<List<SavedRemote>> = _remotesFlow.asStateFlow()

    suspend fun refreshRemotes() = withContext(Dispatchers.IO) {
        _remotesFlow.value = userDb.getAllRemotes()
    }

    suspend fun getAllRemotes(): List<SavedRemote> = withContext(Dispatchers.IO) {
        val list = userDb.getAllRemotes()
        _remotesFlow.value = list
        list
    }

    suspend fun getRemoteById(id: Long): SavedRemote? = withContext(Dispatchers.IO) {
        userDb.getRemoteById(id)
    }

    suspend fun getButtonsForRemote(remoteId: Long): List<SavedButton> = withContext(Dispatchers.IO) {
        userDb.getButtonsForRemote(remoteId)
    }

    suspend fun saveRemote(remote: SavedRemote, buttons: List<SavedButton> = emptyList()): Long = withContext(Dispatchers.IO) {
        val id = userDb.insertRemote(remote)
        buttons.forEachIndexed { idx, btn ->
            userDb.insertButton(btn.copy(remoteId = id, sortOrder = idx))
        }
        refreshRemotes()
        id
    }

    suspend fun updateRemote(remote: SavedRemote) = withContext(Dispatchers.IO) {
        userDb.updateRemote(remote)
        refreshRemotes()
    }

    suspend fun deleteRemote(remoteId: Long) = withContext(Dispatchers.IO) {
        userDb.deleteRemote(remoteId)
        refreshRemotes()
    }

    suspend fun saveButton(button: SavedButton): Long = withContext(Dispatchers.IO) {
        val id = if (button.id == 0L) {
            userDb.insertButton(button)
        } else {
            userDb.updateButton(button)
            button.id
        }
        refreshRemotes()
        id
    }

    suspend fun deleteButton(buttonId: Long) = withContext(Dispatchers.IO) {
        userDb.deleteButton(buttonId)
        refreshRemotes()
    }

    suspend fun importIrdbRemote(
        irdbRemoteId: Long,
        customName: String? = null,
        onUnsupportedProtocol: ((String) -> Unit)? = null
    ): Long = withContext(Dispatchers.IO) {
        val codes = irdbDb.getCodesForRemote(irdbRemoteId)
        // Find remote info from database
        val cursor = irdbDb.getReadableDatabase().rawQuery(
            "SELECT brand, device_type, category, model FROM remotes WHERE id = ?",
            arrayOf(irdbRemoteId.toString())
        )
        val (brand, deviceType, category, model) = cursor.use {
            if (it.moveToNext()) {
                Tuple4(it.getString(0), it.getString(1), it.getString(2), it.getString(3))
            } else {
                Tuple4("Unknown", "Device", "Other", "Remote")
            }
        }

        val name = customName?.takeIf { it.isNotBlank() } ?: "$brand $model"
        val icon = when (category) {
            "TV" -> "📺"
            "Projector" -> "📽️"
            "Audio" -> "🔊"
            "AC / Climate" -> "❄️"
            "Set-Top Box" -> "📦"
            "Media Player" -> "💿"
            else -> "🎮"
        }

        val remoteId = userDb.insertRemote(
            SavedRemote(
                name = name,
                brand = brand,
                deviceType = deviceType,
                category = category,
                icon = icon
            )
        )

        val unsupportedProtocols = mutableSetOf<String>()

        codes.forEachIndexed { index, code ->
            val signal = ProtocolEncoder.encode(code.protocol, code.device, code.subdevice, code.function)
            if (signal != null) {
                userDb.insertButton(
                    SavedButton(
                        remoteId = remoteId,
                        label = code.functionName,
                        icon = mapFunctionToIcon(code.functionName),
                        frequencyHz = signal.frequencyHz,
                        patternMicros = signal.patternMicros.joinToString(","),
                        protocol = code.protocol,
                        device = code.device,
                        subdevice = code.subdevice,
                        functionCode = code.function,
                        sortOrder = index,
                        colSpan = if (code.functionName.contains("POWER", ignoreCase = true) ||
                            code.functionName.contains("OK", ignoreCase = true) ||
                            code.functionName.contains("ENTER", ignoreCase = true)) 2 else 1
                    )
                )
            } else {
                unsupportedProtocols.add(code.protocol)
            }
        }

        if (unsupportedProtocols.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                unsupportedProtocols.forEach { proto ->
                    onUnsupportedProtocol?.invoke(proto)
                }
            }
        }

        refreshRemotes()
        remoteId
    }

    private data class Tuple4(val a: String, val b: String, val c: String, val d: String)
}

fun mapFunctionToIcon(fn: String): String {
    val u = fn.uppercase()
    return when {
        "POWER" in u || "STANDBY" in u -> "power"
        "MUTE" in u -> "mute"
        ("VOL" in u && "+" in u) || ("VOLUME" in u && "+" in u) -> "vol_up"
        ("VOL" in u && "-" in u) || ("VOLUME" in u && "-" in u) -> "vol_down"
        ("CH" in u && "+" in u) || ("CHANNEL" in u && "+" in u) -> "ch_up"
        ("CH" in u && "-" in u) || ("CHANNEL" in u && "-" in u) -> "ch_down"
        "TEMP" in u && "+" in u -> "up"
        "TEMP" in u && "-" in u -> "down"
        "SPEED" in u && "+" in u -> "up"
        "SPEED" in u && "-" in u -> "down"
        u == "UP" -> "up"
        u == "DOWN" -> "down"
        u == "LEFT" -> "left"
        u == "RIGHT" -> "right"
        "OK" in u || "ENTER" in u || "SELECT" in u -> "ok"
        "MENU" in u || "MODE" in u -> "menu"
        "BACK" in u || "RETURN" in u || "EXIT" in u -> "back"
        "HOME" in u -> "home"
        "SOURCE" in u || "INPUT" in u || "HDMI" in u || "AUX" in u || "FAN" in u || "KEYSTONE" in u -> "input"
        "PLAY" in u -> "play"
        "PAUSE" in u -> "pause"
        "STOP" in u -> "stop"
        else -> ""
    }
}


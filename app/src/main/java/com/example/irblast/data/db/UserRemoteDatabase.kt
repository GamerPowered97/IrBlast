package com.example.irblast.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.irblast.data.ir.encoders.NecEncoder
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote

class UserRemoteDatabase(context: Context) : SQLiteOpenHelper(context, "user_remotes.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE saved_remotes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                brand TEXT NOT NULL,
                device_type TEXT NOT NULL,
                category TEXT NOT NULL,
                icon TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE saved_buttons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                remote_id INTEGER NOT NULL,
                label TEXT NOT NULL,
                icon TEXT NOT NULL,
                frequency_hz INTEGER NOT NULL,
                pattern_micros TEXT NOT NULL,
                protocol TEXT NOT NULL,
                device INTEGER NOT NULL,
                subdevice INTEGER NOT NULL,
                function_code INTEGER NOT NULL,
                sort_order INTEGER NOT NULL,
                col_span INTEGER NOT NULL,
                FOREIGN KEY(remote_id) REFERENCES saved_remotes(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX idx_buttons_remote ON saved_buttons(remote_id)")

        // Seed a sample remote (Samsung TV)
        seedInitialRemote(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS saved_buttons")
        db.execSQL("DROP TABLE IF EXISTS saved_remotes")
        onCreate(db)
    }

    private fun seedInitialRemote(db: SQLiteDatabase) {
        val cv = ContentValues().apply {
            put("name", "Living Room TV")
            put("brand", "Samsung")
            put("device_type", "TV")
            put("category", "TV")
            put("icon", "📺")
            put("created_at", System.currentTimeMillis())
        }
        val remoteId = db.insert("saved_remotes", null, cv)

        val defaultButtons = listOf(
            Triple("POWER", "power", 2),
            Triple("SOURCE", "input", 1),
            Triple("MUTE", "mute", 15),
            Triple("VOL +", "vol_up", 7),
            Triple("VOL -", "vol_down", 11),
            Triple("CH +", "ch_up", 18),
            Triple("CH -", "ch_down", 16),
            Triple("UP", "up", 96),
            Triple("DOWN", "down", 97),
            Triple("LEFT", "left", 101),
            Triple("RIGHT", "right", 98),
            Triple("OK", "ok", 104),
            Triple("MENU", "menu", 26),
            Triple("RETURN", "back", 88)
        )

        defaultButtons.forEachIndexed { index, (label, icon, funcCode) ->
            val signal = NecEncoder.encode(device = 7, subDevice = 7, function = funcCode)
            val btnCv = ContentValues().apply {
                put("remote_id", remoteId)
                put("label", label)
                put("icon", icon)
                put("frequency_hz", signal.frequencyHz)
                put("pattern_micros", signal.patternMicros.joinToString(","))
                put("protocol", "NEC")
                put("device", 7)
                put("subdevice", 7)
                put("function_code", funcCode)
                put("sort_order", index)
                put("col_span", if (label == "POWER" || label == "OK") 2 else 1)
            }
            db.insert("saved_buttons", null, btnCv)
        }
    }

    fun getAllRemotes(): List<SavedRemote> {
        val db = readableDatabase
        val list = mutableListOf<SavedRemote>()
        val cursor = db.rawQuery("SELECT id, name, brand, device_type, category, icon, created_at FROM saved_remotes ORDER BY created_at DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SavedRemote(
                        id = it.getLong(0),
                        name = it.getString(1),
                        brand = it.getString(2),
                        deviceType = it.getString(3),
                        category = it.getString(4),
                        icon = it.getString(5),
                        createdAt = it.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun getRemoteById(id: Long): SavedRemote? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, name, brand, device_type, category, icon, created_at FROM saved_remotes WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToNext()) {
                return SavedRemote(
                    id = it.getLong(0),
                    name = it.getString(1),
                    brand = it.getString(2),
                    deviceType = it.getString(3),
                    category = it.getString(4),
                    icon = it.getString(5),
                    createdAt = it.getLong(6)
                )
            }
        }
        return null
    }

    fun insertRemote(remote: SavedRemote): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", remote.name)
            put("brand", remote.brand)
            put("device_type", remote.deviceType)
            put("category", remote.category)
            put("icon", remote.icon)
            put("created_at", remote.createdAt)
        }
        return db.insert("saved_remotes", null, cv)
    }

    fun updateRemote(remote: SavedRemote) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", remote.name)
            put("brand", remote.brand)
            put("device_type", remote.deviceType)
            put("category", remote.category)
            put("icon", remote.icon)
        }
        db.update("saved_remotes", cv, "id = ?", arrayOf(remote.id.toString()))
    }

    fun deleteRemote(remoteId: Long) {
        val db = writableDatabase
        db.delete("saved_buttons", "remote_id = ?", arrayOf(remoteId.toString()))
        db.delete("saved_remotes", "id = ?", arrayOf(remoteId.toString()))
    }

    fun getButtonsForRemote(remoteId: Long): List<SavedButton> {
        val db = readableDatabase
        val list = mutableListOf<SavedButton>()
        val cursor = db.rawQuery("""
            SELECT id, remote_id, label, icon, frequency_hz, pattern_micros, protocol,
                   device, subdevice, function_code, sort_order, col_span
            FROM saved_buttons
            WHERE remote_id = ?
            ORDER BY sort_order ASC, id ASC
        """.trimIndent(), arrayOf(remoteId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SavedButton(
                        id = it.getLong(0),
                        remoteId = it.getLong(1),
                        label = it.getString(2),
                        icon = it.getString(3),
                        frequencyHz = it.getInt(4),
                        patternMicros = it.getString(5),
                        protocol = it.getString(6),
                        device = it.getInt(7),
                        subdevice = it.getInt(8),
                        functionCode = it.getInt(9),
                        sortOrder = it.getInt(10),
                        colSpan = it.getInt(11)
                    )
                )
            }
        }
        return list
    }

    fun insertButton(button: SavedButton): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("remote_id", button.remoteId)
            put("label", button.label)
            put("icon", button.icon)
            put("frequency_hz", button.frequencyHz)
            put("pattern_micros", button.patternMicros)
            put("protocol", button.protocol)
            put("device", button.device)
            put("subdevice", button.subdevice)
            put("function_code", button.functionCode)
            put("sort_order", button.sortOrder)
            put("col_span", button.colSpan)
        }
        return db.insert("saved_buttons", null, cv)
    }

    fun updateButton(button: SavedButton) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("label", button.label)
            put("icon", button.icon)
            put("frequency_hz", button.frequencyHz)
            put("pattern_micros", button.patternMicros)
            put("protocol", button.protocol)
            put("device", button.device)
            put("subdevice", button.subdevice)
            put("function_code", button.functionCode)
            put("sort_order", button.sortOrder)
            put("col_span", button.colSpan)
        }
        db.update("saved_buttons", cv, "id = ?", arrayOf(button.id.toString()))
    }

    fun deleteButton(buttonId: Long) {
        val db = writableDatabase
        db.delete("saved_buttons", "id = ?", arrayOf(buttonId.toString()))
    }
}

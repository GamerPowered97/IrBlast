package com.example.irblast.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.irblast.domain.model.IrdbCandidate
import com.example.irblast.domain.model.IrdbCode
import com.example.irblast.domain.model.IrdbRemote
import java.io.File
import java.io.FileOutputStream

class IrdbDatabase(private val context: Context) {
    private val tag = "IrdbDatabase"
    private var dbInstance: SQLiteDatabase? = null

    @Synchronized
    fun getReadableDatabase(): SQLiteDatabase {
        dbInstance?.let { if (it.isOpen) return it }

        val dbFile = context.getDatabasePath("irdb.db")
        if (!dbFile.exists() || dbFile.length() < 1000) {
            dbFile.parentFile?.mkdirs()
            Log.d(tag, "Copying irdb.db from assets to ${dbFile.absolutePath}...")
            context.assets.open("irdb.db").use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(tag, "Copied irdb.db successfully, size: ${dbFile.length()} bytes")
        }

        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        dbInstance = db
        return db
    }

    fun getCategories(): List<String> {
        val db = getReadableDatabase()
        val list = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT category FROM remotes ORDER BY category ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }

    fun getBrands(category: String? = null, query: String? = null): List<String> {
        val db = getReadableDatabase()
        val list = mutableListOf<String>()
        val sql: String
        val args: Array<String>

        if (!category.isNullOrBlank() && !query.isNullOrBlank()) {
            sql = "SELECT DISTINCT brand FROM remotes WHERE category = ? AND brand LIKE ? ORDER BY brand ASC"
            args = arrayOf(category, "%$query%")
        } else if (!category.isNullOrBlank()) {
            sql = "SELECT DISTINCT brand FROM remotes WHERE category = ? ORDER BY brand ASC"
            args = arrayOf(category)
        } else if (!query.isNullOrBlank()) {
            sql = "SELECT DISTINCT brand FROM remotes WHERE brand LIKE ? ORDER BY brand ASC"
            args = arrayOf("%$query%")
        } else {
            sql = "SELECT DISTINCT brand FROM remotes ORDER BY brand ASC"
            args = emptyArray()
        }

        val cursor = db.rawQuery(sql, args)
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }

    fun getRemotes(brand: String, category: String? = null): List<IrdbRemote> {
        val db = getReadableDatabase()
        val list = mutableListOf<IrdbRemote>()
        val sql: String
        val args: Array<String>

        if (!category.isNullOrBlank()) {
            sql = "SELECT id, brand, device_type, category, model FROM remotes WHERE brand = ? AND category = ? ORDER BY model ASC"
            args = arrayOf(brand, category)
        } else {
            sql = "SELECT id, brand, device_type, category, model FROM remotes WHERE brand = ? ORDER BY model ASC"
            args = arrayOf(brand)
        }

        val cursor = db.rawQuery(sql, args)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    IrdbRemote(
                        id = it.getLong(0),
                        brand = it.getString(1),
                        deviceType = it.getString(2),
                        category = it.getString(3),
                        model = it.getString(4)
                    )
                )
            }
        }
        return list
    }

    fun getCodesForRemote(remoteId: Long): List<IrdbCode> {
        val db = getReadableDatabase()
        val list = mutableListOf<IrdbCode>()
        val cursor = db.rawQuery(
            "SELECT id, remote_id, function_name, protocol, device, subdevice, function FROM codes WHERE remote_id = ? ORDER BY function_name ASC",
            arrayOf(remoteId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    IrdbCode(
                        id = it.getLong(0),
                        remoteId = it.getLong(1),
                        functionName = it.getString(2),
                        protocol = it.getString(3),
                        device = it.getInt(4),
                        subdevice = it.getInt(5),
                        function = it.getInt(6)
                    )
                )
            }
        }
        return list
    }

    fun getCandidatesForSmartScan(
        category: String? = null,
        brand: String? = null,
        functionTarget: String = "POWER"
    ): List<IrdbCandidate> {
        val db = getReadableDatabase()
        val list = mutableListOf<IrdbCandidate>()

        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!category.isNullOrBlank() && category != "All") {
            whereClauses.add("r.category = ?")
            args.add(category)
        }
        if (!brand.isNullOrBlank()) {
            whereClauses.add("r.brand LIKE ?")
            args.add("%$brand%")
        }

        // Fuzzy match on target action (e.g. POWER, MUTE, VOL+, INPUT)
        whereClauses.add("c.function_name LIKE ?")
        args.add("%$functionTarget%")

        val whereSql = if (whereClauses.isNotEmpty()) "WHERE " + whereClauses.joinToString(" AND ") else ""
        val sql = """
            SELECT r.id, r.brand, r.device_type, r.category, r.model,
                   c.function_name, c.protocol, c.device, c.subdevice, c.function
            FROM codes c
            JOIN remotes r ON c.remote_id = r.id
            $whereSql
            GROUP BY c.protocol, c.device, c.subdevice, c.function
            ORDER BY r.brand ASC, r.model ASC
            LIMIT 500
        """.trimIndent()

        val cursor = db.rawQuery(sql, args.toTypedArray())
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    IrdbCandidate(
                        remoteId = it.getLong(0),
                        brand = it.getString(1),
                        deviceType = it.getString(2),
                        category = it.getString(3),
                        model = it.getString(4),
                        functionName = it.getString(5),
                        protocol = it.getString(6),
                        device = it.getInt(7),
                        subdevice = it.getInt(8),
                        function = it.getInt(9)
                    )
                )
            }
        }
        return list
    }
}

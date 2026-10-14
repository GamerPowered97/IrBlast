package com.example.irblast.data

import com.example.irblast.data.ir.FlipperZeroIrParser
import com.example.irblast.data.settings.JsonBackupManager
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipperZeroAndBackupTest {

    @Test
    fun testFlipperZeroIrParser_rawSignal() {
        val irContent = """
            Filetype: IR signals file
            Version: 1
            # 
            name: Power
            type: raw
            frequency: 38000
            duty_cycle: 0.330000
            data: 9000 4500 560 1690 560 560 560 1690 560 560
            # 
            name: Volume_Up
            type: raw
            frequency: 38000
            duty_cycle: 0.330000
            data: 9000 4500 560 560 560 1690 560 1690 560 560
        """.trimIndent()

        val remote = FlipperZeroIrParser.parse(irContent, "Test Flipper")
        assertEquals("Test Flipper", remote.name)
        assertEquals(2, remote.signals.size)

        val s1 = remote.signals[0]
        assertEquals("Power", s1.name)
        assertEquals(38000, s1.frequencyHz)
        assertEquals(10, s1.patternMicros.size)
        assertEquals(9000, s1.patternMicros[0])
        assertEquals(4500, s1.patternMicros[1])

        val (savedRemote, buttons) = FlipperZeroIrParser.toSavedEntities(remote)
        assertEquals("Test Flipper", savedRemote.name)
        assertEquals(2, buttons.size)
        assertEquals("Power", buttons[0].label)
        assertEquals("Volume Up", buttons[1].label)
    }

    @Test
    fun testFlipperZeroIrParser_parsedSignal() {
        val irContent = """
            Filetype: IR signals file
            Version: 1
            # 
            name: Samsung_Power
            type: parsed
            protocol: NEC
            address: 07 07 00 00
            command: 02 00 00 00
        """.trimIndent()

        val remote = FlipperZeroIrParser.parse(irContent, "Samsung TV")
        assertEquals(1, remote.signals.size)
        val sig = remote.signals[0]
        assertEquals("Samsung_Power", sig.name)
        assertEquals(38000, sig.frequencyHz)
        assertEquals("NEC", sig.protocol)
        assertEquals(7, sig.device)
        assertEquals(7, sig.subdevice)
        assertEquals(2, sig.functionCode)
        // 72 elements for NEC standard frame with 2 repeats (68 single + 4 repeat burst)
        assertEquals(72, sig.patternMicros.size)
    }

    @Test
    fun testFlipperZeroIrExport() {
        val remote = SavedRemote(id = 1, name = "Test TV", brand = "Sony", deviceType = "TV", category = "TV")
        val buttons = listOf(
            SavedButton(
                id = 10,
                remoteId = 1,
                label = "VOL +",
                frequencyHz = 40000,
                patternMicros = "2400,600,1200,600,600,600",
                protocol = "SONY12"
            )
        )

        val exported = FlipperZeroIrParser.export(remote, buttons)
        assertTrue(exported.contains("Filetype: IR signals file"))
        assertTrue(exported.contains("name: VOL__"))
        assertTrue(exported.contains("frequency: 40000"))
        assertTrue(exported.contains("data: 2400 600 1200 600 600 600"))

        // Re-parse exported file
        val reimported = FlipperZeroIrParser.parse(exported, "Imported")
        assertEquals(1, reimported.signals.size)
        assertEquals(40000, reimported.signals[0].frequencyHz)
        assertEquals(6, reimported.signals[0].patternMicros.size)
    }

    @Test
    fun testJsonBackupAndRestore() {
        val remotes = listOf(
            SavedRemote(
                id = 1,
                name = "Living Room TV",
                brand = "LG",
                deviceType = "TV",
                category = "TV",
                icon = "📺",
                isFavorite = true
            ) to listOf(
                SavedButton(
                    id = 10,
                    remoteId = 1,
                    label = "POWER",
                    frequencyHz = 38000,
                    patternMicros = "9000,4500,560,560",
                    protocol = "NEC"
                )
            ),
            SavedRemote(
                id = 2,
                name = "Bedroom AC",
                brand = "Daikin",
                deviceType = "AC",
                category = "AC / Climate",
                icon = "❄️",
                isFavorite = false
            ) to emptyList<SavedButton>()
        )

        val json = JsonBackupManager.exportToJson(remotes)
        assertTrue(json.contains("Living Room TV"))
        assertTrue(json.contains("Bedroom AC"))
        assertTrue(json.contains("\"isFavorite\": true"))

        val restored = JsonBackupManager.importFromJson(json)
        assertEquals(2, restored.size)

        val r1 = restored[0].first
        val b1 = restored[0].second
        assertEquals("Living Room TV", r1.name)
        assertEquals("LG", r1.brand)
        assertTrue(r1.isFavorite)
        assertEquals(1, b1.size)
        assertEquals("POWER", b1[0].label)
        assertEquals(38000, b1[0].frequencyHz)

        val r2 = restored[1].first
        assertEquals("Bedroom AC", r2.name)
        assertFalse(r2.isFavorite)
    }

    @Test
    fun testSavedButtonMemoization() {
        val btn = SavedButton(
            id = 1,
            remoteId = 1,
            label = "MUTE",
            patternMicros = "100, 200, 300, 400"
        )
        val arr1 = btn.toPatternIntArray()
        val arr2 = btn.toPatternIntArray()
        // Same memoized instance in memory!
        assertTrue(arr1 === arr2)
        assertEquals(4, arr1.size)
        assertEquals(100, arr1[0])
        assertEquals(400, arr1[3])
    }
}

package com.example.irblast.encoders

import com.example.irblast.data.ir.encoders.JvcEncoder
import com.example.irblast.data.ir.encoders.NecEncoder
import com.example.irblast.data.ir.encoders.PanasonicEncoder
import com.example.irblast.data.ir.encoders.ProntoHexParser
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.data.ir.encoders.RawPatternParser
import com.example.irblast.data.ir.encoders.Rc5Encoder
import com.example.irblast.data.ir.encoders.Rc6Encoder
import com.example.irblast.data.ir.encoders.SharpEncoder
import com.example.irblast.data.ir.encoders.SonyEncoder
import com.example.irblast.data.ir.encoders.Recs80Encoder
import com.example.irblast.data.ir.encoders.MitsubishiEncoder
import com.example.irblast.data.ir.encoders.AiwaEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolEncodersTest {

    @Test
    fun testNecEncoder_patternStructure() {
        val signal = NecEncoder.encode(device = 0x04, subDevice = -1, function = 0x08)
        assertEquals(38000, signal.frequencyHz)
        assertEquals("NEC", signal.protocol)
        // 2 header + 32 * 2 bits + 1 stop bit + 1 trailing space = 68 elements
        assertEquals(68, signal.patternMicros.size)
        // Check leader
        assertEquals(9000, signal.patternMicros[0])
        assertEquals(4500, signal.patternMicros[1])
        // Stop bit
        assertEquals(560, signal.patternMicros[66])
        // Trailing space
        assertEquals(40500, signal.patternMicros[67])
    }

    @Test
    fun testNecEncoder_repeatBursts() {
        val signal = NecEncoder.encode(device = 0x04, subDevice = -1, function = 0x08, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        // 68 elements (leader frame) + 4 elements (repeat burst [9000, 2250, 560, 96190]) = 72 elements
        assertEquals(72, signal.patternMicros.size)
        assertEquals(9000, signal.patternMicros[68])
        assertEquals(2250, signal.patternMicros[69])
        assertEquals(560, signal.patternMicros[70])
        assertEquals(96190, signal.patternMicros[71])
    }

    @Test
    fun testNecEncoder_extendedSubdevice() {
        // Samsung TV Power: device = 7, subDevice = 7, function = 2
        val signal = NecEncoder.encode(device = 7, subDevice = 7, function = 2)
        assertEquals(38000, signal.frequencyHz)
        assertEquals(68, signal.patternMicros.size)

        // Verify Byte 0 (device = 7): bits 0, 1, 2 are 1 (ONE_SPACE = 1690)
        assertEquals(1690, signal.patternMicros[3])
        assertEquals(1690, signal.patternMicros[5])
        assertEquals(1690, signal.patternMicros[7])
        assertEquals(560, signal.patternMicros[9])

        // Verify Byte 1 (subDevice = 7): must NOT be inverted to 248. Bits 0, 1, 2 must be 1 (1690)
        assertEquals(1690, signal.patternMicros[19])
        assertEquals(1690, signal.patternMicros[21])
        assertEquals(1690, signal.patternMicros[23])
        assertEquals(560, signal.patternMicros[25])
    }

    @Test
    fun testNecEncoder_standardInversionWhenNoSubdevice() {
        // Standard NEC (subDevice = -1): subByte should be ~device
        val signal = NecEncoder.encode(device = 7, subDevice = -1, function = 2)
        // Subdevice is inverted: 7 -> ~7 = 248 (0b11111000). Bits 0, 1, 2 are 0 (560)
        assertEquals(560, signal.patternMicros[19])
        assertEquals(560, signal.patternMicros[21])
        assertEquals(560, signal.patternMicros[23])
        assertEquals(1690, signal.patternMicros[25])
    }

    @Test
    fun testNecEncoder_boundaryValues() {
        val sig0 = NecEncoder.encode(device = 0, subDevice = 0, function = 0)
        assertEquals(68, sig0.patternMicros.size)

        val sig255 = NecEncoder.encode(device = 255, subDevice = 255, function = 255)
        assertEquals(68, sig255.patternMicros.size)
    }

    @Test
    fun testSonyEncoder_12BitSingleFrame() {
        val signal = SonyEncoder.encode(bitLength = 12, device = 1, function = 21, repeats = 1)
        assertEquals(40000, signal.frequencyHz)
        assertEquals("SONY12", signal.protocol)
        // Leader: 2, 7 bits func: 14, 5 bits dev: 10 -> total 26 elements
        assertEquals(26, signal.patternMicros.size)
        assertEquals(2400, signal.patternMicros[0])
        assertEquals(600, signal.patternMicros[1])
    }

    @Test
    fun testSonyEncoder_12BitRepeatedFrames() {
        // Physical Sony hardware standard: 3 repeats with 45ms cycle period
        val signal = SonyEncoder.encode(bitLength = 12, device = 1, function = 21, repeats = 3)
        assertEquals(40000, signal.frequencyHz)
        assertEquals(78, signal.patternMicros.size) // 26 * 3 elements
        // First leader
        assertEquals(2400, signal.patternMicros[0])
        assertEquals(600, signal.patternMicros[1])
        // Second frame leader at index 26
        assertEquals(2400, signal.patternMicros[26])
        // Inter-frame gap at index 25 should be around 25ms - 30ms (to complete 45ms cycle)
        assertTrue("Inter-frame gap should be > 20000us, was ${signal.patternMicros[25]}", signal.patternMicros[25] > 20000)
    }

    @Test
    fun testSonyEncoder_15BitAnd20Bit() {
        val s15 = SonyEncoder.encode(bitLength = 15, device = 1, function = 21, repeats = 1)
        assertEquals(32, s15.patternMicros.size)

        val s20 = SonyEncoder.encode(bitLength = 20, device = 1, subDevice = 2, function = 21, repeats = 1)
        assertEquals(42, s20.patternMicros.size)
    }

    @Test
    fun testRc5Encoder() {
        val signal = Rc5Encoder.encode(device = 0, function = 12)
        assertEquals(36000, signal.frequencyHz)
        assertEquals("RC5", signal.protocol)
        assertTrue(signal.patternMicros.isNotEmpty())
        assertEquals("RC5 pulse array must have even length", 0, signal.patternMicros.size % 2)
        val duration = signal.totalDurationMs
        assertTrue("RC5 duration should be around 24ms, was: $duration", duration in 20..30)
    }

    @Test
    fun testRc5Encoder_extendedCommand() {
        // Function >= 64 triggers extended RC5 (field bit = 0)
        val signal = Rc5Encoder.encode(device = 0, function = 65)
        assertEquals("RC5", signal.protocol)
        assertTrue(signal.patternMicros.isNotEmpty())
        assertEquals("RC5 pulse array must have even length", 0, signal.patternMicros.size % 2)
    }

    @Test
    fun testRc6Encoder() {
        val signal = Rc6Encoder.encode(device = 0, function = 12)
        assertEquals(36000, signal.frequencyHz)
        assertEquals("RC6", signal.protocol)
        assertTrue(signal.patternMicros.isNotEmpty())
        assertEquals("RC6 pulse array must have even length", 0, signal.patternMicros.size % 2)
        assertEquals(2666, signal.patternMicros[0])
        assertEquals(889, signal.patternMicros[1])
    }

    @Test
    fun testJvcEncoder() {
        val signal = JvcEncoder.encode(device = 3, function = 14)
        assertEquals(38000, signal.frequencyHz)
        assertEquals("JVC", signal.protocol)
        // 2 leader + 16 dev + 16 func + 1 stop + 1 trailing space = 36 elements
        assertEquals(36, signal.patternMicros.size)
        assertEquals(8400, signal.patternMicros[0])
        assertEquals(4200, signal.patternMicros[1])
        assertEquals(526, signal.patternMicros[34])
        assertEquals(50000, signal.patternMicros[35])
    }

    @Test
    fun testPanasonicEncoder() {
        val signal = PanasonicEncoder.encode(device = 128, subDevice = 0, function = 32)
        assertEquals(37000, signal.frequencyHz)
        assertEquals("PANASONIC", signal.protocol)
        // 2 leader + 48*2 data + 1 stop + 1 trailing space = 100 elements
        assertEquals(100, signal.patternMicros.size)
        assertEquals(3456, signal.patternMicros[0])
        assertEquals(1728, signal.patternMicros[1])
        assertEquals(432, signal.patternMicros[98])
        assertEquals(74000, signal.patternMicros[99])
    }

    @Test
    fun testSharpEncoder() {
        val signal = SharpEncoder.encode(device = 1, function = 20)
        assertEquals(38000, signal.frequencyHz)
        assertEquals("SHARP", signal.protocol)
        // 2 frames * (15*2 + 2) = 64 elements
        assertEquals(64, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
    }

    @Test
    fun testProntoHexParser_validCode() {
        val pronto = "0000 006d 0002 0000 0155 00aa 0015 0015"
        val result = ProntoHexParser.parse(pronto)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertTrue("Frequency should be ~38kHz", signal.frequencyHz in 36000..40000)
        assertEquals(4, signal.patternMicros.size)
    }

    @Test
    fun testProntoHexParser_introAndRepeatBursts() {
        // 1 once-pair + 1 repeat-pair = 2 pairs total (4 elements)
        val pronto = "0000 006d 0001 0001 0155 00aa 0015 0015"
        val result = ProntoHexParser.parse(pronto)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertEquals(4, signal.patternMicros.size)
    }

    @Test
    fun testProntoHexParser_withHexPrefix() {
        val pronto = "0x0000 0x006d 0x0002 0x0000 0x0155 0x00aa 0x0015 0x0015"
        val result = ProntoHexParser.parse(pronto)
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrThrow().patternMicros.size)
    }

    @Test
    fun testProntoHexParser_zeroCountsFallback() {
        // Header with onceCount = 0 and repeatCount = 0
        val pronto = "0000 006d 0000 0000 0155 00aa 0015 0015"
        val result = ProntoHexParser.parse(pronto)
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrThrow().patternMicros.size)
    }

    @Test
    fun testProntoHexParser_invalidFormats() {
        // Empty
        assertTrue(ProntoHexParser.parse("").isFailure)
        // Too short
        assertTrue(ProntoHexParser.parse("0000 006d").isFailure)
        // Invalid non-raw format
        assertTrue(ProntoHexParser.parse("0100 006d 0002 0000 0155 00aa").isFailure)
        // Non-hex
        assertTrue(ProntoHexParser.parse("0000 006d 0002 0000 NOT_HEX 00aa").isFailure)
    }

    @Test
    fun testRawPatternParser_valid() {
        val raw = "9000, 4500, 560, 560, 560, 1690"
        val result = RawPatternParser.parse(raw, 38000)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertEquals(6, signal.patternMicros.size)
        assertEquals(9000, signal.patternMicros[0])
        assertEquals(1690, signal.patternMicros[5])
    }

    @Test
    fun testRawPatternParser_invalid() {
        assertTrue(RawPatternParser.parse("").isFailure)
        assertTrue(RawPatternParser.parse("9000").isFailure) // Less than 2 pulses
        assertTrue(RawPatternParser.parse("9000, -500").isFailure) // Negative pulse
        assertTrue(RawPatternParser.parse("9000, 0").isFailure) // Zero pulse
    }

    @Test
    fun testProtocolEncoder_dispatch() {
        val nec = ProtocolEncoder.encode("NEC", 1, -1, 2)
        assertNotNull(nec)
        assertEquals("NEC", nec?.protocol)

        val sony = ProtocolEncoder.encode("Sony12", 1, -1, 2)
        assertNotNull(sony)
        assertEquals("SONY12", sony?.protocol)
        // ProtocolEncoder uses 3 repeats for reliable transmission to Sony electronics
        assertEquals(78, sony?.patternMicros?.size)

        val rc5 = ProtocolEncoder.encode("RC5", 0, -1, 1)
        assertNotNull(rc5)
        assertEquals("RC5", rc5?.protocol)

        val jvc = ProtocolEncoder.encode("JVC", 0, -1, 1)
        assertNotNull(jvc)
        assertEquals("JVC", jvc?.protocol)

        val panasonic = ProtocolEncoder.encode("Panasonic", 128, 0, 32)
        assertNotNull(panasonic)
        assertEquals("PANASONIC", panasonic?.protocol)

        val sharp = ProtocolEncoder.encode("Sharp", 1, -1, 20)
        assertNotNull(sharp)
        assertEquals("SHARP", sharp?.protocol)

        val denon = ProtocolEncoder.encode("Denon", 12, -1, 127)
        assertNotNull(denon)
        assertEquals("SHARP", denon?.protocol)

        val unknown = ProtocolEncoder.encode("UNKNOWN_PROTO", 0, -1, 1)
        assertNull(unknown)

        val teacK = ProtocolEncoder.encode("TEAC-K", 128, 0, 32)
        assertNotNull(teacK)
        assertEquals("PANASONIC", teacK?.protocol)
    }

    @Test
    fun testRc5Encoder_repeatFrames() {
        val signal = Rc5Encoder.encode(device = 0, function = 12, repeats = 2)
        assertEquals(36000, signal.frequencyHz)
        assertEquals("RC5", signal.protocol)
        assertEquals("RC5 repeat frame array must have even length", 0, signal.patternMicros.size % 2)
        assertTrue("RC5 2-frame duration must be > 100ms", signal.totalDurationMs > 100)
    }

    @Test
    fun testRc6Encoder_repeatFrames() {
        val signal = Rc6Encoder.encode(device = 0, function = 12, repeats = 2)
        assertEquals(36000, signal.frequencyHz)
        assertEquals("RC6", signal.protocol)
        assertEquals("RC6 repeat frame array must have even length", 0, signal.patternMicros.size % 2)
        assertTrue("RC6 2-frame duration must be > 100ms", signal.totalDurationMs > 100)
    }

    @Test
    fun testJvcEncoder_repeatFrames() {
        val signal = JvcEncoder.encode(device = 3, function = 14, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        assertEquals(72, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
    }

    @Test
    fun testPanasonicEncoder_repeatFrames() {
        val signal = PanasonicEncoder.encode(device = 128, subDevice = 0, function = 32, repeats = 2)
        assertEquals(37000, signal.frequencyHz)
        assertEquals(200, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
    }

    @Test
    fun testSharpEncoder_repeatFrames() {
        val signal = SharpEncoder.encode(device = 1, function = 20, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        assertEquals(128, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
    }

    @Test
    fun testRawPatternParser_oddPadding() {
        // 3 pulses (odd): must be padded to 4 pulses (even)
        val raw = "9000, 4500, 560"
        val result = RawPatternParser.parse(raw, 38000)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertEquals(4, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
        assertEquals(20000, signal.patternMicros[3])
    }

    @Test
    fun testRawPatternParser_whitespaceAndCommas() {
        val raw = "  9000 ,   4500,  560 , 560  "
        val result = RawPatternParser.parse(raw, 38000)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertEquals(4, signal.patternMicros.size)
        assertEquals(9000, signal.patternMicros[0])
        assertEquals(4500, signal.patternMicros[1])
    }

    @Test
    fun testProntoHexParser_sampleSony() {
        val pronto = "0000 0067 0000 000d 0060 0018 0018 0018 0030 0018 0030 0018 0030 0018 0018 0018 0030 0018 0018 0018 0030 0018 0018 0018 0018 0018 0018 0018 0018 03e8"
        val result = ProntoHexParser.parse(pronto)
        assertTrue(result.isSuccess)
        val signal = result.getOrThrow()
        assertTrue("Frequency should be ~40kHz", signal.frequencyHz in 38000..42000)
        assertEquals(26, signal.patternMicros.size)
    }

    @Test
    fun testMapFunctionToIcon_allAppliances() {
        // TV & General
        assertEquals("power", com.example.irblast.data.repository.mapFunctionToIcon("POWER"))
        assertEquals("mute", com.example.irblast.data.repository.mapFunctionToIcon("MUTE"))
        assertEquals("vol_up", com.example.irblast.data.repository.mapFunctionToIcon("VOL +"))
        assertEquals("vol_down", com.example.irblast.data.repository.mapFunctionToIcon("VOL -"))
        assertEquals("ch_up", com.example.irblast.data.repository.mapFunctionToIcon("CH +"))
        assertEquals("ch_down", com.example.irblast.data.repository.mapFunctionToIcon("CH -"))
        assertEquals("ok", com.example.irblast.data.repository.mapFunctionToIcon("OK"))
        assertEquals("menu", com.example.irblast.data.repository.mapFunctionToIcon("MENU"))
        assertEquals("back", com.example.irblast.data.repository.mapFunctionToIcon("RETURN"))

        // Climate / AC
        assertEquals("up", com.example.irblast.data.repository.mapFunctionToIcon("TEMP +"))
        assertEquals("down", com.example.irblast.data.repository.mapFunctionToIcon("TEMP -"))
        assertEquals("menu", com.example.irblast.data.repository.mapFunctionToIcon("MODE"))

        // Fan
        assertEquals("up", com.example.irblast.data.repository.mapFunctionToIcon("SPEED +"))
        assertEquals("down", com.example.irblast.data.repository.mapFunctionToIcon("SPEED -"))

        // Projector
        assertEquals("input", com.example.irblast.data.repository.mapFunctionToIcon("HDMI 1"))
        assertEquals("input", com.example.irblast.data.repository.mapFunctionToIcon("KEYSTONE"))

        // Media
        assertEquals("play", com.example.irblast.data.repository.mapFunctionToIcon("PLAY"))
        assertEquals("pause", com.example.irblast.data.repository.mapFunctionToIcon("PAUSE"))
        assertEquals("stop", com.example.irblast.data.repository.mapFunctionToIcon("STOP"))
    }

    @Test
    fun testSavedButton_toPatternIntArray() {
        val emptyButton = com.example.irblast.domain.model.SavedButton(
            remoteId = 1L,
            label = "EMPTY",
            patternMicros = ""
        )
        assertEquals(0, emptyButton.toPatternIntArray().size)

        val validButton = com.example.irblast.domain.model.SavedButton(
            remoteId = 1L,
            label = "VALID",
            patternMicros = "9000, 4500, 560, 1690"
        )
        val arr = validButton.toPatternIntArray()
        assertEquals(4, arr.size)
        assertEquals(9000, arr[0])
        assertEquals(1690, arr[3])

        // Space separated
        val spaceButton = com.example.irblast.domain.model.SavedButton(
            remoteId = 1L,
            label = "SPACE",
            patternMicros = "9000 4500 560 1690"
        )
        val spaceArr = spaceButton.toPatternIntArray()
        assertEquals(4, spaceArr.size)
        assertEquals(9000, spaceArr[0])
        assertEquals(1690, spaceArr[3])

        // Semicolon and mixed whitespace
        val mixedButton = com.example.irblast.domain.model.SavedButton(
            remoteId = 1L,
            label = "MIXED",
            patternMicros = " 9000; 4500 , 560 ; 1690 "
        )
        val mixedArr = mixedButton.toPatternIntArray()
        assertEquals(4, mixedArr.size)
        assertEquals(9000, mixedArr[0])
        assertEquals(1690, mixedArr[3])
    }

    @Test
    fun testRawPatternParser_invalidFrequencyClampedToDefault() {
        val raw = "9000, 4500"
        // Frequency too low (< 10000) or negative
        val lowFreqResult = RawPatternParser.parse(raw, 500)
        assertTrue(lowFreqResult.isSuccess)
        assertEquals(38000, lowFreqResult.getOrThrow().frequencyHz)

        val negativeFreqResult = RawPatternParser.parse(raw, -10)
        assertTrue(negativeFreqResult.isSuccess)
        assertEquals(38000, negativeFreqResult.getOrThrow().frequencyHz)

        val highFreqResult = RawPatternParser.parse(raw, 150000)
        assertTrue(highFreqResult.isSuccess)
        assertEquals(38000, highFreqResult.getOrThrow().frequencyHz)

        // Valid custom frequency in range
        val validFreqResult = RawPatternParser.parse(raw, 56000)
        assertTrue(validFreqResult.isSuccess)
        assertEquals(56000, validFreqResult.getOrThrow().frequencyHz)
    }

    @Test
    fun testRawPatternParser_rejectsInvalidOrEmpty() {
        val emptyResult = RawPatternParser.parse("", 38000)
        assertTrue(emptyResult.isFailure)

        val nonPositiveResult = RawPatternParser.parse("9000, -500, 560, 560", 38000)
        assertTrue(nonPositiveResult.isFailure)

        val singlePulseResult = RawPatternParser.parse("9000", 38000)
        assertTrue(singlePulseResult.isFailure)
    }

    @Test
    fun testProntoHexParser_rejectsMalformed() {
        val invalidResult = ProntoHexParser.parse("0000 0067 NOT_HEX")
        assertTrue(invalidResult.isFailure)

        val shortResult = ProntoHexParser.parse("0000 0067 0000")
        assertTrue(shortResult.isFailure)
    }

    @Test
    fun testRecs80Encoder() {
        val signal = Recs80Encoder.encode(device = 2, function = 16, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        // 24 elements per frame * 2 repeats = 48 elements (even)
        assertEquals(48, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
        // Check start pulse unit (158 us)
        assertEquals(158, signal.patternMicros[0])
        // Check start bit space (space for '1' is 7426 us)
        assertEquals(7426, signal.patternMicros[1])
        // Stop gap at end of frame 1 (index 23)
        assertEquals(45000, signal.patternMicros[23])

        // Verify dispatch via ProtocolEncoder
        val dispatched = ProtocolEncoder.encode("RECS80", 2, -1, 16)
        assertNotNull(dispatched)
        assertEquals(38000, dispatched!!.frequencyHz)
    }

    @Test
    fun testMitsubishiEncoder() {
        val signal = MitsubishiEncoder.encode(device = 1, function = 2, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        // 8 dev + 8 func = 16 bits -> 32 pulses + 1 stop pair = 34 elements * 2 repeats = 68 elements
        assertEquals(68, signal.patternMicros.size)
        assertEquals(0, signal.patternMicros.size % 2)
        assertEquals(300, signal.patternMicros[0])

        val dispatched = ProtocolEncoder.encode("Mitsubishi", 1, -1, 2)
        assertNotNull(dispatched)
    }

    @Test
    fun testAiwaEncoder() {
        val signal = AiwaEncoder.encode(device = 1, subDevice = 2, function = 3, repeats = 2)
        assertEquals(38000, signal.frequencyHz)
        assertEquals(0, signal.patternMicros.size % 2)
        // Leader mark and space
        assertEquals(8800, signal.patternMicros[0])
        assertEquals(4400, signal.patternMicros[1])

        val dispatched = ProtocolEncoder.encode("Aiwa", 1, 2, 3)
        assertNotNull(dispatched)
    }

    @Test
    fun testRcaEncoder() {
        val signal56k = com.example.irblast.data.ir.encoders.RcaEncoder.encode(device = 7, function = 60, is38k = false, repeats = 1)
        assertEquals(56000, signal56k.frequencyHz)
        assertEquals("RCA", signal56k.protocol)
        // 2 header + 24 * 2 bits + 1 stop mark + 1 trailing space = 52 elements
        assertEquals(52, signal56k.patternMicros.size)
        assertEquals(4000, signal56k.patternMicros[0])
        assertEquals(4000, signal56k.patternMicros[1])
        assertEquals(500, signal56k.patternMicros[50])
        assertEquals(8000, signal56k.patternMicros[51])

        val signal38k = com.example.irblast.data.ir.encoders.RcaEncoder.encode(device = 7, function = 60, is38k = true, repeats = 2)
        assertEquals(38000, signal38k.frequencyHz)
        assertEquals("RCA-38", signal38k.protocol)
        assertEquals(104, signal38k.patternMicros.size)

        // ProtocolEncoder dispatch
        val d1 = ProtocolEncoder.encode("RCA", 7, -1, 60)
        assertNotNull(d1)
        assertEquals(56000, d1!!.frequencyHz)

        val d2 = ProtocolEncoder.encode("RCA-38", 7, -1, 60)
        assertNotNull(d2)
        assertEquals(38000, d2!!.frequencyHz)
    }

    @Test
    fun testFujitsuEncoder() {
        val signal = com.example.irblast.data.ir.encoders.FujitsuEncoder.encode(device = 134, subDevice = -1, function = 0, repeats = 1)
        assertEquals(37000, signal.frequencyHz)
        assertEquals("FUJITSU", signal.protocol)
        // 2 header + 48 * 2 bits + 1 stop + 1 trailing space = 100 elements
        assertEquals(100, signal.patternMicros.size)
        assertEquals(3456, signal.patternMicros[0])
        assertEquals(1728, signal.patternMicros[1])
        assertEquals(432, signal.patternMicros[98])
        assertEquals(47520, signal.patternMicros[99])

        val dispatched = ProtocolEncoder.encode("Fujitsu", 134, -1, 0)
        assertNotNull(dispatched)
        assertEquals(37000, dispatched!!.frequencyHz)
    }

    @Test
    fun testDishNetworkEncoder() {
        val signal = com.example.irblast.data.ir.encoders.DishNetworkEncoder.encode(device = 0, subDevice = 0, function = 2, repeats = 1)
        assertEquals(56000, signal.frequencyHz)
        assertEquals("DISH_NETWORK", signal.protocol)
        // 2 header + 16 * 2 bits + 1 stop + 1 trailing space = 36 elements
        assertEquals(36, signal.patternMicros.size)
        assertEquals(400, signal.patternMicros[0])
        assertEquals(6000, signal.patternMicros[1])
        assertEquals(400, signal.patternMicros[34])
        assertEquals(6000, signal.patternMicros[35])

        val dispatched = ProtocolEncoder.encode("Dish_Network", 0, 0, 2)
        assertNotNull(dispatched)
        assertEquals(56000, dispatched!!.frequencyHz)
    }
}



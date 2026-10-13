package com.example.irblast.encoders

import com.example.irblast.data.ir.encoders.JvcEncoder
import com.example.irblast.data.ir.encoders.NecEncoder
import com.example.irblast.data.ir.encoders.ProntoHexParser
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.data.ir.encoders.RawPatternParser
import com.example.irblast.data.ir.encoders.Rc5Encoder
import com.example.irblast.data.ir.encoders.Rc6Encoder
import com.example.irblast.data.ir.encoders.SonyEncoder
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
        // 2 header + 32 * 2 bits + 1 stop bit = 67 elements
        assertEquals(67, signal.patternMicros.size)
        // Check leader
        assertEquals(9000, signal.patternMicros[0])
        assertEquals(4500, signal.patternMicros[1])
        // Stop bit
        assertEquals(560, signal.patternMicros[66])
    }

    @Test
    fun testNecEncoder_extendedSubdevice() {
        // Samsung TV Power: device = 7, subDevice = 7, function = 2
        val signal = NecEncoder.encode(device = 7, subDevice = 7, function = 2)
        assertEquals(38000, signal.frequencyHz)
        assertEquals(67, signal.patternMicros.size)

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
        assertEquals(67, sig0.patternMicros.size)

        val sig255 = NecEncoder.encode(device = 255, subDevice = 255, function = 255)
        assertEquals(67, sig255.patternMicros.size)
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
        val duration = signal.totalDurationMs
        assertTrue("RC5 duration should be around 24ms, was: $duration", duration in 20..30)
    }

    @Test
    fun testRc5Encoder_extendedCommand() {
        // Function >= 64 triggers extended RC5 (field bit = 0)
        val signal = Rc5Encoder.encode(device = 0, function = 65)
        assertEquals("RC5", signal.protocol)
        assertTrue(signal.patternMicros.isNotEmpty())
    }

    @Test
    fun testRc6Encoder() {
        val signal = Rc6Encoder.encode(device = 0, function = 12)
        assertEquals(36000, signal.frequencyHz)
        assertEquals("RC6", signal.protocol)
        assertTrue(signal.patternMicros.isNotEmpty())
        assertEquals(2666, signal.patternMicros[0])
        assertEquals(889, signal.patternMicros[1])
    }

    @Test
    fun testJvcEncoder() {
        val signal = JvcEncoder.encode(device = 3, function = 14)
        assertEquals(38000, signal.frequencyHz)
        assertEquals("JVC", signal.protocol)
        assertEquals(35, signal.patternMicros.size)
        assertEquals(8400, signal.patternMicros[0])
        assertEquals(4200, signal.patternMicros[1])
        assertEquals(526, signal.patternMicros[34])
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

        val unknown = ProtocolEncoder.encode("UNKNOWN_PROTO", 0, -1, 1)
        assertNull(unknown)
    }
}

package com.example.irblast.data.ir.encoders

import com.example.irblast.domain.model.IrSignal

object ProtocolEncoder {
    fun encode(protocol: String, device: Int, subDevice: Int = -1, function: Int): IrSignal? {
        val proto = protocol.trim().uppercase()
        return when {
            proto == "NEC" || proto == "NEC1" || proto == "NEC2" ||
                    proto == "NECX1" || proto == "NECX2" || proto == "APPLE" || proto == "PIONEER" ||
                    proto.startsWith("NEC1-") || proto.startsWith("NEC2-") || proto == "48-NEC1" || proto == "48-NEC2" -> {
                NecEncoder.encode(device, subDevice, function, repeats = 2)
            }
            proto == "SONY12" || proto == "SIRC12" -> {
                SonyEncoder.encode(12, device, subDevice, function, repeats = 3)
            }
            proto == "SONY15" || proto == "SIRC15" -> {
                SonyEncoder.encode(15, device, subDevice, function, repeats = 3)
            }
            proto == "SONY20" || proto == "SIRC20" -> {
                SonyEncoder.encode(20, device, subDevice, function, repeats = 3)
            }
            proto == "SONY" || proto == "SIRC" -> {
                // If subdevice is provided and valid, use 20-bit, otherwise 12-bit
                if (subDevice > 0) SonyEncoder.encode(20, device, subDevice, function, repeats = 3)
                else SonyEncoder.encode(12, device, subDevice, function, repeats = 3)
            }
            proto.startsWith("RC5") -> {
                Rc5Encoder.encode(device, function, repeats = 2)
            }
            proto.startsWith("RC6") || proto == "MCE" -> {
                Rc6Encoder.encode(device, function, repeats = 2)
            }
            proto.startsWith("JVC") -> {
                JvcEncoder.encode(device, function, repeats = 2)
            }
            proto.startsWith("PANASONIC") || proto == "KASEIKYO" || proto == "DENON-K" || proto == "TEAC-K" || proto.endsWith("-K") -> {
                PanasonicEncoder.encode(device, subDevice, function, repeats = 2)
            }
            proto.startsWith("SHARP") || proto.startsWith("DENON") -> {
                SharpEncoder.encode(device, function, repeats = 2)
            }
            proto == "RECS80" || proto.startsWith("RECS") -> {
                Recs80Encoder.encode(device, function, repeats = 2)
            }
            proto.startsWith("MITSUBISHI") -> {
                MitsubishiEncoder.encode(device, function, repeats = 2)
            }
            proto.startsWith("AIWA") -> {
                AiwaEncoder.encode(device, subDevice, function, repeats = 2)
            }
            else -> {
                // Fallback for generic NEC-derived or unknown
                if (proto.contains("NEC")) {
                    NecEncoder.encode(device, subDevice, function, repeats = 2)
                } else {
                    null
                }
            }
        }
    }
}

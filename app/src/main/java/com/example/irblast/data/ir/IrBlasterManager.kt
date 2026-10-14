package com.example.irblast.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

data class TransmitResult(
    val isSuccess: Boolean,
    val isSimulated: Boolean = false,
    val errorMessage: String? = null
)

class IrBlasterManager(private val context: Context) {
    private val tag = "IrBlasterManager"

    private val irManager: ConsumerIrManager? by lazy {
        try {
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Exception) {
            Log.e(tag, "Failed to get ConsumerIrManager service", e)
            null
        }
    }

    val isHardwareAvailable: Boolean
        get() = try {
            irManager?.hasIrEmitter() == true
        } catch (e: Exception) {
            Log.e(tag, "Error checking hasIrEmitter", e)
            false
        }

    val supportedFrequencyRanges: List<Pair<Int, Int>>
        get() {
            val ranges = try {
                irManager?.carrierFrequencies
            } catch (e: Exception) {
                Log.e(tag, "Error querying carrier frequencies", e)
                null
            } ?: return emptyList()
            return ranges.map { it.minFrequency to it.maxFrequency }
        }

    fun transmit(frequencyHz: Int, patternMicros: IntArray): TransmitResult {
        if (frequencyHz <= 0) {
            return TransmitResult(false, errorMessage = "Carrier frequency must be positive ($frequencyHz Hz)")
        }
        if (patternMicros.isEmpty()) {
            return TransmitResult(false, errorMessage = "Empty pattern")
        }
        if (patternMicros.any { it <= 0 }) {
            return TransmitResult(false, errorMessage = "Pattern contains non-positive pulse durations")
        }

        // Hardware HALs require an even number of elements (alternating on/off). Pad trailing space if odd.
        val safePattern = if (patternMicros.size % 2 != 0) {
            patternMicros + 20000
        } else {
            patternMicros
        }

        val manager = irManager
        val hasHardware = try {
            manager != null && manager.hasIrEmitter()
        } catch (e: Exception) {
            false
        }

        return if (hasHardware && manager != null) {
            try {
                val supportedRanges = supportedFrequencyRanges
                val clampedFrequency = if (supportedRanges.isNotEmpty()) {
                    if (supportedRanges.any { frequencyHz in it.first..it.second }) {
                        frequencyHz
                    } else {
                        val closestRange = supportedRanges.minByOrNull { range ->
                            if (frequencyHz < range.first) range.first - frequencyHz
                            else if (frequencyHz > range.second) frequencyHz - range.second
                            else 0
                        }
                        if (closestRange != null) {
                            frequencyHz.coerceIn(closestRange.first, closestRange.second)
                        } else {
                            frequencyHz
                        }
                    }
                } else {
                    frequencyHz
                }

                manager.transmit(clampedFrequency, safePattern)
                Log.d(tag, "Transmitted IR signal: ${clampedFrequency}Hz, ${safePattern.size} pulses")
                TransmitResult(isSuccess = true, isSimulated = false)
            } catch (e: Exception) {
                Log.e(tag, "Error during IR transmission", e)
                TransmitResult(isSuccess = false, errorMessage = e.message)
            }
        } else {
            // Emulated / fallback mode for development or devices without IR emitter
            Log.i(tag, "Simulated IR signal: ${frequencyHz}Hz, ${safePattern.size} pulses (no hardware IR emitter detected)")
            TransmitResult(isSuccess = true, isSimulated = true, errorMessage = "Simulated: No physical IR emitter detected")
        }
    }
}

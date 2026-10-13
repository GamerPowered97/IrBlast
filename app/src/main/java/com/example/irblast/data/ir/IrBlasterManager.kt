package com.example.irblast.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
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
        get() = irManager?.hasIrEmitter() == true

    val supportedFrequencyRanges: List<Pair<Int, Int>>
        get() {
            val ranges = irManager?.carrierFrequencies ?: return emptyList()
            return ranges.map { it.minFrequency to it.maxFrequency }
        }

    fun transmit(frequencyHz: Int, patternMicros: IntArray): TransmitResult {
        if (patternMicros.isEmpty()) {
            return TransmitResult(false, errorMessage = "Empty pattern")
        }

        val manager = irManager
        if (manager != null && manager.hasIrEmitter()) {
            return try {
                manager.transmit(frequencyHz, patternMicros)
                Log.d(tag, "Transmitted IR signal: ${frequencyHz}Hz, ${patternMicros.size} pulses")
                TransmitResult(isSuccess = true, isSimulated = false)
            } catch (e: Exception) {
                Log.e(tag, "Error during IR transmission", e)
                TransmitResult(isSuccess = false, errorMessage = e.message)
            }
        } else {
            // Emulated / fallback mode for development or devices without IR emitter
            Log.i(tag, "Simulated IR signal: ${frequencyHz}Hz, ${patternMicros.size} pulses (no hardware IR emitter detected)")
            return TransmitResult(isSuccess = true, isSimulated = true)
        }
    }
}

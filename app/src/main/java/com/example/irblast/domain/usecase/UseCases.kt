package com.example.irblast.domain.usecase

import com.example.irblast.data.ir.IrBlasterManager
import com.example.irblast.data.ir.TransmitResult
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.domain.model.IrSignal
import com.example.irblast.domain.model.IrdbCandidate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SendIrSignalUseCase(private val irBlasterManager: IrBlasterManager) {
    fun execute(signal: IrSignal): TransmitResult {
        return irBlasterManager.transmit(signal.frequencyHz, signal.patternMicros)
    }

    fun execute(frequencyHz: Int, patternMicros: IntArray): TransmitResult {
        return irBlasterManager.transmit(frequencyHz, patternMicros)
    }
}

sealed class ScanProgressEvent {
    data class Progress(
        val index: Int,
        val total: Int,
        val signal: IrSignal,
        val label: String,
        val candidate: IrdbCandidate? = null
    ) : ScanProgressEvent()

    object Complete : ScanProgressEvent()
}

class ScanCodeFinderUseCase(
    private val sendIrSignalUseCase: SendIrSignalUseCase
) {
    fun scanCandidates(
        candidates: List<IrdbCandidate>,
        delayMs: Long = 400L,
        startIndex: Int = 0
    ): Flow<ScanProgressEvent> = flow {
        val total = candidates.size
        val safeStart = startIndex.coerceIn(0, total)
        for (i in safeStart until total) {
            val candidate = candidates[i]
            val signal = ProtocolEncoder.encode(
                candidate.protocol,
                candidate.device,
                candidate.subdevice,
                candidate.function
            )
            if (signal != null) {
                sendIrSignalUseCase.execute(signal)
                emit(
                    ScanProgressEvent.Progress(
                        index = i + 1,
                        total = total,
                        signal = signal,
                        label = "${candidate.brand} ${candidate.model} [${candidate.functionName}]",
                        candidate = candidate
                    )
                )
                delay(delayMs)
            }
        }
        emit(ScanProgressEvent.Complete)
    }

    fun scanGeneral(
        protocol: String,
        functionCode: Int = 2, // Standard power code
        delayMs: Long = 350L,
        startAddress: Int = 0,
        maxAddresses: Int = 256
    ): Flow<ScanProgressEvent> = flow {
        val total = maxAddresses
        val safeStart = startAddress.coerceIn(0, total)
        for (addr in safeStart until total) {
            val signal = ProtocolEncoder.encode(
                protocol = protocol,
                device = addr,
                subDevice = -1,
                function = functionCode
            )
            if (signal != null) {
                sendIrSignalUseCase.execute(signal)
                emit(
                    ScanProgressEvent.Progress(
                        index = addr + 1,
                        total = total,
                        signal = signal,
                        label = "$protocol Addr: $addr, Cmd: $functionCode",
                        candidate = null
                    )
                )
                delay(delayMs)
            }
        }
        emit(ScanProgressEvent.Complete)
    }
}

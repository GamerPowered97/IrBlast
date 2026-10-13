package com.example.irblast

import android.app.Application
import android.content.Context
import com.example.irblast.data.db.IrdbDatabase
import com.example.irblast.data.db.UserRemoteDatabase
import com.example.irblast.data.ir.IrBlasterManager
import com.example.irblast.data.repository.IrdbRepository
import com.example.irblast.data.repository.RemoteRepository
import com.example.irblast.data.settings.AppSettingsManager
import com.example.irblast.domain.usecase.ScanCodeFinderUseCase
import com.example.irblast.domain.usecase.SendIrSignalUseCase

class IrBlastAppContainer(val context: Context) {
    val irBlasterManager: IrBlasterManager by lazy {
        IrBlasterManager(context)
    }

    val irdbDatabase: IrdbDatabase by lazy {
        IrdbDatabase(context)
    }

    val userRemoteDatabase: UserRemoteDatabase by lazy {
        UserRemoteDatabase(context)
    }

    val irdbRepository: IrdbRepository by lazy {
        IrdbRepository(irdbDatabase)
    }

    val remoteRepository: RemoteRepository by lazy {
        RemoteRepository(userRemoteDatabase, irdbDatabase)
    }

    val sendIrSignalUseCase: SendIrSignalUseCase by lazy {
        SendIrSignalUseCase(irBlasterManager)
    }

    val scanCodeFinderUseCase: ScanCodeFinderUseCase by lazy {
        ScanCodeFinderUseCase(sendIrSignalUseCase)
    }

    val appSettingsManager: AppSettingsManager by lazy {
        AppSettingsManager(context)
    }
}

class IrBlastApplication : Application() {
    lateinit var container: IrBlastAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = IrBlastAppContainer(this)
    }
}

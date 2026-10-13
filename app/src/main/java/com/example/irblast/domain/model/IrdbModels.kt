package com.example.irblast.domain.model

data class IrdbRemote(
    val id: Long,
    val brand: String,
    val deviceType: String,
    val category: String,
    val model: String
)

data class IrdbCode(
    val id: Long,
    val remoteId: Long,
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int
)

data class IrdbCandidate(
    val remoteId: Long,
    val brand: String,
    val deviceType: String,
    val category: String,
    val model: String,
    val functionName: String,
    val protocol: String,
    val device: Int,
    val subdevice: Int,
    val function: Int
)

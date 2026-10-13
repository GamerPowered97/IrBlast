package com.example.irblast

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data class RemoteControl(val remoteId: Long) : NavKey

@Serializable
data class Browse(val initialCategory: String = "") : NavKey

@Serializable
data class CodeFinder(
    val initialCategory: String = "",
    val initialBrand: String = ""
) : NavKey

@Serializable
data object CustomSignal : NavKey

@Serializable
data object Settings : NavKey

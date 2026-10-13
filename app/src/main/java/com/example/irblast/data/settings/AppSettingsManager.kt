package com.example.irblast.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    IRBLAST_DARK,
    SYSTEM,
    DYNAMIC
}

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("irblast_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _defaultScanDelay = MutableStateFlow(prefs.getLong("scan_delay_ms", 350L))
    val defaultScanDelay: StateFlow<Long> = _defaultScanDelay.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.IRBLAST_DARK.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.IRBLAST_DARK.name)
        } catch (e: Exception) {
            ThemeMode.IRBLAST_DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setDefaultScanDelay(delayMs: Long) {
        prefs.edit().putLong("scan_delay_ms", delayMs).apply()
        _defaultScanDelay.value = delayMs
    }
}

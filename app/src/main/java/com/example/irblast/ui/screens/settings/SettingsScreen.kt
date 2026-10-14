package com.example.irblast.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.data.settings.ThemeMode
import com.example.irblast.theme.IrSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: IrBlastAppContainer,
    onBack: () -> Unit
) {
    val themeMode by container.appSettingsManager.themeMode.collectAsState()
    val hapticsEnabled by container.appSettingsManager.hapticsEnabled.collectAsState()
    val scanDelayMs by container.appSettingsManager.defaultScanDelay.collectAsState()

    val isHardwareAvailable = container.irBlasterManager.isHardwareAvailable
    val frequencyRanges = container.irBlasterManager.supportedFrequencyRanges

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Hardware Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Theme Appearance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Choose your preferred color theme:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == ThemeMode.IRBLAST_DARK,
                            onClick = { container.appSettingsManager.setThemeMode(ThemeMode.IRBLAST_DARK) },
                            label = { Text("IrBlast Dark", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { container.appSettingsManager.setThemeMode(ThemeMode.SYSTEM) },
                            label = { Text("System", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = themeMode == ThemeMode.DYNAMIC,
                            onClick = { container.appSettingsManager.setThemeMode(ThemeMode.DYNAMIC) },
                            label = { Text("Dynamic (Material You)", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Haptics & Feedback
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Haptic Vibration Feedback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tactile Button Vibrations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Vibrate on remote button press", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = { container.appSettingsManager.setHapticsEnabled(it) }
                        )
                    }
                }
            }

            // Scan Interval Default
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Default Code Finder Scan Speed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interval Delay:", fontSize = 13.sp)
                        Text("${scanDelayMs} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Slider(
                        value = scanDelayMs.toFloat(),
                        onValueChange = { container.appSettingsManager.setDefaultScanDelay(it.toLong()) },
                        valueRange = 200f..1000f,
                        steps = 15
                    )
                }
            }

            // Hardware IR Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Device Hardware & IR Diagnostics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Hardware Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})",
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Android OS Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        fontSize = 13.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ConsumerIrManager Emitter: ", fontSize = 13.sp)
                        Text(
                            text = if (isHardwareAvailable) "ONLINE & ACTIVE" else "SIMULATED (No emitter detected)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHardwareAvailable) IrSuccess else MaterialTheme.colorScheme.error
                        )
                    }

                    if (frequencyRanges.isNotEmpty()) {
                        Text(text = "Supported Carrier Frequencies:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        frequencyRanges.forEach { range ->
                            Text(
                                text = "• ${range.first / 1000} kHz — ${range.second / 1000} kHz (${range.first} Hz to ${range.second} Hz)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Standard Supported Frequencies: 30 kHz — 60 kHz (NEC, Sony, RC5, RC6, JVC)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Bundled Offline Database: 3,243 remotes, 112,013 codes across 645 brands.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // About IrBlast
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "IrBlast v1.0",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tailored for Xiaomi Redmi Note 15 Pro 5G and all Android ConsumerIr devices. Built with Jetpack Compose & Material 3.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

package com.example.irblast.ui.screens.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.data.ir.encoders.ProntoHexParser
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.data.ir.encoders.RawPatternParser
import com.example.irblast.domain.model.IrSignal
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import com.example.irblast.theme.IrError
import com.example.irblast.theme.IrSuccess
import com.example.irblast.ui.components.HardwareStatusBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSignalScreen(
    container: IrBlastAppContainer,
    onBack: () -> Unit,
    onNavigateToRemote: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pronto Hex, 1 = Raw, 2 = Protocol

    // Pronto Hex state
    var prontoInput by remember { mutableStateOf("") }

    // Raw state
    var rawFrequency by remember { mutableStateOf("38000") }
    var rawInput by remember { mutableStateOf("") }

    // Protocol state
    var protoType by remember { mutableStateOf("NEC") }
    var protoDevice by remember { mutableStateOf("7") }
    var protoSubdevice by remember { mutableStateOf("7") }
    var protoFunction by remember { mutableStateOf("2") }

    var isBlasting by remember { mutableStateOf(false) }
    var lastParsedSignal by remember { mutableStateOf<IrSignal?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var savedRemotes by remember { mutableStateOf<List<SavedRemote>>(emptyList()) }

    LaunchedEffect(Unit) {
        savedRemotes = container.remoteRepository.getAllRemotes()
    }

    fun blast(signal: IrSignal) {
        coroutineScope.launch {
            isBlasting = true
            container.sendIrSignalUseCase.execute(signal)
            delay(250)
            isBlasting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Custom IR Signal Sender",
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
                .verticalScroll(rememberScrollState())
        ) {
            HardwareStatusBanner(
                isHardwareAvailable = container.irBlasterManager.isHardwareAvailable,
                isBlasting = isBlasting
            )

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pronto Hex", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Raw Pulses", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Protocol Builder", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Pronto Hex Tab
                    OutlinedTextField(
                        value = prontoInput,
                        onValueChange = {
                            prontoInput = it
                            parseError = null
                            val res = ProntoHexParser.parse(it)
                            lastParsedSignal = res.getOrNull()
                            if (res.isFailure && it.isNotBlank()) {
                                parseError = res.exceptionOrNull()?.message
                            }
                        },
                        label = { Text("Pronto Hex String") },
                        placeholder = { Text("0000 006d 0022 0002 0155 00aa...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            // Standard Samsung TV Power Pronto code
                            prontoInput = "0000 006d 0022 0000 0156 00ac 0015 0041 0015 0041 0015 0041 0015 0016 0015 0016 0015 0016 0015 0016 0015 0016 0015 0041 0015 0041 0015 0041 0015 0016 0015 0016 0015 0016 0015 0016 0015 0016 0015 0016 0015 0041 0015 0016 0015 0016 0015 0016 0015 0016 0015 0016 0015 0016 0015 0041 0015 0016 0015 0041 0015 0041 0015 0041 0015 0041 0015 0041 0015 0041 0015 0689"
                            val res = ProntoHexParser.parse(prontoInput)
                            lastParsedSignal = res.getOrNull()
                        }
                    ) {
                        Text("Paste Sample Samsung Power Pronto", fontSize = 12.sp)
                    }
                }
                1 -> {
                    // Raw Pulses Tab
                    OutlinedTextField(
                        value = rawFrequency,
                        onValueChange = { rawFrequency = it },
                        label = { Text("Carrier Frequency (Hz)") },
                        placeholder = { Text("38000") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = {
                            rawInput = it
                            parseError = null
                            val res = RawPatternParser.parse(it, rawFrequency.toIntOrNull() ?: 38000)
                            lastParsedSignal = res.getOrNull()
                            if (res.isFailure && it.isNotBlank()) {
                                parseError = res.exceptionOrNull()?.message
                            }
                        },
                        label = { Text("Microsecond Timings (Mark, Space, Mark...)") },
                        placeholder = { Text("9000, 4500, 560, 560, 560, 1690...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            rawFrequency = "38000"
                            rawInput = "9000, 4500, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560"
                            val res = RawPatternParser.parse(rawInput, 38000)
                            lastParsedSignal = res.getOrNull()
                        }
                    ) {
                        Text("Paste Sample Raw Pulses", fontSize = 12.sp)
                    }
                }
                2 -> {
                    // Protocol Builder
                    Text(text = "Protocol Architecture:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("NEC", "SONY12", "SONY15", "RC5", "RC6", "JVC").forEach { p ->
                            FilterChip(
                                selected = protoType == p,
                                onClick = {
                                    protoType = p
                                    lastParsedSignal = ProtocolEncoder.encode(
                                        p,
                                        protoDevice.toIntOrNull() ?: 0,
                                        protoSubdevice.toIntOrNull() ?: -1,
                                        protoFunction.toIntOrNull() ?: 0
                                    )
                                },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = protoDevice,
                            onValueChange = {
                                protoDevice = it
                                lastParsedSignal = ProtocolEncoder.encode(
                                    protoType,
                                    it.toIntOrNull() ?: 0,
                                    protoSubdevice.toIntOrNull() ?: -1,
                                    protoFunction.toIntOrNull() ?: 0
                                )
                            },
                            label = { Text("Device") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = protoSubdevice,
                            onValueChange = {
                                protoSubdevice = it
                                lastParsedSignal = ProtocolEncoder.encode(
                                    protoType,
                                    protoDevice.toIntOrNull() ?: 0,
                                    it.toIntOrNull() ?: -1,
                                    protoFunction.toIntOrNull() ?: 0
                                )
                            },
                            label = { Text("Subdevice") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = protoFunction,
                            onValueChange = {
                                protoFunction = it
                                lastParsedSignal = ProtocolEncoder.encode(
                                    protoType,
                                    protoDevice.toIntOrNull() ?: 0,
                                    protoSubdevice.toIntOrNull() ?: -1,
                                    it.toIntOrNull() ?: 0
                                )
                            },
                            label = { Text("Function") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    LaunchedEffect(protoType, protoDevice, protoSubdevice, protoFunction) {
                        lastParsedSignal = ProtocolEncoder.encode(
                            protoType,
                            protoDevice.toIntOrNull() ?: 0,
                            protoSubdevice.toIntOrNull() ?: -1,
                            protoFunction.toIntOrNull() ?: 0
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Parse Inspection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Signal Analysis Preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val sig = lastParsedSignal
                    if (sig != null) {
                        Text(text = "Carrier Frequency: ${sig.frequencyHz} Hz", fontSize = 13.sp)
                        Text(text = "Pulses count: ${sig.patternMicros.size} timings", fontSize = 13.sp)
                        Text(text = "Estimated transmission burst duration: ${sig.totalDurationMs} ms", fontSize = 13.sp)
                        Text(
                            text = "Timing preview: ${sig.patternMicros.take(8).joinToString(", ")}...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (parseError != null) {
                        Text(
                            text = "Error: $parseError",
                            color = IrError,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Enter a signal above to inspect and transmit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = lastParsedSignal != null,
                    onClick = { lastParsedSignal?.let { blast(it) } },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRANSMIT IR", fontWeight = FontWeight.Bold)
                }

                Button(
                    enabled = lastParsedSignal != null,
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE AS BUTTON", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Save to Remote Dialog
    if (showSaveDialog && lastParsedSignal != null) {
        val signalToSave = lastParsedSignal!!
        var buttonLabel by remember { mutableStateOf("Custom Signal") }
        var selectedRemoteId by remember { mutableStateOf(savedRemotes.firstOrNull()?.id ?: -1L) }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Signal as Button") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = buttonLabel,
                        onValueChange = { buttonLabel = it },
                        label = { Text("Button Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (savedRemotes.isNotEmpty()) {
                        Text(text = "Select Remote to Add To:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        savedRemotes.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRemoteId = r.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${r.icon} ${r.name} (${r.brand})",
                                    color = if (selectedRemoteId == r.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selectedRemoteId == r.id) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        Text("No remotes exist. A new remote 'Custom IR Remote' will be created.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val targetRemoteId = if (selectedRemoteId != -1L) {
                                selectedRemoteId
                            } else {
                                container.remoteRepository.saveRemote(
                                    SavedRemote(
                                        name = "Custom IR Remote",
                                        brand = "Custom",
                                        deviceType = "Device",
                                        category = "Other",
                                        icon = "⚡"
                                    )
                                )
                            }

                            container.remoteRepository.saveButton(
                                SavedButton(
                                    remoteId = targetRemoteId,
                                    label = buttonLabel.trim(),
                                    frequencyHz = signalToSave.frequencyHz,
                                    patternMicros = signalToSave.patternMicros.joinToString(","),
                                    protocol = signalToSave.protocol
                                )
                            )

                            showSaveDialog = false
                            onNavigateToRemote(targetRemoteId)
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

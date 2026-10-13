package com.example.irblast.ui.screens.codefinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.domain.model.IrSignal
import com.example.irblast.domain.model.IrdbCandidate
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import com.example.irblast.domain.usecase.ScanProgressEvent
import com.example.irblast.theme.IrError
import com.example.irblast.theme.IrSuccess
import com.example.irblast.ui.components.HardwareStatusBanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeFinderScreen(
    container: IrBlastAppContainer,
    initialCategory: String = "",
    initialBrand: String = "",
    onBack: () -> Unit,
    onRemoteImported: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Smart Scan, 1 = Full Space Scan

    // Smart Scan States
    var smartCategory by remember { mutableStateOf(initialCategory.ifBlank { "All" }) }
    var smartBrand by remember { mutableStateOf(initialBrand) }
    var smartFunction by remember { mutableStateOf("POWER") }
    var smartCandidates by remember { mutableStateOf<List<IrdbCandidate>>(emptyList()) }

    // General Full Scan States
    var generalProtocol by remember { mutableStateOf("NEC") }
    var generalFunctionCode by remember { mutableIntStateOf(2) }

    // Scanning Execution States
    var isScanning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var scanProgressIndex by remember { mutableIntStateOf(0) }
    var scanTotal by remember { mutableIntStateOf(0) }
    var currentSignalLabel by remember { mutableStateOf("") }
    var lastHitCandidate by remember { mutableStateOf<IrdbCandidate?>(null) }
    var lastHitSignal by remember { mutableStateOf<IrSignal?>(null) }
    var showMatchDialog by remember { mutableStateOf(false) }

    var scanDelayMs by remember { mutableFloatStateOf(400f) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val categories = listOf("All", "TV", "Projector", "Audio", "AC / Climate", "Set-Top Box", "Media Player")
    val protocols = listOf("NEC", "SONY12", "SONY15", "RC5", "RC6", "JVC", "PANASONIC", "SHARP")

    fun loadSmartCandidates() {
        coroutineScope.launch {
            smartCandidates = container.irdbRepository.getCandidatesForSmartScan(
                category = if (smartCategory == "All") null else smartCategory,
                brand = smartBrand.trim().ifBlank { null },
                functionTarget = smartFunction
            )
        }
    }

    LaunchedEffect(smartCategory, smartBrand, smartFunction) {
        if (selectedTab == 0) {
            loadSmartCandidates()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
        isPaused = false
        scanProgressIndex = 0
    }

    fun pauseScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
        isPaused = true
    }

    fun testCurrentSignal() {
        lastHitSignal?.let { sig ->
            if (sig.patternMicros.isEmpty()) {
                Toast.makeText(context, "Protocol '${sig.protocol}' is not yet supported", Toast.LENGTH_SHORT).show()
                return
            }
            coroutineScope.launch {
                val result = container.sendIrSignalUseCase.execute(sig)
                if (!result.isSuccess) {
                    Toast.makeText(context, result.errorMessage ?: "Transmission failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startSmartScan(resume: Boolean = false) {
        if (smartCandidates.isEmpty()) return
        scanJob?.cancel()
        isScanning = true
        isPaused = false
        scanTotal = smartCandidates.size
        val startIdx = if (resume) maxOf(0, scanProgressIndex - 1) else 0
        if (!resume) scanProgressIndex = 0

        scanJob = coroutineScope.launch {
            container.scanCodeFinderUseCase.scanCandidates(
                candidates = smartCandidates,
                delayMs = scanDelayMs.toLong(),
                startIndex = startIdx
            ).collect { event ->
                when (event) {
                    is ScanProgressEvent.Progress -> {
                        scanProgressIndex = event.index
                        currentSignalLabel = event.label
                        lastHitCandidate = event.candidate
                        lastHitSignal = event.signal
                    }
                    is ScanProgressEvent.Complete -> {
                        isScanning = false
                        isPaused = false
                    }
                }
            }
        }
    }

    fun startGeneralScan(resume: Boolean = false) {
        scanJob?.cancel()
        isScanning = true
        isPaused = false
        scanTotal = 256
        val startAddr = if (resume) maxOf(0, scanProgressIndex - 1) else 0
        if (!resume) scanProgressIndex = 0

        scanJob = coroutineScope.launch {
            container.scanCodeFinderUseCase.scanGeneral(
                protocol = generalProtocol,
                functionCode = generalFunctionCode,
                delayMs = scanDelayMs.toLong(),
                startAddress = startAddr
            ).collect { event ->
                when (event) {
                    is ScanProgressEvent.Progress -> {
                        scanProgressIndex = event.index
                        currentSignalLabel = event.label
                        lastHitCandidate = null
                        lastHitSignal = event.signal
                    }
                    is ScanProgressEvent.Complete -> {
                        isScanning = false
                        isPaused = false
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Code Finder Auto-Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        stopScan()
                        onBack()
                    }) {
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
                isBlasting = isScanning
            )

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        stopScan()
                        selectedTab = 0
                    },
                    text = { Text("Smart Filtered Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        stopScan()
                        selectedTab = 1
                    },
                    text = { Text("General Full Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Smart Scan Configuration
                Text(
                    text = "1. Select Device Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = smartCategory == cat,
                            onClick = {
                                stopScan()
                                smartCategory = cat
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = smartBrand,
                    onValueChange = {
                        stopScan()
                        smartBrand = it
                    },
                    label = { Text("Brand Filter (Optional)") },
                    placeholder = { Text("e.g. Epson, Samsung, BenQ...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Target Test Function",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("POWER", "MUTE", "VOL+", "INPUT").forEach { fn ->
                        FilterChip(
                            selected = smartFunction == fn,
                            onClick = {
                                stopScan()
                                smartFunction = fn
                            },
                            label = { Text(fn, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Matching candidates: ${smartCandidates.size}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // General Full Scan Configuration
                Text(
                    text = "1. Protocol Architecture",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    protocols.forEach { proto ->
                        FilterChip(
                            selected = generalProtocol == proto,
                            onClick = {
                                stopScan()
                                generalProtocol = proto
                            },
                            label = { Text(proto, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = generalFunctionCode.toString(),
                    onValueChange = {
                        stopScan()
                        generalFunctionCode = it.toIntOrNull() ?: 2
                    },
                    label = { Text("Function Command Code") },
                    placeholder = { Text("2 (Standard Power), 15 (Mute)...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Cycles 256 address spaces (0x00 to 0xFF) transmitting command $generalFunctionCode",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Scan Interval Speed:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = "${scanDelayMs.toInt()} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = scanDelayMs,
                onValueChange = { scanDelayMs = it },
                valueRange = 250f..1000f,
                steps = 14
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scan Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                isScanning -> "SCANNING IN PROGRESS..."
                                isPaused -> "SCAN PAUSED (READY TO RESUME)"
                                else -> "SCAN STATUS: READY"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isScanning -> MaterialTheme.colorScheme.primary
                                isPaused -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = "$scanProgressIndex / $scanTotal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val progressFraction = if (scanTotal > 0) scanProgressIndex.toFloat() / scanTotal.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (currentSignalLabel.isNotBlank()) "Signal: $currentSignalLabel" else "Waiting for start...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if ((isScanning || isPaused) && scanTotal > 0) {
                        val remainingMs = (scanTotal - scanProgressIndex) * scanDelayMs.toLong()
                        val sec = (remainingMs / 1000) % 60
                        val min = (remainingMs / 1000) / 60
                        Text(
                            text = "Estimated Time Remaining: ${min}m ${sec}s",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scan Trigger Buttons
            if (!isScanning && !isPaused) {
                Button(
                    onClick = {
                        if (selectedTab == 0) startSmartScan(resume = false) else startGeneralScan(resume = false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == 0) "START SMART SCAN" else "START FULL SCAN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else if (isScanning) {
                // When scanning: Big Device Responded button!
                Button(
                    onClick = {
                        pauseScan()
                        showMatchDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IrSuccess)
                ) {
                    Text(
                        text = "🎯 DEVICE RESPONDED! STOP",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { pauseScan() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause")
                    }

                    Button(
                        onClick = { stopScan() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IrError)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel")
                    }
                }
            } else {
                // When PAUSED
                Button(
                    onClick = {
                        showMatchDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IrSuccess)
                ) {
                    Text(
                        text = "🎯 DEVICE RESPONDED! SAVE",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedTab == 0) startSmartScan(resume = true) else startGeneralScan(resume = true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume")
                    }

                    Button(
                        onClick = { testCurrentSignal() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Again")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { stopScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset / Cancel Scan")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Match Found / Import Dialog
    if (showMatchDialog) {
        AlertDialog(
            onDismissRequest = { showMatchDialog = false },
            title = { Text("Code Matched!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Great! Your device responded to this signal:",
                        fontSize = 13.sp
                    )
                    Text(
                        text = currentSignalLabel,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (lastHitCandidate != null) {
                        Text(
                            text = "Would you like to import the complete remote button set from this profile into your remotes?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Would you like to save this working code as a new remote?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                val candidate = lastHitCandidate
                val signal = lastHitSignal
                if (candidate != null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val newId = container.remoteRepository.importIrdbRemote(
                                    irdbRemoteId = candidate.remoteId,
                                    customName = "${candidate.brand} ${candidate.model}",
                                    onUnsupportedProtocol = { unsupportedProtocol ->
                                        Toast.makeText(context, "Protocol '$unsupportedProtocol' is not yet supported", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                showMatchDialog = false
                                stopScan()
                                onRemoteImported(newId)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Full Remote")
                    }
                } else if (signal != null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val remoteName = "Discovered $generalProtocol Remote"
                                val newRemoteId = container.remoteRepository.saveRemote(
                                    SavedRemote(
                                        name = remoteName,
                                        brand = "Generic",
                                        deviceType = "Electronic",
                                        category = "Other",
                                        icon = "⚡"
                                    )
                                )
                                container.remoteRepository.saveButton(
                                    SavedButton(
                                        remoteId = newRemoteId,
                                        label = "POWER",
                                        frequencyHz = signal.frequencyHz,
                                        patternMicros = signal.patternMicros.joinToString(","),
                                        protocol = signal.protocol,
                                        functionCode = generalFunctionCode
                                    )
                                )
                                showMatchDialog = false
                                stopScan()
                                onRemoteImported(newRemoteId)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save as New Remote")
                    }
                } else {
                    Button(
                        onClick = { showMatchDialog = false }
                    ) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMatchDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

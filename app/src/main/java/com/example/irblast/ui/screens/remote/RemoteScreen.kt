package com.example.irblast.ui.screens.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.data.ir.encoders.ProntoHexParser
import com.example.irblast.data.ir.encoders.RawPatternParser
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import com.example.irblast.theme.IrError
import com.example.irblast.ui.components.ButtonStyleVariant
import com.example.irblast.ui.components.DPad
import com.example.irblast.ui.components.HardwareStatusBanner
import com.example.irblast.ui.components.IrButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RemoteScreen(
    remoteId: Long,
    container: IrBlastAppContainer,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var remote by remember { mutableStateOf<SavedRemote?>(null) }
    var buttons by remember { mutableStateOf<List<SavedButton>>(emptyList()) }
    var isBlasting by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var buttonToEdit by remember { mutableStateOf<SavedButton?>(null) }

    val hapticsEnabled by container.appSettingsManager.hapticsEnabled.collectAsState()

    fun loadData() {
        coroutineScope.launch {
            remote = container.remoteRepository.getRemoteById(remoteId)
            buttons = container.remoteRepository.getButtonsForRemote(remoteId)
        }
    }

    LaunchedEffect(remoteId) {
        loadData()
    }

    fun blast(button: SavedButton) {
        coroutineScope.launch {
            isBlasting = true
            container.sendIrSignalUseCase.execute(
                button.frequencyHz,
                button.toPatternIntArray()
            )
            delay(250)
            isBlasting = false
        }
    }

    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", ignoreCase = true) || it.label.contains("STANDBY", ignoreCase = true) }
    val muteBtn = buttons.firstOrNull { it.label.contains("MUTE", ignoreCase = true) }
    val sourceBtn = buttons.firstOrNull { it.label.contains("SOURCE", ignoreCase = true) || it.label.contains("INPUT", ignoreCase = true) }

    val volUpBtn = buttons.firstOrNull { it.label.contains("VOL", ignoreCase = true) && ("+" in it.label || "UP" in it.label) }
    val volDownBtn = buttons.firstOrNull { it.label.contains("VOL", ignoreCase = true) && ("-" in it.label || "DOWN" in it.label) }
    val chUpBtn = buttons.firstOrNull { it.label.contains("CH", ignoreCase = true) && ("+" in it.label || "UP" in it.label) }
    val chDownBtn = buttons.firstOrNull { it.label.contains("CH", ignoreCase = true) && ("-" in it.label || "DOWN" in it.label) }

    val upBtn = buttons.firstOrNull { it.label.equals("UP", ignoreCase = true) }
    val downBtn = buttons.firstOrNull { it.label.equals("DOWN", ignoreCase = true) }
    val leftBtn = buttons.firstOrNull { it.label.equals("LEFT", ignoreCase = true) }
    val rightBtn = buttons.firstOrNull { it.label.equals("RIGHT", ignoreCase = true) }
    val okBtn = buttons.firstOrNull { it.label.equals("OK", ignoreCase = true) || it.label.equals("ENTER", ignoreCase = true) || it.label.equals("SELECT", ignoreCase = true) }

    val backBtn = buttons.firstOrNull { it.label.contains("BACK", ignoreCase = true) || it.label.contains("RETURN", ignoreCase = true) || it.label.contains("EXIT", ignoreCase = true) }
    val menuBtn = buttons.firstOrNull { it.label.contains("MENU", ignoreCase = true) }
    val homeBtn = buttons.firstOrNull { it.label.contains("HOME", ignoreCase = true) }

    // Numbers 0 to 9
    val numButtons = (0..9).map { digit ->
        buttons.firstOrNull { it.label.trim() == digit.toString() }
    }

    // Filter out structured buttons from the general grid
    val structuredButtonIds = setOfNotNull(
        powerBtn?.id, muteBtn?.id, sourceBtn?.id,
        volUpBtn?.id, volDownBtn?.id, chUpBtn?.id, chDownBtn?.id,
        upBtn?.id, downBtn?.id, leftBtn?.id, rightBtn?.id, okBtn?.id,
        backBtn?.id, menuBtn?.id, homeBtn?.id
    ) + numButtons.mapNotNull { it?.id }

    val remainingButtons = buttons.filter { it.id !in structuredButtonIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = remote?.name ?: "Remote Control",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${remote?.brand ?: ""} • ${remote?.deviceType ?: ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNumpad = !showNumpad }) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = "Keypad",
                            tint = if (showNumpad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddButtonDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Button")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HardwareStatusBanner(
                isHardwareAvailable = container.irBlasterManager.isHardwareAvailable,
                isBlasting = isBlasting
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Power & Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (powerBtn != null) {
                    IrButton(
                        label = powerBtn.label,
                        icon = "power",
                        variant = ButtonStyleVariant.POWER,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        onClick = { blast(powerBtn) }
                    )
                }

                if (muteBtn != null) {
                    IrButton(
                        label = muteBtn.label,
                        icon = "mute",
                        variant = ButtonStyleVariant.CIRCLE,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        onClick = { blast(muteBtn) }
                    )
                }

                if (sourceBtn != null) {
                    IrButton(
                        label = sourceBtn.label,
                        icon = "input",
                        variant = ButtonStyleVariant.CIRCLE,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        onClick = { blast(sourceBtn) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Volume & Channel Cluster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume Column Rocker
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    volUpBtn?.let {
                        IrButton(
                            label = "VOL +",
                            icon = "vol_up",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("VOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    volDownBtn?.let {
                        IrButton(
                            label = "VOL -",
                            icon = "vol_down",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                }

                // Middle Quick Navigation (Home / Menu)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    homeBtn?.let {
                        IrButton(
                            label = "HOME",
                            icon = "home",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                    menuBtn?.let {
                        IrButton(
                            label = "MENU",
                            icon = "menu",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                }

                // Channel Column Rocker
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    chUpBtn?.let {
                        IrButton(
                            label = "CH +",
                            icon = "up",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("CH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    chDownBtn?.let {
                        IrButton(
                            label = "CH -",
                            icon = "down",
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onClick = { blast(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // D-Pad Section
            if (upBtn != null || downBtn != null || leftBtn != null || rightBtn != null || okBtn != null) {
                DPad(
                    hapticsEnabled = hapticsEnabled,
                    onUp = { upBtn?.let { blast(it) } },
                    onDown = { downBtn?.let { blast(it) } },
                    onLeft = { leftBtn?.let { blast(it) } },
                    onRight = { rightBtn?.let { blast(it) } },
                    onOk = { okBtn?.let { blast(it) } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back & Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                backBtn?.let {
                    IrButton(
                        label = "BACK",
                        icon = "back",
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier.size(width = 100.dp, height = 44.dp),
                        onClick = { blast(it) }
                    )
                }
            }

            // Expandable Numeric Keypad (0-9)
            AnimatedVisibility(visible = showNumpad) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = listOf(
                        listOf(1, 2, 3),
                        listOf(4, 5, 6),
                        listOf(7, 8, 9),
                        listOf(-1, 0, -2)
                    )

                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (num in row) {
                                if (num >= 0) {
                                    val btn = buttons.firstOrNull { it.label.trim() == num.toString() }
                                    IrButton(
                                        label = num.toString(),
                                        hapticsEnabled = hapticsEnabled,
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        onClick = { btn?.let { blast(it) } }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(56.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Custom Buttons Grid
            if (remainingButtons.isNotEmpty()) {
                Text(
                    text = "Additional Functions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    remainingButtons.forEach { btn ->
                        IrButton(
                            label = btn.label,
                            icon = btn.icon,
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier
                                .height(46.dp)
                                .defaultMinSize(minWidth = 80.dp),
                            onClick = { blast(btn) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add Button Dialog
    if (showAddButtonDialog) {
        AddCustomButtonDialog(
            remoteId = remoteId,
            onDismiss = { showAddButtonDialog = false },
            onSave = { btn ->
                coroutineScope.launch {
                    container.remoteRepository.saveButton(btn)
                    showAddButtonDialog = false
                    loadData()
                }
            }
        )
    }
}

@Composable
private fun AddCustomButtonDialog(
    remoteId: Long,
    onDismiss: () -> Unit,
    onSave: (SavedButton) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var prontoOrRaw by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("38000") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Button") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Button Label") },
                    placeholder = { Text("e.g. HDMI 2, MUTE, SLEEP") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = prontoOrRaw,
                    onValueChange = {
                        prontoOrRaw = it
                        errorMsg = null
                    },
                    label = { Text("Pronto Hex or Raw Pulses") },
                    placeholder = { Text("0000 006d... or 9000, 4500, 560...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                errorMsg?.let {
                    Text(text = it, color = IrError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && prontoOrRaw.isNotBlank(),
                onClick = {
                    val input = prontoOrRaw.trim()
                    val signalResult = if (input.startsWith("0000")) {
                        ProntoHexParser.parse(input)
                    } else {
                        RawPatternParser.parse(input, frequency.toIntOrNull() ?: 38000)
                    }

                    if (signalResult.isSuccess) {
                        val signal = signalResult.getOrThrow()
                        onSave(
                            SavedButton(
                                remoteId = remoteId,
                                label = label.trim(),
                                frequencyHz = signal.frequencyHz,
                                patternMicros = signal.patternMicros.joinToString(","),
                                protocol = signal.protocol
                            )
                        )
                    } else {
                        errorMsg = signalResult.exceptionOrNull()?.message ?: "Invalid code format"
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

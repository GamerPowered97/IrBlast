package com.example.irblast.ui.screens.remote

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.data.ir.encoders.ProntoHexParser
import com.example.irblast.data.ir.encoders.RawPatternParser
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.domain.model.SavedRemote
import com.example.irblast.theme.IrError
import com.example.irblast.theme.IrWarning
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
    onBack: () -> Unit,
    onNavigateToBrowse: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var remote by remember { mutableStateOf<SavedRemote?>(null) }
    var buttons by remember { mutableStateOf<List<SavedButton>>(emptyList()) }
    var isBlasting by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
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
        if (isEditMode) {
            buttonToEdit = button
            return
        }
        val pattern = button.toPatternIntArray()
        if (pattern.isEmpty()) {
            Toast.makeText(context, "No pulse pattern stored for ${button.label}", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            isBlasting = true
            val result = container.sendIrSignalUseCase.execute(
                button.frequencyHz,
                pattern
            )
            if (!result.isSuccess && !result.isSimulated) {
                Toast.makeText(context, result.errorMessage ?: "Transmission failed", Toast.LENGTH_SHORT).show()
            }
            delay(250)
            isBlasting = false
        }
    }

    // Determine layout category
    val category = (remote?.category ?: "").trim()
    val devType = (remote?.deviceType ?: "").trim()

    val isAc = category.contains("AC", ignoreCase = true) ||
            category.contains("Climate", ignoreCase = true) ||
            devType.contains("Air", ignoreCase = true) ||
            devType.contains("Climate", ignoreCase = true)

    val isProjector = category.contains("Projector", ignoreCase = true) ||
            devType.contains("Projector", ignoreCase = true)

    val isAudio = category.contains("Audio", ignoreCase = true) ||
            devType.contains("Audio", ignoreCase = true) ||
            devType.contains("Receiver", ignoreCase = true) ||
            devType.contains("Soundbar", ignoreCase = true) ||
            devType.contains("Amp", ignoreCase = true)

    val isFan = category.contains("Fan", ignoreCase = true) ||
            devType.contains("Fan", ignoreCase = true)

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
                    // Edit Mode Toggle
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Done Editing" else "Edit Buttons",
                            tint = if (isEditMode) IrWarning else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Keypad Toggle (mainly for TV / STB)
                    if (!isAc && !isFan) {
                        IconButton(onClick = { showNumpad = !showNumpad }) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = "Keypad",
                                tint = if (showNumpad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HardwareStatusBanner(
                isHardwareAvailable = container.irBlasterManager.isHardwareAvailable,
                isBlasting = isBlasting
            )

            // Edit Mode Alert Banner
            if (isEditMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = IrWarning.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, IrWarning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🛠️ Edit Mode: Tap any button to edit or delete it",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IrWarning
                        )
                        TextButton(onClick = { isEditMode = false }) {
                            Text("Done", fontWeight = FontWeight.Bold, color = IrWarning)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (buttons.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Buttons Configured",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This remote doesn't have any IR buttons configured yet. You can import from the database or add custom IR buttons.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (onNavigateToBrowse != null) {
                                Button(
                                    onClick = onNavigateToBrowse,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import from DB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { showAddButtonDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Button", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                when {
                    isAc -> {
                        AcRemoteLayout(
                            buttons = buttons,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            onBlast = { blast(it) },
                            onEdit = { buttonToEdit = it }
                        )
                    }
                    isProjector -> {
                        ProjectorRemoteLayout(
                            buttons = buttons,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            onBlast = { blast(it) },
                            onEdit = { buttonToEdit = it }
                        )
                    }
                    isAudio -> {
                        AudioRemoteLayout(
                            buttons = buttons,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            onBlast = { blast(it) },
                            onEdit = { buttonToEdit = it }
                        )
                    }
                    isFan -> {
                        FanRemoteLayout(
                            buttons = buttons,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            onBlast = { blast(it) },
                            onEdit = { buttonToEdit = it }
                        )
                    }
                    else -> {
                        TvRemoteLayout(
                            buttons = buttons,
                            showNumpad = showNumpad,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            onBlast = { blast(it) },
                            onEdit = { buttonToEdit = it }
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

    // Edit / Delete Button Dialog
    buttonToEdit?.let { btn ->
        EditOrDeleteButtonDialog(
            button = btn,
            onDismiss = { buttonToEdit = null },
            onTest = { blast(btn) },
            onSave = { updated ->
                coroutineScope.launch {
                    container.remoteRepository.saveButton(updated)
                    buttonToEdit = null
                    loadData()
                }
            },
            onDelete = {
                coroutineScope.launch {
                    container.remoteRepository.deleteButton(btn.id)
                    buttonToEdit = null
                    loadData()
                }
            }
        )
    }
}

// ==========================================
// 1. TV / UNIVERSAL LAYOUT
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvRemoteLayout(
    buttons: List<SavedButton>,
    showNumpad: Boolean,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
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

    val numButtons = (0..9).map { digit ->
        buttons.firstOrNull { it.label.trim() == digit.toString() }
    }

    val structuredButtonIds = setOfNotNull(
        powerBtn?.id, muteBtn?.id, sourceBtn?.id,
        volUpBtn?.id, volDownBtn?.id, chUpBtn?.id, chDownBtn?.id,
        upBtn?.id, downBtn?.id, leftBtn?.id, rightBtn?.id, okBtn?.id,
        backBtn?.id, menuBtn?.id, homeBtn?.id
    ) + numButtons.mapNotNull { it?.id }

    val remainingButtons = buttons.filter { it.id !in structuredButtonIds }

    // Power & Top Row
    if (powerBtn != null || muteBtn != null || sourceBtn != null) {
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
                    isEditMode = isEditMode,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(powerBtn) },
                    onClick = { onBlast(powerBtn) }
                )
            }
            if (muteBtn != null) {
                IrButton(
                    label = muteBtn.label,
                    icon = "mute",
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(muteBtn) },
                    onClick = { onBlast(muteBtn) }
                )
            }
            if (sourceBtn != null) {
                IrButton(
                    label = sourceBtn.label,
                    icon = "input",
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(sourceBtn) },
                    onClick = { onBlast(sourceBtn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Volume & Channel Cluster
    val hasVolRockers = volUpBtn != null || volDownBtn != null
    val hasChRockers = chUpBtn != null || chDownBtn != null
    val hasMiddleNav = homeBtn != null || menuBtn != null

    if (hasVolRockers || hasChRockers || hasMiddleNav) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasVolRockers) {
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(34.dp))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    volUpBtn?.let {
                        IrButton(
                            label = "VOL +",
                            icon = "vol_up",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("VOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    volDownBtn?.let {
                        IrButton(
                            label = "VOL -",
                            icon = "vol_down",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                }
            }

            if (hasMiddleNav) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    homeBtn?.let {
                        IrButton(
                            label = "HOME",
                            icon = "home",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                    menuBtn?.let {
                        IrButton(
                            label = "MENU",
                            icon = "menu",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                }
            }

            if (hasChRockers) {
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(34.dp))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    chUpBtn?.let {
                        IrButton(
                            label = "CH +",
                            icon = "up",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("CH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    chDownBtn?.let {
                        IrButton(
                            label = "CH -",
                            icon = "down",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // D-Pad Section
    if (upBtn != null || downBtn != null || leftBtn != null || rightBtn != null || okBtn != null) {
        DPad(
            hapticsEnabled = hapticsEnabled,
            onUp = { upBtn?.let { onBlast(it) } },
            onDown = { downBtn?.let { onBlast(it) } },
            onLeft = { leftBtn?.let { onBlast(it) } },
            onRight = { rightBtn?.let { onBlast(it) } },
            onOk = { okBtn?.let { onBlast(it) } }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Back & Exit
    backBtn?.let {
        IrButton(
            label = "BACK",
            icon = "back",
            hapticsEnabled = hapticsEnabled,
            isEditMode = isEditMode,
            modifier = Modifier.size(width = 110.dp, height = 44.dp),
            onLongClick = { onEdit(it) },
            onClick = { onBlast(it) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Numeric Keypad
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (num in row) {
                        if (num >= 0) {
                            val btn = buttons.firstOrNull { it.label.trim() == num.toString() }
                            IrButton(
                                label = num.toString(),
                                hapticsEnabled = hapticsEnabled,
                                isEditMode = isEditMode,
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                onLongClick = { btn?.let { onEdit(it) } },
                                onClick = { btn?.let { onBlast(it) } }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(56.dp))
                        }
                    }
                }
            }
        }
    }

    // Additional Custom Buttons
    RenderAdditionalButtons(
        buttons = remainingButtons,
        hapticsEnabled = hapticsEnabled,
        isEditMode = isEditMode,
        onBlast = onBlast,
        onEdit = onEdit
    )
}

// ==========================================
// 2. AC / CLIMATE LAYOUT
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AcRemoteLayout(
    buttons: List<SavedButton>,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", true) || it.label.contains("ON", true) || it.label.contains("OFF", true) }
    val tempUpBtn = buttons.firstOrNull { it.label.contains("TEMP", true) && ("+" in it.label || "UP" in it.label) }
        ?: buttons.firstOrNull { it.label.equals("UP", true) }
    val tempDownBtn = buttons.firstOrNull { it.label.contains("TEMP", true) && ("-" in it.label || "DOWN" in it.label) }
        ?: buttons.firstOrNull { it.label.equals("DOWN", true) }

    val modeButtons = buttons.filter {
        it.label.contains("MODE", true) || it.label.contains("COOL", true) ||
                it.label.contains("HEAT", true) || it.label.contains("DRY", true) ||
                it.label.contains("AUTO", true)
    }

    val fanSpeedButtons = buttons.filter {
        it.label.contains("FAN", true) || it.label.contains("SPEED", true)
    }

    val swingButtons = buttons.filter {
        it.label.contains("SWING", true) || it.label.contains("AIRFLOW", true) || it.label.contains("VANE", true)
    }

    val comfortButtons = buttons.filter {
        it.label.contains("TURBO", true) || it.label.contains("ECO", true) ||
                it.label.contains("SLEEP", true) || it.label.contains("TIMER", true) ||
                it.label.contains("CLEAN", true)
    }

    val consumedIds = setOfNotNull(powerBtn?.id, tempUpBtn?.id, tempDownBtn?.id) +
            modeButtons.map { it.id } + fanSpeedButtons.map { it.id } +
            swingButtons.map { it.id } + comfortButtons.map { it.id }

    val remainingButtons = buttons.filter { it.id !in consumedIds }

    var localTempDisplay by remember { mutableIntStateOf(24) }

    // Power & Top Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (powerBtn != null) {
            IrButton(
                label = powerBtn.label,
                icon = "power",
                variant = ButtonStyleVariant.POWER,
                hapticsEnabled = hapticsEnabled,
                isEditMode = isEditMode,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                onLongClick = { onEdit(powerBtn) },
                onClick = { onBlast(powerBtn) }
            )
        }

        // Climate indicator chip
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Text(
                text = "❄️ CLIMATE CONTROL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Thermostat Display & Temp Control
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SET TEMPERATURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temp Down Button
                if (tempDownBtn != null) {
                    IrButton(
                        label = "TEMP -",
                        icon = "down",
                        variant = ButtonStyleVariant.CIRCLE,
                        hapticsEnabled = hapticsEnabled,
                        isEditMode = isEditMode,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        onLongClick = { onEdit(tempDownBtn) },
                        onClick = {
                            if (localTempDisplay > 16) localTempDisplay--
                            onBlast(tempDownBtn)
                        }
                    )
                }

                // Big Temperature Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$localTempDisplay°C",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Target Ambient",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Temp Up Button
                if (tempUpBtn != null) {
                    IrButton(
                        label = "TEMP +",
                        icon = "up",
                        variant = ButtonStyleVariant.CIRCLE,
                        hapticsEnabled = hapticsEnabled,
                        isEditMode = isEditMode,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        onLongClick = { onEdit(tempUpBtn) },
                        onClick = {
                            if (localTempDisplay < 32) localTempDisplay++
                            onBlast(tempUpBtn)
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Mode Selection Cluster
    if (modeButtons.isNotEmpty()) {
        Text(
            text = "Operation Mode",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modeButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = "menu",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 74.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Fan Speed & Airflow Cluster
    if (fanSpeedButtons.isNotEmpty() || swingButtons.isNotEmpty()) {
        Text(
            text = "Fan Speed & Swing Airflow",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fanSpeedButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = "input",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 80.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
            swingButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = "menu",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 80.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Comfort & Timers
    if (comfortButtons.isNotEmpty()) {
        Text(
            text = "Comfort & Features",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            comfortButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(42.dp)
                        .defaultMinSize(minWidth = 74.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Additional Custom Buttons
    RenderAdditionalButtons(
        buttons = remainingButtons,
        hapticsEnabled = hapticsEnabled,
        isEditMode = isEditMode,
        onBlast = onBlast,
        onEdit = onEdit
    )
}

// ==========================================
// 3. PROJECTOR LAYOUT
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectorRemoteLayout(
    buttons: List<SavedButton>,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", true) || it.label.contains("STANDBY", true) }
    val sourceButtons = buttons.filter {
        it.label.contains("HDMI", true) || it.label.contains("SOURCE", true) ||
                it.label.contains("INPUT", true) || it.label.contains("VGA", true) ||
                it.label.contains("VIDEO", true)
    }

    val upBtn = buttons.firstOrNull { it.label.equals("UP", true) }
    val downBtn = buttons.firstOrNull { it.label.equals("DOWN", true) }
    val leftBtn = buttons.firstOrNull { it.label.equals("LEFT", true) }
    val rightBtn = buttons.firstOrNull { it.label.equals("RIGHT", true) }
    val okBtn = buttons.firstOrNull { it.label.equals("OK", true) || it.label.equals("ENTER", true) }

    val menuBtn = buttons.firstOrNull { it.label.contains("MENU", true) }
    val backBtn = buttons.firstOrNull { it.label.contains("BACK", true) || it.label.contains("ESC", true) || it.label.contains("EXIT", true) }

    val keystoneButtons = buttons.filter { it.label.contains("KEYSTONE", true) }
    val zoomButtons = buttons.filter { it.label.contains("ZOOM", true) || it.label.contains("FOCUS", true) }
    val displayButtons = buttons.filter {
        it.label.contains("FREEZE", true) || it.label.contains("BLANK", true) ||
                it.label.contains("MUTE", true) || it.label.contains("ASPECT", true) ||
                it.label.contains("AUTO", true)
    }

    val consumedIds = setOfNotNull(powerBtn?.id, upBtn?.id, downBtn?.id, leftBtn?.id, rightBtn?.id, okBtn?.id, menuBtn?.id, backBtn?.id) +
            sourceButtons.map { it.id } + keystoneButtons.map { it.id } + zoomButtons.map { it.id } + displayButtons.map { it.id }

    val remainingButtons = buttons.filter { it.id !in consumedIds }

    // Power & Top Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (powerBtn != null) {
            IrButton(
                label = powerBtn.label,
                icon = "power",
                variant = ButtonStyleVariant.POWER,
                hapticsEnabled = hapticsEnabled,
                isEditMode = isEditMode,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                onLongClick = { onEdit(powerBtn) },
                onClick = { onBlast(powerBtn) }
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Text(
                text = "📽️ PROJECTOR REMOTE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Video Input Selection Row
    if (sourceButtons.isNotEmpty()) {
        Text(
            text = "Video Input Source",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sourceButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = "input",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 76.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // D-Pad Section
    if (upBtn != null || downBtn != null || leftBtn != null || rightBtn != null || okBtn != null) {
        DPad(
            hapticsEnabled = hapticsEnabled,
            onUp = { upBtn?.let { onBlast(it) } },
            onDown = { downBtn?.let { onBlast(it) } },
            onLeft = { leftBtn?.let { onBlast(it) } },
            onRight = { rightBtn?.let { onBlast(it) } },
            onOk = { okBtn?.let { onBlast(it) } }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Menu & Back Navigation
    if (menuBtn != null || backBtn != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            menuBtn?.let {
                IrButton(
                    label = "MENU",
                    icon = "menu",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(width = 110.dp, height = 44.dp),
                    onLongClick = { onEdit(it) },
                    onClick = { onBlast(it) }
                )
            }
            backBtn?.let {
                IrButton(
                    label = "BACK",
                    icon = "back",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(width = 110.dp, height = 44.dp),
                    onLongClick = { onEdit(it) },
                    onClick = { onBlast(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Keystone & Optics
    if (keystoneButtons.isNotEmpty() || zoomButtons.isNotEmpty()) {
        Text(
            text = "Keystone & Optics Geometry",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (keystoneButtons + zoomButtons).forEach { btn ->
                IrButton(
                    label = btn.label,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 84.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Presentation Controls (Blank, Freeze)
    if (displayButtons.isNotEmpty()) {
        Text(
            text = "Presentation Controls",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(42.dp)
                        .defaultMinSize(minWidth = 74.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Additional Custom Buttons
    RenderAdditionalButtons(
        buttons = remainingButtons,
        hapticsEnabled = hapticsEnabled,
        isEditMode = isEditMode,
        onBlast = onBlast,
        onEdit = onEdit
    )
}

// ==========================================
// 4. AUDIO / SOUNDBAR / AMPLIFIER LAYOUT
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioRemoteLayout(
    buttons: List<SavedButton>,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", true) || it.label.contains("STANDBY", true) }
    val muteBtn = buttons.firstOrNull { it.label.contains("MUTE", true) }

    val volUpBtn = buttons.firstOrNull { it.label.contains("VOL", true) && ("+" in it.label || "UP" in it.label) }
    val volDownBtn = buttons.firstOrNull { it.label.contains("VOL", true) && ("-" in it.label || "DOWN" in it.label) }

    val sourceButtons = buttons.filter {
        it.label.contains("INPUT", true) || it.label.contains("SOURCE", true) ||
                it.label.contains("BLUETOOTH", true) || it.label.contains("BT", true) ||
                it.label.contains("OPTICAL", true) || it.label.contains("AUX", true) ||
                it.label.contains("ARC", true) || it.label.contains("USB", true)
    }

    val toneButtons = buttons.filter {
        it.label.contains("BASS", true) || it.label.contains("TREBLE", true) ||
                it.label.contains("EQ", true) || it.label.contains("SURROUND", true) ||
                it.label.contains("SOUND", true)
    }

    val mediaButtons = buttons.filter {
        it.label.contains("PLAY", true) || it.label.contains("PAUSE", true) ||
                it.label.contains("STOP", true) || it.label.contains("PREV", true) ||
                it.label.contains("NEXT", true)
    }

    val consumedIds = setOfNotNull(powerBtn?.id, muteBtn?.id, volUpBtn?.id, volDownBtn?.id) +
            sourceButtons.map { it.id } + toneButtons.map { it.id } + mediaButtons.map { it.id }

    val remainingButtons = buttons.filter { it.id !in consumedIds }

    // Power & Mute Top Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (powerBtn != null) {
            IrButton(
                label = powerBtn.label,
                icon = "power",
                variant = ButtonStyleVariant.POWER,
                hapticsEnabled = hapticsEnabled,
                isEditMode = isEditMode,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                onLongClick = { onEdit(powerBtn) },
                onClick = { onBlast(powerBtn) }
            )
        }

        if (muteBtn != null) {
            IrButton(
                label = muteBtn.label,
                icon = "mute",
                variant = ButtonStyleVariant.CIRCLE,
                hapticsEnabled = hapticsEnabled,
                isEditMode = isEditMode,
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                onLongClick = { onEdit(muteBtn) },
                onClick = { onBlast(muteBtn) }
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Big Volume Rocker Section
    if (volUpBtn != null || volDownBtn != null) {
        Column(
            modifier = Modifier
                .width(96.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(48.dp))
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            volUpBtn?.let {
                IrButton(
                    label = "VOL +",
                    icon = "vol_up",
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(it) },
                    onClick = { onBlast(it) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("VOLUME", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            volDownBtn?.let {
                IrButton(
                    label = "VOL -",
                    icon = "vol_down",
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(it) },
                    onClick = { onBlast(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Audio Input Sources
    if (sourceButtons.isNotEmpty()) {
        Text(
            text = "Audio Input Channels",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sourceButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = "input",
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 78.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Tone & Equalizer
    if (toneButtons.isNotEmpty()) {
        Text(
            text = "Equalizer & Sound Effects",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            toneButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(42.dp)
                        .defaultMinSize(minWidth = 80.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Media Playback Controls
    if (mediaButtons.isNotEmpty()) {
        Text(
            text = "Media Transport",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            mediaButtons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = btn.icon,
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Additional Custom Buttons
    RenderAdditionalButtons(
        buttons = remainingButtons,
        hapticsEnabled = hapticsEnabled,
        isEditMode = isEditMode,
        onBlast = onBlast,
        onEdit = onEdit
    )
}

// ==========================================
// 5. FAN / AIR CIRCULATOR LAYOUT
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FanRemoteLayout(
    buttons: List<SavedButton>,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", true) || it.label.contains("ON", true) || it.label.contains("OFF", true) }
    val speedUpBtn = buttons.firstOrNull { it.label.contains("SPEED", true) && ("+" in it.label || "UP" in it.label) }
        ?: buttons.firstOrNull { it.label.equals("UP", true) }
    val speedDownBtn = buttons.firstOrNull { it.label.contains("SPEED", true) && ("-" in it.label || "DOWN" in it.label) }
        ?: buttons.firstOrNull { it.label.equals("DOWN", true) }

    val speedPresets = buttons.filter {
        it.label in listOf("1", "2", "3", "4", "LOW", "MED", "HIGH", "SPEED 1", "SPEED 2", "SPEED 3")
    }

    val swingButtons = buttons.filter {
        it.label.contains("OSCILL", true) || it.label.contains("SWING", true)
    }

    val featureButtons = buttons.filter {
        it.label.contains("TIMER", true) || it.label.contains("MODE", true) ||
                it.label.contains("WIND", true) || it.label.contains("BREEZE", true) ||
                it.label.contains("LIGHT", true)
    }

    val consumedIds = setOfNotNull(powerBtn?.id, speedUpBtn?.id, speedDownBtn?.id) +
            speedPresets.map { it.id } + swingButtons.map { it.id } + featureButtons.map { it.id }

    val remainingButtons = buttons.filter { it.id !in consumedIds }

    // Power & Top Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (powerBtn != null) {
            IrButton(
                label = powerBtn.label,
                icon = "power",
                variant = ButtonStyleVariant.POWER,
                hapticsEnabled = hapticsEnabled,
                isEditMode = isEditMode,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                onLongClick = { onEdit(powerBtn) },
                onClick = { onBlast(powerBtn) }
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Text(
                text = "🌀 FAN CIRCULATOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Fan Speed Controls
    if (speedUpBtn != null || speedDownBtn != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FAN SPEED INTENSITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    speedDownBtn?.let {
                        IrButton(
                            label = "SPEED -",
                            icon = "down",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    speedUpBtn?.let {
                        IrButton(
                            label = "SPEED +",
                            icon = "up",
                            variant = ButtonStyleVariant.CIRCLE,
                            hapticsEnabled = hapticsEnabled,
                            isEditMode = isEditMode,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            onLongClick = { onEdit(it) },
                            onClick = { onBlast(it) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }

    // Direct Speed Presets (1, 2, 3)
    if (speedPresets.isNotEmpty()) {
        Text(
            text = "Direct Speed Levels",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            speedPresets.forEach { btn ->
                IrButton(
                    label = btn.label,
                    variant = ButtonStyleVariant.CIRCLE,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }

    // Oscillation, Timer, Light Features
    if (swingButtons.isNotEmpty() || featureButtons.isNotEmpty()) {
        Text(
            text = "Oscillation & Features",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (swingButtons + featureButtons).forEach { btn ->
                IrButton(
                    label = btn.label,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 84.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Additional Custom Buttons
    RenderAdditionalButtons(
        buttons = remainingButtons,
        hapticsEnabled = hapticsEnabled,
        isEditMode = isEditMode,
        onBlast = onBlast,
        onEdit = onEdit
    )
}

// ==========================================
// SHARED: ADDITIONAL FUNCTIONS FLOWROW
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderAdditionalButtons(
    buttons: List<SavedButton>,
    hapticsEnabled: Boolean,
    isEditMode: Boolean,
    onBlast: (SavedButton) -> Unit,
    onEdit: (SavedButton) -> Unit
) {
    if (buttons.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Additional Functions (${buttons.size})",
            fontSize = 13.sp,
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
            buttons.forEach { btn ->
                IrButton(
                    label = btn.label,
                    icon = btn.icon,
                    hapticsEnabled = hapticsEnabled,
                    isEditMode = isEditMode,
                    modifier = Modifier
                        .height(46.dp)
                        .defaultMinSize(minWidth = 80.dp),
                    onLongClick = { onEdit(btn) },
                    onClick = { onBlast(btn) }
                )
            }
        }
    }
}

// ==========================================
// ADD CUSTOM BUTTON DIALOG
// ==========================================
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
        title = { Text("Add Custom Button") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Button Label") },
                    placeholder = { Text("e.g. HDMI 2, MUTE, SLEEP") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Carrier Frequency (Hz)") },
                    placeholder = { Text("38000") },
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
                    val freq = frequency.toIntOrNull()?.takeIf { it in 10000..100000 } ?: 38000
                    val signalResult = if (input.startsWith("0000")) {
                        ProntoHexParser.parse(input)
                    } else {
                        RawPatternParser.parse(input, freq)
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

// ==========================================
// EDIT / DELETE BUTTON DIALOG
// ==========================================
@Composable
private fun EditOrDeleteButtonDialog(
    button: SavedButton,
    onDismiss: () -> Unit,
    onTest: () -> Unit,
    onSave: (SavedButton) -> Unit,
    onDelete: () -> Unit
) {
    var editLabel by remember { mutableStateOf(button.label) }
    var editFrequency by remember { mutableStateOf(button.frequencyHz.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Button") },
            text = { Text("Are you sure you want to remove '${button.label}' from this remote?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = IrError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Button Details")
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = IrError)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Button Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editFrequency,
                        onValueChange = { editFrequency = it },
                        label = { Text("Frequency (Hz)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Protocol: ${button.protocol}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            val pulses = button.toPatternIntArray()
                            Text(text = "Pulses Count: ${pulses.size}", fontSize = 12.sp)
                            if (button.device >= 0) {
                                Text(text = "Device: ${button.device}, Subdevice: ${button.subdevice}, Function: ${button.functionCode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transmit Test Signal")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = editLabel.isNotBlank(),
                    onClick = {
                        val parsedFreq = editFrequency.toIntOrNull()?.takeIf { it in 10000..100000 } ?: button.frequencyHz
                        val updated = button.copy(
                            label = editLabel.trim(),
                            frequencyHz = parsedFreq
                        )
                        onSave(updated)
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

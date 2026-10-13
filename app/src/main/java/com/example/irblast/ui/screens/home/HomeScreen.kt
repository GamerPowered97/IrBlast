package com.example.irblast.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import com.example.irblast.data.ir.encoders.NecEncoder
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.data.repository.mapFunctionToIcon
import com.example.irblast.domain.model.IrdbRemote
import com.example.irblast.domain.model.SavedButton
import com.example.irblast.Browse
import com.example.irblast.CodeFinder
import com.example.irblast.CustomSignal
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.RemoteControl
import com.example.irblast.Settings
import com.example.irblast.domain.model.SavedRemote
import com.example.irblast.theme.IrError
import com.example.irblast.ui.components.HardwareStatusBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: IrBlastAppContainer,
    onNavigate: (NavKey) -> Unit
) {
    val context = LocalContext.current
    val remotes by container.remoteRepository.remotesFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var isBlasting by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var remoteToDelete by remember { mutableStateOf<SavedRemote?>(null) }

    LaunchedEffect(Unit) {
        container.remoteRepository.refreshRemotes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IrBlast",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Universal IR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Settings) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
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
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Remote")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Hardware Status Banner
            HardwareStatusBanner(
                isHardwareAvailable = container.irBlasterManager.isHardwareAvailable,
                isBlasting = isBlasting
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Hub Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.Explore,
                    label = "Browse DB",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(Browse())
                }

                QuickActionChip(
                    icon = Icons.Default.Search,
                    label = "Code Finder",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(CodeFinder())
                }

                QuickActionChip(
                    icon = Icons.Default.Tune,
                    label = "Custom IR",
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(CustomSignal)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "My Remotes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${remotes.size} saved",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (remotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📱", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved remotes yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Browse brands or use Code Finder to add your first remote",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(remotes, key = { it.id }) { remote ->
                        RemoteCard(
                            remote = remote,
                            onClick = { onNavigate(RemoteControl(remote.id)) },
                            onDelete = { remoteToDelete = remote },
                            onQuickPower = {
                                coroutineScope.launch {
                                    val buttons = container.remoteRepository.getButtonsForRemote(remote.id)
                                    val powerBtn = buttons.firstOrNull { it.label.contains("POWER", ignoreCase = true) }
                                        ?: buttons.firstOrNull()
                                    if (powerBtn != null) {
                                        isBlasting = true
                                        val result = container.sendIrSignalUseCase.execute(
                                            powerBtn.frequencyHz,
                                            powerBtn.toPatternIntArray()
                                        )
                                        if (!result.isSuccess) {
                                            Toast.makeText(context, result.errorMessage ?: "Transmission failed", Toast.LENGTH_SHORT).show()
                                        }
                                        delay(300)
                                        isBlasting = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Create New Remote Dialog
    if (showCreateDialog) {
        CreateRemoteDialog(
            container = container,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, brand, devType, cat, icon, buttons ->
                coroutineScope.launch {
                    val newId = container.remoteRepository.saveRemote(
                        SavedRemote(
                            name = name,
                            brand = brand,
                            deviceType = devType,
                            category = cat,
                            icon = icon
                        ),
                        buttons = buttons
                    )
                    showCreateDialog = false
                    onNavigate(RemoteControl(newId))
                }
            }
        )
    }

    // Delete confirmation dialog
    remoteToDelete?.let { remote ->
        AlertDialog(
            onDismissRequest = { remoteToDelete = null },
            title = { Text("Delete Remote") },
            text = { Text("Are you sure you want to delete '${remote.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            container.remoteRepository.deleteRemote(remote.id)
                            remoteToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = IrError)
                }
            },
            dismissButton = {
                TextButton(onClick = { remoteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RemoteCard(
    remote: SavedRemote,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onQuickPower: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = remote.icon, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = remote.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${remote.brand} • ${remote.deviceType}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Power Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IrError.copy(alpha = 0.15f))
                        .border(1.dp, IrError.copy(alpha = 0.4f), CircleShape)
                        .clickable { onQuickPower() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = IrError,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private data class ApplianceOption(val label: String, val category: String, val emoji: String)

private fun generateTemplateButtons(category: String, brand: String): List<SavedButton> {
    val templateSpecs = when (category) {
        "AC / Climate" -> listOf(
            Triple("POWER", "power", 2),
            Triple("TEMP +", "up", 1),
            Triple("TEMP -", "down", 1),
            Triple("MODE", "menu", 1),
            Triple("FAN SPEED", "input", 1)
        )
        "Projector" -> listOf(
            Triple("POWER", "power", 2),
            Triple("HDMI", "input", 1),
            Triple("MENU", "menu", 1),
            Triple("KEYSTONE", "input", 1),
            Triple("UP", "up", 1),
            Triple("DOWN", "down", 1),
            Triple("LEFT", "left", 1),
            Triple("RIGHT", "right", 1),
            Triple("OK", "ok", 2)
        )
        "Audio" -> listOf(
            Triple("POWER", "power", 2),
            Triple("VOL +", "vol_up", 1),
            Triple("VOL -", "vol_down", 1),
            Triple("MUTE", "mute", 1),
            Triple("AUX", "input", 1),
            Triple("OPTICAL", "input", 1),
            Triple("BLUETOOTH", "input", 1)
        )
        "Fan" -> listOf(
            Triple("POWER", "power", 2),
            Triple("SPEED +", "up", 1),
            Triple("SPEED -", "down", 1),
            Triple("OSCILLATION", "menu", 1),
            Triple("TIMER", "input", 1)
        )
        "Set-Top Box" -> listOf(
            Triple("POWER", "power", 2),
            Triple("VOL +", "vol_up", 1),
            Triple("VOL -", "vol_down", 1),
            Triple("CH +", "ch_up", 1),
            Triple("CH -", "ch_down", 1),
            Triple("GUIDE", "menu", 1),
            Triple("MENU", "menu", 1),
            Triple("BACK", "back", 1),
            Triple("UP", "up", 1),
            Triple("DOWN", "down", 1),
            Triple("LEFT", "left", 1),
            Triple("RIGHT", "right", 1),
            Triple("OK", "ok", 2),
            Triple("1", "", 1), Triple("2", "", 1), Triple("3", "", 1),
            Triple("4", "", 1), Triple("5", "", 1), Triple("6", "", 1),
            Triple("7", "", 1), Triple("8", "", 1), Triple("9", "", 1),
            Triple("0", "", 1)
        )
        "Media Player" -> listOf(
            Triple("POWER", "power", 2),
            Triple("PLAY", "play", 1),
            Triple("PAUSE", "pause", 1),
            Triple("STOP", "stop", 1),
            Triple("PREV", "left", 1),
            Triple("NEXT", "right", 1),
            Triple("MENU", "menu", 1),
            Triple("UP", "up", 1),
            Triple("DOWN", "down", 1),
            Triple("LEFT", "left", 1),
            Triple("RIGHT", "right", 1),
            Triple("OK", "ok", 2)
        )
        else -> listOf(
            Triple("POWER", "power", 2),
            Triple("MUTE", "mute", 1),
            Triple("INPUT", "input", 1),
            Triple("VOL +", "vol_up", 1),
            Triple("VOL -", "vol_down", 1),
            Triple("CH +", "ch_up", 1),
            Triple("CH -", "ch_down", 1),
            Triple("MENU", "menu", 1),
            Triple("HOME", "home", 1),
            Triple("UP", "up", 1),
            Triple("DOWN", "down", 1),
            Triple("LEFT", "left", 1),
            Triple("RIGHT", "right", 1),
            Triple("OK", "ok", 2),
            Triple("1", "", 1), Triple("2", "", 1), Triple("3", "", 1),
            Triple("4", "", 1), Triple("5", "", 1), Triple("6", "", 1),
            Triple("7", "", 1), Triple("8", "", 1), Triple("9", "", 1),
            Triple("0", "", 1)
        )
    }

    return templateSpecs.mapIndexed { idx, (label, icon, colSpan) ->
        val signal = NecEncoder.encode(device = 1, subDevice = -1, function = idx + 1)
        SavedButton(
            remoteId = 0L,
            label = label,
            icon = icon,
            frequencyHz = signal.frequencyHz,
            patternMicros = signal.patternMicros.joinToString(","),
            protocol = "NEC",
            sortOrder = idx,
            colSpan = colSpan
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRemoteDialog(
    container: IrBlastAppContainer,
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: String, devType: String, cat: String, icon: String, buttons: List<SavedButton>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TV") }
    var selectedEmoji by remember { mutableStateOf("📺") }

    val applianceOptions = listOf(
        ApplianceOption("TV", "TV", "📺"),
        ApplianceOption("AC / Climate", "AC / Climate", "❄️"),
        ApplianceOption("Projector", "Projector", "📽️"),
        ApplianceOption("Audio", "Audio", "🔊"),
        ApplianceOption("Fan", "Fan", "🌀"),
        ApplianceOption("Set-Top Box", "Set-Top Box", "📦"),
        ApplianceOption("Media Player", "Media Player", "💿")
    )

    var brandSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var matchingDbRemotes by remember { mutableStateOf<List<IrdbRemote>>(emptyList()) }
    var selectedModelIndex by remember { mutableIntStateOf(0) }
    var linkDbCodes by remember { mutableStateOf(true) }
    var brandDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(brand, selectedCategory) {
        val trimmed = brand.trim()
        if (trimmed.isNotEmpty()) {
            val catSuggestions = container.irdbRepository.getBrands(
                category = selectedCategory,
                query = trimmed
            )
            brandSuggestions = if (catSuggestions.isNotEmpty()) {
                catSuggestions.take(8)
            } else {
                container.irdbRepository.getBrands(
                    category = null,
                    query = trimmed
                ).take(8)
            }

            var remotes = container.irdbRepository.getRemotes(
                brand = trimmed,
                category = selectedCategory
            )
            if (remotes.isEmpty()) {
                remotes = container.irdbRepository.getRemotes(
                    brand = trimmed,
                    category = null
                )
            }
            matchingDbRemotes = remotes
            selectedModelIndex = 0
        } else {
            brandSuggestions = emptyList()
            matchingDbRemotes = emptyList()
            selectedModelIndex = 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Remote") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Remote Name") },
                    placeholder = { Text("e.g. Living Room TV") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(text = "Appliance Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    applianceOptions.forEach { opt ->
                        FilterChip(
                            selected = selectedCategory == opt.category,
                            onClick = {
                                selectedCategory = opt.category
                                selectedEmoji = opt.emoji
                            },
                            label = { Text("${opt.emoji} ${opt.label}", fontSize = 12.sp) }
                        )
                    }
                }

                // Brand with Auto-Suggest Dropdown
                Column {
                    ExposedDropdownMenuBox(
                        expanded = brandDropdownExpanded && brandSuggestions.isNotEmpty(),
                        onExpandedChange = { brandDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = {
                                brand = it
                                brandDropdownExpanded = true
                            },
                            label = { Text("Brand") },
                            placeholder = { Text("e.g. Sony, Epson, Daikin...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = brandDropdownExpanded && brandSuggestions.isNotEmpty()
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = brandDropdownExpanded && brandSuggestions.isNotEmpty(),
                            onDismissRequest = { brandDropdownExpanded = false }
                        ) {
                            brandSuggestions.forEach { suggestedBrand ->
                                DropdownMenuItem(
                                    text = { Text(suggestedBrand, fontSize = 13.sp) },
                                    onClick = {
                                        brand = suggestedBrand
                                        brandDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (brandSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            brandSuggestions.forEach { suggestedBrand ->
                                Card(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            brand = suggestedBrand
                                            brandDropdownExpanded = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = suggestedBrand,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (matchingDbRemotes.isNotEmpty()) {
                    val activeModel = matchingDbRemotes.getOrNull(selectedModelIndex) ?: matchingDbRemotes.first()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = linkDbCodes,
                                    onCheckedChange = { linkDbCodes = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Link Database Codes (${activeModel.model})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Prefill verified IR codes from irdb.db for $brand",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (matchingDbRemotes.size > 1 && linkDbCodes) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Available Models (${matchingDbRemotes.size}):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    matchingDbRemotes.forEachIndexed { idx, rem ->
                                        FilterChip(
                                            selected = selectedModelIndex == idx,
                                            onClick = { selectedModelIndex = idx },
                                            label = { Text(rem.model, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Template buttons will be generated automatically for $selectedCategory.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    coroutineScope.launch {
                        val buttons = if (matchingDbRemotes.isNotEmpty() && linkDbCodes) {
                            val targetRemote = matchingDbRemotes.getOrNull(selectedModelIndex) ?: matchingDbRemotes.first()
                            val codes = container.irdbRepository.getCodesForRemote(targetRemote.id)
                            val unsupportedProtocols = mutableSetOf<String>()
                            val realButtons = codes.mapNotNull { code ->
                                val signal = ProtocolEncoder.encode(code.protocol, code.device, code.subdevice, code.function)
                                if (signal != null) {
                                    SavedButton(
                                        remoteId = 0L,
                                        label = code.functionName,
                                        icon = mapFunctionToIcon(code.functionName),
                                        frequencyHz = signal.frequencyHz,
                                        patternMicros = signal.patternMicros.joinToString(","),
                                        protocol = code.protocol,
                                        device = code.device,
                                        subdevice = code.subdevice,
                                        functionCode = code.function,
                                        colSpan = if (code.functionName.contains("POWER", ignoreCase = true) ||
                                            code.functionName.contains("OK", ignoreCase = true) ||
                                            code.functionName.contains("ENTER", ignoreCase = true)) 2 else 1
                                    )
                                } else {
                                    unsupportedProtocols.add(code.protocol)
                                    null
                                }
                            }
                            if (unsupportedProtocols.isNotEmpty()) {
                                Toast.makeText(context, "Note: Protocols not yet supported: ${unsupportedProtocols.joinToString()}", Toast.LENGTH_SHORT).show()
                            }
                            if (realButtons.isNotEmpty()) realButtons else generateTemplateButtons(selectedCategory, brand)
                        } else {
                            generateTemplateButtons(selectedCategory, brand)
                        }

                        onConfirm(
                            name.trim(),
                            brand.ifBlank { "Custom" }.trim(),
                            selectedCategory,
                            selectedCategory,
                            selectedEmoji,
                            buttons
                        )
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

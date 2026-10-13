package com.example.irblast.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation3.runtime.NavKey
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        container.sendIrSignalUseCase.execute(
                                            powerBtn.frequencyHz,
                                            powerBtn.toPatternIntArray()
                                        )
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
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, brand, devType, cat, icon ->
                coroutineScope.launch {
                    val newId = container.remoteRepository.saveRemote(
                        SavedRemote(
                            name = name,
                            brand = brand,
                            deviceType = devType,
                            category = cat,
                            icon = icon
                        )
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
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
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
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(IrError.copy(alpha = 0.15f))
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateRemoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, brand: String, devType: String, cat: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var devType by remember { mutableStateOf("TV") }
    var selectedEmoji by remember { mutableStateOf("📺") }

    val emojis = listOf("📺", "📽️", "🔊", "❄️", "📦", "💿", "💡", "🎮")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Remote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Remote Name") },
                    placeholder = { Text("e.g. Bedroom Projector") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    placeholder = { Text("e.g. Epson") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Choose Icon", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    emojis.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedEmoji = emoji
                                    devType = when (emoji) {
                                        "📺" -> "TV"
                                        "📽️" -> "Projector"
                                        "🔊" -> "Audio"
                                        "❄️" -> "Air Conditioner"
                                        "📦" -> "Set-Top Box"
                                        "💿" -> "DVD / Blu-ray"
                                        else -> "Device"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        name.trim(),
                        brand.ifBlank { "Custom" }.trim(),
                        devType,
                        devType,
                        selectedEmoji
                    )
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

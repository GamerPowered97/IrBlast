package com.example.irblast.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import com.example.irblast.data.ir.FlipperZeroIrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.irblast.IrBlastAppContainer
import com.example.irblast.data.ir.encoders.ProtocolEncoder
import com.example.irblast.domain.model.IrdbCode
import com.example.irblast.domain.model.IrdbRemote
import com.example.irblast.ui.components.HardwareStatusBanner
import com.example.irblast.ui.components.IrButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowseScreen(
    container: IrBlastAppContainer,
    initialCategory: String = "",
    onBack: () -> Unit,
    onRemoteImported: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(initialCategory.ifBlank { "All" }) }
    var searchQuery by remember { mutableStateOf("") }
    var brands by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var brandRemotes by remember { mutableStateOf<List<IrdbRemote>>(emptyList()) }

    var inspectRemote by remember { mutableStateOf<IrdbRemote?>(null) }
    var inspectCodes by remember { mutableStateOf<List<IrdbCode>>(emptyList()) }

    var isBlasting by remember { mutableStateOf(false) }

    fun refreshBrands() {
        coroutineScope.launch {
            val cat = if (selectedCategory == "All") null else selectedCategory
            brands = container.irdbRepository.getBrands(
                category = cat,
                query = searchQuery.trim().ifBlank { null }
            )
        }
    }

    LaunchedEffect(Unit) {
        val cats = container.irdbRepository.getCategories()
        categories = listOf("All") + cats
        refreshBrands()
    }

    LaunchedEffect(selectedCategory, searchQuery) {
        refreshBrands()
    }

    LaunchedEffect(selectedBrand) {
        val brand = selectedBrand
        if (brand != null) {
            val cat = if (selectedCategory == "All") null else selectedCategory
            brandRemotes = container.irdbRepository.getRemotes(brand, cat)
        } else {
            brandRemotes = emptyList()
        }
    }

    val irPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (text.isNotBlank()) {
                        val flipperRemote = FlipperZeroIrParser.parse(text, "Imported Remote")
                        if (flipperRemote.signals.isNotEmpty()) {
                            val (newRemote, newButtons) = FlipperZeroIrParser.toSavedEntities(flipperRemote)
                            val id = container.remoteRepository.saveRemote(newRemote, newButtons)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Imported '${newRemote.name}' with ${newButtons.size} buttons!", Toast.LENGTH_SHORT).show()
                                onRemoteImported(id)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "No IR signals found in file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        text = if (selectedBrand != null) selectedBrand!! else "Browse Brands",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedBrand != null) {
                                selectedBrand = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { irPickerLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import .ir File",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                .imePadding()
        ) {
            HardwareStatusBanner(
                isHardwareAvailable = container.irBlasterManager.isHardwareAvailable,
                isBlasting = isBlasting
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedBrand == null) {
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search 645+ brands (e.g. Epson, Samsung, Sony)...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${brands.size} brands found",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (brands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔍", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No brands matched your search", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Try searching by brand name or selecting 'All' category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // Brands List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(brands, key = { it }) { brand ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBrand = brand },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = brand,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Models for Selected Brand
                Text(
                    text = "Select remote profile to test or import:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(brandRemotes, key = { it.id }) { rem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inspectRemote = rem
                                    coroutineScope.launch {
                                        inspectCodes = container.irdbRepository.getCodesForRemote(rem.id)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
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
                                Column {
                                    Text(
                                        text = rem.model,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${rem.deviceType} • ${rem.category}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Preview & Test",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Remote Inspection & Live Test Dialog
    inspectRemote?.let { rem ->
        AlertDialog(
            onDismissRequest = { inspectRemote = null },
            title = {
                Column {
                    Text("${rem.brand} - ${rem.model}")
                    Text(
                        "${rem.deviceType} (${inspectCodes.size} buttons)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tap buttons below to test on your device:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        inspectCodes.forEach { code ->
                            IrButton(
                                label = code.functionName,
                                modifier = Modifier.height(44.dp),
                                onClick = {
                                    coroutineScope.launch {
                                        val signal = ProtocolEncoder.encode(
                                            code.protocol,
                                            code.device,
                                            code.subdevice,
                                            code.function
                                        )
                                        if (signal != null) {
                                            isBlasting = true
                                            val result = container.sendIrSignalUseCase.execute(signal)
                                            if (!result.isSuccess && !result.isSimulated) {
                                                Toast.makeText(context, result.errorMessage ?: "Transmission failed", Toast.LENGTH_SHORT).show()
                                            }
                                            delay(250)
                                            isBlasting = false
                                        } else {
                                            Toast.makeText(context, "Protocol '${code.protocol}' is not yet supported", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newId = container.remoteRepository.importIrdbRemote(
                                irdbRemoteId = rem.id,
                                onUnsupportedProtocol = { unsupportedProtocol ->
                                    Toast.makeText(context, "Protocol '$unsupportedProtocol' is not yet supported", Toast.LENGTH_SHORT).show()
                                }
                            )
                            inspectRemote = null
                            onRemoteImported(newId)
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to My Remotes")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectRemote = null }) {
                    Text("Close")
                }
            }
        )
    }
}

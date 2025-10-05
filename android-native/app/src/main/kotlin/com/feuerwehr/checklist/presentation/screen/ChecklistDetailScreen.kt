package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistItem
import com.feuerwehr.checklist.domain.model.ChecklistItemType
import com.feuerwehr.checklist.presentation.viewmodel.ChecklistViewModel
import com.feuerwehr.checklist.presentation.component.ErrorMessage
import com.feuerwehr.checklist.presentation.component.EmptyState

/**
 * Checklist detail screen showing comprehensive checklist information
 * Displays checklist metadata, all items, and execution options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    checklistId: Int,
    onNavigateBack: () -> Unit,
    onStartExecution: (Int, Int?) -> Unit, // checklistId, vehicleId
    onEditChecklist: ((Int) -> Unit)? = null,
    onSelectVehicleForExecution: (Int) -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Load checklist details when screen opens
    LaunchedEffect(checklistId) {
        viewModel.loadChecklistById(checklistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.selectedChecklist?.name ?: "Checklist Details")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Edit button for admins
                    onEditChecklist?.let { editAction ->
                        IconButton(onClick = { editAction(checklistId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedChecklist != null && !uiState.selectedChecklist!!.template) {
                FloatingActionButton(
                    onClick = { onSelectVehicleForExecution(checklistId) }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Ausführen")
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorMessage(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadChecklistById(checklistId) },
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.selectedChecklist == null -> {
                EmptyState(
                    message = "Checkliste nicht gefunden",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                ChecklistDetailContent(
                    checklist = uiState.selectedChecklist!!,
                    onSelectVehicleForExecution = onSelectVehicleForExecution,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ChecklistDetailContent(
    checklist: Checklist,
    onSelectVehicleForExecution: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Checklist Header Card
        item {
            ChecklistHeaderCard(checklist = checklist)
        }

        // Action Card (if not template)
        if (!checklist.template) {
            item {
                ActionCard(
                    checklistId = checklist.id,
                    onSelectVehicleForExecution = onSelectVehicleForExecution
                )
            }
        }

        // Statistics Card
        item {
            ChecklistStatisticsCard(checklist = checklist)
        }

        // Items Section
        item {
            Text(
                text = "Checklist Items (${checklist.items.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Checklist Items
        itemsIndexed(checklist.items) { index, item ->
            ChecklistItemCard(
                item = item,
                itemNumber = index + 1
            )
        }
    }
}

@Composable
private fun ChecklistHeaderCard(checklist: Checklist) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checklist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (checklist.template) "Vorlage" else "Checkliste",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Status indicator
                if (checklist.template) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Vorlage") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FileCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metadata
            ChecklistInfoRow("Erstellt am", formatDate(checklist.createdAt))
            ChecklistInfoRow("Items", "${checklist.items.size} Prüfpunkte")
            ChecklistInfoRow("Version", checklist.version.toString())
        }
    }
}

@Composable
private fun ActionCard(
    checklistId: Int,
    onSelectVehicleForExecution: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Checkliste ausführen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Wählen Sie ein Fahrzeug aus, um diese Checkliste zu starten.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { onSelectVehicleForExecution(checklistId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fahrzeug auswählen")
            }
        }
    }
}

@Composable
private fun ChecklistStatisticsCard(checklist: Checklist) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistiken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item Type Statistics
                val standardItemsCount = checklist.items.count { it.itemType == ChecklistItemType.STANDARD }
                val quantityItemsCount = checklist.items.count { it.itemType == ChecklistItemType.QUANTITY }
                val dateItemsCount = checklist.items.count { it.itemType == ChecklistItemType.DATE_CHECK }
                
                StatisticItem(
                    label = "Standard",
                    value = standardItemsCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                
                StatisticItem(
                    label = "Mengen",
                    value = quantityItemsCount.toString(),
                    icon = Icons.Default.Edit,
                    modifier = Modifier.weight(1f)
                )
                
                StatisticItem(
                    label = "Termine",
                    value = dateItemsCount.toString(),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ChecklistItemCard(
    item: ChecklistItem,
    itemNumber: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Item number
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Item content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.beschreibung,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                // No additional description field in our model
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Item type and properties
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(getItemTypeText(item.itemType)) },
                        leadingIcon = {
                            Icon(
                                getItemTypeIcon(item.itemType),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    
                    if (item.pflicht) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Pflicht") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper functions
private fun getItemTypeText(type: ChecklistItemType): String = when (type) {
    ChecklistItemType.STANDARD -> "Standard"
    ChecklistItemType.QUANTITY -> "Menge"
    ChecklistItemType.DATE_CHECK -> "Termin"
    ChecklistItemType.STATUS_CHECK -> "Status"
    ChecklistItemType.RATING_1_6 -> "Bewertung"
    ChecklistItemType.PERCENTAGE -> "Prozent"
    ChecklistItemType.ATEMSCHUTZ -> "Atemschutz"
    ChecklistItemType.VEHICLE_INFO -> "Fahrzeuginfo"
}

@Composable
private fun getItemTypeIcon(type: ChecklistItemType) = when (type) {
    ChecklistItemType.STANDARD -> Icons.Default.CheckCircle
    ChecklistItemType.QUANTITY -> Icons.Default.Tag
    ChecklistItemType.DATE_CHECK -> Icons.Default.CalendarToday
    ChecklistItemType.STATUS_CHECK -> Icons.Default.VerifiedUser
    ChecklistItemType.RATING_1_6 -> Icons.Default.Star
    ChecklistItemType.PERCENTAGE -> Icons.Default.ShowChart
    ChecklistItemType.ATEMSCHUTZ -> Icons.Default.Security
    ChecklistItemType.VEHICLE_INFO -> Icons.Default.DirectionsCar
}

private fun formatDate(dateTime: kotlinx.datetime.Instant): String {
    // Simple date formatting - in production app would use proper localization
    val localDateTime = dateTime.toString().take(10) // Gets YYYY-MM-DD part
    return localDateTime
}
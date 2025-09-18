package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.presentation.viewmodel.VehicleViewModel

/**
 * Screen for managing checklists for a specific vehicle
 * Shows available checklists and their execution status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleChecklistScreen(
    vehicleId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToExecution: (Int) -> Unit, // checklistExecutionId
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load vehicle details and checklists when screen opens
    LaunchedEffect(vehicleId) {
        viewModel.selectVehicle(vehicleId)
        viewModel.loadVehicleChecklists(vehicleId)
        viewModel.loadAvailableChecklistsForVehicle(vehicleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.selectedVehicle?.kennzeichen ?: "Fahrzeug Checklisten"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.loadVehicleChecklists(vehicleId)
                            viewModel.loadAvailableChecklistsForVehicle(vehicleId)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Vehicle info card
            uiState.selectedVehicle?.let { vehicle ->
                VehicleInfoCard(
                    vehicle = vehicle,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Error handling
            uiState.checklistError?.let { error ->
                ErrorMessage(
                    error = error,
                    onRetry = { 
                        viewModel.loadVehicleChecklists(vehicleId)
                        viewModel.loadAvailableChecklistsForVehicle(vehicleId)
                    },
                    onDismiss = viewModel::clearChecklistError,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Content sections
            when {
                uiState.isLoadingChecklists || uiState.isLoadingAvailableChecklists -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Active/Recent executions
                        if (uiState.vehicleChecklists.isNotEmpty()) {
                            item {
                                ChecklistStatusSection(
                                    title = "Aktuelle Prüfungen",
                                    checklistStatuses = uiState.vehicleChecklists,
                                    onExecutionClick = onNavigateToExecution,
                                    isLoading = uiState.isStartingChecklist
                                )
                            }
                        }

                        // Available checklists to start
                        if (uiState.availableChecklists.isNotEmpty()) {
                            item {
                                AvailableChecklistsSection(
                                    title = "Verfügbare Checklisten",
                                    checklists = uiState.availableChecklists,
                                    onStartChecklist = { checklistId ->
                                        viewModel.startChecklistForVehicle(vehicleId, checklistId)
                                    },
                                    isLoading = uiState.isStartingChecklist
                                )
                            }
                        }

                        // Empty state if no checklists
                        if (uiState.vehicleChecklists.isEmpty() && uiState.availableChecklists.isEmpty()) {
                            item {
                                EmptyChecklistState()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleInfoCard(
    vehicle: Vehicle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = vehicle.kennzeichen,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = vehicle.fahrzeugtyp.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            vehicle.fahrzeuggruppe?.let { group ->
                Text(
                    text = "Gruppe: ${group.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChecklistStatusSection(
    title: String,
    checklistStatuses: List<VehicleChecklistStatus>,
    onExecutionClick: (Int) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        checklistStatuses.forEach { status ->
            ChecklistStatusCard(
                status = status,
                onExecutionClick = onExecutionClick,
                isLoading = isLoading,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun AvailableChecklistsSection(
    title: String,
    checklists: List<Checklist>,
    onStartChecklist: (Int) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        checklists.forEach { checklist ->
            AvailableChecklistCard(
                checklist = checklist,
                onStartClick = { onStartChecklist(checklist.id) },
                isLoading = isLoading,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistStatusCard(
    status: VehicleChecklistStatus,
    onExecutionClick: (Int) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { 
            status.latestExecution?.let { execution ->
                onExecutionClick(execution.id)
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.checklist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Note: Checklist model doesn't have description field
                    Text(
                        text = "Checkliste ID: ${status.checklist.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Status info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        StatusChip(
                            text = when {
                                status.hasActiveExecution -> "Aktiv"
                                status.latestExecution != null -> "Abgeschlossen"
                                else -> "Verfügbar"
                            },
                            isActive = status.hasActiveExecution
                        )
                        
                        status.latestExecution?.let { execution ->
                            Text(
                                text = "Letzte Prüfung: ${execution.startedAt}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ausführung anzeigen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AvailableChecklistCard(
    checklist: Checklist,
    onStartClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = checklist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Note: Checklist model doesn't have description, using name for now
                    Text(
                        text = "Erstellt: ${checklist.createdAt}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onStartClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("Starten")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = when {
                    isActive -> Icons.Default.PlayArrow
                    text == "Abgeschlossen" -> Icons.Default.CheckCircle
                    else -> Icons.Default.RadioButtonUnchecked
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    )
}

@Composable
private fun EmptyChecklistState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Keine Checklisten verfügbar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Für dieses Fahrzeug sind noch keine Checklisten konfiguriert.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Fehler",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onRetry) {
                    Text("Wiederholen")
                }
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        }
    }
}
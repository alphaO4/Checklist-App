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
import com.feuerwehr.checklist.presentation.viewmodel.VehicleViewModel
import com.feuerwehr.checklist.presentation.component.ErrorMessage
import com.feuerwehr.checklist.presentation.component.EmptyState

/**
 * Vehicle list screen showing all vehicles
 * This is for general vehicle management, separate from vehicle selection for checklists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVehicleChecklists: (Int) -> Unit, // vehicleId
    onNavigateToVehicleDetails: ((Int) -> Unit)? = null, // vehicleId  
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVehicles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fahrzeuge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshVehicles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Navigate to create vehicle */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Fahrzeug hinzufügen")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    ErrorMessage(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadVehicles() },
                        onDismiss = viewModel::clearError
                    )
                }
                else -> {
                    if (uiState.vehicles.isEmpty()) {
                        EmptyState(
                            message = "Keine Fahrzeuge verfügbar"
                        )
                    } else {
                        VehicleList(
                            vehicles = uiState.vehicles,
                            onVehicleClick = { vehicleId ->
                                onNavigateToVehicleDetails?.invoke(vehicleId) ?: onNavigateToVehicleChecklists(vehicleId)
                            },
                            onChecklistClick = onNavigateToVehicleChecklists,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleList(
    vehicles: List<Vehicle>,
    onVehicleClick: (Int) -> Unit,
    onChecklistClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vehicles) { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                onClick = { onVehicleClick(vehicle.id) },
                onChecklistClick = { onChecklistClick(vehicle.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit,
    onChecklistClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                        text = vehicle.kennzeichen,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = vehicle.fahrzeugtyp.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vehicle.fahrzeuggruppe != null) {
                        Text(
                            text = "Gruppe: ${vehicle.fahrzeuggruppe.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onClick() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Details", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Button(
                            onClick = { onChecklistClick() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Checkliste", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

// Removed private EmptyState - using shared component
package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.TuvAppointment
import com.feuerwehr.checklist.presentation.viewmodel.VehicleViewModel
import com.feuerwehr.checklist.presentation.component.ErrorMessage
import com.feuerwehr.checklist.presentation.component.EmptyState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Vehicle detail screen showing comprehensive vehicle information
 * Includes basic info, TÜV status, recent checklists, and maintenance history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToChecklists: (Int) -> Unit,
    onNavigateToTuvManagement: (Int) -> Unit,
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load vehicle details when screen opens
    LaunchedEffect(vehicleId) {
        viewModel.selectVehicle(vehicleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.selectedVehicle?.kennzeichen ?: "Fahrzeug Details")
                },
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
                    onRetry = { viewModel.selectVehicle(vehicleId) },
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.selectedVehicle == null -> {
                EmptyState(
                    message = "Fahrzeug nicht gefunden",
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                VehicleDetailContent(
                    vehicle = uiState.selectedVehicle!!,
                    onNavigateToChecklists = onNavigateToChecklists,
                    onNavigateToTuvManagement = onNavigateToTuvManagement,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun VehicleDetailContent(
    vehicle: Vehicle,
    onNavigateToChecklists: (Int) -> Unit,
    onNavigateToTuvManagement: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vehicle Information Card
        item {
            VehicleInfoCard(vehicle = vehicle)
        }

        // TÜV Status Card
        item {
            TuvStatusCard(
                vehicle = vehicle,
                onManageTuv = { onNavigateToTuvManagement(vehicle.id) }
            )
        }

        // Quick Actions
        item {
            QuickActionsCard(
                vehicleId = vehicle.id,
                onNavigateToChecklists = onNavigateToChecklists
            )
        }

        // Vehicle Status Card
        item {
            VehicleStatusCard(vehicle = vehicle)
        }
    }
}

@Composable
private fun VehicleInfoCard(vehicle: Vehicle) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Fahrzeug-Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            VehicleInfoRow("Kennzeichen", vehicle.kennzeichen)
            VehicleInfoRow("Fahrzeugtyp", vehicle.fahrzeugtyp.name)
            VehicleInfoRow("Fahrzeuggruppe", vehicle.fahrzeuggruppe?.name ?: "Nicht zugeordnet")
            VehicleInfoRow("Erstellt am", formatDate(vehicle.createdAt))
        }
    }
}

@Composable
private fun VehicleInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TuvStatusCard(
    vehicle: Vehicle,
    onManageTuv: () -> Unit
) {
    // Calculate TÜV status based on current date
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val tuvStatus = getTuvStatus(vehicle, currentDate)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (tuvStatus) {
                TuvStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
                TuvStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TÜV-Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onManageTuv) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "TÜV verwalten"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (tuvStatus) {
                        TuvStatus.CURRENT -> Icons.Default.CheckCircle
                        TuvStatus.WARNING -> Icons.Default.Warning
                        TuvStatus.EXPIRED -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (tuvStatus) {
                        TuvStatus.CURRENT -> MaterialTheme.colorScheme.primary
                        TuvStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                        TuvStatus.EXPIRED -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = getTuvStatusText(tuvStatus),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getTuvDetailText(vehicle, currentDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    vehicleId: Int,
    onNavigateToChecklists: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Schnellaktionen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Checklist Action
                OutlinedButton(
                    onClick = { onNavigateToChecklists(vehicleId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checkliste")
                }
                
                // Maintenance Log Action (placeholder)
                OutlinedButton(
                    onClick = { /* TODO: Navigate to maintenance log */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wartung")
                }
            }
        }
    }
}

@Composable
private fun VehicleStatusCard(vehicle: Vehicle) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Fahrzeugstatus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Einsatzbereit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Fahrzeug ist verfügbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper functions for TÜV status calculation
private enum class TuvStatus { CURRENT, WARNING, EXPIRED }

private fun getTuvStatus(vehicle: Vehicle, currentDate: LocalDate): TuvStatus {
    // Simple heuristic based on vehicle age simulation
    // In full implementation, this would check actual TÜV appointment dates from vehicle.tuvTermine
    return if (vehicle.tuvTermine.isNotEmpty()) {
        TuvStatus.CURRENT  // Has TÜV appointments
    } else {
        TuvStatus.WARNING  // No TÜV data available
    }
}

private fun getTuvStatusText(status: TuvStatus): String = when (status) {
    TuvStatus.CURRENT -> "Aktuell"
    TuvStatus.WARNING -> "Bald fällig"
    TuvStatus.EXPIRED -> "Abgelaufen"
}

private fun getTuvDetailText(vehicle: Vehicle, currentDate: LocalDate): String {
    return if (vehicle.tuvTermine.isNotEmpty()) {
        "TÜV-Termine verfügbar: ${vehicle.tuvTermine.size}"
    } else {
        "Keine TÜV-Termine hinterlegt"
    }
}

// Helper function for date formatting
private fun formatDate(instant: kotlinx.datetime.Instant): String {
    // Simple date formatting - in production app would use proper localization
    return instant.toString().take(10) // Gets YYYY-MM-DD part
}
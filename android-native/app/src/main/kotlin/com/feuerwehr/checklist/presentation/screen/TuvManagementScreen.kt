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
import androidx.lifecycle.compose.collectAsState
import com.feuerwehr.checklist.presentation.viewmodel.VehicleViewModel
import com.feuerwehr.checklist.domain.model.TuvAppointment
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.TuvStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


/**
 * TÜV Management Screen for managing vehicle inspection appointments
 * Shows TÜV status, upcoming appointments, and allows scheduling new appointments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuvManagementScreen(
    vehicleId: Int,
    onNavigateBack: () -> Unit = {},
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(vehicleId) {
        viewModel.selectVehicle(vehicleId)
        viewModel.loadTuvAppointments(vehicleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TÜV-Verwaltung") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAppointmentDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "TÜV-Termin hinzufügen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAppointmentDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Neuer TÜV-Termin")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vehicle Info Header
            uiState.selectedVehicle?.let { vehicle ->
                item {
                    VehicleInfoCard(vehicle = vehicle)
                }
            }
            
            // Current TÜV Status
            item {
                CurrentTuvStatusCard(
                    tuvAppointments = uiState.tuvAppointments,
                    onScheduleAppointment = { showAddAppointmentDialog = true }
                )
            }
            
            // TÜV Appointments List
            item {
                Text(
                    text = "TÜV-Termine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.tuvAppointments.sortedByDescending { it.ablaufDatum }) { appointment ->
                TuvAppointmentCard(
                    appointment = appointment,
                    onEdit = { /* TODO: Edit appointment */ },
                    onDelete = { /* TODO: Delete appointment */ }
                )
            }
            
            if (uiState.tuvAppointments.isEmpty()) {
                item {
                    EmptyTuvAppointmentsCard(
                        onScheduleFirst = { showAddAppointmentDialog = true }
                    )
                }
            }
        }
    }
    
    if (showAddAppointmentDialog) {
        AddTuvAppointmentDialog(
            vehicleId = vehicleId,
            onDismiss = { showAddAppointmentDialog = false },
            onConfirm = { date, type ->
                viewModel.scheduleTuvAppointment(vehicleId, date, type)
                showAddAppointmentDialog = false
            }
        )
    }
    
    // Handle loading and error states
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun VehicleInfoCard(vehicle: Vehicle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = vehicle.kennzeichen,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                vehicle.type?.let { type ->
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                vehicle.group?.let { group ->
                    Text(
                        text = "Gruppe: ${group.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTuvStatusCard(
    tuvAppointments: List<TuvAppointment>,
    onScheduleAppointment: () -> Unit
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val currentAppointment = tuvAppointments
        .filter { it.ablaufDatum > today }
        .minByOrNull { it.ablaufDatum }
    
    val lastAppointment = tuvAppointments
        .filter { it.ablaufDatum < today }
        .maxByOrNull { it.ablaufDatum }
    
    val status = when {
        currentAppointment != null -> {
            // Simple status based on appointment existence - improve later
            TuvStatus.CURRENT
        }
        lastAppointment != null -> {
            TuvStatus.WARNING
        }
        else -> TuvStatus.EXPIRED
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TÜV-Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TuvStatusBadge(status = status)
            }
            
            when {
                currentAppointment != null -> {
                    Text(
                        text = "Nächster TÜV-Termin: ${currentAppointment.ablaufDatum}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Termin geplant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                lastAppointment != null -> {
                    Text(
                        text = "Letzter TÜV: ${lastAppointment.ablaufDatum}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Termin abgelaufen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Text(
                        text = "Kein TÜV-Termin erfasst",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (status != TuvStatus.CURRENT) {
                OutlinedButton(
                    onClick = onScheduleAppointment,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TÜV-Termin vereinbaren")
                }
            }
        }
    }
}

@Composable
private fun TuvStatusBadge(status: TuvStatus) {
    val (color, text, icon) = when (status) {
        TuvStatus.CURRENT -> Triple(
            MaterialTheme.colorScheme.primary,
            "Aktuell",
            Icons.Default.CheckCircle
        )
        TuvStatus.WARNING -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            "Warnung",
            Icons.Default.Warning
        )
        TuvStatus.EXPIRED -> Triple(
            MaterialTheme.colorScheme.error,
            "Abgelaufen",
            Icons.Default.Error
        )
        TuvStatus.REMINDER -> Triple(
            MaterialTheme.colorScheme.secondary,
            "Erinnerung",
            Icons.Default.Notifications
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TuvAppointmentCard(
    appointment: TuvAppointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val isPast = appointment.ablaufDatum < today
    val isUpcoming = appointment.ablaufDatum > today
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isUpcoming -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
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
                Column {
                    Text(
                        text = appointment.ablaufDatum.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TÜV-Termin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Show status
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = when (appointment.status) {
                    TuvStatus.CURRENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    TuvStatus.WARNING -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    TuvStatus.EXPIRED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    TuvStatus.REMINDER -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Status: ${appointment.status.name}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyTuvAppointmentsCard(
    onScheduleFirst: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = "Keine TÜV-Termine erfasst",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Erfassen Sie den ersten TÜV-Termin für dieses Fahrzeug",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Button(onClick = onScheduleFirst) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ersten Termin erfassen")
            }
        }
    }
}

@Composable
private fun AddTuvAppointmentDialog(
    vehicleId: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("TÜV-Termin hinzufügen")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date picker would go here - simplified for now
                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = { /* TODO: Implement date picker */ },
                    label = { Text("Ablaufdatum") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Datum auswählen")
                    }
                )
                
                Text(
                    text = "Der TÜV-Termin wird für das ausgewählte Datum erfasst.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDate, "TÜV")
                }
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
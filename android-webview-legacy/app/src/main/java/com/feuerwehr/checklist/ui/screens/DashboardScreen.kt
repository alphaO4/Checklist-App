package com.feuerwehr.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feuerwehr.checklist.data.models.UserRole
import com.feuerwehr.checklist.ui.viewmodel.AuthViewModel
import com.feuerwehr.checklist.ui.viewmodel.TuvViewModel
import com.feuerwehr.checklist.ui.viewmodel.VehicleViewModel

/**
 * Main dashboard screen with overview cards and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToVehicles: () -> Unit,
    onNavigateToChecklists: () -> Unit,
    onNavigateToTuv: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    vehicleViewModel: VehicleViewModel = hiltViewModel(),
    tuvViewModel: TuvViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val vehicles by vehicleViewModel.vehicles.collectAsStateWithLifecycle()
    val tuvStatusSummary by tuvViewModel.getStatusSummary().collectAsStateWithLifecycle()
    val upcomingDeadlines by tuvViewModel.upcomingDeadlines.collectAsStateWithLifecycle()
    
    val currentUser = authState.currentUser
    val isAdmin = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.ORGANISATOR
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Willkommen, ${currentUser?.username ?: "Benutzer"}!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Feuerwehr Fahrzeug-Verwaltung",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Rolle: ${currentUser?.role?.displayName ?: "Unbekannt"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Quick action buttons
        item {
            Text(
                text = "Schnellzugriff",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickActionCard(
                        title = "Fahrzeuge",
                        subtitle = "${vehicles.size} Fahrzeuge",
                        icon = Icons.Default.DirectionsCar,
                        onClick = onNavigateToVehicles
                    )
                }
                
                item {
                    QuickActionCard(
                        title = "Checklisten",
                        subtitle = "Prüfungen durchführen",
                        icon = Icons.Default.Checklist,
                        onClick = onNavigateToChecklists
                    )
                }
                
                item {
                    QuickActionCard(
                        title = "TÜV-Termine",
                        subtitle = "${tuvStatusSummary.total} Termine",
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToTuv
                    )
                }
                
                if (isAdmin) {
                    item {
                        QuickActionCard(
                            title = "Administration",
                            subtitle = "Verwaltung",
                            icon = Icons.Default.AdminPanelSettings,
                            onClick = onNavigateToAdmin
                        )
                    }
                }
            }
        }
        
        // TÜV Status Overview
        item {
            Text(
                text = "TÜV-Status Übersicht",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Aktuell",
                    count = tuvStatusSummary.current,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.CheckCircle
                )
                
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Warnung",
                    count = tuvStatusSummary.warning,
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.Warning
                )
                
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Abgelaufen",
                    count = tuvStatusSummary.expired,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Error
                )
            }
        }
        
        // Upcoming deadlines
        if (upcomingDeadlines.isNotEmpty()) {
            item {
                Text(
                    text = "Anstehende TÜV-Termine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(upcomingDeadlines.take(5)) { tuvTermin ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToTuv
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (tuvTermin.status) {
                                com.feuerwehr.checklist.data.models.TuvStatus.WARNING -> Icons.Default.Warning
                                com.feuerwehr.checklist.data.models.TuvStatus.EXPIRED -> Icons.Default.Error
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = when (tuvTermin.status) {
                                com.feuerwehr.checklist.data.models.TuvStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                                com.feuerwehr.checklist.data.models.TuvStatus.EXPIRED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fahrzeug #${tuvTermin.fahrzeugId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Ablauf: ${tuvTermin.ablaufDatum}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Details anzeigen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(160.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
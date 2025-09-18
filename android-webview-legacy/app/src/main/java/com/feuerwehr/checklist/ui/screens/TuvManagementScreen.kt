package com.feuerwehr.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feuerwehr.checklist.data.models.TuvStatus
import com.feuerwehr.checklist.data.models.TuvTermin
import com.feuerwehr.checklist.ui.viewmodel.TuvViewModel
import java.time.format.DateTimeFormatter

/**
 * TÜV management screen with deadline overview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuvManagementScreen(
    onTuvClick: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TuvViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTermine by viewModel.tuvTermine.collectAsStateWithLifecycle()
    val upcomingDeadlines by viewModel.upcomingDeadlines.collectAsStateWithLifecycle()
    val expiredInspections by viewModel.expiredInspections.collectAsStateWithLifecycle()
    val statusSummary by viewModel.getStatusSummary().collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Übersicht", "Anstehend", "Abgelaufen", "Alle")
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Status summary cards
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusSummaryCard(statusSummary = statusSummary)
            }
            
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
            
            when (selectedTab) {
                0 -> { // Overview
                    item {
                        Text(
                            text = "Kritische TÜV-Termine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val criticalTermine = (upcomingDeadlines + expiredInspections)
                        .sortedBy { it.ablaufDatum }
                        .take(10)
                    
                    items(criticalTermine) { tuvTermin ->
                        TuvTerminCard(
                            tuvTermin = tuvTermin,
                            onClick = { onTuvClick(tuvTermin.id) }
                        )
                    }
                }
                
                1 -> { // Upcoming
                    item {
                        Text(
                            text = "Anstehende Termine (${upcomingDeadlines.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(upcomingDeadlines) { tuvTermin ->
                        TuvTerminCard(
                            tuvTermin = tuvTermin,
                            onClick = { onTuvClick(tuvTermin.id) }
                        )
                    }
                }
                
                2 -> { // Expired
                    item {
                        Text(
                            text = "Abgelaufene Termine (${expiredInspections.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    items(expiredInspections) { tuvTermin ->
                        TuvTerminCard(
                            tuvTermin = tuvTermin,
                            onClick = { onTuvClick(tuvTermin.id) }
                        )
                    }
                }
                
                3 -> { // All
                    item {
                        Text(
                            text = "Alle TÜV-Termine (${allTermine.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(allTermine) { tuvTermin ->
                        TuvTerminCard(
                            tuvTermin = tuvTermin,
                            onClick = { onTuvClick(tuvTermin.id) }
                        )
                    }
                }
            }
            
            if (when (selectedTab) {
                0 -> (upcomingDeadlines + expiredInspections).isEmpty()
                1 -> upcomingDeadlines.isEmpty()
                2 -> expiredInspections.isEmpty()
                3 -> allTermine.isEmpty()
                else -> false
            }) {
                item {
                    EmptyStateCard(
                        title = when (selectedTab) {
                            0 -> "Keine kritischen Termine"
                            1 -> "Keine anstehenden Termine"
                            2 -> "Keine abgelaufenen Termine"
                            3 -> "Keine TÜV-Termine"
                            else -> "Keine Daten"
                        },
                        subtitle = when (selectedTab) {
                            0 -> "Alle TÜV-Termine sind aktuell"
                            1 -> "Keine TÜV-Termine in den nächsten 30 Tagen"
                            2 -> "Alle TÜV-Termine sind aktuell"
                            3 -> "Noch keine TÜV-Termine erfasst"
                            else -> ""
                        },
                        icon = Icons.Default.Schedule
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    statusSummary: com.feuerwehr.checklist.ui.viewmodel.TuvStatusSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "TÜV-Status Übersicht",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(
                    label = "Aktuell",
                    count = statusSummary.current,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatusChip(
                    label = "Warnung",
                    count = statusSummary.warning,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatusChip(
                    label = "Abgelaufen",
                    count = statusSummary.expired,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Gesamt: ${statusSummary.total} Fahrzeuge",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuvTerminCard(
    tuvTermin: TuvTermin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (tuvTermin.status) {
        TuvStatus.CURRENT -> MaterialTheme.colorScheme.primary
        TuvStatus.WARNING -> Color(0xFFFF9800)
        TuvStatus.EXPIRED -> MaterialTheme.colorScheme.error
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = MaterialTheme.shapes.small,
                color = statusColor
            ) {}
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fahrzeug #${tuvTermin.fahrzeugId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Ablauf: ${tuvTermin.ablaufDatum.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                tuvTermin.letztePruefung?.let { letztePruefung ->
                    Text(
                        text = "Letzte Prüfung: ${letztePruefung.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = when (tuvTermin.status) {
                            TuvStatus.CURRENT -> "Aktuell"
                            TuvStatus.WARNING -> "Warnung"
                            TuvStatus.EXPIRED -> "Abgelaufen"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Details anzeigen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
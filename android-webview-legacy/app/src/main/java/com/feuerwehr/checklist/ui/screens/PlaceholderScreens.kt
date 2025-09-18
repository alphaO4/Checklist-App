package com.feuerwehr.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Placeholder screen for functionality not yet implemented
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String = "Diese Funktion wird in einer zukünftigen Version implementiert",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                }
            }
        )
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "In Entwicklung",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Specific placeholder screens
@Composable
fun ChecklistDetailScreen(
    checklistId: Int,
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "Checklist Details",
        subtitle = "Detailansicht für Checklist #$checklistId",
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun ChecklistExecutionScreen(
    checklistId: Int,
    vehicleId: Int,
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "Checklist Ausführung",
        subtitle = "Checklist #$checklistId für Fahrzeug #$vehicleId ausführen",
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun VehicleDetailScreen(
    vehicleId: Int,
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "Fahrzeug Details",
        subtitle = "Detailansicht für Fahrzeug #$vehicleId",
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun TuvDetailScreen(
    tuvId: Int,
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "TÜV Details",
        subtitle = "Detailansicht für TÜV-Termin #$tuvId",
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun VehicleTypeManagementScreen(
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "Fahrzeugtypen",
        subtitle = "Verwaltung der Fahrzeugtypen (MTF, LF, TLF, RTB)",
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun GroupManagementScreen(
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "Gruppenverwaltung",
        subtitle = "Verwaltung von Fahrzeuggruppen und Benutzergruppen",
        onNavigateBack = onNavigateBack
    )
}
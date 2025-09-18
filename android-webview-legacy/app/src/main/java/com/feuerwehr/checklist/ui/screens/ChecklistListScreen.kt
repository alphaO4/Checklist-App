package com.feuerwehr.checklist.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.ui.viewmodel.ChecklistViewModel

/**
 * Checklist list screen with admin editing capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistListScreen(
    onChecklistClick: (Int) -> Unit,
    onExecuteChecklist: (Int, Int) -> Unit,
    onNavigateBack: () -> Unit,
    isAdmin: Boolean,
    onEditChecklist: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checklists by viewModel.checklists.collectAsStateWithLifecycle()
    
    var showVehicleSelectionDialog by remember { mutableStateOf<Checklist?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error and clear it
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Search and filter bar
        SearchFilterBar(
            onSearchQueryChange = viewModel::searchChecklists,
            onFilterChange = { /* TODO: Implement filtering */ },
            isAdmin = isAdmin,
            onImportClick = { showImportDialog = true },
            onCreateClick = { onEditChecklist(-1) } // -1 for new checklist
        )
        
        // Checklist list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(checklists) { checklist ->
                ChecklistCard(
                    checklist = checklist,
                    onChecklistClick = { onChecklistClick(checklist.id) },
                    onExecuteClick = { 
                        showVehicleSelectionDialog = checklist
                    },
                    onEditClick = if (isAdmin) { 
                        { onEditChecklist(checklist.id) }
                    } else null
                )
            }
            
            if (checklists.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateCard(
                        title = "Keine Checklisten gefunden",
                        subtitle = if (isAdmin) {
                            "Erstellen Sie neue Checklisten oder importieren Sie CSV-Dateien"
                        } else {
                            "Keine Checklisten verfügbar"
                        },
                        icon = Icons.Default.Checklist
                    )
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
    
    // Vehicle selection dialog
    showVehicleSelectionDialog?.let { checklist ->
        VehicleSelectionDialog(
            checklistName = checklist.name,
            onVehicleSelected = { vehicleId ->
                onExecuteChecklist(checklist.id, vehicleId)
                showVehicleSelectionDialog = null
            },
            onDismiss = { showVehicleSelectionDialog = null }
        )
    }
    
    // CSV Import dialog
    if (showImportDialog) {
        CsvImportDialog(
            onImportConfirm = { csvContent ->
                viewModel.importFromCsv(csvContent)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false },
            isLoading = uiState.isImporting
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterBar(
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String?) -> Unit,
    isAdmin: Boolean,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    onSearchQueryChange(it)
                },
                label = { Text("Checklisten suchen...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                onSearchQueryChange("")
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Löschen")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Admin actions
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV Import")
                    }
                    
                    Button(
                        onClick = onCreateClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Erstellen")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistCard(
    checklist: Checklist,
    onChecklistClick: () -> Unit,
    onExecuteClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onChecklistClick
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
                        text = checklist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    checklist.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoChip(
                            label = "${checklist.itemCount} Items",
                            icon = Icons.Default.List
                        )
                        
                        InfoChip(
                            label = checklist.fahrzeuggruppe,
                            icon = Icons.Default.Group
                        )
                        
                        InfoChip(
                            label = "v${checklist.version}",
                            icon = Icons.Default.Tag
                        )
                    }
                }
                
                // Action buttons
                Column {
                    onEditClick?.let { editClick ->
                        IconButton(onClick = editClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Bearbeiten",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChecklistClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                
                Button(
                    onClick = onExecuteClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prüfen")
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
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

// TODO: Implement these dialogs in separate files
@Composable
private fun VehicleSelectionDialog(
    checklistName: String,
    onVehicleSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Implement vehicle selection dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fahrzeug wählen") },
        text = { Text("Wählen Sie ein Fahrzeug für die Checkliste '$checklistName'") },
        confirmButton = {
            TextButton(onClick = { onVehicleSelected(1) }) {
                Text("Auswählen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun CsvImportDialog(
    onImportConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var csvContent by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CSV Import") },
        text = {
            Column {
                Text("Fügen Sie den CSV-Inhalt ein:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = csvContent,
                    onValueChange = { csvContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("CSV-Daten hier einfügen...") },
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (csvContent.isNotBlank()) {
                        onImportConfirm(csvContent)
                    }
                },
                enabled = !isLoading && csvContent.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Importieren")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Abbrechen")
            }
        }
    )
}
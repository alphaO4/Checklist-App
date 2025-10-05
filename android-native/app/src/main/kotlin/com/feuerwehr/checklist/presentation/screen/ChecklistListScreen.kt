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
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.presentation.viewmodel.ChecklistViewModel
import com.feuerwehr.checklist.presentation.component.SearchBar
import com.feuerwehr.checklist.presentation.component.ErrorMessage
import com.feuerwehr.checklist.presentation.component.EmptyState

/**
 * Main checklist list screen with search and template support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistListScreen(
    vehicleId: Int? = null, // If provided, show checklists for this vehicle only
    onNavigateToChecklistDetails: (Int) -> Unit,
    onNavigateToExecution: (Int, Int) -> Unit, // checklistId, vehicleId
    onNavigateBack: () -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTemplatesTab by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            viewModel.loadChecklistsForVehicle(vehicleId)
        } else {
            viewModel.loadChecklists()
        }
        viewModel.loadTemplates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (vehicleId != null) "Fahrzeug-Checklisten" else "Checklisten")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (vehicleId != null) {
                            viewModel.loadChecklistsForVehicle(vehicleId)
                        } else {
                            viewModel.refreshChecklists()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Navigate to create checklist */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Checkliste erstellen")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::searchChecklists,
                onClearQuery = viewModel::clearSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Tab row for Checklists vs Templates
            TabRow(
                selectedTabIndex = if (showTemplatesTab) 1 else 0
            ) {
                Tab(
                    selected = !showTemplatesTab,
                    onClick = { showTemplatesTab = false },
                    text = { Text("Checklisten") }
                )
                Tab(
                    selected = showTemplatesTab,
                    onClick = { showTemplatesTab = true },
                    text = { Text("Vorlagen") }
                )
            }

            // Content
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
                    val errorMessage = uiState.error!!
                    ErrorMessage(
                        error = errorMessage,
                        onRetry = { 
                            if (vehicleId != null) {
                                viewModel.loadChecklistsForVehicle(vehicleId)
                            } else {
                                viewModel.loadChecklists()
                            }
                        },
                        onDismiss = viewModel::clearError
                    )
                }
                else -> {
                    val displayChecklists = if (showTemplatesTab) {
                        uiState.templates
                    } else {
                        uiState.checklists
                    }

                    if (displayChecklists.isEmpty()) {
                        EmptyState(
                            message = if (showTemplatesTab) {
                                "Keine Vorlagen verfügbar"
                            } else {
                                "Keine Checklisten gefunden"
                            }
                        )
                    } else {
                        ChecklistList(
                            checklists = displayChecklists,
                            isTemplate = showTemplatesTab,
                            onChecklistClick = onNavigateToChecklistDetails,
                            onExecuteClick = { checklistId ->
                                val executionVehicleId = vehicleId ?: 1 // Use provided vehicleId or fallback
                                onNavigateToExecution(checklistId, executionVehicleId)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Show refresh indicator
    if (uiState.isRefreshing) {
        LaunchedEffect(uiState.isRefreshing) {
            // Pull-to-refresh effect would go here
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Checklisten suchen...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Suchen")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(Icons.Default.Clear, contentDescription = "Löschen")
                }
            }
        },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun ChecklistList(
    checklists: List<Checklist>,
    isTemplate: Boolean,
    onChecklistClick: (Int) -> Unit,
    onExecuteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(checklists) { checklist ->
            ChecklistCard(
                checklist = checklist,
                isTemplate = isTemplate,
                onClick = { onChecklistClick(checklist.id) },
                onExecuteClick = { onExecuteClick(checklist.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistCard(
    checklist: Checklist,
    isTemplate: Boolean,
    onClick: () -> Unit,
    onExecuteClick: () -> Unit
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
                        text = checklist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isTemplate) {
                        Text(
                            text = "Vorlage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Items: ${checklist.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isTemplate) {
                    IconButton(onClick = onExecuteClick) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Checkliste ausführen",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
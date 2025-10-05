package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.feuerwehr.checklist.domain.model.ChecklistItem
import com.feuerwehr.checklist.domain.model.ChecklistItemType
import com.feuerwehr.checklist.domain.model.ItemStatus
import com.feuerwehr.checklist.presentation.viewmodel.ChecklistExecutionViewModel
import com.feuerwehr.checklist.presentation.component.ErrorMessage
import com.feuerwehr.checklist.presentation.component.EmptyState

/**
 * Screen for executing a checklist step by step
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistExecutionScreen(
    checklistId: Int,
    vehicleId: Int,
    onNavigateBack: () -> Unit,
    onExecutionComplete: () -> Unit,
    viewModel: ChecklistExecutionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(checklistId, vehicleId) {
        viewModel.startExecution(checklistId, vehicleId)
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onExecutionComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Checkliste ausführen")
                        uiState.checklist?.let { checklist ->
                            Text(
                                text = checklist.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
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
                val errorMessage = uiState.error!!
                ErrorMessage(
                    error = errorMessage,
                    onRetry = { viewModel.startExecution(checklistId, vehicleId) },
                    onDismiss = viewModel::clearError
                )
            }
            else -> {
                val currentItem = viewModel.getCurrentItem()
                if (currentItem != null) {
                    ChecklistItemExecution(
                        item = currentItem,
                        itemIndex = uiState.currentItemIndex,
                        totalItems = uiState.checklistItems.size,
                        isLastItem = viewModel.isLastItem(),
                        isSubmitting = uiState.isSubmitting,
                        existingResult = viewModel.getItemResult(currentItem.id),
                        onSubmitResult = { status, wert, vorhanden, tuvDatum, tuvStatus, menge, kommentar ->
                            viewModel.submitItemResult(
                                itemId = currentItem.id,
                                status = status,
                                wert = wert,
                                vorhanden = vorhanden,
                                tuvDatum = tuvDatum,
                                tuvStatus = tuvStatus,
                                menge = menge,
                                kommentar = kommentar
                            )
                        },
                        onPrevious = viewModel::previousItem,
                        onNext = viewModel::nextItem,
                        onComplete = viewModel::completeExecution,
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    EmptyState(
                        message = "Keine Items in dieser Checkliste",
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistItemExecution(
    item: ChecklistItem,
    itemIndex: Int,
    totalItems: Int,
    isLastItem: Boolean,
    isSubmitting: Boolean,
    existingResult: com.feuerwehr.checklist.domain.model.ItemResult?,
    onSubmitResult: (
        status: ItemStatus,
        wert: Map<String, Any>?,
        vorhanden: Boolean?,
        tuvDatum: kotlinx.datetime.LocalDate?,
        tuvStatus: String?,
        menge: Int?,
        kommentar: String?
    ) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember(item.id) { mutableStateOf(existingResult?.status ?: ItemStatus.OK) }
    var vorhanden by remember(item.id) { mutableStateOf(existingResult?.vorhanden ?: true) }
    var kommentar by remember(item.id) { mutableStateOf(existingResult?.kommentar ?: "") }
    var menge by remember(item.id) { mutableStateOf(existingResult?.menge?.toString() ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (itemIndex + 1).toFloat() / totalItems,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Item ${itemIndex + 1} von $totalItems",
            style = MaterialTheme.typography.labelMedium
        )

        // Item details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.beschreibung,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (item.pflicht) {
                    Text(
                        text = "Pflichtfeld",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = "Typ: ${item.itemType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Status selection
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Status auswählen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                ItemStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (status) {
                                ItemStatus.OK -> "OK"
                                ItemStatus.FEHLER -> "Fehler"
                                ItemStatus.NICHT_PRUEFBAR -> "Nicht prüfbar"
                            }
                        )
                    }
                }
            }
        }

        // Item-specific input based on type
        when (item.itemType) {
            ChecklistItemType.STANDARD -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Vorhanden?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = vorhanden,
                                onCheckedChange = { vorhanden = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (vorhanden) "Ja" else "Nein")
                        }
                    }
                }
            }
            ChecklistItemType.QUANTITY -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Menge eingeben",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = menge,
                            onValueChange = { menge = it },
                            label = { Text("Anzahl") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                // For other types, show a placeholder
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Weitere Eingabefelder für ${item.itemType.name} werden noch implementiert",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Comment field
        OutlinedTextField(
            value = kommentar,
            onValueChange = { kommentar = it },
            label = { Text("Kommentar (optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (itemIndex > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zurück")
                }
            }
            
            Button(
                onClick = {
                    val mengeInt = menge.toIntOrNull()
                    onSubmitResult(
                        selectedStatus,
                        null, // wert - to be implemented for specific item types
                        vorhanden,
                        null, // tuvDatum - to be implemented
                        null, // tuvStatus - to be implemented
                        mengeInt,
                        kommentar.takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    if (isLastItem) {
                        Text("Abschließen")
                    } else {
                        Text("Weiter")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

// Removed duplicate ErrorMessage and EmptyState implementations
// Using shared components from com.feuerwehr.checklist.presentation.component package
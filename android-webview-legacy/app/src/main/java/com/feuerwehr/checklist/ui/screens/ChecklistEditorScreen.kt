package com.feuerwehr.checklist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.feuerwehr.checklist.data.models.ChecklistItem
import com.feuerwehr.checklist.ui.viewmodel.ChecklistViewModel

/**
 * Admin checklist editor screen for creating/editing checklists
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistEditorScreen(
    checklistId: Int?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChecklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var checklistName by remember { mutableStateOf("") }
    var checklistDescription by remember { mutableStateOf("") }
    var fahrzeuggruppe by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ChecklistItemEditor>()) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    
    val isEditing = checklistId != null && checklistId != -1
    
    // Load existing checklist if editing
    LaunchedEffect(checklistId) {
        if (isEditing && checklistId != null) {
            viewModel.getChecklistById(checklistId)
        }
    }
    
    // Update form when checklist loads
    LaunchedEffect(uiState.selectedChecklist) {
        uiState.selectedChecklist?.let { checklist ->
            checklistName = checklist.name
            checklistDescription = checklist.description ?: ""
            fahrzeuggruppe = checklist.fahrzeuggruppe
            items = checklist.items?.map { item ->
                ChecklistItemEditor(
                    id = item.id,
                    beschreibung = item.beschreibung,
                    kategorie = item.kategorie ?: "",
                    pflicht = item.pflicht,
                    reihenfolge = item.reihenfolge,
                    erwarteterWert = item.erwarteterWert ?: "",
                    einheit = item.einheit ?: ""
                )
            } ?: emptyList()
        }
    }
    
    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaveSuccess()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar with save/cancel
        TopAppBar(
            title = { 
                Text(if (isEditing) "Checkliste bearbeiten" else "Neue Checkliste") 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (checklistName.isNotBlank() && fahrzeuggruppe.isNotBlank()) {
                            val checklistItems = items.mapIndexed { index, item ->
                                ChecklistItem(
                                    id = item.id ?: 0,
                                    checklisteId = checklistId ?: 0,
                                    beschreibung = item.beschreibung,
                                    kategorie = item.kategorie.takeIf { it.isNotBlank() },
                                    pflicht = item.pflicht,
                                    reihenfolge = index + 1,
                                    erwarteterWert = item.erwarteterWert.takeIf { it.isNotBlank() },
                                    einheit = item.einheit.takeIf { it.isNotBlank() },
                                    createdAt = System.currentTimeMillis()
                                )
                            }
                            
                            if (isEditing && checklistId != null) {
                                viewModel.updateChecklist(
                                    id = checklistId,
                                    name = checklistName,
                                    description = checklistDescription.takeIf { it.isNotBlank() },
                                    fahrzeuggruppe = fahrzeuggruppe,
                                    items = checklistItems
                                )
                            } else {
                                viewModel.createChecklist(
                                    name = checklistName,
                                    description = checklistDescription.takeIf { it.isNotBlank() },
                                    fahrzeuggruppe = fahrzeuggruppe,
                                    items = checklistItems
                                )
                            }
                        }
                    },
                    enabled = checklistName.isNotBlank() && fahrzeuggruppe.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Speichern")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic information
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Grunddaten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = checklistName,
                            onValueChange = { checklistName = it },
                            label = { Text("Name der Checkliste *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = checklistName.isBlank()
                        )
                        
                        OutlinedTextField(
                            value = checklistDescription,
                            onValueChange = { checklistDescription = it },
                            label = { Text("Beschreibung (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        
                        OutlinedTextField(
                            value = fahrzeuggruppe,
                            onValueChange = { fahrzeuggruppe = it },
                            label = { Text("Fahrzeuggruppe *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = fahrzeuggruppe.isBlank(),
                            placeholder = { Text("z.B. MTF, LF, TLF, RTB") }
                        )
                    }
                }
            }
            
            // Items section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prüfpunkte (${items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedButton(
                        onClick = { showAddItemDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prüfpunkt")
                    }
                }
            }
            
            // Checklist items
            itemsIndexed(items) { index, item ->
                ChecklistItemCard(
                    item = item,
                    index = index,
                    onItemUpdate = { updatedItem ->
                        items = items.toMutableList().apply {
                            this[index] = updatedItem
                        }
                    },
                    onItemDelete = {
                        items = items.toMutableList().apply {
                            removeAt(index)
                        }
                    },
                    onMoveUp = if (index > 0) {
                        {
                            items = items.toMutableList().apply {
                                val temp = this[index]
                                this[index] = this[index - 1]
                                this[index - 1] = temp
                            }
                        }
                    } else null,
                    onMoveDown = if (index < items.size - 1) {
                        {
                            items = items.toMutableList().apply {
                                val temp = this[index]
                                this[index] = this[index + 1]
                                this[index + 1] = temp
                            }
                        }
                    } else null
                )
            }
            
            // Empty state
            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Keine Prüfpunkte",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Fügen Sie Prüfpunkte für diese Checkliste hinzu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add item dialog
    if (showAddItemDialog) {
        AddItemDialog(
            onItemAdd = { newItem ->
                items = items + newItem
                showAddItemDialog = false
            },
            onDismiss = { showAddItemDialog = false }
        )
    }
    
    // Error display
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show error snackbar
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistItemCard(
    item: ChecklistItemEditor,
    index: Int,
    onItemUpdate: (ChecklistItemEditor) -> Unit,
    onItemDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${item.beschreibung}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.kategorie.isNotBlank()) {
                            AssistChip(
                                onClick = { },
                                label = { Text(item.kategorie) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        
                        AssistChip(
                            onClick = { },
                            label = { Text(if (item.pflicht) "Pflicht" else "Optional") },
                            leadingIcon = {
                                Icon(
                                    if (item.pflicht) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                
                Row {
                    // Move buttons
                    onMoveUp?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Nach oben")
                        }
                    }
                    onMoveDown?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nach unten")
                        }
                    }
                    
                    // Expand button
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Einklappen" else "Ausklappen"
                        )
                    }
                }
            }
            
            // Expanded content
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = item.beschreibung,
                        onValueChange = { onItemUpdate(item.copy(beschreibung = it)) },
                        label = { Text("Beschreibung *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = item.kategorie,
                        onValueChange = { onItemUpdate(item.copy(kategorie = it)) },
                        label = { Text("Kategorie") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("z.B. Elektronik, Sicherheit, Ausrüstung") }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = item.erwarteterWert,
                            onValueChange = { onItemUpdate(item.copy(erwarteterWert = it)) },
                            label = { Text("Erwarteter Wert") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = item.einheit,
                            onValueChange = { onItemUpdate(item.copy(einheit = it)) },
                            label = { Text("Einheit") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("z.B. V, bar, °C") }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.pflicht,
                                onCheckedChange = { onItemUpdate(item.copy(pflicht = it)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pflichtprüfung")
                        }
                        
                        OutlinedButton(
                            onClick = onItemDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Löschen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
    onItemAdd: (ChecklistItemEditor) -> Unit,
    onDismiss: () -> Unit
) {
    var beschreibung by remember { mutableStateOf("") }
    var kategorie by remember { mutableStateOf("") }
    var erwarteterWert by remember { mutableStateOf("") }
    var einheit by remember { mutableStateOf("") }
    var pflicht by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prüfpunkt hinzufügen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = beschreibung,
                    onValueChange = { beschreibung = it },
                    label = { Text("Beschreibung *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = kategorie,
                    onValueChange = { kategorie = it },
                    label = { Text("Kategorie") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = erwarteterWert,
                        onValueChange = { erwarteterWert = it },
                        label = { Text("Erwarteter Wert") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = einheit,
                        onValueChange = { einheit = it },
                        label = { Text("Einheit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = pflicht,
                        onCheckedChange = { pflicht = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pflichtprüfung")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (beschreibung.isNotBlank()) {
                        onItemAdd(
                            ChecklistItemEditor(
                                id = null,
                                beschreibung = beschreibung,
                                kategorie = kategorie,
                                pflicht = pflicht,
                                reihenfolge = 0,
                                erwarteterWert = erwarteterWert,
                                einheit = einheit
                            )
                        )
                    }
                },
                enabled = beschreibung.isNotBlank()
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

data class ChecklistItemEditor(
    val id: Int?,
    val beschreibung: String,
    val kategorie: String,
    val pflicht: Boolean,
    val reihenfolge: Int,
    val erwarteterWert: String,
    val einheit: String
)
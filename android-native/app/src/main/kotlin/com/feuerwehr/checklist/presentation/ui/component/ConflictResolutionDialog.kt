package com.feuerwehr.checklist.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.feuerwehr.checklist.data.sync.ConflictResolutionStrategy
import com.feuerwehr.checklist.data.sync.SyncableEntity
import kotlinx.datetime.Instant

/**
 * Dialog for resolving sync conflicts
 * Allows users to choose conflict resolution strategy
 */
@Composable
fun ConflictResolutionDialog(
    conflicts: List<ConflictData>,
    onResolveConflict: (ConflictData, ConflictResolutionStrategy) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Konflikt",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Sync-Konflikte lösen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Die folgenden Daten haben Konflikte zwischen lokalen und Remote-Änderungen:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Conflict list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conflicts) { conflict ->
                        ConflictItem(
                            conflict = conflict,
                            onResolve = { strategy ->
                                onResolveConflict(conflict, strategy)
                            }
                        )
                    }
                }
                
                // Global resolution buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            conflicts.forEach { conflict ->
                                onResolveConflict(conflict, ConflictResolutionStrategy.REMOTE_WINS)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alle Remote")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            conflicts.forEach { conflict ->
                                onResolveConflict(conflict, ConflictResolutionStrategy.LOCAL_WINS)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alle Lokal")
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fertig")
                    }
                }
            }
        }
    }
}

/**
 * Individual conflict item with resolution options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictItem(
    conflict: ConflictData,
    onResolve: (ConflictResolutionStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStrategy by remember { mutableStateOf<ConflictResolutionStrategy?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Conflict summary
            Text(
                text = "${conflict.entityType} ID: ${conflict.entityId}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Lokal: ${conflict.localLastModified} • Remote: ${conflict.remoteLastModified}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            if (isExpanded) {
                Divider()
                
                // Resolution options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Wähle Auflösung:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = {
                                selectedStrategy = ConflictResolutionStrategy.LOCAL_WINS
                                onResolve(ConflictResolutionStrategy.LOCAL_WINS)
                            },
                            label = { Text("Lokal behalten") },
                            selected = selectedStrategy == ConflictResolutionStrategy.LOCAL_WINS
                        )
                        
                        FilterChip(
                            onClick = {
                                selectedStrategy = ConflictResolutionStrategy.REMOTE_WINS
                                onResolve(ConflictResolutionStrategy.REMOTE_WINS)
                            },
                            label = { Text("Remote verwenden") },
                            selected = selectedStrategy == ConflictResolutionStrategy.REMOTE_WINS
                        )
                        
                        FilterChip(
                            onClick = {
                                selectedStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
                                onResolve(ConflictResolutionStrategy.LAST_WRITE_WINS)
                            },
                            label = { Text("Neueste") },
                            selected = selectedStrategy == ConflictResolutionStrategy.LAST_WRITE_WINS
                        )
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a conflict for the UI
 */
data class ConflictData(
    val entityType: String,
    val entityId: String,
    val localEntity: SyncableEntity,
    val remoteEntity: SyncableEntity,
    val localLastModified: Instant,
    val remoteLastModified: Instant
)

/**
 * Extension function to convert sync conflicts to UI data
 */
fun <T : SyncableEntity> Map<String, Pair<T, T>>.toConflictData(): List<ConflictData> {
    return this.map { (id, pair) ->
        val (local, remote) = pair
        ConflictData(
            entityType = local::class.simpleName ?: "Unknown",
            entityId = id,
            localEntity = local,
            remoteEntity = remote,
            localLastModified = local.getLastModifiedTime(),
            remoteLastModified = remote.getLastModifiedTime()
        )
    }
}
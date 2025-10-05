package com.feuerwehr.checklist.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmation dialog components for consistent user confirmation flows
 * Follows Material Design guidelines with German fire department context
 */

/**
 * Standard confirmation dialog for destructive actions
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Bestätigen",
    dismissText: String = "Abbrechen",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Delete confirmation dialog specifically for fire department assets
 */
@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    itemType: String = "Element",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "$itemType löschen",
        message = "Möchten Sie \"$itemName\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.",
        confirmText = "Löschen",
        dismissText = "Abbrechen",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
        icon = Icons.Default.Delete
    )
}

/**
 * Logout confirmation dialog
 */
@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Abmelden",
        message = "Möchten Sie sich wirklich abmelden? Nicht gespeicherte Änderungen gehen verloren.",
        confirmText = "Abmelden",
        dismissText = "Abbrechen",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Default.ExitToApp
    )
}

/**
 * Checklist execution completion confirmation
 */
@Composable
fun ChecklistCompletionDialog(
    checklistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Checkliste abschließen",
        message = "Möchten Sie die Ausführung von \"$checklistName\" wirklich abschließen? Danach können keine Änderungen mehr vorgenommen werden.",
        confirmText = "Abschließen",
        dismissText = "Zurück",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Default.CheckCircle
    )
}

/**
 * Vehicle out-of-service confirmation
 */
@Composable
fun VehicleOutOfServiceDialog(
    vehicleKennzeichen: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Fahrzeug außer Dienst stellen",
        message = "Möchten Sie das Fahrzeug \"$vehicleKennzeichen\" wirklich außer Dienst stellen? Das Fahrzeug wird als nicht einsatzbereit markiert.",
        confirmText = "Außer Dienst",
        dismissText = "Abbrechen",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
        icon = Icons.Default.Warning
    )
}

/**
 * TÜV expiration warning dialog
 */
@Composable
fun TuvExpirationWarningDialog(
    vehicleKennzeichen: String,
    expirationDate: String,
    onAcknowledge: () -> Unit,
    onScheduleTuv: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "TÜV-Warnung",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Das Fahrzeug \"$vehicleKennzeichen\" hat einen abgelaufenen oder bald ablaufenden TÜV-Termin.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Ablaufdatum: $expirationDate",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Fahrzeuge mit abgelaufenem TÜV dürfen nicht im Einsatz verwendet werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAcknowledge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Verstanden")
            }
        },
        dismissButton = onScheduleTuv?.let {
            {
                TextButton(onClick = it) {
                    Text("TÜV planen")
                }
            }
        }
    )
}

/**
 * Sync conflict resolution dialog
 */
@Composable
fun SyncConflictDialog(
    conflictDescription: String,
    localVersion: String,
    serverVersion: String,
    onUseLocal: () -> Unit,
    onUseServer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                text = "Synchronisierungskonflikt",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = conflictDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Lokale Version: $localVersion",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Server Version: $serverVersion",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Welche Version möchten Sie behalten?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onUseLocal) {
                    Text("Lokal")
                }
                Button(onClick = onUseServer) {
                    Text("Server")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Checklist template creation dialog
 */
@Composable
fun CreateTemplateDialog(
    checklistName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("$checklistName (Vorlage)") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FileCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Vorlage erstellen",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Erstellen Sie eine Vorlage basierend auf \"$checklistName\".",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Name der Vorlage") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(templateName)
                    onDismiss()
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Progress dialog for long-running operations
 */
@Composable
fun ProgressDialog(
    title: String,
    message: String,
    progress: Float? = null, // null for indeterminate progress
    onCancel: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = onCancel != null,
            dismissOnClickOutside = false
        ),
        icon = {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% abgeschlossen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { },
        dismissButton = onCancel?.let {
            {
                TextButton(onClick = it) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
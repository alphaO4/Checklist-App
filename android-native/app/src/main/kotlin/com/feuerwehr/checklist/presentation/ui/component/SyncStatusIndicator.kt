package com.feuerwehr.checklist.presentation.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.feuerwehr.checklist.data.sync.SyncState
import com.feuerwehr.checklist.presentation.theme.FireDepartmentTheme

/**
 * Sync status indicator component
 * Shows current synchronization state with visual feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    onSyncClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color, message) = when {
        !syncState.isOnline -> Triple(
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.error,
            "Offline"
        )
        syncState.isSyncing -> Triple(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.primary,
            "Synchronisiere..."
        )
        syncState.conflicts > 0 -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error,
            "${syncState.conflicts} Konflikte"
        )
        syncState.pendingUploads > 0 -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.secondary,
            "${syncState.pendingUploads} ausstehend"
        )
        syncState.lastSyncTime != null -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50), // Green success color
            "Aktuell"
        )
        else -> Triple(
            Icons.Default.Sync,
            MaterialTheme.colorScheme.outline,
            "Nie synchronisiert"
        )
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "sync_color_animation"
    )

    Card(
        onClick = { if (!syncState.isSyncing) onSyncClicked() },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        enabled = !syncState.isSyncing,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Sync Status",
                tint = animatedColor,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = animatedColor
            )
            
            // Show additional info if available
            syncState.lastError?.let { error ->
                Text(
                    text = " • $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Compact sync status indicator for toolbars
 */
@Composable
fun CompactSyncStatusIndicator(
    syncState: SyncState,
    onSyncClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when {
        !syncState.isOnline -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
        syncState.isSyncing -> Icons.Default.Sync to MaterialTheme.colorScheme.primary
        syncState.conflicts > 0 -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        syncState.pendingUploads > 0 -> Icons.Default.Warning to MaterialTheme.colorScheme.secondary
        else -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "compact_sync_color_animation"
    )

    IconButton(
        onClick = { if (!syncState.isSyncing) onSyncClicked() },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sync Status",
            tint = animatedColor
        )
    }
    
    // Show badge for pending items
    if (syncState.pendingUploads > 0 || syncState.conflicts > 0) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}
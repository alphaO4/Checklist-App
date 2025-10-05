package com.feuerwehr.checklist.presentation.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.feuerwehr.checklist.domain.exception.ChecklistException
import com.feuerwehr.checklist.presentation.error.ErrorSeverity
import com.feuerwehr.checklist.presentation.error.ErrorState
import com.feuerwehr.checklist.presentation.error.getSeverity
import com.feuerwehr.checklist.presentation.error.requiresImmediateAction

/**
 * Error display component with different styles based on error severity
 * Provides user-friendly German error messages with retry options
 */
@Composable
fun ErrorDisplay(
    errorState: ErrorState?,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    AnimatedVisibility(
        visible = errorState != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        errorState?.let { state ->
            if (compact) {
                CompactErrorDisplay(
                    errorState = state,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            } else {
                FullErrorDisplay(
                    errorState = state,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * Full error display with detailed information
 */
@Composable
private fun FullErrorDisplay(
    errorState: ErrorState,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    val severity = errorState.exception.getSeverity()
    val (backgroundColor, textColor, icon) = getErrorColors(severity)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and title
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Fehler",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = getSeverityTitle(severity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Dismiss button
                onDismiss?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Error message
            Text(
                text = errorState.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            
            // Error code for debugging (if needed)
            if (errorState.errorCode.isNotEmpty()) {
                Text(
                    text = "Fehlercode: ${errorState.errorCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Retry button
                if (errorState.isRetryable && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = textColor,
                            contentColor = backgroundColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wiederholen")
                    }
                }
                
                // Additional actions for specific error types
                if (errorState.exception.requiresImmediateAction()) {
                    OutlinedButton(
                        onClick = { /* Handle specific action */ },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(textColor)
                        )
                    ) {
                        Text("Lösen")
                    }
                }
            }
        }
    }
}

/**
 * Compact error display for limited space
 */
@Composable
private fun CompactErrorDisplay(
    errorState: ErrorState,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    val severity = errorState.exception.getSeverity()
    val (backgroundColor, textColor, icon) = getErrorColors(severity)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Fehler",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = errorState.userMessage,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Compact action buttons
            if (errorState.isRetryable && onRetry != null) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Wiederholen",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            onDismiss?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Snackbar-style error display
 */
@Composable
fun ErrorSnackbar(
    errorState: ErrorState,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorState.isRetryable && onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text("WIEDERHOLEN")
                    }
                }
                onDismiss?.let {
                    TextButton(onClick = it) {
                        Text("OK")
                    }
                }
            }
        },
        dismissAction = onDismiss?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen"
                    )
                }
            }
        }
    ) {
        Text(
            text = errorState.userMessage,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Gets colors and icon based on error severity
 */
@Composable
private fun getErrorColors(severity: ErrorSeverity): Triple<Color, Color, ImageVector> {
    return when (severity) {
        ErrorSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Info
        )
        ErrorSeverity.WARNING -> Triple(
            Color(0xFFFFF3CD), // Light yellow
            Color(0xFF856404), // Dark yellow
            Icons.Default.Warning
        )
        ErrorSeverity.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        ErrorSeverity.CRITICAL -> Triple(
            Color(0xFFD32F2F), // Dark red
            Color.White,
            Icons.Default.ErrorOutline
        )
    }
}

/**
 * Gets title text based on severity
 */
private fun getSeverityTitle(severity: ErrorSeverity): String {
    return when (severity) {
        ErrorSeverity.INFO -> "Information"
        ErrorSeverity.WARNING -> "Warnung"
        ErrorSeverity.ERROR -> "Fehler"
        ErrorSeverity.CRITICAL -> "Kritischer Fehler"
    }
}

/**
 * Preview error states for testing
 */
@Composable
fun PreviewErrorStates() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Network error
        ErrorDisplay(
            errorState = ErrorState(
                exception = com.feuerwehr.checklist.domain.exception.NetworkException.NoInternetConnection(),
                userMessage = "Keine Internetverbindung verfügbar.",
                errorCode = "NETWORK_001",
                isRetryable = true,
                timestamp = System.currentTimeMillis()
            ),
            onRetry = { },
            onDismiss = { }
        )
        
        // Authentication error
        ErrorDisplay(
            errorState = ErrorState(
                exception = com.feuerwehr.checklist.domain.exception.AuthenticationException.InvalidCredentials(),
                userMessage = "Benutzername oder Passwort ist ungültig.",
                errorCode = "AUTH_001",
                isRetryable = false,
                timestamp = System.currentTimeMillis()
            ),
            onDismiss = { },
            compact = true
        )
    }
}
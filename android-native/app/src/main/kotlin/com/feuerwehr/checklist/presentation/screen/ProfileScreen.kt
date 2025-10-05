package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.compose.collectAsState
import com.feuerwehr.checklist.presentation.viewmodel.AuthViewModel
import com.feuerwehr.checklist.presentation.components.ConfirmationDialogs
import com.feuerwehr.checklist.domain.model.UserRole

/**
 * Profile Screen for user account management and app settings
 * Shows user information, role, and provides access to settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        viewModel.getCurrentUser()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.currentUser?.let { user ->
                ProfileHeader(
                    username = user.username,
                    role = user.role,
                    groupName = user.groupName
                )
                
                UserDetailsCard(
                    username = user.username,
                    role = user.role,
                    groupName = user.groupName,
                    createdAt = user.createdAt.toString()
                )
                
                SettingsSection(
                    onLogoutClick = { showLogoutDialog = true }
                )
                
                AboutSection()
            } ?: run {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Benutzerinformationen nicht verfügbar")
                    }
                }
            }
        }
    }
    
    if (showLogoutDialog) {
        ConfirmationDialogs.LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLogout()
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    role: UserRole,
    groupName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = getRoleDisplayName(role),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                if (groupName != null) {
                    Text(
                        text = "Gruppe: $groupName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserDetailsCard(
    username: String,
    role: UserRole,
    groupName: String?,
    createdAt: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Benutzerinformationen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            UserDetailRow(
                icon = Icons.Default.Person,
                label = "Benutzername",
                value = username
            )
            
            UserDetailRow(
                icon = Icons.Default.Security,
                label = "Rolle",
                value = getRoleDisplayName(role)
            )
            
            if (groupName != null) {
                UserDetailRow(
                    icon = Icons.Default.Group,
                    label = "Gruppe",
                    value = groupName
                )
            }
            
            UserDetailRow(
                icon = Icons.Default.CalendarToday,
                label = "Registriert seit",
                value = createdAt.substringBefore('T') // Simple date formatting
            )
        }
    }
}

@Composable
private fun UserDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsSection(
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "Abmelden",
                subtitle = "Aus der Anwendung abmelden",
                onClick = onLogoutClick,
                showArrow = false,
                textColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Über die App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Feuerwehr Checklisten App",
                subtitle = "Version 1.0.0",
                onClick = { /* TODO: Show version details */ }
            )
            
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Fahrzeugprüfungen",
                subtitle = "TÜV und Wartungschecklisten verwalten",
                onClick = { /* TODO: Show help */ }
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = true,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = textColor.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

private fun getRoleDisplayName(role: UserRole): String {
    return when (role) {
        UserRole.BENUTZER -> "Benutzer"
        UserRole.GRUPPENLEITER -> "Gruppenleiter"
        UserRole.ORGANISATOR -> "Organisator"
        UserRole.ADMIN -> "Administrator"
    }
}
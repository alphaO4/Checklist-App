package com.feuerwehr.checklist.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.feuerwehr.checklist.presentation.viewmodel.LoginViewModel
import com.feuerwehr.checklist.presentation.component.UsernameField
import com.feuerwehr.checklist.presentation.component.PasswordField

/**
 * Login screen with native Android UI
 * Replaces the WebView-based login from the old implementation
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Try auto-login when screen loads
    LaunchedEffect(Unit) {
        viewModel.tryAutoLogin()
    }
    
    // Handle successful login
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "Feuerwehr Checkliste",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Fahrzeugprüfung und TÜV-Verwaltung",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Username field with validation
        UsernameField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field with validation
        PasswordField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Remember Me checkbox
        var rememberMe by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                enabled = !uiState.isLoading
            )
            Text(
                text = "Anmeldedaten merken",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login button with proper validation
        val isFormValid = username.isNotBlank() && 
                         password.isNotBlank() && 
                         username.length >= 3 && 
                         password.length >= 6
        
        Button(
            onClick = { viewModel.login(username, password, rememberMe) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && isFormValid
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Anmelden")
            }
        }
        
        // Error message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
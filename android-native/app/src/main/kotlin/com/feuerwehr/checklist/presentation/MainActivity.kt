package com.feuerwehr.checklist.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.feuerwehr.checklist.presentation.navigation.ChecklistNavigation
import com.feuerwehr.checklist.presentation.theme.ChecklistTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the Android-first Checklist App
 * Uses Jetpack Compose for native Android UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChecklistNavigation()
                }
            }
        }
    }
}
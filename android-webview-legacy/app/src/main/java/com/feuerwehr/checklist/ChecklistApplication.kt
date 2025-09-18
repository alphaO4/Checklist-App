package com.feuerwehr.checklist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Fire Department Checklist App
 * Enables Hilt dependency injection
 */
@HiltAndroidApp
class ChecklistApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Application initialization
        // Hilt will handle all dependency injection setup
    }
}
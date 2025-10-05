package com.feuerwehr.checklist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.remote.api.*
import com.feuerwehr.checklist.data.error.RepositoryErrorHandler
import com.feuerwehr.checklist.core.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test configuration for dependency injection
 * Provides test doubles and in-memory database for testing
 */
@Module
@InstallIn(SingletonComponent::class)
object TestModule {

    @Provides
    @Singleton
    fun provideTestDatabase(): ChecklistDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChecklistDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @Provides
    @Singleton 
    fun provideMockAuthApiService(): AuthApiService = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideMockVehicleApiService(): VehicleApiService = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideMockChecklistApiService(): ChecklistApiService = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideRepositoryErrorHandler(): RepositoryErrorHandler = RepositoryErrorHandler()
}

/**
 * Test application class for running tests
 */
class TestChecklistApplication : ChecklistApplication() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize test-specific configurations
        setupTestLogging()
    }
    
    private fun setupTestLogging() {
        // Configure logging for test environment
        AppLogger.d(AppLogger.Context.SYSTEM, "Test environment initialized")
    }
}
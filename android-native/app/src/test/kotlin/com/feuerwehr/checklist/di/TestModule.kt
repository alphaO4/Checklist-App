package com.feuerwehr.checklist.di

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.local.dao.ChecklistDao
import com.feuerwehr.checklist.data.remote.api.AuthApiService
import com.feuerwehr.checklist.data.remote.api.VehicleApiService
import com.feuerwehr.checklist.data.remote.api.ChecklistApiService
import com.feuerwehr.checklist.data.local.storage.TokenStorage
import com.feuerwehr.checklist.data.local.storage.SecureStorage
import com.feuerwehr.checklist.data.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito.mock
import javax.inject.Singleton

/**
 * Test module for Hilt dependency injection in tests
 * Provides mock dependencies and in-memory database for testing
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class, NetworkModule::class, StorageModule::class]
)
object TestModule {

    @Provides
    @Singleton
    fun provideTestDatabase(): ChecklistDatabase {
        return Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChecklistDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
    }

    @Provides
    fun provideVehicleDao(database: ChecklistDatabase): VehicleDao {
        return database.vehicleDao()
    }

    @Provides
    fun provideChecklistDao(database: ChecklistDatabase): ChecklistDao {
        return database.checklistDao()
    }

    @Provides
    @Singleton
    fun provideMockAuthApiService(): AuthApiService {
        return mock(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMockVehicleApiService(): VehicleApiService {
        return mock(VehicleApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMockChecklistApiService(): ChecklistApiService {
        return mock(ChecklistApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMockTokenStorage(): TokenStorage {
        return mock(TokenStorage::class.java)
    }

    @Provides
    @Singleton
    fun provideMockSecureStorage(): SecureStorage {
        return mock(SecureStorage::class.java)
    }

    @Provides
    @Singleton
    fun provideMockSyncManager(): SyncManager {
        return mock(SyncManager::class.java)
    }
}
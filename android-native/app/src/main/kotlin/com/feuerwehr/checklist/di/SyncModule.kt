package com.feuerwehr.checklist.di

import android.content.Context
import androidx.work.WorkManager
import com.feuerwehr.checklist.data.sync.SyncEngine
import com.feuerwehr.checklist.data.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for sync-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSyncEngine(
        authRepository: com.feuerwehr.checklist.domain.repository.AuthRepository,
        vehicleDao: com.feuerwehr.checklist.data.local.dao.VehicleDao,
        checklistDao: com.feuerwehr.checklist.data.local.dao.ChecklistDao,
        userDao: com.feuerwehr.checklist.data.local.dao.UserDao,
        vehicleApi: com.feuerwehr.checklist.data.remote.api.VehicleApiService,
        checklistApi: com.feuerwehr.checklist.data.remote.api.ChecklistApiService,
        authApi: com.feuerwehr.checklist.data.remote.api.AuthApiService,
        syncManager: SyncManager
    ): SyncEngine {
        return SyncEngine(
            authRepository = authRepository,
            vehicleDao = vehicleDao,
            checklistDao = checklistDao,
            userDao = userDao,
            vehicleApi = vehicleApi,
            checklistApi = checklistApi,
            authApi = authApi,
            syncManager = syncManager
        )
    }
}
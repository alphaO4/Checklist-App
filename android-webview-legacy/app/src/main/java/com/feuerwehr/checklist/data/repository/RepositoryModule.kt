package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.auth.AuthRepository
import com.feuerwehr.checklist.data.auth.SecureTokenStorage
import com.feuerwehr.checklist.data.database.*
import com.feuerwehr.checklist.data.network.AuthInterceptor
import com.feuerwehr.checklist.data.network.ChecklistApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiService: ChecklistApiService,
        tokenStorage: SecureTokenStorage,
        userDao: UserDao,
        authInterceptor: AuthInterceptor
    ): AuthRepository {
        return AuthRepository(apiService, tokenStorage, userDao, authInterceptor)
    }

    @Provides
    @Singleton
    fun provideVehicleRepository(
        apiService: ChecklistApiService,
        vehicleDao: VehicleDao,
        vehicleTypeDao: VehicleTypeDao,
        vehicleGroupDao: VehicleGroupDao
    ): VehicleRepository {
        return VehicleRepository(apiService, vehicleDao, vehicleTypeDao, vehicleGroupDao)
    }

    @Provides
    @Singleton
    fun provideChecklistRepository(
        apiService: ChecklistApiService,
        checklistDao: ChecklistDao,
        checklistItemDao: ChecklistItemDao,
        checklistAusfuehrungDao: ChecklistAusfuehrungDao,
        itemErgebnisDao: ItemErgebnisDao,
        tuvTerminDao: TuvTerminDao
    ): ChecklistRepository {
        return ChecklistRepository(
            apiService,
            checklistDao,
            checklistItemDao,
            checklistAusfuehrungDao,
            itemErgebnisDao,
            tuvTerminDao
        )
    }

    @Provides
    @Singleton
    fun provideTuvRepository(
        apiService: ChecklistApiService,
        tuvTerminDao: TuvTerminDao,
        vehicleDao: VehicleDao
    ): TuvRepository {
        return TuvRepository(apiService, tuvTerminDao, vehicleDao)
    }
}
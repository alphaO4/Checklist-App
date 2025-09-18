package com.feuerwehr.checklist.di

import com.feuerwehr.checklist.data.repository.ChecklistRepositoryImpl
import com.feuerwehr.checklist.data.repository.VehicleRepositoryImpl
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository dependency injection module
 * Binds repository implementations to interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        checklistRepositoryImpl: ChecklistRepositoryImpl
    ): ChecklistRepository

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        vehicleRepositoryImpl: VehicleRepositoryImpl
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.feuerwehr.checklist.data.repository.AuthRepositoryImpl
    ): com.feuerwehr.checklist.domain.repository.AuthRepository
}
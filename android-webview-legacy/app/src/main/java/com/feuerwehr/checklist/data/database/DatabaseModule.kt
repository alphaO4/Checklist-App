package com.feuerwehr.checklist.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideChecklistDatabase(@ApplicationContext context: Context): ChecklistDatabase {
        return ChecklistDatabase.buildDatabase(context)
    }

    @Provides
    fun provideUserDao(database: ChecklistDatabase): UserDao = database.userDao()

    @Provides
    fun provideGroupDao(database: ChecklistDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideVehicleTypeDao(database: ChecklistDatabase): VehicleTypeDao = database.vehicleTypeDao()

    @Provides
    fun provideVehicleGroupDao(database: ChecklistDatabase): VehicleGroupDao = database.vehicleGroupDao()

    @Provides
    fun provideVehicleDao(database: ChecklistDatabase): VehicleDao = database.vehicleDao()

    @Provides
    fun provideTuvTerminDao(database: ChecklistDatabase): TuvTerminDao = database.tuvTerminDao()

    @Provides
    fun provideChecklistDao(database: ChecklistDatabase): ChecklistDao = database.checklistDao()

    @Provides
    fun provideChecklistItemDao(database: ChecklistDatabase): ChecklistItemDao = database.checklistItemDao()

    @Provides
    fun provideChecklistAusfuehrungDao(database: ChecklistDatabase): ChecklistAusfuehrungDao = database.checklistAusfuehrungDao()

    @Provides
    fun provideItemErgebnisDao(database: ChecklistDatabase): ItemErgebnisDao = database.itemErgebnisDao()
}
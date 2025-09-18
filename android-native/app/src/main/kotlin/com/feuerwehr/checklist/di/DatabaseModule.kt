package com.feuerwehr.checklist.di

import android.content.Context
import androidx.room.Room
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.local.dao.ChecklistDao
import com.feuerwehr.checklist.data.local.dao.UserDao
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideChecklistDatabase(@ApplicationContext context: Context): ChecklistDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ChecklistDatabase::class.java,
            ChecklistDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Remove in production
            .build()
    }
    
    @Provides
    fun provideUserDao(database: ChecklistDatabase): UserDao = database.userDao()
    
    @Provides
    fun provideVehicleDao(database: ChecklistDatabase): VehicleDao = database.vehicleDao()
    
    @Provides
    fun provideChecklistDao(database: ChecklistDatabase): ChecklistDao = database.checklistDao()
}
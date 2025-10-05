package com.feuerwehr.checklist.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.feuerwehr.checklist.data.sync.HiltWorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for WorkManager dependency injection
 * Configures WorkManager with custom worker factory
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
        workerFactory: HiltWorkerFactory
    ): WorkManager {
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
        
        WorkManager.initialize(context, config)
        return WorkManager.getInstance(context)
    }
}
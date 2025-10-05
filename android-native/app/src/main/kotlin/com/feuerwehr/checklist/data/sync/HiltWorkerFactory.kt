package com.feuerwehr.checklist.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt WorkerFactory for dependency injection in Workers
 * Enables constructor injection in WorkManager workers
 */
@Singleton
class HiltWorkerFactory @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val checklistRepository: ChecklistRepository,
    private val conflictResolver: ConflictResolver,
    private val syncManager: SyncManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            PeriodicSyncWorker::class.java.name -> {
                PeriodicSyncWorker(
                    context = appContext,
                    params = workerParameters,
                    vehicleRepository = vehicleRepository,
                    checklistRepository = checklistRepository,
                    conflictResolver = conflictResolver,
                    syncManager = syncManager
                )
            }
            ImmediateSyncWorker::class.java.name -> {
                ImmediateSyncWorker(
                    context = appContext,
                    params = workerParameters,
                    vehicleRepository = vehicleRepository,
                    checklistRepository = checklistRepository,
                    conflictResolver = conflictResolver,
                    syncManager = syncManager
                )
            }
            else -> null
        }
    }
}
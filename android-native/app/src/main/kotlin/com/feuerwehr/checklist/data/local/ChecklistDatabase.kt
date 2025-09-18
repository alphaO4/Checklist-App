package com.feuerwehr.checklist.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.feuerwehr.checklist.data.local.converter.DateTimeConverters
import com.feuerwehr.checklist.data.local.dao.ChecklistDao
import com.feuerwehr.checklist.data.local.dao.UserDao
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.local.entity.*

/**
 * Room database for offline-first vehicle checklist storage
 * Mirrors the exact structure of the FastAPI SQLAlchemy backend
 */
@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        VehicleTypeEntity::class,
        VehicleGroupEntity::class,
        VehicleEntity::class,
        TuvAppointmentEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ChecklistExecutionEntity::class,
        ItemResultEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(DateTimeConverters::class)
abstract class ChecklistDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun checklistDao(): ChecklistDao
    
    companion object {
        const val DATABASE_NAME = "checklist_database"
        
        @Volatile
        private var INSTANCE: ChecklistDatabase? = null
        
        fun getDatabase(context: Context): ChecklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChecklistDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // For better debugging
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
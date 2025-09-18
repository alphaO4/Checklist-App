package com.feuerwehr.checklist.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.feuerwehr.checklist.data.models.*

/**
 * Room Database for offline storage of Fire Department Checklist data
 */
@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        VehicleTypeEntity::class,
        VehicleGroupEntity::class,
        VehicleEntity::class,
        TuvTerminEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ChecklistAusfuehrungEntity::class,
        ItemErgebnisEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChecklistDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun vehicleTypeDao(): VehicleTypeDao
    abstract fun vehicleGroupDao(): VehicleGroupDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun tuvTerminDao(): TuvTerminDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun checklistAusfuehrungDao(): ChecklistAusfuehrungDao
    abstract fun itemErgebnisDao(): ItemErgebnisDao

    companion object {
        const val DATABASE_NAME = "checklist_database"

        fun buildDatabase(context: Context): ChecklistDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChecklistDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2) // Future migrations
                .fallbackToDestructiveMigration() // For development only
                .build()
        }

        // Future migration example
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration logic for future schema changes
                // Example:
                // database.execSQL("ALTER TABLE users ADD COLUMN new_column TEXT DEFAULT ''")
            }
        }
    }
}
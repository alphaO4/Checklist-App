package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.datetime.Instant
import com.feuerwehr.checklist.data.sync.SyncableEntity

/**
 * Room entity for User (Benutzer)
 * Mirrors backend SQLAlchemy model: app/models/user.py -> Benutzer
 */
@Entity(
    tableName = "benutzer",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val email: String,
    val passwordHash: String? = null,    // Only stored locally, not synced
    val rolle: String,                   // UserRole.value
    val createdAt: Instant,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant = createdAt,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is UserEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

enum class SyncStatus {
    SYNCED,          // Fully synchronized
    PENDING_UPLOAD,  // Local changes need upload  
    PENDING_DOWNLOAD,// Remote changes need download
    CONFLICT         // Sync conflict needs resolution
}
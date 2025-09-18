package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.datetime.Instant

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
)

enum class SyncStatus {
    SYNCED,          // Fully synchronized
    PENDING_UPLOAD,  // Local changes need upload  
    PENDING_DOWNLOAD,// Remote changes need download
    CONFLICT         // Sync conflict needs resolution
}
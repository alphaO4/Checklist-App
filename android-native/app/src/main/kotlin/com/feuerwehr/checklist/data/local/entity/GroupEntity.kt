package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant
import com.feuerwehr.checklist.data.sync.SyncableEntity

/**
 * Room entity for Group (Gruppe)
 * Mirrors backend SQLAlchemy model: app/models/group.py -> Gruppe
 */
@Entity(
    tableName = "gruppen",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["gruppenleiterId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = VehicleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeuggrupeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["gruppenleiterId"]),
        Index(value = ["fahrzeuggrupeId"]),
        Index(value = ["name"])
    ]
)
data class GroupEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val gruppenleiterId: Int?,           // Group leader user ID
    val fahrzeuggrupeId: Int?,          // Vehicle group ID
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
        return other is GroupEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}
package com.feuerwehr.checklist.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

/**
 * User data models for the Fire Department Checklist App
 * Corresponds to backend Benutzer model
 */

// Network DTOs for API communication
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer",
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val rolle: String, // "benutzer", "gruppenleiter", "organisator", "admin"
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("gruppe_id")
    val gruppeId: Int? = null,
    val gruppe: GroupDto? = null
)

data class GroupDto(
    val id: Int,
    val name: String,
    @SerializedName("gruppenleiter_id")
    val gruppenleiterId: Int,
    @SerializedName("created_at")
    val createdAt: String
)

// Room database entities
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val username: String,
    val email: String,
    val rolle: String,
    val createdAt: String,
    val gruppeId: Int? = null,
    val lastSyncedAt: Long = 0L // For offline sync tracking
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val gruppenleiterId: Int,
    val createdAt: String,
    val lastSyncedAt: Long = 0L
)

// Domain models for UI layer
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val rolle: UserRole,
    val createdAt: Instant,
    val gruppeId: Int? = null,
    val gruppe: Group? = null
)

data class Group(
    val id: Int,
    val name: String,
    val gruppenleiterId: Int,
    val createdAt: Instant
)

enum class UserRole(val value: String, val displayName: String, val level: Int) {
    BENUTZER("benutzer", "Benutzer", 1),
    GRUPPENLEITER("gruppenleiter", "Gruppenleiter", 2),
    ORGANISATOR("organisator", "Organisator", 3),
    ADMIN("admin", "Administrator", 4);

    companion object {
        fun fromString(role: String): UserRole {
            return entries.find { it.value == role } ?: BENUTZER
        }
    }
    
    fun hasPermissionFor(requiredRole: UserRole): Boolean {
        return this.level >= requiredRole.level
    }
}
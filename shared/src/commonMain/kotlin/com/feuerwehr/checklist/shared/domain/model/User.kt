package com.feuerwehr.checklist.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Shared domain model for User (Benutzer)
 * Can be used by Android, web, and future iOS platforms
 */
@Serializable
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val rolle: UserRole,
    val createdAt: Instant
)

@Serializable
enum class UserRole(val value: String) {
    BENUTZER("benutzer"),
    GRUPPENLEITER("gruppenleiter"),  
    ORGANISATOR("organisator"),
    ADMIN("admin");
    
    companion object {
        fun fromString(value: String): UserRole = 
            values().find { it.value == value } ?: BENUTZER
    }
}
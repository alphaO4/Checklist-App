package com.feuerwehr.checklist.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for User (Benutzer)
 * Maps to backend: app/models/user.py -> Benutzer
 */
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val rolle: UserRole,
    val createdAt: Instant
)

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
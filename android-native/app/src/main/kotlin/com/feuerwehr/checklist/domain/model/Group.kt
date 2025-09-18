package com.feuerwehr.checklist.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for Group (Gruppe)
 * Maps to backend: app/models/group.py -> Gruppe
 */
data class Group(
    val id: Int,
    val name: String,
    val gruppenleiterId: Int?,            // Group leader user ID
    val fahrzeuggrupeId: Int?,           // Vehicle group ID
    val createdAt: Instant
)
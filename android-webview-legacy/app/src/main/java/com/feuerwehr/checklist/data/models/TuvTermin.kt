package com.feuerwehr.checklist.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * TÜV (Technical Inspection) data models for the Fire Department Checklist App
 * Corresponds to backend TuvTermin model
 */

// Network DTOs for API communication
data class TuvTerminDto(
    val id: Int,
    @SerializedName("fahrzeug_id")
    val fahrzeugId: Int,
    @SerializedName("ablauf_datum")
    val ablaufDatum: String, // ISO date string (YYYY-MM-DD)
    val status: String, // "current", "warning", "expired"
    @SerializedName("letzte_pruefung")
    val letztePruefung: String?, // ISO date string
    @SerializedName("created_at")
    val createdAt: String,
    val fahrzeug: VehicleDto? = null
)

data class CreateTuvTerminRequest(
    @SerializedName("fahrzeug_id")
    val fahrzeugId: Int,
    @SerializedName("ablauf_datum")
    val ablaufDatum: String, // ISO date string (YYYY-MM-DD)
    @SerializedName("letzte_pruefung")
    val letztePruefung: String? // ISO date string
)

data class UpdateTuvTerminRequest(
    @SerializedName("ablauf_datum")
    val ablaufDatum: String?,
    @SerializedName("letzte_pruefung")
    val letztePruefung: String?
)

// Room database entity
@Entity(tableName = "tuv_termine")
data class TuvTerminEntity(
    @PrimaryKey
    val id: Int,
    val fahrzeugId: Int,
    val ablaufDatum: String, // ISO date string
    val status: String,
    val letztePruefung: String?,
    val createdAt: String,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false
)

// Domain model for UI layer
data class TuvTermin(
    val id: Int,
    val fahrzeugId: Int,
    val ablaufDatum: LocalDate,
    val status: TuvStatus,
    val letztePruefung: LocalDate?,
    val createdAt: Instant,
    val fahrzeug: Vehicle? = null,
    val isLocalOnly: Boolean = false
) {
    /**
     * Calculate days until expiration (negative means already expired)
     */
    fun daysUntilExpiration(): Long {
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        return ablaufDatum.toEpochDays() - today.toEpochDays()
    }

    /**
     * Check if TÜV inspection is due soon (within warning threshold)
     */
    fun isDueSoon(warningDays: Int = 30): Boolean {
        val daysLeft = daysUntilExpiration()
        return daysLeft in 1..warningDays.toLong()
    }

    /**
     * Check if TÜV inspection has expired
     */
    fun isExpired(): Boolean {
        return daysUntilExpiration() < 0
    }

    /**
     * Get display text for expiration status
     */
    fun getExpirationText(): String {
        val days = daysUntilExpiration()
        return when {
            days < 0 -> "Abgelaufen (vor ${-days} Tagen)"
            days == 0L -> "Läuft heute ab"
            days == 1L -> "Läuft morgen ab"
            days <= 7 -> "Läuft in $days Tagen ab"
            days <= 30 -> "Läuft in $days Tagen ab"
            else -> "Gültig bis ${ablaufDatum}"
        }
    }
}

enum class TuvStatus(val value: String, val displayName: String) {
    CURRENT("current", "Aktuell"),
    WARNING("warning", "Warnung"),
    EXPIRED("expired", "Abgelaufen");

    companion object {
        fun fromString(status: String): TuvStatus {
            return entries.find { it.value == status } ?: CURRENT
        }

        /**
         * Calculate status based on expiration date
         */
        fun calculateStatus(ablaufDatum: LocalDate, warningDays: Int = 30): TuvStatus {
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val daysUntilExpiration = ablaufDatum.toEpochDays() - today.toEpochDays()

            return when {
                daysUntilExpiration < 0 -> EXPIRED
                daysUntilExpiration <= warningDays -> WARNING
                else -> CURRENT
            }
        }
    }
}
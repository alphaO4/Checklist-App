package com.feuerwehr.checklist.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

/**
 * Vehicle data models for the Fire Department Checklist App
 * Corresponds to backend Fahrzeug, FahrzeugTyp, and FahrzeugGruppe models
 */

// Network DTOs for API communication
data class VehicleTypeDto(
    val id: Int,
    val name: String,
    val beschreibung: String?,
    @SerializedName("created_at")
    val createdAt: String
)

data class VehicleGroupDto(
    val id: Int,
    val name: String,
    @SerializedName("gruppe_id")
    val gruppeId: Int,
    @SerializedName("created_at")
    val createdAt: String
)

data class VehicleDto(
    val id: Int,
    val kennzeichen: String,
    @SerializedName("fahrzeugtyp_id")
    val fahrzeugtypId: Int,
    @SerializedName("fahrzeuggruppe_id")
    val fahrzeuggruppeId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val fahrzeugtyp: VehicleTypeDto? = null,
    val fahrzeuggruppe: VehicleGroupDto? = null,
    @SerializedName("tuv_termine")
    val tuvTermine: List<TuvTerminDto>? = null
)

data class CreateVehicleRequest(
    val kennzeichen: String,
    @SerializedName("fahrzeugtyp_id")
    val fahrzeugtypId: Int,
    @SerializedName("fahrzeuggruppe_id")
    val fahrzeuggruppeId: Int
)

data class UpdateVehicleRequest(
    val kennzeichen: String?,
    @SerializedName("fahrzeugtyp_id")
    val fahrzeugtypId: Int?,
    @SerializedName("fahrzeuggruppe_id")
    val fahrzeuggruppeId: Int?
)

// Room database entities
@Entity(tableName = "vehicle_types")
data class VehicleTypeEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val beschreibung: String?,
    val createdAt: String,
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "vehicle_groups")
data class VehicleGroupEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val gruppeId: Int,
    val createdAt: String,
    val lastSyncedAt: Long = 0L
)

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: Int,
    val kennzeichen: String,
    val fahrzeugtypId: Int,
    val fahrzeuggruppeId: Int,
    val createdAt: String,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false // For offline-created vehicles
)

// Domain models for UI layer
data class VehicleType(
    val id: Int,
    val name: String,
    val beschreibung: String?,
    val createdAt: Instant
) {
    companion object {
        val COMMON_TYPES = listOf(
            "MTF" to "Mannschaftstransportfahrzeug",
            "RTB" to "Rüstwagen",
            "LF" to "Löschfahrzeug",
            "TLF" to "Tanklöschfahrzeug",
            "DLK" to "Drehleiter",
            "RW" to "Rüstwagen",
            "ELW" to "Einsatzleitwagen"
        )
    }
}

data class VehicleGroup(
    val id: Int,
    val name: String,
    val gruppeId: Int,
    val createdAt: Instant
)

data class Vehicle(
    val id: Int,
    val kennzeichen: String,
    val fahrzeugtypId: Int,
    val fahrzeuggruppeId: Int,
    val createdAt: Instant,
    val fahrzeugtyp: VehicleType? = null,
    val fahrzeuggruppe: VehicleGroup? = null,
    val tuvTermine: List<TuvTermin>? = null,
    val isLocalOnly: Boolean = false
) {
    val displayName: String
        get() = "$kennzeichen (${fahrzeugtyp?.name ?: "Unbekannt"})"
}
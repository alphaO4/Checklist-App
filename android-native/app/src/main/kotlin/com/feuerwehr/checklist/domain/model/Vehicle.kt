package com.feuerwehr.checklist.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for Vehicle (Fahrzeug)
 * Maps to backend: app/models/vehicle.py -> Fahrzeug
 */
data class Vehicle(
    val id: Int,
    val kennzeichen: String,              // License plate
    val fahrzeugtypId: Int,
    val fahrzeuggruppeId: Int,
    val createdAt: Instant,
    val fahrzeugtyp: VehicleType,         // Joined data
    val fahrzeuggruppe: VehicleGroup? = null, // Joined data (optional)
    val tuvTermine: List<TuvAppointment> = emptyList(),
    val availableChecklists: List<Checklist> = emptyList() // Available checklists through fahrzeuggruppe
)

/**
 * Domain model for VehicleGroup (FahrzeugGruppe)
 * Maps to backend: app/models/vehicle.py -> FahrzeugGruppe
 */
data class VehicleGroup(
    val id: Int,
    val name: String,
    val createdAt: Instant,
    val vehicles: List<Vehicle> = emptyList(),
    val checklists: List<Checklist> = emptyList(),
    val activeChecklists: List<Checklist> = emptyList(),    // Non-template checklists
    val templateChecklists: List<Checklist> = emptyList()   // Template checklists
)

/**
 * Domain model for VehicleType (FahrzeugTyp)
 * Maps to backend: app/models/vehicle_type.py -> FahrzeugTyp
 */
data class VehicleType(
    val id: Int,
    val name: String,                     // MTF, RTB, LF, etc.
    val beschreibung: String?,
    val aktiv: Boolean = true,
    val createdAt: Instant
)

/**
 * Domain model representing the status of a checklist for a vehicle
 * Used for vehicle-checklist relationship data
 */
data class VehicleChecklistStatus(
    val vehicle: Vehicle? = null,         // Vehicle information (when getting vehicles for checklist)
    val checklist: Checklist,            // Checklist information (when getting checklists for vehicle)
    val hasActiveExecution: Boolean,      // Whether there's an active execution
    val activeExecutionId: Int? = null,   // ID of active execution if exists
    val latestExecution: ChecklistExecution? = null  // Most recent execution
)
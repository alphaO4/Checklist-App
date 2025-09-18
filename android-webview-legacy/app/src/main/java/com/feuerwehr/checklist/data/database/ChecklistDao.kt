package com.feuerwehr.checklist.data.database

import androidx.room.*
import com.feuerwehr.checklist.data.models.*

/**
 * Room Database DAOs for offline storage
 */

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Int): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM groups")
    suspend fun clearGroups()
}

@Dao
interface VehicleTypeDao {
    @Query("SELECT * FROM vehicle_types ORDER BY name")
    suspend fun getAllVehicleTypes(): List<VehicleTypeEntity>

    @Query("SELECT * FROM vehicle_types WHERE id = :id")
    suspend fun getVehicleTypeById(id: Int): VehicleTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleType(vehicleType: VehicleTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleTypes(vehicleTypes: List<VehicleTypeEntity>)

    @Update
    suspend fun updateVehicleType(vehicleType: VehicleTypeEntity)

    @Delete
    suspend fun deleteVehicleType(vehicleType: VehicleTypeEntity)

    @Query("DELETE FROM vehicle_types")
    suspend fun clearVehicleTypes()
}

@Dao
interface VehicleGroupDao {
    @Query("SELECT * FROM vehicle_groups ORDER BY name")
    suspend fun getAllVehicleGroups(): List<VehicleGroupEntity>

    @Query("SELECT * FROM vehicle_groups WHERE id = :id")
    suspend fun getVehicleGroupById(id: Int): VehicleGroupEntity?

    @Query("SELECT * FROM vehicle_groups WHERE gruppeId = :gruppeId ORDER BY name")
    suspend fun getVehicleGroupsByGruppe(gruppeId: Int): List<VehicleGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleGroup(vehicleGroup: VehicleGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleGroups(vehicleGroups: List<VehicleGroupEntity>)

    @Update
    suspend fun updateVehicleGroup(vehicleGroup: VehicleGroupEntity)

    @Delete
    suspend fun deleteVehicleGroup(vehicleGroup: VehicleGroupEntity)

    @Query("DELETE FROM vehicle_groups")
    suspend fun clearVehicleGroups()
}

@Dao
interface VehicleDao {
    @Query("""
        SELECT * FROM vehicles 
        ORDER BY kennzeichen
    """)
    suspend fun getAllVehicles(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE kennzeichen LIKE '%' || :kennzeichen || '%'")
    suspend fun searchVehiclesByKennzeichen(kennzeichen: String): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE fahrzeugtypId = :fahrzeugtypId")
    suspend fun getVehiclesByType(fahrzeugtypId: Int): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE fahrzeuggruppeId = :fahrzeuggruppeId")
    suspend fun getVehiclesByGroup(fahrzeuggruppeId: Int): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles")
    suspend fun clearVehicles()
}

@Dao
interface TuvTerminDao {
    @Query("""
        SELECT * FROM tuv_termine 
        ORDER BY ablaufDatum ASC
    """)
    suspend fun getAllTuvTermine(): List<TuvTerminEntity>

    @Query("SELECT * FROM tuv_termine WHERE id = :id")
    suspend fun getTuvTerminById(id: Int): TuvTerminEntity?

    @Query("SELECT * FROM tuv_termine WHERE fahrzeugId = :fahrzeugId ORDER BY ablaufDatum DESC")
    suspend fun getTuvTermineByVehicle(fahrzeugId: Int): List<TuvTerminEntity>

    @Query("SELECT * FROM tuv_termine WHERE status = :status ORDER BY ablaufDatum ASC")
    suspend fun getTuvTermineByStatus(status: String): List<TuvTerminEntity>

    @Query("""
        SELECT * FROM tuv_termine 
        WHERE status IN ('warning', 'expired') 
        ORDER BY ablaufDatum ASC
    """)
    suspend fun getTuvAlerts(): List<TuvTerminEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuvTermin(tuvTermin: TuvTerminEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuvTermine(tuvTermine: List<TuvTerminEntity>)

    @Update
    suspend fun updateTuvTermin(tuvTermin: TuvTerminEntity)

    @Delete
    suspend fun deleteTuvTermin(tuvTermin: TuvTerminEntity)

    @Query("DELETE FROM tuv_termine WHERE fahrzeugId = :fahrzeugId")
    suspend fun deleteTuvTermineByVehicle(fahrzeugId: Int)

    @Query("DELETE FROM tuv_termine")
    suspend fun clearTuvTermine()
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists ORDER BY name")
    suspend fun getAllChecklists(): List<ChecklistEntity>

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistById(id: Int): ChecklistEntity?

    @Query("SELECT * FROM checklists WHERE fahrzeuggruppeId = :fahrzeuggruppeId")
    suspend fun getChecklistsByGroup(fahrzeuggruppeId: Int): List<ChecklistEntity>

    @Query("SELECT * FROM checklists WHERE template = :isTemplate")
    suspend fun getChecklistsByTemplate(isTemplate: Boolean): List<ChecklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklists(checklists: List<ChecklistEntity>)

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Delete
    suspend fun deleteChecklist(checklist: ChecklistEntity)

    @Query("DELETE FROM checklists")
    suspend fun clearChecklists()
}

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE checklisteId = :checklisteId ORDER BY reihenfolge")
    suspend fun getItemsByChecklist(checklisteId: Int): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getItemById(id: Int): ChecklistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE checklisteId = :checklisteId")
    suspend fun deleteItemsByChecklist(checklisteId: Int)

    @Query("DELETE FROM checklist_items")
    suspend fun clearItems()
}

@Dao
interface ChecklistAusfuehrungDao {
    @Query("SELECT * FROM checklist_ausfuehrungen ORDER BY startedAt DESC")
    suspend fun getAllExecutions(): List<ChecklistAusfuehrungEntity>

    @Query("SELECT * FROM checklist_ausfuehrungen WHERE id = :id")
    suspend fun getExecutionById(id: Int): ChecklistAusfuehrungEntity?

    @Query("SELECT * FROM checklist_ausfuehrungen WHERE fahrzeugId = :fahrzeugId ORDER BY startedAt DESC")
    suspend fun getExecutionsByVehicle(fahrzeugId: Int): List<ChecklistAusfuehrungEntity>

    @Query("SELECT * FROM checklist_ausfuehrungen WHERE benutzerId = :benutzerId ORDER BY startedAt DESC")
    suspend fun getExecutionsByUser(benutzerId: Int): List<ChecklistAusfuehrungEntity>

    @Query("SELECT * FROM checklist_ausfuehrungen WHERE status = :status ORDER BY startedAt DESC")
    suspend fun getExecutionsByStatus(status: String): List<ChecklistAusfuehrungEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: ChecklistAusfuehrungEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutions(executions: List<ChecklistAusfuehrungEntity>)

    @Update
    suspend fun updateExecution(execution: ChecklistAusfuehrungEntity)

    @Delete
    suspend fun deleteExecution(execution: ChecklistAusfuehrungEntity)

    @Query("DELETE FROM checklist_ausfuehrungen")
    suspend fun clearExecutions()
}

@Dao
interface ItemErgebnisDao {
    @Query("SELECT * FROM item_ergebnisse WHERE ausfuehrungId = :ausfuehrungId ORDER BY createdAt")
    suspend fun getResultsByExecution(ausfuehrungId: Int): List<ItemErgebnisEntity>

    @Query("SELECT * FROM item_ergebnisse WHERE id = :id")
    suspend fun getResultById(id: Int): ItemErgebnisEntity?

    @Query("SELECT * FROM item_ergebnisse WHERE ausfuehrungId = :ausfuehrungId AND itemId = :itemId")
    suspend fun getResultByExecutionAndItem(ausfuehrungId: Int, itemId: Int): ItemErgebnisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ItemErgebnisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<ItemErgebnisEntity>)

    @Update
    suspend fun updateResult(result: ItemErgebnisEntity)

    @Delete
    suspend fun deleteResult(result: ItemErgebnisEntity)

    @Query("DELETE FROM item_ergebnisse WHERE ausfuehrungId = :ausfuehrungId")
    suspend fun deleteResultsByExecution(ausfuehrungId: Int)

    @Query("DELETE FROM item_ergebnisse")
    suspend fun clearResults()
}
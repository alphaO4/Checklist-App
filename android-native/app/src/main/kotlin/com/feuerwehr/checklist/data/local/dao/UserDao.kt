package com.feuerwehr.checklist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.feuerwehr.checklist.data.local.entity.UserEntity
import com.feuerwehr.checklist.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    // User queries
    @Query("SELECT * FROM benutzer ORDER BY username ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM benutzer WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?
    
    @Query("SELECT * FROM benutzer WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?
    
    @Query("SELECT * FROM benutzer WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM benutzer WHERE rolle = :role ORDER BY username ASC")
    fun getUsersByRoleFlow(role: String): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    // Group queries  
    @Query("SELECT * FROM gruppen ORDER BY name ASC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>
    
    @Query("SELECT * FROM gruppen WHERE id = :id")
    suspend fun getGroupById(id: Int): GroupEntity?
    
    @Query("SELECT * FROM gruppen WHERE gruppenleiterId = :leaderId")
    fun getGroupsByLeaderFlow(leaderId: Int): Flow<List<GroupEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)
    
    @Update
    suspend fun updateGroup(group: GroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}
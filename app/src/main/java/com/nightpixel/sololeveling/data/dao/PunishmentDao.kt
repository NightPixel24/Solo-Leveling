package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.PunishmentAssignment
import com.nightpixel.sololeveling.data.entity.PunishmentPoolItem
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import kotlinx.coroutines.flow.Flow

@Dao
interface PunishmentDao {
    @Query("SELECT * FROM punishment_pool_items ORDER BY createdAt ASC")
    fun observePoolItems(): Flow<List<PunishmentPoolItem>>

    @Insert
    suspend fun insertPoolItem(item: PunishmentPoolItem): Long

    @Delete
    suspend fun deletePoolItem(item: PunishmentPoolItem)

    @Query("SELECT * FROM punishment_pool_items WHERE severity = :severity ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomItem(severity: PunishmentSeverity): PunishmentPoolItem?

    @Query("SELECT * FROM punishment_assignments WHERE resolved = 0 ORDER BY dateAssigned DESC")
    fun observeActiveAssignments(): Flow<List<PunishmentAssignment>>

    /** Ignored on conflict - [PunishmentAssignment.sourceRef] is unique, so re-scanning the same
     * miss is always safe to call again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignment(assignment: PunishmentAssignment)

    @Update
    suspend fun updateAssignment(assignment: PunishmentAssignment)

    @Query("SELECT * FROM punishment_pool_items")
    suspend fun getAllItemsOnce(): List<PunishmentPoolItem>

    @Query("SELECT * FROM punishment_assignments")
    suspend fun getAllAssignmentsOnce(): List<PunishmentAssignment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PunishmentPoolItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<PunishmentAssignment>)

    @Query("DELETE FROM punishment_pool_items")
    suspend fun clearItems()

    @Query("DELETE FROM punishment_assignments")
    suspend fun clearAssignments()

    @Transaction
    suspend fun replaceAll(items: List<PunishmentPoolItem>, assignments: List<PunishmentAssignment>) {
        clearAssignments()
        clearItems()
        insertItems(items)
        insertAssignments(assignments)
    }
}

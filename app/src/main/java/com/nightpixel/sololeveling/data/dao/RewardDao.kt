package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.RewardInventoryItem
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM reward_pool_items ORDER BY createdAt ASC")
    fun observePoolItems(): Flow<List<RewardPoolItem>>

    @Insert
    suspend fun insertPoolItem(item: RewardPoolItem): Long

    @Delete
    suspend fun deletePoolItem(item: RewardPoolItem)

    @Query("SELECT * FROM reward_inventory ORDER BY claimedAt DESC")
    fun observeInventory(): Flow<List<RewardInventoryItem>>

    @Insert
    suspend fun insertInventoryItem(item: RewardInventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: RewardInventoryItem)

    @Query("SELECT * FROM reward_pool_items")
    suspend fun getAllPoolItemsOnce(): List<RewardPoolItem>

    @Query("SELECT * FROM reward_inventory")
    suspend fun getAllInventoryOnce(): List<RewardInventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoolItems(items: List<RewardPoolItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<RewardInventoryItem>)

    @Query("DELETE FROM reward_pool_items")
    suspend fun clearPoolItems()

    @Query("DELETE FROM reward_inventory")
    suspend fun clearInventory()

    @Transaction
    suspend fun replaceAll(poolItems: List<RewardPoolItem>, inventory: List<RewardInventoryItem>) {
        clearInventory()
        clearPoolItems()
        insertPoolItems(poolItems)
        insertInventoryItems(inventory)
    }
}

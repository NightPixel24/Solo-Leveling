package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.GoldBalance
import com.nightpixel.sololeveling.data.entity.GoldTransaction
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import com.nightpixel.sololeveling.data.entity.RewardTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM gold_balance WHERE id = 0")
    fun observeBalance(): Flow<GoldBalance?>

    @Query("SELECT * FROM gold_balance WHERE id = 0")
    suspend fun getBalanceOnce(): GoldBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBalance(balance: GoldBalance)

    @Insert
    suspend fun insertTransaction(transaction: GoldTransaction)

    @Query("SELECT * FROM gold_transactions ORDER BY timestamp DESC")
    fun observeTransactions(): Flow<List<GoldTransaction>>

    @Query("SELECT * FROM reward_pool_items ORDER BY createdAt ASC")
    fun observePoolItems(): Flow<List<RewardPoolItem>>

    @Insert
    suspend fun insertPoolItem(item: RewardPoolItem): Long

    @Delete
    suspend fun deletePoolItem(item: RewardPoolItem)

    /** [OnConflictStrategy.REPLACE] against [RewardTarget]'s unique (pool, periodStart) index -
     * picking a new target for a period you've already picked one for replaces it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: RewardTarget)

    @Update
    suspend fun updateTarget(target: RewardTarget)

    @Query("SELECT * FROM reward_targets ORDER BY periodStart DESC")
    fun observeTargets(): Flow<List<RewardTarget>>

    @Query("SELECT * FROM gold_transactions")
    suspend fun getAllTransactionsOnce(): List<GoldTransaction>

    @Query("SELECT * FROM reward_pool_items")
    suspend fun getAllPoolItemsOnce(): List<RewardPoolItem>

    @Query("SELECT * FROM reward_targets")
    suspend fun getAllTargetsOnce(): List<RewardTarget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<GoldTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoolItems(items: List<RewardPoolItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<RewardTarget>)

    @Query("DELETE FROM gold_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM reward_pool_items")
    suspend fun clearPoolItems()

    @Query("DELETE FROM reward_targets")
    suspend fun clearTargets()

    @Transaction
    suspend fun replaceAll(
        balance: GoldBalance,
        transactions: List<GoldTransaction>,
        poolItems: List<RewardPoolItem>,
        targets: List<RewardTarget>
    ) {
        clearTargets()
        clearPoolItems()
        clearTransactions()
        upsertBalance(balance)
        insertPoolItems(poolItems)
        insertTransactions(transactions)
        insertTargets(targets)
    }
}

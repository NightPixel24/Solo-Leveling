package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.PlayerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 0")
    fun observe(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 0")
    suspend fun getOnce(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: PlayerProfile)

    @Query("DELETE FROM player_profile")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(profile: PlayerProfile?) {
        clear()
        upsert(profile ?: PlayerProfile())
    }
}

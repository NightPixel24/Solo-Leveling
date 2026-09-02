package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.RestDayNote
import kotlinx.coroutines.flow.Flow

@Dao
interface RestDayNoteDao {
    @Query("SELECT * FROM rest_day_notes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<RestDayNote>>

    @Insert
    suspend fun insert(note: RestDayNote): Long

    @Update
    suspend fun update(note: RestDayNote)

    @Delete
    suspend fun delete(note: RestDayNote)

    @Query("SELECT * FROM rest_day_notes")
    suspend fun getAllOnce(): List<RestDayNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<RestDayNote>)

    @Query("DELETE FROM rest_day_notes")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(notes: List<RestDayNote>) {
        clear()
        insertAll(notes)
    }
}

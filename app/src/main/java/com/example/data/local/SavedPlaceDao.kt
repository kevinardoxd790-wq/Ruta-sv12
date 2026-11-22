package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY isFavorite DESC, lastUsedAt DESC")
    fun getAllPlaces(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlaceEntity): Long

    @Update
    suspend fun updatePlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE saved_places SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markUsed(id: Int, timestamp: Long = System.currentTimeMillis())
}

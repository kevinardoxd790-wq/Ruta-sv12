package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflinePlaceDao {

    @Query("SELECT COUNT(*) FROM offline_places_fts")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(places: List<OfflinePlaceFtsEntity>)

    @Query("DELETE FROM offline_places_fts")
    suspend fun clearAll()

    @Query(
        """
        SELECT * FROM offline_places_fts
        WHERE normalizedName MATCH :ftsQuery
        ORDER BY rankPriority DESC
        LIMIT :limit
        """
    )
    suspend fun searchFts(ftsQuery: String, limit: Int = 80): List<OfflinePlaceFtsEntity>
}

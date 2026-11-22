package com.example.data.local

import kotlinx.coroutines.flow.Flow

class SavedPlaceRepository(private val dao: SavedPlaceDao) {
    val allPlaces: Flow<List<SavedPlaceEntity>> = dao.getAllPlaces()

    suspend fun savePlace(
        name: String,
        notes: String,
        category: String,
        latitude: Double,
        longitude: Double,
        isFavorite: Boolean = true
    ): Long {
        return dao.insertPlace(
            SavedPlaceEntity(
                name = name,
                notes = notes,
                category = category,
                latitude = latitude,
                longitude = longitude,
                isFavorite = isFavorite
            )
        )
    }

    suspend fun updatePlace(place: SavedPlaceEntity) = dao.updatePlace(place)

    suspend fun deleteById(id: Int) = dao.deleteById(id)

    suspend fun markUsed(id: Int) = dao.markUsed(id)
}

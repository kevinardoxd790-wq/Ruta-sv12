package com.example.data.local

import com.example.location.OfflineLocationTracker

data class OfflinePlaceMatch(
    val name: String,
    val category: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val rankPriority: Int,
    val distanceMeters: Double?
)

class OfflinePlaceRepository(private val dao: OfflinePlaceDao) {

    suspend fun count(): Int = dao.count()

    suspend fun insertBatch(places: List<OfflinePlaceFtsEntity>) {
        if (places.isNotEmpty()) {
            dao.insertAll(places)
        }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun search(
        rawQuery: String,
        userLat: Double?,
        userLon: Double?,
        maxResults: Int = 25
    ): List<OfflinePlaceMatch> {
        val ftsQuery = OfflinePlaceFtsEntity.buildFtsPrefixQuery(rawQuery) ?: return emptyList()
        val normalizedQuery = OfflinePlaceFtsEntity.normalizeText(rawQuery)
        val candidates = dao.searchFts(ftsQuery, limit = 100)
        if (candidates.isEmpty()) return emptyList()

        return candidates
            .map { entity ->
                val dist = if (userLat != null && userLon != null) {
                    OfflineLocationTracker.haversineMeters(
                        userLat, userLon, entity.latitude, entity.longitude
                    )
                } else {
                    null
                }
                val exactMatch = entity.normalizedName == normalizedQuery
                val startsWithMatch = entity.normalizedName.startsWith(normalizedQuery)
                val relevanceScore =
                    (if (exactMatch) 10_000.0 else if (startsWithMatch) 4_000.0 else 0.0) +
                        (entity.rankPriority * 900.0) -
                        ((dist ?: 0.0) / 1000.0 * 12.0)

                relevanceScore to OfflinePlaceMatch(
                    name = entity.name,
                    category = entity.category,
                    subtitle = entity.subtitle,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    rankPriority = entity.rankPriority,
                    distanceMeters = dist
                )
            }
            .sortedByDescending { it.first }
            .distinctBy { "${OfflinePlaceFtsEntity.normalizeText(it.second.name)}_${(it.second.latitude * 200).toInt()}_${(it.second.longitude * 200).toInt()}" }
            .take(maxResults)
            .map { it.second }
    }
}

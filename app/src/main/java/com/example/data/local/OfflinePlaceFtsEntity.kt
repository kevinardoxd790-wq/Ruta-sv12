package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4
import java.text.Normalizer

@Fts4
@Entity(tableName = "offline_places_fts")
data class OfflinePlaceFtsEntity(
    val name: String,
    val normalizedName: String,
    val category: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val rankPriority: Int
) {
    companion object {
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val NON_ALNUM_REGEX = Regex("[^a-z0-9]+")

        fun normalizeText(raw: String): String {
            val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            val withoutAccents = DIACRITICS_REGEX.replace(decomposed, "")
            return withoutAccents.lowercase().replace(NON_ALNUM_REGEX, " ").trim()
        }

        fun buildFtsPrefixQuery(rawQuery: String): String? {
            val normalized = normalizeText(rawQuery)
            if (normalized.length < 2) return null
            val tokens = normalized.split(' ')
                .filter { it.isNotBlank() }
                .take(5)
            if (tokens.isEmpty()) return null
            return tokens.joinToString(" ") { "$it*" }
        }
    }
}

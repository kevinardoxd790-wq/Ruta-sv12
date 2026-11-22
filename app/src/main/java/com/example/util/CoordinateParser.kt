package com.example.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.abs

sealed class CoordinateParseResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val label: String?,
        val isInsideElSalvador: Boolean
    ) : CoordinateParseResult()

    data class ShortenedLinkWarning(
        val url: String
    ) : CoordinateParseResult()

    data object Empty : CoordinateParseResult()
    data object NoMatch : CoordinateParseResult()
}

object CoordinateParser {
    // El Salvador bounding box (with generous coastal/border margin)
    private const val SV_MIN_LAT = 12.95
    private const val SV_MAX_LAT = 14.55
    private const val SV_MIN_LON = -90.25
    private const val SV_MAX_LON = -87.55

    private val shortenedUrlRegex = Regex(
        """https?://(?:maps\.app\.goo\.gl|goo\.gl/maps|g\.co/maps|bit\.ly|tinyurl\.com)/\S+""",
        RegexOption.IGNORE_CASE
    )

    // Matches !3d13.6929!4d-89.2182 (Google Maps place URLs)
    private val googleDataParamRegex = Regex(
        """!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)"""
    )

    // Matches @13.6929,-89.2182 or q=13.6929,-89.2182 or ll=13.6929,-89.2182 or query=13.6929,-89.2182 or geo:13.6929,-89.2182
    private val decimalPairRegex = Regex(
        """(-?\d{1,2}(?:\.\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:\.\d+)?)"""
    )

    // Matches OpenStreetMap #map=16/13.6929/-89.2182
    private val osmHashRegex = Regex(
        """#map=\d+/(-?\d{1,2}\.\d+)/(-?\d{1,3}\.\d+)"""
    )

    // Matches mlat=13.6929&mlon=-89.2182
    private val mlatMlonRegex = Regex(
        """mlat=(-?\d{1,2}\.\d+)&mlon=(-?\d{1,3}\.\d+)""",
        RegexOption.IGNORE_CASE
    )

    // Matches DMS like 13°41'34.4"N 89°13'05.5"W
    private val dmsRegex = Regex(
        """(\d{1,2})[°º]\s*(\d{1,2})['′]\s*(\d{1,2}(?:\.\d+)?)["″]?\s*([NS])\s*[,;\s]*(\d{1,3})[°º]\s*(\d{1,2})['′]\s*(\d{1,2}(?:\.\d+)?)["″]?\s*([EWWO])""",
        RegexOption.IGNORE_CASE
    )

    private val geoLabelRegex = Regex("""\(([^)]+)\)""")

    fun isInsideElSalvador(lat: Double, lon: Double): Boolean {
        return lat in SV_MIN_LAT..SV_MAX_LAT && lon in SV_MIN_LON..SV_MAX_LON
    }

    fun formatCoordinates(lat: Double, lon: Double): String {
        return String.format(Locale.US, "%.5f, %.5f", lat, lon)
    }

    fun parse(rawInput: String?): CoordinateParseResult {
        val text = rawInput?.trim().orEmpty()
        if (text.isEmpty()) return CoordinateParseResult.Empty

        val decoded = runCatching {
            URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        }.getOrDefault(text)

        val extractedLabel = extractLabel(decoded)

        // 1. OSM mlat/mlon
        mlatMlonRegex.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lon = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 2. OSM #map=zoom/lat/lon
        osmHashRegex.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lon = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 3. Google Maps !3dLAT!4dLON
        googleDataParamRegex.find(decoded)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lon = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 4. DMS format (13°41'34.4"N 89°13'05.5"W)
        dmsRegex.find(decoded)?.let { m ->
            val latD = m.groupValues[1].toDouble()
            val latM = m.groupValues[2].toDouble()
            val latS = m.groupValues[3].toDouble()
            val latH = m.groupValues[4].uppercase(Locale.US)
            val lonD = m.groupValues[5].toDouble()
            val lonM = m.groupValues[6].toDouble()
            val lonS = m.groupValues[7].toDouble()
            val lonH = m.groupValues[8].uppercase(Locale.US)

            var lat = latD + latM / 60.0 + latS / 3600.0
            if (latH == "S") lat = -lat
            var lon = lonD + lonM / 60.0 + lonS / 3600.0
            if (lonH == "W" || lonH == "O") lon = -lon

            if (isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 5. Decimal coordinates anywhere in text/URL (skipping geo:0,0 placeholder)
        val candidates = decimalPairRegex.findAll(decoded).toList()
        for (match in candidates) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            if (abs(lat) < 0.001 && abs(lon) < 0.001) continue // ignore geo:0,0?q=...
            if (isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 6. Shortened link without coordinates embedded
        shortenedUrlRegex.find(text)?.let { m ->
            return CoordinateParseResult.ShortenedLinkWarning(m.value)
        }

        return CoordinateParseResult.NoMatch
    }

    private fun extractLabel(decoded: String): String? {
        val parenMatch = geoLabelRegex.find(decoded)
        val candidate = parenMatch?.groupValues?.getOrNull(1)?.trim()
        return candidate?.takeIf { it.isNotBlank() && it.length <= 60 }
    }

    private fun isValidLatLon(lat: Double, lon: Double): Boolean {
        return lat in -90.0..90.0 && lon in -180.0..180.0 &&
            !(abs(lat) < 0.0001 && abs(lon) < 0.0001)
    }

    private fun buildSuccess(lat: Double, lon: Double, label: String?): CoordinateParseResult.Success {
        return CoordinateParseResult.Success(
            latitude = lat,
            longitude = lon,
            label = label,
            isInsideElSalvador = isInsideElSalvador(lat, lon)
        )
    }
}

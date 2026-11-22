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

    // Matches !3d13.6929!4d-89.2182 (Google Maps exact pin data param)
    private val googleDataParamRegex = Regex(
        """!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)"""
    )

    // Matches explicit Google Maps / Geo query parameters (higher priority than viewport @lat,lon)
    private val googleExplicitParamRegex = Regex(
        """(?:[?&](?:q|query|daddr|destination|ll|center|sll|pb)=|(?:^|\s|/)loc:|google\.navigation:q=)(?:loc:)?(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches Google Maps /place/Some+Place+Name/
    private val googlePlacePathRegex = Regex(
        """/maps/place/([^/@?]+)(?:/|@|\?|$)""",
        RegexOption.IGNORE_CASE
    )

    // Matches Google Maps /dir/origin/destination/
    private val googleDirUrlRegex = Regex(
        """/maps/dir/""",
        RegexOption.IGNORE_CASE
    )

    // Matches @13.6929,-89.2182 or plain decimal coordinate pair
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

    fun extractGoogleMapsPlaceQuery(rawInput: String?): String? {
        val text = rawInput?.trim().orEmpty()
        if (!text.contains("google.", ignoreCase = true) && !text.contains("maps", ignoreCase = true)) {
            return null
        }
        val decoded = runCatching {
            URLDecoder.decode(text, StandardCharsets.UTF_8.name())
        }.getOrDefault(text)

        googlePlacePathRegex.find(decoded)?.groupValues?.getOrNull(1)?.let { name ->
            val clean = name.replace('+', ' ').trim()
            if (clean.isNotBlank() && decimalPairRegex.find(clean) == null && dmsRegex.find(clean) == null) {
                return clean
            }
        }
        val qMatch = Regex("""[?&](?:q|query|destination)=([^&#]+)""", RegexOption.IGNORE_CASE).find(decoded)
        val qVal = qMatch?.groupValues?.getOrNull(1)?.replace('+', ' ')?.trim()
        if (!qVal.isNullOrBlank() && decimalPairRegex.find(qVal) == null && dmsRegex.find(qVal) == null) {
            return qVal
        }
        return null
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

        // 3. Google Maps !3dLAT!4dLON (take last match in case URL is /dir/A/B/)
        googleDataParamRegex.findAll(decoded).lastOrNull()?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lon = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 4. Explicit Google Maps query params (?q=, ?daddr=, ?destination=, ?ll=, ?center=, google.navigation:q=)
        googleExplicitParamRegex.findAll(decoded).forEach { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lon = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 5. DMS format (13°41'34.4"N 89°13'05.5"W)
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

        // 6. Decimal coordinates anywhere in text/URL (skipping geo:0,0 placeholder)
        val candidates = decimalPairRegex.findAll(decoded).toList()
        val orderedCandidates = if (googleDirUrlRegex.containsMatchIn(decoded) && candidates.size > 1) {
            // In /maps/dir/A/B/@viewport, prefer non-@ coordinate pairs, taking the destination
            val withoutViewport = candidates.filter { match ->
                val start = match.range.first
                start == 0 || decoded[start - 1] != '@'
            }
            if (withoutViewport.isNotEmpty()) withoutViewport.reversed() else candidates
        } else {
            candidates
        }
        for (match in orderedCandidates) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            if (abs(lat) < 0.001 && abs(lon) < 0.001) continue // ignore geo:0,0?q=...
            if (isValidLatLon(lat, lon)) {
                return buildSuccess(lat, lon, extractedLabel)
            }
        }

        // 7. Shortened link without coordinates embedded
        shortenedUrlRegex.find(text)?.let { m ->
            return CoordinateParseResult.ShortenedLinkWarning(m.value)
        }

        return CoordinateParseResult.NoMatch
    }

    private fun extractLabel(decoded: String): String? {
        val parenMatch = geoLabelRegex.find(decoded)
        val parenCandidate = parenMatch?.groupValues?.getOrNull(1)?.trim()
        if (!parenCandidate.isNullOrBlank() && parenCandidate.length <= 60) {
            return parenCandidate
        }
        val placeMatch = googlePlacePathRegex.find(decoded)?.groupValues?.getOrNull(1)
        val placeClean = placeMatch?.replace('+', ' ')?.trim()
        if (!placeClean.isNullOrBlank() &&
            placeClean.length <= 60 &&
            decimalPairRegex.find(placeClean) == null &&
            dmsRegex.find(placeClean) == null
        ) {
            return placeClean
        }
        return null
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

package com.example.util

import com.example.data.ElSalvadorPlaces
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

object GoogleMapsLinkResolver {

    /**
     * Parses any coordinate, geo:, WhatsApp text, or Google Maps URL offline first.
     * If it is a shortened Google Maps URL (maps.app.goo.gl, goo.gl/maps) without embedded coordinates,
     * attempts a brief redirect expansion if the phone currently has connectivity, and falls back to
     * offline El Salvador place matching or a clear offline hint if there is no signal.
     */
    suspend fun resolve(rawInput: String?): CoordinateParseResult {
        val initial = CoordinateParser.parse(rawInput)
        if (initial is CoordinateParseResult.Success) return initial

        if (initial is CoordinateParseResult.ShortenedLinkWarning) {
            val expanded = expandShortUrl(initial.url)
            if (expanded != null) {
                val parsedExpanded = CoordinateParser.parse(expanded)
                if (parsedExpanded is CoordinateParseResult.Success) {
                    return parsedExpanded
                }
                val placeFromExpanded = matchOfflineGazetteer(expanded)
                if (placeFromExpanded != null) return placeFromExpanded
            }
            return initial
        }

        val placeMatch = matchOfflineGazetteer(rawInput)
        if (placeMatch != null) return placeMatch

        return initial
    }

    private fun matchOfflineGazetteer(rawInput: String?): CoordinateParseResult.Success? {
        val query = CoordinateParser.extractGoogleMapsPlaceQuery(rawInput) ?: return null
        val match = ElSalvadorPlaces.search(query).firstOrNull() ?: return null
        return CoordinateParseResult.Success(
            latitude = match.latitude,
            longitude = match.longitude,
            label = match.name,
            isInsideElSalvador = true
        )
    }

    private suspend fun expandShortUrl(shortUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            var currentUrl = shortUrl
            repeat(5) {
                val conn = (URI.create(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "HEAD"
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) RutaSV/1.0")
                }
                try {
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location") ?: return@runCatching currentUrl
                        val resolved = URI.create(currentUrl).resolve(location).toString()
                        currentUrl = resolved
                        if (CoordinateParser.parse(currentUrl) is CoordinateParseResult.Success) {
                            return@runCatching currentUrl
                        }
                    } else {
                        return@runCatching currentUrl
                    }
                } finally {
                    conn.disconnect()
                }
            }
            currentUrl
        }.getOrNull()
    }
}

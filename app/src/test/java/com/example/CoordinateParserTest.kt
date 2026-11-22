package com.example

import com.example.util.CoordinateParseResult
import com.example.util.CoordinateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun parsesPlainDecimalCoordinatesInElSalvador() {
        val result = CoordinateParser.parse("13.6929, -89.2182")
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.6929, success.latitude, 0.0001)
        assertEquals(-89.2182, success.longitude, 0.0001)
        assertTrue(success.isInsideElSalvador)
    }

    @Test
    fun parsesWhatsAppMessageWithGoogleMapsUrl() {
        val msg = "Hola! Te espero en la entrada de la finca: https://maps.google.com/?q=13.8414,-89.7456"
        val result = CoordinateParser.parse(msg)
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.8414, success.latitude, 0.0001)
        assertEquals(-89.7456, success.longitude, 0.0001)
        assertTrue(success.isInsideElSalvador)
    }

    @Test
    fun parsesGeoUriWithZeroPlaceholderAndLabel() {
        val uri = "geo:0,0?q=13.4834,-88.1833(Casa%20de%20Carlos)"
        val result = CoordinateParser.parse(uri)
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.4834, success.latitude, 0.0001)
        assertEquals(-88.1833, success.longitude, 0.0001)
        assertEquals("Casa de Carlos", success.label)
        assertTrue(success.isInsideElSalvador)
    }

    @Test
    fun parsesDmsCoordinates() {
        val dms = """13°41'34.4"N 89°13'05.5"W"""
        val result = CoordinateParser.parse(dms)
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.6929, success.latitude, 0.001)
        assertEquals(-89.2182, success.longitude, 0.001)
        assertTrue(success.isInsideElSalvador)
    }

    @Test
    fun detectsShortenedGoogleLinkWarning() {
        val result = CoordinateParser.parse("Te mando el pin: https://maps.app.goo.gl/AbCdEf123")
        assertTrue(result is CoordinateParseResult.ShortenedLinkWarning)
    }

    @Test
    fun flagsCoordinatesOutsideElSalvador() {
        val result = CoordinateParser.parse("14.6349, -90.5069") // Guatemala City
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertFalse(success.isInsideElSalvador)
    }

    @Test
    fun parsesGoogleMapsPlaceUrlWithPinAndName() {
        val url = "https://www.google.com/maps/place/Colonia+Escalon/@13.7012,-89.2400,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d13.7045!4d-89.2421!16s"
        val result = CoordinateParser.parse(url)
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.7045, success.latitude, 0.0001)
        assertEquals(-89.2421, success.longitude, 0.0001)
        assertEquals("Colonia Escalon", success.label)
    }

    @Test
    fun parsesGoogleNavigationUri() {
        val uri = "google.navigation:q=13.4921,-89.3812"
        val result = CoordinateParser.parse(uri)
        assertTrue(result is CoordinateParseResult.Success)
        val success = result as CoordinateParseResult.Success
        assertEquals(13.4921, success.latitude, 0.0001)
        assertEquals(-89.3812, success.longitude, 0.0001)
    }
}

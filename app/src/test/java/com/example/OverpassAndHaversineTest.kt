package com.example

import com.example.data.util.OverpassParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OverpassAndHaversineTest {

  @Test
  fun testHaversineDistanceBogotaToMedellin() {
    // Bogota (~4.6097, -74.0817) to Medellin (~6.2442, -75.5812) is ~240 km
    val distKm = OverpassParser.calculateHaversineKm(4.6097, -74.0817, 6.2442, -75.5812)
    assertTrue("Distance should be approx 240km, got $distKm", distKm in 220.0..260.0)
  }

  @Test
  fun testDistanceFormattingMetersAndKm() {
    assertEquals("500 m", OverpassParser.formatDistanceText(0.5))
    assertEquals("3.2 km", OverpassParser.formatDistanceText(3.24))
  }

  @Test
  fun testOverpassJsonParsing_nodeWithDirectLatLon() {
    val sampleJson = """
      {
        "elements": [
          {
            "type": "node",
            "id": 998877,
            "lat": 4.6853,
            "lon": -74.0531,
            "tags": {
              "name": "Clínica Veterinaria Prueba",
              "amenity": "veterinary",
              "addr:street": "Carrera 15",
              "addr:housenumber": "98-42",
              "addr:city": "Bogotá",
              "phone": "+57 601 555 1234",
              "emergency": "yes",
              "opening_hours": "24/7"
            }
          }
        ]
      }
    """.trimIndent()

    val parsed = OverpassParser.parseOverpassJson(sampleJson, userLat = 4.6800, userLng = -74.0500)
    assertEquals(1, parsed.size)
    val vet = parsed.first()
    assertEquals("998877", vet.id)
    assertEquals("Clínica Veterinaria Prueba", vet.name)
    assertEquals("Carrera 15 #98-42, Bogotá", vet.address)
    assertEquals("+57 601 555 1234", vet.phone)
    assertTrue(vet.is24hEmergency)
    assertEquals("https://www.openstreetmap.org/node/998877", vet.osmSourceUrl)
  }

  @Test
  fun testOverpassJsonParsing_wayWithCenterCoordinates() {
    val sampleWayJson = """
      {
        "elements": [
          {
            "type": "way",
            "id": 123456789,
            "center": {
              "lat": 6.2442,
              "lon": -75.5812
            },
            "tags": {
              "name": "Hospital Veterinario Metropolitano",
              "amenity": "veterinary",
              "addr:street": "Avenida El Poblado",
              "addr:housenumber": "10-25",
              "addr:city": "Medellín",
              "contact:phone": "+57 604 444 9999",
              "opening_hours": "24/7"
            }
          }
        ]
      }
    """.trimIndent()

    val parsed = OverpassParser.parseOverpassJson(sampleWayJson, userLat = 6.2400, userLng = -75.5800)
    assertEquals(1, parsed.size)
    val vet = parsed.first()
    assertEquals("123456789", vet.id)
    assertEquals("Hospital Veterinario Metropolitano", vet.name)
    assertEquals(6.2442, vet.latitude, 0.0001)
    assertEquals(-75.5812, vet.longitude, 0.0001)
    assertEquals("Avenida El Poblado #10-25, Medellín", vet.address)
    assertEquals("+57 604 444 9999", vet.phone)
    assertTrue(vet.is24hEmergency)
    assertEquals("https://www.openstreetmap.org/way/123456789", vet.osmSourceUrl)
  }

  @Test
  fun testOverpassJsonParsing_missingAddressAndPhoneDefaults() {
    val sampleNoDataJson = """
      {
        "elements": [
          {
            "type": "node",
            "id": 555111,
            "lat": 3.4516,
            "lon": -76.5320,
            "tags": {
              "name": "Veterinaria Cali Centro",
              "amenity": "veterinary"
            }
          }
        ]
      }
    """.trimIndent()

    val parsed = OverpassParser.parseOverpassJson(sampleNoDataJson)
    assertEquals(1, parsed.size)
    val vet = parsed.first()
    assertEquals("Dirección no registrada en OSM", vet.address)
    assertEquals("Sin teléfono registrado", vet.phone)
    assertFalse(vet.is24hEmergency)
  }

  @Test
  fun testPhotonGeoJsonCoordinatesParsing() {
    val photonSample = """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "geometry": {
              "coordinates": [-74.08175, 4.60971],
              "type": "Point"
            },
            "properties": {
              "name": "Bogotá",
              "country": "Colombia"
            }
          }
        ]
      }
    """.trimIndent()

    val root = org.json.JSONObject(photonSample)
    val features = root.optJSONArray("features")
    val feat = features!!.getJSONObject(0)
    val coords = feat.getJSONObject("geometry").getJSONArray("coordinates")
    val lon = coords.getDouble(0)
    val lat = coords.getDouble(1)

    assertEquals(-74.08175, lon, 0.0001)
    assertEquals(4.60971, lat, 0.0001)
  }
}

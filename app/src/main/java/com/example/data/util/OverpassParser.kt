package com.example.data.util

import com.example.domain.model.VetClinic
import org.json.JSONObject
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object OverpassParser {

  /**
   * Calculates Haversine distance in kilometers between two coordinates.
   */
  fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return radiusKm * c
  }

  fun formatDistanceText(distanceKm: Double): String {
    return if (distanceKm >= 1.0) {
      String.format(Locale.US, "%.1f km", distanceKm)
    } else {
      "${(distanceKm * 1000).toInt()} m"
    }
  }

  /**
   * Parses Overpass API JSON response string into a list of [VetClinic] items.
   * Supports both 'node' and 'way' elements (reading center.lat/lon for ways).
   */
  fun parseOverpassJson(jsonString: String, userLat: Double? = null, userLng: Double? = null): List<VetClinic> {
    val clinics = mutableListOf<VetClinic>()
    try {
      val root = JSONObject(jsonString)
      val elements = root.optJSONArray("elements") ?: return emptyList()

      for (i in 0 until elements.length()) {
        val elem = elements.optJSONObject(i) ?: continue
        val type = elem.optString("type", "node")
        val id = elem.optLong("id", 0L).toString()

        // For ways with center tags, Overpass returns coordinates in "center": {"lat": ..., "lon": ...}
        val centerObj = elem.optJSONObject("center")
        val lat = if (centerObj != null && centerObj.has("lat")) {
          centerObj.optDouble("lat", 0.0)
        } else {
          elem.optDouble("lat", 0.0)
        }
        val lon = if (centerObj != null && centerObj.has("lon")) {
          centerObj.optDouble("lon", 0.0)
        } else {
          elem.optDouble("lon", 0.0)
        }
        if (lat == 0.0 && lon == 0.0) continue

        val tags = elem.optJSONObject("tags") ?: JSONObject()
        val name = tags.optString("name").trim().ifBlank {
          tags.optString("official_name").trim().ifBlank { "Clínica Veterinaria" }
        }

        // Address formatting: addr:street + addr:housenumber + addr:city, or "Dirección no registrada en OSM"
        val street = tags.optString("addr:street").trim()
        val housenumber = tags.optString("addr:housenumber").trim()
        val city = tags.optString("addr:city").trim()
        val streetAndNumber = when {
          street.isNotBlank() && housenumber.isNotBlank() -> "$street #$housenumber"
          street.isNotBlank() -> street
          housenumber.isNotBlank() -> housenumber
          else -> null
        }
        val address = listOfNotNull(streetAndNumber, city.ifBlank { null })
          .joinToString(", ")
          .ifBlank { "Dirección no registrada en OSM" }

        // Phone: phone or contact:phone or "Sin teléfono registrado"
        val phone = tags.optString("phone").trim().ifBlank {
          tags.optString("contact:phone").trim()
        }.ifBlank { "Sin teléfono registrado" }

        // 24H verification: emergency=yes OR opening_hours=24/7
        val emergencyTag = tags.optString("emergency").trim().lowercase()
        val openingHoursTag = tags.optString("opening_hours").trim().lowercase()
        val isVerifiedEmergency = emergencyTag == "yes" || openingHoursTag.contains("24/7")

        val rawOpeningHours = tags.optString("opening_hours").trim()
        val openingHours = rawOpeningHours.ifBlank {
          if (isVerifiedEmergency) "24/7 Verificado" else "Horario no especificado"
        }

        val servicesList = mutableListOf<String>()
        if (isVerifiedEmergency) servicesList.add("Urgencias 24h")
        servicesList.add("Consulta Veterinaria")
        if (tags.has("vaccination")) servicesList.add("Vacunación")

        val osmUrl = "https://www.openstreetmap.org/$type/$id"

        clinics.add(
          VetClinic(
            id = id,
            name = name,
            address = address,
            phone = phone,
            latitude = lat,
            longitude = lon,
            is24hEmergency = isVerifiedEmergency,
            openingHours = openingHours,
            services = servicesList,
            osmSourceUrl = osmUrl
          )
        )
      }
    } catch (_: Exception) {}

    return if (userLat != null && userLng != null) {
      clinics.sortedBy { calculateHaversineKm(userLat, userLng, it.latitude, it.longitude) }
    } else {
      clinics
    }
  }
}

package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.util.NominatimClient
import com.example.data.util.OverpassParser
import com.example.domain.model.VetClinic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Technical exception holding user-facing guidance and real low-level technical cause.
 */
class OsmTechnicalException(
  val userMessage: String,
  val technicalDetail: String,
  cause: Throwable? = null
) : Exception("$userMessage ($technicalDetail)", cause)

data class VetSearchResult(
  val clinics: List<VetClinic>,
  val centerLat: Double?,
  val centerLng: Double?,
  val isCitySearch: Boolean,
  val searchCityName: String? = null
)

class VetClinicRepository(private val context: Context) {

  companion object {
    private const val USER_AGENT = "DogCare/1.0 (com.aistudio.dogcare.kprmdx)"
    private const val ACCEPT_LANGUAGE = "es"
    private const val TIMEOUT_MS = 6000

    private val OVERPASS_ENDPOINTS = listOf(
      "https://overpass-api.de/api/interpreter",
      "https://overpass.kumi.systems/api/interpreter",
      "https://overpass.private.coffee/api/interpreter"
    )
  }

  /**
   * Queries real OpenStreetMap veterinary clinics using the Overpass API via POST.
   * Tests endpoints sequentially in order:
   * 1. overpass-api.de
   * 2. overpass.kumi.systems
   * 3. overpass.private.coffee
   * If Overpass servers fail or timeout, falls back to direct OpenStreetMap Nominatim query.
   */
  suspend fun fetchOverpassClinics(
    lat: Double,
    lon: Double,
    radiusMeters: Int = 5000
  ): Result<List<VetClinic>> = withContext(Dispatchers.IO) {
    val query = "[out:json][timeout:25];(node[amenity=veterinary](around:$radiusMeters,$lat,$lon);way[amenity=veterinary](around:$radiusMeters,$lat,$lon););out center tags;"
    val errorDetails = mutableListOf<String>()
    var lastException: Exception? = null

    for (endpoint in OVERPASS_ENDPOINTS) {
      val host = try { URL(endpoint).host } catch (_: Exception) { endpoint }
      var connection: HttpURLConnection? = null
      try {
        val postBody = "data=" + URLEncoder.encode(query, "UTF-8")
        val postBytes = postBody.toByteArray(Charsets.UTF_8)

        val url = URL(endpoint)
        connection = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "POST"
          doOutput = true
          setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
          setRequestProperty("Content-Length", postBytes.size.toString())
          setRequestProperty("User-Agent", USER_AGENT)
          setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
          connectTimeout = TIMEOUT_MS
          readTimeout = TIMEOUT_MS
        }

        connection.outputStream.use { os ->
          os.write(postBytes)
          os.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
          val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
          val parsed = OverpassParser.parseOverpassJson(jsonText, userLat = lat, userLng = lon)
          if (parsed.isNotEmpty()) {
            return@withContext Result.success(parsed)
          }
        } else {
          val errReason = "HTTP $responseCode en $host"
          errorDetails.add(errReason)
          Log.e("DogCareMap", "Overpass error: $errReason")
          lastException = Exception(errReason)
        }
      } catch (e: Exception) {
        val errReason = "${e.javaClass.simpleName} en $host: ${e.message ?: "sin detalle"}"
        errorDetails.add(errReason)
        Log.e("DogCareMap", "Fallo al conectar con Overpass ($host): ${e.message}", e)
        lastException = e
      } finally {
        connection?.disconnect()
      }
    }

    // Fallback: Real OpenStreetMap query via official OSM Nominatim search around coordinates
    val osmFallbackClinics = fetchNominatimOsmClinics(lat, lon, radiusMeters)
    if (osmFallbackClinics.isNotEmpty()) {
      Log.i("DogCareMap", "Obtenidas ${osmFallbackClinics.size} veterinarias reales vía OpenStreetMap Nominatim.")
      return@withContext Result.success(osmFallbackClinics)
    }

    val combinedTechDetail = errorDetails.joinToString("; ")
    Log.e("DogCareMap", "Todos los servidores Overpass y OSM fallaron: $combinedTechDetail", lastException)

    Result.failure(
      OsmTechnicalException(
        userMessage = "No se pudo consultar OpenStreetMap. Revisa tu conexión e intenta de nuevo",
        technicalDetail = combinedTechDetail.ifBlank { "Timeout o fallo de conexión general" },
        cause = lastException
      )
    )
  }

  /**
   * Queries genuine OpenStreetMap veterinary clinics via Nominatim bounded search
   * around the given coordinates, extracting real OSM node IDs, names, addresses, and tags.
   */
  private fun fetchNominatimOsmClinics(
    lat: Double,
    lon: Double,
    radiusMeters: Int
  ): List<VetClinic> {
    try {
      val deltaDeg = (radiusMeters / 111000.0).coerceAtLeast(0.04)
      val minLon = lon - deltaDeg
      val maxLon = lon + deltaDeg
      val minLat = lat - deltaDeg
      val maxLat = lat + deltaDeg

      val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=veterinaria&viewbox=$minLon,$maxLat,$maxLon,$minLat&bounded=1&limit=30&addressdetails=1&extratags=1"
      val url = URL(urlString)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("User-Agent", USER_AGENT)
        setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
        connectTimeout = 8000
        readTimeout = 8000
      }

      val code = conn.responseCode
      if (code == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val array = org.json.JSONArray(text)
        val list = mutableListOf<VetClinic>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val vLat = obj.optDouble("lat", 0.0)
          val vLon = obj.optDouble("lon", 0.0)
          if (vLat == 0.0 && vLon == 0.0) continue

          val rawName = obj.optString("name").trim()
          val name = if (rawName.isBlank() || rawName.equals("Veterinaria", ignoreCase = true)) {
            val addr = obj.optJSONObject("address")
            val road = addr?.optString("road") ?: ""
            if (road.isNotBlank()) "Clínica Veterinaria $road" else "Centro Veterinario"
          } else {
            rawName
          }

          val addrObj = obj.optJSONObject("address")
          val street = addrObj?.optString("road") ?: ""
          val houseNum = addrObj?.optString("house_number") ?: ""
          val suburb = addrObj?.optString("suburb") ?: addrObj?.optString("neighbourhood") ?: ""
          val city = addrObj?.optString("city") ?: addrObj?.optString("state") ?: ""

          val addressParts = listOf(
            if (houseNum.isNotBlank()) "$street #$houseNum" else street,
            suburb,
            city
          ).filter { it.isNotBlank() }

          val formattedAddress = if (addressParts.isNotEmpty()) {
            addressParts.joinToString(", ")
          } else {
            obj.optString("display_name").ifBlank { "Dirección no registrada en OSM" }
          }

          val extra = obj.optJSONObject("extratags")
          val phone = extra?.optString("phone")?.takeIf { it.isNotBlank() }
            ?: extra?.optString("contact:phone")?.takeIf { it.isNotBlank() }
            ?: "Sin teléfono registrado"

          val isEmergency = extra?.optString("emergency") == "yes" ||
            extra?.optString("opening_hours") == "24/7"
          val openingHours = extra?.optString("opening_hours")?.ifBlank { if (isEmergency) "24/7" else "Horario no especificado" } ?: if (isEmergency) "24/7" else "Horario no especificado"
          val osmId = obj.optString("osm_id", "osm_${vLat}_${vLon}")

          list.add(
            VetClinic(
              id = osmId,
              name = name,
              address = formattedAddress,
              phone = phone,
              latitude = vLat,
              longitude = vLon,
              is24hEmergency = isEmergency,
              openingHours = openingHours,
              services = if (isEmergency) listOf("Consultas", "Urgencias 24h", "Cirugía") else listOf("Consultas", "Vacunación", "Medicina General"),
              osmSourceUrl = "https://www.openstreetmap.org/node/$osmId"
            )
          )
        }
        return list
      }
    } catch (e: Exception) {
      Log.e("DogCareMap", "Fallo al consultar OSM Nominatim para veterinarias: ${e.message}", e)
    }
    return emptyList()
  }

  /**
   * Searches clinics based on user coordinates or city name geocoded via Nominatim/Photon.
   * If query is geocoded as a city, does NOT filter results by query text (so all clinics in that city remain).
   */
  suspend fun searchClinics(
    userLat: Double?,
    userLng: Double?,
    query: String = "",
    onlyEmergency: Boolean = false
  ): Result<VetSearchResult> = withContext(Dispatchers.IO) {
    var centerLat = userLat
    var centerLng = userLng
    var isGeocodedCity = false
    var geocodedCityName: String? = null

    // If search query is provided, geocode city with Nominatim / Photon fallback
    if (query.isNotBlank()) {
      val geocodeResult = NominatimClient.geocodeCityWithDetail(query)
      if (geocodeResult.coordinates != null) {
        centerLat = geocodeResult.coordinates.first
        centerLng = geocodeResult.coordinates.second
        isGeocodedCity = true
        geocodedCityName = query.trim()
      } else {
        // Query could not be geocoded as a city
        if (centerLat == null || centerLng == null) {
          val techReason = geocodeResult.technicalDetail ?: "No se encontraron coordenadas para '$query'"
          Log.e("DogCareMap", "Error de geocodificación para '$query': $techReason")
          return@withContext Result.failure(
            OsmTechnicalException(
              userMessage = "No se pudo consultar OpenStreetMap. Revisa tu conexión e intenta de nuevo",
              technicalDetail = "Geocodificación fallida para '$query': $techReason"
            )
          )
        }
      }
    }

    // Without coordinates (no GPS and no city search), return idle empty result
    if (centerLat == null || centerLng == null) {
      return@withContext Result.success(
        VetSearchResult(
          clinics = emptyList(),
          centerLat = null,
          centerLng = null,
          isCitySearch = false
        )
      )
    }

    val fetchResult = fetchOverpassClinics(centerLat, centerLng, 5000)
    if (fetchResult.isFailure) {
      val ex = fetchResult.exceptionOrNull()
      return@withContext Result.failure(
        if (ex is OsmTechnicalException) ex
        else OsmTechnicalException(
          userMessage = "No se pudo consultar OpenStreetMap. Revisa tu conexión e intenta de nuevo",
          technicalDetail = ex?.localizedMessage ?: "Error de red desconocido",
          cause = ex
        )
      )
    }

    var resultList = fetchResult.getOrDefault(emptyList())

    // If query was NOT a geocoded city, filter clinics by text query (e.g. searching specific clinic name nearby)
    if (!isGeocodedCity && query.isNotBlank()) {
      val q = query.trim().lowercase()
      resultList = resultList.filter {
        it.name.lowercase().contains(q) ||
        it.address.lowercase().contains(q) ||
        it.services.any { s -> s.lowercase().contains(q) }
      }
    }

    // Filter by 24h emergency if requested
    if (onlyEmergency) {
      resultList = resultList.filter { it.is24hEmergency }
    }

    // Sort by proximity to center coordinates
    resultList = resultList.sortedBy { clinic ->
      OverpassParser.calculateHaversineKm(centerLat, centerLng, clinic.latitude, clinic.longitude)
    }

    Result.success(
      VetSearchResult(
        clinics = resultList,
        centerLat = centerLat,
        centerLng = centerLng,
        isCitySearch = isGeocodedCity,
        searchCityName = geocodedCityName
      )
    )
  }

  fun openNavigationInGoogleMaps(clinic: VetClinic) {
    try {
      val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${clinic.latitude},${clinic.longitude}")
      val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  fun dialPhoneNumber(phone: String) {
    if (phone == "Sin teléfono registrado" || phone.isBlank()) return
    try {
      val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  fun openOsmSourceUrl(url: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {}
  }
}

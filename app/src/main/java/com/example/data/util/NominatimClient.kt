package com.example.data.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object NominatimClient {

  private const val USER_AGENT = "DogCare/1.0 (com.aistudio.dogcare.kprmdx)"
  private const val ACCEPT_LANGUAGE = "es"
  private const val TIMEOUT_MS = 15000

  private val cache = ConcurrentHashMap<String, Pair<Double, Double>>()
  private var lastRequestTime = 0L

  data class GeocodeResult(
    val coordinates: Pair<Double, Double>?,
    val provider: String? = null,
    val technicalDetail: String? = null
  )

  /**
   * Geocodes a city name using Nominatim first, with automatic fallback to Photon (komoot).
   * Headers: User-Agent 'DogCare/1.0 (com.aistudio.dogcare.kprmdx)' and Accept-Language 'es'.
   * Timeouts: 15 seconds.
   */
  suspend fun geocodeCityWithDetail(query: String): GeocodeResult = withContext(Dispatchers.IO) {
    val key = query.trim().lowercase()
    if (key.isBlank()) return@withContext GeocodeResult(null, null, "Consulta vacía")

    cache[key]?.let {
      return@withContext GeocodeResult(it, "cache", "Encontrado en caché local")
    }

    var technicalDetail = ""

    // 1) Try Nominatim OpenStreetMap
    try {
      // Respect Nominatim rate limit: 1 req/sec
      val elapsed = System.currentTimeMillis() - lastRequestTime
      if (elapsed < 1000) {
        delay(1000 - elapsed)
      }

      val encoded = URLEncoder.encode(query, "UTF-8")
      val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=1"
      val url = URL(urlString)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("User-Agent", USER_AGENT)
        setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
      }

      lastRequestTime = System.currentTimeMillis()

      val responseCode = conn.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        if (array.length() > 0) {
          val obj = array.getJSONObject(0)
          val lat = obj.optDouble("lat", 0.0)
          val lon = obj.optDouble("lon", 0.0)
          if (lat != 0.0 || lon != 0.0) {
            val coords = Pair(lat, lon)
            cache[key] = coords
            return@withContext GeocodeResult(coords, "nominatim", "OK en nominatim.openstreetmap.org")
          }
        }
        technicalDetail = "Nominatim no devolvió coordenadas para '$query'"
      } else {
        technicalDetail = "HTTP $responseCode en nominatim.openstreetmap.org"
        Log.e("DogCareMap", "Nominatim HTTP $responseCode para '$query'")
      }
    } catch (e: Exception) {
      technicalDetail = "timeout/error en nominatim (${e.javaClass.simpleName})"
      Log.e("DogCareMap", "Error en Nominatim para '$query': ${e.message}", e)
    }

    // 2) Fallback to Photon (komoot GeoJSON: coordinates [lon, lat])
    try {
      val encoded = URLEncoder.encode(query, "UTF-8")
      val photonUrl = "https://photon.komoot.io/api/?q=$encoded&limit=1"
      val url = URL(photonUrl)
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("User-Agent", USER_AGENT)
        setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
      }

      val responseCode = conn.responseCode
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val features = root.optJSONArray("features")
        if (features != null && features.length() > 0) {
          val feat = features.getJSONObject(0)
          val geom = feat.optJSONObject("geometry")
          val coords = geom?.optJSONArray("coordinates")
          if (coords != null && coords.length() >= 2) {
            // GeoJSON coordinates format: [longitude, latitude]
            val lon = coords.optDouble(0, 0.0)
            val lat = coords.optDouble(1, 0.0)
            if (lat != 0.0 || lon != 0.0) {
              val pair = Pair(lat, lon)
              cache[key] = pair
              return@withContext GeocodeResult(pair, "photon", "OK en photon.komoot.io")
            }
          }
        }
        technicalDetail += "; Photon no devolvió resultados para '$query'"
      } else {
        technicalDetail += "; HTTP $responseCode en photon.komoot.io"
        Log.e("DogCareMap", "Photon HTTP $responseCode para '$query'")
      }
    } catch (e: Exception) {
      val photonErr = "timeout/error en photon (${e.javaClass.simpleName})"
      technicalDetail += "; $photonErr"
      Log.e("DogCareMap", "Error en Photon para '$query': ${e.message}", e)
    }

    GeocodeResult(null, null, technicalDetail)
  }

  suspend fun geocodeCity(query: String): Pair<Double, Double>? {
    return geocodeCityWithDetail(query).coordinates
  }
}

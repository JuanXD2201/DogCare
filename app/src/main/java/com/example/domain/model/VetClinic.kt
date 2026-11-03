package com.example.domain.model

data class VetClinic(
  val id: String,
  val name: String,
  val address: String,
  val phone: String,
  val latitude: Double,
  val longitude: Double,
  val is24hEmergency: Boolean,
  val openingHours: String,
  val services: List<String>,
  val osmSourceUrl: String = "https://www.openstreetmap.org"
) {
  fun formatDistance(userLat: Double, userLng: Double): String {
    val distanceKm = com.example.data.util.OverpassParser.calculateHaversineKm(userLat, userLng, latitude, longitude)
    return com.example.data.util.OverpassParser.formatDistanceText(distanceKm)
  }
}

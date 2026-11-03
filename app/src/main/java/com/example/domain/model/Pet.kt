package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Pet(
  val id: String = "",
  val userId: String = "",
  val name: String = "",
  val breed: String = "Mestizo",
  val sizeCategory: String = "medium", // small, medium, large, giant
  val ageYears: Int = 1,
  val ageMonths: Int = 0,
  val weightKg: Double = 12.0,
  val humanAgeYears: Double = 15.0,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

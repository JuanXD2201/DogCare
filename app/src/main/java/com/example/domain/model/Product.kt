package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProductCategory(val label: String, val iconRes: String) {
  DRY_FOOD("Concentrado / Seco", "croquettes"),
  WET_FOOD("Alimento Húmedo", "can"),
  SNACKS("Snacks y Premios", "bone");

  companion object {
    fun fromKey(key: String): ProductCategory {
      return entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.name.lowercase().contains(key.lowercase()) } ?: DRY_FOOD
    }
  }
}

enum class ProductLifeStage(val label: String) {
  ALL_STAGES("Todas las edades"),
  PUPPY("Cachorros"),
  ADULT("Adultos"),
  SENIOR("Senior")
}

@IgnoreExtraProperties
data class StoreCategory(
  val id: String = "",
  val name: String = "",
  val description: String = "",
  val order: Int = 0
)

@IgnoreExtraProperties
// El documento guarda su propio campo "id" (lo exigen las reglas): @DocumentId lanza excepción si ese campo existe en los datos.
data class Product(
  val id: String = "",
  val sellerUid: String = "", // UID del usuario que publicó el producto
  val sellerName: String = "",
  val name: String = "",
  val brand: String = "",
  val category: String = "dry_food", // dry_food, wet_food, snacks
  val lifeStage: String = "all_stages", // puppy, adult, senior, all_stages
  val presentation: String = "1 kg",
  val priceCop: Double = 0.0,
  val stock: Int = 0,
  val species: String = "dog",
  val lastOrderId: String? = null,
  val description: String = "",
  val imageUrl: String = "",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

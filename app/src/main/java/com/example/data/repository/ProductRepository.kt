package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.domain.model.Product
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.example.domain.model.StoreCategory

open class ProductRepository(private val context: Context) {
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
    context.getString(R.string.firestore_database_id)
  )

  private val collectionRef = db.collection("products")
  private val categoriesRef = db.collection("categories")

  // E) Chips de la tienda leen de Firestore con la enum/valores por defecto como respaldo
  open fun observeCategories(): Flow<List<StoreCategory>> = flow {
    categoriesRef.snapshots()
      .map { snapshot ->
        val list = snapshot.toObjects(StoreCategory::class.java).sortedBy { it.order }
        if (list.isEmpty()) {
          getDefaultCategories()
        } else {
          list
        }
      }
      .catch {
        emit(getDefaultCategories())
      }
      .collect { emit(it) }
  }

  fun getDefaultCategories(): List<StoreCategory> {
    return listOf(
      StoreCategory(id = "dry_food", name = "Concentrado seco", description = "Alimento balanceado en croquetas para perros", order = 1),
      StoreCategory(id = "wet_food", name = "Alimento húmedo", description = "Patés, estofados y latas de alta hidratación", order = 2),
      StoreCategory(id = "snacks", name = "Snacks comestibles caninos", description = "Snacks, premios y masticables caninos", order = 3)
    )
  }

  // Marketplace multi-vendedor: se listan los productos activos y, además, los inactivos del propio vendedor
  // para que pueda reactivarlos. Si no hay productos se muestra el estado vacío (no hay catálogo sembrado).
  open fun observeProducts(): Flow<List<Product>> = flow {
    val path = "products"
    collectionRef.snapshots()
      .map { snapshot ->
        val myUid = Firebase.auth.currentUser?.uid
        snapshot.toObjects(Product::class.java).filter { it.active || (myUid != null && it.sellerUid == myUid) }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

data class CartValidationResult(
  val isValid: Boolean,
  val errorMessage: String = ""
)

  suspend fun validateCartItemsStockAndPrice(items: com.example.domain.model.CartItem): CartValidationResult {
    return validateCartItemsList(listOf(items))
  }

  suspend fun validateCartItemsList(items: List<com.example.domain.model.CartItem>): CartValidationResult = withContext(Dispatchers.IO) {
    for (cartItem in items) {
      val prodId = cartItem.product.id
      if (prodId.isBlank()) continue

      val latest = getProductById(prodId)
        ?: return@withContext CartValidationResult(false, "El producto '${cartItem.product.name}' ya no está disponible en la tienda.")

      if (!latest.active) {
        return@withContext CartValidationResult(false, "El producto '${latest.name}' ha sido desactivado.")
      }

      if (latest.priceCop != cartItem.product.priceCop) {
        return@withContext CartValidationResult(
          false,
          "El precio de '${latest.name}' cambió a $${latest.priceCop.toInt()} COP. Actualiza tu carrito."
        )
      }

      if (latest.stock < cartItem.quantity) {
        return@withContext CartValidationResult(
          false,
          "Stock insuficiente para '${latest.name}'. Quedan ${latest.stock} unidades disponible(s)."
        )
      }
    }

    CartValidationResult(true)
  }

  suspend fun getProductById(id: String): Product? {
    return try {
      val doc = collectionRef.document(id).get().await()
      doc.toObject(Product::class.java)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.READ, "products/$id")
      null
    }
  }

  suspend fun addProduct(product: Product): String {
    val seller = Firebase.auth.currentUser
      ?: throw IllegalStateException("Inicia sesión para publicar un producto.")
    val docRef = collectionRef.document()
    val payload = mapOf(
      "id" to docRef.id,
      "sellerUid" to seller.uid,
      "sellerName" to (seller.displayName?.takeIf { it.isNotBlank() } ?: "Vendedor DogCare"),
      "name" to product.name,
      "brand" to product.brand,
      "category" to product.category,
      "lifeStage" to product.lifeStage,
      "presentation" to product.presentation,
      "priceCop" to product.priceCop,
      "stock" to product.stock,
      "species" to "dog",
      "description" to product.description,
      "imageUrl" to product.imageUrl,
      "active" to product.active,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateProduct(product: Product) {
    require(product.id.isNotBlank()) { "ID de producto requerido" }
    val docRef = collectionRef.document(product.id)
    val updates = mapOf(
      "name" to product.name,
      "brand" to product.brand,
      "category" to product.category,
      "lifeStage" to product.lifeStage,
      "presentation" to product.presentation,
      "priceCop" to product.priceCop,
      "stock" to product.stock,
      "species" to "dog",
      "description" to product.description,
      "imageUrl" to product.imageUrl,
      "active" to product.active,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteProduct(productId: String) {
    val docRef = collectionRef.document(productId)
    try {
      // Soft-delete or hard delete
      docRef.delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun updateStock(productId: String, quantityChange: Int) {
    val docRef = collectionRef.document(productId)
    try {
      docRef.update("stock", FieldValue.increment(quantityChange.toLong())).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }
}

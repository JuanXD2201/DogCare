package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.domain.model.CartItem
import com.example.domain.model.CartSummary
import com.example.domain.model.Product
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CartRepository(private var appContext: Context? = null) {
  private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
  val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

  private val _cartSummary = MutableStateFlow(CartSummary())
  val cartSummary: StateFlow<CartSummary> = _cartSummary.asStateFlow()

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val db: FirebaseFirestore? by lazy {
    try {
      if (appContext != null) {
        val dbId = appContext!!.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  fun initContext(context: Context) {
    this.appContext = context
  }

  companion object {
    val instance = CartRepository()
    const val FREE_SHIPPING_THRESHOLD = 90000.0
    const val STANDARD_SHIPPING_COP = 8500.0
  }

  // D) Carga el carrito desde /carts/{uid} al iniciar sesión
  suspend fun loadCartForUser(uid: String) {
    if (uid.isBlank()) return
    val database = db ?: return
    try {
      val doc = database.collection("carts").document(uid).get().await()
      if (doc.exists()) {
        val rawList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
        val loadedItems = rawList.mapNotNull { itemMap ->
          val quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
          val prodMap = itemMap["product"] as? Map<String, Any> ?: return@mapNotNull null
          val product = Product(
            id = prodMap["id"] as? String ?: "",
            name = prodMap["name"] as? String ?: "",
            brand = prodMap["brand"] as? String ?: "",
            category = prodMap["category"] as? String ?: "dry_food",
            lifeStage = prodMap["lifeStage"] as? String ?: "all_stages",
            presentation = prodMap["presentation"] as? String ?: "",
            priceCop = (prodMap["priceCop"] as? Number)?.toDouble() ?: 0.0,
            stock = (prodMap["stock"] as? Number)?.toInt() ?: 0,
            species = prodMap["species"] as? String ?: "dog",
            description = prodMap["description"] as? String ?: "",
            imageUrl = prodMap["imageUrl"] as? String ?: "",
            active = prodMap["active"] as? Boolean ?: true
          )
          CartItem(product = product, quantity = quantity)
        }
        _cartItems.value = loadedItems
        recalculateSummary(persist = false)
      }
    } catch (_: Exception) {
      // Ignorar fallas si offline o reglas locales
    }
  }

  // D) Guarda en /carts/{uid} en cada cambio cuando hay sesión
  private fun persistToFirestore() {
    val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null } ?: return
    val database = db ?: return
    coroutineScope.launch {
      try {
        val payload = mapOf(
          "items" to _cartItems.value.map { item ->
            mapOf(
              "quantity" to item.quantity,
              "product" to mapOf(
                "id" to item.product.id,
                "name" to item.product.name,
                "brand" to item.product.brand,
                "category" to item.product.category,
                "lifeStage" to item.product.lifeStage,
                "presentation" to item.product.presentation,
                "priceCop" to item.product.priceCop,
                "stock" to item.product.stock,
                "species" to item.product.species,
                "description" to item.product.description,
                "imageUrl" to item.product.imageUrl,
                "active" to item.product.active
              )
            )
          },
          "updatedAt" to FieldValue.serverTimestamp()
        )
        database.collection("carts").document(uid).set(payload).await()
      } catch (_: Exception) {
        // Ignorar fallas si offline
      }
    }
  }

  fun addToCart(product: Product, quantityToAdd: Int = 1): Result<Unit> {
    if (product.stock <= 0) {
      return Result.failure(IllegalStateException("El producto ${product.name} está agotado."))
    }

    var errorMsg: String? = null
    _cartItems.update { current ->
      val existing = current.firstOrNull { it.product.id == product.id }
      val currentQty = existing?.quantity ?: 0
      val newQty = currentQty + quantityToAdd

      if (newQty > product.stock) {
        errorMsg = "Solo hay ${product.stock} unidades disponibles en inventario de ${product.name}."
        current
      } else {
        if (existing != null) {
          current.map {
            if (it.product.id == product.id) it.copy(quantity = newQty) else it
          }
        } else {
          current + CartItem(product = product, quantity = newQty)
        }
      }
    }

    if (errorMsg != null) {
      return Result.failure(IllegalStateException(errorMsg))
    }

    recalculateSummary()
    return Result.success(Unit)
  }

  fun updateQuantity(productId: String, newQuantity: Int): Result<Unit> {
    if (newQuantity <= 0) {
      removeFromCart(productId)
      return Result.success(Unit)
    }

    var errorMsg: String? = null
    _cartItems.update { current ->
      current.map { item ->
        if (item.product.id == productId) {
          if (newQuantity > item.product.stock) {
            errorMsg = "Inventario máximo disponible: ${item.product.stock} unidades."
            item.copy(quantity = item.product.stock)
          } else {
            item.copy(quantity = newQuantity)
          }
        } else item
      }
    }

    recalculateSummary()
    return if (errorMsg != null) Result.failure(IllegalStateException(errorMsg)) else Result.success(Unit)
  }

  fun removeFromCart(productId: String) {
    _cartItems.update { current ->
      current.filterNot { it.product.id == productId }
    }
    recalculateSummary()
  }

  fun clearCart() {
    _cartItems.value = emptyList()
    recalculateSummary()
  }

  private fun recalculateSummary(persist: Boolean = true) {
    val items = _cartItems.value
    val subtotal = items.sumOf { it.subtotal }
    val totalCount = items.sumOf { it.quantity }
    val shipping = if (subtotal == 0.0 || subtotal >= FREE_SHIPPING_THRESHOLD) 0.0 else STANDARD_SHIPPING_COP
    val total = if (subtotal == 0.0) 0.0 else subtotal + shipping

    _cartSummary.value = CartSummary(
      items = items,
      subtotalCop = subtotal,
      shippingCop = shipping,
      totalCop = total,
      totalItemsCount = totalCount
    )

    if (persist) {
      persistToFirestore()
    }
  }
}

package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.domain.model.CartItem
import com.example.domain.model.Order
import com.example.domain.model.OrderItemRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StockOrPriceChangedException(message: String) : Exception(message)

open class OrderRepository(private val context: Context) {
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
    context.getString(R.string.firestore_database_id)
  )

  private fun requireUserId(): String {
    return Firebase.auth.currentUser?.uid
      ?: throw IllegalStateException("Usuario no autenticado para gestionar pedidos.")
  }

  open fun observeUserOrders(): Flow<List<Order>> = flow {
    val uid = requireUserId()
    val path = "orders"
    db.collection("orders")
      .whereEqualTo("userId", uid)
      .snapshots()
      .map { snapshot -> snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt } }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  suspend fun createOrder(
    customerName: String,
    deliveryAddress: String,
    customerPhone: String,
    items: List<CartItem>,
    subtotal: Double,
    shipping: Double,
    total: Double
  ): Order {
    val uid = requireUserId()
    val ordersRef = db.collection("orders")
    val docRef = ordersRef.document()

    val itemRecords = items.map {
      OrderItemRecord(
        productId = it.product.id,
        productName = it.product.name,
        unitPriceCop = it.product.priceCop,
        quantity = it.quantity,
        subtotalCop = it.subtotal
      )
    }

    val quantitiesMap = items.filter { it.product.id.isNotBlank() }.associate { it.product.id to it.quantity }

    val order = Order(
      id = docRef.id,
      userId = uid,
      customerName = customerName,
      deliveryAddress = deliveryAddress,
      customerPhone = customerPhone,
      items = itemRecords,
      quantities = quantitiesMap,
      subtotalCop = subtotal,
      shippingCop = shipping,
      totalCop = total,
      paymentStatus = "PENDING",
      paymentReference = "REF-${System.currentTimeMillis()}-${(1000..9999).random()}",
      paymentMethod = "PASARELA_SANDBOX"
    )

    val payload = mapOf(
      "id" to order.id,
      "userId" to uid,
      "customerName" to order.customerName,
      "deliveryAddress" to order.deliveryAddress,
      "customerPhone" to order.customerPhone,
      "items" to order.items.map { mapOf(
        "productId" to it.productId,
        "productName" to it.productName,
        "unitPriceCop" to it.unitPriceCop,
        "quantity" to it.quantity,
        "subtotalCop" to it.subtotalCop
      )},
      "quantities" to quantitiesMap,
      "subtotalCop" to order.subtotalCop,
      "shippingCop" to order.shippingCop,
      "totalCop" to order.totalCop,
      "paymentStatus" to "PENDING",
      "paymentReference" to order.paymentReference,
      "paymentMethod" to order.paymentMethod,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    // B.1) createOrder crea en un batch solo el pedido PENDING y el pago PENDING; NO toca el stock
    val batch = db.batch()
    batch.set(docRef, payload)

    // Pago inicial en estado PENDING
    val paymentRef = db.collection("payments").document()
    val paymentPayload = mapOf(
      "id" to paymentRef.id,
      "orderId" to docRef.id,
      "userId" to uid,
      "amountCop" to order.totalCop,
      "provider" to "PASARELA_SANDBOX",
      "status" to "PENDING",
      "paymentReference" to order.paymentReference,
      "createdAt" to FieldValue.serverTimestamp()
    )
    batch.set(paymentRef, paymentPayload)

    try {
      batch.commit().await()
      return order
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // B.2) confirmApprovedPayment con db.runTransaction: lee productos, valida active, precio y stock,
  // y actualiza orden a APPROVED, descuenta stock y crea pago APPROVED
  suspend fun confirmApprovedPayment(order: Order, reference: String) {
    val uid = requireUserId()
    try {
      db.runTransaction { transaction ->
        // Lee cada producto y verifica active, precio igual al del pedido y stock >= cantidad
        val productsToUpdate = mutableListOf<Triple<com.google.firebase.firestore.DocumentReference, Int, Int>>()

        for (item in order.items) {
          if (item.productId.isBlank()) continue
          val productRef = db.collection("products").document(item.productId)
          val snap = transaction.get(productRef)

          if (!snap.exists()) {
            throw StockOrPriceChangedException("El producto '${item.productName}' ya no se encuentra en el catálogo.")
          }

          val active = snap.getBoolean("active") ?: true
          if (!active) {
            throw StockOrPriceChangedException("El producto '${item.productName}' ha sido desactivado.")
          }

          val currentPrice = snap.getDouble("priceCop") ?: 0.0
          if (currentPrice != item.unitPriceCop) {
            throw StockOrPriceChangedException("El precio de '${item.productName}' cambió a $${currentPrice.toInt()} COP. Por favor revisa tu compra.")
          }

          val currentStock = (snap.getLong("stock") ?: 0L).toInt()
          if (currentStock < item.quantity) {
            throw StockOrPriceChangedException("Stock insuficiente para '${item.productName}'. Disponibles: $currentStock, solicitadas: ${item.quantity}.")
          }

          val newStock = currentStock - item.quantity
          productsToUpdate.add(Triple(productRef, currentStock, newStock))
        }

        // Si todo está bien, en la misma transacción:
        // 1. Actualiza el pedido a APPROVED con paymentReference
        val orderRef = db.collection("orders").document(order.id)
        transaction.update(
          orderRef,
          mapOf(
            "paymentStatus" to "APPROVED",
            "paymentReference" to reference,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        // 2. Baja el stock de cada producto con el valor leído en la transacción y lastOrderId
        for ((productRef, _, newStock) in productsToUpdate) {
          transaction.update(
            productRef,
            mapOf(
              "stock" to newStock,
              "lastOrderId" to order.id,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        // 3. Crea el documento de /payments con status APPROVED
        val paymentRef = db.collection("payments").document()
        val paymentPayload = mapOf(
          "id" to paymentRef.id,
          "orderId" to order.id,
          "userId" to uid,
          "amountCop" to order.totalCop,
          "provider" to "PASARELA_SANDBOX",
          "status" to "APPROVED",
          "paymentReference" to reference,
          "createdAt" to FieldValue.serverTimestamp()
        )
        transaction.set(paymentRef, paymentPayload)
      }.await()
    } catch (e: StockOrPriceChangedException) {
      // B.3) Si la transacción falla por stock o precio, marca el pedido ERROR con su pago ERROR
      markOrderError(
        orderId = order.id,
        reference = reference.ifBlank { "ERR-${System.currentTimeMillis()}" },
        amountCop = order.totalCop,
        errorMessage = e.message ?: "Error de stock o precio"
      )
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
      throw e
    }
  }

  // B.4) REJECTED: marca el pedido REJECTED y crea su pago REJECTED, sin tocar el stock
  suspend fun markOrderRejected(orderId: String, reference: String, amountCop: Double) {
    val uid = requireUserId()
    val docRef = db.collection("orders").document(orderId)
    val paymentRef = db.collection("payments").document()

    val batch = db.batch()
    batch.update(
      docRef,
      mapOf(
        "paymentStatus" to "REJECTED",
        "paymentReference" to reference,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    val paymentPayload = mapOf(
      "id" to paymentRef.id,
      "orderId" to orderId,
      "userId" to uid,
      "amountCop" to amountCop,
      "provider" to "PASARELA_SANDBOX",
      "status" to "REJECTED",
      "paymentReference" to reference,
      "createdAt" to FieldValue.serverTimestamp()
    )
    batch.set(paymentRef, paymentPayload)

    try {
      batch.commit().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // B.3) Marcar orden como ERROR con comprobante ERROR
  suspend fun markOrderError(orderId: String, reference: String, amountCop: Double, errorMessage: String) {
    val uid = requireUserId()
    val docRef = db.collection("orders").document(orderId)
    val paymentRef = db.collection("payments").document()

    val batch = db.batch()
    batch.update(
      docRef,
      mapOf(
        "paymentStatus" to "ERROR",
        "paymentReference" to reference,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    val paymentPayload = mapOf(
      "id" to paymentRef.id,
      "orderId" to orderId,
      "userId" to uid,
      "amountCop" to amountCop,
      "provider" to "PASARELA_SANDBOX",
      "status" to "ERROR",
      "paymentReference" to reference,
      "createdAt" to FieldValue.serverTimestamp()
    )
    batch.set(paymentRef, paymentPayload)

    try {
      batch.commit().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun updatePaymentStatus(orderId: String, newStatus: String, reference: String, amountCop: Double = 0.0) {
    val uid = requireUserId()
    val docRef = db.collection("orders").document(orderId)
    val paymentRef = db.collection("payments").document()

    val batch = db.batch()
    batch.update(
      docRef,
      mapOf(
        "paymentStatus" to newStatus,
        "paymentReference" to reference,
        "updatedAt" to FieldValue.serverTimestamp()
      )
    )

    if (amountCop > 0) {
      val paymentPayload = mapOf(
        "id" to paymentRef.id,
        "orderId" to orderId,
        "userId" to uid,
        "amountCop" to amountCop,
        "provider" to "PASARELA_SANDBOX",
        "status" to newStatus,
        "paymentReference" to reference,
        "createdAt" to FieldValue.serverTimestamp()
      )
      batch.set(paymentRef, paymentPayload)
    }

    try {
      batch.commit().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }
}

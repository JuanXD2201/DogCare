package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderPaymentStatus(val label: String, val colorHex: Long) {
  PENDING("Pendiente de pago", 0xFFB47D16),
  APPROVED("Pago Aprobado", 0xFF2E7D32),
  REJECTED("Pago Rechazado", 0xFFBA1A1A)
}

@IgnoreExtraProperties
data class OrderItemRecord(
  val productId: String = "",
  val productName: String = "",
  val unitPriceCop: Double = 0.0,
  val quantity: Int = 1,
  val subtotalCop: Double = 0.0
)

@IgnoreExtraProperties
data class Order(
  val id: String = "",
  val userId: String = "",
  val customerName: String = "",
  val deliveryAddress: String = "",
  val customerPhone: String = "",
  val items: List<OrderItemRecord> = emptyList(),
  val quantities: Map<String, Int> = emptyMap(),
  val subtotalCop: Double = 0.0,
  val shippingCop: Double = 0.0,
  val totalCop: Double = 0.0,
  val paymentStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
  val paymentReference: String = "",
  val paymentMethod: String = "SANDBOX_GATEWAY",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

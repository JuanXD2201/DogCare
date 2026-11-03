package com.example.domain.model

data class CartItem(
  val product: Product,
  val quantity: Int
) {
  val subtotal: Double
    get() = product.priceCop * quantity
}

data class CartSummary(
  val items: List<CartItem> = emptyList(),
  val subtotalCop: Double = 0.0,
  val shippingCop: Double = 0.0,
  val totalCop: Double = 0.0,
  val totalItemsCount: Int = 0
)

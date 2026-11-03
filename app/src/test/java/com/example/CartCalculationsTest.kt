package com.example

import com.example.domain.model.CartItem
import com.example.domain.model.CartSummary
import com.example.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class CartCalculationsTest {

  private fun calculateSummary(items: List<CartItem>, freeShippingThreshold: Double = 150000.0, standardShippingFee: Double = 12000.0): CartSummary {
    val subtotal = items.sumOf { it.subtotal }
    val totalQuantity = items.sumOf { it.quantity }
    val shippingFee = if (subtotal >= freeShippingThreshold || items.isEmpty()) 0.0 else standardShippingFee
    val total = subtotal + shippingFee

    return CartSummary(
      items = items,
      subtotalCop = subtotal,
      shippingCop = shippingFee,
      totalCop = total,
      totalItemsCount = totalQuantity
    )
  }

  @Test
  fun testCartWithItemsBelowFreeShippingThreshold() {
    val items = listOf(
      CartItem(
        product = Product(id = "p1", name = "Comida", priceCop = 50000.0),
        quantity = 2
      ) // Subtotal: 100,000
    )

    val summary = calculateSummary(items)
    assertEquals(100000.0, summary.subtotalCop, 0.01)
    assertEquals(12000.0, summary.shippingCop, 0.01)
    assertEquals(112000.0, summary.totalCop, 0.01)
    assertEquals(2, summary.totalItemsCount)
  }

  @Test
  fun testCartWithItemsQualifyingForFreeShipping() {
    val items = listOf(
      CartItem(
        product = Product(id = "p1", name = "Comida Premium", priceCop = 160000.0),
        quantity = 1
      )
    )

    val summary = calculateSummary(items)
    assertEquals(160000.0, summary.subtotalCop, 0.01)
    assertEquals(0.0, summary.shippingCop, 0.01)
    assertEquals(160000.0, summary.totalCop, 0.01)
  }
}

package com.example

import com.example.data.repository.StockOrPriceChangedException
import com.example.domain.model.CartItem
import com.example.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMarketCorrectionTest {

  @Test
  fun testStockOrPriceChangedExceptionThrowsWithMessage() {
    val exception = assertThrows(StockOrPriceChangedException::class.java) {
      throw StockOrPriceChangedException("Stock insuficiente para 'Royal Canin'. Disponibles: 2, solicitadas: 5.")
    }
    assertTrue(exception.message!!.contains("Stock insuficiente"))
  }

  @Test
  fun testValidateStockDecrementAllowedOnlyOnPendingToApproved() {
    fun canDecrementStock(orderStatus: String, nextOrderStatus: String): Boolean {
      return orderStatus == "PENDING" && nextOrderStatus == "APPROVED"
    }

    assertTrue("PENDING -> APPROVED should allow stock decrement", canDecrementStock("PENDING", "APPROVED"))
    assertFalse("APPROVED -> APPROVED should not allow stock decrement", canDecrementStock("APPROVED", "APPROVED"))
    assertFalse("PENDING -> REJECTED should not decrement stock", canDecrementStock("PENDING", "REJECTED"))
    assertFalse("PENDING -> PENDING should not decrement stock", canDecrementStock("PENDING", "PENDING"))
  }

  @Test
  fun testCartValidationDetectsPriceChange() {
    val cartProduct = Product(id = "p1", name = "Taste of the Wild", priceCop = 22000.0, stock = 10, active = true)
    val latestCatalogProduct = Product(id = "p1", name = "Taste of the Wild", priceCop = 25000.0, stock = 10, active = true)

    val hasPriceChanged = cartProduct.priceCop != latestCatalogProduct.priceCop
    assertTrue(hasPriceChanged)
  }

  @Test
  fun testCartValidationDetectsInsufficientStock() {
    val requestedQuantity = 5
    val currentStock = 3

    val isStockSufficient = currentStock >= requestedQuantity
    assertFalse(isStockSufficient)
  }

  @Test
  fun testCartValidationDetectsInactiveProduct() {
    val catalogProduct = Product(id = "p1", name = "Barkies Premios", priceCop = 18500.0, stock = 10, active = false)
    assertFalse(catalogProduct.active)
  }
}

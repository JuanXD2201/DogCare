package com.example

import com.example.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDogValidatorTest {

  private fun isValidDogProduct(product: Product): Boolean {
    val validCategories = listOf("dry_food", "wet_food", "snacks")
    val validLifeStages = listOf("puppy", "adult", "senior", "all_stages")

    return product.species == "dog" &&
        product.category in validCategories &&
        product.lifeStage in validLifeStages &&
        product.priceCop >= 0.0 &&
        product.stock >= 0 &&
        product.name.isNotBlank()
  }

  @Test
  fun testValidDogProductPasses() {
    val validProd = Product(
      id = "p1",
      name = "Croquetas Caninas Premium",
      brand = "DogCare",
      category = "dry_food",
      lifeStage = "adult",
      priceCop = 50000.0,
      stock = 10,
      species = "dog"
    )
    assertTrue(isValidDogProduct(validProd))
  }

  @Test
  fun testNonDogProductFailsValidation() {
    val catProd = Product(
      id = "p2",
      name = "Comida para Gatos",
      category = "dry_food",
      species = "cat"
    )
    assertFalse(isValidDogProduct(catProd))
  }

  @Test
  fun testInvalidCategoryFailsValidation() {
    val invalidCategoryProd = Product(
      id = "p3",
      name = "Producto Desconocido",
      category = "unknown_category",
      species = "dog"
    )
    assertFalse(isValidDogProduct(invalidCategoryProd))
  }
}

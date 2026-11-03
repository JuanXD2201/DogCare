package com.example

import com.example.data.repository.CartRepository
import com.example.data.repository.PaymentGatewayRepository
import com.example.data.repository.SimulatedPaymentOutcome
import com.example.domain.model.DogLifeStage
import com.example.domain.model.DogSizeCategory
import com.example.domain.model.Order
import com.example.domain.model.Product
import com.example.domain.model.VetClinic
import com.example.domain.usecase.CalculateDogAgeUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DogCareUnitTest {

  private lateinit var ageUseCase: CalculateDogAgeUseCase
  private lateinit var cartRepository: CartRepository
  private lateinit var paymentGateway: PaymentGatewayRepository

  @Before
  fun setUp() {
    ageUseCase = CalculateDogAgeUseCase()
    cartRepository = CartRepository()
    paymentGateway = PaymentGatewayRepository()
  }

  // ==========================================
  // Test 1: Canine Age Calculation (Phase 1)
  // ==========================================
  @Test
  fun testAgeCalculation_firstYear_isPuppy() {
    val result = ageUseCase.calculate(years = 1, months = 0, size = DogSizeCategory.MEDIUM)
    assertEquals(15.0, result.humanYearsEquivalent, 0.1)
    assertEquals(DogLifeStage.ADULT, result.stage)
  }

  @Test
  fun testAgeCalculation_sixMonths_isPuppy() {
    val result = ageUseCase.calculate(years = 0, months = 6, size = DogSizeCategory.MEDIUM)
    assertEquals(7.5, result.humanYearsEquivalent, 0.1)
    assertEquals(DogLifeStage.PUPPY, result.stage)
    assertTrue(result.nutritionAdvice.contains("cachorros", ignoreCase = true))
  }

  @Test
  fun testAgeCalculation_secondYear_reachesTwentyFour() {
    val result = ageUseCase.calculate(years = 2, months = 0, size = DogSizeCategory.MEDIUM)
    assertEquals(24.0, result.humanYearsEquivalent, 0.1)
  }

  @Test
  fun testAgeCalculation_differentialBySize_afterTwoYears() {
    // 5 years old: Base 24 + 3 subsequent years
    val smallDog = ageUseCase.calculate(years = 5, months = 0, size = DogSizeCategory.SMALL)
    // 24 + 3 * 4 = 36
    assertEquals(36.0, smallDog.humanYearsEquivalent, 0.1)

    val giantDog = ageUseCase.calculate(years = 5, months = 0, size = DogSizeCategory.GIANT)
    // 24 + 3 * 7.5 = 46.5
    assertEquals(46.5, giantDog.humanYearsEquivalent, 0.1)
  }

  @Test
  fun testAgeCalculation_seniorStage_giantVsSmall() {
    // A giant dog at 6 years is Senior
    val giantSenior = ageUseCase.calculate(years = 6, months = 0, size = DogSizeCategory.GIANT)
    assertEquals(DogLifeStage.SENIOR, giantSenior.stage)

    // A small dog at 6 years is still Adult
    val smallAdult = ageUseCase.calculate(years = 6, months = 0, size = DogSizeCategory.SMALL)
    assertEquals(DogLifeStage.ADULT, smallAdult.stage)
  }

  // ==========================================
  // Test 2: Shopping Cart & Stock Validation (Phase 5 & 6)
  // ==========================================
  @Test
  fun testCart_addItem_calculatesSubtotalAndShipping() {
    val testProduct = Product(
      id = "p1",
      name = "Croquetas Test",
      brand = "Brand",
      category = "dry_food",
      priceCop = 30000.0,
      stock = 5
    )

    val result = cartRepository.addToCart(testProduct, 2)
    assertTrue(result.isSuccess)

    val summary = cartRepository.cartSummary.value
    assertEquals(2, summary.totalItemsCount)
    assertEquals(60000.0, summary.subtotalCop, 0.01)
    // Below threshold (90.000 COP) -> standard shipping applies
    assertEquals(CartRepository.STANDARD_SHIPPING_COP, summary.shippingCop, 0.01)
    assertEquals(68500.0, summary.totalCop, 0.01)
  }

  @Test
  fun testCart_freeShipping_thresholdExceeded() {
    val expensiveProduct = Product(
      id = "p2",
      name = "Bolsa Grande 15kg",
      brand = "Brand",
      category = "dry_food",
      priceCop = 120000.0,
      stock = 10
    )

    cartRepository.addToCart(expensiveProduct, 1)
    val summary = cartRepository.cartSummary.value
    assertEquals(0.0, summary.shippingCop, 0.01)
    assertEquals(120000.0, summary.totalCop, 0.01)
  }

  @Test
  fun testCart_exceedingStock_isRejected() {
    val lowStockProduct = Product(
      id = "p3",
      name = "Snack Edición Limitada",
      brand = "Brand",
      category = "snacks",
      priceCop = 15000.0,
      stock = 2
    )

    // Attempt to add 5 units when only 2 are available
    val result = cartRepository.addToCart(lowStockProduct, 5)
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("disponibles") == true)
  }

  // ==========================================
  // Test 3: Sandbox Payment Gateway (Phase 7)
  // ==========================================
  @Test
  fun testPaymentGateway_approvedOutcome() = runBlocking {
    val sampleOrder = Order(
      id = "ord_001",
      userId = "user_1",
      customerName = "Juan Pérez",
      deliveryAddress = "Calle 10 # 20-30",
      customerPhone = "3001234567",
      totalCop = 55000.0,
      paymentReference = "REF-TEST-001"
    )

    val receipt = paymentGateway.processSandboxPayment(
      order = sampleOrder,
      cardNumber = "4242 4242 4242 4242",
      cardHolder = "Juan Pérez",
      cvv = "123",
      expMonthYear = "10/28"
    )

    assertEquals(SimulatedPaymentOutcome.APPROVED, receipt.status)
    assertTrue(receipt.authorizationCode.startsWith("AUTH-"))
    assertNotNull(receipt.transactionId)
  }

  @Test
  fun testPaymentGateway_rejectedOutcome() = runBlocking {
    val sampleOrder = Order(
      id = "ord_002",
      userId = "user_1",
      totalCop = 30000.0,
      paymentReference = "REF-TEST-002"
    )

    val receipt = paymentGateway.processSandboxPayment(
      order = sampleOrder,
      cardNumber = "4000 0000 0000 0002",
      cardHolder = "Juan Pérez",
      cvv = "123",
      expMonthYear = "10/28"
    )

    assertEquals(SimulatedPaymentOutcome.REJECTED, receipt.status)
    assertEquals("REJ-00", receipt.authorizationCode)
  }

  // ==========================================
  // Test 4: Vet Clinic Model & Distance (Phase 4)
  // ==========================================
  @Test
  fun testVetClinic_distanceFormatting() {
    val clinic = VetClinic(
      id = "v1",
      name = "Hospital Veterinario",
      address = "Calle 100",
      phone = "1234567",
      latitude = 4.6853,
      longitude = -74.0531,
      is24hEmergency = true,
      openingHours = "24 Horas",
      services = listOf("Urgencias")
    )

    // User is located exactly at the clinic
    val formattedSame = clinic.formatDistance(4.6853, -74.0531)
    assertEquals("0 m", formattedSame)
  }
}

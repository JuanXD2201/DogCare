package com.example

import com.example.domain.model.DogLifeStage
import com.example.domain.model.DogSizeCategory
import com.example.domain.usecase.CalculateDogAgeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DogAgeCalculatorTest {

  private lateinit var useCase: CalculateDogAgeUseCase

  @Before
  fun setUp() {
    useCase = CalculateDogAgeUseCase()
  }

  @Test
  fun testPuppyCalculation() {
    val result = useCase.calculate(years = 0, months = 6, size = DogSizeCategory.MEDIUM)
    assertEquals(DogLifeStage.PUPPY, result.stage)
    assertEquals(7.5, result.humanYearsEquivalent, 0.1)
    assertTrue(result.nutritionAdvice.contains("DHA"))
  }

  @Test
  fun testAdultCalculation() {
    val result = useCase.calculate(years = 3, months = 0, size = DogSizeCategory.MEDIUM)
    assertEquals(DogLifeStage.ADULT, result.stage)
    // 24 + 1*5 = 29
    assertEquals(29.0, result.humanYearsEquivalent, 0.1)
  }

  @Test
  fun testSeniorGiantCalculation() {
    val result = useCase.calculate(years = 7, months = 0, size = DogSizeCategory.GIANT)
    assertEquals(DogLifeStage.SENIOR, result.stage)
    // 24 + 5*7.5 = 61.5
    assertEquals(61.5, result.humanYearsEquivalent, 0.1)
    assertTrue(result.nutritionAdvice.contains("glucosamina") || result.nutritionAdvice.contains("condroprotectores"))
  }
}

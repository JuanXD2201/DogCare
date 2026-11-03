package com.example.domain.model

enum class DogSizeCategory(val label: String, val weightRange: String) {
  SMALL("Pequeño", "Menos de 10 kg (Chihuahua, Poodle toy, Pug)"),
  MEDIUM("Mediano", "10 a 22 kg (Beagle, Bulldog francés, Cocker)"),
  LARGE("Grande", "23 a 40 kg (Golden Retriever, Pastor Alemán, Labrador)"),
  GIANT("Gigante", "Más de 40 kg (Gran Danés, San Bernardo, Mastín)");

  companion object {
    fun fromString(value: String): DogSizeCategory {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
  }
}

enum class DogLifeStage(val title: String, val badgeColorHex: Long) {
  PUPPY("Cachorro", 0xFF2E7D32),
  ADULT("Adulto", 0xFF1B4D3E),
  SENIOR("Senior (Adulto Mayor)", 0xFFE86A33)
}

data class DogAgeCalculationResult(
  val humanYearsEquivalent: Double,
  val stage: DogLifeStage,
  val summary: String,
  val nutritionAdvice: String,
  val healthAdvice: String,
  val exerciseAdvice: String
)

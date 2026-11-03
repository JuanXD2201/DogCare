package com.example.domain.usecase

import com.example.domain.model.DogAgeCalculationResult
import com.example.domain.model.DogLifeStage
import com.example.domain.model.DogSizeCategory
import kotlin.math.roundToInt

class CalculateDogAgeUseCase {

  /**
   * Calculates canine-to-human age equivalence based on scientific AVMA non-linear curve:
   * - Year 1: ~15 human years (rapid growth and sexual maturity)
   * - Year 2: +9 human years (~24 human years total)
   * - Subsequent years:
   *   * Small (<10kg): +4 human years / dog year
   *   * Medium (10-22kg): +5 human years / dog year
   *   * Large (23-40kg): +6 human years / dog year
   *   * Giant (>40kg): +7.5 human years / dog year
   */
  fun calculate(
    years: Int,
    months: Int = 0,
    size: DogSizeCategory = DogSizeCategory.MEDIUM
  ): DogAgeCalculationResult {
    val totalDogYears = years.coerceAtLeast(0) + (months.coerceIn(0, 11) / 12.0)

    val humanAge = when {
      totalDogYears <= 0.0 -> 0.0
      totalDogYears <= 1.0 -> totalDogYears * 15.0
      totalDogYears <= 2.0 -> 15.0 + ((totalDogYears - 1.0) * 9.0)
      else -> {
        val baseAtTwo = 24.0
        val remainingYears = totalDogYears - 2.0
        val yearlyMultiplier = when (size) {
          DogSizeCategory.SMALL -> 4.0
          DogSizeCategory.MEDIUM -> 5.0
          DogSizeCategory.LARGE -> 6.0
          DogSizeCategory.GIANT -> 7.5
        }
        baseAtTwo + (remainingYears * yearlyMultiplier)
      }
    }

    val stage = when {
      totalDogYears < 1.0 -> DogLifeStage.PUPPY
      size == DogSizeCategory.GIANT && totalDogYears >= 6.0 -> DogLifeStage.SENIOR
      size == DogSizeCategory.LARGE && totalDogYears >= 7.0 -> DogLifeStage.SENIOR
      size == DogSizeCategory.MEDIUM && totalDogYears >= 8.0 -> DogLifeStage.SENIOR
      size == DogSizeCategory.SMALL && totalDogYears >= 10.0 -> DogLifeStage.SENIOR
      else -> DogLifeStage.ADULT
    }

    val roundedHuman = (humanAge * 10.0).roundToInt() / 10.0

    val nutritionAdvice = when (stage) {
      DogLifeStage.PUPPY -> "Alimento especial para cachorros rico en DHA, calcio y proteína de alta digestibilidad. 3 a 4 comidas al día."
      DogLifeStage.ADULT -> "Alimento balanceado según su nivel de actividad física para prevenir sobrepeso. 2 raciones al día."
      DogLifeStage.SENIOR -> "Dieta baja en calorías, con condroprotectores (glucosamina) para articulaciones y fibra suave."
    }

    val healthAdvice = when (stage) {
      DogLifeStage.PUPPY -> "Esquema completo de vacunación (parvovirus, moquillo, rabia) y desparasitación cada 3 meses."
      DogLifeStage.ADULT -> "Chequeo veterinario anual, refuerzos de vacunas y profilaxis dental preventiva."
      DogLifeStage.SENIOR -> "Chequeo geriátrico semestral con perfil renal, hepático y evaluación cardiológica."
    }

    val exerciseAdvice = when (stage) {
      DogLifeStage.PUPPY -> "Juegos cortos y socialización controlada. Evitar ejercicio extenuante para proteger cartílagos."
      DogLifeStage.ADULT -> "Paseos diarios de 45 a 60 minutos, juegos de estimulación mental y actividad aeróbica."
      DogLifeStage.SENIOR -> "Caminatas suaves sobre superficies blandas. Evitar saltos bruscos y escaleras empinadas."
    }

    val summary = "A sus $years años y $months meses, tu perro equivale a una persona de aproximadamente ${roundedHuman.toInt()} años (${stage.title})."

    return DogAgeCalculationResult(
      humanYearsEquivalent = roundedHuman,
      stage = stage,
      summary = summary,
      nutritionAdvice = nutritionAdvice,
      healthAdvice = healthAdvice,
      exerciseAdvice = exerciseAdvice
    )
  }
}

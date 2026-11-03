package com.example

import com.example.domain.util.EmergencyDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyDetectorTest {

  @Test
  fun testPoisoningDetectedAsEmergency() {
    assertTrue(EmergencyDetector.isEmergency("Mi perro comió veneno para ratas y está vomitando"))
  }

  @Test
  fun testSeizureDetectedWithAndWithoutAccent() {
    assertTrue(EmergencyDetector.isEmergency("Mi mascota está sufriendo una convulsión en este momento"))
    assertTrue(EmergencyDetector.isEmergency("tiene una CONVULSION"))
  }

  @Test
  fun testBleedingDetectedAsEmergency() {
    assertTrue(EmergencyDetector.isEmergency("Tiene mucha sangre brotando de la pata por un corte"))
  }

  @Test
  fun testBreathingAndToxicFoodDetected() {
    assertTrue(EmergencyDetector.isEmergency("Mi perro no respira bien y tiene las encías pálidas"))
    assertTrue(EmergencyDetector.isEmergency("Se comió un puñado de uvas"))
    assertTrue(EmergencyDetector.isEmergency("Creo que tiene un golpe de calor"))
  }

  @Test
  fun testGeneralQuestionNotDetectedAsEmergency() {
    assertFalse(EmergencyDetector.isEmergency("Cuál es la mejor marca de comida seca para un cachorro Golden Retriever?"))
    assertFalse(EmergencyDetector.isEmergency("Cada cuánto debo bañar a mi perro"))
  }

  @Test
  fun testBlankIsNotEmergency() {
    assertFalse(EmergencyDetector.isEmergency("   "))
  }
}

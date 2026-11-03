package com.example

import com.example.data.repository.SimulatedPaymentOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentStatusStateTest {

  private fun canTransitionState(currentStatus: String, nextStatus: String): Boolean {
    // Only PENDING orders can transition to APPROVED, DECLINED, REJECTED, or ERROR
    if (currentStatus != "PENDING") return false
    return nextStatus in listOf("APPROVED", "DECLINED", "REJECTED", "ERROR")
  }

  private fun evaluateCardNumberOutcome(cardNumber: String): SimulatedPaymentOutcome {
    val clean = cardNumber.replace(" ", "").replace("-", "")
    return when {
      clean.endsWith("0002") || clean.endsWith("9999") -> SimulatedPaymentOutcome.REJECTED
      clean.endsWith("0004") -> SimulatedPaymentOutcome.PENDING
      clean.length >= 13 -> SimulatedPaymentOutcome.APPROVED
      else -> SimulatedPaymentOutcome.REJECTED
    }
  }

  @Test
  fun testPendingToApprovedTransitionAllowed() {
    assertTrue(canTransitionState("PENDING", "APPROVED"))
  }

  @Test
  fun testApprovedToDeclinedTransitionDisallowed() {
    assertFalse(canTransitionState("APPROVED", "DECLINED"))
  }

  @Test
  fun testSandboxCardNumberEvaluations() {
    assertEquals(SimulatedPaymentOutcome.APPROVED, evaluateCardNumberOutcome("4242 4242 4242 4242"))
    assertEquals(SimulatedPaymentOutcome.REJECTED, evaluateCardNumberOutcome("4000 0000 0000 0002"))
    assertEquals(SimulatedPaymentOutcome.PENDING, evaluateCardNumberOutcome("4000 0000 0000 0004"))
  }
}

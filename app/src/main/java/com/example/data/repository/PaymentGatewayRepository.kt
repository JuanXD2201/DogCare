package com.example.data.repository

import com.example.domain.model.Order
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SimulatedPaymentOutcome {
  APPROVED, REJECTED, PENDING
}

data class PaymentReceipt(
  val transactionId: String,
  val orderId: String,
  val reference: String,
  val amountCop: Double,
  val status: SimulatedPaymentOutcome,
  val authorizationCode: String,
  val timestamp: String,
  val maskedCard: String,
  val statusMessage: String
)

class PaymentGatewayRepository {

  suspend fun processSandboxPayment(
    order: Order,
    cardNumber: String,
    cardHolder: String,
    cvv: String,
    expMonthYear: String,
    forcedOutcome: SimulatedPaymentOutcome? = null
  ): PaymentReceipt {
    // Simulate real gateway network latency
    delay(1800)

    val cleanCard = cardNumber.replace(" ", "").replace("-", "")
    val outcome = forcedOutcome ?: when {
      cleanCard.endsWith("0002") || cleanCard.endsWith("9999") -> SimulatedPaymentOutcome.REJECTED
      cleanCard.endsWith("0004") -> SimulatedPaymentOutcome.PENDING
      cleanCard.length >= 13 -> SimulatedPaymentOutcome.APPROVED
      else -> SimulatedPaymentOutcome.REJECTED
    }

    val txId = "TXN-${UUID.randomUUID().toString().take(10).uppercase()}"
    val authCode = if (outcome == SimulatedPaymentOutcome.APPROVED) {
      "AUTH-${(100000..999999).random()}"
    } else "REJ-00"

    val masked = if (cleanCard.length >= 4) {
      "•••• •••• •••• ${cleanCard.takeLast(4)}"
    } else "•••• •••• •••• 4242"

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(Date())

    val message = when (outcome) {
      SimulatedPaymentOutcome.APPROVED -> "Pago SIMULADO aprobado. No se realizó ningún cobro real."
      SimulatedPaymentOutcome.REJECTED -> "Pago SIMULADO rechazado (tarjeta de prueba inválida o terminada en 0002/9999)."
      SimulatedPaymentOutcome.PENDING -> "Pago SIMULADO pendiente (tarjeta de prueba terminada en 0004). No hay banco real involucrado."
    }

    return PaymentReceipt(
      transactionId = txId,
      orderId = order.id,
      reference = order.paymentReference,
      amountCop = order.totalCop,
      status = outcome,
      authorizationCode = authCode,
      timestamp = formattedDate,
      maskedCard = masked,
      statusMessage = message
    )
  }
}

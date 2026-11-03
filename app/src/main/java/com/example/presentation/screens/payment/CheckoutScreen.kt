package com.example.presentation.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CartRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PaymentGatewayRepository
import com.example.data.repository.PaymentReceipt
import com.example.data.repository.ProductRepository
import com.example.data.repository.SimulatedPaymentOutcome
import com.example.domain.model.CartSummary
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CheckoutScreen(
  cartSummary: CartSummary,
  deliveryAddress: String,
  customerPhone: String,
  currentUserName: String,
  orderRepository: OrderRepository,
  productRepository: ProductRepository,
  cartRepository: CartRepository,
  onOrderCompleted: () -> Unit,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val paymentGateway = remember { PaymentGatewayRepository() }

  var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
  var cardHolder by remember { mutableStateOf(currentUserName.ifBlank { "Titular de prueba" }) }
  var expDate by remember { mutableStateOf("12/28") }
  var cvv by remember { mutableStateOf("123") }
  var selectedProvider by remember { mutableStateOf("Simulación estilo Wompi") }

  var isProcessing by remember { mutableStateOf(false) }
  var receipt by remember { mutableStateOf<PaymentReceipt?>(null) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val copFormat = remember {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
      maximumFractionDigits = 0
    }
  }

  fun executePayment(forcedOutcome: SimulatedPaymentOutcome? = null) {
    if (isProcessing) return
    isProcessing = true
    errorMessage = null

    scope.launch {
      try {
        // 0. Pre-validación de stock y precio en Firestore
        val validation = productRepository.validateCartItemsList(cartSummary.items)
        if (!validation.isValid) {
          errorMessage = validation.errorMessage
          isProcessing = false
          return@launch
        }

        // 1. Crear pedido inicial en PENDING y pago PENDING (sin tocar stock)
        val createdOrder = orderRepository.createOrder(
          customerName = cardHolder,
          deliveryAddress = deliveryAddress,
          customerPhone = customerPhone,
          items = cartSummary.items,
          subtotal = cartSummary.subtotalCop,
          shipping = cartSummary.shippingCop,
          total = cartSummary.totalCop
        )

        // 2. Procesar con la pasarela de pagos Sandbox
        val resultReceipt = paymentGateway.processSandboxPayment(
          order = createdOrder,
          cardNumber = cardNumber,
          cardHolder = cardHolder,
          cvv = cvv,
          expMonthYear = expDate,
          forcedOutcome = forcedOutcome
        )

        // 3. Flujo según resultado de la pasarela:
        when (resultReceipt.status) {
          SimulatedPaymentOutcome.APPROVED -> {
            orderRepository.confirmApprovedPayment(createdOrder, resultReceipt.transactionId)
            cartRepository.clearCart()
            receipt = resultReceipt
          }

          SimulatedPaymentOutcome.REJECTED -> {
            orderRepository.markOrderRejected(
              orderId = createdOrder.id,
              reference = resultReceipt.transactionId,
              amountCop = createdOrder.totalCop
            )
            receipt = resultReceipt
          }

          SimulatedPaymentOutcome.PENDING -> {
            // El pedido ya quedó creado en estado PENDING: se vacía el carrito para no duplicarlo al reintentar
            cartRepository.clearCart()
            receipt = resultReceipt.copy(
              statusMessage = "Pago pendiente de confirmación"
            )
          }
        }
      } catch (e: com.example.data.repository.StockOrPriceChangedException) {
        errorMessage = e.message ?: "Cambio imprevisto en stock o precio de los productos."
      } catch (e: Exception) {
        val stockCause = e.cause as? com.example.data.repository.StockOrPriceChangedException
        errorMessage = stockCause?.message ?: "Error al procesar el pago: ${e.localizedMessage}"
      } finally {
        isProcessing = false
      }
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Top bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (receipt == null) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
          }
        }
        Text(
          text = if (receipt == null) "Pago simulado (modo de pruebas)" else "Comprobante de Transacción",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // If receipt is ready, show Voucher
      if (receipt != null) {
        PaymentVoucherCard(
          receipt = receipt!!,
          formattedTotal = copFormat.format(receipt!!.amountCop),
          onDone = onOrderCompleted
        )
      } else {
        // Payment Form
        ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Total a Pagar:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = copFormat.format(cartSummary.totalCop),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Pasarela simulada (no se contacta al proveedor):",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              FilterChip(
                selected = selectedProvider.contains("Wompi"),
                onClick = { selectedProvider = "Simulación estilo Wompi" },
                label = { Text("Simula Wompi", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
              )
              FilterChip(
                selected = selectedProvider.contains("Mercado"),
                onClick = { selectedProvider = "Simulación estilo Mercado Pago" },
                label = { Text("Simula Mercado Pago", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Test presets
            Text(
              text = "Resultados de prueba simulados:",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedButton(
                onClick = {
                  cardNumber = "4242 4242 4242 4242"
                  executePayment(SimulatedPaymentOutcome.APPROVED)
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("test_approved_payment_btn"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Aprobar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {
                  cardNumber = "4000 0000 0000 0002"
                  executePayment(SimulatedPaymentOutcome.REJECTED)
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("test_rejected_payment_btn"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Rechazar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
              }

              OutlinedButton(
                onClick = {
                  cardNumber = "4000 0000 0000 0004"
                  executePayment(SimulatedPaymentOutcome.PENDING)
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("test_pending_payment_btn"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Pendiente", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            OutlinedTextField(
              value = cardNumber,
              onValueChange = { cardNumber = it },
              label = { Text("Número de Tarjeta") },
              leadingIcon = {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("card_number_input"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = cardHolder,
              onValueChange = { cardHolder = it },
              label = { Text("Nombre del Titular") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("card_holder_input"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = expDate,
                onValueChange = { expDate = it },
                label = { Text("MM/AA") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
              )
              OutlinedTextField(
                value = cvv,
                onValueChange = { cvv = it },
                label = { Text("CVV") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
              )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
              )
            }

            Button(
              onClick = { executePayment() },
              enabled = !isProcessing && cardNumber.isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_payment_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              if (isProcessing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Procesando pago seguro...")
              } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Confirmar Pago Seguro (${copFormat.format(cartSummary.totalCop)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "SIMULACIÓN • No se realiza ningún cobro ni se contacta a un banco",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentVoucherCard(
  receipt: PaymentReceipt,
  formattedTotal: String,
  onDone: () -> Unit
) {
  val statusColor = when (receipt.status) {
    SimulatedPaymentOutcome.APPROVED -> MaterialTheme.colorScheme.primary
    SimulatedPaymentOutcome.REJECTED -> MaterialTheme.colorScheme.error
    SimulatedPaymentOutcome.PENDING -> MaterialTheme.colorScheme.tertiary
  }

  val statusIcon = when (receipt.status) {
    SimulatedPaymentOutcome.APPROVED -> Icons.Filled.CheckCircle
    SimulatedPaymentOutcome.REJECTED -> Icons.Filled.Error
    SimulatedPaymentOutcome.PENDING -> Icons.Filled.HourglassTop
  }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("payment_voucher_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(statusColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(38.dp))
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = when (receipt.status) {
          SimulatedPaymentOutcome.APPROVED -> "¡PAGO EXITOSO!"
          SimulatedPaymentOutcome.REJECTED -> "TRANSACCIÓN DECLINADA"
          SimulatedPaymentOutcome.PENDING -> "PAGO PENDIENTE"
        },
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        color = statusColor
      )

      Text(
        text = receipt.statusMessage,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
      )

      HorizontalDivider()
      Spacer(modifier = Modifier.height(14.dp))

      VoucherRow(label = "Monto Total:", value = formattedTotal, isBold = true)
      VoucherRow(label = "ID Transacción:", value = receipt.transactionId, isMonospace = true)
      VoucherRow(label = "ID Pedido:", value = receipt.orderId, isMonospace = true)
      VoucherRow(label = "Código de Autorización:", value = receipt.authorizationCode, isMonospace = true)
      VoucherRow(label = "Fecha y Hora:", value = receipt.timestamp)
      VoucherRow(label = "Tarjeta:", value = receipt.maskedCard)

      Spacer(modifier = Modifier.height(22.dp))

      Button(
        onClick = onDone,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("voucher_finish_button")
      ) {
        Text("Finalizar y Volver a la Tienda", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
      }
    }
  }
}

@Composable
private fun VoucherRow(
  label: String,
  value: String,
  isBold: Boolean = false,
  isMonospace: Boolean = false
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      text = value,
      fontSize = 12.5.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
      fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

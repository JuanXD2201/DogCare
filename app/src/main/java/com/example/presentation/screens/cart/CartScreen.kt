package com.example.presentation.screens.cart

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.CartRepository
import com.example.domain.model.CartItem
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CartScreen(
  cartRepository: CartRepository,
  currentUserName: String?,
  onProceedToCheckout: (deliveryAddress: String, customerPhone: String) -> Unit,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val cartSummary by cartRepository.cartSummary.collectAsState()

  var deliveryAddress by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var showConfirmDialog by remember { mutableStateOf(false) }

  val copFormat = remember {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
      maximumFractionDigits = 0
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Header
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
          }
          Text(
            text = "Carrito de Compras (${cartSummary.totalItemsCount})",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
          )
          if (cartSummary.items.isNotEmpty()) {
            IconButton(onClick = { cartRepository.clearCart() }) {
              Icon(Icons.Filled.Delete, contentDescription = "Vaciar carrito", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      if (cartSummary.items.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(32.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.ShoppingCart,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(36.dp)
                )
              }
              Spacer(modifier = Modifier.height(18.dp))
              Text(
                text = "Tu carrito está vacío",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Explora nuestro catálogo de concentrados, alimentos húmedos y snacks saludables para tu perro.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(22.dp))
              Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(46.dp)
              ) {
                Text("Explorar la Tienda", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Free Shipping Progress Card
          item {
            val freeShippingThreshold = CartRepository.FREE_SHIPPING_THRESHOLD
            val progress = (cartSummary.subtotalCop / freeShippingThreshold).toFloat().coerceIn(0f, 1f)
            val qualifiesForFreeShipping = cartSummary.subtotalCop >= freeShippingThreshold

            ElevatedCard(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.elevatedCardColors(
                containerColor = if (qualifiesForFreeShipping) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (qualifiesForFreeShipping) Icons.Filled.CheckCircle else Icons.Filled.LocalShipping,
                      contentDescription = null,
                      tint = if (qualifiesForFreeShipping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (qualifiesForFreeShipping) "¡Calificas para Envío GRATIS!" else "Envío GRATIS en pedidos > $90.000 COP",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = if (qualifiesForFreeShipping) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }

                if (!qualifiesForFreeShipping) {
                  Spacer(modifier = Modifier.height(8.dp))
                  LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  val diff = freeShippingThreshold - cartSummary.subtotalCop
                  Text(
                    text = "Agrega ${copFormat.format(diff)} más para obtener envío gratuito.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // Cart Item Rows
          items(cartSummary.items, key = { it.product.id }) { item ->
            CartItemRow(
              item = item,
              formattedSubtotal = copFormat.format(item.subtotal),
              formattedUnit = copFormat.format(item.product.priceCop),
              onIncrease = {
                val res = cartRepository.updateQuantity(item.product.id, item.quantity + 1)
                if (res.isFailure) {
                  scope.launch {
                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error al actualizar cantidad")
                  }
                }
              },
              onDecrease = {
                cartRepository.updateQuantity(item.product.id, item.quantity - 1)
              },
              onRemove = {
                cartRepository.removeFromCart(item.product.id)
              }
            )
          }

          // Delivery Info Card
          item {
            ElevatedCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Datos de Entrega en Colombia",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                  value = deliveryAddress,
                  onValueChange = { deliveryAddress = it },
                  label = { Text("Dirección de Envío") },
                  placeholder = { Text("Ej: Calle 100 # 15-20 Apto 301, Bogotá") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_address_input"),
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                  )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                  value = customerPhone,
                  onValueChange = { customerPhone = it },
                  label = { Text("Teléfono de Contacto") },
                  placeholder = { Text("Ej: 310 123 4567") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input"),
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                  )
                )
              }
            }
          }

          // Cost Breakdown Card
          item {
            ElevatedCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Resumen de la Orden",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Subtotal productos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(copFormat.format(cartSummary.subtotalCop), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Costo de Envío", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(
                    text = if (cartSummary.shippingCop == 0.0) "GRATIS" else copFormat.format(cartSummary.shippingCop),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cartSummary.shippingCop == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Total a Pagar", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                  Text(
                    text = copFormat.format(cartSummary.totalCop),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }

          item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Bottom Proceed to Checkout Bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Button(
              onClick = {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                  scope.launch {
                    snackbarHostState.showSnackbar("Inicia sesión para completar tu compra.")
                  }
                  return@Button
                }
                if (deliveryAddress.isBlank() || customerPhone.isBlank()) {
                  scope.launch {
                    snackbarHostState.showSnackbar("Por favor ingresa tu dirección de entrega y teléfono.")
                  }
                  return@Button
                }
                showConfirmDialog = true
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("proceed_checkout_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Proceder al Pago (${copFormat.format(cartSummary.totalCop)})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp
              )
            }
          }
        }
      }
    }

    // Confirmation Dialog before opening Sandbox Payment Screen
    if (showConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        icon = {
          Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Confirmar Orden de Compra", fontWeight = FontWeight.Bold) },
        text = {
          Column {
            Text(
              text = "Total a cancelar: ${copFormat.format(cartSummary.totalCop)}",
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Destino: $deliveryAddress")
            Text("Teléfono: $customerPhone")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "El stock de los productos se verificará y descontará de forma atómica una vez tu pago simulado sea aprobado.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              showConfirmDialog = false
              onProceedToCheckout(deliveryAddress, customerPhone)
            }
          ) {
            Text("Ir a Pagar")
          }
        },
        dismissButton = {
          TextButton(onClick = { showConfirmDialog = false }) {
            Text("Cancelar")
          }
        }
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 80.dp)
    )
  }
}

@Composable
private fun CartItemRow(
  item: CartItem,
  formattedSubtotal: String,
  formattedUnit: String,
  onIncrease: () -> Unit,
  onDecrease: () -> Unit,
  onRemove: () -> Unit
) {
  ElevatedCard(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        if (item.product.imageUrl.isNotBlank()) {
          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
              .data(item.product.imageUrl)
              .crossfade(true)
              .build(),
            contentDescription = item.product.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          Icon(Icons.Filled.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.product.name,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )
        Text(
          text = "$formattedUnit c/u",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = formattedSubtotal,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }

      // Quantity controls
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(horizontal = 4.dp, vertical = 2.dp)
      ) {
        IconButton(
          onClick = onDecrease,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(
            imageVector = if (item.quantity == 1) Icons.Filled.Delete else Icons.Filled.Remove,
            contentDescription = "Disminuir",
            modifier = Modifier.size(16.dp),
            tint = if (item.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "${item.quantity}",
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp,
          modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(
          onClick = onIncrease,
          modifier = Modifier.size(30.dp)
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

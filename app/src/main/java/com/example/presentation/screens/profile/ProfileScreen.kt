package com.example.presentation.screens.profile

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.repository.AuthRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ProductRepository
import com.example.domain.model.Order
import com.example.domain.model.Pet
import com.example.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
  authRepository: AuthRepository,
  petRepository: PetRepository,
  orderRepository: OrderRepository,
  productRepository: ProductRepository,
  userRole: String? = "user",
  currentUser: FirebaseUser?,
  onSignOut: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val pets by if (currentUser != null) {
    petRepository.observePets().collectAsState(initial = emptyList())
  } else {
    remember { mutableStateOf(emptyList()) }
  }

  val orders by if (currentUser != null) {
    orderRepository.observeUserOrders().collectAsState(initial = emptyList())
  } else {
    remember { mutableStateOf(emptyList()) }
  }

  val profileName by if (currentUser != null) {
    authRepository.observeUserProfile(currentUser.uid).collectAsState(initial = null)
  } else {
    remember { mutableStateOf<UserProfile?>(null) }
  }

  // Mensaje de resultado de acciones del perfil (sincronizar, editar, registrar perro)
  var migrationResult by remember { mutableStateOf<String?>(null) }
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showAddPetDialog by remember { mutableStateOf(false) }

  val copFormat = remember {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
      maximumFractionDigits = 0
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // User Header Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(42.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = profileName?.displayName?.takeIf { it.isNotBlank() }
                ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: if (currentUser == null) "Usuario Invitado" else "Agrega tu nombre",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
              onClick = { showEditProfileDialog = true },
              modifier = Modifier.size(28.dp).testTag("edit_profile_btn")
            ) {
              Icon(Icons.Filled.Edit, contentDescription = "Editar nombre", modifier = Modifier.size(16.dp))
            }
          }

          Text(
            text = currentUser?.email ?: "Modo Demostración",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))


          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                authRepository.signOut(scope) {
                  onSignOut()
                }
              },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("sign_out_button")
            ) {
              Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cerrar Sesión", fontSize = 12.5.sp)
            }

            Button(
              onClick = {
                scope.launch {
                  val isAdminRole = userRole?.trim()?.lowercase() in listOf("admin", "administrador")
                  if (isAdminRole) {
                    migrationResult = authRepository.migratePublicProfiles().second
                  } else {
                    // Un usuario normal solo puede sincronizar su propio perfil
                    val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    migrationResult = if (me == null) {
                      "Inicia sesión para sincronizar tu perfil."
                    } else try {
                      authRepository.syncUserProfile(me)
                      "Perfil público sincronizado."
                    } catch (e: Exception) {
                      "No se pudo sincronizar el perfil. Revisa tu conexión."
                    }
                  }
                }
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("migrate_public_profiles_btn")
            ) {
              Text("Sincronizar Perfil Público", fontSize = 11.5.sp)
            }
          }

          if (migrationResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = migrationResult!!,
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Panel de consulta de usuarios para cuentas con rol admin (la gestión de productos es por vendedor)
    if (userRole?.trim()?.lowercase() in listOf("admin", "administrador")) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_panel_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Panel de Administrador", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Consulta de usuarios registrados. La tienda ya no requiere rol de administrador: cada usuario gestiona sus propios productos.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var registeredUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
            var isLoadingUsers by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
              isLoadingUsers = true
              registeredUsers = authRepository.getAllUsersForAdmin()
              isLoadingUsers = false
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Usuarios Registrados en Firestore (${registeredUsers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              IconButton(
                onClick = {
                  scope.launch {
                    isLoadingUsers = true
                    registeredUsers = authRepository.getAllUsersForAdmin()
                    isLoadingUsers = false
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualizar usuarios", modifier = Modifier.size(16.dp))
              }
            }

            if (isLoadingUsers) {
              LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            } else if (registeredUsers.isEmpty()) {
              Text(
                text = "No se encontraron usuarios en la colección /users de Firestore.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                registeredUsers.take(10).forEach { user ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName.ifBlank { "Sin nombre" }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(user.email.ifBlank { "ID: ${user.userId.take(8)}..." }, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (user.role.equals("admin", ignoreCase = true) || user.role.equals("administrador", ignoreCase = true))
                          MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                      ) {
                        Text(
                          text = user.role.uppercase(),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          color = if (user.role.equals("admin", ignoreCase = true) || user.role.equals("administrador", ignoreCase = true))
                            MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Pets Section Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Mis Perros Registrados", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            IconButton(
              onClick = { showAddPetDialog = true },
              modifier = Modifier.testTag("add_pet_btn")
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Agregar Perro", tint = MaterialTheme.colorScheme.primary)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (pets.isEmpty()) {
            Text(
              text = "No has registrado perros aún. Agrega el perfil de tu mascota para calcular su edad y recibir recomendaciones de nutrición.",
              fontSize = 12.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            pets.forEach { pet ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "🐶 ${pet.name}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.5.sp,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = "${pet.ageYears} año(s) • ~${pet.humanAgeYears.toInt()} años hum.",
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  Text(
                    text = "Raza: ${pet.breed} | Peso: ${pet.weightKg} kg | Talla: ${when (pet.sizeCategory.lowercase()) { "small" -> "Pequeño"; "medium" -> "Mediano"; "large" -> "Grande"; "giant" -> "Gigante"; else -> pet.sizeCategory }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  if (pet.notes.isNotBlank()) {
                    Text(
                      text = "Notas: ${pet.notes}",
                      fontSize = 11.5.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Orders History Title
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Historial de Pedidos (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }

    if (orders.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text(
              text = "Aún no has realizado pedidos de alimentos caninos.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(orders, key = { it.id }) { order ->
        OrderRecordCard(order = order, formattedTotal = copFormat.format(order.totalCop))
      }
    }

    item { Spacer(modifier = Modifier.height(24.dp)) }
  }

  // Edit Name Dialog
  if (showEditProfileDialog) {
    var editNameText by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var editPhoneText by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text("Editar Perfil de Usuario", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = editNameText,
            onValueChange = { editNameText = it },
            label = { Text("Nombre Completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = editPhoneText,
            onValueChange = { editPhoneText = it },
            label = { Text("Teléfono de Contacto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editNameText.trim().isNotBlank()) {
              if (editNameText.trim().length > 100 || editPhoneText.trim().length > 30) {
                migrationResult = "El nombre admite hasta 100 caracteres y el teléfono hasta 30."
              } else {
                showEditProfileDialog = false
                scope.launch {
                  val saved = authRepository.updateUserProfile(editNameText.trim(), editPhoneText.trim())
                  migrationResult = if (saved) "Perfil actualizado." else "No se pudo guardar el perfil. Inicia sesión e inténtalo de nuevo."
                }
              }
            }
          }
        ) {
          Text("Guardar Cambios")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Add Pet Dialog
  if (showAddPetDialog) {
    var petName by remember { mutableStateOf("") }
    var petBreed by remember { mutableStateOf("") }
    var petAgeText by remember { mutableStateOf("") }
    var petSizeKgText by remember { mutableStateOf("") }
    var petFeatures by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showAddPetDialog = false },
      title = { Text("Registrar Nuevo Perro 🐶", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = petName,
            onValueChange = { petName = it },
            label = { Text("Nombre de la Mascota") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = petBreed,
            onValueChange = { petBreed = it },
            label = { Text("Raza (ej: Labrador, Mestizo)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = petAgeText,
              onValueChange = { petAgeText = it },
              label = { Text("Edad (años)") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = petSizeKgText,
              onValueChange = { petSizeKgText = it },
              label = { Text("Peso (kg)") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }
          OutlinedTextField(
            value = petFeatures,
            onValueChange = { petFeatures = it },
            label = { Text("Características y Salud") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (petName.trim().isNotBlank() && petAgeText.toDoubleOrNull() != null && petSizeKgText.toDoubleOrNull()?.let { it > 0 } == true) {
              val age = petAgeText.toDoubleOrNull() ?: 1.0
              val weight = petSizeKgText.toDoubleOrNull() ?: 10.0
              val sizeCat = if (weight < 10) "small" else if (weight < 25) "medium" else "large"
              showAddPetDialog = false
              scope.launch {
                try {
                  if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                    migrationResult = "Inicia sesión para registrar un perro."
                  } else if (petName.trim().length > 50) {
                    migrationResult = "El nombre del perro admite hasta 50 caracteres."
                  } else {
                    petRepository.addPet(
                      name = petName.trim(),
                      breed = petBreed.trim(),
                      sizeCategory = sizeCat,
                      ageYears = age.toInt(),
                      ageMonths = 0,
                      weightKg = weight,
                      notes = petFeatures.trim()
                    )
                    migrationResult = "Perro registrado."
                  }
                } catch (e: kotlinx.coroutines.CancellationException) {
                  throw e
                } catch (e: Exception) {
                  migrationResult = "No se pudo registrar al perro. Revisa tu conexión e inténtalo de nuevo."
                }
              }
            }
          }
        ) {
          Text("Registrar Perro")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddPetDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }
}

@Composable
private fun OrderRecordCard(order: Order, formattedTotal: String) {
  val isApproved = order.paymentStatus == "APPROVED"
  val isRejected = order.paymentStatus in listOf("REJECTED", "DECLINED", "ERROR")

  val statusColor = when {
    isApproved -> MaterialTheme.colorScheme.primary
    isRejected -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.tertiary
  }

  val statusLabel = when (order.paymentStatus) {
    "APPROVED" -> "Aprobado"
    "REJECTED", "DECLINED" -> "Rechazado"
    "ERROR" -> "Error en el pago"
    else -> "Pendiente"
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Pedido #${order.id.takeLast(6).uppercase()}",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(statusColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = statusLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Entrega: ${order.deliveryAddress}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = "${order.items.size} producto(s): ${order.items.joinToString { it.productName }}",
        fontSize = 12.sp,
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Total:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
          text = formattedTotal,
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}

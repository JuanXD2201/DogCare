package com.example.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CartRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.UserChatRepository
import com.example.data.repository.VetClinicRepository
import com.example.presentation.components.DogCareBottomBar
import com.example.presentation.components.DogCareTab
import com.example.presentation.components.DogCareTopBar
import com.example.presentation.screens.agecalc.AgeCalculatorScreen
import com.example.presentation.screens.aichat.AiChatScreen
import com.example.presentation.screens.aichat.initialAiMessages
import com.example.presentation.screens.auth.AuthScreen
import com.example.presentation.screens.cart.CartScreen
import com.example.presentation.screens.map.VetMapScreen
import com.example.presentation.screens.payment.CheckoutScreen
import com.example.presentation.screens.profile.ProfileScreen
import com.example.presentation.screens.store.StoreScreen
import com.example.presentation.screens.userchat.UserChatScreen
import com.google.firebase.auth.FirebaseUser

enum class AppSubScreen {
  NONE, CART, CHECKOUT, PROFILE
}

@Composable
fun DogCareApp(
  authRepository: AuthRepository,
  petRepository: PetRepository,
  productRepository: ProductRepository,
  cartRepository: CartRepository,
  orderRepository: OrderRepository,
  aiRepository: AiAssistantRepository,
  vetRepository: VetClinicRepository,
  userChatRepository: UserChatRepository
) {
  val authUser by authRepository.authStateFlow().collectAsState(initial = authRepository.currentUser)
  var isGuestMode by remember { mutableStateOf(false) }

  var currentTab by remember { mutableStateOf(DogCareTab.CALCULATOR) }
  var subScreen by remember { mutableStateOf(AppSubScreen.NONE) }
  // El botón Atrás del sistema cierra primero la subpantalla (perfil, carrito, pago) en vez de salir de la app
  androidx.activity.compose.BackHandler(enabled = subScreen != AppSubScreen.NONE) {
    subScreen = AppSubScreen.NONE
  }
  var pendingDeliveryAddress by remember { mutableStateOf("") }
  var pendingCustomerPhone by remember { mutableStateOf("") }
  var userRole by remember { mutableStateOf("user") }
  // El historial del chat IA se reinicia solo al cambiar de usuario
  val aiMessages = remember(authUser?.uid) { initialAiMessages() }

  val cartSummary by cartRepository.cartSummary.collectAsState()

  LaunchedEffect(authUser?.uid) {
    val uid = authUser?.uid
    if (uid != null) {
      // Al iniciar sesión, AuthScreen sale de composición y cancelaba su propia sincronización del perfil:
      // se hace aquí, que vive mientras haya sesión.
      authUser?.let { user ->
        try {
          authRepository.syncUserProfile(user)
        } catch (e: kotlinx.coroutines.CancellationException) {
          throw e
        } catch (e: Exception) {
          android.util.Log.w("DogCareApp", "No se pudo sincronizar el perfil: ${e.message}")
        }
      }
      cartRepository.loadCartForUser(uid)
      authRepository.observeUserProfile(uid).collect { profile ->
        if (profile != null) {
          userRole = profile.role
        }
      }
    } else {
      userRole = "user"
      // El carrito es un singleton: sin esto, el siguiente usuario heredaba los productos del anterior
      cartRepository.clearCart()
    }
  }

  // Authentication gate
  if (authUser == null && !isGuestMode) {
    AuthScreen(
      authRepository = authRepository,
      onAuthSuccess = { /* Automatically handled by authStateFlow */ },
      onContinueAsGuest = { isGuestMode = true }
    )
    return
  }

  // Active user session or guest mode
  val currentUid = authUser?.uid
  val currentUserName = authUser?.displayName ?: "Usuario Invitado"

  Scaffold(
    topBar = {
      if (subScreen == AppSubScreen.NONE) {
        DogCareTopBar(
          title = when (currentTab) {
            DogCareTab.CALCULATOR -> "Calculadora de Edad Canina"
            DogCareTab.STORE -> "Tienda de Alimentos Caninos"
            DogCareTab.AI_CHAT -> "Asistente Veterinario IA"
            DogCareTab.VET_MAP -> "Veterinarias y Urgencias 24h"
            DogCareTab.COMMUNITY -> "Comunidad de Mascotas"
          },
          cartItemCount = cartSummary.totalItemsCount,
          onCartClick = { subScreen = AppSubScreen.CART },
          actions = {
            IconButton(
              onClick = { subScreen = AppSubScreen.PROFILE },
              modifier = Modifier.testTag("topbar_profile_button")
            ) {
              Icon(Icons.Filled.AccountCircle, contentDescription = "Mi Perfil")
            }
          }
        )
      }
    },
    bottomBar = {
      if (subScreen == AppSubScreen.NONE) {
        DogCareBottomBar(
          currentTab = currentTab,
          onTabSelected = { selected ->
            currentTab = selected
            subScreen = AppSubScreen.NONE
          },
          cartBadgeCount = cartSummary.totalItemsCount
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (subScreen) {
        AppSubScreen.CART -> {
          CartScreen(
            cartRepository = cartRepository,
            currentUserName = currentUserName,
            onProceedToCheckout = { address, phone ->
              pendingDeliveryAddress = address
              pendingCustomerPhone = phone
              subScreen = AppSubScreen.CHECKOUT
            },
            onBack = { subScreen = AppSubScreen.NONE }
          )
        }

        AppSubScreen.CHECKOUT -> {
          CheckoutScreen(
            cartSummary = cartSummary,
            deliveryAddress = pendingDeliveryAddress,
            customerPhone = pendingCustomerPhone,
            currentUserName = currentUserName,
            orderRepository = orderRepository,
            productRepository = productRepository,
            cartRepository = cartRepository,
            onOrderCompleted = {
              subScreen = AppSubScreen.NONE
              currentTab = DogCareTab.STORE
            },
            onBack = { subScreen = AppSubScreen.CART }
          )
        }

        AppSubScreen.PROFILE -> {
          ProfileScreen(
            authRepository = authRepository,
            petRepository = petRepository,
            orderRepository = orderRepository,
            productRepository = productRepository,
            userRole = userRole,
            currentUser = authUser,
            onSignOut = {
              isGuestMode = false
              subScreen = AppSubScreen.NONE
              currentTab = DogCareTab.CALCULATOR
            }
          )
        }

        AppSubScreen.NONE -> {
          Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
            when (tab) {
              DogCareTab.CALCULATOR -> {
                AgeCalculatorScreen(
                  petRepository = petRepository,
                  currentUserId = currentUid
                )
              }

              DogCareTab.STORE -> {
                StoreScreen(
                  productRepository = productRepository,
                  cartRepository = cartRepository,
                  userRole = userRole,
                  onOpenCart = { subScreen = AppSubScreen.CART }
                )
              }

              DogCareTab.AI_CHAT -> {
                AiChatScreen(
                  messages = aiMessages,
                  aiRepository = aiRepository,
                  onNavigateToVetMap = { currentTab = DogCareTab.VET_MAP }
                )
              }

              DogCareTab.VET_MAP -> {
                VetMapScreen(
                  vetRepository = vetRepository
                )
              }

              DogCareTab.COMMUNITY -> {
                UserChatScreen(
                  chatRepository = userChatRepository,
                  currentUserId = currentUid
                )
              }
            }
          }
        }
      }
    }
  }
}

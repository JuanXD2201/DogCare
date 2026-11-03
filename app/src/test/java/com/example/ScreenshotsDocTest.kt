package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CartRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.UserChatRepository
import com.example.data.repository.VetClinicRepository
import com.example.domain.model.AiChatMessage
import com.example.domain.model.CartItem
import com.example.domain.model.CartSummary
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.Order
import com.example.domain.model.OrderItemRecord
import com.example.domain.model.Pet
import com.example.domain.model.Product
import com.example.domain.model.UserProfile
import com.example.presentation.screens.agecalc.AgeCalculatorScreen
import com.example.presentation.screens.aichat.AiChatScreen
import com.example.presentation.screens.auth.AuthScreen
import com.example.presentation.screens.cart.CartScreen
import com.example.presentation.screens.map.VetMapScreen
import com.example.presentation.screens.payment.CheckoutScreen
import com.example.presentation.screens.profile.ProfileScreen
import com.example.presentation.screens.store.StoreScreen
import com.example.presentation.screens.userchat.UserChatScreen
import com.example.ui.theme.DogCareTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Genera las capturas de documentación renderizando los Composables REALES de la app con
 * repositorios de demostración (datos ficticios, sin red ni Firebase real).
 * No son capturas de un dispositivo ni de un emulador: son renders JVM (Robolectric + Roborazzi).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class ScreenshotsDocTest {

  @get:Rule val rule = createComposeRule()

  private lateinit var ctx: Context
  private val uid = "demo-uid-1"

  // ---------- Datos de demostración (ficticios) ----------
  private val demoProducts = listOf(
    Product(id = "p1", sellerUid = "otro", sellerName = "Tienda Demo", name = "Concentrado adulto raza mediana", brand = "Marca Demo",
      category = "dry_food", lifeStage = "adult", presentation = "4 kg", priceCop = 68000.0, stock = 12,
      description = "Alimento seco de demostración para perros adultos."),
    Product(id = "p2", sellerUid = "otro", sellerName = "Tienda Demo", name = "Concentrado cachorro", brand = "Marca Demo",
      category = "dry_food", lifeStage = "puppy", presentation = "2 kg", priceCop = 45000.0, stock = 3,
      description = "Alimento seco de demostración para cachorros."),
    Product(id = "p3", sellerUid = "otro", sellerName = "Tienda Demo", name = "Alimento húmedo de pollo", brand = "Marca Demo",
      category = "wet_food", lifeStage = "all_stages", presentation = "Lata 330 g", priceCop = 9500.0, stock = 30,
      description = "Lata de demostración."),
    Product(id = "p4", sellerUid = "otro", sellerName = "Tienda Demo", name = "Snack dental", brand = "Marca Demo",
      category = "snacks", lifeStage = "adult", presentation = "Bolsa 200 g", priceCop = 14000.0, stock = 0,
      description = "Snack de demostración sin existencias.")
  )

  private val demoOrder = Order(
    id = "order-demo-1", userId = uid, customerName = "Usuario Demo", deliveryAddress = "Calle 1 # 2-3, Cali",
    customerPhone = "3000000000",
    items = listOf(OrderItemRecord("p1", "Concentrado adulto raza mediana", 68000.0, 1, 68000.0)),
    quantities = mapOf("p1" to 1), subtotalCop = 68000.0, shippingCop = 8500.0, totalCop = 76500.0,
    paymentStatus = "APPROVED", paymentReference = "REF-DEMO-0001"
  )

  // ---------- Repositorios de demostración ----------
  private inner class DemoProducts : ProductRepository(ctx) {
    override fun observeProducts(): Flow<List<Product>> = flowOf(demoProducts)
  }

  private inner class DemoPets : PetRepository(ctx) {
    override fun observePets(): Flow<List<Pet>> =
      flowOf(listOf(Pet(id = "pet1", userId = uid, name = "Luna", breed = "Mestizo", sizeCategory = "medium",
        ageYears = 4, ageMonths = 2, weightKg = 14.0, humanAgeYears = 32.0)))
  }

  private inner class DemoOrders : OrderRepository(ctx) {
    override fun observeUserOrders(): Flow<List<Order>> = flowOf(listOf(demoOrder))
  }

  private inner class DemoChat : UserChatRepository(ctx) {
    override fun observeUsersDirectory(): Flow<List<UserProfile>> = flowOf(
      listOf(UserProfile(userId = "u2", displayName = "Ana (demo)"), UserProfile(userId = "u3", displayName = "Carlos (demo)"))
    )
    override fun observeUserConversations(): Flow<List<Conversation>> = flowOf(
      listOf(Conversation(id = "c1", participantUids = listOf(uid, "u2"),
        participantNames = mapOf(uid to "Usuario Demo", "u2" to "Ana (demo)"),
        lastMessageText = "¿Qué alimento le das a tu perro?", lastSenderUid = "u2"))
    )
    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flowOf(
      listOf(ChatMessage("m1", conversationId, "u2", "Ana (demo)", "Hola, ¿qué alimento le das a tu perro?"),
        ChatMessage("m2", conversationId, uid, "Usuario Demo", "Concentrado para adultos, funciona muy bien."))
    )
    override fun observeBlockedUids(): Flow<Set<String>> = flowOf(emptySet())
  }

  @Before
  fun setup() {
    ctx = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(
        ctx,
        FirebaseOptions.Builder().setApplicationId("1:1:android:demo").setApiKey("demo-key").setProjectId("demo-project").build()
      )
    }
  }

  private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
    rule.setContent { DogCareTheme { content() } }
    rule.waitForIdle()
    rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/doc/$name.png")
  }

  @Test fun login() = shot("01_login") {
    AuthScreen(authRepository = AuthRepository(ctx), onAuthSuccess = {}, onContinueAsGuest = {})
  }

  @Test fun calculadoraEdad() = shot("02_calculadora_edad") {
    AgeCalculatorScreen(petRepository = DemoPets(), currentUserId = uid)
  }

  @Test fun chatIa() {
    val msgs = mutableStateListOf(
      AiChatMessage(isUser = true, text = "¿Cada cuánto debo bañar a mi perro?"),
      AiChatMessage(isUser = false, text = "Demostración de interfaz: en general cada 4 a 8 semanas, según el pelaje y la actividad. " +
        "Esta orientación no sustituye la valoración de un médico veterinario."),
      AiChatMessage(isUser = true, text = "Mi perro comió chocolate"),
      AiChatMessage(isUser = false, text = "Posible urgencia: contacta a un veterinario de inmediato.", isUrgent = true)
    )
    shot("03_chat_ia") { AiChatScreen(aiRepository = AiAssistantRepository(), messages = msgs, onNavigateToVetMap = {}) }
  }

  @Test fun veterinarias() = shot("04_veterinarias") { VetMapScreen(vetRepository = VetClinicRepository(ctx)) }

  @Test fun tienda() = shot("05_tienda") {
    StoreScreen(productRepository = DemoProducts(), cartRepository = CartRepository(ctx), userRole = "user", onOpenCart = {})
  }

  @Test fun carrito() {
    val cart = CartRepository(ctx)
    cart.addToCart(demoProducts[0], 1)
    cart.addToCart(demoProducts[2], 3)
    shot("06_carrito") {
      CartScreen(cartRepository = cart, currentUserName = "Usuario Demo", onProceedToCheckout = { _, _ -> }, onBack = {})
    }
  }

  @Test fun pago() {
    val items = listOf(CartItem(demoProducts[0], 1), CartItem(demoProducts[2], 3))
    val summary = CartSummary(items, 96500.0, 8500.0, 105000.0, 4)
    shot("07_pago_simulado") {
      CheckoutScreen(summary, "Calle 1 # 2-3, Cali", "3000000000", "Usuario Demo", DemoOrders(), DemoProducts(), CartRepository(ctx), {}, {})
    }
  }

  @Test fun chatUsuarios() = shot("08_chat_usuarios") { UserChatScreen(chatRepository = DemoChat(), currentUserId = uid) }

  @Test fun perfil() = shot("09_perfil_invitado") {
    ProfileScreen(AuthRepository(ctx), DemoPets(), DemoOrders(), DemoProducts(), "user", null, {})
  }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CartRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.PetRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.UserChatRepository
import com.example.data.repository.VetClinicRepository
import com.example.data.util.configureAppCheck
import com.example.presentation.DogCareApp
import com.example.ui.theme.DogCareTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    // App Check MUST be configured in onCreate() with launch intent before Compose or ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    // Initialize application repositories
    val authRepository = AuthRepository(applicationContext)
    val petRepository = PetRepository(applicationContext)
    val productRepository = ProductRepository(applicationContext)
    val cartRepository = CartRepository.instance.apply {
      initContext(applicationContext)
    }
    val orderRepository = OrderRepository(applicationContext)
    val aiRepository = AiAssistantRepository()
    val vetRepository = VetClinicRepository(applicationContext)
    val userChatRepository = UserChatRepository(applicationContext)

    setContent {
      DogCareTheme {
        DogCareApp(
          authRepository = authRepository,
          petRepository = petRepository,
          productRepository = productRepository,
          cartRepository = cartRepository,
          orderRepository = orderRepository,
          aiRepository = aiRepository,
          vetRepository = vetRepository,
          userChatRepository = userChatRepository
        )
      }
    }
  }
}

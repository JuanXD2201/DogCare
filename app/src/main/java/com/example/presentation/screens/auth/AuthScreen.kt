package com.example.presentation.screens.auth

import android.app.Activity
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  authRepository: AuthRepository,
  onAuthSuccess: (FirebaseUser) -> Unit,
  onContinueAsGuest: () -> Unit
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()

  var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
  var nameText by remember { mutableStateOf("") }
  var emailText by remember { mutableStateOf("") }
  var passwordText by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var showResetDialog by remember { mutableStateOf(false) }
  var resetEmailText by remember { mutableStateOf("") }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Logo
      Box(
        modifier = Modifier
          .size(90.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.dogcare_logo_1792629369949),
          contentDescription = "DogCare Logo",
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "DogCare",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary
      )

      Text(
        text = "Cuidado integral, salud y nutrición canina",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Tab Row
          TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
          ) {
            Tab(
              selected = selectedTabIndex == 0,
              onClick = {
                selectedTabIndex = 0
                errorMessage = null
                successMessage = null
              },
              text = { Text("Iniciar sesión", fontWeight = FontWeight.Bold) },
              modifier = Modifier.testTag("tab_login")
            )
            Tab(
              selected = selectedTabIndex == 1,
              onClick = {
                selectedTabIndex = 1
                errorMessage = null
                successMessage = null
              },
              text = { Text("Registrarse", fontWeight = FontWeight.Bold) },
              modifier = Modifier.testTag("tab_register")
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Error and Success Messages
          if (errorMessage != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
            ) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          if (successMessage != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp)
            ) {
              Text(
                text = successMessage!!,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          // Registration extra field: Name
          if (selectedTabIndex == 1) {
            OutlinedTextField(
              value = nameText,
              onValueChange = { nameText = it },
              label = { Text("Nombre completo") },
              leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_name"),
              shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          // Email field
          OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
              imeAction = ImeAction.Next
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_email"),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Password field
          OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_password"),
            shape = RoundedCornerShape(12.dp)
          )

          if (selectedTabIndex == 0) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(
                onClick = {
                  resetEmailText = emailText
                  showResetDialog = true
                },
                modifier = Modifier.testTag("forgot_password_button")
              ) {
                Text(
                  text = "¿Olvidaste tu contraseña?",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.height(8.dp))
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Action Submit Button
          Button(
            onClick = {
              errorMessage = null
              successMessage = null
              val email = emailText.trim()
              val password = passwordText.trim()

              if (email.isEmpty() || !email.contains("@")) {
                errorMessage = "Por favor ingresa un correo electrónico válido."
                return@Button
              }
              if (password.length < 6) {
                errorMessage = "La contraseña debe tener al menos 6 caracteres."
                return@Button
              }

              if (selectedTabIndex == 1) {
                val name = nameText.trim()
                if (name.isEmpty()) {
                  errorMessage = "Por favor ingresa tu nombre completo."
                  return@Button
                }
                isLoading = true
                scope.launch {
                  val result = authRepository.signUpWithEmail(name, email, password)
                  isLoading = false
                  result.fold(
                    onSuccess = { user ->
                      onAuthSuccess(user)
                    },
                    onFailure = { err ->
                      errorMessage = err.localizedMessage ?: "Error al crear la cuenta"
                    }
                  )
                }
              } else {
                isLoading = true
                scope.launch {
                  val result = authRepository.signInWithEmail(email, password)
                  isLoading = false
                  result.fold(
                    onSuccess = { user ->
                      onAuthSuccess(user)
                    },
                    onFailure = { err ->
                      errorMessage = "Correo o contraseña incorrectos. Verifica tus datos."
                    }
                  )
                }
              }
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("auth_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = if (selectedTabIndex == 0) "Iniciar sesión" else "Crear mi cuenta",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
              text = "  o  ",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Google Sign-In Button
          OutlinedButton(
            onClick = {
              if (activity != null) {
                isLoading = true
                errorMessage = null
                scope.launch {
                  authRepository.signInWithGoogle(
                    activity = activity,
                    onSuccess = { user ->
                      isLoading = false
                      onAuthSuccess(user)
                    },
                    onError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    onCancelled = {
                      isLoading = false
                    }
                  )
                }
              }
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Continuar con Google",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Demo Mode Button
          TextButton(
            onClick = onContinueAsGuest,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("continue_as_guest_button")
          ) {
            Text(
              text = "Explorar como invitado (solo lectura)",
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Protegido con Firebase Authentication & Cloud Firestore",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  // Dialog for Password Reset
  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Recuperar Contraseña", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Ingresa tu correo para recibir las instrucciones de restablecimiento:")
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = resetEmailText,
            onValueChange = { resetEmailText = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (resetEmailText.trim().isNotEmpty() && resetEmailText.contains("@")) {
              showResetDialog = false
              scope.launch {
                val result = authRepository.sendPasswordResetEmail(resetEmailText.trim())
                result.fold(
                  onSuccess = {
                    successMessage = "Se ha enviado un correo de recuperación a ${resetEmailText.trim()}."
                  },
                  onFailure = {
                    errorMessage = "No se pudo enviar el correo de recuperación. Verifica el correo."
                  }
                )
              }
            }
          }
        ) {
          Text("Enviar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }
}

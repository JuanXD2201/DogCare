package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.domain.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
  private val auth: FirebaseAuth = Firebase.auth
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
    context.getString(R.string.firestore_database_id)
  )
  private val credentialManager = CredentialManager.create(context)

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  val currentUserId: String?
    get() = auth.currentUser?.uid

  fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithGoogle(
    activity: Activity,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
  ) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onError("Configuración de Google Sign-In faltante: default_web_client_id no encontrado")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(authCredential).await()
        val user = authResult.user
        if (user != null) {
          syncUserProfile(user)
          onSuccess(user)
        } else {
          onError("No se pudo obtener el usuario de Firebase")
        }
      } else {
        onError("Tipo de credencial no esperado")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("AuthRepository", "Inicio de sesión con Google cancelado: ${e.message}", e)
      onCancelled()
    } catch (e: Exception) {
      Log.e("AuthRepository", "Fallo al iniciar sesión con Google", e)
      onError(e.localizedMessage ?: "Error en autenticación")
    }
  }

  suspend fun syncUserProfile(user: FirebaseUser, customDisplayName: String? = null) {
    try {
      val resolvedName = customDisplayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: user.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Dueño de Mascota"

      val userDoc = db.collection("users").document(user.uid)
      val snapshot = try { userDoc.get().await() } catch (e: Exception) { null }
      if (snapshot == null || !snapshot.exists()) {
        val profileData = mapOf(
          "userId" to user.uid,
          "displayName" to resolvedName,
          "email" to (user.email ?: ""),
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "phone" to (user.phoneNumber ?: ""),
          "role" to "user",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc.set(profileData).await()
        Log.i("AuthRepository", "Perfil de usuario creado en Firestore para ${user.uid}")
      }

      // Sync public profile to /publicProfiles/{uid}
      val publicDoc = db.collection("publicProfiles").document(user.uid)
      val publicData = mapOf(
        "userId" to user.uid,
        "displayName" to resolvedName,
        "photoUrl" to (user.photoUrl?.toString() ?: ""),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      publicDoc.set(publicData, com.google.firebase.firestore.SetOptions.merge()).await()
      Log.i("AuthRepository", "Perfil público creado en Firestore para ${user.uid}")
    } catch (e: Exception) {
      Log.e("AuthRepository", "Error sincronizando perfil en Firestore: ${e.message}", e)
      handleFirestoreError(e, OperationType.CREATE, "users/${user.uid}")
    }
  }

  suspend fun getAllUsersForAdmin(): List<UserProfile> {
    return try {
      val snapshot = db.collection("users").get().await()
      snapshot.toObjects(UserProfile::class.java)
    } catch (e: Exception) {
      Log.e("AuthRepository", "Error obteniendo lista de usuarios para admin: ${e.message}", e)
      handleFirestoreError(e, OperationType.LIST, "users")
      emptyList()
    }
  }

  suspend fun getUserProfile(userId: String): UserProfile? {
    return try {
      val doc = db.collection("users").document(userId).get().await()
      doc.toObject(UserProfile::class.java)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.READ, "users/$userId")
      null
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
    val registration = db.collection("users").document(userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          trySend(null)
          return@addSnapshotListener
        }
        val profile = snapshot?.toObject(UserProfile::class.java)
        trySend(profile)
      }
    awaitClose { registration.remove() }
  }

  suspend fun updateUserProfile(name: String, phone: String): Boolean {
    val uid = currentUserId ?: return false
    return try {
      db.collection("users").document(uid).update(
        mapOf(
          "displayName" to name,
          "phone" to phone,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      // Nombre en la cuenta de Firebase Auth: el chat lo usa como nombre del remitente
      com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateProfile(
        com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(name).build()
      )?.await()

      // Also update public profile
      db.collection("publicProfiles").document(uid).set(
        mapOf(
          "userId" to uid,
          "displayName" to name,
          "updatedAt" to FieldValue.serverTimestamp()
        ),
        com.google.firebase.firestore.SetOptions.merge()
      ).await()

      true
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
      false
    }
  }

  suspend fun migratePublicProfiles(): Pair<Int, String> {
    return try {
      val usersSnapshot = db.collection("users").get().await()
      var count = 0
      for (doc in usersSnapshot.documents) {
        val uid = doc.id
        val name = doc.getString("displayName") ?: "Dueño de Mascota"
        val photo = doc.getString("photoUrl") ?: ""
        val publicDoc = db.collection("publicProfiles").document(uid)
        publicDoc.set(
          mapOf(
            "userId" to uid,
            "displayName" to name,
            "photoUrl" to photo,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          ),
          com.google.firebase.firestore.SetOptions.merge()
        ).await()
        count++
      }
      Pair(count, "Migración exitosa: $count perfiles públicos migrados a /publicProfiles.")
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "users")
      Pair(0, "Error durante la migración: ${e.localizedMessage}")
    }
  }

  suspend fun signUpWithEmail(name: String, email: String, pass: String): Result<FirebaseUser> {
    return try {
      val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
      val user = authResult.user
      if (user != null) {
        try {
          val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim())
            .build()
          user.updateProfile(profileUpdates).await()
        } catch (e: Exception) {
          Log.w("AuthRepository", "Non-fatal error updating display name: ${e.message}")
        }
        syncUserProfile(user, name.trim())
        Result.success(user)
      } else {
        Result.failure(Exception("No se pudo registrar la cuenta de usuario."))
      }
    } catch (e: Exception) {
      Log.e("AuthRepository", "Error en registro por correo", e)
      Result.failure(e)
    }
  }

  suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
    return try {
      val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
      val user = authResult.user
      if (user != null) {
        syncUserProfile(user)
        Result.success(user)
      } else {
        Result.failure(Exception("No se pudo iniciar sesión con las credenciales proporcionadas."))
      }
    } catch (e: Exception) {
      Log.e("AuthRepository", "Error en inicio de sesión por correo", e)
      Result.failure(e)
    }
  }

  suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    return try {
      auth.sendPasswordResetEmail(email.trim()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("AuthRepository", "Error enviando correo de recuperación", e)
      Result.failure(e)
    }
  }

  fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
    auth.signOut()
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e("AuthRepository", "Error al limpiar credenciales", e)
      } finally {
        onComplete()
      }
    }
  }
}


package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserChatRepository(private val context: Context) {
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
    context.getString(R.string.firestore_database_id)
  )

  private fun requireUserId(): String {
    return Firebase.auth.currentUser?.uid
      ?: throw IllegalStateException("Usuario no autenticado para el chat de comunidad.")
  }

  fun observeUsersDirectory(): Flow<List<UserProfile>> = flow {
    val uid = requireUserId()
    val path = "publicProfiles"
    db.collection("publicProfiles")
      .limit(31)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserProfile::class.java).filter { it.userId != uid }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  fun observeUserConversations(): Flow<List<Conversation>> = flow {
    val uid = requireUserId()
    val path = "conversations"
    db.collection("conversations")
      .whereArrayContains("participantUids", uid)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Conversation::class.java)
          .sortedByDescending { it.lastMessageTimestamp ?: it.createdAt }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
    val path = "conversations/$conversationId/messages"
    db.collection("conversations").document(conversationId).collection("messages")
      .orderBy("createdAt")
      .snapshots()
      .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptyList())
      }
      .collect { emit(it) }
  }

  suspend fun getOrCreateConversation(otherUser: UserProfile): String {
    val myUid = requireUserId()
    val myName = Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario DogCare"
    val convsRef = db.collection("conversations")

    // Check if an existing conversation exists between both participants
    val existingSnapshot = convsRef
      .whereArrayContains("participantUids", myUid)
      .get()
      .await()

    val existing = existingSnapshot.documents.firstOrNull { doc ->
      val participants = doc.get("participantUids") as? List<*>
      participants?.contains(otherUser.userId) == true
    }

    if (existing != null) {
      return existing.id
    }

    // Create new conversation
    val newDoc = convsRef.document()
    val payload = mapOf(
      "id" to newDoc.id,
      "participantUids" to listOf(myUid, otherUser.userId),
      "participantNames" to mapOf(
        myUid to myName,
        otherUser.userId to otherUser.displayName
      ),
      "lastMessageText" to "Conversación iniciada",
      "lastSenderUid" to myUid,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      newDoc.set(payload).await()
      return newDoc.id
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, newDoc.path)
      throw e
    }
  }

  suspend fun sendMessage(conversationId: String, text: String) {
    val myUid = requireUserId()
    val myName = Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Usuario DogCare"
    val convRef = db.collection("conversations").document(conversationId)
    val msgDoc = convRef.collection("messages").document()

    val msgPayload = mapOf(
      "id" to msgDoc.id,
      "conversationId" to conversationId,
      "senderUid" to myUid,
      "senderName" to myName,
      "text" to text,
      "createdAt" to FieldValue.serverTimestamp()
    )

    try {
      msgDoc.set(msgPayload).await()
      convRef.update(
        mapOf(
          "lastMessageText" to text,
          "lastSenderUid" to myUid,
          "lastMessageTimestamp" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgDoc.path)
      throw e
    }
  }

  /** UIDs que el usuario actual ha bloqueado (users/{uid}/blockedUsers). */
  fun observeBlockedUids(): Flow<Set<String>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/blockedUsers"
    db.collection("users").document(uid).collection("blockedUsers")
      .snapshots()
      .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        emit(emptySet())
      }
      .collect { emit(it) }
  }

  suspend fun blockUser(blockedUid: String) {
    val myUid = requireUserId()
    require(blockedUid.isNotBlank() && blockedUid != myUid) { "Usuario inválido para bloquear." }
    val ref = db.collection("users").document(myUid).collection("blockedUsers").document(blockedUid)
    try {
      ref.set(mapOf("blockedUid" to blockedUid, "createdAt" to FieldValue.serverTimestamp())).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ref.path)
      throw e
    }
  }

  suspend fun unblockUser(blockedUid: String) {
    val myUid = requireUserId()
    val ref = db.collection("users").document(myUid).collection("blockedUsers").document(blockedUid)
    try {
      ref.delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path)
      throw e
    }
  }

  /** Crea un reporte que solo puede leer un administrador. */
  suspend fun reportUser(reportedUid: String, conversationId: String, reason: String) {
    val myUid = requireUserId()
    val cleanReason = reason.trim()
    require(cleanReason.isNotEmpty() && cleanReason.length <= 500) { "El motivo debe tener entre 1 y 500 caracteres." }
    require(reportedUid.isNotBlank() && reportedUid != myUid) { "Usuario inválido para reportar." }
    val ref = db.collection("reports").document()
    try {
      ref.set(
        mapOf(
          "id" to ref.id,
          "reporterUid" to myUid,
          "reportedUid" to reportedUid,
          "conversationId" to conversationId,
          "reason" to cleanReason,
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ref.path)
      throw e
    }
  }
}

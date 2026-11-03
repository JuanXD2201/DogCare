package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val phone: String? = null,
  val role: String = "user",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
  val id: String = "",
  val participantUids: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val lastMessageText: String = "",
  val lastSenderUid: String = "",
  @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
  val id: String = "",
  val conversationId: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

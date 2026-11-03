package com.example.domain.model

data class AiChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isUrgent: Boolean = false,
  val isSystemError: Boolean = false,
  val canRetry: Boolean = false,
  val userPromptToRetry: String? = null,
  val timestampMillis: Long = System.currentTimeMillis()
)

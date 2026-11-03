package com.example.presentation.screens.aichat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiAssistantRepository
import com.example.domain.model.AiChatMessage
import com.example.domain.util.EmergencyDetector
import kotlinx.coroutines.launch

/** Historial inicial del chat; lo conserva DogCareApp para que no se pierda al cambiar de pestaña. */
fun initialAiMessages(): SnapshotStateList<AiChatMessage> = mutableStateListOf(
  AiChatMessage(
    isUser = false,
    text = "¡Hola! Soy tu Asistente Canino DogCare 🐾. Puedo ayudarte con preguntas sobre nutrición, hábitos, bienestar y cuidados preventivos para tu perro."
  )
)

@Composable
fun AiChatScreen(
  aiRepository: AiAssistantRepository,
  messages: SnapshotStateList<AiChatMessage>,
  onNavigateToVetMap: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  var inputPrompt by remember { mutableStateOf("") }
  var isTyping by remember { mutableStateOf(false) }
  var hasDetectedUrgency by remember { mutableStateOf(false) }

  val suggestedPrompts = listOf(
    "¿Qué hago si mi perro comió chocolate?",
    "¿Cuántas veces al día debe comer un cachorro?",
    "Síntomas de deshidratación en perros",
    "¿Cada cuánto debo bañar a mi perro?"
  )

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  fun sendUserMessage(text: String) {
    val clean = text.trim()
    if (clean.isBlank() || isTyping) return

    val isUrgentInput = EmergencyDetector.isEmergency(clean)
    if (isUrgentInput) {
      hasDetectedUrgency = true
    }

    val userMsg = AiChatMessage(isUser = true, text = clean)
    messages.add(userMsg)
    inputPrompt = ""
    isTyping = true

    scope.launch {
      try {
        val reply = aiRepository.sendMessage(history = messages.toList(), userPrompt = clean)
        val finalReply = if (isUrgentInput && !reply.isUrgent) {
          reply.copy(isUrgent = true)
        } else reply
        if (finalReply.isUrgent) {
          hasDetectedUrgency = true
        }
        messages.add(finalReply)
      } catch (e: Exception) {
        messages.add(
          AiChatMessage(
            isUser = false,
            text = "Error al procesar la consulta con la IA (${e.localizedMessage ?: "error inesperado"}).",
            isUrgent = isUrgentInput,
            isSystemError = true,
            canRetry = true,
            userPromptToRetry = clean
          )
        )
      } finally {
        isTyping = false
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1) Permanent Mandatory Disclaimer & Top Emergency Action Button
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "La IA no sustituye la valoración de un médico veterinario",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Always visible Emergency Button
          Button(
            onClick = onNavigateToVetMap,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("top_emergency_button")
          ) {
            Icon(Icons.Filled.Emergency, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Urgencias", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 2) Red Banner / Card if Urgency was detected
    AnimatedVisibility(visible = hasDetectedUrgency) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Aviso de Seguridad DogCare: Urgencia",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Los signos descritos pueden comprometer la vida de tu perro. Acude de inmediato a un centro veterinario.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onErrorContainer
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onNavigateToVetMap,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("see_emergency_vets_button")
            ) {
              Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ver Veterinarias", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                try {
                  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:123"))
                  context.startActivity(intent)
                } catch (_: Exception) {}
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("call_emergency_button")
            ) {
              Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Llamar 123", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3) Scrollable message list
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        ChatMessageBubble(
          message = msg,
          onNavigateToVetMap = onNavigateToVetMap,
          onRetry = {
            msg.userPromptToRetry?.let { sendUserMessage(it) }
          }
        )
      }

      if (isTyping) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DogCare IA respondiendo...",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 4) Quick suggestions chips
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(suggestedPrompts) { prompt ->
        SuggestionChip(
          onClick = { sendUserMessage(prompt) },
          label = { Text(prompt, fontSize = 11.5.sp) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      }
    }

    // 5) Bottom Input Row
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputPrompt,
          onValueChange = { inputPrompt = it },
          placeholder = { Text("Escribe una consulta sobre tu perro...", fontSize = 13.sp) },
          modifier = Modifier
            .weight(1f)
            .testTag("ai_chat_input"),
          shape = RoundedCornerShape(24.dp),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = { sendUserMessage(inputPrompt) },
          enabled = inputPrompt.isNotBlank() && !isTyping,
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
              if (inputPrompt.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("ai_chat_send_button")
        ) {
          Icon(
            imageVector = Icons.Filled.Send,
            contentDescription = "Enviar mensaje",
            tint = if (inputPrompt.isNotBlank() && !isTyping) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(
  message: AiChatMessage,
  onNavigateToVetMap: () -> Unit,
  onRetry: () -> Unit
) {
  val isUser = message.isUser
  val isSystemError = message.isSystemError

  val bubbleColor = when {
    isUser -> MaterialTheme.colorScheme.primary
    isSystemError -> MaterialTheme.colorScheme.surfaceVariant
    message.isUrgent -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surface
  }

  val textColor = when {
    isUser -> MaterialTheme.colorScheme.onPrimary
    isSystemError -> MaterialTheme.colorScheme.onSurfaceVariant
    message.isUrgent -> MaterialTheme.colorScheme.onErrorContainer
    else -> MaterialTheme.colorScheme.onSurface
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
      if (!isUser) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
              when {
                isSystemError -> MaterialTheme.colorScheme.errorContainer
                message.isUrgent -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primaryContainer
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              isSystemError -> Icons.Filled.ErrorOutline
              message.isUrgent -> Icons.Filled.Warning
              else -> Icons.Filled.SmartToy
            },
            contentDescription = null,
            tint = when {
              isSystemError -> MaterialTheme.colorScheme.error
              message.isUrgent -> Color.White
              else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
      }

      ElevatedCard(
        shape = RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (isUser) 16.dp else 4.dp,
          bottomEnd = if (isUser) 4.dp else 16.dp
        ),
        colors = CardDefaults.elevatedCardColors(containerColor = bubbleColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(0.88f)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // System Error Header - explicitly marked as DogCare system notice, NOT AI
          if (isSystemError) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Mensaje del Sistema DogCare",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.error
              )
            }
          } else if (message.isUrgent) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.Emergency,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "ALERTA DE EMERGENCIA VETERINARIA",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.error
              )
            }
          }

          Text(
            text = message.text,
            color = textColor,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
          )

          // If system error has retry option
          if (message.canRetry) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = onRetry,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("ai_retry_button")
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Reintentar consulta", fontSize = 11.5.sp)
            }
          }

          // Urgent safety notice actions (buttons to map and call), even if system error occurred
          if (message.isUrgent) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "Aviso de seguridad DogCare (Protocolo de Urgencia):",
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = onNavigateToVetMap,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_emergency_vet_button")
                ) {
                  Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Ver Veterinarias de Urgencia 24h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

package com.example.presentation.screens.userchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserChatRepository
import com.example.domain.model.ChatMessage
import com.example.domain.model.Conversation
import com.example.domain.model.UserProfile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun UserChatScreen(
  chatRepository: UserChatRepository,
  currentUserId: String?
) {
  val scope = rememberCoroutineScope()
  var selectedTab by remember { mutableIntStateOf(0) }
  var activeConversationId by remember { mutableStateOf<String?>(null) }
  var activeChatOtherUserName by remember { mutableStateOf("Dueño de Mascota") }
  var activeChatOtherUid by remember { mutableStateOf("") }
  var actionError by remember { mutableStateOf<String?>(null) }

  val conversations by if (currentUserId != null) {
    chatRepository.observeUserConversations().collectAsState(initial = emptyList())
  } else {
    remember { mutableStateOf(emptyList<Conversation>()) }
  }

  val directoryUsers by if (currentUserId != null) {
    chatRepository.observeUsersDirectory().collectAsState(initial = emptyList())
  } else {
    remember { mutableStateOf(emptyList<UserProfile>()) }
  }

  val blockedUids by if (currentUserId != null) {
    chatRepository.observeBlockedUids().collectAsState(initial = emptySet())
  } else {
    remember { mutableStateOf(emptySet<String>()) }
  }

  // Active chat conversation view
  if (activeConversationId != null && currentUserId != null) {
    ConversationDetailView(
      conversationId = activeConversationId!!,
      otherUserName = activeChatOtherUserName,
      otherUserUid = activeChatOtherUid,
      isBlockedByMe = activeChatOtherUid in blockedUids,
      currentUserId = currentUserId,
      chatRepository = chatRepository,
      onBack = { activeConversationId = null }
    )
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Tabs
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Mis Chats (${conversations.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Comunidad de Dueños", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        )
      }
    }

    // Tab content
    if (currentUserId == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        ElevatedCard(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Inicia sesión para chatear", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Conecta e intercambia consejos y experiencias con otros dueños de perros de la comunidad.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 13.sp
            )
          }
        }
      }
    } else if (selectedTab == 0) {
      if (conversations.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          ElevatedCard(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(28.dp)
            ) {
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
              Spacer(modifier = Modifier.height(14.dp))
              Text("No tienes conversaciones activas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Ve a la pestaña 'Comunidad de Dueños' para seleccionar un contacto e iniciar una conversación.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(conversations, key = { it.id }) { conv ->
            val otherName = conv.participantNames.entries.firstOrNull { it.key != currentUserId }?.value ?: "Compañero Canino"
            ConversationItemCard(
              conversation = conv,
              otherName = otherName,
              onClick = {
                activeChatOtherUserName = otherName
                activeChatOtherUid = conv.participantUids.firstOrNull { it != currentUserId } ?: ""
                activeConversationId = conv.id
              }
            )
          }
        }
      }
    } else {
      if (directoryUsers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Group,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(30.dp)
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Comunidad de Dueños DogCare",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Tu perfil ya está registrado en la comunidad. Cuando otros usuarios creen su cuenta aparecerán aquí para conversar.",
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (actionError != null) {
            item { Text(actionError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
          }
          items(directoryUsers, key = { it.userId }) { user ->
            val isBlocked = user.userId in blockedUids
            DirectoryUserCard(
              user = user,
              isBlocked = isBlocked,
              onToggleBlock = {
                scope.launch {
                  try {
                    actionError = null
                    if (isBlocked) chatRepository.unblockUser(user.userId) else chatRepository.blockUser(user.userId)
                  } catch (e: Exception) {
                    actionError = "No se pudo actualizar el bloqueo. Revisa tu conexión."
                  }
                }
              },
              onStartChat = {
                scope.launch {
                  try {
                    val convId = chatRepository.getOrCreateConversation(user)
                    activeChatOtherUserName = user.displayName
                    activeChatOtherUid = user.userId
                    activeConversationId = convId
                  } catch (e: Exception) {
                    actionError = "No se pudo iniciar la conversación. Es posible que no haya conexión o que este usuario no pueda recibir mensajes."
                  }
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ConversationItemCard(
  conversation: Conversation,
  otherName: String,
  onClick: () -> Unit
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("conversation_item_${conversation.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(text = otherName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = conversation.lastMessageText.ifBlank { "Toca para abrir el chat" },
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
private fun DirectoryUserCard(
  user: UserProfile,
  isBlocked: Boolean,
  onToggleBlock: () -> Unit,
  onStartChat: () -> Unit
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onStartChat() },
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Filled.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
          text = if (isBlocked) "Usuario bloqueado" else user.email.ifBlank { "Miembro de la comunidad" },
          fontSize = 12.sp,
          color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (isBlocked) {
        TextButton(onClick = onToggleBlock) { Text("Desbloquear") }
      } else {
        IconButton(onClick = onToggleBlock, modifier = Modifier.size(38.dp)) {
          Icon(Icons.Filled.Block, contentDescription = "Bloquear usuario", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
      }

      if (!isBlocked) IconButton(
        onClick = onStartChat,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Iniciar chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun ConversationDetailView(
  conversationId: String,
  otherUserName: String,
  otherUserUid: String,
  isBlockedByMe: Boolean,
  currentUserId: String,
  chatRepository: UserChatRepository,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  var messageText by remember { mutableStateOf("") }
  val messages by chatRepository.observeMessages(conversationId).collectAsState(initial = emptyList())
  var menuOpen by remember { mutableStateOf(false) }
  var showReport by remember { mutableStateOf(false) }
  var reportReason by remember { mutableStateOf("") }
  var sendError by remember { mutableStateOf<String?>(null) }
  // Los mensajes de un usuario bloqueado se ocultan
  val visibleMessages = if (isBlockedByMe) messages.filter { it.senderUid != otherUserUid } else messages

  val timeFormatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  fun sendMsg() {
    val clean = messageText.trim()
    if (clean.isBlank()) return
    messageText = ""
    scope.launch {
      try {
        sendError = null
        chatRepository.sendMessage(conversationId, clean)
      } catch (e: Exception) {
        // Las reglas rechazan el envío si el otro usuario te bloqueó; también puede faltar conexión
        sendError = "No se pudo enviar el mensaje. Es posible que no haya conexión o que el usuario no pueda recibirlo."
      }
    }
  }

  if (showReport) {
    AlertDialog(
      onDismissRequest = { showReport = false },
      title = { Text("Reportar a $otherUserName") },
      text = {
        OutlinedTextField(
          value = reportReason,
          onValueChange = { if (it.length <= 500) reportReason = it },
          label = { Text("Motivo del reporte") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 4
        )
      },
      confirmButton = {
        TextButton(
          enabled = reportReason.isNotBlank(),
          onClick = {
            val reason = reportReason
            showReport = false
            reportReason = ""
            scope.launch {
              try {
                chatRepository.reportUser(otherUserUid, conversationId, reason)
                sendError = "Reporte enviado. Gracias por ayudarnos a mantener la comunidad segura."
              } catch (e: Exception) {
                sendError = "No se pudo enviar el reporte. Inténtalo de nuevo."
              }
            }
          }
        ) { Text("Enviar reporte") }
      },
      dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancelar") } }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = otherUserName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
          Text(text = "Chat Privado de Comunidad", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
          IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
          }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
              text = { Text(if (isBlockedByMe) "Desbloquear usuario" else "Bloquear usuario") },
              onClick = {
                menuOpen = false
                scope.launch {
                  try {
                    if (isBlockedByMe) chatRepository.unblockUser(otherUserUid) else chatRepository.blockUser(otherUserUid)
                  } catch (e: Exception) {
                    sendError = "No se pudo actualizar el bloqueo."
                  }
                }
              }
            )
            DropdownMenuItem(
              text = { Text("Reportar usuario") },
              onClick = { menuOpen = false; showReport = true }
            )
          }
        }
      }
    }

    // Messages list
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(visibleMessages, key = { it.id }) { msg ->
        val isMine = msg.senderUid == currentUserId
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
          ElevatedCard(
            shape = RoundedCornerShape(
              topStart = 16.dp,
              topEnd = 16.dp,
              bottomStart = if (isMine) 16.dp else 4.dp,
              bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            colors = CardDefaults.elevatedCardColors(
              containerColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Text(
                text = msg.text,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                lineHeight = 18.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = msg.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "",
                fontSize = 10.sp,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }
        }
      }
    }

    if (sendError != null) {
      Text(
        text = sendError!!,
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
      )
    }

    // Input Bar
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
          value = messageText,
          onValueChange = { messageText = it },
          enabled = !isBlockedByMe,
          placeholder = { Text(if (isBlockedByMe) "Bloqueaste a este usuario" else "Escribe un mensaje...", fontSize = 13.sp) },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
          ),
          maxLines = 3
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = { sendMsg() },
          enabled = messageText.isNotBlank() && !isBlockedByMe,
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
              if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Enviar",
            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

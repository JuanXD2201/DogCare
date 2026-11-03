package com.example.data.repository

import android.util.Log
import com.example.domain.model.AiChatMessage
import com.example.domain.util.EmergencyDetector
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

private const val TAG = "DogCareAI"

// Modelos de Gemini vía Firebase AI Logic, en orden de preferencia. gemini-2.5-flash dejó de estar disponible
// para proyectos nuevos (el servidor sugiere gemini-3.8-flash). Si un modelo no responde a tiempo o ya no
// existe, se prueba el siguiente; solo hay que editar esta lista cuando Google retire alguno.
// gemini-3.5-flash-lite va primero: gemini-3.8-flash aceptaba la solicitud pero no respondía en 25 s en dispositivo,
// y el servidor sugirió este modelo al rechazar gemini-2.5-flash-lite y gemini-2.0-flash (retirados).
private val MODEL_CANDIDATES = listOf("gemini-3.5-flash-lite", "gemini-3.8-flash")

// Tiempo máximo por modelo; sin límite la interfaz quedaba cargando indefinidamente
private const val MODEL_TIMEOUT_MS = 25_000L
private const val APP_CHECK_TIMEOUT_MS = 10_000L
private const val MAX_CONTEXT_MESSAGES = 6

class AiAssistantRepository {

  private val systemPrompt = """
    Eres el Asistente Canino Oficial de DogCare. Eres un experto empático y científico en bienestar, nutrición, higiene, cuidados y comportamiento de perros.

    REGLAS OBLIGATORIAS:
    1. AVISO LEGAL: Recuerda siempre que tus respuestas son orientativas y de prevención. No sustituyes el examen físico de un médico veterinario colegiado. No formules recetas médicas farmacológicas.
    2. PROTOCOLO DE URGENCIAS: Si detectas señales de riesgo vital (abdomen hinchado y tenso, intentos de vómito improductivo, encías blancas o azules, convulsiones, ingestión de tóxicos como chocolate/cebolla/veneno, hemorragia profusa, traumatismo o dificultad respiratoria grave), inicia tu respuesta indicando claramente: "[ALERTA DE URGENCIA VETERINARIA] Tu perro requiere atención médica presencial inmediata en una clínica veterinaria de urgencias 24h."
    3. Responde de manera clara, cálida, estructurada con viñetas cuando sea útil y en español.
  """.trimIndent()

  /**
   * Sends user message to Gemini via Firebase AI Logic.
   * If Gemini fails, returns a clearly marked system error message (not an AI response),
   * specifying the cause (App Check, no connection, quota exhausted, timeout or empty response)
   * with a retry option. If the question was urgent, maintains the emergency security flag.
   */
  suspend fun sendMessage(
    history: List<AiChatMessage>,
    userPrompt: String
  ): AiChatMessage = withContext(Dispatchers.IO) {
    val isUrgentQuestion = EmergencyDetector.isEmergency(userPrompt)

    fun systemError(text: String) = AiChatMessage(
      isUser = false,
      text = text,
      isUrgent = isUrgentQuestion,
      isSystemError = true,
      canRetry = true,
      userPromptToRetry = userPrompt
    )

    // Las últimas intervenciones dan contexto a la IA (los mensajes de error del sistema no cuentan)
    val recentContext = history
      .filter { !it.isSystemError && it.text.isNotBlank() }
      .let { h -> if (h.lastOrNull()?.let { it.isUser && it.text == userPrompt } == true) h.dropLast(1) else h }
      .takeLast(MAX_CONTEXT_MESSAGES)
      .joinToString("\n") { (if (it.isUser) "Dueño: " else "Asistente: ") + it.text }
    val promptWithContext = buildString {
      if (recentContext.isNotBlank()) append("Conversación previa:\n$recentContext\n\n")
      append("Pregunta del dueño del perro: $userPrompt")
    }

    // 1) App Check: si falla, ningún modelo responderá. Se registra el motivo (nunca el token).
    val appCheckProblem = checkAppCheck()

    // 2) Se prueban los modelos en orden hasta obtener texto
    var lastError: Exception? = null
    var anyTimeout = false
    for (modelName in MODEL_CANDIDATES) {
      try {
        val startedAt = System.currentTimeMillis()
        Log.i(TAG, "Consultando $modelName")
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
          .generativeModel(modelName = modelName, systemInstruction = content { text(systemPrompt) })
        val replyText = withTimeoutOrNull(MODEL_TIMEOUT_MS) {
          model.generateContent(promptWithContext).text?.trim()
        }
        val elapsed = System.currentTimeMillis() - startedAt
        if (replyText == null) {
          Log.w(TAG, "$modelName sin respuesta tras $elapsed ms (tiempo agotado)")
          anyTimeout = true
          continue
        }
        Log.i(TAG, "$modelName respondió en $elapsed ms (${replyText.length} caracteres)")
        if (replyText.isBlank()) continue
        return@withContext AiChatMessage(
          isUser = false,
          text = replyText,
          isUrgent = isUrgentQuestion || replyText.contains("URGENCIA", ignoreCase = true),
          isSystemError = false,
          canRetry = false
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "$modelName falló: ${e.javaClass.simpleName}: ${e.message}")
        lastError = e
        // Sin conexión no tiene sentido probar otros modelos
        if (isNetworkError(e) && !isModelUnavailable(e)) break
      }
    }

    // 3) Ningún modelo respondió: mensaje según la causa más probable
    val error = lastError
    return@withContext when {
      appCheckProblem != null -> systemError(appCheckProblem)
      error != null && isQuotaExhaustedError(error) ->
        systemError("Se ha agotado la cuota de consultas a la IA por el momento. Por favor, espera unos minutos e intenta nuevamente.")
      error != null && isNetworkError(error) && !isModelUnavailable(error) ->
        systemError("Sin conexión a internet. No se pudo contactar al servicio de IA. Revisa tu conexión de red e intenta de nuevo.")
      error != null ->
        systemError("No fue posible obtener una respuesta de la IA (${error.localizedMessage ?: "error del servicio"}). Intenta de nuevo.")
      anyTimeout ->
        systemError("La IA tardó demasiado en responder. Revisa tu conexión e inténtalo de nuevo en unos segundos.")
      else ->
        systemError("La IA no generó ninguna respuesta para esta consulta. Por favor, reintenta reformulando tu pregunta.")
    }
  }

  /** Devuelve null si App Check entrega un token; si no, un mensaje para el usuario. */
  private suspend fun checkAppCheck(): String? {
    return try {
      val token = withTimeoutOrNull(APP_CHECK_TIMEOUT_MS) {
        FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
      }
      if (token == null) {
        Log.w(TAG, "App Check: sin token tras $APP_CHECK_TIMEOUT_MS ms")
        "No se pudo verificar la app con Firebase App Check (tiempo agotado). Revisa la conexión; en la versión de depuración, el token del dispositivo debe estar registrado en la consola de Firebase."
      } else {
        Log.i(TAG, "App Check: token obtenido")
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "App Check: error ${e.javaClass.simpleName}: ${e.message}")
      "No se pudo verificar la app con Firebase App Check (${e.localizedMessage ?: "error"}). En la versión de depuración, registra el token del dispositivo en la consola de Firebase."
    }
  }

  /** El modelo fue retirado o no está disponible para este proyecto: conviene probar el siguiente. */
  private fun isModelUnavailable(e: Throwable): Boolean {
    val msg = e.message?.lowercase() ?: ""
    return msg.contains("no longer available") ||
        msg.contains("not found") ||
        msg.contains("is not supported") ||
        msg.contains("404")
  }

  private fun isNetworkError(e: Throwable): Boolean {
    if (e is IOException) return true
    val msg = e.message?.lowercase() ?: ""
    return msg.contains("network") ||
        msg.contains("unable to resolve") ||
        msg.contains("timeout") ||
        msg.contains("connect") ||
        msg.contains("connection") ||
        msg.contains("host")
  }

  private fun isQuotaExhaustedError(e: Throwable): Boolean {
    val msg = e.message?.lowercase() ?: ""
    return msg.contains("quota") ||
        msg.contains("429") ||
        msg.contains("resource_exhausted") ||
        msg.contains("rate limit")
  }
}

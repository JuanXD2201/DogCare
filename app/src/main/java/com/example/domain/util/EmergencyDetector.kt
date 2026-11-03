package com.example.domain.util

import java.text.Normalizer

/**
 * Detección prudente de posibles urgencias veterinarias por palabras clave.
 *
 * Limitaciones: no entiende contexto ni negaciones ("no tiene sangre" también alerta)
 * y no cubre todos los casos posibles. Se prefiere un falso positivo (recomendar
 * atención) antes que un falso negativo. No es un diagnóstico veterinario.
 */
object EmergencyDetector {
  // Todas en minúsculas y sin tildes: el texto del usuario se normaliza igual antes de comparar.
  private val redFlagsKeywords = listOf(
    // Intoxicación
    "envenen", "intoxic", "veneno", "chocolate", "uvas", "pasas", "xilitol", "raticida",
    "anticongelante", "se comio un medicamento", "tomo pastillas",
    // Respiración y circulación
    "no respira", "dificultad para respirar", "respirar", "asfixia", "ahogando", "se ahoga",
    "encias blancas", "encias palidas", "encias azules", "lengua azul", "morado", "colapso",
    "desmayo", "desmayado", "inconsciente", "no reacciona", "no despierta",
    // Neurológico
    "convulsion", "convulsiones", "temblores fuertes", "paralisis", "no puede caminar",
    "no puede pararse", "ataque epileptico",
    // Digestivo y urinario
    "torsion", "abdomen hinchado", "abombado", "barriga hinchada", "vomito constante",
    "vomita sangre", "diarrea con sangre", "no puede orinar", "no orina",
    "trago un objeto", "obstruccion", "se atraganto",
    // Trauma y otros
    "sangre", "sangrado", "hemorragia", "atropell", "fractura", "quebrado", "golpe fuerte",
    "mordedura de serpiente", "mordio una serpiente", "golpe de calor", "insolacion",
    "quemadura", "ojo salido", "parto complicado",
    // Genéricas
    "urgencia", "emergencia", "se esta muriendo", "agonizando"
  )

  /** Minúsculas y sin diacríticos, para que "convulsión" y "convulsion" coincidan. */
  private fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

  fun isEmergency(prompt: String): Boolean {
    if (prompt.isBlank()) return false
    val normalized = normalize(prompt)
    return redFlagsKeywords.any { normalized.contains(it) }
  }
}

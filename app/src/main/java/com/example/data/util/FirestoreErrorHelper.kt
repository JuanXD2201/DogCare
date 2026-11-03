package com.example.data.util

import android.util.Log

enum class OperationType {
  CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String) {
  val message = """
    {
      "event": "firestore_error",
      "operation": "${operationType.name}",
      "path": "$path",
      "message": "${exception.localizedMessage?.replace("\"", "'") ?: "Unknown error"}"
    }
  """.trimIndent()
  Log.e("FirestoreError", message, exception)
}

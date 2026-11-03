package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.domain.model.Pet
import com.example.domain.usecase.CalculateDogAgeUseCase
import com.example.domain.model.DogSizeCategory
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

open class PetRepository(private val context: Context) {
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
    context.getString(R.string.firestore_database_id)
  )
  private val ageUseCase = CalculateDogAgeUseCase()

  private fun requireUserId(): String {
    return Firebase.auth.currentUser?.uid
      ?: throw IllegalStateException("Usuario no autenticado para acceder a sus mascotas.")
  }

  open fun observePets(): Flow<List<Pet>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/pets"
    emitAll(
      db.collection("users").document(uid).collection("pets")
        .snapshots()
        .map { snapshot -> snapshot.toObjects(Pet::class.java) }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  suspend fun addPet(
    name: String,
    breed: String,
    sizeCategory: String,
    ageYears: Int,
    ageMonths: Int,
    weightKg: Double,
    notes: String
  ): String {
    val uid = requireUserId()
    val cleanSize = sizeCategory.trim().lowercase().ifBlank { "medium" }
    val calculation = ageUseCase.calculate(
      years = ageYears,
      months = ageMonths,
      size = DogSizeCategory.fromString(cleanSize)
    )

    val petsRef = db.collection("users").document(uid).collection("pets")
    val newDoc = petsRef.document()
    val pet = Pet(
      id = newDoc.id,
      userId = uid,
      name = name.trim(),
      breed = breed.trim().ifBlank { "Mestizo" },
      sizeCategory = cleanSize,
      ageYears = ageYears,
      ageMonths = ageMonths,
      weightKg = weightKg,
      humanAgeYears = calculation.humanYearsEquivalent,
      notes = notes.trim()
    )

    val payload = mapOf(
      "id" to pet.id,
      "userId" to uid,
      "name" to pet.name,
      "breed" to pet.breed,
      "sizeCategory" to pet.sizeCategory,
      "ageYears" to pet.ageYears,
      "ageMonths" to pet.ageMonths,
      "weightKg" to pet.weightKg,
      "humanAgeYears" to pet.humanAgeYears,
      "notes" to pet.notes,
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

  suspend fun updatePet(pet: Pet) {
    val uid = requireUserId()
    require(pet.id.isNotBlank()) { "El ID de la mascota no puede estar vacío" }

    val calculation = ageUseCase.calculate(
      years = pet.ageYears,
      months = pet.ageMonths,
      size = DogSizeCategory.fromString(pet.sizeCategory)
    )

    val docRef = db.collection("users").document(uid).collection("pets").document(pet.id)
    val updates = mapOf(
      "name" to pet.name,
      "breed" to pet.breed,
      "sizeCategory" to pet.sizeCategory,
      "ageYears" to pet.ageYears,
      "ageMonths" to pet.ageMonths,
      "weightKg" to pet.weightKg,
      "humanAgeYears" to calculation.humanYearsEquivalent,
      "notes" to pet.notes,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    try {
      docRef.update(updates).await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deletePet(petId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("pets").document(petId)
    try {
      docRef.delete().await()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // Helper for direct Kotlin flow emitAll
  private suspend inline fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    flow.collect { value -> emit(value) }
  }
}

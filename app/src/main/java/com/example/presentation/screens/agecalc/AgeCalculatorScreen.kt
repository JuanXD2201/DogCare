package com.example.presentation.screens.agecalc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.PetRepository
import com.example.domain.model.DogLifeStage
import com.example.domain.model.DogSizeCategory
import com.example.domain.model.Pet
import com.example.domain.usecase.CalculateDogAgeUseCase
import kotlinx.coroutines.launch

@Composable
fun AgeCalculatorScreen(
  petRepository: PetRepository,
  currentUserId: String?
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val ageUseCase = remember { CalculateDogAgeUseCase() }

  var dogName by remember { mutableStateOf("") }
  var dogBreed by remember { mutableStateOf("Mestizo") }
  var selectedSize by remember { mutableStateOf(DogSizeCategory.MEDIUM) }
  var years by remember { mutableIntStateOf(2) }
  var months by remember { mutableIntStateOf(0) }
  var weightKg by remember { mutableFloatStateOf(14.0f) }
  var isSaving by remember { mutableStateOf(false) }

  // Observe registered pets
  val userPetsState = if (currentUserId != null) {
    petRepository.observePets().collectAsState(initial = emptyList())
  } else {
    remember { mutableStateOf(emptyList<Pet>()) }
  }

  // Reactive calculation
  val calculation = remember(years, months, selectedSize) {
    ageUseCase.calculate(years = years, months = months, size = selectedSize)
  }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(10.dp)) }

      // 1) Hero banner with human age equivalent & life stage indicator
      item {
        AgeResultHeroCard(
          dogName = dogName.ifBlank { "Tu Perro" },
          humanYears = calculation.humanYearsEquivalent,
          stage = calculation.stage,
          summary = calculation.summary,
          chronologicalYears = years,
          chronologicalMonths = months
        )
      }

      // 2) Form Card for Dog Profile and Sliders
      item {
        ElevatedCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Tune,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Text(
                text = "Personaliza los Datos de tu Perro",
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dog Name Input
            OutlinedTextField(
              value = dogName,
              onValueChange = { dogName = it },
              label = { Text("Nombre del perro") },
              placeholder = { Text("Ej: Max, Luna, Toby") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dog_name_input"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dog Breed Input
            OutlinedTextField(
              value = dogBreed,
              onValueChange = { dogBreed = it },
              label = { Text("Raza") },
              placeholder = { Text("Ej: Golden Retriever, Criollo, Beagle") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dog_breed_input"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
              ),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Tamaño y Contextura",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Size Category Cards Grid
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              DogSizeCategory.entries.forEach { size ->
                val isSelected = selectedSize == size
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                  modifier = Modifier
                    .weight(1f)
                    .clickable { selectedSize = size }
                ) {
                  Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                  ) {
                    Text(
                      text = size.label,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = when (size) {
                        DogSizeCategory.SMALL -> "< 10 kg"
                        DogSizeCategory.MEDIUM -> "10-25 kg"
                        DogSizeCategory.LARGE -> "25-45 kg"
                        DogSizeCategory.GIANT -> "> 45 kg"
                      },
                      fontSize = 10.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Years Stepper & Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Edad en Años:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { if (years > 0) years-- },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Filled.Remove, contentDescription = "Menos años", modifier = Modifier.size(18.dp))
                }
                Text(
                  text = "$years años",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                  onClick = { if (years < 25) years++ },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Más años", modifier = Modifier.size(18.dp))
                }
              }
            }

            Slider(
              value = years.toFloat(),
              onValueChange = { years = it.toInt() },
              valueRange = 0f..20f,
              steps = 19,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("dog_years_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Months Stepper & Slider
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Meses Adicionales:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { if (months > 0) months-- },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Filled.Remove, contentDescription = "Menos meses", modifier = Modifier.size(18.dp))
                }
                Text(
                  text = "$months meses",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                  onClick = { if (months < 11) months++ },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Más meses", modifier = Modifier.size(18.dp))
                }
              }
            }

            Slider(
              value = months.toFloat(),
              onValueChange = { months = it.toInt() },
              valueRange = 0f..11f,
              steps = 10,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
              ),
              modifier = Modifier.testTag("dog_months_slider")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save to Firebase button
            Button(
              onClick = {
                if (currentUserId == null) {
                  scope.launch {
                    snackbarHostState.showSnackbar("Inicia sesión para guardar el perfil de tu perro en la nube.")
                  }
                  return@Button
                }
                if (dogName.isBlank()) {
                  scope.launch {
                    snackbarHostState.showSnackbar("Por favor ingresa el nombre de tu perro.")
                  }
                  return@Button
                }
                isSaving = true
                scope.launch {
                  try {
                    petRepository.addPet(
                      name = dogName.trim(),
                      breed = dogBreed.trim(),
                      sizeCategory = selectedSize.name.lowercase(),
                      ageYears = years,
                      ageMonths = months,
                      weightKg = weightKg.toDouble(),
                      notes = ""
                    )
                    snackbarHostState.showSnackbar("¡${dogName.trim()} guardado exitosamente!")
                  } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error al guardar: ${e.localizedMessage}")
                  } finally {
                    isSaving = false
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_pet_button")
            ) {
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardando...")
              } else {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar en Mis Perros", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 3) Veterinary Recommendations Cards
      item {
        Text(
          text = "Pautas de Cuidado según su Etapa",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      item {
        AdviceCard(
          icon = Icons.Filled.Restaurant,
          title = "Nutrición Recomendada",
          advice = calculation.nutritionAdvice,
          tint = MaterialTheme.colorScheme.primary
        )
      }

      item {
        AdviceCard(
          icon = Icons.Filled.MedicalServices,
          title = "Atención Veterinaria Preventiva",
          advice = calculation.healthAdvice,
          tint = MaterialTheme.colorScheme.tertiary
        )
      }

      item {
        AdviceCard(
          icon = Icons.Filled.FitnessCenter,
          title = "Actividad y Ejercicio",
          advice = calculation.exerciseAdvice,
          tint = MaterialTheme.colorScheme.secondary
        )
      }

      // 4) Saved Pets Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Mis Perros Registrados (${userPetsState.value.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      if (userPetsState.value.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Aún no tienes perros registrados",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Completa los datos arriba y pulsa 'Guardar en Mis Perros' para llevar el control de su edad.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        items(userPetsState.value, key = { it.id }) { pet ->
          RegisteredPetCard(
            pet = pet,
            onRecalculate = {
              dogName = pet.name
              dogBreed = pet.breed
              years = pet.ageYears
              months = pet.ageMonths
              selectedSize = DogSizeCategory.fromString(pet.sizeCategory)
            },
            onDelete = {
              scope.launch {
                try {
                  petRepository.deletePet(pet.id)
                  snackbarHostState.showSnackbar("${pet.name} eliminado de tus registros.")
                } catch (e: Exception) {
                  snackbarHostState.showSnackbar("Error al eliminar: ${e.localizedMessage}")
                }
              }
            }
          )
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
    )
  }
}

@Composable
private fun AgeResultHeroCard(
  dogName: String,
  humanYears: Double,
  stage: DogLifeStage,
  summary: String,
  chronologicalYears: Int,
  chronologicalMonths: Int
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("age_result_hero_card"),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              MaterialTheme.colorScheme.surface
            )
          )
        )
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = dogName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "$chronologicalYears años, $chronologicalMonths meses caninos",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color(stage.badgeColorHex))
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = stage.title,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.5.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large expressive human age display
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = "${humanYears.toInt()}",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 64.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
              text = "AÑOS HUMANOS",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "equivalentes aproximados",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Life Stage Progress Indicator
        val stageProgress = when (stage) {
          DogLifeStage.PUPPY -> 0.25f
          DogLifeStage.ADULT -> 0.65f
          DogLifeStage.SENIOR -> 0.95f
        }

        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Cachorro", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Adulto", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Senior", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { stageProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = summary,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@Composable
private fun AdviceCard(
  icon: ImageVector,
  title: String,
  advice: String,
  tint: Color
) {
  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = advice,
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )
      }
    }
  }
}

@Composable
private fun RegisteredPetCard(
  pet: Pet,
  onRecalculate: () -> Unit,
  onDelete: () -> Unit
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onRecalculate() }
      .testTag("pet_card_${pet.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = pet.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${pet.breed} • ${pet.ageYears}a ${pet.ageMonths}m",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "≈ ${pet.humanAgeYears.toInt()} años humanos",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Filled.Delete,
          contentDescription = "Eliminar mascota",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

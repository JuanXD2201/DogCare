package com.example.presentation.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.CartRepository
import com.example.data.repository.ProductRepository
import com.example.domain.model.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StoreScreen(
  productRepository: ProductRepository,
  cartRepository: CartRepository,
  userRole: String? = "user",
  onOpenCart: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val productsState by productRepository.observeProducts().collectAsState(initial = emptyList())
  val categoriesState by productRepository.observeCategories().collectAsState(initial = productRepository.getDefaultCategories())

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
  val myUid = Firebase.auth.currentUser?.uid // cualquier usuario con sesión puede publicar y gestionar sus propios productos

  // Admin dialog state
  var showAdminDialog by remember { mutableStateOf(false) }
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var productToDelete by remember { mutableStateOf<Product?>(null) }

  val filteredProducts = remember(productsState, searchQuery, selectedCategoryKey) {
    productsState.filter { product ->
      val matchesSearch = searchQuery.isBlank() ||
        product.name.contains(searchQuery, ignoreCase = true) ||
        product.brand.contains(searchQuery, ignoreCase = true)

      val matchesCategory = selectedCategoryKey == null ||
        product.category.equals(selectedCategoryKey, ignoreCase = true)

      matchesSearch && matchesCategory
    }
  }

  val copFormat = remember {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
      maximumFractionDigits = 0
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Search Bar and Category Chips Header
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Buscar alimento o snack canino...", fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                  }
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("store_search_input"),
              shape = RoundedCornerShape(24.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
              ),
              singleLine = true
            )

            if (myUid != null) {
              Spacer(modifier = Modifier.width(10.dp))
              IconButton(
                onClick = {
                  editingProduct = null
                  showAdminDialog = true
                },
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer)
                  .testTag("store_admin_button")
              ) {
                Icon(
                  imageVector = Icons.Filled.Add,
                  contentDescription = "Publicar producto",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Category Chips with active count
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            item {
              FilterChip(
                selected = selectedCategoryKey == null,
                onClick = { selectedCategoryKey = null },
                label = { Text("Todos (${productsState.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_cat_all")
              )
            }
            items(categoriesState, key = { it.id }) { cat ->
              val isSelected = selectedCategoryKey == cat.id
              FilterChip(
                selected = isSelected,
                onClick = { selectedCategoryKey = if (isSelected) null else cat.id },
                label = { Text(cat.name, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_cat_${cat.id}")
              )
            }
          }
        }
      }

      // Products List
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
      ) {
        if (filteredProducts.isEmpty()) {
          item {
            ElevatedCard(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Filled.Restaurant,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                val emptyText = if (productsState.isEmpty()) {
                  "Aún no hay productos publicados. Toca + para publicar el primero."
                } else {
                  "No se encontraron alimentos con estos filtros."
                }
                Text(
                  text = emptyText,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Medium,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        } else {
          items(filteredProducts, key = { it.id }) { product ->
            ProductCard(
              product = product,
              formattedPrice = copFormat.format(product.priceCop),
              isOwner = myUid != null && product.sellerUid == myUid,
              onAddToCart = { quantity ->
                val res = cartRepository.addToCart(product, quantity)
                scope.launch {
                  if (res.isSuccess) {
                    snackbarHostState.showSnackbar("¡$quantity x ${product.name} añadido al carrito!")
                  } else {
                    snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error al agregar")
                  }
                }
              },
              onEdit = {
                editingProduct = product
                showAdminDialog = true
              },
              onDelete = { productToDelete = product }
            )
          }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
      }
    }

    // Snackbar Host
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
    )

    // Confirmación antes de eliminar un producto propio
    productToDelete?.let { target ->
      AlertDialog(
        onDismissRequest = { productToDelete = null },
        title = { Text("Eliminar producto") },
        text = { Text("¿Eliminar \"${target.name}\" de la tienda? Esta acción no se puede deshacer.") },
        confirmButton = {
          TextButton(onClick = {
            productToDelete = null
            scope.launch {
              try {
                productRepository.deleteProduct(target.id)
                snackbarHostState.showSnackbar("${target.name} eliminado de la tienda.")
              } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al eliminar: ${e.localizedMessage}")
              }
            }
          }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { productToDelete = null }) { Text("Cancelar") } }
      )
    }

    // Diálogo para publicar o editar un producto propio
    if (showAdminDialog) {
      AdminProductDialog(
        initialProduct = editingProduct,
        onDismiss = { showAdminDialog = false },
        onSave = { updatedProduct ->
          scope.launch {
            try {
              if (editingProduct == null) {
                productRepository.addProduct(updatedProduct)
                snackbarHostState.showSnackbar("Producto creado exitosamente.")
              } else {
                productRepository.updateProduct(updatedProduct)
                snackbarHostState.showSnackbar("Producto actualizado.")
              }
              showAdminDialog = false
            } catch (e: Exception) {
              snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
            }
          }
        }
      )
    }
  }
}

@Composable
private fun ProductCard(
  product: Product,
  formattedPrice: String,
  isOwner: Boolean,
  onAddToCart: (Int) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var selectedQty by remember { mutableIntStateOf(1) }

  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("product_card_${product.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.5.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Thumbnail with fallback
        Box(
          modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          if (product.imageUrl.isNotBlank()) {
            AsyncImage(
              model = ImageRequest.Builder(LocalContext.current)
                .data(product.imageUrl)
                .crossfade(true)
                .build(),
              contentDescription = product.name,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(
              imageVector = Icons.Filled.Pets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = product.brand.uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.secondary,
              letterSpacing = 0.5.sp
            )

            // Stock Badge
            val stockBadgeColor = when {
              product.stock > 5 -> MaterialTheme.colorScheme.primaryContainer
              product.stock in 1..5 -> MaterialTheme.colorScheme.tertiaryContainer
              else -> MaterialTheme.colorScheme.errorContainer
            }
            val stockTextColor = when {
              product.stock > 5 -> MaterialTheme.colorScheme.onPrimaryContainer
              product.stock in 1..5 -> MaterialTheme.colorScheme.onTertiaryContainer
              else -> MaterialTheme.colorScheme.onErrorContainer
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(stockBadgeColor)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                text = if (product.stock > 0) "${product.stock} disp." else "Agotado",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = stockTextColor
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = product.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Presentación: ${product.presentation}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = formattedPrice,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      if (product.sellerName.isNotBlank()) {
        Text(
          text = if (isOwner) "Publicado por ti" else "Vendido por ${product.sellerName}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (product.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = product.description,
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 17.sp
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom Row: Stepper + Add to Cart
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isOwner) {
          Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
          }
        } else {
          // Quantity Selector
          if (product.stock > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
              IconButton(
                onClick = { if (selectedQty > 1) selectedQty-- },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Filled.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
              }
              Text(
                text = "$selectedQty",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
              )
              IconButton(
                onClick = { if (selectedQty < product.stock) selectedQty++ },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
              }
            }
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }
        }

        Button(
          onClick = { onAddToCart(selectedQty) },
          enabled = product.stock > 0,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("add_to_cart_btn_${product.id}")
        ) {
          Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Agregar al Carrito", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AdminProductDialog(
  initialProduct: Product?,
  onDismiss: () -> Unit,
  onSave: (Product) -> Unit
) {
  var name by remember { mutableStateOf(initialProduct?.name ?: "") }
  var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
  var category by remember { mutableStateOf(initialProduct?.category ?: "dry_food") }
  var lifeStage by remember { mutableStateOf(initialProduct?.lifeStage ?: "adult") }
  var presentation by remember { mutableStateOf(initialProduct?.presentation ?: "2 kg") }
  var priceText by remember { mutableStateOf(initialProduct?.priceCop?.toInt()?.toString() ?: "") }
  var stockText by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }
  var description by remember { mutableStateOf(initialProduct?.description ?: "") }
  var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "") }
  var active by remember { mutableStateOf(initialProduct?.active ?: true) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val validCategories = listOf("dry_food", "wet_food", "snacks")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initialProduct == null) "Publicar alimento canino" else "Editar alimento canino")
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (validationError != null) {
          Text(
            text = validationError!!,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it; validationError = null },
          label = { Text("Nombre del producto") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        OutlinedTextField(
          value = brand,
          onValueChange = { brand = it; validationError = null },
          label = { Text("Marca (ej: Royal Canin)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Text(
          text = "Solo se permiten alimentos para perros (concentrado, húmedo o snacks).",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          validCategories.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = {
                Text(
                  when (cat) {
                    "dry_food" -> "Seco"
                    "wet_food" -> "Húmedo"
                    else -> "Snacks"
                  },
                  fontSize = 11.sp
                )
              }
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it; validationError = null },
            label = { Text("Precio COP") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
          OutlinedTextField(
            value = stockText,
            onValueChange = { stockText = it; validationError = null },
            label = { Text("Stock") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("all_stages" to "Todas", "puppy" to "Cachorro", "adult" to "Adulto", "senior" to "Senior").forEach { (key, label) ->
            FilterChip(
              selected = lifeStage == key,
              onClick = { lifeStage = key },
              label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
            )
          }
        }

        OutlinedTextField(
          value = presentation,
          onValueChange = { presentation = it },
          label = { Text("Presentación (ej: 2 kg, 370 g)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3
        )

        OutlinedTextField(
          value = imageUrl,
          onValueChange = { imageUrl = it; validationError = null },
          label = { Text("URL de la imagen (https://...)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Visible en la tienda", fontSize = 13.sp)
          Switch(checked = active, onCheckedChange = { active = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val price = priceText.toDoubleOrNull()
          val stock = stockText.toIntOrNull()
          val image = imageUrl.trim()
          validationError = when {
            name.isBlank() || brand.isBlank() -> "Nombre y marca son obligatorios."
            name.trim().length > 100 || brand.trim().length > 60 -> "Nombre (máx. 100) o marca (máx. 60) demasiado largos."
            price == null || price < 0 -> "Ingresa un precio válido (0 o mayor)."
            stock == null || stock < 0 -> "Ingresa un stock válido (0 o mayor)."
            presentation.trim().length > 100 || description.trim().length > 1000 -> "Presentación (máx. 100) o descripción (máx. 1000) demasiado largas."
            image.isNotEmpty() && (!image.startsWith("https://") || image.length > 500) -> "La imagen debe ser una URL https de hasta 500 caracteres."
            else -> null
          }
          if (validationError != null || price == null || stock == null) return@Button

          val productToSave = Product(
            id = initialProduct?.id ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            brand = brand.trim(),
            category = category,
            lifeStage = lifeStage,
            presentation = presentation.trim(),
            priceCop = price,
            stock = stock,
            description = description.trim(),
            imageUrl = image,
            species = "dog",
            active = active
          )
          onSave(productToSave)
        }
      ) {
        Text("Guardar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

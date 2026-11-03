package com.example.presentation.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.repository.OsmTechnicalException
import com.example.data.repository.VetClinicRepository
import com.example.domain.model.VetClinic
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

enum class MapViewMode {
  SPLIT,
  FULL_MAP,
  LIST_ONLY
}

private val QUICK_COLOMBIAN_CITIES = listOf(
  "Bogotá",
  "Medellín",
  "Cali",
  "Barranquilla",
  "Bucaramanga",
  "Pereira",
  "Cartagena"
)

@SuppressLint("MissingPermission")
@Composable
fun VetMapScreen(
  vetRepository: VetClinicRepository
) {
  val context = LocalContext.current
  val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

  // Set osmdroid userAgent
  remember {
    Configuration.getInstance().userAgentValue = "DogCare/1.0 (com.aistudio.dogcare.kprmdx)"
  }

  var userLat by remember { mutableStateOf<Double?>(null) }
  var userLng by remember { mutableStateOf<Double?>(null) }
  var currentCenterLat by remember { mutableStateOf<Double?>(null) }
  var currentCenterLng by remember { mutableStateOf<Double?>(null) }

  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  var showPermissionRationale by remember { mutableStateOf(!hasLocationPermission) }
  var searchQuery by remember { mutableStateOf("") }
  var onlyEmergency by remember { mutableStateOf(false) }
  var viewMode by remember { mutableStateOf(MapViewMode.SPLIT) }

  var clinics by remember { mutableStateOf<List<VetClinic>>(emptyList()) }
  var selectedClinic by remember { mutableStateOf<VetClinic?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var userFacingErrorMessage by remember { mutableStateOf<String?>(null) }
  var technicalErrorDetail by remember { mutableStateOf<String?>(null) }
  var retryTrigger by remember { mutableIntStateOf(0) }
  var mapRef by remember { mutableStateOf<MapView?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasLocationPermission = fineGranted || coarseGranted
    showPermissionRationale = false

    if (hasLocationPermission) {
      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
          userLat = loc.latitude
          userLng = loc.longitude
          currentCenterLat = loc.latitude
          currentCenterLng = loc.longitude
          mapRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
          mapRef?.controller?.setZoom(14.0)
        }
      }
    }
  }

  // Request initial location if permission is already granted
  LaunchedEffect(hasLocationPermission) {
    if (hasLocationPermission && userLat == null) {
      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
          userLat = loc.latitude
          userLng = loc.longitude
          currentCenterLat = loc.latitude
          currentCenterLng = loc.longitude
          mapRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
          mapRef?.controller?.setZoom(14.0)
        }
      }
    }
  }

  // Fetch real OSM clinics upon location, search, filter, or retry changes
  LaunchedEffect(userLat, userLng, onlyEmergency, searchQuery, retryTrigger) {
    isLoading = true
    userFacingErrorMessage = null
    technicalErrorDetail = null

    val searchResult = vetRepository.searchClinics(
      userLat = userLat,
      userLng = userLng,
      query = searchQuery,
      onlyEmergency = onlyEmergency
    )

    if (searchResult.isSuccess) {
      val data = searchResult.getOrNull()
      clinics = data?.clinics ?: emptyList()
      userFacingErrorMessage = null
      technicalErrorDetail = null

      // Center map on searched city if query was geocoded
      if (data?.centerLat != null && data.centerLng != null) {
        currentCenterLat = data.centerLat
        currentCenterLng = data.centerLng
        mapRef?.let { map ->
          val targetPoint = GeoPoint(data.centerLat, data.centerLng)
          map.controller.animateTo(targetPoint)
          if (data.isCitySearch) {
            map.controller.setZoom(13.0)
          }
        }
      }

      if (selectedClinic == null && clinics.isNotEmpty()) {
        selectedClinic = clinics.first()
      } else if (clinics.none { it.id == selectedClinic?.id }) {
        selectedClinic = clinics.firstOrNull()
      }
    } else {
      clinics = emptyList()
      selectedClinic = null
      val exception = searchResult.exceptionOrNull()
      userFacingErrorMessage = "No se pudo consultar OpenStreetMap. Revisa tu conexión e intenta de nuevo"
      technicalErrorDetail = if (exception is OsmTechnicalException) {
        exception.technicalDetail
      } else {
        exception?.localizedMessage ?: "Error de red desconocido"
      }
    }
    isLoading = false

    // Update markers on osmdroid MapView
    mapRef?.let { mapView ->
      try {
        mapView.overlays.clear()
        clinics.forEach { clinic ->
          val marker = Marker(mapView).apply {
            position = GeoPoint(clinic.latitude, clinic.longitude)
            title = clinic.name
            snippet = "${clinic.address}\nTel: ${clinic.phone}"
            setOnMarkerClickListener { _, _ ->
              selectedClinic = clinic
              mapView.controller.animateTo(GeoPoint(clinic.latitude, clinic.longitude))
              true
            }
          }
          mapView.overlays.add(marker)
        }
        mapView.invalidate()
      } catch (_: Exception) {}
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1) Header with Search Bar, Quick City Chips & View Mode
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        // Search text field
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              "Buscar ciudad (ej. Bogotá, Cali) o veterinaria...",
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          },
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Limpiar búsqueda",
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier
                    .size(20.dp)
                    .padding(end = 6.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("map_search_input"),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Colombian City Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Filled.LocationCity,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )

          QUICK_COLOMBIAN_CITIES.forEach { city ->
            val isSelected = searchQuery.equals(city, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier
                .clickable {
                  searchQuery = city
                }
                .testTag("quick_city_$city")
            ) {
              Text(
                text = city,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters and View Mode row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            FilterChip(
              selected = !onlyEmergency,
              onClick = { onlyEmergency = false },
              label = { Text("Todas (${clinics.size})", fontSize = 11.5.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.testTag("filter_all_vets")
            )

            FilterChip(
              selected = onlyEmergency,
              onClick = { onlyEmergency = true },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Filled.Emergency,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.error
                )
              },
              label = {
                Text(
                  "Solo Urgencias 24h",
                  fontSize = 11.5.sp,
                  color = if (onlyEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
              ),
              modifier = Modifier.testTag("filter_emergency_vets")
            )
          }

          // View mode toggle button
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
              .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                viewMode = when (viewMode) {
                  MapViewMode.FULL_MAP -> MapViewMode.SPLIT
                  MapViewMode.SPLIT -> MapViewMode.LIST_ONLY
                  MapViewMode.LIST_ONLY -> MapViewMode.SPLIT
                }
              },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(
                imageVector = when (viewMode) {
                  MapViewMode.SPLIT -> Icons.Filled.Map
                  MapViewMode.FULL_MAP -> Icons.Filled.FormatListBulleted
                  MapViewMode.LIST_ONLY -> Icons.Filled.Map
                },
                contentDescription = "Cambiar vista",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }

    // Permission Banner (if not granted)
    AnimatedVisibility(
      visible = showPermissionRationale && !hasLocationPermission,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Activa tu ubicación para ubicar veterinarias cercanas automáticamente.",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
          Button(
            onClick = {
              permissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION
                )
              )
            },
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
          ) {
            Text("Activar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Error State Banner if OpenStreetMap / Overpass query failed
    AnimatedVisibility(
      visible = userFacingErrorMessage != null && !isLoading,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
          .testTag("osm_error_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Filled.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = userFacingErrorMessage ?: "No se pudo consultar OpenStreetMap. Revisa tu conexión e intenta de nuevo",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
              if (!technicalErrorDetail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Detalle técnico: $technicalErrorDetail",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { retryTrigger++ },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("retry_osm_button")
            ) {
              Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reintentar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Main Content Area: Map and List
    if (viewMode == MapViewMode.LIST_ONLY) {
      // Full List Mode
      ClinicListView(
        clinics = clinics,
        selectedClinic = selectedClinic,
        userLat = userLat,
        userLng = userLng,
        isLoading = isLoading,
        userFacingErrorMessage = userFacingErrorMessage,
        searchQuery = searchQuery,
        onSelect = { clinic ->
          selectedClinic = clinic
          viewMode = MapViewMode.SPLIT
          mapRef?.controller?.animateTo(GeoPoint(clinic.latitude, clinic.longitude))
        },
        vetRepository = vetRepository,
        onActivateLocation = {
          permissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
            )
          )
        }
      )
    } else {
      // Map View (Full or Split)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(if (viewMode == MapViewMode.FULL_MAP) 1f else 0.55f)
          .testTag("osmdroid_map_container")
      ) {
        AndroidView(
          factory = { ctx ->
            MapView(ctx).apply {
              setTileSource(TileSourceFactory.MAPNIK)
              setMultiTouchControls(true)
              val startPoint = GeoPoint(userLat ?: 4.6853, userLng ?: -74.0531)
              controller.setZoom(13.0)
              controller.setCenter(startPoint)
              mapRef = this
            }
          },
          update = { mapView ->
            mapRef = mapView
          },
          modifier = Modifier.fillMaxSize()
        )

        // Floating Map Controls (Zoom In/Out, Recenter, GPS)
        Column(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // GPS My Location Button
          FloatingActionButton(
            onClick = {
              if (hasLocationPermission) {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                  if (loc != null) {
                    userLat = loc.latitude
                    userLng = loc.longitude
                    currentCenterLat = loc.latitude
                    currentCenterLng = loc.longitude
                    mapRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    mapRef?.controller?.setZoom(14.5)
                  }
                }
              } else {
                permissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                  )
                )
              }
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = if (hasLocationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            elevation = FloatingActionButtonDefaults.elevation(3.dp),
            modifier = Modifier
              .size(42.dp)
              .testTag("my_location_button")
          ) {
            Icon(
              imageVector = Icons.Filled.MyLocation,
              contentDescription = "Mi ubicación",
              modifier = Modifier.size(20.dp)
            )
          }

          // Recenter on Searched City / Current Center
          if (currentCenterLat != null && currentCenterLng != null) {
            FloatingActionButton(
              onClick = {
                mapRef?.controller?.animateTo(GeoPoint(currentCenterLat!!, currentCenterLng!!))
                mapRef?.controller?.setZoom(13.0)
              },
              shape = CircleShape,
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.primary,
              elevation = FloatingActionButtonDefaults.elevation(3.dp),
              modifier = Modifier
                .size(42.dp)
                .testTag("recenter_button")
            ) {
              Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = "Recentrar",
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Zoom In (+)
          FloatingActionButton(
            onClick = { mapRef?.controller?.zoomIn() },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(3.dp),
            modifier = Modifier.size(38.dp)
          ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Acercar", modifier = Modifier.size(18.dp))
          }

          // Zoom Out (-)
          FloatingActionButton(
            onClick = { mapRef?.controller?.zoomOut() },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(3.dp),
            modifier = Modifier.size(38.dp)
          ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Alejar", modifier = Modifier.size(18.dp))
          }
        }

        // Clean OpenStreetMap Contributors Attribution Badge in bottom right
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
          shape = RoundedCornerShape(topStart = 8.dp),
          modifier = Modifier.align(Alignment.BottomEnd)
        ) {
          Text(
            text = "© OpenStreetMap contributors",
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        // Floating Selected Clinic Card (like Google Maps) on top of the map
        selectedClinic?.let { clinic ->
          FloatingClinicPreviewCard(
            clinic = clinic,
            userLat = userLat,
            userLng = userLng,
            onClose = { selectedClinic = null },
            onNavigate = { vetRepository.openNavigationInGoogleMaps(clinic) },
            onCall = { vetRepository.dialPhoneNumber(clinic.phone) },
            onOpenOsmSource = { vetRepository.openOsmSourceUrl(clinic.osmSourceUrl) },
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(horizontal = 14.dp, vertical = 10.dp)
          )
        }
      }

      // Bottom half: scrollable list in SPLIT mode
      if (viewMode == MapViewMode.SPLIT) {
        ClinicListView(
          clinics = clinics,
          selectedClinic = selectedClinic,
          userLat = userLat,
          userLng = userLng,
          isLoading = isLoading,
          userFacingErrorMessage = userFacingErrorMessage,
          searchQuery = searchQuery,
          onSelect = { clinic ->
            selectedClinic = clinic
            mapRef?.controller?.animateTo(GeoPoint(clinic.latitude, clinic.longitude))
            mapRef?.controller?.setZoom(15.0)
          },
          vetRepository = vetRepository,
          onActivateLocation = {
            permissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          },
          modifier = Modifier.weight(0.45f)
        )
      }
    }
  }
}

/**
 * Modern floating bottom sheet card previewing the selected clinic over the map.
 */
@Composable
private fun FloatingClinicPreviewCard(
  clinic: VetClinic,
  userLat: Double?,
  userLng: Double?,
  onClose: () -> Unit,
  onNavigate: () -> Unit,
  onCall: () -> Unit,
  onOpenOsmSource: () -> Unit,
  modifier: Modifier = Modifier
) {
  ElevatedCard(
    modifier = modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(18.dp))
      .testTag("floating_clinic_preview_${clinic.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = clinic.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = clinic.address,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (clinic.is24hEmergency) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                text = "24H VERIFICADO",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Cerrar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (userLat != null && userLng != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "A ${clinic.formatDistance(userLat, userLng)}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        } else {
          Text(
            text = clinic.openingHours,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = if (clinic.phone == "Sin teléfono registrado") "Sin teléfono" else "Tel: ${clinic.phone}",
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onCall,
          enabled = clinic.phone != "Sin teléfono registrado",
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Llamar", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = onOpenOsmSource,
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Fuente OSM", fontSize = 11.sp)
        }

        Button(
          onClick = onNavigate,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1.2f)
            .height(36.dp)
            .testTag("directions_button_${clinic.id}"),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cómo llegar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * List of clinics with clean spacing and empty/prompt states.
 */
@Composable
private fun ClinicListView(
  clinics: List<VetClinic>,
  selectedClinic: VetClinic?,
  userLat: Double?,
  userLng: Double?,
  isLoading: Boolean,
  userFacingErrorMessage: String?,
  searchQuery: String,
  onSelect: (VetClinic) -> Unit,
  vetRepository: VetClinicRepository,
  onActivateLocation: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(vertical = 10.dp)
  ) {
    if (clinics.isEmpty() && !isLoading && userFacingErrorMessage == null) {
      if (userLat == null && searchQuery.isBlank()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
              .testTag("osm_prompt_location_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text("Explora veterinarias reales", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Toca una ciudad colombiana arriba (ej. Bogotá, Medellín) o activa tu GPS para consultar clínicas en OpenStreetMap.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = onActivateLocation,
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Activar ubicación", fontSize = 12.sp)
              }
            }
          }
        }
      } else {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
              .testTag("osm_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No hay veterinarias registradas en OpenStreetMap en 5 km. Prueba otra ciudad",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Puedes seleccionar otra ciudad colombiana arriba para explorar clínicas reales en esa zona.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    } else {
      items(clinics, key = { it.id }) { clinic ->
        ClinicCard(
          clinic = clinic,
          userLat = userLat,
          userLng = userLng,
          isSelected = selectedClinic?.id == clinic.id,
          onSelect = { onSelect(clinic) },
          onNavigate = { vetRepository.openNavigationInGoogleMaps(clinic) },
          onCall = { vetRepository.dialPhoneNumber(clinic.phone) },
          onOpenOsmSource = { vetRepository.openOsmSourceUrl(clinic.osmSourceUrl) }
        )
      }
    }
  }
}

@Composable
private fun ClinicCard(
  clinic: VetClinic,
  userLat: Double?,
  userLng: Double?,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onNavigate: () -> Unit,
  onCall: () -> Unit,
  onOpenOsmSource: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelect() }
      .testTag("clinic_card_${clinic.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      else MaterialTheme.colorScheme.surface
    ),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    else null,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = clinic.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = clinic.address,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (clinic.is24hEmergency) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.errorContainer)
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Emergency,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "24H VERIFICADO",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (userLat != null && userLng != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "A ${clinic.formatDistance(userLat, userLng)}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        } else {
          Text(
            text = clinic.openingHours,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = if (clinic.phone == "Sin teléfono registrado") "Sin teléfono" else "Tel: ${clinic.phone}",
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onCall,
          enabled = clinic.phone != "Sin teléfono registrado",
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Llamar", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = onOpenOsmSource,
          modifier = Modifier
            .weight(1f)
            .height(36.dp),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Fuente OSM", fontSize = 11.sp)
        }

        Button(
          onClick = onNavigate,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1.2f)
            .height(36.dp)
            .testTag("directions_button_${clinic.id}"),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
          Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cómo llegar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

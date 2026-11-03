package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DogCareTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  CALCULATOR("Edad & Perros", Icons.Filled.Pets, Icons.Outlined.Pets, "tab_calculator"),
  STORE("Tienda", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "tab_store"),
  AI_CHAT("Asistente IA", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "tab_ai_chat"),
  VET_MAP("Veterinarias", Icons.Filled.LocationOn, Icons.Outlined.LocationOn, "tab_vet_map"),
  COMMUNITY("Comunidad", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "tab_community")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogCareTopBar(
  title: String,
  cartItemCount: Int = 0,
  onCartClick: (() -> Unit)? = null,
  actions: @Composable () -> Unit = {}
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 1.dp
  ) {
    CenterAlignedTopAppBar(
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.Pets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(19.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = title,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      },
      actions = {
        if (onCartClick != null) {
          IconButton(
            onClick = onCartClick,
            modifier = Modifier.testTag("topbar_cart_button")
          ) {
            BadgedBox(
              badge = {
                if (cartItemCount > 0) {
                  Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                  ) {
                    Text(cartItemCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = "Carrito de compras",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
        actions()
      },
      colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
      )
    )
  }
}

@Composable
fun DogCareBottomBar(
  currentTab: DogCareTab,
  onTabSelected: (DogCareTab) -> Unit,
  cartBadgeCount: Int = 0
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shadowElevation = 8.dp
  ) {
    NavigationBar(
      containerColor = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      modifier = Modifier.navigationBarsPadding()
    ) {
      DogCareTab.entries.forEach { tab ->
        val isSelected = currentTab == tab
        NavigationBarItem(
          selected = isSelected,
          onClick = { onTabSelected(tab) },
          icon = {
            if (tab == DogCareTab.STORE && cartBadgeCount > 0) {
              BadgedBox(
                badge = {
                  Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                  ) {
                    Text(cartBadgeCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(
                  imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label
                )
              }
            } else {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label
              )
            }
          },
          label = {
            Text(
              text = tab.label,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.testTag(tab.testTag)
        )
      }
    }
  }
}

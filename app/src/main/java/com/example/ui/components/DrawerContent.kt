package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.MusicViewModel

@Composable
fun DrawerContent(
    viewModel: MusicViewModel,
    onNavigateToSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.allTracks.collectAsState()
    val favoritesCount = remember(tracks) { tracks.count { it.isFavorite } }
    val totalSongs = tracks.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. BRAND HEADER Section
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicVideo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Amusic",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Premium Player",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(24.dp))

            // 2. DRAWER MENU NAVIGATION ITEMS
            Text(
                text = "NAVIGATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            DrawerItemRow(
                title = "Songs Library",
                counter = "$totalSongs items",
                icon = Icons.Default.MusicNote,
                onClick = {
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                title = "Favorites Sync",
                counter = "$favoritesCount tracks",
                icon = Icons.Default.Favorite,
                onClick = {
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                title = "Equalizer Preferences",
                counter = "Active",
                icon = Icons.Default.Equalizer,
                onClick = {
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                title = "Toggles Settings",
                counter = "Prefs",
                icon = Icons.Default.Settings,
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                },
                tag = "drawer_settings_button"
            )
        }

        // 3. STORAGE & AUTHORIZATION STATUS footer Section
        Column {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Local Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (viewModel.hasPermission.collectAsState().value) "Storage Allowed ✔" else "Storage Access Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (viewModel.hasPermission.collectAsState().value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "v1.0.0 Alpha • OpenSource",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun DrawerItemRow(
    title: String,
    counter: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (counter != null) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = counter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

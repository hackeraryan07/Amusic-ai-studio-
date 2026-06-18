package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.MusicViewModel

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val showArtist by viewModel.showArtist.collectAsState()
    val showDuration by viewModel.showDuration.collectAsState()
    val showAlbumNameSelection by viewModel.showAlbumName.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. CARD: METADATA VISIBILITY PREFERENCES
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Track Display Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure what metadata is visible inside the music items lists across the application.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle 1: Album Cover Image
                SettingToggleRow(
                    title = "Show Album Art",
                    subtitle = "Draw album thumbnails using Glide image loader",
                    icon = Icons.Default.Image,
                    checked = showAlbumArt,
                    onCheckedChange = { viewModel.setMetadataSetting("show_album_art", it) },
                    tag = "setting_toggle_art"
                )

                // Toggle 2: Artist Title
                SettingToggleRow(
                    title = "Show Artist Name",
                    subtitle = "Display track singer/composer credit label",
                    icon = Icons.Default.Person,
                    checked = showArtist,
                    onCheckedChange = { viewModel.setMetadataSetting("show_artist", it) },
                    tag = "setting_toggle_artist"
                )

                // Toggle 3: Track Length (Duration)
                SettingToggleRow(
                    title = "Show Track Duration",
                    subtitle = "Display song runtime details in minutes",
                    icon = Icons.Default.Timelapse,
                    checked = showDuration,
                    onCheckedChange = { viewModel.setMetadataSetting("show_duration", it) },
                    tag = "setting_toggle_duration"
                )

                // Toggle 4: Album Name Subtitle
                SettingToggleRow(
                    title = "Show Album Subtitle",
                    subtitle = "Draw additional album project title annotations",
                    icon = Icons.Default.Album,
                    checked = showAlbumNameSelection,
                    onCheckedChange = { viewModel.setMetadataSetting("show_album_name", it) },
                    tag = "setting_toggle_album"
                )
            }
        }

        // B. CARD: DATABASE MAINTENANCE ACTIONS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Database & Scanning Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sync cache values, re-scan local storage tables, or reset to original streaming demo seeds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refresh Local Library",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hasPermission) "Permissions allowed. Scans MediaStore." else "Request media read permission",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.scanForLocalAudio() },
                        enabled = !isScanning && hasPermission,
                        modifier = Modifier.testTag("rescan_library_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(size = 18.dp, strokeWidth = 2.dp)
                        } else {
                            Text("Scan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset Database Cache",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cleans active tables and re-initializes stock tracks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.forceResetDatabase() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier.testTag("reset_database_button")
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        // ABOUT APP & LOGO BRUSH
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicVideo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Amusic v1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A Premium Jetpack Compose Open Music Service",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, strokeWidth: androidx.compose.ui.unit.Dp) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        strokeWidth = strokeWidth,
        color = MaterialTheme.colorScheme.primary
    )
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.database.TrackEntity
import com.example.ui.components.TrackArt
import com.example.ui.components.TrackItem
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.SortType

@Composable
fun SongsScreen(
    viewModel: MusicViewModel,
    tracks: List<TrackEntity>,
    searchQuery: String,
    sortType: SortType,
    onTrackSelect: (TrackEntity, List<TrackEntity>) -> Unit
) {
    val showAlbumArt by viewModel.showAlbumArt.collectAsState()
    val showArtist by viewModel.showArtist.collectAsState()
    val showDuration by viewModel.showDuration.collectAsState()
    val showAlbumNameByViewModel by viewModel.showAlbumName.collectAsState()

    // 1. Live Filter and Sort Calculations
    val filteredTracks = remember(tracks, searchQuery, sortType) {
        tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }.let { list ->
            when (sortType) {
                SortType.NAME -> list.sortedBy { it.title.lowercase() }
                SortType.ARTIST -> list.sortedBy { it.artist.lowercase() }
                SortType.DURATION -> list.sortedByDescending { it.duration }
                SortType.MODIFIED_DATE -> list.sortedByDescending { it.dateModified }
            }
        }
    }

    if (filteredTracks.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.MusicNote,
            text = "No songs found matches. Try scanning or reset database in Settings!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredTracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    onClick = { onTrackSelect(track, filteredTracks) },
                    showAlbumArt = showAlbumArt,
                    showArtist = showArtist,
                    showDuration = showDuration,
                    showAlbum = showAlbumNameByViewModel,
                    isFavorite = track.isFavorite,
                    onFavoriteToggle = { fav -> viewModel.toggleFavorite(track.id, fav) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
            }
        }
    }
}

@Composable
fun ArtistsScreen(
    viewModel: MusicViewModel,
    tracks: List<TrackEntity>,
    searchQuery: String,
    onTrackSelect: (TrackEntity, List<TrackEntity>) -> Unit
) {
    // Group tracks by artist name
    val artistGroups = remember(tracks, searchQuery) {
        tracks.filter {
            it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.title.contains(searchQuery, ignoreCase = true)
        }.groupBy { it.artist }
    }

    var selectedArtist by remember { mutableStateOf<String?>(null) }

    if (artistGroups.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Person, text = "No Artists found")
        return
    }

    if (selectedArtist != null) {
        // Render songs list for selected artist
        val artistSongs = artistGroups[selectedArtist] ?: emptyList()
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedArtist = null }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back to Artists (${selectedArtist})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(artistSongs) { track ->
                    TrackItem(
                        track = track,
                        onClick = { onTrackSelect(track, artistSongs) },
                        showAlbumArt = true,
                        showArtist = false,
                        showDuration = true,
                        showAlbum = true,
                        isFavorite = track.isFavorite,
                        onFavoriteToggle = { fav -> viewModel.toggleFavorite(track.id, fav) }
                    )
                }
            }
        }
    } else {
        // List artists in cards
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
        ) {
            items(artistGroups.keys.toList()) { artist ->
                val songsCount = artistGroups[artist]?.size ?: 0
                val artistArt = when (artist) {
                    "SoundHelix" -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150"
                    else -> "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=150"
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { selectedArtist = artist },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArt(
                            model = artistArt,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$songsCount songs cataloged",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open artist songs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(
    viewModel: MusicViewModel,
    tracks: List<TrackEntity>,
    searchQuery: String,
    onTrackSelect: (TrackEntity, List<TrackEntity>) -> Unit
) {
    // Group listed tracks by Album name
    val albumGroups = remember(tracks, searchQuery) {
        tracks.filter {
            it.album.contains(searchQuery, ignoreCase = true) ||
                    it.title.contains(searchQuery, ignoreCase = true)
        }.groupBy { it.album }
    }

    var selectedAlbum by remember { mutableStateOf<String?>(null) }

    if (albumGroups.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Album, text = "No Albums found")
        return
    }

    if (selectedAlbum != null) {
        // Render content for selected album
        val albumSongs = albumGroups[selectedAlbum] ?: emptyList()
        val representativeTrack = albumSongs.firstOrNull()
        
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedAlbum = null }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back to Albums (${selectedAlbum})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(albumSongs) { track ->
                    TrackItem(
                        track = track,
                        onClick = { onTrackSelect(track, albumSongs) },
                        showAlbumArt = false, // already on album details, hide small art if preferred
                        showArtist = true,
                        showDuration = true,
                        showAlbum = false,
                        isFavorite = track.isFavorite,
                        onFavoriteToggle = { fav -> viewModel.toggleFavorite(track.id, fav) }
                    )
                }
            }
        }
    } else {
        // List albums in a nice 2x2 grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 90.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albumGroups.keys.toList()) { album ->
                val songs = albumGroups[album] ?: emptyList()
                val artist = songs.firstOrNull()?.artist ?: "Unknown Artist"
                
                // Get preloaded mock album covers for demo visual splendor, or construct custom
                val albumArt = when (album) {
                    "Helix Odyssey" -> "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=300"
                    "Ambience World" -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"
                    "Synthwave Beats" -> "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300"
                    else -> "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300"
                }

                Card(
                    onClick = { selectedAlbum = album },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        TrackArt(
                            model = albumArt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = album,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${songs.size} Audio tracks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersScreen(
    viewModel: MusicViewModel,
    tracks: List<TrackEntity>,
    searchQuery: String,
    onTrackSelect: (TrackEntity, List<TrackEntity>) -> Unit
) {
    // Group listed tracks by DirectoryFolderName
    val folderGroups = remember(tracks, searchQuery) {
        tracks.filter {
            it.folderName.contains(searchQuery, ignoreCase = true) ||
                    it.title.contains(searchQuery, ignoreCase = true)
        }.groupBy { it.folderName }
    }

    var selectedFolder by remember { mutableStateOf<String?>(null) }

    if (folderGroups.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Folder, text = "No folder structures available")
        return
    }

    if (selectedFolder != null) {
        val folderSongs = folderGroups[selectedFolder] ?: emptyList()
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedFolder = null }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back to Folders (${selectedFolder})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(folderSongs) { track ->
                    TrackItem(
                        track = track,
                        onClick = { onTrackSelect(track, folderSongs) },
                        showAlbumArt = true,
                        showArtist = true,
                        showDuration = true,
                        showAlbum = true,
                        isFavorite = track.isFavorite,
                        onFavoriteToggle = { fav -> viewModel.toggleFavorite(track.id, fav) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
        ) {
            items(folderGroups.keys.toList()) { folder ->
                val songsCount = folderGroups[folder]?.size ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { selectedFolder = folder },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Folder Icon",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$songsCount audio files inside",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    tracks: List<TrackEntity>,
    onTrackSelect: (TrackEntity, List<TrackEntity>) -> Unit
) {
    // 1. Favorites list (filter based on Room's isFavorite column)
    val favoriteTracks = remember(tracks) {
        tracks.filter { it.isFavorite }
    }

    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }

    if (selectedPlaylistName != null) {
        val playlistSongs = if (selectedPlaylistName == "Favorites") {
            favoriteTracks
        } else {
            // standard playlist fallback
            tracks
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPlaylistName = null }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Back to Playlists (${selectedPlaylistName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (playlistSongs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Favorite,
                    text = "This playlist is empty. Mark songs as favorite to populate!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(playlistSongs) { track ->
                        TrackItem(
                            track = track,
                            onClick = { onTrackSelect(track, playlistSongs) },
                            showAlbumArt = true,
                            showArtist = true,
                            showDuration = true,
                            showAlbum = true,
                            isFavorite = track.isFavorite,
                            onFavoriteToggle = { fav -> viewModel.toggleFavorite(track.id, fav) }
                        )
                    }
                }
            }
        }
    } else {
        // List active playlist categories
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp)
        ) {
            // A. Favorites Category
            item {
                PlaylistCategoryRow(
                    title = "Favorites",
                    description = "${favoriteTracks.size} Loved tracks synced",
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFE91E63),
                    onClick = { selectedPlaylistName = "Favorites" }
                )
            }
            // B. Demo streams playlist
            item {
                val demoTracksCount = tracks.count { it.isDemo }
                PlaylistCategoryRow(
                    title = "Online Demo Streams",
                    description = "$demoTracksCount public audio streams",
                    icon = Icons.Default.LibraryMusic,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { selectedPlaylistName = "Online Demo Streams" }
                )
            }
        }
    }
}

@Composable
fun PlaylistCategoryRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

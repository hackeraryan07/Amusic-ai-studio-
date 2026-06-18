package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.database.TrackEntity
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerOverlay(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.audioPlayer.currentTrack.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val position by viewModel.audioPlayer.currentPosition.collectAsState()
    val duration by viewModel.audioPlayer.duration.collectAsState()
    val cropAlbumArt by viewModel.cropAlbumArt.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    // If no song is loaded, hide the controller
    val activeTrack = currentTrack ?: return

    val density = LocalDensity.current
    val swipeThresholdPx = remember { with(density) { 56.dp.toPx() } }

    Box(modifier = modifier) {
        if (!isExpanded) {
            // A. COLLAPSED MINI PLAYER BAR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        var dragAmountX = 0f
                        var dragAmountY = 0f
                        var swipedTriggered = false
                        detectDragGestures(
                            onDragStart = {
                                dragAmountX = 0f
                                dragAmountY = 0f
                                swipedTriggered = false
                            },
                            onDragEnd = {
                                if (!swipedTriggered && kotlin.math.abs(dragAmountX) < 15f && kotlin.math.abs(dragAmountY) < 15f) {
                                    isExpanded = true
                                }
                            },
                            onDragCancel = {
                                swipedTriggered = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAmountX += dragAmount.x
                                dragAmountY += dragAmount.y
                                
                                if (!swipedTriggered) {
                                    if (dragAmountX > swipeThresholdPx) {
                                        swipedTriggered = true
                                        viewModel.playPrevious()
                                    } else if (dragAmountX < -swipeThresholdPx) {
                                        swipedTriggered = true
                                        viewModel.playNext()
                                    } else if (dragAmountY > swipeThresholdPx) {
                                        swipedTriggered = true
                                        viewModel.stopPlaybackService()
                                    }
                                }
                            }
                        )
                    }
                    .testTag("mini_player_bar"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArt(
                            model = activeTrack.path,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            centerCrop = cropAlbumArt
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTrack.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                    text = activeTrack.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play/Pause button
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.testTag("mini_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play Pause",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Skip Next button
                        IconButton(
                            onClick = { viewModel.playNext() },
                            modifier = Modifier.testTag("mini_play_next")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Progress Track Bar
                    val progressPercent = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }

    // B. EXPANDED NOW PLAYING SHEET DIALOG
    if (isExpanded) {
        ModalBottomSheet(
            onDismissRequest = { isExpanded = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxHeight(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isExpanded = false }) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Close player")
                    }
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Companion.Unspecified
                    )
                    IconButton(
                        onClick = { viewModel.toggleFavorite(activeTrack.id, !activeTrack.isFavorite) }
                    ) {
                        Icon(
                            imageVector = if (activeTrack.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (activeTrack.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Album Cover Cover art card (Large box) with horizontal swipe gesture
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .size(280.dp)
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            var dragAmountX = 0f
                            var swipedTriggered = false
                            detectDragGestures(
                                onDragStart = {
                                    dragAmountX = 0f
                                    swipedTriggered = false
                                },
                                onDragEnd = {
                                    swipedTriggered = false
                                },
                                onDragCancel = {
                                    swipedTriggered = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAmountX += dragAmount.x
                                    if (!swipedTriggered) {
                                        if (dragAmountX > swipeThresholdPx) {
                                            swipedTriggered = true
                                            viewModel.playPrevious()
                                        } else if (dragAmountX < -swipeThresholdPx) {
                                            swipedTriggered = true
                                            viewModel.playNext()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    TrackArt(
                        model = activeTrack.path,
                        modifier = Modifier.fillMaxSize(),
                        centerCrop = cropAlbumArt
                    )
                }

                // Meta Titles column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeTrack.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTrack.artist,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeTrack.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Seeking Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    var isUserSeeking by remember { mutableStateOf(false) }
                    var sliderPos by remember { mutableStateOf(0f) }

                    val activePos = if (isUserSeeking) sliderPos else position.toFloat()

                    Slider(
                        value = activePos,
                        onValueChange = {
                            isUserSeeking = true
                            sliderPos = it
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            viewModel.audioPlayer.seekTo(sliderPos.toInt())
                        },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(activePos.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(duration.toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Music Play Controllers actions row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous song
                    IconButton(
                        onClick = { viewModel.playPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Main large play button
                    PlayButtonCircular(
                        isPlaying = isPlaying,
                        onClick = { viewModel.togglePlayPause() }
                    )

                    // Next song
                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayButtonCircular(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .testTag("big_play_pause_button"),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.example.playback.AlbumArtHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TrackArt(
    model: Any?,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.MusicNote,
    placeholderColor: Color = MaterialTheme.colorScheme.primaryContainer,
    centerCrop: Boolean = true
) {
    val cached = remember(model) {
        if (model is String && !model.startsWith("http") && !model.startsWith("content")) {
            AlbumArtHelper.getCachedByteArray(model)
        } else {
            null
        }
    }

    var artBytes by remember(model) { mutableStateOf<ByteArray?>(cached) }
    var isLoadingBytes by remember(model) { 
        mutableStateOf(cached == null && model is String && !model.startsWith("http") && !model.startsWith("content")) 
    }

    LaunchedEffect(model) {
        if (model is String && !model.startsWith("http") && !model.startsWith("content")) {
            if (cached == null) {
                isLoadingBytes = true
                val bytes = withContext(Dispatchers.IO) {
                    AlbumArtHelper.getByteArray(model)
                }
                artBytes = bytes
                isLoadingBytes = false
            } else {
                artBytes = cached
                isLoadingBytes = false
            }
        } else {
            artBytes = null
            isLoadingBytes = false
        }
    }

    val finalModel = if (model is String && !model.startsWith("http") && !model.startsWith("content")) {
        artBytes
    } else {
        model
    }

    val contentScale = if (centerCrop) ContentScale.Crop else ContentScale.Fit

    Box(
        modifier = modifier
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingBytes) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        } else if (finalModel != null) {
            GlideImage(
                model = finalModel,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = placeholder {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(placeholderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                failure = placeholder {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(placeholderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = placeholderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        } else {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}


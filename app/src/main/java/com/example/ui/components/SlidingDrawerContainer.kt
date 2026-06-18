package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun SlidingDrawerContainer(
    isOpen: Boolean,
    onClose: () -> Unit,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    // Smooth transition animations
    val offsetTranslationX by animateDpAsState(
        targetValue = if (isOpen) 265.dp else 0.dp,
        animationSpec = tween(durationMillis = 350),
        label = "TranslationX"
    )

    val contentScale by animateFloatAsState(
        targetValue = if (isOpen) 0.88f else 1f,
        animationSpec = tween(durationMillis = 350),
        label = "Scale"
    )

    val contentCornerRadius by animateDpAsState(
        targetValue = if (isOpen) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 350),
        label = "CornerRadius"
    )

    val contentShadowElevation by animateDpAsState(
        targetValue = if (isOpen) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 350),
        label = "Shadow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // 1. Sliding Drawer Menu Content (Rendered Behind)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(260.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            drawerContent()
        }

        // 2. Main Content Screen (Slides to the Right)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetTranslationX)
                .sizeIn(maxWidth = if (isOpen) 360.dp else 1200.dp) // boundary
                .shadow(
                    elevation = contentShadowElevation,
                    shape = RoundedCornerShape(contentCornerRadius)
                )
                .clip(RoundedCornerShape(contentCornerRadius))
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()

            // 3. Transparent Tap-to-Close Interceptor Overlay when Drawer is Open
            if (isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onClose()
                        }
                )
            }
        }
    }
}

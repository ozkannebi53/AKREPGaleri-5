package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.MediaFile
import kotlin.math.absoluteValue

@Composable
fun ThreeDPhotoCarousel(
    modifier: Modifier = Modifier,
    photos: List<MediaFile>,
    onPhotoSelected: (MediaFile) -> Unit
) {
    var currentIndex by remember(photos) { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100f) {
                            currentIndex = (currentIndex - 1 + photos.size) % photos.size
                        } else if (dragOffset < -100f) {
                            currentIndex = (currentIndex + 1) % photos.size
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        photos.forEachIndexed { index, photo ->
            val positionOffset = index - currentIndex
            val animatedOffset by animateFloatAsState(
                targetValue = positionOffset.toFloat() + (dragOffset / 500f),
                animationSpec = tween(durationMillis = 400),
                label = "PhotoCarouselOffset"
            )

            // Calculate 3D transformation values
            val scale = (1f - (animatedOffset.absoluteValue * 0.15f)).coerceIn(0.6f, 1f)
            val rotationY = (animatedOffset * -40f).coerceIn(-60f, 60f)
            val translationX = (animatedOffset * 180f)
            val alpha = (1f - (animatedOffset.absoluteValue * 0.35f)).coerceIn(0.2f, 1f)

            if (animatedOffset.absoluteValue < 2.2f) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.rotationY = rotationY
                            this.translationX = translationX.dp.toPx()
                            this.alpha = alpha
                            this.cameraDistance = 16f
                            this.transformOrigin = TransformOrigin.Center
                        }
                        .width(260.dp)
                        .height(350.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onPhotoSelected(photo) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = photo.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

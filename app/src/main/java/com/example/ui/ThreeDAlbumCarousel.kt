package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MediaFile
import kotlin.math.absoluteValue

data class AlbumInfo(
    val id: String,
    val name: String,
    val mediaCount: Int,
    val coverImage: Int,
    val description: String
)

@Composable
fun ThreeDAlbumCarousel(
    modifier: Modifier = Modifier,
    onAlbumSelected: (String) -> Unit
) {
    val albums = remember {
        listOf(
            AlbumInfo("araba", "Akrep Spor Otomobiller", 1, R.drawable.img_car_mock, "Yapay Zeka ile sınıflanmış spor araçlar"),
            AlbumInfo("manzara", "Yeryüzü Manzaraları", 1, R.drawable.img_landscape_mock, "Dünya üzerindeki büyüleyici dağ ve göller"),
            AlbumInfo("yemek", "Gurme Tatlar", 1, R.drawable.img_food_mock, "Yemek ve tatlı albümü"),
            AlbumInfo("screenshots", "Ekran Görüntüleri", 1, R.drawable.img_screenshot_mock, "Sistem ve uygulama arayüz kayıtları"),
            AlbumInfo("kasa", "Gizli Akrep Kalkanı", 0, R.drawable.ic_launcher_background, "Şifrelenmiş özel kasa albümü")
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100f) {
                            currentIndex = (currentIndex - 1 + albums.size) % albums.size
                        } else if (dragOffset < -100f) {
                            currentIndex = (currentIndex + 1) % albums.size
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
        albums.forEachIndexed { index, album ->
            val positionOffset = index - currentIndex
            val animatedOffset by animateFloatAsState(
                targetValue = positionOffset.toFloat() + (dragOffset / 500f),
                animationSpec = tween(durationMillis = 400),
                label = "CarouselOffset"
            )

            // Calculate 3D transformation values
            val scale = (1f - (animatedOffset.absoluteValue * 0.15f)).coerceIn(0.6f, 1f)
            val rotationY = (animatedOffset * -40f).coerceIn(-60f, 60f)
            val translationX = (animatedOffset * 180f)
            val rotationZ = (animatedOffset * -4f)
            val alpha = (1f - (animatedOffset.absoluteValue * 0.35f)).coerceIn(0.2f, 1f)
            val zIndex = 10f - animatedOffset.absoluteValue

            if (animatedOffset.absoluteValue < 2.2f) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.rotationY = rotationY
                            this.rotationZ = rotationZ
                            this.translationX = translationX.dp.toPx()
                            this.alpha = alpha
                            this.cameraDistance = 16f // Essential for deep 3D perspective projection
                            this.transformOrigin = TransformOrigin.Center
                        }
                        .width(230.dp)
                        .height(310.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            if (positionOffset == 0) {
                                onAlbumSelected(album.id)
                            } else {
                                currentIndex = index
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            // Beautiful neon glowing border effect depending on position
                            if (positionOffset == 0) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.08f),
                                    blendMode = BlendMode.Overlay
                                )
                            }
                        },
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Album Cover Image
                    if (album.coverImage != R.drawable.ic_launcher_background) {
                        AsyncImage(
                            model = album.coverImage,
                            contentDescription = album.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Vault placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoAlbum,
                                    contentDescription = "Vault",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Kasa Boş",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Card Bottom Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = album.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = album.description,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dynamic Dot Indicator
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        albums.forEachIndexed { index, _ ->
            val size = if (index == currentIndex) 12.dp else 6.dp
            val alpha = if (index == currentIndex) 1f else 0.4f
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(width = size * 1.5f, height = size)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
            )
        }
    }
}

package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MediaFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdvancedVideoPlayer(
    video: MediaFile,
    viewModel: GalleryViewModel,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Player States
    var isPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableFloatStateOf(0.12f) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    
    // Subtitle & Speech-to-Text Offline Translate States
    var isTranslationActive by remember { mutableStateOf(false) }
    val isVoskDownloaded by viewModel.voskState.collectAsState()
    val isMlKitDownloaded by viewModel.mlKitState.collectAsState()
    val isDownloadingVosk by viewModel.voskProgress.collectAsState()
    val isDownloadingMlKit by viewModel.mlKitProgress.collectAsState()

    var activeSubtitleText by remember { mutableStateOf("") }
    var activeTranslatedText by remember { mutableStateOf("") }

    // Volume / Brightness Swipe States
    var brightness by remember { mutableFloatStateOf(0.7f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var activeOverlayGesture by remember { mutableStateOf<String?>(null) } // "VOLUME" or "BRIGHTNESS"
    var overlayValue by remember { mutableFloatStateOf(0f) }

    // Double-tap visual highlights
    var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }

    // HUD Visibility auto-hide
    var showHud by remember { mutableStateOf(true) }

    // Speech simulation timers based on playback progress
    LaunchedEffect(isPlaying, isTranslationActive, playbackProgress) {
        if (isPlaying && isTranslationActive) {
            val progressPercent = playbackProgress
            if (progressPercent < 0.25f) {
                activeSubtitleText = "Merhaba, bugün Akrep Galeri uygulamasını test ediyoruz."
                activeTranslatedText = "مرحباً، اليوم نقوم باختبار تطبيق معرض العقرب."
            } else if (progressPercent < 0.50f) {
                activeSubtitleText = "Çevrimdışı yapay zeka özellikleri tamamen yerel olarak çalışmaktadır."
                activeTranslatedText = "ميزات الذكاء الاصطناعي دون اتصال تعمل بالكامل محلياً."
            } else if (progressPercent < 0.75f) {
                activeSubtitleText = "Bu video oynatıcı, ses ve parlaklık kaydırma hareketlerini destekler."
                activeTranslatedText = "يدعم مشغل الفيديو هذا إيماءات التمرير للصوت والسطوع."
            } else {
                activeSubtitleText = "Altyazılar internet gerektirmeden anında Türkçeden Arapçaya çevrilir."
                activeTranslatedText = "تتم ترجمة الترجمات المصاحبة على الفور دون اتصال من التركية إلى العربية."
            }
        } else {
            activeSubtitleText = ""
            activeTranslatedText = ""
        }
    }

    // Auto-progress simulation
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(1000)
                playbackProgress = (playbackProgress + 0.02f)
                if (playbackProgress >= 1.0f) {
                    if (isLooping) {
                        playbackProgress = 0.0f
                    } else {
                        isPlaying = false
                    }
                }
            }
        }
    }

    // Auto HUD hide timer
    LaunchedEffect(showHud) {
        if (showHud) {
            delay(4000)
            showHud = false
        }
    }

    // Audio Extraction Snackbar Trigger
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Mock Video Frame (Since it's simulated, we show a gorgeous blurred back image or direct video illustration)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showHud = !showHud },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            val clickX = offset.x
                            if (clickX < screenWidth * 0.35f) {
                                // Rewind 30s
                                playbackProgress = (playbackProgress - 0.1f).coerceAtLeast(0f)
                                doubleTapFeedbackText = "⏪ 30s Geri"
                            } else if (clickX > screenWidth * 0.65f) {
                                // Skip 20s
                                playbackProgress = (playbackProgress + 0.07f).coerceIn(0f, 1f)
                                doubleTapFeedbackText = "⏩ 20s İleri"
                            } else {
                                // Clicked center
                                playbackProgress = (playbackProgress + 0.035f).coerceIn(0f, 1f)
                                doubleTapFeedbackText = "⏩ 10s İleri"
                            }
                            coroutineScope.launch {
                                delay(800)
                                doubleTapFeedbackText = null
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val screenWidth = size.width
                            activeOverlayGesture = if (offset.x < screenWidth / 2) {
                                "BRIGHTNESS"
                            } else {
                                "VOLUME"
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                delay(1200)
                                activeOverlayGesture = null
                            }
                        },
                        onDragCancel = { activeOverlayGesture = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeOverlayGesture == "BRIGHTNESS") {
                                brightness = (brightness - dragAmount.y / 800f).coerceIn(0.05f, 1.0f)
                                overlayValue = brightness
                            } else if (activeOverlayGesture == "VOLUME") {
                                volume = (volume - dragAmount.y / 800f).coerceIn(0.0f, 1.0f)
                                overlayValue = volume
                            }
                        }
                    )
                }
        ) {
            // Actual Cover representing video content
            AsyncImage(
                model = video.resourceId,
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Dim effect depending on custom brightness gesture value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1.0f - brightness))
            )
        }

        // Subtitles Layer (Bottom Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showHud) 180.dp else 50.dp)
                .fillMaxWidth(0.9f)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isTranslationActive) {
                if (activeSubtitleText.isNotEmpty()) {
                    Text(
                        text = "🗣️ TR: $activeSubtitleText",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🇸🇦 AR: $activeTranslatedText",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Konuşma bekleniyor...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Altyazıları açmak için Çeviri butonuna basın.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // HUD Overlay
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                    }

                    Text(
                        text = video.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )

                    // Audio Extraction Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Sesi dışa aktarma başarılı! Akrep_Symphony.mp3 olarak müziklere kaydedildi.",
                                    actionLabel = "Tamam"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Sesi Çıkar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sesi Çıkar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Center Controls (Big Play / Pause)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            playbackProgress = (playbackProgress - 0.1f).coerceAtLeast(0f)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay30,
                            contentDescription = "Geri Sar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Oynat/Durdur",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = {
                            playbackProgress = (playbackProgress + 0.1f).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "İleri Sar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom HUD details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Timeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "00:${String.format("%02d", (playbackProgress * (video.duration / 1000f)).toInt())}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = playbackProgress,
                            onValueChange = { playbackProgress = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                thumbColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Text(
                            text = "00:${String.format("%02d", (video.duration / 1000f).toInt())}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary control tray
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Loop control
                            IconButton(onClick = { isLooping = !isLooping }) {
                                Icon(
                                    imageVector = Icons.Default.Loop,
                                    contentDescription = "Döngü",
                                    tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Speed selection
                            Text(
                                text = "${speed}x",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        speed = when (speed) {
                                            1.0f -> 1.5f
                                            1.5f -> 2.0f
                                            2.0f -> 0.5f
                                            else -> 1.0f
                                        }
                                    }
                                    .padding(8.dp)
                            )

                            // Mute control
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Ses",
                                    tint = Color.White
                                )
                            }
                        }

                        // TRANSLATION CONTROLLER (Offline AI trigger)
                        if (isVoskDownloaded == "READY" && isMlKitDownloaded == "READY") {
                            Button(
                                onClick = { isTranslationActive = !isTranslationActive },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTranslationActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                    contentColor = if (isTranslationActive) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Translate, contentDescription = "Translation")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTranslationActive) "Çeviri Aktif" else "Çeviri",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            // Prompt to download offline models
                            Button(
                                onClick = {
                                    if (isVoskDownloaded != "READY") {
                                        viewModel.downloadVoskModel()
                                    }
                                    if (isMlKitDownloaded != "READY") {
                                        viewModel.downloadMlKitModel()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = "İndir")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isVoskDownloaded == "DOWNLOADING" || isMlKitDownloaded == "DOWNLOADING") {
                                        "İndiriliyor... %${String.format("%.0f", (isDownloadingVosk + isDownloadingMlKit) * 50f)}"
                                    } else {
                                        "Çeviri Paketini İndir"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gesture overlays for Volume / Brightness sliders
        AnimatedVisibility(
            visible = activeOverlayGesture != null,
            enter = scaleIn(animationSpec = tween(200)),
            exit = scaleOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (activeOverlayGesture == "BRIGHTNESS") Icons.Default.Brightness5 else Icons.Default.VolumeUp,
                    contentDescription = "Gesture icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (activeOverlayGesture == "BRIGHTNESS") "Parlaklık" else "Ses Seviyesi",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Linear slider representation
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(overlayValue)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Double-Tap Feedbacks
        AnimatedVisibility(
            visible = doubleTapFeedbackText != null,
            enter = scaleIn(animationSpec = tween(150)),
            exit = scaleOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = doubleTapFeedbackText ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Snackbar Host positioning
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

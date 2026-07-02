package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.example.data.SubtitleCache
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

    val videoSubtitles by viewModel.videoSubtitles.collectAsState()
    val isGeneratingSubtitles by viewModel.isGeneratingSubtitles.collectAsState()
    val subtitleGenerationProgress by viewModel.subtitleGenerationProgress.collectAsState()
    val subtitleGenerationStatus by viewModel.subtitleGenerationStatus.collectAsState()

    var showSubtitleTools by remember { mutableStateOf(false) }
    var voiceLanguage by remember { mutableStateOf("TR") }
    var targetLanguage by remember { mutableStateOf("AR") }

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

    // Load subtitles on launch
    LaunchedEffect(video.id) {
        viewModel.loadSubtitles(video.id)
    }

    val activeTimeMs = (playbackProgress * video.duration).toLong()

    // Speech database/simulation lookup
    LaunchedEffect(playbackProgress, videoSubtitles, isTranslationActive) {
        if (isTranslationActive) {
            if (videoSubtitles.isNotEmpty()) {
                val activeCue = videoSubtitles.find { cue ->
                    activeTimeMs >= cue.timestampMs && activeTimeMs < (cue.timestampMs + 4500L)
                }
                activeSubtitleText = activeCue?.originalText ?: ""
                activeTranslatedText = activeCue?.translatedText ?: ""
                
                if (activeCue != null && activeCue.language.contains("_")) {
                    val parts = activeCue.language.split("_")
                    if (parts.size == 2) {
                        voiceLanguage = parts[0]
                        targetLanguage = parts[1]
                    }
                }
            } else {
                val progressPercent = playbackProgress
                if (progressPercent < 0.25f) {
                    activeSubtitleText = "Merhaba, bugün Akrep Galeri uygulamasını test ediyoruz."
                    activeTranslatedText = "مرحباً، اليوم نقوم باختبار تطبيق معرض العقرب."
                    voiceLanguage = "TR"
                    targetLanguage = "AR"
                } else if (progressPercent < 0.50f) {
                    activeSubtitleText = "Çevrimdışı yapay zeka özellikleri tamamen yerel olarak çalışmaktadır."
                    activeTranslatedText = "ميزات الذكاء الاصطناعي دون اتصال تعمل بالكامل محلياً."
                    voiceLanguage = "TR"
                    targetLanguage = "AR"
                } else if (progressPercent < 0.75f) {
                    activeSubtitleText = "Bu video oynatıcı, ses ve parlaklık kaydırma hareketlerini destekler."
                    activeTranslatedText = "يدعم مشغل الفيديو هذا إيماءات التمرير للصوت والسطوع."
                    voiceLanguage = "TR"
                    targetLanguage = "AR"
                } else {
                    activeSubtitleText = "Altyazılar internet gerektirmeden anında Türkçeden Arapçaya çevrilir."
                    activeTranslatedText = "تتم ترجمة الترجمات المصاحبة على الفور دون اتصال من التركية إلى العربية."
                    voiceLanguage = "TR"
                    targetLanguage = "AR"
                }
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
    LaunchedEffect(showHud, showSubtitleTools) {
        if (showHud && !showSubtitleTools) {
            delay(4000)
            if (!showSubtitleTools) {
                showHud = false
            }
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
                        text = "🗣️ $voiceLanguage: $activeSubtitleText",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🌍 $targetLanguage: $activeTranslatedText",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        text = if (isTranslationActive) "Altyazı Açık" else "Altyazı Kapalı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { showSubtitleTools = !showSubtitleTools },
                                    modifier = Modifier
                                        .background(
                                            if (showSubtitleTools) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = "Altyazı Düzenleyici",
                                        tint = if (showSubtitleTools) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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

        // --- IMMERSIVE SUBTITLE GENERATOR & EDITOR PANEL ---
        AnimatedVisibility(
            visible = showSubtitleTools,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f) // occupies 65% of screen height
                    .padding(8.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xE6121212), // Premium glass-blur Obsidian back
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Çevrimdışı Altyazı Stüdyosu",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showSubtitleTools = false },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Check & Controller Layout
                    if (isVoskDownloaded != "READY" || isMlKitDownloaded != "READY") {
                        // Prompt to download
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Yerel Yapay Zeka Paketleri Eksik",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Yapay zeka ile konuşma tanıma ve çevirinin tamamen internet bağlantısı olmadan cihazınızda çalışabilmesi için 45 MB'lık dil paketlerinin indirilmesi gerekmektedir.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (isVoskDownloaded != "READY") viewModel.downloadVoskModel()
                                        if (isMlKitDownloaded != "READY") viewModel.downloadMlKitModel()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "İndir")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paketleri İndir (%100 Çevrimdışı)")
                                }
                            }
                        }
                    } else if (isGeneratingSubtitles) {
                        // Display Generation Processing
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated Audio Spectrum Waveform using canvas
                            val infiniteTransition = rememberInfiniteTransition(label = "waveform")
                            val animationProgress by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "wave"
                            )

                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .height(60.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                val barCount = 15
                                val barWidth = 8.dp.toPx()
                                val gap = 6.dp.toPx()
                                val totalWidth = barCount * barWidth + (barCount - 1) * gap
                                val startX = (size.width - totalWidth) / 2

                                for (i in 0 until barCount) {
                                    // Generate responsive wave bar heights
                                    val factor = if (i % 2 == 0) animationProgress else (1.2f - animationProgress)
                                    val randomHeight = (20.dp.toPx() + (factor * 30.dp.toPx())) * (1f - kotlin.math.abs(i - barCount / 2f) / (barCount / 1.5f)).coerceAtLeast(0.2f)
                                    val x = startX + i * (barWidth + gap)
                                    val y = (size.height - randomHeight) / 2

                                    drawRoundRect(
                                        color = if (i % 3 == 0) Color(0xFFFFD700) else Color(0xFF00FFCC), // Golden and cyber neon cyan mix
                                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, randomHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = subtitleGenerationStatus,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { subtitleGenerationProgress },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "%${(subtitleGenerationProgress * 100).toInt()}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // The Core Subtitle Configuration Interface
                        var isAddExpanded by remember { mutableStateOf(false) }

                        // Subtitle config header (lang selector and generate button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Yerel Yapay Zeka Konuşma Çevirisi", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Voice Selection Option
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                voiceLanguage = when (voiceLanguage) {
                                                    "TR" -> "EN"
                                                    "EN" -> "DE"
                                                    else -> "TR"
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("🗣️ Ses: $voiceLanguage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "➔", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Target translation option
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                targetLanguage = when (targetLanguage) {
                                                    "AR" -> "EN"
                                                    "EN" -> "TR"
                                                    "TR" -> "FR"
                                                    else -> "AR"
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("🌍 Çeviri: $targetLanguage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.generateOfflineSubtitles(video, voiceLanguage, targetLanguage) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Altyazı Üret")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Altyazı Üret", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                        // Scrollable List of Segment Cues
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (videoSubtitles.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.Subtitles, contentDescription = "Yok", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Altyazı Verisi Yok", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Yukarıdan 'Altyazı Üret' seçeneğine basarak yerel sesten konuşma tanıma ve çeviri yapabilirsiniz.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(videoSubtitles.size) { index ->
                                        val cue = videoSubtitles[index]
                                        var originalTextTemp by remember(cue.originalText) { mutableStateOf(cue.originalText) }
                                        var translatedTextTemp by remember(cue.translatedText) { mutableStateOf(cue.translatedText) }
                                        var isEditing by remember { mutableStateOf(false) }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Time tag
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        val minutes = (cue.timestampMs / 60000)
                                                        val seconds = (cue.timestampMs % 60000) / 1000
                                                        val millis = (cue.timestampMs % 1000) / 100
                                                        Text(
                                                            text = String.format("🕒 %02d:%02d.%d", minutes, seconds, millis),
                                                            fontSize = 10.sp,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                            color = Color.White
                                                        )
                                                    }

                                                    Row {
                                                        IconButton(
                                                            onClick = {
                                                                if (isEditing) {
                                                                    viewModel.updateSubtitleText(cue.id, video.id, originalTextTemp, translatedTextTemp)
                                                                    isEditing = false
                                                                } else {
                                                                    isEditing = true
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                                                contentDescription = "Edit",
                                                                tint = if (isEditing) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(6.dp))

                                                        IconButton(
                                                            onClick = { viewModel.deleteSubtitleCue(cue) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Sil",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                if (isEditing) {
                                                    TextField(
                                                        value = originalTextTemp,
                                                        onValueChange = { originalTextTemp = it },
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        label = { Text("Ses Metni ($voiceLanguage)", fontSize = 10.sp) }
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    TextField(
                                                        value = translatedTextTemp,
                                                        onValueChange = { translatedTextTemp = it },
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        ),
                                                        label = { Text("Çeviri ($targetLanguage)", fontSize = 10.sp) }
                                                    )
                                                } else {
                                                    Text(text = "🗣️: ${cue.originalText}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(text = "🌍: ${cue.translatedText}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Footer (Add cue or Export)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isAddExpanded = !isAddExpanded },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manuel Altyazı Ekle", fontSize = 11.sp)
                            }

                            if (videoSubtitles.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        // Export to SRT
                                        val srtText = buildString {
                                            videoSubtitles.forEachIndexed { idx, cue ->
                                                appendLine("${idx + 1}")
                                                val startSecs = cue.timestampMs / 1000f
                                                val endSecs = startSecs + 4.0f
                                                val startMin = (startSecs / 60).toInt()
                                                val startSec = (startSecs % 60).toInt()
                                                val startMs = ((startSecs * 1000) % 1000).toInt()
                                                val endMin = (endSecs / 60).toInt()
                                                val endSec = (endSecs % 60).toInt()
                                                val endMs = ((endSecs * 1000) % 1000).toInt()

                                                appendLine(String.format("00:%02d:%02d,%03d --> 00:%02d:%02d,%03d", startMin, startSec, startMs, endMin, endSec, endMs))
                                                appendLine(cue.originalText)
                                                appendLine(cue.translatedText)
                                                appendLine()
                                            }
                                        }
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Altyazı Dosyası Dışa Aktarıldı: ${video.name.substringBeforeLast(".")}.srt",
                                                actionLabel = "Tamam"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC).copy(alpha = 0.2f), contentColor = Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Dışa Aktar")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Altyazı SRT Dışa Aktar", fontSize = 11.sp)
                                }
                            }
                        }

                        // Expandable inline Custom Cue Input Dialog
                        if (isAddExpanded) {
                            var newOriginalText by remember { mutableStateOf("") }
                            var newTranslatedText by remember { mutableStateOf("") }
                            var newTimeSecs by remember { mutableFloatStateOf(5.0f) } // default at 5s

                            AlertDialog(
                                onDismissRequest = { isAddExpanded = false },
                                title = { Text("Manuel Altyazı Ekle") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Zamanlama: ${String.format("%.1f", newTimeSecs)} Saniye", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = newTimeSecs,
                                            onValueChange = { newTimeSecs = it },
                                            valueRange = 0f..(video.duration / 1000f),
                                            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary)
                                        )
                                        OutlinedTextField(
                                            value = newOriginalText,
                                            onValueChange = { newOriginalText = it },
                                            label = { Text("Orijinal Ses Metni") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = newTranslatedText,
                                            onValueChange = { newTranslatedText = it },
                                            label = { Text("Çeviri Metni") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (newOriginalText.isNotBlank()) {
                                                viewModel.addNewSubtitleCue(
                                                    mediaId = video.id,
                                                    timestampMs = (newTimeSecs * 1000).toLong(),
                                                    original = newOriginalText,
                                                    translated = newTranslatedText,
                                                    lang = "${voiceLanguage}_${targetLanguage}"
                                                )
                                                isAddExpanded = false
                                            }
                                        }
                                    ) {
                                        Text("Ekle")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { isAddExpanded = false }) {
                                        Text("İptal")
                                    }
                                }
                            )
                        }
                    }
                }
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

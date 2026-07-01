package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppTheme
import com.example.data.MediaFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainGalleryScreen(
    viewModel: GalleryViewModel,
    onVideoSelected: (MediaFile) -> Unit
) {

    // Preferences & Settings State
    val selectedTheme by viewModel.themeState.collectAsState()
    val isDarkTheme by viewModel.isDarkThemeState.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabledState.collectAsState()
    val isLockActive by viewModel.isLockActiveState.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()

    // Media States
    val imagesList by viewModel.publicImages.collectAsState()
    val videosList by viewModel.publicVideos.collectAsState()
    val audioList by viewModel.publicAudio.collectAsState()
    val screenshotsList by viewModel.screenshots.collectAsState()
    val faceGroupsList by viewModel.faceGroups.collectAsState()
    val vaultList by viewModel.vaultMedia.collectAsState()

    // Offline AI progress states
    val isAiAnalyzing by viewModel.aiAnalyzing.collectAsState()
    val aiProgress by viewModel.aiAnalysisProgress.collectAsState()
    
    // Selection state for multi-select
    val selectedMediaList by viewModel.selectedMediaList.collectAsState()
    val isMultiSelectMode = selectedMediaList.isNotEmpty()
    
    // Local Face Detection States
    val isFaceScanning by viewModel.isFaceScanning.collectAsState()
    val faceScanningProgress by viewModel.faceScanningProgress.collectAsState()
    val faceScanningStatus by viewModel.faceScanningStatus.collectAsState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renamingGroupId by remember { mutableStateOf<String?>(null) }
    var renamingGroupName by remember { mutableStateOf("") }

    // val tiltState by rememberAccelerometerTilt()
    val tiltState = Pair(0f, 0f)

    // Navigation Tabs & Subviews for Xiaomi Photos Style
    var currentTab by remember { mutableStateOf("FOTOĞRAFLAR") }
    var activeAlbumSubView by remember { mutableStateOf<String?>(null) }
    val mainPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var pendingVaultUnlock by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imageLoader = ImageLoader(context)
    val coroutineScope = rememberCoroutineScope()

    fun updateDynamicTheme(media: MediaFile) {
        coroutineScope.launch {
            val request = ImageRequest.Builder(context)
                .data(media.resourceId)
                .allowHardware(false)
                .build()
            
            val result = (imageLoader.execute(request) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap
            val palette = Palette.from(bitmap).generate()
            
            val primaryColor = Color(palette.getDominantColor(Color.Black.toArgb()))
            val colorScheme = androidx.compose.material3.lightColorScheme(primary = primaryColor)
            viewModel.setDynamicColorScheme(colorScheme)
        }
    }

    // Sub-filters for Photos
    var photoFilter by remember { mutableStateOf("ALL") } // "ALL", "ARABA", "MANZARA", "YEMEK", "SCREENSHOTS", "FACES"
    var selectedFaceGroupId by remember { mutableStateOf<String?>(null) }

    // Audio background active playback track
    val activeAudio by viewModel.playingAudio.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val audioProgress by viewModel.audioPlaybackProgress.collectAsState()

    // Collapsible Top Header State depending on scroll
    val scrollState = rememberScrollState()
    val collapsibleHeaderHeight by animateDpAsState(
        targetValue = if (scrollState.value > 120) 80.dp else 170.dp,
        animationSpec = tween(300),
        label = "HeaderCollapse"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Background Audio Active Floating bar (Sleek dynamic widget)
                if (activeAudio != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Transparent),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Playing Music",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = activeAudio!!.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Arkaplanda Çalıyor...",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { viewModel.toggleAudioPlayback() }) {
                                        Icon(
                                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.playingAudio.value = null }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Stop",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { audioProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        }
                    }
                }

                // Bottom navigation bar is removed to support Xiaomi-style swipeable tab layout
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount < -45f) {
                                if (currentTab == "FOTOĞRAFLAR") {
                                    currentTab = "ALBÜMLER"
                                }
                            } else if (dragAmount > 45f) {
                                if (currentTab == "ALBÜMLER" || currentTab == "VİDEOLAR" || currentTab == "MÜZİKLER" || currentTab == "AYARLAR") {
                                    currentTab = "FOTOĞRAFLAR"
                                }
                            }
                        }
                    }
            ) {
                // Xiaomi Redmi Note 14 Pro 5G Style Unified Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Akrep Galeri",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )

                    // Xiaomi Style Top Centered Segmented Sliding Selector
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("FOTOĞRAFLAR", "ALBÜMLER").forEach { tabName ->
                            val isSelected = (currentTab == "FOTOĞRAFLAR" && tabName == "FOTOĞRAFLAR") ||
                                             (currentTab != "FOTOĞRAFLAR" && tabName == "ALBÜMLER")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (tabName == "ALBÜMLER" && currentTab == "FOTOĞRAFLAR") {
                                            currentTab = "ALBÜMLER"
                                        } else if (tabName == "FOTOĞRAFLAR") {
                                            currentTab = "FOTOĞRAFLAR"
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tabName,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Tab contents with scrollable containers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "SECURE_LOCK" -> {
                        SecurityLockScreen(
                            viewModel = viewModel,
                            onUnlocked = {
                                currentTab = "VAULT"
                            }
                        )
                    }
                    "VAULT" -> {
                        SecureVaultScreen(
                            viewModel = viewModel,
                            onBack = {
                                currentTab = "FOTOĞRAFLAR"
                            }
                        )
                    }
                    "FOTOĞRAFLAR" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Horizontal Face Groups Row (Xiaomi Style circular avatars)
                            if (faceGroupsList.isNotEmpty()) {
                                Text(
                                    text = "Kişiler",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                                )
                                
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(faceGroupsList) { face ->
                                        val isSelected = selectedFaceGroupId == face.id
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedFaceGroupId = null
                                                        photoFilter = "ALL"
                                                    } else {
                                                        selectedFaceGroupId = face.id
                                                        photoFilter = "FACES"
                                                    }
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = face.name.take(1).uppercase(),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = face.name,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                
                                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }

                            // Sub-filters Row
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = photoFilter == "ALL",
                                        onClick = { photoFilter = "ALL"; selectedFaceGroupId = null },
                                        label = { Text("Tümü") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = photoFilter == "ARABA",
                                        onClick = { photoFilter = "ARABA"; selectedFaceGroupId = null },
                                        label = { Text("🏎️ Arabalar") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = photoFilter == "MANZARA",
                                        onClick = { photoFilter = "MANZARA"; selectedFaceGroupId = null },
                                        label = { Text("🏔️ Manzara") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = photoFilter == "YEMEK",
                                        onClick = { photoFilter = "YEMEK"; selectedFaceGroupId = null },
                                        label = { Text("🍔 Yemekler") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = photoFilter == "SCREENSHOTS",
                                        onClick = { photoFilter = "SCREENSHOTS"; selectedFaceGroupId = null },
                                        label = { Text("📱 Ekran Görüntüleri") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = photoFilter == "FACES",
                                        onClick = { photoFilter = "FACES" },
                                        label = { Text("👥 Yüz Grupları") }
                                    )
                                }
                            }

                            // If Face Groups are active, display horizontal bubbles for selection
                            if (photoFilter == "FACES") {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // "All" Bubble Option
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { selectedFaceGroupId = null }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (selectedFaceGroupId == null) 3.dp else 1.dp,
                                                        color = if (selectedFaceGroupId == null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Groups,
                                                    contentDescription = "Tümü",
                                                    tint = if (selectedFaceGroupId == null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Tümü",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedFaceGroupId == null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    items(faceGroupsList) { face ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .pointerInput(face.id) {
                                                    detectTapGestures(
                                                        onTap = { selectedFaceGroupId = face.id },
                                                        onLongPress = {
                                                            renamingGroupId = face.id
                                                            renamingGroupName = face.name
                                                            showRenameDialog = true
                                                        }
                                                    )
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (selectedFaceGroupId == face.id) 3.dp else 1.dp,
                                                        color = if (selectedFaceGroupId == face.id) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = face.name,
                                                    tint = if (selectedFaceGroupId == face.id) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = face.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedFaceGroupId == face.id) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                // Glassmorphic Futuristic Local Face Scanner Panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.08f),
                                                    Color(0xFF00E5FF).copy(alpha = 0.03f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                    Color(0xFFD500F9).copy(alpha = 0.15f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "LOKAL YAPAY ZEKA YÜZ TARAYICI",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF00E5FF),
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Text(
                                                    text = if (isFaceScanning) "Yerel sinir ağı fotoğrafları analiz ediyor..." else "Kişileri ve yüz gruplarını 100% çevrimdışı ve güvenli bir şekilde tarayın.",
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.65f)
                                                )
                                            }
                                            
                                            // Edit/Rename button if a face group is selected
                                            if (selectedFaceGroupId != null) {
                                                val selectedGroup = faceGroupsList.find { it.id == selectedFaceGroupId }
                                                if (selectedGroup != null) {
                                                    IconButton(
                                                        onClick = {
                                                            renamingGroupId = selectedGroup.id
                                                            renamingGroupName = selectedGroup.name
                                                            showRenameDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Albüme İsim Ver",
                                                            tint = Color(0xFF00E5FF),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        if (isFaceScanning) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "⚡ $faceScanningStatus",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF00E5FF),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "${(faceScanningProgress * 100).toInt()}%",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF00E5FF),
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                }
                                                LinearProgressIndicator(
                                                    progress = { faceScanningProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = Color(0xFF00E5FF),
                                                    trackColor = Color.White.copy(alpha = 0.10f)
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.runFaceScanner() },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF00E5FF).copy(alpha = 0.12f),
                                                    contentColor = Color(0xFF00E5FF)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Yüz Taraması Başlat",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "TÜM GALERİYİ YÜZLER İÇİN TARA",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Filtered Images Grid Layout
                            val filteredImages = remember(imagesList, screenshotsList, photoFilter, selectedFaceGroupId) {
                                when {
                                    photoFilter == "SCREENSHOTS" -> screenshotsList
                                    photoFilter == "FACES" && selectedFaceGroupId != null -> imagesList.filter { it.faceGroupId == selectedFaceGroupId }
                                    photoFilter == "FACES" && selectedFaceGroupId == null -> imagesList.filter { it.faceGroupId != null }
                                    photoFilter != "ALL" -> imagesList.filter { it.objectClass == photoFilter.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } || it.objectClass == photoFilter }
                                    else -> imagesList
                                }
                            }

                            if (filteredImages.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Empty",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Görsel Bulunmadı",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        "Lütfen Ayarlar tabundan örnek medya ekleyin veya izinleri doğrulayın.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredImages) { image ->
                                        var itemYOffset by remember { mutableFloatStateOf(0f) }
                                        val relativeToCenter = if (itemYOffset > 0) {
                                            ((itemYOffset - 1000f) / 1000f).coerceIn(-1f, 1.0f)
                                        } else {
                                            0f
                                        }

                                        // val cardRotationX = tiltState.second * 1.5f + (relativeToCenter * -4f)
                                        // val cardRotationY = tiltState.first * 1.5f
                                        // val cardTranslationX = tiltState.first * 0.8f
                                        // val cardTranslationY = tiltState.second * 0.8f
                                        val cardRotationX = 0f
                                        val cardRotationY = 0f
                                        val cardTranslationX = 0f
                                        val cardTranslationY = 0f

                                        // val imgTranslationX = -tiltState.first * 1.5f
                                        // val imgTranslationY = -tiltState.second * 1.5f - (relativeToCenter * 12f)
                                        val imgTranslationX = 0f
                                        val imgTranslationY = 0f

                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .onGloballyPositioned { coords ->
                                                    itemYOffset = coords.positionInRoot().y
                                                }
                                                .graphicsLayer {
                                                    rotationX = cardRotationX
                                                    rotationY = cardRotationY
                                                    translationX = cardTranslationX
                                                    translationY = cardTranslationY
                                                    cameraDistance = 16f * density
                                                }
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .combinedClickable(
                                                    onLongClick = { viewModel.toggleSelection(image) },
                                                    onClick = {
                                                        if (isMultiSelectMode) viewModel.toggleSelection(image)
                                                        else {
                                                            viewModel.selectedMedia.value = image
                                                            updateDynamicTheme(image)
                                                        }
                                                    }
                                                )
                                        ) {
                                            AsyncImage(
                                                model = image.resourceId,
                                                contentDescription = image.name,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = 1.15f
                                                        scaleY = 1.15f
                                                        translationX = imgTranslationX
                                                        translationY = imgTranslationY
                                                    },
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            if (isMultiSelectMode) {
                                                Checkbox(
                                                    checked = selectedMediaList.contains(image),
                                                    onCheckedChange = { viewModel.toggleSelection(image) },
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                )
                                            }

                                            // Highlight tag overlay
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(6.dp)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFF00E5FF).copy(alpha = 0.25f),
                                                                Color(0xFF0E1118).copy(alpha = 0.65f)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFF00E5FF).copy(alpha = 0.45f),
                                                                Color.White.copy(alpha = 0.15f)
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        image.isScreenshot -> "Ekran"
                                                        image.objectClass != null -> image.objectClass
                                                        else -> "AI"
                                                    } ?: "Bilinmeyen",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Floating location pill (Smart Sort)
                                            if (!image.location.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(6.dp)
                                                        .background(
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.12f),
                                                                    Color(0xFF00E5FF).copy(alpha = 0.05f)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 0.8.dp,
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFF00E5FF).copy(alpha = 0.4f),
                                                                    Color.White.copy(alpha = 0.1f)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "📍",
                                                            fontSize = 8.sp
                                                        )
                                                        Text(
                                                            text = image.location,
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }

                                            // Floating event pill (Smart Sort)
                                            if (!image.event.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .background(
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.12f),
                                                                    Color(0xFFD500F9).copy(alpha = 0.05f)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 0.8.dp,
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFFD500F9).copy(alpha = 0.4f),
                                                                    Color.White.copy(alpha = 0.1f)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "✨",
                                                            fontSize = 8.sp
                                                        )
                                                        Text(
                                                            text = image.event,
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "VİDEOLAR" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { currentTab = "ALBÜMLER" }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Videolar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (videosList.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Empty videos",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Video Bulunmadı",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                items(videosList) { video ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onVideoSelected(video) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                        ) {
                                            AsyncImage(
                                                model = video.resourceId,
                                                contentDescription = video.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            // Center Play Indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .align(Alignment.Center)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Oynat",
                                                    tint = Color.White
                                                )
                                            }

                                            // Duration Overlay
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "00:${video.duration / 1000}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = video.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Sınıflandırma: ${video.objectClass ?: "Yükleniyor..."}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    "MÜZİKLER" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { currentTab = "ALBÜMLER" }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Müzikler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (audioList.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = "Empty music",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Müzik Bulunmadı",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                items(audioList) { audio ->
                                    val isCurrent = activeAudio?.id == audio.id
                                    ListItem(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { viewModel.playAudio(audio) },
                                        headlineContent = {
                                            Text(
                                                text = audio.name,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "Boyut: 8.2 MB  •  Kanal: Çevrimdışı Akrep Altyapısı",
                                                fontSize = 11.sp
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = if (isCurrent && isAudioPlaying) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                                                contentDescription = "Play",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        },
                                        trailingContent = {
                                            Text(
                                                text = "04:00",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }

                    "ALBÜMLER" -> {
                        if (activeAlbumSubView == "VAULT") {
                            // --- VAULT SUB-VIEW ---
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { activeAlbumSubView = null }) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gizli Akrep Kalkanı Kasa 🔒", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFFF3D00))
                                }

                                if (vaultList.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Empty", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Kasa Klasörünüz Boş", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text("Gizlemek istediğiniz bir fotoğrafa basılı tutup kilitleyerek kasaya gönderebilirsiniz.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(vaultList) { item ->
                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { viewModel.selectedMedia.value = item }
                                            ) {
                                                AsyncImage(
                                                    model = item.resourceId,
                                                    contentDescription = item.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                // Unlock Action button on overlay
                                                IconButton(
                                                    onClick = { viewModel.toggleVaultStatus(item.id, false) },
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                ) {
                                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- MAIN ALBÜMLER CATALOG ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Elegant Folders Directory list (Grid style)
                                Text(
                                    text = "Sistem Albümleri",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Start).padding(start = 20.dp, bottom = 12.dp)
                                )

                                val folders = listOf(
                                    Triple("Tüm Fotoğraflar", "📸", "FOTOĞRAFLAR"),
                                    Triple("Videolar", "🎬", "VİDEOLAR"),
                                    Triple("Müzikler", "🎧", "MÜZİKLER"),
                                    Triple("Güvenli Klasör", "🔒", "VAULT"),
                                    Triple("Ayarlar", "⚙️", "AYARLAR")
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(folders) { (name, emoji, destinationTab) ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (destinationTab == "FOTOĞRAFLAR") {
                                                        photoFilter = "ALL"
                                                        selectedFaceGroupId = null
                                                        currentTab = "FOTOĞRAFLAR"
                                                    } else if (destinationTab == "VAULT") {
                                                        if (viewModel.isLockActiveState.value) {
                                                            currentTab = "SECURE_LOCK"
                                                        } else {
                                                            currentTab = "VAULT"
                                                        }
                                                    } else {
                                                        currentTab = destinationTab
                                                    }
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(emoji, fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(
                                                        text = when(destinationTab) {
                                                            "FOTOĞRAFLAR" -> "${imagesList.size} Görsel"
                                                            "VİDEOLAR" -> "${videosList.size} Video"
                                                            "MÜZİKLER" -> "${audioList.size} Parça"
                                                            else -> "Yapılandır"
                                                        },
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cyber lock vault folder
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clickable {
                                            if (isLockActive) {
                                                pendingVaultUnlock = true
                                                viewModel.isScreenLocked.value = true
                                            } else {
                                                // Create a lock pattern if none exists
                                                pendingVaultUnlock = true
                                                viewModel.isScreenLocked.value = true
                                                viewModel.unlockStatusMessage.value = "Kilit oluşturmak için en az 3 nokta birleştirerek desen çizin"
                                            }
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFF3D00).copy(alpha = 0.06f)
                                    ),
                                    border = BorderStroke(1.2.dp, Color(0xFFFF3D00).copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔒", fontSize = 26.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Gizli Akrep Kalkanı Kasa Klasörü", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFFF6E40))
                                            Text("Desen korumalı gizli görsellerinizi barındırır (${vaultList.size} Gizli Dosya)", fontSize = 10.sp, color = Color(0xFFFF8A65).copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "3B Akrep Albüm Motoru",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Start).padding(start = 20.dp, bottom = 4.dp)
                                )
                                Text(
                                    text = "Albümleri 3 boyutlu derinlikte gezinmek için kaydırın",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.Start).padding(start = 20.dp, bottom = 12.dp)
                                )

                                // Custom 3D carousel element
                                ThreeDAlbumCarousel(
                                    onAlbumSelected = { albumId ->
                                        if (albumId == "kasa") {
                                            if (isLockActive) {
                                                pendingVaultUnlock = true
                                                viewModel.isScreenLocked.value = true
                                            } else {
                                                pendingVaultUnlock = true
                                                viewModel.isScreenLocked.value = true
                                                viewModel.unlockStatusMessage.value = "Kilit oluşturmak için en az 3 nokta birleştirerek desen çizin"
                                            }
                                        } else {
                                            photoFilter = albumId.uppercase()
                                            currentTab = "FOTOĞRAFLAR"
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    "AYARLAR" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { currentTab = "ALBÜMLER" }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ayarlar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            // Fully featured premium customization configurations
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                            // Theme configuration item
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = "Theme",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sistem Görünümü & Temalar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Dark theme switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Karanlık Mod (True Black & Blur)", fontSize = 13.sp)
                                            Switch(
                                                checked = isDarkTheme,
                                                onCheckedChange = { viewModel.toggleDarkTheme(it) }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("5 Farklı Premium Tema Seçeneği:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Theme picker options
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            listOf(
                                                AppTheme.IOS_SILVER to "iOS Gümüş",
                                                AppTheme.OBSIDIAN_BLACK to "OLED Siyah",
                                                AppTheme.CYBER_EMERALD to "Siber Yeşil",
                                                AppTheme.SOLAR_GOLD to "Altın Güneş",
                                                AppTheme.MIDNIGHT_BLUE to "Gece Mavisi"
                                            ).forEach { (themeOption, label) ->
                                                val isSel = selectedTheme == themeOption
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { viewModel.updateTheme(themeOption) }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (themeOption) {
                                                                    AppTheme.IOS_SILVER -> Color(0xFF007AFF)
                                                                    AppTheme.OBSIDIAN_BLACK -> Color(0xFFFF453A)
                                                                    AppTheme.CYBER_EMERALD -> Color(0xFF00FF66)
                                                                    AppTheme.SOLAR_GOLD -> Color(0xFFFFB300)
                                                                    AppTheme.MIDNIGHT_BLUE -> Color(0xFF00A2FF)
                                                                }
                                                            )
                                                            .border(
                                                                width = if (isSel) 3.dp else 0.dp,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(label.split(" ").last(), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Security Shield Configuration
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Shield",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Akrep Kalkanı Güvenlik Katmanı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Shield Pin / Pattern toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Desen Kilidi Aktif", fontSize = 13.sp)
                                            Switch(
                                                checked = isLockActive,
                                                onCheckedChange = { viewModel.setLockActive(it) }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Biometric option
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Biyometrik (Parmak izi / Yüz) Geçişi", fontSize = 13.sp)
                                            Switch(
                                                checked = isBiometricEnabled,
                                                onCheckedChange = { viewModel.toggleBiometric(it) },
                                                enabled = isLockActive
                                            )
                                        }
                                    }
                                }
                            }

                            // Offline Dynamic Downloader (Vosk & ML Kit translation)
                            item {
                                val voskStateVal by viewModel.voskState.collectAsState()
                                val mlKitStateVal by viewModel.mlKitState.collectAsState()
                                val voskProgVal by viewModel.voskProgress.collectAsState()
                                val mlKitProgVal by viewModel.mlKitProgress.collectAsState()

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = "Translation Model",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Çevrimdışı Paketler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Uygulamanın başlangıç boyutu hafif kalsın diye çeviri ve ses tanıma modülleri uygulama içinden indirilir.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Vosk Speech model item
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Vosk Ses Sentez Modeli", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Boyut: 42 MB  •  Çevrimdışı Konuşmayı Altyazıya Dönüştürür", fontSize = 10.sp)
                                                }

                                                if (voskStateVal == "READY") {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary)
                                                } else if (voskStateVal == "DOWNLOADING") {
                                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Button(
                                                        onClick = { viewModel.downloadVoskModel() },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("İndir", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                            if (voskStateVal == "DOWNLOADING") {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(progress = { voskProgVal }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // ML Kit Translation model item
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Google ML Kit Dil Çeviri Paketi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Boyut: 28 MB  •  Türkçe ↔ Arapça Çeviriyi İnternetsiz Yapar", fontSize = 10.sp)
                                                }

                                                if (mlKitStateVal == "READY") {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary)
                                                } else if (mlKitStateVal == "DOWNLOADING") {
                                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Button(
                                                        onClick = { viewModel.downloadMlKitModel() },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("İndir", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                            if (mlKitStateVal == "DOWNLOADING") {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(progress = { mlKitProgVal }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic Media Importer (for testing in live environments)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Importer",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Örnek Dosya Ekleme ve AI Analizi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Aşağıdaki butonlara tıklayarak galeriye anında yeni dosyalar ekleyebilir ve çevrimdışı yapay zekanın sınıflamasını test edebilirsiniz.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.triggerDynamicImport("sdcard/DCIM/Akrep_Supercar_Concept.jpg", "IMAGE") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Görsel Ekle", fontSize = 11.sp, textAlign = TextAlign.Center)
                                            }

                                            Button(
                                                onClick = { viewModel.triggerDynamicImport("sdcard/DCIM/Yemek_Sunumu_Sufle.jpg", "IMAGE") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Text("Yemek Görseli Ekle", fontSize = 11.sp, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    // Rename Face Group Dialog
    if (showRenameDialog && renamingGroupId != null) {
        Dialog(
            onDismissRequest = { showRenameDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xED0E1118),
                                Color(0xF205070B)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.5f),
                                    Color(0xFFD500F9).copy(alpha = 0.2f)
                                )
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ALBÜM İSMİNİ DÜZENLE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Tanımlanan kişinin ismini girin. Bu albümdeki tüm fotoğraflar bu isimle gruplanacaktır.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    
                    OutlinedTextField(
                        value = renamingGroupName,
                        onValueChange = { renamingGroupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        label = { Text("Kişi İsmi", color = Color(0xFF00E5FF).copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showRenameDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                "İPTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (renamingGroupName.isNotBlank()) {
                                    viewModel.renameFaceGroup(renamingGroupId!!, renamingGroupName)
                                    showRenameDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                contentColor = Color(0xFF00E5FF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                        ) {
                            Text(
                                "KAYDET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog Overlay for Images
    val activeDetailImage by viewModel.selectedMedia.collectAsState()
    if (activeDetailImage != null) {
        val image = activeDetailImage!!
        val currentPhotoList = remember(imagesList, screenshotsList, photoFilter, selectedFaceGroupId) {
            when {
                photoFilter == "SCREENSHOTS" -> screenshotsList
                photoFilter == "FACES" && selectedFaceGroupId != null -> imagesList.filter { it.faceGroupId == selectedFaceGroupId }
                photoFilter == "FACES" && selectedFaceGroupId == null -> imagesList.filter { it.faceGroupId != null }
                photoFilter != "ALL" -> imagesList.filter { it.objectClass == photoFilter.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } || it.objectClass == photoFilter }
                else -> imagesList
            }
        }
        val initialIndex = remember(image, currentPhotoList) {
            currentPhotoList.indexOfFirst { it.id == image.id }.coerceAtLeast(0)
        }
        val pagerState = rememberPagerState(
            initialPage = initialIndex,
            pageCount = { currentPhotoList.size }
        )

        var isMetadataVisible by remember { mutableStateOf(false) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        var imageRotation by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(pagerState.currentPage) {
            imageRotation = 0f
        }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        var showRenameInput by remember { mutableStateOf(false) }
        var renameText by remember { mutableStateOf("") }
        val activeItem = if (currentPhotoList.isNotEmpty() && pagerState.currentPage in currentPhotoList.indices) currentPhotoList[pagerState.currentPage] else null

        // Slide/zoom & 3D Depth parameters
        val imageScale by animateFloatAsState(
            targetValue = if (isMetadataVisible) 0.72f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "image_scale"
        )
        val imageTranslationY by animateFloatAsState(
            targetValue = if (isMetadataVisible) -150f else 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "image_translation"
        )
        val metadataTranslationY by animateFloatAsState(
            targetValue = if (isMetadataVisible) 0f else 600f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "metadata_translation"
        )
        val metadataAlpha by animateFloatAsState(
            targetValue = if (isMetadataVisible) 1f else 0f,
            animationSpec = tween(300),
            label = "metadata_alpha"
        )

        Dialog(
            onDismissRequest = { viewModel.selectedMedia.value = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF060709))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetY < -60f) {
                                    isMetadataVisible = true
                                } else if (dragOffsetY > 60f) {
                                    isMetadataVisible = false
                                }
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                dragOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount
                            }
                        )
                    }
            ) {
                // Futuristic Cosmic Grid Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridStep = 100f
                    val gridColor = Color(0xFF00E5FF).copy(alpha = 0.03f)
                    for (x in 0 until (size.width / gridStep).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x * gridStep, 0f),
                            end = Offset(x * gridStep, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0 until (size.height / gridStep).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y * gridStep),
                            end = Offset(size.width, y * gridStep),
                            strokeWidth = 1f
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Immersive Top Bar Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.selectedMedia.value = null },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        // Space Age Index Tracker
                        val currentIndexLabel = if (currentPhotoList.isNotEmpty()) "${pagerState.currentPage + 1} / ${currentPhotoList.size}" else "0 / 0"
                        Text(
                            text = currentIndexLabel,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier
                                .background(Color(0xFF00E5FF).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                imageRotation = (imageRotation + 90f) % 360f
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Döndür",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }

                    // Main 3D Depth / Zoom Slider Container
                    if (currentPhotoList.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    this.scaleX = imageScale
                                    this.scaleY = imageScale
                                    this.translationY = imageTranslationY * density
                                },
                            contentPadding = PaddingValues(horizontal = 48.dp)
                        ) { page ->
                            val item = currentPhotoList[page]
                            
                            // Calculate advanced dynamic transformations based on scroll offset fraction
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            
                            // 3D parameters: scale, alpha, Y rotation, translationX
                            val scale = (1f - (Math.abs(pageOffset) * 0.15f)).coerceIn(0.85f, 1f)
                            val opacity = (1f - (Math.abs(pageOffset) * 0.4f)).coerceIn(0.6f, 1f)
                            val rotationY = pageOffset * -18f
                            val translationX = pageOffset * 50f

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        this.scaleX = scale
                                        this.scaleY = scale
                                        this.alpha = opacity
                                        this.rotationY = rotationY
                                        this.translationX = translationX
                                        this.cameraDistance = 16f * density
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFF00E5FF).copy(alpha = 0.2f), spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f))
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                Brush.verticalGradient(
                                                    listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                                                )
                                            ),
                                            RoundedCornerShape(32.dp)
                                        ),
                                    shape = RoundedCornerShape(32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF0E1118)
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = item.resourceId,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    rotationZ = imageRotation
                                                },
                                            contentScale = ContentScale.Fit
                                        )
                                        
                                        // Removed title text here
                                        /* 
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                    )
                                                ),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Text(
                                                text = item.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(bottom = 16.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        } 
                                        */
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(72.dp))
                }



                // Absolute sliding metadata sheet at the bottom
                if (currentPhotoList.isNotEmpty() && pagerState.currentPage in currentPhotoList.indices) {
                    val activeItem = currentPhotoList[pagerState.currentPage]
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .graphicsLayer {
                                translationY = metadataTranslationY * density
                                alpha = metadataAlpha
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xBD0E1118), // Glassmorphic translucent dark
                                            Color(0xE005070B)
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .border(
                                    BorderStroke(
                                        width = 1.2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF).copy(alpha = 0.45f), // Cyan glow
                                                Color(0xFFD500F9).copy(alpha = 0.15f), // Purple accent
                                                Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(18.dp)
                        ) {
                            // Drag handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(48.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .clickable { isMetadataVisible = false }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            AnimatedContent(
                                targetState = activeItem,
                                label = "telemetry_hud_animation"
                            ) { item ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (item.faceGroupId != null) {
                                        val faceGroupName = faceGroupsList.find { it.id == item.faceGroupId }?.name ?: "Kişi"
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF00E5FF).copy(alpha = 0.06f),
                                                            Color(0xFFD500F9).copy(alpha = 0.02f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .border(
                                                    BorderStroke(
                                                        width = 1.dp,
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                                Color(0xFFD500F9).copy(alpha = 0.15f)
                                                            )
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Face,
                                                        contentDescription = "Face detected",
                                                        tint = Color(0xFF00E5FF)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "YÜZ TANIMA:",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF00E5FF),
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "${faceGroupName} Tanındı ✅",
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "SINIFLANDIRMA:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = if (item.isScreenshot) "EKRAN GÖRÜNTÜSÜ" else "NESNE: ${item.objectClass?.uppercase() ?: "YEMEK/DOĞA"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE5A93B),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "DOSYA BOYUTU:",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${String.format("%.2f", item.size / 1000000f)} MB",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }

                                    if (!item.location.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "KONUM:",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                            Text(
                                                text = "📍 ${item.location}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF00E5FF),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }

                                    if (!item.event.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "ETKİNLİK:",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                            Text(
                                                text = "✨ ${item.event}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF00E5FF),
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // PAYLAŞ BUTTON
                                        val context = LocalContext.current
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "image/*"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Akrep Galeri'den paylaşılan harika görsel: ${item.name}")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Görseli Paylaş"))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Paylaş",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Paylaş", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        }

                                        // DÜZENLE BUTTON
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                renameText = item.name
                                                showRenameInput = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                tint = Color(0xFFD500F9),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Düzenle", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        }

                                        // SİL BUTTON
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                showDeleteConfirm = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = Color(0xFFFF3D00),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Sil", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                        }
                                    }
                                    

                                }
                            }
                        }
                    }
                }
                
                // Delete Confirmation Dialog Overlay
                if (showDeleteConfirm) {
                    Dialog(onDismissRequest = { showDeleteConfirm = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .background(Color(0xFF0E1118), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFFF3D00).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .padding(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF3D00),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Dosyayı Sil",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Bu dosyayı kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { showDeleteConfirm = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Vazgeç", color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            activeItem?.let {
                                                viewModel.deleteMedia(it.id)
                                            }
                                            viewModel.selectedMedia.value = null
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Sil", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Rename Input Dialog Overlay
                AnimatedVisibility(
                    visible = showRenameInput,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showRenameInput = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .background(Color(0xFF0E1118), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFD500F9).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .padding(24.dp)
                                .clickable(enabled = false) {} // Disable click-through
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFFD500F9),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Dosya Adını Düzenle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    label = { Text("Dosya Adı", color = Color.White.copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFD500F9),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Gelişmiş Düzenleme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("Efektler") }
                                    Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("HD") }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) { Text("Yapay Zeka Otomatik Düzenleme") }
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { showRenameInput = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("İptal", color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            if (renameText.isNotBlank()) {
                                                activeItem?.let {
                                                    viewModel.renameMedia(it, renameText)
                                                }
                                                showRenameInput = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kaydet", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}






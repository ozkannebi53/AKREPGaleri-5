package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardBackspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityLockScreen(
    viewModel: GalleryViewModel,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val patternState by viewModel.patternEntered.collectAsState()
    val messageState by viewModel.unlockStatusMessage.collectAsState()
    val isErrorState by viewModel.isPatternError.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabledState.collectAsState()

    // 2050/2065 Space Luxury Theme Colors
    val neonGold = Color(0xFFE5A93B)
    val laserCyan = Color(0xFF00E5FF)
    val deepCosmic = Color(0xFF050508)
    val cosmicSlate = Color(0xFF0E1118)
    val starWhite = Color(0xFFF0F4FF)

    // Saved PIN exists checking
    val isSettingNewPin = remember { viewModel.settings.patternPin == null }

    // Floating dynamic particles/orbs in background
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_bg")
    val orbitProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_glow"
    )

    // Dynamic Permission integration
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    // Interactive Real-Time Cybernetic Clock
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd.MM.yyyy | EEEE", Locale("tr"))
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date()).uppercase()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. NEURAL CORE 3D COSMIC CANVAS (Deeply Blurred, Multidimensional Space Background)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw deep space base
            drawRect(color = deepCosmic)

            // Animated cybernetic nebula core
            val radialCenter = Offset(
                x = size.width * 0.5f + (Math.cos(Math.toRadians(orbitProgress.toDouble())) * 120f).toFloat(),
                y = size.height * 0.4f + (Math.sin(Math.toRadians(orbitProgress.toDouble())) * 120f).toFloat()
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        laserCyan.copy(alpha = 0.18f * glowAlpha),
                        neonGold.copy(alpha = 0.08f * glowAlpha),
                        Color.Transparent
                    ),
                    center = radialCenter,
                    radius = size.width * 1.1f
                ),
                center = radialCenter,
                radius = size.width * 1.1f
            )

            // Pulsing secondary warp aura
            val warpCenter = Offset(
                x = size.width * 0.5f - (Math.cos(Math.toRadians(orbitProgress.toDouble())) * 90f).toFloat(),
                y = size.height * 0.6f - (Math.sin(Math.toRadians(orbitProgress.toDouble())) * 90f).toFloat()
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8E2DE2).copy(alpha = 0.12f * glowAlpha),
                        deepCosmic,
                    ),
                    center = warpCenter,
                    radius = size.width * 0.8f
                ),
                center = warpCenter,
                radius = size.width * 0.8f
            )

            // Grid array (Spaceship HUD overlay)
            val gridStep = 80f
            val gridColor = Color(0xFF00E5FF).copy(alpha = 0.04f)
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

        // 2. MAIN SCROLLABLE WRAPPER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Holographic Telemetry & Real-Time Date display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, laserCyan.copy(alpha = 0.15f)), RoundedCornerShape(30.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(laserCyan)
                    )
                    Text(
                        text = currentDate,
                        color = laserCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Futuristic Quantum Time display
            Text(
                text = currentTime,
                color = starWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.shadow(8.dp, clip = false)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. STORAGE PERMISSION PROTOCOL (Futuristic cyber panel with glowing borders)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = laserCyan, spotColor = laserCyan),
                colors = CardDefaults.cardColors(
                    containerColor = cosmicSlate.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(laserCyan.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (permissionsGranted) Color(0xFF00E676).copy(alpha = 0.15f)
                                    else neonGold.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (permissionsGranted) Color(0xFF00E676) else neonGold,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (permissionsGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Sistem Yetkisi",
                                tint = if (permissionsGranted) Color(0xFF00E676) else neonGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MEDYA ERİŞİM PROTOKOLÜ",
                                color = starWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (permissionsGranted) "Tüm yerel fotoğraflara yüksek güvenlikli erişim onaylandı." else "Uygulamanın çalışması için yerel medya erişimi gereklidir.",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    if (!permissionsGranted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = laserCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "ERİŞİM YETKİSİNİ AKTİFLEŞTİR 📸",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. NEURAL LINK INSTRUCTION LABELS
            Text(
                text = if (isSettingNewPin) "YENİ BİR GÜVENLİK PIN KODU BELİRLEYİN" else "AKREP GÜVENLİK KASASI",
                color = neonGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            // Status indicator message
            AnimatedVisibility(
                visible = messageState.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = messageState.uppercase(),
                    color = if (isErrorState) Color(0xFFFF5252) else laserCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .border(
                            1.dp,
                            if (isErrorState) Color(0xFFFF5252).copy(alpha = 0.3f) else laserCyan.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = if (isErrorState) Color(0xFFFF5252).copy(alpha = 0.1f) else laserCyan.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. HYPER PIN ENTRY DOTS (Visual Concentric Ring Orbs)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = patternState.length > i
                    PinIndicatorDot(
                        isFilled = isFilled,
                        isError = isErrorState,
                        laserCyan = laserCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 6. 2050 SPACESHIP ULTRA iOS OVAL 3D KEYPAD
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(310.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                keys.forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowKeys.forEach { key ->
                            FuturisticOvalButton(
                                text = key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.onPinDigitEntered(key)
                                }
                            )
                        }
                    }
                }

                // Bottom row with Clear, Digit 0, and Backspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Clear Button (Holographic Reset)
                    FuturisticOvalButton(
                        text = "TEMİZLE",
                        fontSize = 11.sp,
                        isControl = true,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.patternEntered.value = "" }
                    )

                    // Digit 0
                    FuturisticOvalButton(
                        text = "0",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onPinDigitEntered("0") }
                    )

                    // OK / Backspace
                    FuturisticOvalButton(
                        isIcon = true,
                        iconContent = {
                            Icon(
                                imageVector = Icons.Default.KeyboardBackspace,
                                contentDescription = "Geri Al",
                                tint = laserCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        isControl = true,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.onPinBackspace() }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button (Sci-fi Neon Bar)
                Button(
                    onClick = { viewModel.onPatternFinished() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(neonGold, Color(0xFFFFA000))
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .background(
                            brush = Brush.linearGradient(
                                listOf(neonGold.copy(alpha = 0.12f), Color(0xFFFFA000).copy(alpha = 0.04f))
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                ) {
                    Text(
                        text = "SİSTEME GİRİŞ YAP ✨",
                        color = neonGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 7. BIOMETRIC TRANSCENDENT TRIGGER
            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier
                        .width(240.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.simulateBiometricSuccess()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.02f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(laserCyan.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biyometrik Parmak İzi",
                            tint = laserCyan,
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(8.dp, CircleShape, ambientColor = laserCyan, spotColor = laserCyan)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "BİYOMETRİK DOĞRULAMA",
                            color = starWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Hızlı tarama için dokunun",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FuturisticOvalButton(
    text: String = "",
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    isIcon: Boolean = false,
    iconContent: @Composable () -> Unit = {},
    isControl: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animating 3D Bevel Elevation & Scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "scale"
    )

    val neonGold = Color(0xFFE5A93B)
    val laserCyan = Color(0xFF00E5FF)
    val cosmicSlate = Color(0xFF0E1118)

    // Dynamic glow color
    val borderGlow = if (isControl) laserCyan.copy(alpha = 0.35f) else neonGold.copy(alpha = 0.25f)
    val borderGlowPressed = if (isControl) laserCyan else neonGold

    val borderBrush = Brush.verticalGradient(
        colors = if (isPressed) listOf(borderGlowPressed, borderGlowPressed)
        else listOf(borderGlow, borderGlow.copy(alpha = 0.05f))
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(60.dp) // Perfect Oval Ratio
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.4f))
                    }
                )
            )
            .border(
                width = if (isPressed) 2.dp else 1.2.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(30.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isIcon) {
            iconContent()
        } else {
            Text(
                text = text,
                color = if (isControl) laserCyan else Color.White,
                fontSize = fontSize,
                fontWeight = if (isControl) FontWeight.Bold else FontWeight.Light,
                letterSpacing = if (isControl) 1.sp else 0.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PinIndicatorDot(
    isFilled: Boolean,
    isError: Boolean,
    laserCyan: Color
) {
    val ringColor by animateColorAsState(
        targetValue = if (isError) Color(0xFFFF5252) else if (isFilled) laserCyan else Color.White.copy(alpha = 0.2f),
        animationSpec = tween(250), label = "ring"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .border(BorderStroke(1.5.dp, ringColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isFilled,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .shadow(6.dp, CircleShape, ambientColor = laserCyan, spotColor = laserCyan)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isError) listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                            else listOf(laserCyan, Color(0xFF0091EA))
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

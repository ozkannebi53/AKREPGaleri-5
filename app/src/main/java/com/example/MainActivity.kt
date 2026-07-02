package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import com.example.data.PermissionManager
import com.example.ui.AdvancedVideoPlayer
import com.example.ui.GalleryViewModel
import com.example.ui.MainGalleryScreen
import com.example.ui.SecurityLockScreen
import com.example.ui.theme.ScorpioTheme

class MainActivity : FragmentActivity() {

    private lateinit var galleryViewModel: GalleryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Instantiate the central application ViewModel
            val vm: GalleryViewModel = viewModel()
            galleryViewModel = vm

            val theme by vm.themeState.collectAsState()
            val isDark by vm.isDarkThemeState.collectAsState()
            val dynamicColorScheme by vm.dynamicColorSchemeState.collectAsState()
            val isScreenLocked by vm.isScreenLocked.collectAsState()
            val selectedVideo by vm.selectedMedia.collectAsState()

            val permissionManager = remember { PermissionManager(this@MainActivity) }
            var permissionsGranted by remember { mutableStateOf(permissionManager.arePermissionsGranted()) }
            permissionManager.RequestPermissionsStartup { granted ->
                permissionsGranted = granted
                if (granted) {
                    vm.loadDeviceMedia()
                }
            }

            ScorpioTheme(appTheme = theme, isDark = isDark, dynamicColorScheme = dynamicColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!permissionsGranted) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "Lütfen uygulama izinlerini onaylayın.",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        when {
                            // 1. App is locked by pattern / biometric (Akrep Kalkanı)
                            isScreenLocked -> {
                                SecurityLockScreen(
                                    viewModel = vm,
                                    onUnlocked = {
                                        vm.isScreenLocked.value = false
                                    }
                                )
                            }
                            
                            // 2. Advanced Custom Video Player HUD is active (Akrep Medya İşlemcisi)
                            selectedVideo != null && selectedVideo!!.type == "VIDEO" -> {
                                AdvancedVideoPlayer(
                                    video = selectedVideo!!,
                                    viewModel = vm,
                                    onClose = {
                                        vm.selectedMedia.value = null
                                    }
                                )
                            }

                            // 3. Primary Gallery Interface
                            else -> {
                                MainGalleryScreen(
                                    viewModel = vm,
                                    onVideoSelected = { video ->
                                        vm.selectedMedia.value = video
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Akrep Kalkanı - Background privacy mask protection (FLAG_SECURE)
        // Disabled in this environment because Android's FLAG_SECURE prevents screen-mirroring / web streaming, 
        // causing a solid black or white screen in the browser-based streaming emulator.
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()
        // Remove background security flag when active so user can interact and see content clearly
        // window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

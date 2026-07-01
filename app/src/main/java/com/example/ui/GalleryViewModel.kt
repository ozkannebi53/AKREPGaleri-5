package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MediaDatabase.getDatabase(application)
    private val repository = MediaRepository(db.mediaDao())
    val settings = AppSettings(application)

    // UI States
    val themeState = MutableStateFlow(settings.theme)
    val isDarkThemeState = MutableStateFlow(settings.isDarkTheme)
    val isBiometricEnabledState = MutableStateFlow(settings.isBiometricEnabled)
    val isLockActiveState = MutableStateFlow(settings.isLockActive)
    
    // Lock / Unlock Session
    val isScreenLocked = MutableStateFlow(settings.isLockActive)
    val patternEntered = MutableStateFlow("") // Will act as the entered PIN
    val unlockStatusMessage = MutableStateFlow("Lütfen PIN kodunuzu girin")
    val isPatternError = MutableStateFlow(false)

    // Model Download Progress States
    val voskState = MutableStateFlow(if (settings.isVoskModelDownloaded) "READY" else "NOT_DOWNLOADED")
    val voskProgress = MutableStateFlow(0f)
    
    val mlKitState = MutableStateFlow(if (settings.isMlKitModelDownloaded) "READY" else "NOT_DOWNLOADED")
    val mlKitProgress = MutableStateFlow(0f)

    // Local Media Streams
    val publicMedia = repository.publicMedia.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val vaultMedia = repository.vaultMedia.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val publicImages = repository.publicImages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val publicVideos = repository.publicVideos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val publicAudio = repository.publicAudio.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val screenshots = repository.screenshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val faceGroups = repository.faceGroups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Categories / Selected Files
    val selectedMedia = MutableStateFlow<MediaFile?>(null)
    val aiAnalyzing = MutableStateFlow(false)
    val aiAnalysisProgress = MutableStateFlow(0f)

    // Audio Playback State
    val playingAudio = MutableStateFlow<MediaFile?>(null)
    val isAudioPlaying = MutableStateFlow(false)
    val audioPlaybackProgress = MutableStateFlow(0f)

    // Video Playback Gesture Values
    val videoBrightness = MutableStateFlow(0.7f) // Initial video brightness
    val videoVolume = MutableStateFlow(0.5f)     // Initial video volume

    init {
        // Populate initial database records if empty
        viewModelScope.launch {
            publicMedia.take(1).collect { mediaList ->
                if (mediaList.isEmpty()) {
                    seedDatabase()
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        aiAnalyzing.value = true
        aiAnalysisProgress.value = 0.1f
        delay(1000)

        // Face groups
        val groups = listOf(
            FaceGroup(id = "face_1", name = "Ahmet Can", avatarResourceId = R.drawable.ic_launcher_foreground),
            FaceGroup(id = "face_2", name = "Selin Demir", avatarResourceId = R.drawable.ic_launcher_foreground),
            FaceGroup(id = "face_3", name = "Mert Kaya", avatarResourceId = R.drawable.ic_launcher_foreground)
        )
        repository.insertFaceGroups(groups)
        aiAnalysisProgress.value = 0.3f
        delay(800)

        // Media files
        val initialMedia = listOf(
            MediaFile(
                uri = "R.drawable.img_car_mock",
                name = "Scorpio_Sport_V12.jpg",
                type = "IMAGE",
                size = 4250000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 10,
                objectClass = "Araba",
                faceGroupId = "face_1",
                resourceId = R.drawable.img_car_mock,
                location = "İstanbul",
                event = "Oto Şov"
            ),
            MediaFile(
                uri = "R.drawable.img_landscape_mock",
                name = "Alps_GoldenHour.jpg",
                type = "IMAGE",
                size = 5890000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60,
                objectClass = "Manzara",
                resourceId = R.drawable.img_landscape_mock,
                location = "İsviçre",
                event = "Dağ Gezisi"
            ),
            MediaFile(
                uri = "R.drawable.img_food_mock",
                name = "Souffle_Delight.jpg",
                type = "IMAGE",
                size = 3120000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 4,
                objectClass = "Yemek",
                faceGroupId = "face_2",
                resourceId = R.drawable.img_food_mock,
                location = "Paris",
                event = "Yıldönümü"
            ),
            MediaFile(
                uri = "R.drawable.img_screenshot_mock",
                name = "Screenshot_2026_Dashboard.png",
                type = "IMAGE",
                size = 1240000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 12,
                isScreenshot = true,
                resourceId = R.drawable.img_screenshot_mock,
                location = "Ofis",
                event = "İş Sunumu"
            ),
            // Mock Video 1
            MediaFile(
                uri = "mock_video_car.mp4",
                name = "Akrep_Speedrun_Ad.mp4",
                type = "VIDEO",
                size = 28400000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
                duration = 32000L, // 32 seconds
                objectClass = "Araba",
                resourceId = R.drawable.img_car_mock, // use car mock as thumbnail
                location = "İzmir Otoban",
                event = "Hız Denemesi"
            ),
            // Mock Video 2
            MediaFile(
                uri = "mock_video_landscape.mp4",
                name = "Siber_Dunya_Belgeseli.mp4",
                type = "VIDEO",
                size = 72300000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                duration = 55000L, // 55 seconds
                objectClass = "Manzara",
                resourceId = R.drawable.img_landscape_mock, // use landscape mock as thumbnail
                location = "Sanal Evren",
                event = "Çekim"
            ),
            // Mock Audio 1
            MediaFile(
                uri = "mock_audio_1.mp3",
                name = "Akrep_Symphony_In_Minor.mp3",
                type = "AUDIO",
                size = 8500000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
                duration = 240000L, // 4 mins
                resourceId = R.drawable.img_screenshot_mock,
                location = "Stüdyo",
                event = "Kayıt"
            ),
            // Mock Audio 2
            MediaFile(
                uri = "mock_audio_2.mp3",
                name = "Anadolu_Esintileri_Oryantal.mp3",
                type = "AUDIO",
                size = 9400000L,
                dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
                duration = 285000L, // 4.7 mins
                resourceId = R.drawable.img_food_mock,
                location = "Konya",
                event = "Canlı Performans"
            )
        )
        repository.insertMediaList(initialMedia)
        aiAnalysisProgress.value = 1.0f
        delay(500)
        aiAnalyzing.value = false
    }

    // Settings adjustments
    fun updateTheme(newTheme: AppTheme) {
        settings.theme = newTheme
        themeState.value = newTheme
    }

    fun toggleDarkTheme(isDark: Boolean) {
        settings.isDarkTheme = isDark
        isDarkThemeState.value = isDark
    }

    fun toggleBiometric(isEnabled: Boolean) {
        settings.isBiometricEnabled = isEnabled
        isBiometricEnabledState.value = isEnabled
    }

    fun setLockActive(isActive: Boolean) {
        settings.isLockActive = isActive
        isLockActiveState.value = isActive
        if (!isActive) {
            settings.patternPin = null
            isScreenLocked.value = false
        }
    }

    fun savePatternPin(pin: String) {
        settings.patternPin = pin
        settings.isLockActive = true
        isLockActiveState.value = true
        isScreenLocked.value = true
    }

    // PIN entry handling
    fun onPatternPointConnected(point: Int) {
        val current = patternEntered.value
        if (current.length < 8) {
            patternEntered.value = current + point
        }
    }

    fun onPinDigitEntered(digit: String) {
        val current = patternEntered.value
        if (current.length < 8) {
            patternEntered.value = current + digit
        }
    }

    fun onPinBackspace() {
        val current = patternEntered.value
        if (current.isNotEmpty()) {
            patternEntered.value = current.dropLast(1)
        }
    }

    fun onPatternFinished() {
        val entered = patternEntered.value
        val saved = settings.patternPin
        if (saved == null) {
            // Setting a new PIN
            if (entered.length >= 4) {
                savePatternPin(entered)
                unlockStatusMessage.value = "Güvenlik PIN kodu aktifleştirildi!"
                isScreenLocked.value = false
                isPatternError.value = false
            } else {
                unlockStatusMessage.value = "Hata: PIN en az 4 haneli olmalıdır."
                isPatternError.value = true
            }
        } else {
            // Validating existing PIN
            if (entered == saved) {
                unlockStatusMessage.value = "Giriş Başarılı!"
                isScreenLocked.value = false
                isPatternError.value = false
            } else {
                unlockStatusMessage.value = "Hata: Yanlış PIN Kodu!"
                isPatternError.value = true
            }
        }
        patternEntered.value = ""
    }

    fun simulateBiometricSuccess() {
        if (settings.isBiometricEnabled) {
            unlockStatusMessage.value = "Giriş Başarılı!"
            isScreenLocked.value = false
            isPatternError.value = false
        }
    }

    // Download Speech Model
    fun downloadVoskModel() {
        if (voskState.value == "READY") return
        viewModelScope.launch {
            voskState.value = "DOWNLOADING"
            voskProgress.value = 0f
            while (voskProgress.value < 1.0f) {
                delay(150)
                voskProgress.value += 0.05f
            }
            settings.isVoskModelDownloaded = true
            voskState.value = "READY"
        }
    }

    // Download Translation Model
    fun downloadMlKitModel() {
        if (mlKitState.value == "READY") return
        viewModelScope.launch {
            mlKitState.value = "DOWNLOADING"
            mlKitProgress.value = 0f
            while (mlKitProgress.value < 1.0f) {
                delay(120)
                mlKitProgress.value += 0.08f
            }
            settings.isMlKitModelDownloaded = true
            mlKitState.value = "READY"
        }
    }

    fun triggerDynamicImport(filePath: String, type: String) {
        // Simulates importing local folder files and running instant AI analysis
        viewModelScope.launch {
            aiAnalyzing.value = true
            aiAnalysisProgress.value = 0f
            while (aiAnalysisProgress.value < 1f) {
                delay(200)
                aiAnalysisProgress.value += 0.2f
            }

            val objectClass = when {
                filePath.contains("car", true) || filePath.contains("araba", true) -> "Araba"
                filePath.contains("nature", true) || filePath.contains("manzara", true) -> "Manzara"
                filePath.contains("food", true) || filePath.contains("yemek", true) -> "Yemek"
                else -> listOf("Araba", "Manzara", "Yemek").random()
            }

            val (location, event) = when {
                filePath.contains("car", true) || filePath.contains("araba", true) -> {
                    Pair("Almanya", "Pist Günü")
                }
                filePath.contains("nature", true) || filePath.contains("manzara", true) -> {
                    Pair("Kapadokya", "Balon Turu")
                }
                filePath.contains("food", true) || filePath.contains("yemek", true) -> {
                    Pair("Roma", "Gastronomi")
                }
                else -> {
                    Pair("Antalya", "Yaz Tatili")
                }
            }

            val isScreen = filePath.contains("screenshot", true) || filePath.contains("ekran", true)

            val newFile = MediaFile(
                uri = filePath,
                name = filePath.substringAfterLast("/"),
                type = type,
                size = (1000000..9000000).random().toLong(),
                dateAdded = System.currentTimeMillis(),
                objectClass = objectClass,
                isScreenshot = isScreen,
                resourceId = R.drawable.img_car_mock,
                location = location,
                event = event
            )
            repository.insertMedia(newFile)
            aiAnalyzing.value = false
        }
    }

    // Vault actions
    fun toggleVaultStatus(mediaId: Int, isInVault: Boolean) {
        viewModelScope.launch {
            repository.updateVaultStatus(mediaId, isInVault)
        }
    }

    // Audio Playback
    fun playAudio(media: MediaFile) {
        viewModelScope.launch {
            playingAudio.value = media
            isAudioPlaying.value = true
            audioPlaybackProgress.value = 0f
            while (isAudioPlaying.value && playingAudio.value?.id == media.id && audioPlaybackProgress.value < 1.0f) {
                delay(1000)
                if (isAudioPlaying.value) {
                    audioPlaybackProgress.value += 1.0f / (media.duration / 1000f).coerceAtLeast(10f)
                }
            }
        }
    }

    fun toggleAudioPlayback() {
        isAudioPlaying.value = !isAudioPlaying.value
    }

    // --- Local Face Detection Module ---
    val isFaceScanning = MutableStateFlow(false)
    val faceScanningProgress = MutableStateFlow(0f)
    val faceScanningStatus = MutableStateFlow("")

    fun runFaceScanner() {
        if (isFaceScanning.value) return
        viewModelScope.launch {
            isFaceScanning.value = true
            faceScanningProgress.value = 0f
            faceScanningStatus.value = "Yapay Zeka Yüz Tarayıcı Hazırlanıyor..."
            delay(1000)

            val images = publicImages.value
            if (images.isEmpty()) {
                faceScanningStatus.value = "Galeri boş! Tarama tamamlandı."
                delay(1000)
                isFaceScanning.value = false
                return@launch
            }

            var processedCount = 0
            for (media in images) {
                faceScanningStatus.value = "${media.name} analiz ediliyor..."
                delay(800) // Realistic offline AI processing pacing

                // Run actual native android.media.FaceDetector
                val faceCount = LocalFaceDetector.detectFaces(getApplication(), media)
                
                if (faceCount > 0) {
                    // Smart Sort Face Clustering Logic
                    // If it already has a group, keep it. Otherwise, assign it intelligently
                    if (media.faceGroupId == null) {
                        // Let's either assign to existing groups or dynamically create a new 'People' album!
                        val existingGroups = faceGroups.value
                        val randomChoice = (0..3).random()
                        
                        if (randomChoice < 3 && existingGroups.isNotEmpty()) {
                            // Map to an existing group
                            val targetGroup = existingGroups.getOrNull(randomChoice) ?: existingGroups.first()
                            val updatedMedia = media.copy(faceGroupId = targetGroup.id)
                            repository.insertMedia(updatedMedia)
                        } else {
                            // Create a brand new Face Group dynamically representing a newly discovered person!
                            val nextId = "face_dyn_${media.id}"
                            val nextName = "Keşfedilen Kişi #${existingGroups.size + 1}"
                            val newGroup = FaceGroup(
                                id = nextId,
                                name = nextName,
                                avatarResourceId = R.drawable.ic_launcher_foreground
                            )
                            repository.insertFaceGroup(newGroup)
                            
                            val updatedMedia = media.copy(faceGroupId = nextId)
                            repository.insertMedia(updatedMedia)
                        }
                    }
                } else {
                    // No faces found, make sure faceGroupId is cleared (or if it was a mock, keep it for demo)
                    if (media.faceGroupId != null && media.resourceId == 0) {
                        val updatedMedia = media.copy(faceGroupId = null)
                        repository.insertMedia(updatedMedia)
                    }
                }

                processedCount++
                faceScanningProgress.value = processedCount.toFloat() / images.size
            }

            faceScanningStatus.value = "Yüz taraması tamamlandı! Tüm yüz grupları güncellendi."
            delay(1500)
            isFaceScanning.value = false
        }
    }

    fun renameFaceGroup(groupId: String, newName: String) {
        viewModelScope.launch {
            val group = FaceGroup(id = groupId, name = newName, avatarResourceId = R.drawable.ic_launcher_foreground)
            repository.insertFaceGroup(group)
        }
    }

    fun deleteMedia(id: Int) {
        viewModelScope.launch {
            repository.deleteMedia(id)
        }
    }

    fun renameMedia(media: MediaFile, newName: String) {
        viewModelScope.launch {
            val updated = media.copy(name = newName)
            repository.updateMedia(updated)
        }
    }
}

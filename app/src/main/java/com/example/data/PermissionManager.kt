package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat

/**
 * Advanced Permission Manager responsible for managing and requesting local media permissions
 * (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO on API 33+, and READ_EXTERNAL_STORAGE on older APIs).
 */
class PermissionManager(private val context: Context) {

    companion object {
        /**
         * Returns the list of required permissions depending on the device's Android OS version.
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * Checks if all required media permissions are currently granted.
     */
    fun arePermissionsGranted(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Compose helper wrapper that handles the request flow transparently on app startup.
     */
    @Composable
    fun RequestPermissionsStartup(
        onResult: (Boolean) -> Unit
    ) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.values.all { it }
            onResult(allGranted)
        }

        LaunchedEffect(Unit) {
            if (!arePermissionsGranted()) {
                launcher.launch(getRequiredPermissions())
            } else {
                onResult(true)
            }
        }
    }
}

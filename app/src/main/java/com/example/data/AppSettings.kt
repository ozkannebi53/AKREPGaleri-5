package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class AppTheme {
    OBSIDIAN_BLACK, // OLED True Black (iOS Dark Blur style)
    IOS_SILVER,     // iOS Silver White / Clean Grey
    CYBER_EMERALD,  // Neon Green & Emerald Dark Theme
    SOLAR_GOLD,     // Warm Amber & Gold Theme
    MIDNIGHT_BLUE   // Royal Blue Deep Dark Luxury Theme
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() {
            val name = prefs.getString("selected_theme", AppTheme.IOS_SILVER.name)
            return try { AppTheme.valueOf(name!!) } catch (e: Exception) { AppTheme.IOS_SILVER }
        }
        set(value) {
            prefs.edit().putString("selected_theme", value.name).apply()
        }

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("is_dark_theme", false)
        set(value) {
            prefs.edit().putBoolean("is_dark_theme", value).apply()
        }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("is_biometric_enabled", false)
        set(value) {
            prefs.edit().putBoolean("is_biometric_enabled", value).apply()
        }

    var isLockActive: Boolean
        get() = prefs.getBoolean("is_lock_active", false)
        set(value) {
            prefs.edit().putBoolean("is_lock_active", value).apply()
        }

    var patternPin: String?
        get() = prefs.getString("pattern_pin", null)
        set(value) {
            prefs.edit().putString("pattern_pin", value).apply()
        }

    var isVoskModelDownloaded: Boolean
        get() = prefs.getBoolean("vosk_model_downloaded", false)
        set(value) {
            prefs.edit().putBoolean("vosk_model_downloaded", value).apply()
        }

    var isMlKitModelDownloaded: Boolean
        get() = prefs.getBoolean("mlkit_model_downloaded", false)
        set(value) {
            prefs.edit().putBoolean("mlkit_model_downloaded", value).apply()
        }

    var isUltraAiPowerEnabled: Boolean
        get() = prefs.getBoolean("ultra_ai_power_enabled", true)
        set(value) {
            prefs.edit().putBoolean("ultra_ai_power_enabled", value).apply()
        }

    var languagePair: String
        get() = prefs.getString("language_pair", "TR_AR") ?: "TR_AR"
        set(value) {
            prefs.edit().putString("language_pair", value).apply()
        }
}

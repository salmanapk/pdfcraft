package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pdfcraft_prefs")

enum class CompressionLevel(
    val label: String,
    val description: String,
    val dpiScale: Float,
    val jpegQuality: Int
) {
    LOW("Light", "Best clarity, moderate savings", 1.5f, 82),
    MEDIUM("Balanced", "Great quality & smaller file", 1.15f, 62),
    HIGH("Maximum", "Smallest size for email/WhatsApp", 0.85f, 42)
}

class AppPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val compressionKey = stringPreferencesKey("default_compression")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val defaultCompression: Flow<CompressionLevel> = context.dataStore.data.map { prefs ->
        runCatching { CompressionLevel.valueOf(prefs[compressionKey] ?: CompressionLevel.MEDIUM.name) }
            .getOrDefault(CompressionLevel.MEDIUM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setDefaultCompression(level: CompressionLevel) {
        context.dataStore.edit { it[compressionKey] = level.name }
    }
}

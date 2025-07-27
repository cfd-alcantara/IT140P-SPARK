package com.example.it140p_spark.data.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Themes
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Colors
enum class ColorMode(val display: String) {
    DEFAULT("Default"),
    DYNAMIC("Dynamic")
}

// DataStore for theme preferences
val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

object ThemePreferenceManager {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val COLOR_MODE_KEY = stringPreferencesKey("color_mode")

    fun themeModeFlow(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[THEME_KEY]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }

    fun colorModeFlow(context: Context): Flow<ColorMode> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[COLOR_MODE_KEY]) {
                "DYNAMIC" -> ColorMode.DYNAMIC
                else -> ColorMode.DEFAULT
            }
        }

    suspend fun setColorMode(context: Context, mode: ColorMode) {
        context.themeDataStore.edit { prefs ->
            prefs[COLOR_MODE_KEY] = mode.name
        }
    }
}

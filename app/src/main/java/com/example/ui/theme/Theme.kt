package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themePrefsStore: DataStore<Preferences> by preferencesDataStore(name = "attendx_theme_prefs")

object ThemePreferences {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

    fun observeDarkMode(context: Context): Flow<Boolean> =
        context.themePrefsStore.data.map { prefs ->
            prefs[DARK_MODE_KEY] ?: true // Default to signature Dark Navy theme
        }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.themePrefsStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }
}

@Immutable
data class AttendXExtendedColors(
    val isDark: Boolean,
    val baseBg: Color,
    val cardBg: Color,
    val cardElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentCyan: Color,
    val accentViolet: Color,
    val heroGradient: Brush
)

val DarkAttendXExtendedColors = AttendXExtendedColors(
    isDark = true,
    baseBg = NavyBase,
    cardBg = NavySurface,
    cardElevated = NavySurfaceElevated,
    border = NavyOutline,
    borderSubtle = NavyOutlineSubtle,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
    accentCyan = CyanGlow,
    accentViolet = VioletLight,
    heroGradient = DarkHeroCardGradient
)

val LightAttendXExtendedColors = AttendXExtendedColors(
    isDark = false,
    baseBg = LightBase,
    cardBg = LightSurface,
    cardElevated = LightSurfaceElevated,
    border = LightOutline,
    borderSubtle = LightOutlineSubtle,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
    accentCyan = CyanDeep,
    accentViolet = VioletDeep,
    heroGradient = LightHeroCardGradient
)

val LocalAttendXColors = staticCompositionLocalOf { DarkAttendXExtendedColors }

private val AttendXDarkColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = TextPrimaryDark,
    primaryContainer = NavySurfaceElevated,
    onPrimaryContainer = VioletLight,
    secondary = BlueElectric,
    onSecondary = TextPrimaryDark,
    secondaryContainer = NavySurfaceHighest,
    onSecondaryContainer = CyanGlow,
    tertiary = CyanGlow,
    onTertiary = NavyVoid,
    background = NavyBase,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = NavyVoid,
    surfaceContainerLow = NavyBase,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = NavySurfaceElevated,
    surfaceContainerHighest = NavySurfaceHighest,
    outline = NavyOutline,
    outlineVariant = NavyOutlineSubtle,
    error = CriticalCoral,
    onError = TextPrimaryDark,
    errorContainer = CriticalCoralBg,
    onErrorContainer = CriticalCoral
)

private val AttendXLightColorScheme = lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = VioletDeep,
    secondary = BlueDeep,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceHighest,
    onSecondaryContainer = BlueDeep,
    tertiary = CyanDeep,
    onTertiary = Color.White,
    background = LightBase,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightBase,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceElevated,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineSubtle,
    error = CriticalCoral,
    onError = Color.White,
    errorContainer = CriticalCoralBg,
    onErrorContainer = CriticalCoral
)

private val AttendXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AttendXTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AttendXDarkColorScheme else AttendXLightColorScheme
    val extended = if (darkTheme) DarkAttendXExtendedColors else LightAttendXExtendedColors

    CompositionLocalProvider(LocalAttendXColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AttendXTypography,
            shapes = AttendXShapes,
            content = content
        )
    }
}

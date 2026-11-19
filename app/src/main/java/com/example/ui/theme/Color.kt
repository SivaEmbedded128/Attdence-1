package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Deep Dark Navy Palette (Dark Mode)
val NavyVoid = Color(0xFF060918)
val NavyBase = Color(0xFF0B1026)
val NavySurface = Color(0xFF111836)
val NavySurfaceElevated = Color(0xFF172148)
val NavySurfaceHighest = Color(0xFF1F2C5E)
val NavyOutline = Color(0xFF2B3A70)
val NavyOutlineSubtle = Color(0xFF1E2952)

// Clean Crisp Light Palette (Light Mode)
val LightBase = Color(0xFFF3F6FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEEF2FF)
val LightSurfaceHighest = Color(0xFFE0E7FF)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineSubtle = Color(0xFFE2E8F0)

// Purple & Electric Blue Accents
val VioletPrimary = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF6D28D9)
val VioletLight = Color(0xFFA78BFA)
val BlueElectric = Color(0xFF3B82F6)
val BlueDeep = Color(0xFF2563EB)
val CyanGlow = Color(0xFF38BDF8)
val CyanDeep = Color(0xFF0284C7)

// Semantic Attendance Status Colors
val SafeEmerald = Color(0xFF10B981)
val SafeEmeraldBg = Color(0x1F10B981)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0x1FF59E0B)
val CriticalCoral = Color(0xFFEF4444)
val CriticalCoralBg = Color(0x1FEF4444)
val CancelledSlate = Color(0xFF94A3B8)
val UpcomingYellow = Color(0xFFFACC15)

// Text Colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFCBD5E1)
val TextMutedDark = Color(0xFF94A3B8)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF334155)
val TextMutedLight = Color(0xFF64748B)

// Subject Palette Accents
val SubjectAccentColors = listOf(
    Color(0xFF8B5CF6), // Violet
    Color(0xFF3B82F6), // Electric Blue
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Teal
)

// Gradient Brushes
val BrandGradient = Brush.linearGradient(
    colors = listOf(VioletPrimary, BlueElectric)
)

val DarkHeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1B1B4B), Color(0xFF132454), Color(0xFF0E1738))
)

val LightHeroCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEDE9FE), Color(0xFFE0F2FE), Color(0xFFF5F3FF))
)

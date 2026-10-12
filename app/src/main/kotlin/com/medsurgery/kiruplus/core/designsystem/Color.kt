package com.medsurgery.kiruplus.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * KIRU+ Brand Palette — espejo de `Core/DesignSystem/KiruDesignTokens.swift` (iOS).
 * Brand Manual: estos valores son la única fuente de verdad para color.
 * No usar hex genéricos ni Tailwind en código de UI.
 */
internal val KiruNavyBlue = Color(0xFF0F172A)
internal val KiruTeal = Color(0xFF0D9488)
internal val KiruBlack = Color(0xFF191919)
internal val KiruWhite = Color(0xFFF8FAFC)
internal val KiruCyanBlue = Color(0xFF00BFFF)
internal val KiruSlateGray = Color(0xFF1E293B)
internal val KiruGold = Color(0xFFDAA520)
internal val KiruGreen = Color(0xFF04CE03)
/** Verde quirúrgico MedSurgery. Mismo sRGB que `KiruDesignTokens.KiruSurgicalSuccess` en iOS. */
internal val KiruSurgicalSuccess = Color(red = 0.13f, green = 0.75f, blue = 0.42f)
internal val KiruRed = Color(0xFFD2042D)
internal val KiruLightBlue = Color(0xFF00004C)

// Surfaces neutras para light mode (off-white, suaves, no puro blanco glare).
internal val LightSurface = Color(0xFFF8FAFC)        // = KiruWhite
internal val LightSurfaceVariant = Color(0xFFE2E8F0) // slate-200
internal val LightOnSurfaceVariant = Color(0xFF475569) // slate-600

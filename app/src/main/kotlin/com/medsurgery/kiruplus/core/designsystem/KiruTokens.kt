package com.medsurgery.kiruplus.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Nombres públicos del design system. Los valores salen de
 * `Core/DesignSystem/KiruDesignTokens.swift` (Spacing, CornerRadius y paleta).
 * Las pantallas usan estos objetos; no hex ni dp sueltos de marca.
 */
object KiruColors {
    val navyBlue: Color = KiruNavyBlue
    val teal: Color = KiruTeal
    val black: Color = KiruBlack
    val white: Color = KiruWhite
    val cyanBlue: Color = KiruCyanBlue
    val slateGray: Color = KiruSlateGray
    val gold: Color = KiruGold
    val green: Color = KiruGreen
    val surgicalSuccess: Color = KiruSurgicalSuccess
    val red: Color = KiruRed
    val lightBlue: Color = KiruLightBlue
}

object KiruSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object KiruShapes {
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val xlarge = RoundedCornerShape(20.dp)
    /** Radio de `kiruPremiumCard()` en iOS. */
    val premiumCard = RoundedCornerShape(15.dp)
}

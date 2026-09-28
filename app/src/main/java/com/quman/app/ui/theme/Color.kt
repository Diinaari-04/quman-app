package com.quman.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand palette (Quman deep blue to violet)
val QumanDeepBlue = Color(0xFF1E3A8A)
val QumanBlueVariant = Color(0xFF1E40AF)
val QumanViolet = Color(0xFF8B5CF6)
val QumanVioletDark = Color(0xFF6D28D9)
val QumanVioletLight = Color(0xFFDDD6FE)

// Semantic tokens
val MoneyOutRed = Color(0xFFDC2626)
val MoneyOutRedContainer = Color(0xFFFEE2E2)
val MoneyInGreen = Color(0xFF16A34A)
val MoneyInGreenContainer = Color(0xFFDCFCE7)
val PromoAmber = Color(0xFFF59E0B)
val PromoAmberContainer = Color(0xFFFEF3C7)

// Neutral surfaces (clean modern fintech)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextMuted = Color(0xFF94A3B8)
val DividerColor = Color(0xFFE2E8F0)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(QumanDeepBlue, QumanViolet)
)
val HeaderGradient = Brush.verticalGradient(
    colors = listOf(QumanDeepBlue, Color(0xFF312E81))
)
val ActiveCardBorderGradient = Brush.horizontalGradient(
    colors = listOf(QumanDeepBlue.copy(alpha = 0.8f), QumanViolet.copy(alpha = 0.8f))
)

@Immutable
data class QumanSemanticColors(
    val moneyOut: Color = MoneyOutRed,
    val moneyOutContainer: Color = MoneyOutRedContainer,
    val moneyIn: Color = MoneyInGreen,
    val moneyInContainer: Color = MoneyInGreenContainer,
    val promo: Color = PromoAmber,
    val promoContainer: Color = PromoAmberContainer,
    val primaryGradient: Brush = PrimaryGradient,
    val headerGradient: Brush = HeaderGradient
)

val LocalQumanColors = staticCompositionLocalOf { QumanSemanticColors() }

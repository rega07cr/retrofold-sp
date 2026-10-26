package com.retrofold.sp.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Palette retrò: oro GBA SP + viola schermo + verde "battery" su fondo quasi nero. */
object RetroColors {
    val Gold = Color(0xFFFFD34D)
    val GoldDeep = Color(0xFF9C6B1A)
    val Cobalt = Color(0xFF2E63D8)
    val Screen = Color(0xFFB9B4D6)
    val ScreenGreen = Color(0xFF9FD08A)
    val Bg = Color(0xFF0B0C10)
    val Card = Color(0xFF16171D)
    val Line = Color(0xFF26272F)
    val Mut = Color(0xFF9AA0AA)
}

fun RetroFoldColors() = darkColorScheme(
    primary = RetroColors.Gold,
    onPrimary = Color(0xFF141414),
    secondary = RetroColors.Cobalt,
    background = RetroColors.Bg,
    surface = RetroColors.Card,
    surfaceVariant = RetroColors.Line,
    onBackground = Color.White,
    onSurface = Color.White
)

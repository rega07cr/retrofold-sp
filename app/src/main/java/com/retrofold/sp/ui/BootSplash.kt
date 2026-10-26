package com.retrofold.sp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Splash retrò: il Game Boy chiuso si apre (guscio dorato),
 * lo schermo si accende e il logo "cade" con il classico *ping*.
 * Tocca per saltare. Durata totale ~3s.
 */
@Composable
fun BootSplash(s: S, onDone: () -> Unit) {
    val open = remember { Animatable(0f) }
    var logoIn by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val finish: () -> Unit = { if (!done) { done = true; onDone() } }

    LaunchedEffect(Unit) {
        delay(350)
        open.animateTo(1f, tween(1000, easing = LinearEasing))
        delay(350)
        logoIn = true
        delay(1500)
        finish()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
            .clickable(onClick = { finish() })
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Game Boy che si apre ──
        Box(contentAlignment = Alignment.Center) {
            // base (metà sotto, sempre visibile)
            Box(
                Modifier
                    .size(width = 190.dp, height = 96.dp)
                    .offset(y = 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFD9A441), Color(0xFF9C6B1A))
                        )
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(18.dp))
            ) {
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(Color(0xFF3A3A3E))
                            .border(2.dp, Color.Black, CircleShape)
                    )
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(Color(0xFF3A3A3E))
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }
            // coperchio (metà sopra) che ruota/si alza con "open"
            Box(
                Modifier
                    .size(width = 190.dp, height = 104.dp)
                    .offset(y = (-58 * open.value).dp)
                    .alpha(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF2CE6B), Color(0xFFB57E1D))
                        )
                    )
                    .border(3.dp, Color.Black, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                // schermo che si accende mentre si apre
                Box(
                    Modifier
                        .size(width = 130.dp, height = 62.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(2.dp, Color(0xFF5B4413), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .alpha(open.value)
                            .background(Color(0xFF9FD08A))
                    )
                    if (open.value > 0.85f) {
                        Text(
                            "▶",
                            color = Color(0xFF1d3a1d),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(70.dp))

        // ── Logo che "cade" ──
        val logoScale = if (logoIn) 1f else 0.01f
        Text(
            "★ RetroFold SP ★",
            color = Color(0xFFFFD34D),
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.scale(logoScale)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "GAME BOY ADVANCE SP",
            color = Color.White.copy(alpha = 0.85f),
            fontFamily = PixelFont,
            fontSize = 13.sp,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(22.dp))
        LinearProgressIndicator(
            progress = { open.value },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Color(0xFFFFD34D),
            trackColor = Color(0xFF26272F)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            s.splashLoading,
            color = Color(0xFF9AA0AA),
            fontFamily = PixelFont,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "(${s.splashTapToSkip})",
            color = Color(0xFF555A64),
            fontFamily = PixelFont,
            fontSize = 11.sp
        )
    }
}

package com.retrofold.sp.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animazione all'avvio di ogni gioco: sfondo bianco, scritta pixelata
 * "RETROFOLD SP" che scorre nei colori dell'arcobaleno + titolo del gioco.
 * Tocca per saltare. Durata ~2.4s.
 */
@Composable
fun GameBootScreen(romTitle: String, s: S, onDone: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    val finish: () -> Unit = { if (!done) { done = true; onDone() } }
    val rainbow = rememberInfiniteTransition(label = "rainbow")
    val hue by rainbow.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "hue"
    )
    val blink by rainbow.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(450, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "blink"
    )

    LaunchedEffect(romTitle) {
        delay(2400)
        finish()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = { finish() })
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "★ ★ ★",
            color = Color(0xFF9C6B1A),
            fontFamily = PixelFont,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(10.dp))
        RainbowPixelText(text = "RETROFOLD SP", baseHue = hue, fontSize = 38.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "GAME BOY ADVANCE SP",
            color = Color.Black,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B0C10))
                .border(3.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                "▶ $romTitle",
                color = Color(0xFFFFD34D),
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Color.hsv(hue % 360f, 0.85f, 0.9f),
            trackColor = Color(0xFFE3E3E3)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            s.startingGame,
            color = Color.Black.copy(alpha = 0.65f),
            fontFamily = PixelFont,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "▶",
            color = Color.Black,
            fontFamily = PixelFont,
            fontSize = 22.sp,
            modifier = Modifier.alpha(blink)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "(${s.splashTapToSkip})",
            color = Color.Black.copy(alpha = 0.4f),
            fontFamily = PixelFont,
            fontSize = 11.sp
        )
    }
}

/** Scritta pixelata con ogni lettera di un colore diverso dell'arcobaleno. */
@Composable
fun RainbowPixelText(text: String, baseHue: Float, fontSize: TextUnit) {
    Row(horizontalArrangement = Arrangement.Center) {
        text.forEachIndexed { i, ch ->
            Text(
                ch.toString(),
                color = Color.hsv((baseHue + i * 22f) % 360f, 0.85f, 0.88f),
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

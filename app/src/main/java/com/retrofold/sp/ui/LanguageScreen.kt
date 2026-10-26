package com.retrofold.sp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Prima apertura: scelta della lingua, con bottoni enormi e chiari.
 * IT e EN mostrati entrambi così è intuibile in qualsiasi lingua.
 */
@Composable
fun LanguageScreen(onPick: (AppLang) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C10))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌍", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Scegli la lingua / Choose language",
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Puoi cambiarla quando vuoi / You can change it anytime",
            fontFamily = PixelFont,
            fontSize = 13.sp,
            color = Color(0xFF9AA0AA),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(28.dp))
        RetroButton(
            text = "🇮🇹  ITALIANO",
            onClick = { onPick(AppLang.IT) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        RetroButton(
            text = "🇬🇧  ENGLISH",
            onClick = { onPick(AppLang.EN) },
            modifier = Modifier.fillMaxWidth(),
            bg = Color(0xFF2E63D8),
            fg = Color.White
        )
    }
}

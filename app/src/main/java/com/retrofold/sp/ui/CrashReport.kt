package com.retrofold.sp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Schermata report crash: mostra COSA è successo (stacktrace Java o
 * stadio nativo) con bottoni Condividi / Elimina / Chiudi.
 * È il modo per inchiodare i bug senza logcat.
 */
@Composable
fun CrashReportOverlay(
    s: S,
    report: String,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(Modifier.fillMaxWidth(0.92f)) {
            Text(
                "💥 ${s.crashTitle}",
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                s.crashMsg,
                fontFamily = PixelFont,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9AA0AA)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                Text(
                    report.take(4000),
                    fontFamily = PixelFont,
                    fontSize = 10.sp,
                    color = Color(0xFF7DFF9A),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetroButton(
                    text = "📤 ${s.share}",
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFF2E63D8),
                    fg = Color.White
                )
                RetroButton(
                    text = "🗑",
                    onClick = onDelete,
                    modifier = Modifier.weight(0.5f),
                    bg = Color(0xFF3A3A3E),
                    fg = Color.White
                )
            }
            Spacer(Modifier.height(8.dp))
            RetroButton(
                text = "✖ ${s.close}",
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                bg = Color(0xFF26272F),
                fg = Color.White
            )
        }
    }
}

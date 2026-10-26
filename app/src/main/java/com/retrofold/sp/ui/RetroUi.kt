package com.retrofold.sp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Font pixelosa: monospace ovunque per l'effetto retro. */
val PixelFont = FontFamily.Monospace

/**
 * Bottone retrò con bordo spesso e ombra dura (stile 16-bit).
 * Testo sempre maiuscolo + icona, chiarissimo da capire.
 */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = Color(0xFFFFD34D),
    fg: Color = Color(0xFF141414),
    corner: Dp = 12.dp
) {
    val shape = RoundedCornerShape(corner)
    Box(modifier) {
        // ombra dura
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(shape)
                .background(Color.Black)
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(bg)
                .border(3.dp, Color.Black, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text,
                color = fg,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/** Card retrò con bordo spesso e ombra dura. */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    bg: Color = Color(0xFF16171D),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.85f))
        )
        Column(
            Modifier
                .clip(shape)
                .background(bg)
                .border(3.dp, Color(0xFFFFD34D).copy(alpha = 0.55f), shape)
                .padding(14.dp),
            content = content
        )
    }
}

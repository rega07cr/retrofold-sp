package com.retrofold.sp.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.retrofold.sp.core.RomEntry
import com.retrofold.sp.device.CoverProfile

/**
 * CoverPocketScreen — telefono chiuso: 1-Hand Pocket Arcade.
 * Safe-area dinamica: evita punch-hole/camere usando il profilo device.
 * Tasti volume fisici rimappabili: in MainActivity intercetta onKeyDown
 * VOLUME_UP = salto/A, VOLUME_DOWN = sparo/B (solo in cover).
 */
@Composable
fun CoverPocketScreen(
    rom: RomEntry?,
    profile: CoverProfile,
    onBack: () -> Unit,
    onOpenFlexHint: () -> Unit,
    s: S = S.it
) {
    var score by remember { mutableIntStateOf(0) }
    // Padding alto = zona camera di ogni marca
    val topSafe = (profile.safeInsetCamDp).dp

    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(top = topSafe).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "📱 ${profile.deviceLabel} • cover ${profile.coverW}×${profile.coverH} @${profile.maxHz}Hz",
            color = Color.Gray, style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1B1E3A), Color(0xFF0B0C18)))
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👾 POCKET ARCADE", color = Color(0xFFFFD34D))
                Text(
                    rom?.title ?: s.noRom,
                    color = Color.White, style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("${s.score} $score", color = Color(0xFF7DFF9A))
                Spacer(Modifier.height(8.dp))
                Text(
                    s.coverHint,
                    color = Color.Gray, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { score += 100 }) { Text(s.tapJump) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = onBack) { Text(s.library) }
            Button(onClick = onOpenFlexHint) { Text(s.openFlex) }
        }
        if (!profile.fullAppOnCover) {
            Text(
                "${profile.deviceLabel}: ${s.coverWarn}",
                color = Color(0xFFFF9D5C), style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * FlipPetOverlay — cucciolo pixel cerniera-interattivo.
 * Half-opened = guarda in alto e saluta • Chiuso = dorme (zZz) + clack.
 * Audio clack via ExoPlayer/SoundPool con sample meccanico d'epoca (assets/clack.wav).
 */
@Composable
fun FlipPetOverlay(flexOpen: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        val face = if (flexOpen) "(•̀ᴗ•́)و 👋" else "(－‿－) zZz"
        Text(
            face,
            color = Color.White,
            modifier = Modifier.padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/** Helper: mappa tasti volume fisici ad azioni cover (da chiamare in onKeyDown). */
fun coverVolumeToAction(keyCode: Int): String? = when (keyCode) {
    KeyEvent.KEYCODE_VOLUME_UP -> "JUMP"
    KeyEvent.KEYCODE_VOLUME_DOWN -> "FIRE"
    else -> null
}

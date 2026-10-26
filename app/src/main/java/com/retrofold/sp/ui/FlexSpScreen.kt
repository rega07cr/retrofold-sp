package com.retrofold.sp.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrofold.sp.core.EmulatorEngine
import com.retrofold.sp.core.RomEntry

/**
 * FlexSpScreen — telefono piegato a L.
 * Sopra: schermo gioco con bezel nero + scritta GAME BOY ADVANCE SP + shader AGS-101.
 * Sotto: base oro (o skin scelta) con D-pad, A/B, Start/Select, L/R, speaker 4x4, LED.
 *
 * Replica la tua foto: guscio oro metallizzato, bezel nero, POWER ON / BATTERY FULL,
 * speaker centrale, D-pad incassato, pill diagonale B+A, griglia speaker in basso.
 */
@Composable
fun FlexSpScreen(
    rom: RomEntry,
    engine: EmulatorEngine,
    skin: GbaSkin,
    skins: List<GbaSkin>,
    ags101: Boolean,
    onSkin: (GbaSkin) -> Unit,
    onToggleFilter: () -> Unit,
    onBack: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    s: S = S.it
) {
    var showMacro by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // ── SCHERMO SUPERIORE ──
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(skin.shellTop, skin.shellBottom))
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.bezel)
                    .border(2.dp, skin.shellEdge, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("POWER ON", color = Color(0xFF9FD8FF), style = MaterialTheme.typography.labelSmall)
                    Text("BATTERY FULL", color = Color(0xFF9FD8FF), style = MaterialTheme.typography.labelSmall)
                }
                // Superficie emulatori: il core NDK disegna qui a 240x160 scalato GPU
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            // BUGFIX: la surface non è pronta nel factory — aspetta il callback,
                            // altrimenti il core disegna sul nulla (schermo nero).
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(h: SurfaceHolder) {
                                    try { engine.nativeSetSurface(h.surface) } catch (_: Throwable) {}
                                }
                                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = Unit
                                override fun surfaceDestroyed(h: SurfaceHolder) {
                                    try { engine.nativeSetSurface(null) } catch (_: Throwable) {}
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(skin.screenTint)
                )
                if (ags101) {
                    // Overlay shader AGS-101: grille micro-pixel + scanline (GPU reale nel .so,
                    // qui anteprima Compose leggera)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 2f)
                            .offset(y = 0.dp)
                    )
                }
                Text(
                    "${skin.screenText} • ${rom.title} ${if (ags101) "• AGS-101 ON" else ""}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                // Badge speciale per le tue grafiche (Pokémon / Triforce / Nintendo / Crystal / Minimal)
                skin.specialBadge?.let { badge ->
                    Text(
                        when (badge) {
                            "POKEMON" -> "◉ Pokémon • Squirtle"
                            "TRIFORCE" -> "▲ TRIFORCE"
                            "NINTENDO_RED" -> "Nintendo"
                            "CRYSTAL" -> "PLAY Mods • Device Designer • CR-CUSTOMS"
                            "MINIMAL" -> "MINIMAL AI WALLPAPER"
                            else -> badge
                        },
                        color = when (badge) {
                            "POKEMON" -> Color(0xFFFFD34D)
                            "TRIFORCE" -> Color(0xFFFFD34D)
                            "NINTENDO_RED" -> Color(0xFFFF3B3B)
                            "CRYSTAL" -> Color(0xFF00C2C2)
                            else -> Color.White.copy(0.7f)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Text(
                    "GAME BOY ADVANCE SP",
                    color = skin.logoColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
                )
            }
        }

        // Cerniera
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .background(Brush.horizontalGradient(listOf(skin.shellEdge, skin.shellTop, skin.shellEdge)))
        )

        // ── BASE INFERIORE (gamepad) ──
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(skin.shellTop, skin.shellBottom)))
                .padding(14.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // speaker centrale + L/R
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    KeyCap("L", skin) { down ->
                        if (down) press(engine, EmulatorEngine.Keys.L)
                        else release(engine, EmulatorEngine.Keys.L)
                    }
                    Box(
                        Modifier.size(14.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) { Box(Modifier.size(6.dp).clip(CircleShape).background(Color.DarkGray)) }
                    KeyCap("R", skin) { down ->
                        if (down) press(engine, EmulatorEngine.Keys.R)
                        else release(engine, EmulatorEngine.Keys.R)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DPad(skin) { mask, down ->
                        if (down) press(engine, mask) else release(engine, mask)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (skin.id == "gold_triforce") "SELECT   START" else "START GAME",
                            color = Color.White.copy(0.9f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            KeyCap("B", skin, big = true) { down ->
                                if (down) press(engine, EmulatorEngine.Keys.B)
                                else release(engine, EmulatorEngine.Keys.B)
                            }
                            KeyCap("A", skin, big = true) { down ->
                                if (down) press(engine, EmulatorEngine.Keys.A)
                                else release(engine, EmulatorEngine.Keys.A)
                            }
                        }
                    }
                }
                // Start/Select + speaker 4x4 + LED
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyCap("SELECT", skin, small = true) { down ->
                            if (down) press(engine, EmulatorEngine.Keys.SELECT)
                            else release(engine, EmulatorEngine.Keys.SELECT)
                        }
                        KeyCap("START", skin, small = true) { down ->
                            if (down) press(engine, EmulatorEngine.Keys.START)
                            else release(engine, EmulatorEngine.Keys.START)
                        }
                    }
                    SpeakerGrille()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(skin.ledOn))
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = { showMacro = !showMacro }) { Text("⋯ ${s.macro}") }
                        TextButton(onClick = onToggleFilter) { Text(if (ags101) "LCD:ON" else "LCD:OFF") }
                        TextButton(onClick = onBack) { Text("⏏ ${s.back}") }
                    }
                }
                if (showMacro) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { onSave(0) }) { Text("💾 ${s.save}") }
                        Button(onClick = { onLoad(0) }) { Text("📂 ${s.load}") }
                        Button(onClick = {
                            try { engine.nativeRewind(10) } catch (_: Throwable) {}
                        }) { Text("⏪ 10s") }
                        Button(onClick = {
                            try { engine.nativeSetFastForward(if (engine.fastForward == 1) 2 else 1) } catch (_: Throwable) {}
                        }) { Text("⏩") }
                    }
                }
                // Skin picker
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(skins) { s ->
                        val sel = s.id == skin.id
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(listOf(s.shellTop, s.shellBottom))
                                )
                                .border(
                                    if (sel) 3.dp else 1.dp,
                                    if (sel) Color.White else Color.Black.copy(0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSkin(s) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) { Text(s.label, color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

private fun press(e: EmulatorEngine, m: Int) { try { e.nativePress(m) } catch (_: Throwable) {} }
private fun release(e: EmulatorEngine, m: Int) { try { e.nativeRelease(m) } catch (_: Throwable) {} }

@Composable
private fun KeyCap(
    label: String,
    skin: GbaSkin,
    big: Boolean = false,
    small: Boolean = false,
    onPress: (Boolean) -> Unit
) {
    val size = when { big -> 64.dp; small -> 44.dp; else -> 52.dp }
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(skin.buttons)
            .border(2.dp, Color.Black.copy(0.5f), CircleShape)
            // BUGFIX: prima era solo clickable = tasto sempre "giù" (input incastrato).
            // Ora press alla pressione e release al rilascio, come un tasto vero.
            .pointerInput(label) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress(true)
                    do {
                        val ev = awaitPointerEvent()
                    } while (ev.changes.any { it.pressed })
                    onPress(false)
                }
            },
        contentAlignment = Alignment.Center
    ) { Text(label, color = skin.buttonText, fontWeight = FontWeight.Bold) }
}

@Composable
private fun DPad(skin: GbaSkin, onDir: (Int, Boolean) -> Unit) {
    // Croce fedele: contenitore circolare incassato come nella foto
    Box(
        Modifier.size(132.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DPadBtn("▲", skin) { onDir(EmulatorEngine.Keys.UP, it) }
            Row {
                DPadBtn("◀", skin) { onDir(EmulatorEngine.Keys.LEFT, it) }
                Spacer(Modifier.size(44.dp))
                DPadBtn("▶", skin) { onDir(EmulatorEngine.Keys.RIGHT, it) }
            }
            DPadBtn("▼", skin) { onDir(EmulatorEngine.Keys.DOWN, it) }
        }
    }
}

@Composable
private fun DPadBtn(t: String, skin: GbaSkin, onPress: (Boolean) -> Unit) {
    Box(
        Modifier.size(44.dp).background(skin.dpad)
            .pointerInput(t) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress(true)
                    do {
                        val ev = awaitPointerEvent()
                    } while (ev.changes.any { it.pressed })
                    onPress(false)
                }
            },
        contentAlignment = Alignment.Center
    ) { Text(t, color = Color.White) }
}

@Composable
private fun SpeakerGrille() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Color.Black))
                }
            }
        }
    }
}

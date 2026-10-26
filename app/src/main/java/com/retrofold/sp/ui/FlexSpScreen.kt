package com.retrofold.sp.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrofold.sp.core.EmulatorEngine
import com.retrofold.sp.core.RomEntry

/**
 * FlexSpScreen — schermo sopra + gamepad sotto (piegato a L o aperto).
 * Foto: la foto resta come sfondo, ma i comandi sono un gamepad
 * semitrasparente SEMPRE visibile (non dipende dalla calibrazione della
 * foto); guscio procedurale altrimenti.
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
    onToggleSpeed: () -> Unit = {},
    s: S = S.it
) {
    var showMacro by remember { mutableStateOf(false) }
    // Copia locale osservabile: engine.fastForward è un campo semplice,
    // leggerlo qui dentro non ricomporrebbe la label ⏩ dopo il toggle.
    var speedLabel by remember { mutableIntStateOf(engine.fastForward) }
    // Slot di salvataggio selezionato (prima solo lo slot 0, gli altri
    // esistevano nel codice ma erano irraggiungibili dalla UI).
    var slot by remember { mutableIntStateOf(0) }
    val onSaveSlot: (Int) -> Unit = { onSave(slot) }
    val onLoadSlot: (Int) -> Unit = { onLoad(slot) }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (skin.photoRes != null) {
            PhotoTopHalf(rom, engine, skin, skins, onSkin, ags101, onBack, onToggleFilter, showMacro, { showMacro = !showMacro }, Modifier.weight(1f))
        } else {
            ProceduralTopHalf(rom, engine, skin, ags101, Modifier.weight(1f))
        }

        // Cerniera
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .background(Brush.horizontalGradient(listOf(skin.shellEdge, skin.shellTop, skin.shellEdge)))
        )

        if (skin.photoRes != null) {
            PhotoBottomHalf(
                engine, skin, skins,
                showMacro = showMacro,
                speedLabel = speedLabel,
                slot = slot,
                onSlot = { slot = it },
                onSpeed = { onToggleSpeed(); speedLabel = engine.fastForward },
                onSave = onSaveSlot, onLoad = onLoadSlot, onSkin = onSkin, s = s,
                modifier = Modifier.weight(1f)
            )
        } else {
            ProceduralBottomHalf(
                engine, skin, skins,
                showMacro = showMacro,
                onShowMacro = { showMacro = !showMacro },
                speedLabel = speedLabel,
                slot = slot,
                onSlot = { slot = it },
                onSpeed = { onToggleSpeed(); speedLabel = engine.fastForward },
                onToggleFilter = onToggleFilter,
                onBack = onBack,
                onSave = onSaveSlot, onLoad = onLoadSlot, onSkin = onSkin,
                ags101 = ags101, s = s,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** SurfaceView con callback corretta: la surface si passa solo quando esiste. */
@Composable
private fun EmuView(engine: EmulatorEngine, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(h: SurfaceHolder) {
                        try { engine.nativeSetSurface(h.surface) } catch (_: Throwable) {}
                    }
                    // Reimposta geometry+window anche al resize (rotazione,
                    // multi-window): prima veniva ignorato e il video restava
                    // stirato/vuoto finché la surface non veniva ricreata.
                    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
                        try { engine.nativeSetSurface(h.surface) } catch (_: Throwable) {}
                    }
                    override fun surfaceDestroyed(h: SurfaceHolder) {
                        try { engine.nativeSetSurface(null) } catch (_: Throwable) {}
                    }
                })
            }
        },
        modifier = modifier
    )
}

// ── MODALITÀ FOTO (uguale alle originali) ──

@Composable
private fun PhotoTopHalf(
    rom: RomEntry,
    engine: EmulatorEngine,
    skin: GbaSkin,
    skins: List<GbaSkin>,
    onSkin: (GbaSkin) -> Unit,
    ags101: Boolean,
    onBack: () -> Unit,
    onToggleFilter: () -> Unit,
    showMacro: Boolean,
    onMacro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxWidth().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(skin.photoRes!!),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        // Emulatore sopra lo schermo della foto (posizioni calibrate per skin).
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            Box(
                Modifier
                    .offset(x = w * skin.emuMargin, y = h * skin.emuTop)
                    .size(width = w * (1f - 2f * skin.emuMargin), height = h * (skin.emuBottom - skin.emuTop))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFFFD34D).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            ) {
                EmuView(engine, Modifier.fillMaxSize())
                if (ags101) {
                    Canvas(Modifier.fillMaxSize()) {
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                Color.Black.copy(alpha = 0.22f),
                                Offset(0f, y), Offset(size.width, y), strokeWidth = 1f
                            )
                            y += 4f
                        }
                    }
                }
            }
        }
        // Scorciatoie discrete sopra la foto.
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            SkinDropdown(skins = skins, current = skin, onSkin = onSkin)
            TextButton(onClick = onMacro) {
                Text("⋯", color = Color.White.copy(alpha = if (showMacro) 1f else 0.7f))
            }
            TextButton(onClick = onToggleFilter) {
                Text(if (ags101) "LCD:ON" else "LCD:OFF", color = Color.White.copy(alpha = 0.8f))
            }
            TextButton(onClick = onBack) { Text("⏏", color = Color.White) }
        }
        Text(
            rom.title,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PhotoBottomHalf(
    engine: EmulatorEngine,
    skin: GbaSkin,
    skins: List<GbaSkin>,
    showMacro: Boolean,
    speedLabel: Int,
    slot: Int,
    onSlot: (Int) -> Unit,
    onSpeed: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onSkin: (GbaSkin) -> Unit,
    s: S,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxWidth().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(skin.photoRes!!),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )
        // Gamepad SEMPRE visibile sopra la foto: le vecchie zone invisibili
        // dipendevano da frazioni generiche e su molte foto mancavano i
        // tasti veri. Ora la foto fa da sfondo e i comandi funzionano ovunque.
        OverlayGamepad(engine, Modifier.fillMaxSize().padding(10.dp))
        // Pannello macro + picker skin in basso.
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(6.dp)
        ) {
            if (showMacro) {
                MacroPanel(
                    engine = engine,
                    speedLabel = speedLabel,
                    slot = slot,
                    onSlot = onSlot,
                    onSpeed = onSpeed,
                    onSave = onSave,
                    onLoad = onLoad,
                    s = s
                )
            }
            // L/R sempre a portata: prima esistevano solo come zone
            // invisibili legate alla foto (spesso mancate).
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PhotoKey("L") { down ->
                    if (down) press(engine, EmulatorEngine.Keys.L)
                    else release(engine, EmulatorEngine.Keys.L)
                }
                PhotoKey("R") { down ->
                    if (down) press(engine, EmulatorEngine.Keys.R)
                    else release(engine, EmulatorEngine.Keys.R)
                }
            }
            SkinThumbs(skins, skin, onSkin)
        }
    }
}

/** Gamepad semitrasparente sopra la foto: D-pad + A/B + START/SELECT
 * sempre visibili e funzionanti su QUALSIASI sfondo. La foto resta
 * visibile dietro (alpha 0.55) ma i comandi non dipendono più da dove
 * cadono i tasti veri nella foto. */
@Composable
private fun OverlayGamepad(engine: EmulatorEngine, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OverlayDPad { mask, down ->
            if (down) press(engine, mask) else release(engine, mask)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoKey("SELECT") { down ->
                if (down) press(engine, EmulatorEngine.Keys.SELECT)
                else release(engine, EmulatorEngine.Keys.SELECT)
            }
            PhotoKey("START") { down ->
                if (down) press(engine, EmulatorEngine.Keys.START)
                else release(engine, EmulatorEngine.Keys.START)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverlayBtn("B") { down ->
                if (down) press(engine, EmulatorEngine.Keys.B)
                else release(engine, EmulatorEngine.Keys.B)
            }
            OverlayBtn("A") { down ->
                if (down) press(engine, EmulatorEngine.Keys.A)
                else release(engine, EmulatorEngine.Keys.A)
            }
        }
    }
}

@Composable
private fun OverlayDPad(onDir: (Int, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OverlayBtn("▲") { onDir(EmulatorEngine.Keys.UP, it) }
        Row {
            OverlayBtn("◀") { onDir(EmulatorEngine.Keys.LEFT, it) }
            Spacer(Modifier.size(52.dp))
            OverlayBtn("▶") { onDir(EmulatorEngine.Keys.RIGHT, it) }
        }
        OverlayBtn("▼") { onDir(EmulatorEngine.Keys.DOWN, it) }
    }
}

@Composable
private fun OverlayBtn(label: String, onPress: (Boolean) -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
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
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** Pannello macro completo: save/load sullo slot scelto, rewind, speed, reset.
 * Prima save/load usavano solo lo slot 0 e rewind/reset esistevano solo
 * in una delle due metà — ora identico ovunque. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroPanel(
    engine: EmulatorEngine,
    speedLabel: Int,
    slot: Int,
    onSlot: (Int) -> Unit,
    onSpeed: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    s: S
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SmallBtn("💾 ${s.save}") { onSave(slot) }
            SmallBtn("📂 ${s.load}") { onLoad(slot) }
            SmallBtn("⏩ ${if (speedLabel == 1) "1x" else "${speedLabel}x"}") { onSpeed() }
            SmallBtn("⏪ 10s") { try { engine.nativeRewind(10) } catch (_: Throwable) {} }
            SmallBtn("🔄 ${s.reset}") { try { engine.nativeReset() } catch (_: Throwable) {} }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.slot, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            (0..3).forEach { i ->
                FilterChip(
                    selected = slot == i,
                    onClick = { onSlot(i) },
                    label = { Text("$i") }
                )
            }
        }
    }
}

/** Menu a tendina per lo sfondo/colori del Game Boy: foto originali
 * + tinte unite (inclusa Mezzanotte relax per non disturbare la vista). */
@Composable
private fun SkinDropdown(
    skins: List<GbaSkin>,
    current: GbaSkin,
    onSkin: (GbaSkin) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("🎨", color = Color.White.copy(alpha = 0.9f))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            skins.forEach { sk ->
                DropdownMenuItem(
                    text = { Text((if (sk.id == current.id) "✔ " else "") + sk.label) },
                    onClick = { onSkin(sk); expanded = false }
                )
            }
        }
    }
}

/** Tasto semitrasparente sopra la foto: funziona su QUALSIASI sfondo. */
@Composable
private fun PhotoKey(label: String, onPress: (Boolean) -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .pointerInput(label) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress(true)
                    do {
                        val ev = awaitPointerEvent()
                    } while (ev.changes.any { it.pressed })
                    onPress(false)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SmallBtn(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SkinThumbs(skins: List<GbaSkin>, current: GbaSkin, onSkin: (GbaSkin) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(skins) { sk ->
            val sel = sk.id == current.id
            if (sk.photoRes != null) {
                Image(
                    painterResource(sk.photoRes),
                    contentDescription = sk.label,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            if (sel) 3.dp else 1.dp,
                            if (sel) Color(0xFFFFD34D) else Color.White.copy(0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSkin(sk) },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(sk.shellTop, sk.shellBottom)))
                        .border(
                            if (sel) 3.dp else 1.dp,
                            if (sel) Color(0xFFFFD34D) else Color.White.copy(0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSkin(sk) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        sk.label.take(2),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ── MODALITÀ PROCEDURALE (skin senza foto) ──

@Composable
private fun ProceduralTopHalf(
    rom: RomEntry,
    engine: EmulatorEngine,
    skin: GbaSkin,
    ags101: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(skin.shellTop, skin.shellBottom)))
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(skin.screenTint)
            ) {
                EmuView(engine, Modifier.fillMaxSize())
                if (ags101) {
                    Canvas(Modifier.fillMaxSize()) {
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                Color.Black.copy(alpha = 0.22f),
                                Offset(0f, y), Offset(size.width, y), strokeWidth = 1f
                            )
                            y += 4f
                        }
                    }
                }
            }
            Text(
                "${skin.screenText} • ${rom.title} ${if (ags101) "• AGS-101 ON" else ""}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
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
}

@Composable
private fun ProceduralBottomHalf(
    engine: EmulatorEngine,
    skin: GbaSkin,
    skins: List<GbaSkin>,
    showMacro: Boolean,
    onShowMacro: () -> Unit,
    speedLabel: Int,
    slot: Int,
    onSlot: (Int) -> Unit,
    onSpeed: () -> Unit,
    onToggleFilter: () -> Unit,
    onBack: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onSkin: (GbaSkin) -> Unit,
    ags101: Boolean,
    s: S,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(skin.shellTop, skin.shellBottom)))
            .padding(14.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
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
                    TextButton(onClick = onShowMacro) { Text("⋯ ${s.macro}") }
                    TextButton(onClick = onToggleFilter) { Text(if (ags101) "LCD:ON" else "LCD:OFF") }
                    SkinDropdown(skins = skins, current = skin, onSkin = onSkin)
                    TextButton(onClick = onBack) { Text("⏏ ${s.back}") }
                }
            }
            if (showMacro) {
                MacroPanel(
                    engine = engine,
                    speedLabel = speedLabel,
                    slot = slot,
                    onSlot = onSlot,
                    onSpeed = onSpeed,
                    onSave = onSave,
                    onLoad = onLoad,
                    s = s
                )
            }
            SkinThumbs(skins, skin, onSkin)
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

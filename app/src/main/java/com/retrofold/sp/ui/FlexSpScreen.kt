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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import com.retrofold.sp.RetroFoldApp
import com.retrofold.sp.core.EmulatorEngine
import com.retrofold.sp.core.RomEntry
import com.retrofold.sp.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * FlexSpScreen — schermo sopra + gamepad sotto (piegato a L o aperto).
 * Foto: la foto resta come sfondo, gamepad semitrasparente SEMPRE visibile,
 * ora spostabile a piacere (edit mode) con opacità regolabile.
 * Gli stili si possono nascondere per non disturbare la vista.
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
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val scope = rememberCoroutineScope()
    var showMacro by remember { mutableStateOf(false) }
    var speedLabel by remember { mutableIntStateOf(engine.fastForward) }
    var slot by remember { mutableIntStateOf(0) }
    val onSaveSlot: (Int) -> Unit = { onSave(slot) }
    val onLoadSlot: (Int) -> Unit = { onLoad(slot) }

    // ── Layout custom: globale + per-gioco ──
    var editPad by remember { mutableStateOf(false) }
    var opacity by remember { mutableFloatStateOf(0.55f) }
    var dpadOff by remember { mutableStateOf(Offset.Zero) }
    var abOff by remember { mutableStateOf(Offset.Zero) }
    var sysOff by remember { mutableStateOf(Offset.Zero) }
    var gameScale by remember { mutableFloatStateOf(0.86f) }
    var gameOff by remember { mutableStateOf(Offset.Zero) }
    var lrScale by remember { mutableFloatStateOf(1f) }
    var lrOff by remember { mutableStateOf(Offset.Zero) }
    var dpadScale by remember { mutableFloatStateOf(1f) }
    var abScale by remember { mutableFloatStateOf(1f) }
    var sysScale by remember { mutableFloatStateOf(1f) }
    // Offset di OGNI singolo tasto (regolazione fisica col dito).
    var btnOffs by remember { mutableStateOf(mapOf<String, Offset>()) }
    var perGame by remember { mutableStateOf(false) }
    var hideThumbs by remember { mutableStateOf(false) }
    var hiddenIds by remember { mutableStateOf(setOf<String>()) }
    var loaded by remember { mutableStateOf(false) }
    val gtag = remember(rom.id) { rom.id.take(16) }
    fun gk(name: String) = androidx.datastore.preferences.core.floatPreferencesKey("g_${gtag}_$name")
    fun bk(k: String, axis: String) = androidx.datastore.preferences.core.floatPreferencesKey("btn_${k}_$axis")
    fun gbk(k: String, axis: String) = androidx.datastore.preferences.core.floatPreferencesKey("g_${gtag}_b_${k}_$axis")
    val btnKeys = listOf("up", "down", "left", "right", "a", "b", "start", "select", "l", "r")
    fun seedBtns(): Map<String, Offset> = mapOf(
        "up" to dpadOff, "down" to dpadOff, "left" to dpadOff, "right" to dpadOff,
        "a" to abOff, "b" to abOff,
        "start" to sysOff, "select" to sysOff,
        "l" to lrOff, "r" to lrOff
    )
    fun moveBtn(k: String, o: Offset) { btnOffs = btnOffs + (k to o) }

    LaunchedEffect(Unit) {
        try {
            val p = app.dataStore.data.first()
            opacity = (p[RetroFoldApp.PAD_OPACITY] ?: 0.55f).coerceIn(0.25f, 1f)
            dpadOff = Offset(p[RetroFoldApp.PAD_DPAD_X] ?: 0f, p[RetroFoldApp.PAD_DPAD_Y] ?: 0f)
            abOff = Offset(p[RetroFoldApp.PAD_AB_X] ?: 0f, p[RetroFoldApp.PAD_AB_Y] ?: 0f)
            sysOff = Offset(p[RetroFoldApp.PAD_SYS_X] ?: 0f, p[RetroFoldApp.PAD_SYS_Y] ?: 0f)
            gameScale = (p[RetroFoldApp.GAME_SCALE] ?: 0.86f).coerceIn(0.6f, 1f)
            gameOff = Offset(p[RetroFoldApp.GAME_DX] ?: 0f, p[RetroFoldApp.GAME_DY] ?: 0f)
            lrScale = (p[RetroFoldApp.LR_SCALE] ?: 1f).coerceIn(0.7f, 1.6f)
            lrOff = Offset(p[RetroFoldApp.LR_DX] ?: 0f, p[RetroFoldApp.LR_DY] ?: 0f)
            dpadScale = (p[RetroFoldApp.DPAD_SCALE] ?: 1f).coerceIn(0.7f, 1.6f)
            abScale = (p[RetroFoldApp.AB_SCALE] ?: 1f).coerceIn(0.7f, 1.6f)
            sysScale = (p[RetroFoldApp.SYS_SCALE] ?: 1f).coerceIn(0.7f, 1.6f)
            perGame = p[RetroFoldApp.PER_GAME] ?: false
            hideThumbs = p[RetroFoldApp.HIDE_THUMBS] ?: false
            hiddenIds = (p[RetroFoldApp.HIDDEN_SKINS] ?: "").split(",").filter { it.isNotBlank() }.toSet()
            // Migrazione: vecchi offset di gruppo -> singoli tasti; se esistono
            // offset singoli salvati, quelli vincono.
            val seed = mapOf(
                "up" to Offset(p[RetroFoldApp.PAD_DPAD_X] ?: 0f, p[RetroFoldApp.PAD_DPAD_Y] ?: 0f),
                "down" to Offset(p[RetroFoldApp.PAD_DPAD_X] ?: 0f, p[RetroFoldApp.PAD_DPAD_Y] ?: 0f),
                "left" to Offset(p[RetroFoldApp.PAD_DPAD_X] ?: 0f, p[RetroFoldApp.PAD_DPAD_Y] ?: 0f),
                "right" to Offset(p[RetroFoldApp.PAD_DPAD_X] ?: 0f, p[RetroFoldApp.PAD_DPAD_Y] ?: 0f),
                "a" to Offset(p[RetroFoldApp.PAD_AB_X] ?: 0f, p[RetroFoldApp.PAD_AB_Y] ?: 0f),
                "b" to Offset(p[RetroFoldApp.PAD_AB_X] ?: 0f, p[RetroFoldApp.PAD_AB_Y] ?: 0f),
                "start" to Offset(p[RetroFoldApp.PAD_SYS_X] ?: 0f, p[RetroFoldApp.PAD_SYS_Y] ?: 0f),
                "select" to Offset(p[RetroFoldApp.PAD_SYS_X] ?: 0f, p[RetroFoldApp.PAD_SYS_Y] ?: 0f),
                "l" to Offset(p[RetroFoldApp.LR_DX] ?: 0f, p[RetroFoldApp.LR_DY] ?: 0f),
                "r" to Offset(p[RetroFoldApp.LR_DX] ?: 0f, p[RetroFoldApp.LR_DY] ?: 0f)
            )
            val m = seed.toMutableMap()
            var any = false
            for (k in btnKeys) {
                val x = p[bk(k, "x")]; val y = p[bk(k, "y")]
                if (x != null && y != null) { m[k] = Offset(x, y); any = true }
            }
            btnOffs = m
        } catch (_: Throwable) {}
        loaded = true
    }
    // Quando cambia gioco (o si attiva per-gioco): carica override per-gioco se esiste.
    LaunchedEffect(rom.id, perGame, loaded) {
        if (!loaded || !perGame) return@LaunchedEffect
        try {
            val p = app.dataStore.data.first()
            p[gk("op")]?.let { opacity = it.coerceIn(0.25f, 1f) }
            val dx = p[gk("ddx")]; val dy = p[gk("ddy")]
            if (dx != null && dy != null) dpadOff = Offset(dx, dy)
            val ax = p[gk("abx")]; val ay = p[gk("aby")]
            if (ax != null && ay != null) abOff = Offset(ax, ay)
            val sx = p[gk("sysx")]; val sy = p[gk("sysy")]
            if (sx != null && sy != null) sysOff = Offset(sx, sy)
            p[gk("gsc")]?.let { gameScale = it.coerceIn(0.6f, 1f) }
            val gx = p[gk("gdx")]; val gy = p[gk("gdy")]
            if (gx != null && gy != null) gameOff = Offset(gx, gy)
            p[gk("lrs")]?.let { lrScale = it.coerceIn(0.7f, 1.6f) }
            val lx = p[gk("ldx")]; val ly = p[gk("ldy")]
            if (lx != null && ly != null) lrOff = Offset(lx, ly)
            p[gk("dps")]?.let { dpadScale = it.coerceIn(0.7f, 1.6f) }
            p[gk("abs")]?.let { abScale = it.coerceIn(0.7f, 1.6f) }
            p[gk("sys")]?.let { sysScale = it.coerceIn(0.7f, 1.6f) }
            val seed = seedBtns().toMutableMap()
            var any = false
            for (k in btnKeys) {
                val x = p[gbk(k, "x")]; val y = p[gbk(k, "y")]
                if (x != null && y != null) { seed[k] = Offset(x, y); any = true }
            }
            if (any) btnOffs = seed
        } catch (_: Throwable) {}
    }
    fun saveAll() {
        if (!loaded) return
        scope.launch {
            try {
                app.dataStore.edit {
                    it[RetroFoldApp.PER_GAME] = perGame
                    it[RetroFoldApp.HIDE_THUMBS] = hideThumbs
                    it[RetroFoldApp.HIDDEN_SKINS] = hiddenIds.joinToString(",")
                    if (perGame) {
                        it[gk("op")] = opacity
                        it[gk("ddx")] = dpadOff.x; it[gk("ddy")] = dpadOff.y
                        it[gk("abx")] = abOff.x; it[gk("aby")] = abOff.y
                        it[gk("sysx")] = sysOff.x; it[gk("sysy")] = sysOff.y
                        it[gk("gsc")] = gameScale
                        it[gk("gdx")] = gameOff.x; it[gk("gdy")] = gameOff.y
                        it[gk("lrs")] = lrScale
                        it[gk("ldx")] = lrOff.x; it[gk("ldy")] = lrOff.y
                        it[gk("dps")] = dpadScale
                        it[gk("abs")] = abScale
                        it[gk("sys")] = sysScale
                        for (k in btnKeys) {
                            val o = btnOffs[k] ?: Offset.Zero
                            it[gbk(k, "x")] = o.x; it[gbk(k, "y")] = o.y
                        }
                    } else {
                        it[RetroFoldApp.PAD_OPACITY] = opacity
                        it[RetroFoldApp.PAD_DPAD_X] = dpadOff.x
                        it[RetroFoldApp.PAD_DPAD_Y] = dpadOff.y
                        it[RetroFoldApp.PAD_AB_X] = abOff.x
                        it[RetroFoldApp.PAD_AB_Y] = abOff.y
                        it[RetroFoldApp.PAD_SYS_X] = sysOff.x
                        it[RetroFoldApp.PAD_SYS_Y] = sysOff.y
                        it[RetroFoldApp.GAME_SCALE] = gameScale
                        it[RetroFoldApp.GAME_DX] = gameOff.x
                        it[RetroFoldApp.GAME_DY] = gameOff.y
                        it[RetroFoldApp.LR_SCALE] = lrScale
                        it[RetroFoldApp.LR_DX] = lrOff.x
                        it[RetroFoldApp.LR_DY] = lrOff.y
                        it[RetroFoldApp.DPAD_SCALE] = dpadScale
                        it[RetroFoldApp.AB_SCALE] = abScale
                        it[RetroFoldApp.SYS_SCALE] = sysScale
                        for (k in btnKeys) {
                            val o = btnOffs[k] ?: Offset.Zero
                            it[bk(k, "x")] = o.x; it[bk(k, "y")] = o.y
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }
    LaunchedEffect(opacity, dpadOff, abOff, sysOff, gameScale, gameOff, lrScale, lrOff, dpadScale, abScale, sysScale, btnOffs, perGame, hideThumbs, hiddenIds) { saveAll() }

    val visibleSkins = remember(skins, hiddenIds) {
        val v = skins.filterNot { hiddenIds.contains(it.id) }
        if (v.isEmpty()) skins else v
    }
    fun hideSkin(id: String) { hiddenIds = hiddenIds + id }
    fun showAllSkins() { hiddenIds = emptySet() }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (skin.photoRes != null) {
            PhotoTopHalf(rom, engine, skin, visibleSkins, skins, hiddenIds, onSkin, ::hideSkin, ::showAllSkins, ags101, onBack, onToggleFilter, showMacro, { showMacro = !showMacro }, editPad, { editPad = !editPad }, gameScale, gameOff, { gameOff = it }, Modifier.weight(1f))
        } else {
            ProceduralTopHalf(rom, engine, skin, ags101, gameScale, Modifier.weight(1f))
        }

        // Cerniera
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .background(Brush.horizontalGradient(listOf(skin.shellEdge, skin.shellTop, skin.shellEdge)))
        )

        if (skin.photoRes != null) {
            PhotoBottomHalf(
                engine, skin, visibleSkins, skins, hiddenIds,
                showMacro = showMacro,
                speedLabel = speedLabel,
                slot = slot,
                onSlot = { slot = it },
                onSpeed = { onToggleSpeed(); speedLabel = engine.fastForward },
                onSave = onSaveSlot, onLoad = onLoadSlot, onSkin = onSkin,
                onHideSkin = ::hideSkin, onShowAll = ::showAllSkins,
                editPad = editPad, onToggleEdit = { editPad = !editPad },
                opacity = opacity, onOpacity = { opacity = it },
                dpadOff = dpadOff, abOff = abOff, sysOff = sysOff,
                onDpad = { dpadOff = it }, onAb = { abOff = it }, onSys = { sysOff = it },
                gameScale = gameScale, onGameScale = { gameScale = it },
                lrScale = lrScale, onLrScale = { lrScale = it },
                lrOff = lrOff, onLrOff = { lrOff = it },
                dpadScale = dpadScale, onDpadScale = { dpadScale = it },
                abScale = abScale, onAbScale = { abScale = it },
                sysScale = sysScale, onSysScale = { sysScale = it },
                btnOffs = btnOffs, onBtnMove = ::moveBtn,
                perGame = perGame, onPerGame = { perGame = it },
                hideThumbs = hideThumbs, onToggleThumbs = { hideThumbs = !hideThumbs },
                onResetPos = { dpadOff = Offset.Zero; abOff = Offset.Zero; sysOff = Offset.Zero; gameOff = Offset.Zero; lrOff = Offset.Zero; btnOffs = mapOf(); opacity = 0.55f; gameScale = 0.86f; lrScale = 1f; dpadScale = 1f; abScale = 1f; sysScale = 1f },
                onResetBtns = { btnOffs = mapOf() },
                s = s,
                modifier = Modifier.weight(1f)
            )
        } else {
            ProceduralBottomHalf(
                engine, skin, visibleSkins,
                showMacro = showMacro,
                onShowMacro = { showMacro = !showMacro },
                speedLabel = speedLabel,
                slot = slot,
                onSlot = { slot = it },
                onSpeed = { onToggleSpeed(); speedLabel = engine.fastForward },
                onToggleFilter = onToggleFilter,
                onBack = onBack,
                onSave = onSaveSlot, onLoad = onLoadSlot, onSkin = onSkin,
                onToggleEdit = { editPad = !editPad }, editPad = editPad,
                hideThumbs = hideThumbs, onToggleThumbs = { hideThumbs = !hideThumbs },
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
    visibleSkins: List<GbaSkin>,
    allSkins: List<GbaSkin>,
    hiddenIds: Set<String>,
    onSkin: (GbaSkin) -> Unit,
    onHideSkin: (String) -> Unit,
    onShowAll: () -> Unit,
    ags101: Boolean,
    onBack: () -> Unit,
    onToggleFilter: () -> Unit,
    showMacro: Boolean,
    onMacro: () -> Unit,
    editPad: Boolean,
    onToggleEdit: () -> Unit,
    gameScale: Float,
    gameOff: Offset,
    onGameOff: (Offset) -> Unit,
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
        // Schermo gioco centrato e RIDIMENSIONABILE: larghezza = gameScale
        // (slider 60–100%), rapporto GBA 3:2, in edit mode si trascina.
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bw = maxWidth * gameScale
            DragGroup(gameOff, editPad, onGameOff) {
                Box(
                    Modifier
                        .width(bw)
                        .aspectRatio(3f / 2f)
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
        }
        // Scorciatoie discrete sopra la foto.
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            SkinDropdown(skins = visibleSkins, current = skin, onSkin = onSkin, onHide = onHideSkin, onShowAll = onShowAll, showAllVisible = hiddenIds.isNotEmpty())
            TextButton(onClick = onToggleEdit) {
                Text(if (editPad) "✓" else "✎", color = Color.White.copy(alpha = 0.9f))
            }
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
    visibleSkins: List<GbaSkin>,
    allSkins: List<GbaSkin>,
    hiddenIds: Set<String>,
    showMacro: Boolean,
    speedLabel: Int,
    slot: Int,
    onSlot: (Int) -> Unit,
    onSpeed: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onSkin: (GbaSkin) -> Unit,
    onHideSkin: (String) -> Unit,
    onShowAll: () -> Unit,
    editPad: Boolean,
    onToggleEdit: () -> Unit,
    opacity: Float,
    onOpacity: (Float) -> Unit,
    dpadOff: Offset,
    abOff: Offset,
    sysOff: Offset,
    onDpad: (Offset) -> Unit,
    onAb: (Offset) -> Unit,
    onSys: (Offset) -> Unit,
    gameScale: Float,
    onGameScale: (Float) -> Unit,
    lrScale: Float,
    onLrScale: (Float) -> Unit,
    lrOff: Offset,
    onLrOff: (Offset) -> Unit,
    dpadScale: Float,
    onDpadScale: (Float) -> Unit,
    abScale: Float,
    onAbScale: (Float) -> Unit,
    sysScale: Float,
    onSysScale: (Float) -> Unit,
    btnOffs: Map<String, Offset>,
    onBtnMove: (String, Offset) -> Unit,
    onResetBtns: () -> Unit,
    perGame: Boolean,
    onPerGame: (Boolean) -> Unit,
    hideThumbs: Boolean,
    onToggleThumbs: () -> Unit,
    onResetPos: () -> Unit,
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
        OverlayGamepad(
            engine, Modifier.fillMaxSize().padding(10.dp),
            opacity = opacity, editMode = editPad,
            btnOffs = btnOffs, onBtnMove = onBtnMove,
            dpadScale = dpadScale, abScale = abScale, sysScale = sysScale
        )
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(6.dp)
        ) {
            // Barra personalizzazione tasti + stili.
            CustomizeBar(
                editPad = editPad, onToggleEdit = onToggleEdit,
                hideThumbs = hideThumbs, onToggleThumbs = onToggleThumbs, s = s
            )
            if (editPad) {
                Text(s.editHint, color = Color(0xFFFFD34D), style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.perGame, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Switch(checked = perGame, onCheckedChange = onPerGame)
                }
                TuneRow(s.opacity, opacity, 0.25f..1f, onOpacity)
                TuneRow(s.gameSize, gameScale, 0.6f..1f, onGameScale)
                TuneRow(s.lrSize, lrScale, 0.7f..1.6f, onLrScale)
                TuneRow(s.dpadSize, dpadScale, 0.7f..1.6f, onDpadScale)
                TuneRow(s.abSize, abScale, 0.7f..1.6f, onAbScale)
                TuneRow(s.sysSize, sysScale, 0.7f..1.6f, onSysScale)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onResetBtns) { Text(s.resetBtns) }
                    OutlinedButton(onClick = onResetPos) { Text(s.resetPos) }
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
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DragGroup(btnOffs["l"] ?: Offset.Zero, editPad, { onBtnMove("l", it) }) {
                    PhotoKey("L", opacity, scale = lrScale) { down ->
                        if (down) press(engine, EmulatorEngine.Keys.L)
                        else release(engine, EmulatorEngine.Keys.L)
                    }
                }
                DragGroup(btnOffs["r"] ?: Offset.Zero, editPad, { onBtnMove("r", it) }) {
                    PhotoKey("R", opacity, scale = lrScale) { down ->
                        if (down) press(engine, EmulatorEngine.Keys.R)
                        else release(engine, EmulatorEngine.Keys.R)
                    }
                }
            }
            }
            if (!hideThumbs) SkinThumbs(visibleSkins, skin, onSkin, onHideSkin)
            else TextButton(onClick = onShowAll) { Text(s.showAll) }
        }
    }
}

/** Barra ✎ Tasti + 👁 stili, compatta. */
@Composable
private fun CustomizeBar(editPad: Boolean, onToggleEdit: () -> Unit, hideThumbs: Boolean, onToggleThumbs: () -> Unit, s: S) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onToggleEdit) { Text(if (editPad) "✓ ${s.done}" else s.customize, color = Color(0xFFFFD34D)) }
        TextButton(onClick = onToggleThumbs) { Text(if (hideThumbs) s.showStyles else s.hideStyles, color = Color.White.copy(alpha = 0.8f)) }
    }
}

/** Gruppo trascinabile: in edit mode si sposta col dito, i tasti non sparano. */
@Composable
private fun DragGroup(offset: Offset, editable: Boolean, onDrag: (Offset) -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset((offset.x * density.density).roundToInt(), (offset.y * density.density).roundToInt()) }
            .then(
                if (editable) Modifier
                    .border(1.dp, Color(0xFFFFD34D).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .pointerInput(offset) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                onDrag(offset + Offset(drag.x / density.density, drag.y / density.density))
                            }
                        )
                    }
                else Modifier
            )
            .padding(if (editable) 6.dp else 0.dp)
    ) { content() }
}

/** Gamepad semitrasparente sopra la foto: D-pad + A/B + START/SELECT
 * sempre visibili. In edit mode i 3 gruppi si trascinano dove vuoi
 * e l'opacità si regola dallo slider. */
/** Riga slider con valore live (es. 120%). */
@Composable
private fun TuneRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(140.dp))
        Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.weight(1f))
        Text("${(value * 100).roundToInt()}%", color = Color(0xFFFFD34D), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
    }
}

/** Gamepad: OGNI tasto si trascina da solo col dito (edit mode).
 * Niente più gruppi rigidi: D-pad, A/B, SELECT/START vivono dove li metti. */
@Composable
private fun OverlayGamepad(
    engine: EmulatorEngine, modifier: Modifier = Modifier,
    opacity: Float = 0.55f, editMode: Boolean = false,
    btnOffs: Map<String, Offset> = mapOf(),
    onBtnMove: (String, Offset) -> Unit = { _, _ -> },
    dpadScale: Float = 1f, abScale: Float = 1f, sysScale: Float = 1f
) {
    fun off(k: String) = btnOffs[k] ?: Offset.Zero
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DragGroup(off("up"), editMode, { onBtnMove("up", it) }) {
                OverlayBtn("▲", opacity, enabled = !editMode, scale = dpadScale) { onDirBtn(engine, EmulatorEngine.Keys.UP, it) }
            }
            Row {
                DragGroup(off("left"), editMode, { onBtnMove("left", it) }) {
                    OverlayBtn("◀", opacity, enabled = !editMode, scale = dpadScale) { onDirBtn(engine, EmulatorEngine.Keys.LEFT, it) }
                }
                Spacer(Modifier.size((52 * dpadScale).dp))
                DragGroup(off("right"), editMode, { onBtnMove("right", it) }) {
                    OverlayBtn("▶", opacity, enabled = !editMode, scale = dpadScale) { onDirBtn(engine, EmulatorEngine.Keys.RIGHT, it) }
                }
            }
            DragGroup(off("down"), editMode, { onBtnMove("down", it) }) {
                OverlayBtn("▼", opacity, enabled = !editMode, scale = dpadScale) { onDirBtn(engine, EmulatorEngine.Keys.DOWN, it) }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DragGroup(off("select"), editMode, { onBtnMove("select", it) }) {
                PhotoKey("SELECT", opacity, enabled = !editMode, scale = sysScale) { down ->
                    if (down) press(engine, EmulatorEngine.Keys.SELECT)
                    else release(engine, EmulatorEngine.Keys.SELECT)
                }
            }
            DragGroup(off("start"), editMode, { onBtnMove("start", it) }) {
                PhotoKey("START", opacity, enabled = !editMode, scale = sysScale) { down ->
                    if (down) press(engine, EmulatorEngine.Keys.START)
                    else release(engine, EmulatorEngine.Keys.START)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DragGroup(off("b"), editMode, { onBtnMove("b", it) }) {
                OverlayBtn("B", opacity, enabled = !editMode, scale = abScale) { down ->
                    if (down) press(engine, EmulatorEngine.Keys.B)
                    else release(engine, EmulatorEngine.Keys.B)
                }
            }
            DragGroup(off("a"), editMode, { onBtnMove("a", it) }) {
                OverlayBtn("A", opacity, enabled = !editMode, scale = abScale) { down ->
                    if (down) press(engine, EmulatorEngine.Keys.A)
                    else release(engine, EmulatorEngine.Keys.A)
                }
            }
        }
    }
}

private fun onDirBtn(e: EmulatorEngine, m: Int, down: Boolean) { if (down) press(e, m) else release(e, m) }

@Composable
private fun OverlayDPad(opacity: Float, enabled: Boolean, scale: Float = 1f, onDir: (Int, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OverlayBtn("▲", opacity, enabled, scale) { onDir(EmulatorEngine.Keys.UP, it) }
        Row {
            OverlayBtn("◀", opacity, enabled, scale) { onDir(EmulatorEngine.Keys.LEFT, it) }
            Spacer(Modifier.size((52 * scale).dp))
            OverlayBtn("▶", opacity, enabled, scale) { onDir(EmulatorEngine.Keys.RIGHT, it) }
        }
        OverlayBtn("▼", opacity, enabled, scale) { onDir(EmulatorEngine.Keys.DOWN, it) }
    }
}

@Composable
private fun OverlayBtn(label: String, opacity: Float = 0.55f, enabled: Boolean = true, scale: Float = 1f, onPress: (Boolean) -> Unit) {
    Box(
        Modifier.size((52 * scale).dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = opacity))
            .border(2.dp, Color.White.copy(alpha = 0.45f * opacity + 0.1f), CircleShape)
            .pointerInput(label, enabled) {
                if (!enabled) return@pointerInput
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
        Text(label, color = Color.White.copy(alpha = 0.4f + 0.6f * opacity), fontWeight = FontWeight.Bold, fontSize = (16 * scale).sp)
    }
}

/** Pannello macro completo: save/load sullo slot scelto, rewind, speed, reset. */
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

/** Menu a tendina sfondi/colori + nascondi singolo stile. */
@Composable
private fun SkinDropdown(
    skins: List<GbaSkin>,
    current: GbaSkin,
    onSkin: (GbaSkin) -> Unit,
    onHide: (String) -> Unit = {},
    onShowAll: () -> Unit = {},
    showAllVisible: Boolean = false
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
                    trailingIcon = {
                        if (sk.id != current.id) {
                            TextButton(onClick = { onHide(sk.id) }) { Text("👁") }
                        }
                    },
                    onClick = { onSkin(sk); expanded = false }
                )
            }
            if (showAllVisible) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("↺ all") },
                    onClick = { onShowAll() }
                )
            }
        }
    }
}

/** Tasto semitrasparente sopra la foto: opacità + dimensione regolabili. */
@Composable
private fun PhotoKey(label: String, opacity: Float = 0.55f, enabled: Boolean = true, scale: Float = 1f, onPress: (Boolean) -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = opacity))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .pointerInput(label, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    onPress(true)
                    do {
                        val ev = awaitPointerEvent()
                    } while (ev.changes.any { it.pressed })
                    onPress(false)
                }
            }
            .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.4f + 0.6f * opacity),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            fontSize = (13 * scale).sp
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
private fun SkinThumbs(skins: List<GbaSkin>, current: GbaSkin, onSkin: (GbaSkin) -> Unit, onHide: (String) -> Unit = {}) {
    LazyRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(skins, key = { it.id }) { sk ->
            val sel = sk.id == current.id
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                if (!sel) {
                    TextButton(onClick = { onHide(sk.id) }, contentPadding = PaddingValues(0.dp)) {
                        Text("👁", style = MaterialTheme.typography.labelSmall)
                    }
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
    gameScale: Float = 0.86f,
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
                .fillMaxWidth(gameScale.coerceIn(0.6f, 1f))
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
    onToggleEdit: () -> Unit,
    editPad: Boolean,
    hideThumbs: Boolean,
    onToggleThumbs: () -> Unit,
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
            if (!hideThumbs) SkinThumbs(skins, skin, onSkin)
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

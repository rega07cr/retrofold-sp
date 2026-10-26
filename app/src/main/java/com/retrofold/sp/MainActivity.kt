package com.retrofold.sp

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowInfoTracker
import com.retrofold.sp.core.EmulatorEngine
import com.retrofold.sp.core.RomEntry
import com.retrofold.sp.core.RomManager
import com.retrofold.sp.device.DeviceAdapter
import com.retrofold.sp.ui.AppLang
import com.retrofold.sp.ui.BootSplash
import com.retrofold.sp.ui.CoverPocketScreen
import com.retrofold.sp.ui.FlexSpScreen
import com.retrofold.sp.ui.FlipPetOverlay
import com.retrofold.sp.ui.GameBootScreen
import com.retrofold.sp.ui.LanguageScreen
import com.retrofold.sp.ui.PixelCard
import com.retrofold.sp.ui.PixelFont
import com.retrofold.sp.ui.RetroButton
import com.retrofold.sp.ui.RetroFoldColors
import com.retrofold.sp.ui.S
import com.retrofold.sp.ui.Skins
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val engine = EmulatorEngine()
    private var roms by mutableStateOf(emptyList<RomEntry>())
    private var currentRom by mutableStateOf<RomEntry?>(null)
    private var skinId by mutableStateOf("gold")
    private var ags101 by mutableStateOf(true)
    private var isFlex by mutableStateOf(false)
    private var booted by mutableStateOf(false)
    private var lang by mutableStateOf<AppLang?>(null)
    /** Gioco in fase di boot-animazione (rainbow). Null = mostra l'emulatore. */
    private var bootRom by mutableStateOf<RomEntry?>(null)

    // Tasti fisici del volume come pulsanti d'azione in Cover Mode.
    // BUGFIX documentato: prima erano solo scritti nei testi ma mai intercettati.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val mask = actionMaskFor(keyCode)
        if (mask != null && currentRom != null && !isFlex && bootRom == null) {
            try { engine.nativePress(mask) } catch (_: Throwable) {}
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val mask = actionMaskFor(keyCode)
        if (mask != null && currentRom != null && !isFlex && bootRom == null) {
            try { engine.nativeRelease(mask) } catch (_: Throwable) {}
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun actionMaskFor(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> EmulatorEngine.Keys.A
        KeyEvent.KEYCODE_VOLUME_DOWN -> EmulatorEngine.Keys.B
        else -> null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshRoms()

        // Lingua salvata (null = prima apertura -> schermata scelta)
        lifecycleScope.launch {
            val code = application.dataStore.data
                .map { it[RetroFoldApp.LANG_KEY] }
                .first()
            lang = AppLang.fromCode(code)
        }

        // Rileva piega a 90° in tempo reale (universale, tutti i Flip)
        lifecycleScope.launch {
            WindowInfoTracker.getOrCreate(this@MainActivity)
                .windowLayoutInfo(this@MainActivity)
                .collect { info ->
                    val folding = info.displayFeatures
                        .filterIsInstance<androidx.window.layout.FoldingFeature>()
                        .firstOrNull()
                    isFlex = DeviceAdapter.isFlexMode(folding)
                }
        }

        setContent {
            MaterialTheme(colorScheme = RetroFoldColors()) {
                val snackbar = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // 1) Animazione retrò di avvio (Game Boy che si apre)
                if (!booted) {
                    BootSplash(s = lang?.let { S.of(it) } ?: S.it) {
                        booted = true
                    }
                    return@MaterialTheme
                }
                // 2) Scelta lingua al primo avvio
                val l = lang
                if (l == null) {
                    LanguageScreen(onPick = { picked ->
                        lang = picked
                        lifecycleScope.launch {
                            application.dataStore.edit {
                                it[RetroFoldApp.LANG_KEY] = picked.code
                            }
                        }
                    })
                    return@MaterialTheme
                }
                val s = S.of(l)

                val pickRom = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) importUri(uri, s,
                        onError = { msg ->
                            scope.launch { snackbar.showSnackbar(msg) }
                        })
                }

                val profile = remember {
                    DeviceAdapter.profileFor(Build.MANUFACTURER, Build.MODEL)
                }

                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    val br = bootRom
                    if (br != null) {
                        // Animazione rainbow all'avvio di ogni gioco
                        GameBootScreen(romTitle = br.title, s = s) {
                            if (bootRom?.id == br.id) bootRom = null
                        }
                    } else if (currentRom == null) {
                        LibraryScreen(
                            s = s,
                            roms = roms,
                            deviceLabel = "${profile.deviceLabel} • ${Build.MANUFACTURER} ${Build.MODEL}",
                            onAdd = { pickRom.launch(arrayOf("*/*")) },
                            onPlay = { currentRom = it; bootRom = it }
                        )
                    } else if (isFlex) {
                        // MODALITA' FLEX a L: GBA SP Reborn (schermo sopra + gamepad sotto)
                        FlexSpScreen(
                            rom = currentRom!!,
                            engine = engine,
                            skin = Skins.byId(skinId),
                            skins = Skins.ALL,
                            ags101 = ags101,
                            onSkin = { skinId = it.id },
                            onToggleFilter = {
                                ags101 = !ags101
                                try { engine.nativeSetFilterAGS101(ags101) } catch (_: Throwable) {}
                            },
                            onBack = { currentRom = null; bootRom = null },
                            onSave = { slot -> saveSlot(slot) },
                            onLoad = { slot -> loadSlot(slot) },
                            s = s
                        )
                    } else {
                        // Telefono aperto piatto o chiuso: cover pocket + pet
                        CoverPocketScreen(
                            rom = currentRom,
                            profile = profile,
                            onBack = { currentRom = null; bootRom = null },
                            onOpenFlexHint = {},
                            s = s
                        )
                    }
                    FlipPetOverlay(flexOpen = isFlex)
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    private fun refreshRoms() {
        lifecycleScope.launch {
            roms = RomManager.listRoms(this@MainActivity)
        }
    }

    private fun importUri(uri: Uri, s: S, onError: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* alcuni provider non lo supportano */ }
            val res = RomManager.importRom(this@MainActivity, uri)
            res.onSuccess { refreshRoms(); currentRom = it; bootRom = it }
            res.onFailure { e -> onError(S.errorText(s, e.message)) }
        }
    }

    private fun saveSlot(slot: Int) {
        val rom = currentRom ?: return
        lifecycleScope.launch {
            val f = RomManager.stateFile(this@MainActivity, rom.id, slot)
            try { engine.nativeSaveState(f.absolutePath) } catch (_: Throwable) {}
        }
    }

    private fun loadSlot(slot: Int) {
        val rom = currentRom ?: return
        lifecycleScope.launch {
            val f = RomManager.stateFile(this@MainActivity, rom.id, slot)
            if (f.exists()) try { engine.nativeLoadState(f.absolutePath) } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun LibraryScreen(
    s: S,
    roms: List<RomEntry>,
    deviceLabel: String,
    onAdd: () -> Unit,
    onPlay: (RomEntry) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🎮 RetroFold SP",
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            deviceLabel,
            fontFamily = PixelFont,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9AA0AA)
        )
        Spacer(Modifier.height(12.dp))
        PixelCard(Modifier.fillMaxWidth()) {
            Text(
                s.libHow,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD34D)
            )
            Spacer(Modifier.height(6.dp))
            Text(s.step1, fontFamily = PixelFont, style = MaterialTheme.typography.bodyMedium)
            Text(s.step2, fontFamily = PixelFont, style = MaterialTheme.typography.bodyMedium)
            Text(s.step3, fontFamily = PixelFont, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        RetroButton(
            text = s.addRom,
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            s.addRomHint,
            fontFamily = PixelFont,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9AA0AA)
        )
        Spacer(Modifier.height(12.dp))
        if (roms.isEmpty()) {
            Text(
                s.emptyLib,
                fontFamily = PixelFont,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        } else {
            Text(
                s.romsTitle,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.fillMaxWidth()) {
                items(roms) { rom ->
                    PixelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Text(
                            "▶ ${rom.title}",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${rom.system} • ${rom.sizeBytes / 1024} KB",
                            fontFamily = PixelFont,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9AA0AA)
                        )
                        Spacer(Modifier.height(8.dp))
                        RetroButton(
                            text = "▶",
                            onClick = { onPlay(rom) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

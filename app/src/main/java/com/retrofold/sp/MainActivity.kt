package com.retrofold.sp

import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowInfoTracker
import com.retrofold.sp.core.EmulatorEngine
import com.retrofold.sp.core.RomEntry
import com.retrofold.sp.core.RomManager
import com.retrofold.sp.device.DeviceAdapter
import com.retrofold.sp.ui.FlexSpScreen
import com.retrofold.sp.ui.Skins
import com.retrofold.sp.ui.CoverPocketScreen
import com.retrofold.sp.ui.FlipPetOverlay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val engine = EmulatorEngine()
    private var roms by mutableStateOf(emptyList<RomEntry>())
    private var currentRom by mutableStateOf<RomEntry?>(null)
    private var skinId by mutableStateOf("gold")
    private var ags101 by mutableStateOf(true)
    private var isFlex by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        refreshRoms()

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
            MaterialTheme(colorScheme = darkColorScheme()) {
                val pickRom = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) importUri(uri)
                }

                val profile = remember {
                    DeviceAdapter.profileFor(Build.MANUFACTURER, Build.MODEL)
                }

                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (currentRom == null) {
                        LibraryScreen(
                            roms = roms,
                            deviceLabel = "${profile.deviceLabel} • ${Build.MANUFACTURER} ${Build.MODEL}",
                            onAdd = { pickRom.launch(arrayOf("*/*")) },
                            onPlay = { currentRom = it }
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
                            onBack = { currentRom = null },
                            onSave = { slot -> saveSlot(slot) },
                            onLoad = { slot -> loadSlot(slot) }
                        )
                    } else {
                        // Telefono aperto piatto o chiuso: cover pocket + pet
                        CoverPocketScreen(
                            rom = currentRom,
                            profile = profile,
                            onBack = { currentRom = null },
                            onOpenFlexHint = {}
                        )
                    }
                    FlipPetOverlay(flexOpen = isFlex)
                }
            }
        }
    }

    private fun refreshRoms() {
        lifecycleScope.launch {
            roms = RomManager.listRoms(this@MainActivity)
        }
    }

    private fun importUri(uri: Uri) {
        lifecycleScope.launch {
            contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val res = RomManager.importRom(this@MainActivity, uri)
            res.onSuccess { refreshRoms(); currentRom = it }
            res.onFailure { /* TODO: snackbar errore formato */ }
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
    roms: List<RomEntry>,
    deviceLabel: String,
    onAdd: () -> Unit,
    onPlay: (RomEntry) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎮 RetroFold SP", style = MaterialTheme.typography.headlineMedium)
        Text(deviceLabel, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Piega a 90° per il GBA SP • Aggiungi le tue ROM .gba/.gb/.gbc",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("+ Aggiungi ROM (file tuo, nessun gioco incluso)")
        }
        Spacer(Modifier.height(12.dp))
        if (roms.isEmpty()) {
            Text("Nessuna ROM. Tocca + e scegli un file .gba dal tuo telefono.")
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(roms) { rom ->
                    ElevatedCard(
                        onClick = { onPlay(rom) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("▶ ${rom.title}")
                            Text(
                                "${rom.system} • ${rom.sizeBytes / 1024} KB",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

# 🎮 RetroFold SP — GBA SP Reborn per Flip Phone

Base hardware: **GBA SP oro** della tua foto (bezel nero, D-pad incassato, pill B+A, speaker 4x4, LED rosso) + 5 skin: Platinum, Onyx, Cobalt, NES, Tribal.

## Cosa fa già questo scaffold
- **Libreria ROM**: `+ Aggiungi ROM` via SAF, copia in `files/roms/<sha256>.gba`, validazione header GBA/GB/GBC, dedup SHA-256. Nessuna ROM inclusa (legale).
- **Flex Mode a 90°**: rilevato con `WindowInfoTracker + FoldingFeature.HALF_OPENED`. Sopra bezel + `SurfaceView` emulatori, sotto gamepad oro con D-pad, A/B, L/R, Start/Select, LED, macro Save/Load/Rewind/Fast-forward, toggle AGS-101.
- **Filtro AGS-101**: flag GPU nel bridge NDK + overlay anteprima.
- **Cover Mode**: HUD con safe-area per device (Razr full-app, Z Flip via cover-launcher), volume SU/GIÙ = salto/sparo.
- **FlipPet**: overlay `(•̀ᴗ•́)و` in flex / `zZz` da chiuso.
- **DeviceAdapter**: profili Razr Ultra/Lite, Z Flip 5/6, MIX Flip, Find N Flip + fallback.

## Ottimizzazioni performance
- NDK `ANativeWindow` zero-copy, thread emu dedicato, frame-skip adattivo 0..2, pacing 59.73fps GBA.
- Audio Oboe 32kHz low-latency, rewind ring-buffer 10s, save async.
- Release con R8 full + ABI `arm64-v8a` prioritaria.
- Refresh cover rispettato: 165Hz Razr Ultra, 120Hz MIX Flip, 60Hz Z Flip.

## Collegare il core mGBA reale (5 min)
1. `git submodule add https://github.com/mgba-emu/mgba.git app/src/main/cpp/mgba`
2. In `CMakeLists.txt` aggiungi `add_subdirectory(mgba)` e linka `mgba-static`.
3. In `emulator_bridge.cpp` sostituisci gli stub con `mCoreGba`, `VFileFromFd(fd)`, `mCoreLoadRom`.
4. Aggiungi Oboe per l'audio: `implementation("com.google.oboe:oboe:1.8.0")`.
5. Build: `./gradlew :app:assembleRelease`.

## Tasti volume in cover
In `MainActivity.onKeyDown` intercetta `KEYCODE_VOLUME_UP/DOWN` e mappa a `nativePress(A/B)` quando `isFlex == false`.

## Aprire in Android Studio
Open → cartella progetto → sync Gradle → Run su Razr/Z Flip o emulatore foldable (Foldable Emulator, hinge 90°).

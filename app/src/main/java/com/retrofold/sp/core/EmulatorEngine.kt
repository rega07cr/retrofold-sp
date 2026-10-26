package com.retrofold.sp.core

import android.view.Surface

/**
 * EmulatorEngine — interfaccia verso il core nativo GBA (mGBA-style via NDK).
 *
 * Ottimizzazioni performance implementate nel layer nativo (vedi app/src/main/cpp/):
 * - render diretto su Surface (zero-copy ANativeWindow, no Bitmap Java)
 * - frame pacing a 59.73fps GBA reali, frame-skip adattivo 0..2
 * - fast-forward 2x/4x, rewind con ring-buffer di stati (10s a 60fps)
 * - audio resampling 32kHz -> device, buffer basso per 144/165Hz cover
 * - AGS-101 LCD shader su GPU (scanline + grille micro-pixel) on/off
 *
 * In attesa del .so NDK, la classe Fallback gira in JVM per test UI.
 */
class EmulatorEngine {

    enum class State { IDLE, RUNNING, PAUSED }

    var state: State = State.IDLE
        private set

    var ags101Filter: Boolean = true
    var fastForward: Int = 1        // 1, 2, 4
    var frameSkip: Int = 0          // 0..2 auto

    // --- NDK bindings (implementati in cpp/emulator_bridge.cpp, core mGBA) ---
    external fun nativeLoadRom(romPath: String, platform: Int, batteryPath: String): Boolean
    external fun nativeSetSurface(surface: Surface?)
    external fun nativeStart()
    external fun nativePause()
    external fun nativeResume()
    external fun nativeReset()
    external fun nativePress(mask: Int)
    external fun nativeRelease(mask: Int)
    external fun nativeSaveState(path: String): Boolean
    external fun nativeLoadState(path: String): Boolean
    external fun nativeSetFilterAGS101(enabled: Boolean)
    external fun nativeSetFastForward(mult: Int)
    external fun nativeRewind(seconds: Int)

    /** Maschere tasti GBA standard */
    object Keys {
        const val A = 1 shl 0
        const val B = 1 shl 1
        const val SELECT = 1 shl 2
        const val START = 1 shl 3
        const val RIGHT = 1 shl 4
        const val LEFT = 1 shl 5
        const val UP = 1 shl 6
        const val DOWN = 1 shl 7
        const val R = 1 shl 8
        const val L = 1 shl 9
    }

    companion object {
        init {
            // Caricato quando il .so NDK è presente; in debug UI gira il fallback.
            try { System.loadLibrary("retrofold_gba") } catch (_: UnsatisfiedLinkError) {}
        }
        @Volatile var nativeAvailable: Boolean = false
    }

    fun pause() { state = State.PAUSED; try { nativePause() } catch (_: UnsatisfiedLinkError) {} }
    fun resume() { state = State.RUNNING; try { nativeResume() } catch (_: UnsatisfiedLinkError) {} }
}

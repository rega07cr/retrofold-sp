package com.retrofold.sp.device

import androidx.window.layout.FoldingFeature

/**
 * DeviceAdapter — un profilo per ogni Flip sul mercato.
 * L'app rileva il modello via Build.MANUFACTURER/MODEL e applica
 * risoluzione cover, safe-area (punch-hole / camere), refresh e haptics.
 *
 * Dati display esterni (cover):
 * - Razr 40 Ultra / 50 Ultra: 3.6" pOLED 1056x1066, 165/144Hz, full app OK
 * - Z Flip 5 / 6: 3.4" FlexWindow 720x748, widget/cover-launcher, 60Hz
 * - Xiaomi MIX Flip: ~4.0" 1392x1208, full app
 * - OPPO Find N Flip / N3 Flip: 3.26" verticale 720x382, widget
 */
data class CoverProfile(
    val deviceLabel: String,
    val coverW: Int,
    val coverH: Int,
    val maxHz: Int,
    val fullAppOnCover: Boolean,
    val safeInsetTopDp: Int,
    val safeInsetCamDp: Int,
    val hapticAmplitude: Int // 1..255 profilo motore lineare
)

object DeviceAdapter {

    val RAZR_ULTRA = CoverProfile("Motorola Razr Ultra", 1056, 1066, 165, true, 0, 56, 200)
    val RAZR_LITE = CoverProfile("Motorola Razr", 1056, 1066, 144, true, 0, 56, 180)
    val Z_FLIP = CoverProfile("Galaxy Z Flip 5/6", 720, 748, 60, false, 24, 48, 170)
    val MIX_FLIP = CoverProfile("Xiaomi MIX Flip", 1392, 1208, 120, true, 0, 40, 190)
    val FIND_N_FLIP = CoverProfile("OPPO Find N Flip", 720, 382, 60, false, 16, 44, 160)
    val FALLBACK = CoverProfile("Foldable generico", 720, 720, 60, true, 16, 40, 150)

    fun profileFor(manufacturer: String, model: String): CoverProfile {
        val m = "$manufacturer $model".lowercase()
        return when {
            "razr" in m && ("ultra" in m || "40" in m || "50" in m) -> RAZR_ULTRA
            "razr" in m -> RAZR_LITE
            "z flip" in m || "sm-f73" in m || "sm-f74" in m -> Z_FLIP
            "mix flip" in m || "2311bpn" in m -> MIX_FLIP
            "find n" in m || "flip" in m && ("oppo" in m || "oneplus" in m) -> FIND_N_FLIP
            else -> FALLBACK
        }
    }

    /** true se telefono piegato a L (half-opened, hinge orizzontale) = modalità GBA SP */
    fun isFlexMode(feature: FoldingFeature?): Boolean {
        if (feature == null) return false
        return feature.state == FoldingFeature.State.HALF_OPENED &&
            feature.orientation == FoldingFeature.Orientation.HORIZONTAL
    }

    /** true se completamente chiuso = usa cover display */
    fun isClosed(feature: FoldingFeature?): Boolean {
        // Su molti Flip la cover non riporta FoldingFeature: si usa anche screen off / smallest width.
        return feature != null && feature.state == FoldingFeature.State.FLAT && feature.isSeparating.not()
    }
}

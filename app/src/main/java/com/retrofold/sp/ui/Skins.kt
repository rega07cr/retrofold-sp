package com.retrofold.sp.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.retrofold.sp.R

/**
 * Skin guscio GBA SP.
 * - `photoRes`: foto originale dell'utente (res/drawable-nodpi) usata come
 *   sfondo fedele al 100% in Flex Mode. Null = guscio procedurale.
 * - Le frazioni sotto mappano schermo e tasti sulla foto (0..1).
 * - La vecchia `assetFile` resta solo come riferimento storico.
 */
data class GbaSkin(
    val id: String,
    val label: String,
    val shellTop: Color,
    val shellBottom: Color,
    val shellEdge: Color,
    val bezel: Color,
    val screenTint: Color,
    val screenText: String = "▶ START GAME",
    val logoColor: Color,
    val dpad: Color,
    val buttons: Color,
    val buttonText: Color = Color.White,
    val ledOn: Color,
    val translucent: Boolean = false,
    val specialBadge: String? = null,
    val assetFile: String? = null,
    // ── Foto originale ──
    @DrawableRes val photoRes: Int? = null,
    /** Porzione verticale della foto mostrata nella metà sopra (0..1 della foto). */
    val photoTopEnd: Float = 0.52f,
    /** Emulatore sovrapposto: frazioni della metà sopra (0..1). */
    val emuTop: Float = 0.18f,
    val emuBottom: Float = 0.80f,
    val emuMargin: Float = 0.10f,
    /** Zone touch invisibili: frazioni della metà sotto (0..1).
     * BUGFIX: le foto non sono calibrate una per una, quindi le zone sono
     * VOLUTAMENTE larghe per beccare i tasti veri anche se la foto è
     * spostata di qualche mm rispetto alle frazioni generiche. */
    val dpadCx: Float = 0.22f,
    val dpadCy: Float = 0.34f,
    val dpadSize: Float = 0.38f,
    val abCx: Float = 0.75f,
    val abCy: Float = 0.32f,
    val abR: Float = 0.115f
)

object Skins {
    val GOLD_ZELDA = GbaSkin(
        id = "gold", label = "Gold Edition (foto)",
        shellTop = Color(0xFFD9A441), shellBottom = Color(0xFF9C6B1A),
        shellEdge = Color(0xFF6E4A0E), bezel = Color(0xFF111111),
        screenTint = Color(0xFFB9B4D6), logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp.jpg",
        photoRes = R.drawable.skin_gold
    )
    val PLATINUM = GbaSkin(
        id = "platinum", label = "Platinum Silver",
        shellTop = Color(0xFFD7D9DD), shellBottom = Color(0xFF9EA2A8),
        shellEdge = Color(0xFF6B6F76), bezel = Color(0xFF1A1A1C),
        screenTint = Color(0xFFB9B4D6), logoColor = Color.White,
        dpad = Color(0xFF2E2E32), buttons = Color(0xFF2E2E32),
        ledOn = Color(0xFFFF2D2D)
    )
    val ONYX = GbaSkin(
        id = "onyx", label = "Onyx Black",
        shellTop = Color(0xFF3A3D42), shellBottom = Color(0xFF151619),
        shellEdge = Color(0xFF000000), bezel = Color(0xFF0B0B0C),
        screenTint = Color(0xFFAAA6C8), logoColor = Color(0xFFE8E8E8),
        dpad = Color(0xFF1C1C1F), buttons = Color(0xFF1C1C1F),
        ledOn = Color(0xFFFF2D2D)
    )
    val COBALT = GbaSkin(
        id = "cobalt", label = "Cobalt Blue",
        shellTop = Color(0xFF2E63D8), shellBottom = Color(0xFF16307A),
        shellEdge = Color(0xFF0C1C4E), bezel = Color(0xFF101114),
        screenTint = Color(0xFFB9B4D6), logoColor = Color.White,
        dpad = Color(0xFF232428), buttons = Color(0xFF232428),
        ledOn = Color(0xFFFF2D2D)
    )
    val NES = GbaSkin(
        id = "nes", label = "Classic NES Edition",
        shellTop = Color(0xFFD8D5CE), shellBottom = Color(0xFFA8A49A),
        shellEdge = Color(0xFF4A4A4A), bezel = Color(0xFF1E1E1E),
        screenTint = Color(0xFFB9B4D6), logoColor = Color(0xFFD22B2B),
        dpad = Color(0xFF1A1A1A), buttons = Color(0xFFB32020),
        ledOn = Color(0xFFFF2D2D)
    )
    val TRIBAL = GbaSkin(
        id = "tribal", label = "Tribal Edition",
        shellTop = Color(0xFF8F959C), shellBottom = Color(0xFF4E545B),
        shellEdge = Color(0xFF2A2E33), bezel = Color(0xFF111214),
        screenTint = Color(0xFFB9B4D6), logoColor = Color(0xFFFFD34D),
        dpad = Color(0xFF26262A), buttons = Color(0xFF26262A),
        ledOn = Color(0xFFFF2D2D)
    )

    val POKEMON_AQUA = GbaSkin(
        id = "pokemon_aqua", label = "Pokémon Aqua (foto)",
        shellTop = Color(0xFF9BDCED), shellBottom = Color(0xFF4FA8C9),
        shellEdge = Color(0xFF2B7A96), bezel = Color(0xFF111111),
        screenTint = Color.White, logoColor = Color.White,
        dpad = Color(0xFF2547D0), buttons = Color(0xFF2547D0),
        ledOn = Color(0xFFFF2D2D), specialBadge = "POKEMON",
        assetFile = "skins/gbasp1.jpg",
        photoRes = R.drawable.skin_pokemon
    )
    val GRAPHITE_RED = GbaSkin(
        id = "graphite_red", label = "Graphite Red (foto)",
        shellTop = Color(0xFFC9CCD1), shellBottom = Color(0xFF8E9299),
        shellEdge = Color(0xFF3A3D42), bezel = Color(0xFF111111),
        screenTint = Color.White, logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFFD21F26),
        buttonText = Color.White, ledOn = Color(0xFF2DFF5C),
        specialBadge = "NINTENDO_RED", assetFile = "skins/gbasp2.jpg",
        photoRes = R.drawable.skin_graphite
    )
    val MATTE_BLACK = GbaSkin(
        id = "matte_black", label = "Matte Black (foto)",
        shellTop = Color(0xFF232326), shellBottom = Color(0xFF0C0C0E),
        shellEdge = Color.Black, bezel = Color(0xFF0B0B0C),
        screenTint = Color.White, logoColor = Color(0xFFE8E8E8),
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp3.jpg",
        photoRes = R.drawable.skin_matte
    )
    val FLAME_RED = GbaSkin(
        id = "flame_red", label = "Flame Red (foto)",
        shellTop = Color(0xFFFF3B3B), shellBottom = Color(0xFFB01212),
        shellEdge = Color(0xFF6E0A0A), bezel = Color(0xFF111111),
        screenTint = Color(0xFFFFF1F1), logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp4.jpg",
        photoRes = R.drawable.skin_flame
    )
    val GOLD_TRIFORCE = GbaSkin(
        id = "gold_triforce", label = "Gold Zelda Triforce (foto)",
        shellTop = Color(0xFFE0B252), shellBottom = Color(0xFFA2761F),
        shellEdge = Color(0xFF6E4A0E), bezel = Color(0xFF111111),
        screenTint = Color(0xFF9FD08A), screenText = "PRESS START",
        logoColor = Color.White,
        dpad = Color(0xFF1A1A1A), buttons = Color(0xFF1A1A1A),
        ledOn = Color(0xFF2DFF5C), specialBadge = "TRIFORCE",
        assetFile = "skins/gbasp4.png",
        photoRes = R.drawable.skin_gold_triforce
    )
    val CRYSTAL_MOD = GbaSkin(
        id = "crystal_mod", label = "Crystal Mod (foto)",
        shellTop = Color(0xFFBFE8C9), shellBottom = Color(0xFF6FA98A),
        shellEdge = Color(0xFF3E6B58), bezel = Color(0xFF111111),
        screenTint = Color.White, screenText = "PLAY Mods",
        logoColor = Color(0xFF00C2C2),
        dpad = Color(0xFF1A1A1A), buttons = Color(0xFF1A1A1A),
        ledOn = Color(0xFFFF2D2D), translucent = true,
        specialBadge = "CRYSTAL", assetFile = "skins/gbasp5.jpg",
        photoRes = R.drawable.skin_crystal
    )
    /** Guscio scuro anti-distrazione: niente foto, niente riflessi,
     * solo schermo + tasti. Per giocare senza che lo sfondo disturbi. */
    val MIDNIGHT = GbaSkin(
        id = "midnight", label = "Mezzanotte relax",
        shellTop = Color(0xFF232838), shellBottom = Color(0xFF0B0D14),
        shellEdge = Color(0xFF000000), bezel = Color(0xFF050506),
        screenTint = Color(0xFF101018), screenText = "▶ START GAME",
        logoColor = Color(0xFF8E96AC),
        dpad = Color(0xFF2E3444), buttons = Color(0xFF2E3444),
        buttonText = Color(0xFFDDE3F0),
        ledOn = Color(0xFFFF2D2D)
    )
    val MINIMAL_MONO = GbaSkin(
        id = "minimal_mono", label = "Minimal Mono (foto)",
        shellTop = Color.White, shellBottom = Color(0xFFD9D9D9),
        shellEdge = Color(0xFF111111), bezel = Color.Black,
        screenTint = Color.Black, screenText = "► START GAME",
        logoColor = Color.White,
        dpad = Color.Black, buttons = Color.Black, buttonText = Color.White,
        ledOn = Color(0xFFFF2D2D), specialBadge = "MINIMAL",
        assetFile = "skins/gbasp7.jpg",
        photoRes = R.drawable.skin_minimal
    )

    val ALL = listOf(
        GOLD_ZELDA, GOLD_TRIFORCE, POKEMON_AQUA, FLAME_RED,
        MATTE_BLACK, GRAPHITE_RED, CRYSTAL_MOD, MINIMAL_MONO,
        MIDNIGHT, PLATINUM, ONYX, COBALT, NES, TRIBAL
    )
    fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: GOLD_ZELDA
}

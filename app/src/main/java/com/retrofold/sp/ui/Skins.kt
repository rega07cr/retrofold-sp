package com.retrofold.sp.ui

import androidx.compose.ui.graphics.Color

/**
 * Skin guscio GBA SP.
 * - Le 8 grafiche utente vivono in app/src/main/assets/skins/ (vedi docs/GRAPHICS.md).
 * - Se l'asset manca, fallback al gradiente procedurale (stessi colori) -> compila sempre.
 * - Base hardware default = gbasp.jpg oro.
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
    val specialBadge: String? = null, // es. "POKEMON", "TRIFORCE", "NINTENDO_RED", "CRYSTAL", "MINIMAL"
    val assetFile: String? = null     // es. "skins/gbasp1.jpg"
)

object Skins {
    // ── Base oro tua foto (gbasp.jpg) ──
    val GOLD_ZELDA = GbaSkin(
        id = "gold", label = "Gold Edition (base)",
        shellTop = Color(0xFFD9A441), shellBottom = Color(0xFF9C6B1A),
        shellEdge = Color(0xFF6E4A0E), bezel = Color(0xFF111111),
        screenTint = Color(0xFFB9B4D6), logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp.jpg"
    )
    // ── Storiche ──
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

    // ── Tue 8 grafiche ──
    // gbasp1.jpg: azzurro Pokémon, D-pad/tasti blu royal, Squirtle, logo Pokémon, Pokéball su start/select
    val POKEMON_AQUA = GbaSkin(
        id = "pokemon_aqua", label = "Pokémon Aqua (Squirtle)",
        shellTop = Color(0xFF9BDCED), shellBottom = Color(0xFF4FA8C9),
        shellEdge = Color(0xFF2B7A96), bezel = Color(0xFF111111),
        screenTint = Color.White, logoColor = Color.White,
        dpad = Color(0xFF2547D0), buttons = Color(0xFF2547D0),
        ledOn = Color(0xFFFF2D2D), specialBadge = "POKEMON",
        assetFile = "skins/gbasp1.jpg"
    )
    // gbasp2.jpg: grigio + faceplate nero, tasti B/A rossi, scritte Nintendo rosse, slat volume, LED verde
    val GRAPHITE_RED = GbaSkin(
        id = "graphite_red", label = "Graphite Red Nintendo",
        shellTop = Color(0xFFC9CCD1), shellBottom = Color(0xFF8E9299),
        shellEdge = Color(0xFF3A3D42), bezel = Color(0xFF111111),
        screenTint = Color.White, logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFFD21F26),
        buttonText = Color.White, ledOn = Color(0xFF2DFF5C),
        specialBadge = "NINTENDO_RED", assetFile = "skins/gbasp2.jpg"
    )
    // gbasp3.jpg: nero opaco, tasti dark grey, capsula glossy, LED rosso
    val MATTE_BLACK = GbaSkin(
        id = "matte_black", label = "Matte Black",
        shellTop = Color(0xFF232326), shellBottom = Color(0xFF0C0C0E),
        shellEdge = Color.Black, bezel = Color(0xFF0B0B0C),
        screenTint = Color.White, logoColor = Color(0xFFE8E8E8),
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp3.jpg"
    )
    // gbasp4.jpg: rosso vivo #ff3333, D-pad/A/B dark grey, start/select ovali
    val FLAME_RED = GbaSkin(
        id = "flame_red", label = "Flame Red",
        shellTop = Color(0xFFFF3B3B), shellBottom = Color(0xFFB01212),
        shellEdge = Color(0xFF6E0A0A), bezel = Color(0xFF111111),
        screenTint = Color(0xFFFFF1F1), logoColor = Color.White,
        dpad = Color(0xFF3A3A3E), buttons = Color(0xFF3A3A3E),
        ledOn = Color(0xFFFF2D2D), assetFile = "skins/gbasp4.jpg"
    )
    // gbasp4.png: oro Zelda, Triforce, LED verde, SELECT/START etichettati, scena foresta PRESS START
    val GOLD_TRIFORCE = GbaSkin(
        id = "gold_triforce", label = "Gold Zelda Triforce",
        shellTop = Color(0xFFE0B252), shellBottom = Color(0xFFA2761F),
        shellEdge = Color(0xFF6E4A0E), bezel = Color(0xFF111111),
        screenTint = Color(0xFF9FD08A), screenText = "PRESS START",
        logoColor = Color.White,
        dpad = Color(0xFF1A1A1A), buttons = Color(0xFF1A1A1A),
        ledOn = Color(0xFF2DFF5C), specialBadge = "TRIFORCE",
        assetFile = "skins/gbasp4.png"
    )
    // gbasp5.jpg: custom trasparente verde + PLAY Mods Device Designer CR-CUSTOMS, membrana cyan
    val CRYSTAL_MOD = GbaSkin(
        id = "crystal_mod", label = "Crystal Mod (trasparente)",
        shellTop = Color(0xFFBFE8C9), shellBottom = Color(0xFF6FA98A),
        shellEdge = Color(0xFF3E6B58), bezel = Color(0xFF111111),
        screenTint = Color.White, screenText = "PLAY Mods",
        logoColor = Color(0xFF00C2C2),
        dpad = Color(0xFF1A1A1A), buttons = Color(0xFF1A1A1A),
        ledOn = Color(0xFFFF2D2D), translucent = true,
        specialBadge = "CRYSTAL", assetFile = "skins/gbasp5.jpg"
    )
    // gbasp7.jpg: minimal bianco/nero, testi MINIMAL AI WALLPAPER
    val MINIMAL_MONO = GbaSkin(
        id = "minimal_mono", label = "Minimal Mono",
        shellTop = Color.White, shellBottom = Color(0xFFD9D9D9),
        shellEdge = Color(0xFF111111), bezel = Color.Black,
        screenTint = Color.Black, screenText = "► START GAME",
        logoColor = Color.White,
        dpad = Color.Black, buttons = Color.Black, buttonText = Color.White,
        ledOn = Color(0xFFFF2D2D), specialBadge = "MINIMAL",
        assetFile = "skins/gbasp7.jpg"
    )
    // gbasp.jpg già = GOLD_ZELDA base

    val ALL = listOf(
        GOLD_ZELDA, GOLD_TRIFORCE, POKEMON_AQUA, FLAME_RED,
        MATTE_BLACK, GRAPHITE_RED, CRYSTAL_MOD, MINIMAL_MONO,
        PLATINUM, ONYX, COBALT, NES, TRIBAL
    )
    fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: GOLD_ZELDA
}

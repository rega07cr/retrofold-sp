# 🎨 Grafiche RetroFold SP — come usare le tue 8 immagini

Le foto originali vivono in `app/src/main/res/drawable-nodpi/`
(uguali al 100% perché SONO le tue foto, mostrate come sfondo in Flex Mode):

```
app/src/main/res/drawable-nodpi/
  skin_gold.jpg      -> Gold base (già default)
  gbasp1.jpg     -> Pokemon Aqua (azzurro + Squirtle)
  gbasp2.jpg     -> Graphite Red (grigio + tasti rossi Nintendo)
  gbasp3.jpg     -> Matte Black (nero opaco)
  gbasp4.jpg     -> Flame Red (#ff3333)
  gbasp4.png     -> Gold Zelda Triforce (oro + Triforce + SELECT/START)
  gbasp5.jpg     -> Crystal Mod (trasparente verde + PLAY Mods)
  gbasp7.jpg     -> Minimal Mono (bianco/nero MINIMAL AI)
```

## Collegamento in app (già fatto in `Skins.kt`)
Ogni skin ha `assetFile` che punta al file sopra. Se il file manca,
l'app usa il gradiente procedurale (stessi colori) — quindi compila comunque.

## Anteprima Flex
`FlexSpScreen` mostra:
- bezel nero + `POWER ON / BATTERY FULL / ▶ START GAME`
- guscio col colore della skin
- D-pad + pill B/A come nelle foto (blu su Pokemon, rossi su Graphite, ecc.)
- dettagli speciali:
  - Pokemon Aqua: logo Pokémon + Pokéball su Start/Select
  - Gold Zelda: Triforce dorata + LED verde + scritte SELECT/START
  - Graphite: scritte rosse `Nintendo B A` + LED verde + slat volume
  - Crystal: effetto traslucido + membrana cyan (semi-trasparenza)
  - Minimal: testi `MINIMAL AI WALLPAPER` sullo screen

## Aggiungere una nuova grafica
1. Metti il jpg/png in `app/src/main/assets/skins/`
2. Aggiungi una `GbaSkin(...)` in `Skins.kt` con `assetFile = "skins/tuo.jpg"`
3. Ricompila — appare nel picker skin in Flex Mode.

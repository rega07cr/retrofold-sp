package com.retrofold.sp.ui

/** Lingua scelta al primo avvio, salvata in DataStore. */
enum class AppLang(val code: String) {
    IT("it"), EN("en");

    companion object {
        fun fromCode(code: String?): AppLang? =
            entries.firstOrNull { it.code == code }
    }
}

/** Tutti i testi dell'app. Aggiungere una lingua = un nuovo oggetto qui. */
data class S(
    val splashLoading: String,
    val splashTapToSkip: String,
    val startingGame: String,
    val langTitle: String,
    val langSub: String,
    val libHow: String,
    val step1: String,
    val step2: String,
    val step3: String,
    val addRom: String,
    val addRomHint: String,
    val romsTitle: String,
    val emptyLib: String,
    val errUnsupported: String,
    val errInvalid: String,
    val errRead: String,
    val errZipEmpty: String,
    val errStart: String,
    val ok: String,
    val save: String,
    val load: String,
    val reset: String,
    val slot: String,
    val macro: String,
    val back: String,
    val score: String,
    val coverHint: String,
    val tapJump: String,
    val library: String,
    val openFlex: String,
    val coverWarn: String,
    val noRom: String,
    val langName: String,
    val crashTitle: String,
    val crashMsg: String,
    val share: String,
    val close: String,
    val customize: String,
    val done: String,
    val opacity: String,
    val resetPos: String,
    val editHint: String,
    val hideStyles: String,
    val showStyles: String,
    val hideThis: String,
    val showAll: String,
    val perGame: String,
    val gameSize: String,
    val lrSize: String,
    val dpadSize: String,
    val abSize: String,
    val sysSize: String,
    val resetBtns: String
) {
    companion object {
        val it = S(
            splashLoading = "CARICAMENTO…",
            splashTapToSkip = "tocca per saltare",
            startingGame = "AVVIO DEL GIOCO…",
            langTitle = "🌍 Scegli la lingua",
            langSub = "Puoi cambiarla quando vuoi dalle impostazioni",
            libHow = "🎮 Come si gioca:",
            step1 = "1️⃣ Tocca + e scegli la tua ROM (.gba, .gb, .gbc o .zip)",
            step2 = "2️⃣ Tocca un gioco per farlo partire",
            step3 = "3️⃣ Gioca a telefono aperto o piegato a 90°!",
            addRom = "＋ Aggiungi ROM / ZIP",
            addRomHint = "File tuoi — nessun gioco incluso nell'app",
            romsTitle = "📚 I tuoi giochi",
            emptyLib = "Ancora nessun gioco. Tocca ＋ e scegli un file dal telefono!",
            errUnsupported = "Formato non supportato. Usa .gba, .gb, .gbc oppure uno .zip che li contiene.",
            errInvalid = "File non valido: non sembra una ROM Game Boy vera.",
            errRead = "Impossibile leggere il file. Riprova.",
            errZipEmpty = "Lo ZIP non contiene nessuna ROM (.gba / .gb / .gbc).",
            errStart = "Impossibile avviare il gioco su questo telefono. Riprova o cambia ROM.",
            ok = "OK",
            save = "Salva",
            load = "Carica",
            reset = "Reset",
            slot = "Slot",
            macro = "Macro",
            back = "Giochi",
            score = "PUNTI",
            coverHint = "VOLUME SU = salto • VOLUME GIÙ = sparo\nTocca = A • Trascina = D-pad",
            tapJump = "▶ TOCCA = SALTO (+100)",
            library = "📚 Giochi",
            openFlex = "Apri a 90° → Game Boy",
            coverWarn = "Su questo modello la cover usa cover-launcher/Good Lock: segui il wizard al primo avvio.",
            noRom = "Nessuna ROM",
            langName = "🌍 Lingua",
            crashTitle = "L'app si è chiusa da sola",
            crashMsg = "Ecco cosa è successo. Condividi questo report: serve a sistemare il bug al 100%.",
            share = "Condividi",
            close = "Chiudi",
            customize = "✎ Tasti",
            done = "Fine",
            opacity = "Visibilità tasti",
            resetPos = "Reset posizioni",
            editHint = "Trascina OGNI tasto col dito dove vuoi (anche L/R). Regola visibilità e dimensioni qui sotto.",
            hideStyles = "Nascondi striscia stili",
            showStyles = "Mostra striscia stili",
            hideThis = "Nascondi",
            showAll = "↺ Mostra tutti gli stili",
            perGame = "🎮 Ricorda per questo gioco",
            gameSize = "Dimensione schermo",
            lrSize = "Dimensione L / R",
            dpadSize = "Dimensione D-pad",
            abSize = "Dimensione A / B",
            sysSize = "Dimensione Start / Select",
            resetBtns = "Reset solo tasti"
        )
        val en = S(
            splashLoading = "LOADING…",
            splashTapToSkip = "tap to skip",
            startingGame = "STARTING GAME…",
            langTitle = "🌍 Choose language",
            langSub = "You can change it anytime in settings",
            libHow = "🎮 How to play:",
            step1 = "1️⃣ Tap + and pick your ROM (.gba, .gb, .gbc or .zip)",
            step2 = "2️⃣ Tap a game to start it",
            step3 = "3️⃣ Play flat or folded at 90°!",
            addRom = "＋ Add ROM / ZIP",
            addRomHint = "Your own files — no games bundled in the app",
            romsTitle = "📚 Your games",
            emptyLib = "No games yet. Tap ＋ and pick a file from your phone!",
            errUnsupported = "Unsupported format. Use .gba, .gb, .gbc or a .zip containing them.",
            errInvalid = "Invalid file: it doesn't look like a real Game Boy ROM.",
            errRead = "Can't read the file. Try again.",
            errZipEmpty = "The ZIP contains no ROM (.gba / .gb / .gbc).",
            errStart = "Can't start the game on this phone. Try again or pick another ROM.",
            ok = "OK",
            save = "Save",
            load = "Load",
            reset = "Reset",
            slot = "Slot",
            macro = "Macro",
            back = "Games",
            score = "SCORE",
            coverHint = "VOLUME UP = jump • VOLUME DOWN = fire\nTap = A • Swipe = D-pad",
            tapJump = "▶ TAP = JUMP (+100)",
            library = "📚 Games",
            openFlex = "Open to 90° → Game Boy",
            coverWarn = "On this model the cover needs a cover-launcher/Good Lock: follow the wizard on first launch.",
            noRom = "No ROM",
            langName = "🌍 Language",
            crashTitle = "The app crashed",
            crashMsg = "Here's what happened. Share this report: it fixes the bug for good.",
            share = "Share",
            close = "Close",
            customize = "✎ Buttons",
            done = "Done",
            opacity = "Button visibility",
            resetPos = "Reset positions",
            editHint = "Drag EVERY button with your finger (L/R too). Adjust visibility and sizes below.",
            hideStyles = "Hide style strip",
            showStyles = "Show style strip",
            hideThis = "Hide",
            showAll = "↺ Show all styles",
            perGame = "🎮 Remember for this game",
            gameSize = "Game screen size",
            lrSize = "L / R size",
            dpadSize = "D-pad size",
            abSize = "A / B size",
            sysSize = "Start / Select size",
            resetBtns = "Reset buttons only"
        )

        fun of(lang: AppLang): S = when (lang) {
            AppLang.IT -> it
            AppLang.EN -> en
        }

        fun errorText(s: S, code: String?): String = when (code) {
            "ERR_UNSUPPORTED" -> s.errUnsupported
            "ERR_INVALID" -> s.errInvalid
            "ERR_ZIP_EMPTY" -> s.errZipEmpty
            "ERR_START" -> s.errStart
            else -> s.errRead
        }
    }
}
